package com.example.mainproject.Recipes;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mainproject.R;
import com.example.mainproject.entities.Ingredient;
import com.example.mainproject.entities.Recipe;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditRecipeActivity extends AppCompatActivity{

    //Firebase
    FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/");
    FirebaseAuth myAuth;

    // UI references
    private EditText recipeTitle, recipeDesc, recipePrepTime, recipeCookTime, recipeServingSize;
    private Spinner recipeCategory, recipePrepTimeUnits, recipeCookTimeUnits, recipeDifficultyLevel;
    private AutoCompleteTextView customTagsText, cookwareText;
    private ChipGroup tagChipGroup, cookwareChipGroup;
    private Switch recipeVisibilitySwitch;
    private ListView listViewInstructions, listViewIngredients;
    private Button updateButton;

    // Lists
    private ArrayList<String> instructionList = new ArrayList<>();
    private ArrayAdapter<String> instructionAdapter;

    private ArrayList<Ingredient> ingredientList = new ArrayList<>();
    private ArrayList<String> ingredientDisplayList = new ArrayList<>();
    private ArrayAdapter<String> ingredientAdapter;

    private String recipeId;
    private Recipe originalRecipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recipe);

        myAuth = FirebaseAuth.getInstance();
        recipeId = getIntent().getStringExtra("RECIPE_ID");

        findViews();
        setupSpinners();
        setupAdapters();
        setupTagInput();
        setupCookwareInput();

        loadRecipeFromFirebase();
    }

    private void findViews() {
        recipeTitle = findViewById(R.id.recipeTitle);
        recipeDesc = findViewById(R.id.recipeDesc);
        recipeCategory = findViewById(R.id.recipeCategory);
    }

    private void setupSpinners() {}

    private void setupAdapters() {}

    private void setupTagInput() {}

    private void setupCookwareInput() {}

    private void loadRecipeFromFirebase() {}

    private void populateUI(Recipe r) {}

    private void refreshInstructionDisplay() {}

    private void showEditInstructionDialog(int index) {}

    private void sowEditIngredientDialog(int index) {}

    private void addTagChip(String tag) {}

    private void addCookwareChip(String text) {}

    private List<String> getTagsFromChips() {}

    private List<String> getCookwareFromChips() {}

    private void updateRecipe() {}

}
