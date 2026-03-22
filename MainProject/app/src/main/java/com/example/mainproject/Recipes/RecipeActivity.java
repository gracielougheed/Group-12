package com.example.mainproject.Recipes;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
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

import java.text.NumberFormat;

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

        recipePrepTime.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        recipeCookTime.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        recipeServingSize.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        recipePrepTime.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(4) });
        recipeCookTime.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(4) });
        recipeServingSize.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(3) });

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
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recipeDifficultyLevel.setAdapter(difficultyAdapter);
    }

    public void saveRecipe(View view){
        // Disable button to prevent invalid input
        saveButton.setEnabled(false);

        // Read and trim text fields
        String title = recipeTitle.getText().toString().trim();
        String desc = recipeDesc.getText().toString().trim();
        String category = (String) recipeCategory.getSelectedItem();
        String prepTimeText = recipePrepTime.getText().toString().trim();
        String cookTimeText = recipeCookTime.getText().toString().trim();
        String servingSizeText = recipeServingSize.getText().toString().trim();
        String prepTimeUnit = (String) recipePrepTimeUnits.getSelectedItem();
        String cookTimeUnit = (String) recipeCookTimeUnits.getSelectedItem();
        String difficultyText = (String) recipeDifficultyLevel.getSelectedItem();

        // Validate all inputs
        if (!validateInputs(title, desc, category, prepTimeText, prepTimeUnit, cookTimeText,
                cookTimeUnit, servingSizeText, difficultyText)) {
            saveButton.setEnabled(true);
            return;
        }

        // Safe parsing (after validation)
        int prepTimeValue = Integer.parseInt(prepTimeText);
        int cookTimeValue = Integer.parseInt(cookTimeText);
        int servingSize = Integer.parseInt(servingSizeText);
        int difficultyLevel = Integer.parseInt(difficultyText);

        // Ensure user is signed in
        if (myAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            return;
        }

        String uid = myAuth.getCurrentUser().getUid();
        DatabaseReference recipesRef = database.getReference("users").child(uid).child("recipes");

        // Generate a new key for this recipe
        String recipeId = recipesRef.push().getKey();
        if (recipeId == null) {
            Toast.makeText(this, "Could not generate recipe ID", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            return;
        }

        // Build recipe object
        Recipe recipe = new Recipe(recipeId, title, desc, category, prepTimeValue,
                prepTimeUnit, cookTimeValue, cookTimeUnit, servingSize, difficultyLevel);

        // Write to Firebase
        recipesRef.child(recipeId).setValue(recipe).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Recipe saved", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            startActivity(new Intent(this, HomeActivity.class));
        }).addOnFailureListener(e -> {
            android.util.Log.e("RecipeActivity", "Write failed", e);
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            saveButton.setEnabled(true);
        });
    }

        //call recipe validation to validate recipes
        //create recipe object from recipe form
        //then call writeRecipe to write to the databse

    public boolean validateInputs(String title, String desc, String category, String prepTimeText,
                                  String prepTimeUnit, String cookTimeText, String cookTimeUnit,
                                  String servingSizeText, String difficultyText) {
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
        if (category == null || category.equalsIgnoreCase("Select category")) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            recipeCategory.requestFocus();
            return false;
        }

        int prepTimeValue;
        try {
            prepTimeValue = Integer.parseInt(prepTimeText);
        } catch (NumberFormatException e) {
            recipePrepTime.setError("Enter a whole number (0+)");
            recipePrepTime.requestFocus();
            return false;
        }
        if (prepTimeValue < 0) {
            recipePrepTime.setError("Prep time cannot be negative");
            recipePrepTime.requestFocus();
            return false;
        }

        int cookTimeValue;
        try {
            cookTimeValue = Integer.parseInt(cookTimeText);
        } catch (NumberFormatException e) {
            recipeCookTime.setError("Enter a whole number (0+)");
            recipeCookTime.requestFocus();
            return false;
        }
        if (cookTimeValue < 0) {
            recipeCookTime.setError("Cook time cannot be negative");
            recipeCookTime.requestFocus();
            return false;
        }

        int servingSize;
        try {
            servingSize = Integer.parseInt(servingSizeText);
        } catch (NumberFormatException e) {
            recipeServingSize.setError("Enter a whole number (1+)");
            recipeServingSize.requestFocus();
            return false;
        }
        if (servingSize < 1) {
            recipeServingSize.setError("Serving size must be at least 1 person");
            recipeServingSize.requestFocus();
            return false;
        }

        if (difficultyText == null || difficultyText.equalsIgnoreCase("Select difficulty")) {
            Toast.makeText(this, "Please select a difficulty level", Toast.LENGTH_SHORT).show();
            recipeDifficultyLevel.requestFocus();
            return false;
        }

        if (prepTimeUnit == null || prepTimeUnit.isEmpty()) {
            Toast.makeText(this, "Please select prep time units", Toast.LENGTH_SHORT).show();
            recipePrepTimeUnits.requestFocus();
            return false;
        }
        if (cookTimeUnit == null || cookTimeUnit.isEmpty()) {
            Toast.makeText(this, "Please select cook time units", Toast.LENGTH_SHORT).show();
            recipeCookTimeUnits.requestFocus();
            return false;
        }

        return true;
    }

    public void goBack(View view){
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }
}