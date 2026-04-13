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
        recipePrepTime = findViewById(R.id.recipePrepTime);
        recipePrepTimeUnits = findViewById(R.id.recipePrepTimeUnits);
        recipeCookTime = findViewById(R.id.recipeCookTime);
        recipeCookTimeUnits = findViewById(R.id.recipeCookTimeUnits);
        recipeServingSize = findViewById(R.id.recipeServingSize);
        recipeDifficultyLevel = findViewById(R.id.recipeDifficultyLevel);
        customTagsText = findViewById(R.id.customTagsText);
        tagChipGroup = findViewById(R.id.tagChipGroup);
        recipeVisibilitySwitch = findViewById(R.id.recipeVisibilitySwitch);
        cookwareText = findViewById(R.id.cookwareText);
        cookwareChipGroup = findViewById(R.id.cookwareChipGroup);
        listViewInstructions = findViewById(R.id.listViewInstructions);
        listViewIngredients = findViewById(R.id.listViewIngredients);
        updateButton = findViewById(R.id.updateRecipeButton);

        updateButton.setOnClickListener(v -> updateRecipe());
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(this,
                R.array.categories, android.R.layout.simple_spinner_item);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recipeCategory.setAdapter(catAdapter);

        ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(this,
                R.array.time_units, android.R.layout.simple_spinner_item);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recipePrepTimeUnits.setAdapter(timeAdapter);
        recipeCookTimeUnits.setAdapter(timeAdapter);

        ArrayAdapter<CharSequence> diffAdapter = ArrayAdapter.createFromResource(
                this, R.array.difficulty_levels, android.R.layout.simple_spinner_item);
        recipeDifficultyLevel.setAdapter(diffAdapter);

        recipePrepTime.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        recipeCookTime.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        recipeServingSize.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        recipePrepTime.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(4) });
        recipeCookTime.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(4) });
        recipeServingSize.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(3) });
    }

    private void setupAdapters() {
        instructionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, new ArrayList<>());
        listViewInstructions.setAdapter(instructionAdapter);

        ingredientAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, ingredientDisplayList);
        listViewIngredients.setAdapter(ingredientAdapter);

        listViewInstructions.setOnItemClickListener((parent, view, position, id) -> {
            showEditInstructionDialog(position);
        });
        listViewIngredients.setOnItemClickListener((parent, view, position, id) -> {
            showEditIngredientDialog(position);
        });
    }

    private void setupTagInput() {
        ArrayAdapter<String> tagAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.suggested_tags));
        customTagsText.setAdapter(tagAdapter);

        customTagsText.setOnItemClickListener((parent, view, position, id) -> {
            addTagChip(parent.getItemAtPosition(position).toString());
            customTagsText.setText("");
        });

        customTagsText.setOnEditorActionListener((v, actionId, event) -> {
            String tag = customTagsText.getText().toString().trim();
            if (!tag.isEmpty()) {
                addTagChip(tag);
                customTagsText.setText("");
            }
            return true;
        });
    }

    private void setupCookwareInput() {
        ArrayAdapter<String> cookwareAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.cookware_items));
        cookwareText.setAdapter(cookwareAdapter);

        cookwareText.setOnItemClickListener((parent, view, position, id) -> {
            addCookwareChip(parent.getItemAtPosition(position).toString());
            cookwareText.setText("");
        });

        cookwareText.setOnEditorActionListener((v, actionId, event) -> {
            String item = cookwareText.getText().toString().trim();
            if (!item.isEmpty()) {
                addCookwareChip(item);
                cookwareText.setText("");
            }
            return true;
        });
    }

    private void loadRecipeFromFirebase() {
        String uid = myAuth.getCurrentUser().getUid();
        DatabaseReference ref = database.getReference("users")
                .child(uid).child("recipes").child(recipeId);

        ref.get().addOnSuccessListener(snapshot -> {
            originalRecipe = snapshot.getValue(Recipe.class);
            populateUI(originalRecipe);
        });
    }

    private void populateUI(Recipe r) {}

    private void refreshInstructionDisplay() {}

    private void showEditInstructionDialog(int index) {}

    private void showEditIngredientDialog(int index) {}

    private void addTagChip(String tag) {}

    private void addCookwareChip(String text) {}

    private List<String> getTagsFromChips() {}

    private List<String> getCookwareFromChips() {}

    private void updateRecipe() {}

}
