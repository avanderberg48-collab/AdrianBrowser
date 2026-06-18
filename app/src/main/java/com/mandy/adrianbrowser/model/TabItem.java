package com.mandy.adrianbrowser.model;

public class TabItem {
    public String title;
    public String url;
    public boolean isActive;
    public int id;
    private static int counter = 0;
    public TabItem(String title, String url) {
        this.title = title; this.url = url;
        this.id = ++counter;
    }
}
