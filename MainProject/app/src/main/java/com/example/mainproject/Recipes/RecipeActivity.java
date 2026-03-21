package com.example.mainproject.Recipes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mainproject.HomeActivity;
import com.example.mainproject.R;
import com.example.mainproject.entities.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RecipeActivity extends AppCompatActivity {
    // Firebase
    FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
    );
    private FirebaseAuth myAuth;

    // UI references
    private EditText recipeTitle;
    private EditText recipeDesc;
    private Spinner recipeCategory;
    private EditText recipePrepTime;
    private Spinner recipePrepTimeUnits;
    private EditText recipeCookTime;
    private Spinner recipeCookTimeUnits;
    private EditText recipeServingSize;
    private Spinner recipeDifficultyLevel;

    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recipe);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase auth
        myAuth = FirebaseAuth.getInstance();

        // Find all views
        recipeTitle = findViewById(R.id.recipeTitle);
        recipeDesc = findViewById(R.id.recipeDesc);
        recipeCategory = findViewById(R.id.recipeCategory);
        recipePrepTime = findViewById(R.id.recipePrepTime);
        recipePrepTimeUnits = findViewById(R.id.recipePrepTimeUnits);
        recipeCookTime = findViewById(R.id.recipeCookTime);
        recipeCookTimeUnits = findViewById(R.id.recipeCookTimeUnits);
        recipeServingSize = findViewById(R.id.recipeServingSize);
        recipeDifficultyLevel = findViewById(R.id.recipeDifficultyLevel);
        saveButton = findViewById(R.id.addRecipeButton);

        // Set up Spinners
        setupCategorySpinner();
        setupTimeUnitSpinners();
        setupDifficultySpinner();
    }

    private void setupCategorySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recipeCategory.setAdapter(adapter);
    }

    private void setupTimeUnitSpinners() {
        ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(
                this, R.array.time_units, android.R.layout.simple_spinner_item);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recipePrepTimeUnits.setAdapter(timeAdapter);
        recipeCookTimeUnits.setAdapter(timeAdapter);
    }

    private void setupDifficultySpinner() {
        ArrayAdapter<CharSequence> difficultyAdapter = ArrayAdapter.createFromResource(
                this, R.array.difficulty_levels, android.R.layout.simple_spinner_item);

    }

    public void saveRecipe(View view){
        String title = recipeTitle.getText().toString().trim();
        String desc = recipeDesc.getText().toString().trim();

        if (!recipeInfoValidation(title, desc)) {
            return;
        }
        saveButton.setEnabled(false);

        Recipe recipe = new Recipe(recipeId, title, desc, category, prepTimeValue,
                prepTimeUnit, cookTimeValue, cookTimeUnit, servingSize, difficultyLevel);
        writeRecipe(recipe);
        saveButton.setEnabled(true);
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

        //call recipe validation to validate recipes
        //create recipe object from recipe form
        //then call writeRecipe to write to the databse

    public boolean recipeInfoValidation(String title, String desc){
        if (title.isEmpty()) {
            recipeTitle.setError("Title is required");
            recipeTitle.requestFocus();
            return false;
        }
        if (desc.isEmpty()) {
            recipeDesc.setError("Description is required");
            recipeDesc.requestFocus();
            return false;
        }
        return true;
    }

    public void writeRecipe(Recipe recipe) {
        if (recipe == null) return;

        if (myAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            return;
        }

        String uid = myAuth.getCurrentUser().getUid();
        DatabaseReference recipesRef = database.getReference("users")
                .child(uid)
                .child("recipes");

        recipesRef.push().setValue(recipe)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Recipe saved", Toast.LENGTH_SHORT).show();
                    saveButton.setEnabled(true);
                    startActivity(new Intent(this, HomeActivity.class));
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("RecipeActivity", "Write failed", e);
                    Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    saveButton.setEnabled(true);
                });
    }


    public void goBack(View view){
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }
}