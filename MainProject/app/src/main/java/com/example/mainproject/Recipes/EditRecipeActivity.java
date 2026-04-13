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

    private void populateUI(Recipe r) {
        recipeTitle.setText(r.title);
        recipeDesc.setText(r.description);

        recipeCategory.setSelection(
                ((ArrayAdapter) recipeCategory.getAdapter()).getPosition(r.category));

        recipePrepTime.setText(String.valueOf(r.prepTimeValue));
        recipeCookTime.setText(String.valueOf(r.cookTimeValue));
        recipeServingSize.setText(String.valueOf(r.servingSize));

        recipePrepTimeUnits.setSelection(
                ((ArrayAdapter) recipePrepTimeUnits.getAdapter()).getPosition(r.prepTimeUnit));
        recipeCookTimeUnits.setSelection(
                ((ArrayAdapter) recipeDifficultyLevel.getAdapter())
                        .getPosition(String.valueOf(r.difficultyLevel)));

        recipeVisibilitySwitch.setChecked(r.isPublic);

        tagChipGroup.removeAllViews();
        if (r.tags != null) {
            for (String c : r.cookware) addCookwareChip(c);
        }

        instructionList.clear();
        if (r.instructions != null) instructionList.addAll(r.instructions);
        refreshInstructionDisplay();

        ingredientList.clear();
        ingredientDisplayList.clear();
        if (r.ingredients != null) {
            ingredientList.addAll(r.ingredients);
            for (Ingredient ing : ingredientList) {
                String display = ing.quantity + " " + ing.unit + " " + ing.name;
                if (ing.prep != null && !ing.prep.isEmpty()) display += " (" + ing.prep + ")";
                ingredientDisplayList.add(display);
            }
        }
        ingredientAdapter.notifyDataSetChanged();
    }

    private void refreshInstructionDisplay() {
        ArrayList<String> display = new ArrayList<>();
        for (int i = 0; i < instructionList.size(); i++) {
            display.add((i + 1) + ". " + instructionList.get(i));
        }
        instructionAdapter.clear();
        instructionAdapter.addAll(display);
        instructionAdapter.notifyDataSetChanged();
    }

    private void showEditInstructionDialog(int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Instruction");

        EditText input = new EditText(this);
        input.setText(instructionList.get(index));
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            instructionList.set(index, input.getText().toString().trim());
            refreshInstructionDisplay();
        });

        builder.setNeutralButton("Delete", (dialog, which) -> {
            instructionList.remove(index);
            refreshInstructionDisplay();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showEditIngredientDialog(int index) {
        Ingredient ing = ingredientList.get(index);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_ingredient, null);

        EditText nameEdit = dialogView.findViewById(R.id.editIngredientName);
        EditText quantityEdit = dialogView.findViewById(R.id.editIngredientQuantity);
        Spinner unitSpinner = dialogView.findViewById(R.id.spinnerIngredientUnit);
        EditText customUnitEdit = dialogView.findViewById(R.id.editCustomUnit);
        Spinner prepSpinner = dialogView.findViewById(R.id.spinnerPreparation);
        EditText customPrepEdit = dialogView.findViewById(R.id.editCustomPreparation);

        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(
                this, R.array.ingredient_units, android.R.layout.simple_spinner_item);
        unitSpinner.setAdapter(unitAdapter);

        ArrayAdapter<CharSequence> prepAdapter = ArrayAdapter.createFromResource(
                this, R.array.preparation_styles, android.R.layout.simple_spinner_item);
        prepSpinner.setAdapter(prepAdapter);

        nameEdit.setText(ing.name);
        quantityEdit.setText(ing.quantity);

        int unitPos = unitAdapter.getPosition(ing.unit);
        if (unitPos >= 0) {
            unitSpinner.setSelection(unitPos);
        }
        else {
            unitSpinner.setSelection(unitAdapter.getPosition("Other"));
            customUnitEdit.setVisibility(View.VISIBLE);
            customUnitEdit.setText(ing.unit);
        }

        int prepPos = prepAdapter.getPosition(ing.prep);
        if (prepPos >= 0) {
            prepSpinner.setSelection(prepPos);
        }
        else {
            prepSpinner.setSelection(prepAdapter.getPosition("Other"));
            customPrepEdit.setVisibility(View.VISIBLE);
            customPrepEdit.setText(ing.prep);
        }

        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            ing.name = nameEdit.getText().toString().trim();
            ing.quantity = quantityEdit.getText().toString().trim();

            String unit = unitSpinner.getSelectedItem().toString();
            if ("Other".equals(unit)) unit = customUnitEdit.getText().toString().trim();
            ing.unit = unit;

            String prep = prepSpinner.getSelectedItem().toString();
            if ("Other".equals(prep)) prep = customPrepEdit.getText().toString().trim();
            ing.prep = prep;

            ingredientDisplayList.set(index,
                    ing.quantity + " " + ing.unit + " " + ing.name +
                            (ing.prep.isEmpty() ? "" : " (" + ing.prep + ")"));

            ingredientAdapter.notifyDataSetChanged();
        });

        builder.setNeutralButton("Delete", (dialog, which) -> {
            ingredientList.remove(index);
            ingredientDisplayList.remove(index);
            ingredientAdapter.notifyDataSetChanged();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addTagChip(String tag) {
        Chip chip = new Chip(this);
        chip.setText(tag);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> tagChipGroup.removeView(chip));
        tagChipGroup.addView(chip);
    }

    private void addCookwareChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> cookwareChipGroup.removeView(chip));
        cookwareChipGroup.addView(chip);
    }

    private List<String> getTagsFromChips() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < tagChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) tagChipGroup.getChildAt(i);
            list.add(chip.getText().toString());
        }
        return list;
    }

    private List<String> getCookwareFromChips() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < cookwareChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) cookwareChipGroup.getChildAt(i);
            list.add(chip.getText().toString());
        }
        return list;
    }

    private void updateRecipe() {
        updateButton.setEnabled(false);

        String title = recipeTitle.getText().toString().trim();
        String desc = recipeDesc.getText().toString().trim();
        String category = (String) recipeCategory.getSelectedItem();
        String prepTimeText = recipePrepTime.getText().toString().trim();
        String cookTimeText = recipeCookTime.getText().toString().trim();
        String servingSizeText = recipeServingSize.getText().toString().trim();
        String prepTimeUnit = (String) recipePrepTimeUnits.getSelectedItem();
        String cookTimeUnit = (String) recipeCookTimeUnits.getSelectedItem();
        String difficultyText = (String) recipeDifficultyLevel.getSelectedItem();

        int prepTimeValue = Integer.parseInt(prepTimeText);
        int cookTimeValue = Integer.parseInt(cookTimeText);
        int servingSize = Integer.parseInt(servingSizeText);
        int difficultyLevel = Integer.parseInt(difficultyText);

        List<String> tags = getTagsFromChips();
        List<String> cookwareList = getCookwareFromChips();
        List<String> instructions = new ArrayList<>(instructionList);
        List<Ingredient> ingredients = new ArrayList<>(ingredientList);
        boolean isPublic = recipeVisibilitySwitch.isChecked();

        String uid = myAuth.getCurrentUser().getUid();
        DatabaseReference ref = database.getReference("users")
                .child(uid).child("recipes").child(recipeId);

        Map<String, Object> updates = new HashMap<>();

        if (!title.equals(originalRecipe.title)) updates.put("title", title);
        if (!desc.equals(originalRecipe.description)) updates.put("description", desc);
        if (!category.equals(originalRecipe.category)) updates.put("category", category);
        if (prepTimeValue != originalRecipe.prepTimeValue) updates.put("prepTimeValue", prepTimeValue);
        if (!prepTimeUnit.equals(originalRecipe.prepTimeUnit)) updates.put("prepTimeUnit", prepTimeUnit);
        if (cookTimeValue != originalRecipe.cookTimeValue) updates.put("cookTimeValue", cookTimeValue);
        if (!cookTimeUnit.equals(originalRecipe.cookTimeUnit)) updates.put("cookTimeUnit", cookTimeUnit);
        if (servingSize != originalRecipe.servingSize) updates.put("servingSize", servingSize);
        if (difficultyLevel != originalRecipe.difficultyLevel) updates.put("difficultyLevel", difficultyLevel);
        if (isPublic != originalRecipe.isPublic) updates.put("isPublic", isPublic);

        if (!tags.equals(originalRecipe.tags)) updates.put("tags", tags);
        if (!cookwareList.equals(originalRecipe.cookware)) updates.put("cookware", cookwareList);
        if (!instructions.equals(originalRecipe.instructions)) updates.put("instructions", instructions);
        if (!ingredients.equals(originalRecipe.ingredients)) updates.put("ingredients", ingredients);

        if (updates.isEmpty()) {
            finish();
            return;
        }

        ref.updateChildren(updates).addOnSuccessListener(unused -> finish());
    }

}
