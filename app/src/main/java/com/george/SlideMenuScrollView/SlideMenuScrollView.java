package com.george.SlideMenuScrollView;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;

public class SlideMenuScrollView extends HorizontalScrollView {
    private int mScrollThreshold;
    private int menuDefaultWidth;
    private float initialX = 0;
    private float initialY = 0;
    private final float TOUCH_THRESHOLD = 10f;
    private boolean isMoving;
    private boolean isMenuConfirm = false;

    /**
     * slide-out menu reveals extra options when pulled from the side.
     */
    private int menuId;
    /**
     *The content area of an item displayed on the screen.
     */
    private int contentLayoutId;

    private TextView menuText;
    private LinearLayout contentLayout;

    private OnMenuStateChangeListener mOnMenuStateChangeListener;

    public SlideMenuScrollView(Context context) {
        this(context, null);
    }

    public SlideMenuScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SlideToDeleteScrollView);
        menuId = typedArray.getResourceId(R.styleable.SlideToDeleteScrollView_menu_id, -1);
        contentLayoutId = typedArray.getResourceId(R.styleable.SlideToDeleteScrollView_content_layout_id, -1);
        typedArray.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        validateViewId(menuId, "SlideToDeleteScrollView_menu_id");
        menuText = findViewById(menuId);
        validateViewId(contentLayoutId, "SlideToDeleteScrollView_content_layout_id");
        contentLayout = findViewById(contentLayoutId);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        ViewGroup.LayoutParams layoutParams = contentLayout.getLayoutParams();
        layoutParams.width = screenWidth;
        contentLayout.setLayoutParams(layoutParams);

        menuText.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mScrollThreshold = menuText.getWidth();
                menuDefaultWidth = mScrollThreshold;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    menuText.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    menuText.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });

        menuText.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mOnMenuStateChangeListener.onActionDown(SlideMenuScrollView.this);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (isMenuConfirm) {
                            mOnMenuStateChangeListener.onMenuConfirm(SlideMenuScrollView.this);
                        } else {
                            updateMenuState();
                        }
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (isFullyOpened() && oldl < mScrollThreshold) {
            notifyMenuFullyOpened();
        } else if (l == 0) {
            notifyMenuClosed();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                initialX = ev.getX();
                initialY = ev.getY();
                isMoving = false;
                mOnMenuStateChangeListener.onActionDown(this);
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(initialX - ev.getX()) > TOUCH_THRESHOLD) {
                    isMoving = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                mScrollThreshold = menuText.getWidth();
                if (!isMoving) {
                    notifyAboutToOpen();
                    if (getScrollX() == 0) {
                        mOnMenuStateChangeListener.onContentClick(this);
                    }
                } else {
                    if (getScrollX() > mScrollThreshold / 2) {
                        smoothScrollTo(mScrollThreshold, 0);
                    } else if (getScrollX() <= mScrollThreshold / 2) {
                        smoothScrollTo(0, 0);
                    }
                }
                break;
            default:
                break;
        }
        return super.onTouchEvent(ev);
    }

    public void scrollWithAnimation(int destX, int destY, int duration) {
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        int startX = getScrollX();
        int startY = getScrollY();
        int deltaX = destX - startX;
        int deltaY = destY - startY;

        animator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            int scrollX = (int) (startX + fraction * deltaX);
            int scrollY = (int) (startY + fraction * deltaY);
            scrollTo(scrollX, scrollY);
        });

        animator.setDuration(duration);
        animator.start();
    }

    private void validateViewId(int viewId, String attributeName) {
        if (viewId == -1) {
            throw new IllegalArgumentException("You must set the '" + attributeName + "' attribute.");
        }
        View view = findViewById(viewId);
        if (view == null) {
            throw new IllegalArgumentException("No view found with the specified '" + attributeName + "'.");
        }
    }

    private void updateMenuState() {
        isMenuConfirm = true;
        menuText.setText(getResources().getText(R.string.confirm_delete));
        ViewGroup.LayoutParams params = menuText.getLayoutParams();
        int newWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 144, getResources().getDisplayMetrics());
        int difference = newWidth - params.width;
        params.width = newWidth;
        menuText.setLayoutParams(params);
        scrollWithAnimation(mScrollThreshold + difference, 0, 100);
    }

    private void resetMenuState() {
        ViewGroup.LayoutParams textParams = menuText.getLayoutParams();
        if (textParams.width != menuDefaultWidth) {
            isMenuConfirm = false;
            menuText.setText(getResources().getText(R.string.delete));
            textParams.width = menuDefaultWidth;
            menuText.setLayoutParams(textParams);
            mScrollThreshold = menuDefaultWidth;
        }
    }

    private void notifyAboutToOpen() {
        mOnMenuStateChangeListener.onMenuAboutToOpen(this);
    }

    private boolean isFullyOpened() {
        return getScrollX() >= mScrollThreshold;
    }

    private void notifyMenuClosed() {
        mOnMenuStateChangeListener.onMenuClosed(this);
        isMenuConfirm = false;
        resetMenuState();
    }

    private void notifyMenuFullyOpened() {
        if (isFullyOpened()) {
            mOnMenuStateChangeListener.onMenuFullyOpened(this);
        }
    }

    public void setOnMenuStateChangeListener(OnMenuStateChangeListener listener) {
        this.mOnMenuStateChangeListener = listener;
    }

    public void removeOnMenuStateChangeListener(OnMenuStateChangeListener listener) {
        this.mOnMenuStateChangeListener = null;
    }

    public interface OnMenuStateChangeListener {
        /**
         * @Scenario: When the slide menu is closed.
         * @Function: Removes the closed menu from a list of open menus.
         */
        void onMenuClosed(SlideMenuScrollView view);

        /**
         * @Scenario: When the slide menu is fully opened.
         * @Function: Adds the menu to a list of open menus.
         */
        void onMenuFullyOpened(SlideMenuScrollView view);

        /**
         * @Scenario: When the slide menu is about to open.
         * @Function: Closes any already opened menus.
         */
        void onMenuAboutToOpen(SlideMenuScrollView view);

        /**
         * @Scenario: When a finger is pressed down.
         * @Function: Closes all menus except the current one.
         */
        void onActionDown(SlideMenuScrollView view);

        /**
         * @Scenario: When the slide menu is confirmed.
         * @Function: Performs actions related to confirming the menu.
         */
        void onMenuConfirm(SlideMenuScrollView view);

        /**
         * @Scenario: When the content area is clicked.
         * @Function: Performs actions related to clicking on the content area.
         */
        void onContentClick(SlideMenuScrollView view);
    }
}
