package com.example.mainproject.entities;

public class Ingredient {
    public String ingredientId;
    public String name;
    public String quantity;
    public String unit;
    public String prep;

    public Ingredient() {}

    public Ingredient(String ingredientId, String name, String quantity,
                      String unit, String prep) {
        this.ingredientId = ingredientId;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.prep = prep;
    }
}
