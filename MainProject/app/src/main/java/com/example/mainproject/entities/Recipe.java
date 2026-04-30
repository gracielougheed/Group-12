package com.example.mainproject.entities;

import java.util.List;

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
    public boolean isPublic;
    public List<String> tags;
    public List<Ingredient> ingredients;
    public List<String> cookware;
    public List<String> instructions;
    public List<String> collaborators;
    public String ownerId;

    public Recipe() {}

    public Recipe(String recipeId, String title, String description, String category, int prepTimeValue,
                  String prepTimeUnit, int cookTimeValue, String cookTimeUnit, int servingSize,
                  int difficultyLevel, boolean isPublic, List<String> tags,
                  List<Ingredient> ingredients, List<String> cookware, List<String> instructions,
                List<String> collaborators, String ownerId ) {
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
        this.isPublic = isPublic;
        this.tags = tags;
        this.ingredients = ingredients;
        this.cookware = cookware;
        this.instructions = instructions;
        this.collaborators = collaborators;
        this.ownerId = ownerId;
    }
}
