package com.george.SlideMenuScrollView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private List<String> data;

    private List<SlideMenuScrollView> openedMenus = new ArrayList<>();

    public MyAdapter(List<String> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.contentText.setText(data.get(position));
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) holder.itemView.getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;

        LinearLayout contentLayout = holder.itemView.findViewById(R.id.content_layout);
        ViewGroup.LayoutParams layoutParams = contentLayout.getLayoutParams();
        layoutParams.width = screenWidth;
        contentLayout.setLayoutParams(layoutParams);

        holder.scrollView.setOnMenuStateChangeListener(new SlideMenuScrollView.OnMenuStateChangeListener() {
            @Override
            public void onMenuClosed(SlideMenuScrollView view) {
                openedMenus.remove(view);
            }

            @Override
            public void onMenuFullyOpened(SlideMenuScrollView view) {
                openedMenus.add(view);
            }

            @Override
            public void onMenuAboutToOpen(SlideMenuScrollView view) {
                for (SlideMenuScrollView openedMenu : new ArrayList<>(openedMenus)) {
                    openedMenu.scrollWithAnimation(0, 0,300);
                }
                openedMenus.clear();
            }

            @Override
            public void onActionDown(SlideMenuScrollView view) {
                for (SlideMenuScrollView openedMenu : new ArrayList<>(openedMenus)) {
                    if (openedMenu != view) {
                        openedMenu.scrollWithAnimation(0, 0,300);
                        openedMenus.remove(openedMenu);
                    }
                }
            }

            @Override
            public void onMenuConfirm(SlideMenuScrollView view) {
                data.remove(position);
                notifyDataSetChanged();
            }

            @Override
            public void onContentClick(SlideMenuScrollView view) {
                Toast.makeText(view.getContext(), "Menu confirm", Toast.LENGTH_SHORT).show();
            }
        });


    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        SlideMenuScrollView scrollView;
        TextView contentText;
        TextView menuText;
        LinearLayout menuLayout;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            scrollView = itemView.findViewById(R.id.scroll_view);
            contentText = itemView.findViewById(R.id.content_text);
            menuText = itemView.findViewById(R.id.menu_text);
            menuLayout = itemView.findViewById(R.id.menu_layout);
        }
    }
}

