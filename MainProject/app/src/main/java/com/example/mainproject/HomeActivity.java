package com.example.mainproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

/**
 * HomeActivity serves as the main menu of the CookBook app.
 * It provides navigation to different modules of the application.
 */
public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize Cards
        MaterialCardView cardRecipes = findViewById(R.id.cardRecipes);
        MaterialCardView cardMealPlanner = findViewById(R.id.cardMealPlanner);
        MaterialCardView cardShoppingList = findViewById(R.id.cardShoppingList);
        MaterialCardView cardPantry = findViewById(R.id.cardPantry);

        // Set Click Listeners for navigation
        cardRecipes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to RecipesActivity
                Intent intent = new Intent(HomeActivity.this, RecipesActivity.class);
                startActivity(intent);
            }
        });

        cardMealPlanner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to MealPlannerActivity
                Intent intent = new Intent(HomeActivity.this, MealPlannerActivity.class);
                startActivity(intent);
            }
        });

        cardShoppingList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to ShoppingListActivity
                Intent intent = new Intent(HomeActivity.this, ShoppingListActivity.class);
                startActivity(intent);
            }
        });

        cardPantry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to PantryActivity
                Intent intent = new Intent(HomeActivity.this, PantryActivity.class);
                startActivity(intent);
            }
        });
    }
}
