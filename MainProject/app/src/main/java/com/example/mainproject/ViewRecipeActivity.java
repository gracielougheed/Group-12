package com.example.mainproject;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mainproject.Recipes.EditRecipeActivity;
import com.example.mainproject.ShareRecipeActivity;
import com.example.mainproject.entities.Ingredient;
import com.example.mainproject.entities.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * ViewRecipeActivity loads and displays full recipe details from Firebase.
 * Always reads fresh data so edits are immediately reflected.
 */
public class ViewRecipeActivity extends AppCompatActivity {

    private static final String DB_URL =
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/";

    private String recipeId;
    private String ownerUid;

    // Header
    private TextView tvTitle;

    // Info row
    private TextView tvCategory, tvPrepTime, tvCookTime, tvServings, tvDifficulty, tvVisibility;

    // Sections
    private TextView tvTags, tvCookware;
    private LinearLayout layoutInstructions, layoutIngredients;

    // Buttons
    private Button btnEdit, btnShare, btnDelete;

    private ValueEventListener recipeListener;
    private DatabaseReference recipeRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recipe);

        recipeId = getIntent().getStringExtra("RECIPE_ID");
        ownerUid = getIntent().getStringExtra("OWNER_UID");

        String currentUid = FirebaseAuth.getInstance().getUid();

        // If no ownerUid provided, assume it's the current user's recipe
        if (ownerUid == null) {
            ownerUid = currentUid;
        }

        bindViews();

        // Hide owner-only buttons if viewing someone else's recipe
        boolean isOwner = ownerUid.equals(currentUid);
        btnEdit.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        btnShare.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        btnDelete.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        btnEdit.setOnClickListener(v -> openEditActivity());
        btnShare.setOnClickListener(v -> {
            Intent intent = new Intent(this, ShareRecipeActivity.class);
            intent.putExtra("RECIPE_ID", recipeId);
            startActivity(intent);
        });
        btnDelete.setOnClickListener(v -> confirmDelete());

        // Load from Firebase
        recipeRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("users").child(ownerUid).child("recipes").child(recipeId);

        listenForRecipe();
    }

    private void bindViews() {
        tvTitle       = findViewById(R.id.tvTitle);
        tvCategory    = findViewById(R.id.tvCategory);
        tvPrepTime    = findViewById(R.id.tvPrepTime);
        tvCookTime    = findViewById(R.id.tvCookTime);
        tvServings    = findViewById(R.id.tvServings);
        tvDifficulty  = findViewById(R.id.tvDifficulty);
        tvVisibility  = findViewById(R.id.tvVisibility);
        tvTags        = findViewById(R.id.tvTags);
        tvCookware    = findViewById(R.id.tvCookware);
        layoutInstructions = findViewById(R.id.layoutInstructions);
        layoutIngredients  = findViewById(R.id.layoutIngredients);
        btnEdit   = findViewById(R.id.btnEdit);
        btnShare  = findViewById(R.id.btnShare);
        btnDelete = findViewById(R.id.btnDelete);
    }

    /** Attaches a realtime listener so the screen refreshes after an edit. */
    private void listenForRecipe() {
        recipeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Recipe recipe = snapshot.getValue(Recipe.class);
                if (recipe != null) {
                    populateUI(recipe);
                } else {
                    Toast.makeText(ViewRecipeActivity.this,
                            "Recipe not found.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewRecipeActivity.this,
                        "Failed to load recipe.", Toast.LENGTH_SHORT).show();
            }
        };
        recipeRef.addValueEventListener(recipeListener);
    }

    private void populateUI(Recipe r) {
        tvTitle.setText(r.title != null ? r.title : "");
        tvCategory.setText(r.category != null ? r.category : "");

        tvPrepTime.setText(r.prepTimeValue + " " + (r.prepTimeUnit != null ? r.prepTimeUnit : ""));
        tvCookTime.setText(r.cookTimeValue + " " + (r.cookTimeUnit != null ? r.cookTimeUnit : ""));
        tvServings.setText(String.valueOf(r.servingSize));

        // Difficulty stored as int 1-5 (or whatever your spinner maps to)
        tvDifficulty.setText("Difficulty: " + r.difficultyLevel);
        tvVisibility.setText(r.isPublic ? "Public" : "Private");

        // Tags
        if (r.tags != null && !r.tags.isEmpty()) {
            tvTags.setText(android.text.TextUtils.join(", ", r.tags));
        } else {
            tvTags.setText("None");
        }

        // Cookware
        if (r.cookware != null && !r.cookware.isEmpty()) {
            tvCookware.setText(android.text.TextUtils.join(", ", r.cookware));
        } else {
            tvCookware.setText("None");
        }

        // Instructions
        layoutInstructions.removeAllViews();
        if (r.instructions != null) {
            for (int i = 0; i < r.instructions.size(); i++) {
                TextView tv = makeBodyTextView((i + 1) + ". " + r.instructions.get(i));
                layoutInstructions.addView(tv);
            }
        }

        // Ingredients
        layoutIngredients.removeAllViews();
        if (r.ingredients != null) {
            for (Ingredient ing : r.ingredients) {
                String line = ing.quantity + " " + ing.unit + " " + ing.name;
                if (ing.prep != null && !ing.prep.isEmpty()) line += " (" + ing.prep + ")";
                layoutIngredients.addView(makeBodyTextView("• " + line));
            }
        }
    }

    private TextView makeBodyTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15f);
        tv.setPadding(0, 6, 0, 6);
        return tv;
    }

    private void openEditActivity() {
        Intent intent = new Intent(this, EditRecipeActivity.class);
        intent.putExtra("RECIPE_ID", recipeId);
        startActivity(intent);
        // No finish() — user returns here; the realtime listener will refresh data automatically.
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Recipe")
                .setMessage("Are you sure you want to permanently delete this recipe?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRecipe())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecipe() {
        recipeRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Recipe deleted.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to delete recipe.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Go back arrow in header */
    public void goBack(View view) {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recipeRef != null && recipeListener != null) {
            recipeRef.removeEventListener(recipeListener);
        }
    }
}