package com.example.gesturemaster;


public class ActionItem {
    private String title;
    private int frottements;
    private int iconResId;
    private boolean enabled; // Nouveau champ

    public ActionItem(String title, int frottements, int iconResId) {
        this.title = title;
        this.frottements = frottements;
        this.iconResId = iconResId;
        this.enabled = true; // Valeur par défaut
    }

    // Getters et setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getFrottements() {
        return frottements;
    }

    public void setFrottements(int frottements) {
        this.frottements = frottements;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
