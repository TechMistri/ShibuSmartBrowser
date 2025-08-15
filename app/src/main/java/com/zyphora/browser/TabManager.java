package com.zyphora.browser;

import java.util.ArrayList;
import java.util.List;

public class TabManager {
    private List<Tab> tabs;
    private int currentTabIndex;

    public TabManager() {
        tabs = new ArrayList<>();
        currentTabIndex = -1;
    }

    public void addTab(Tab tab) {
        tabs.add(tab);
        currentTabIndex = tabs.size() - 1;
    }

    public void removeTab(int index) {
        tabs.remove(index);
        if (currentTabIndex >= index) {
            currentTabIndex--;
        }
    }

    public Tab getTab(int index) {
        return tabs.get(index);
    }

    public Tab getCurrentTab() {
        if (currentTabIndex >= 0 && currentTabIndex < tabs.size()) {
            return tabs.get(currentTabIndex);
        }
        return null;
    }

    public void setCurrentTabIndex(int index) {
        currentTabIndex = index;
    }

    public int getCurrentTabIndex() {
        return currentTabIndex;
    }

    public int getTabCount() {
        return tabs.size();
    }
}
