package com.example.mainproject.Recipes;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mainproject.R;
import com.example.mainproject.ViewRecipeActivity;
import com.example.mainproject.entities.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class RecipesFragment extends Fragment {

    private LinearLayout breakfastContainer;
    private LinearLayout lunchContainer;
    private LinearLayout dinnerContainer;
    private LinearLayout dessertContainer;

    public RecipesFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recipes, container, false);

        breakfastContainer = view.findViewById(R.id.breakfastContainer);
        lunchContainer = view.findViewById(R.id.lunchContainer);
        dinnerContainer = view.findViewById(R.id.dinnerContainer);
        dessertContainer = view.findViewById(R.id.dessertContainer);

        view.findViewById(R.id.addRecipeButton).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), RecipeActivity.class));
        });

        loadRecipesFromFirebase();

        return view;
    }

    private void loadRecipesFromFirebase() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance(
                "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
        ).getReference("users").child(uid).child("recipes");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Clear containers before reloading
                breakfastContainer.removeAllViews();
                lunchContainer.removeAllViews();
                dinnerContainer.removeAllViews();
                dessertContainer.removeAllViews();

                for (DataSnapshot recipeSnapshot : snapshot.getChildren()) {
                    Recipe recipe = recipeSnapshot.getValue(Recipe.class);
                    if (recipe != null) {
                        displayRecipe(recipe);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load recipes", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void displayRecipe(Recipe recipe) {
        // Simple View for the list item
        View itemView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);
        TextView text = itemView.findViewById(android.R.id.text1);
        text.setText(recipe.title);
        text.setPadding(0, 20, 0, 20);

        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ViewRecipeActivity.class);
            intent.putExtra("RECIPE_ID", recipe.recipeId);
            intent.putExtra("TITLE", recipe.title);
            intent.putExtra("DESCRIPTION", recipe.description);
            intent.putExtra("CATEGORY", recipe.category);
            intent.putExtra("PREP_TIME", recipe.prepTimeValue);
            intent.putExtra("PREP_UNIT", recipe.prepTimeUnit);
            intent.putExtra("COOK_TIME", recipe.cookTimeValue);
            intent.putExtra("COOK_UNIT", recipe.cookTimeUnit);
            intent.putExtra("SERVINGS", recipe.servingSize);
            startActivity(intent);
        });

        // Add to correct container based on category
        String cat = recipe.category.toLowerCase();
        if (cat.contains("breakfast")) {
            breakfastContainer.addView(itemView);
        } else if (cat.contains("lunch")) {
            lunchContainer.addView(itemView);
        } else if (cat.contains("dinner")) {
            dinnerContainer.addView(itemView);
        } else {
            dessertContainer.addView(itemView);
        }
    }
}
