package com.example.mainproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.mainproject.Friends.FriendsFragment;
import com.example.mainproject.Recipes.RecipeActivity;
import com.example.mainproject.Recipes.RecipesFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        String fragmentToOpen = getIntent().getStringExtra("openFragment");

        if (savedInstanceState == null) {
            Fragment defaultFragment;

            if ("friends".equals(fragmentToOpen)) {
                defaultFragment = new FriendsFragment();
                bottomNav.setSelectedItemId(R.id.nav_friends);
            } else {
                defaultFragment = new RecipesFragment();
                bottomNav.setSelectedItemId(R.id.nav_recipes);
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, defaultFragment)
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_recipes) {
                    selectedFragment = new RecipesFragment();
                } else if (itemId == R.id.nav_pantry) {
                    selectedFragment = new PantryFragment();
                } else if (itemId == R.id.nav_friends) {
                    selectedFragment = new FriendsFragment();
                } else if (itemId == R.id.nav_meal_planner) {
                    selectedFragment = new MealPlannerFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                    return true;
                }
                return false;
            }
        });
    }

    public void addRecipe(View view){
        Intent intent = new Intent(this, RecipeActivity.class);
        startActivity(intent);
    }
}