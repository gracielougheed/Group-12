package com.example.mainproject;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mainproject.entities.Ingredient;
import com.example.mainproject.entities.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShoppingListActivity extends AppCompatActivity {

    private LinearLayout recipeSelectionContainer;
    private Button btnGenerateList;
    private TextView shoppingListResult;
    private DatabaseReference recipesRef;
    private List<Recipe> allRecipes = new ArrayList<>();
    private List<CheckBox> checkBoxes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list);
        setTitle("Shopping List");

        recipeSelectionContainer = findViewById(R.id.recipeSelectionContainer);
        btnGenerateList = findViewById(R.id.btnGenerateList);
        shoppingListResult = findViewById(R.id.shoppingListResult);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            recipesRef = FirebaseDatabase.getInstance(
                    "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
            ).getReference("users").child(uid).child("recipes");
            loadRecipesForSelection();
        }

        btnGenerateList.setOnClickListener(v -> generateShoppingList());
    }

    private void loadRecipesForSelection() {
        recipesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recipeSelectionContainer.removeAllViews();
                allRecipes.clear();
                checkBoxes.clear();

                for (DataSnapshot recipeSnapshot : snapshot.getChildren()) {
                    Recipe recipe = recipeSnapshot.getValue(Recipe.class);
                    if (recipe != null) {
                        allRecipes.add(recipe);
                        addRecipeToSelectionUI(recipe);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ShoppingListActivity.this, "Failed to load recipes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addRecipeToSelectionUI(Recipe recipe) {
        View view = getLayoutInflater().inflate(R.layout.item_recipe_selection, recipeSelectionContainer, false);
        CheckBox checkBox = view.findViewById(R.id.recipeCheckbox);
        TextView nameText = view.findViewById(R.id.recipeName);

        nameText.setText(recipe.title);
        checkBoxes.add(checkBox);
        recipeSelectionContainer.addView(view);
    }

    private void generateShoppingList() {
        Map<String, List<String>> combinedIngredients = new HashMap<>();

        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isChecked()) {
                Recipe selectedRecipe = allRecipes.get(i);
                if (selectedRecipe.ingredients != null) {
                    for (Ingredient ingredient : selectedRecipe.ingredients) {
                        String key = ingredient.name.toLowerCase().trim();
                        String detail = ingredient.quantity + " " + ingredient.unit;
                        
                        if (!combinedIngredients.containsKey(key)) {
                            combinedIngredients.put(key, new ArrayList<>());
                        }
                        combinedIngredients.get(key).add(detail);
                    }
                }
            }
        }

        if (combinedIngredients.isEmpty()) {
            shoppingListResult.setText("No recipes selected or no ingredients found.");
            return;
        }

        StringBuilder resultBuilder = new StringBuilder();
        resultBuilder.append("Shopping List:\n\n");
        for (Map.Entry<String, List<String>> entry : combinedIngredients.entrySet()) {
            resultBuilder.append("- ").append(capitalize(entry.getKey())).append(": ");
            List<String> details = entry.getValue();
            for (int i = 0; i < details.size(); i++) {
                resultBuilder.append(details.get(i));
                if (i < details.size() - 1) resultBuilder.append(" + ");
            }
            resultBuilder.append("\n");
        }

        shoppingListResult.setText(resultBuilder.toString());
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
