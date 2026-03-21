package com.example.mainproject.entities;

public class Recipe {
    public String recipeId;
    public String title;
    public String description;
    public String category;

    public Recipe() {}

    public Recipe(String title, String description, String category) {
        this.title = title;
        this.description = description;
        this.category = category;
    }
}
