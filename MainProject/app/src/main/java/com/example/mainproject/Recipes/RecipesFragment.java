package com.example.mainproject.Recipes;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.mainproject.R;
import com.example.mainproject.ShoppingListActivity;
import com.example.mainproject.entities.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RecipesFragment extends Fragment {

    private LinearLayout breakfastContainer, lunchContainer, dinnerContainer, dessertContainer, snackContainer;
    private DatabaseReference recipesRef;

    public RecipesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipes, container, false);

        // Initialize containers
        breakfastContainer = view.findViewById(R.id.breakfastContainer);
        lunchContainer = view.findViewById(R.id.lunchContainer);
        dinnerContainer = view.findViewById(R.id.dinnerContainer);
        dessertContainer = view.findViewById(R.id.dessertContainer);
        snackContainer = view.findViewById(R.id.snackContainer);

        // Set up Add Recipe button
        view.findViewById(R.id.addRecipeButton).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RecipeActivity.class);
            startActivity(intent);
        });

        // Set up Shopping List button
        view.findViewById(R.id.btnGoToShoppingList).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ShoppingListActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.sharedRecipes).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ViewFriendsRecipesActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.btnGoToCollabs).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CollaborativeActivity.class);
            startActivity(intent);
        });

        // Initialize Firebase Reference
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            recipesRef = FirebaseDatabase.getInstance(
                    "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
            ).getReference("users").child(uid).child("recipes");

            loadRecipes();
        }

        return view;
    }

    private void loadRecipes() {
        recipesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Clear all containers before loading new data
                breakfastContainer.removeAllViews();
                lunchContainer.removeAllViews();
                dinnerContainer.removeAllViews();
                dessertContainer.removeAllViews();
                snackContainer.removeAllViews();

                for (DataSnapshot recipeSnapshot : snapshot.getChildren()) {
                    Recipe recipe = recipeSnapshot.getValue(Recipe.class);
                    if (recipe != null) {
                        addRecipeToUI(recipe);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle database error if needed
            }
        });
    }

    private void addRecipeToUI(Recipe recipe) {
        // Create a simple text view for each recipe
        TextView recipeItem = new TextView(getContext());
        recipeItem.setText(recipe.title);
        recipeItem.setTextSize(18);
        recipeItem.setPadding(10, 20, 10, 20);
        recipeItem.setBackgroundResource(android.R.drawable.list_selector_background);
        recipeItem.setClickable(true);
        recipeItem.setFocusable(true);

        // Navigate to ViewRecipeActivity on click
        recipeItem.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ViewRecipeActivity.class);
            intent.putExtra("title", recipe.title);
            intent.putExtra("instructions", recipe.description);
            // Passing other details for ViewRecipeActivity
            intent.putExtra("CATEGORY", recipe.category);
            intent.putExtra("PREP_TIME", recipe.prepTimeValue);
            intent.putExtra("PREP_UNIT", recipe.prepTimeUnit);
            intent.putExtra("COOK_TIME", recipe.cookTimeValue);
            intent.putExtra("COOK_UNIT", recipe.cookTimeUnit);
            intent.putExtra("SERVINGS", recipe.servingSize);
            intent.putExtra("RECIPE_ID", recipe.recipeId);
            startActivity(intent);
        });

        // Add to the appropriate category container
        String category = recipe.category != null ? recipe.category.toLowerCase() : "";
        if (category.contains("breakfast")) {
            breakfastContainer.addView(recipeItem);
        } else if (category.contains("lunch")) {
            lunchContainer.addView(recipeItem);
        } else if (category.contains("dinner")) {
            dinnerContainer.addView(recipeItem);
        } else if (category.contains("dessert")){
            dessertContainer.addView(recipeItem);
        } else {
            snackContainer.addView(recipeItem);
        }
    }
}
