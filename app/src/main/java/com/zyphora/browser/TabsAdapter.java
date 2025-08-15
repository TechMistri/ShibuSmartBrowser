package com.zyphora.browser;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class TabsAdapter extends FragmentStateAdapter {

    private List<Tab> tabs;

    public TabsAdapter(@NonNull FragmentActivity fragmentActivity, List<Tab> tabs) {
        super(fragmentActivity);
        this.tabs = tabs;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return BrowserFragment.newInstance(tabs.get(position).getUrl());
    }

    @Override
    public int getItemCount() {
        return tabs.size();
    }

    public List<Tab> getTabs() {
        return tabs;
    }
}
