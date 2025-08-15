package com.zyphora.browser;

public class MenuItem {
    private String text;
    private int icon;

    public MenuItem(String text, int icon) {
        this.text = text;
        this.icon = icon;
    }

    public String getText() {
        return text;
    }

    public int getIcon() {
        return icon;
    }
}
