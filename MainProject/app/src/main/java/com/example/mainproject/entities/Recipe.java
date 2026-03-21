package com.example.mainproject.entities;

public class Recipe {
    public String recipeId;
    public String title;
    public String description;
    public String category;
    public int prepTimeValue;
    public String prepTimeUnit;
    public int cookTimeValue;
    public String cookTimeUnit;
    public int servingSize;
    public int difficultyLevel;

    public Recipe() {}

    public Recipe(String recipeId, String title, String description, String category, int prepTimeValue,
                  String prepTimeUnit, int cookTimeValue, String cookTimeUnit, int servingSize, int difficultyLevel) {
        this.recipeId = recipeId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.prepTimeValue = prepTimeValue;
        this.prepTimeUnit = prepTimeUnit;
        this.cookTimeValue = cookTimeValue;
        this.cookTimeUnit = cookTimeUnit;
        this.servingSize = servingSize;
        this.difficultyLevel = difficultyLevel;
    }
}
