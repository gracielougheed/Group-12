package com.example.mainproject.Recipes;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mainproject.HomeActivity;
import com.example.mainproject.R;
import com.example.mainproject.entities.Ingredient;
import com.example.mainproject.entities.Recipe;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * RecipeActivity — create a new recipe and save it to Firebase.
 */
public class RecipeActivity extends AppCompatActivity {

    private static final String DB_URL =
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/";

    // Firebase
    private FirebaseAuth myAuth;

    // Basic info
    private EditText recipeTitle, recipeDesc, recipePrepTime, recipeCookTime, recipeServingSize;
    private Spinner recipeCategory, recipePrepTimeUnits, recipeCookTimeUnits, recipeDifficultyLevel;

    // Tags
    private AutoCompleteTextView customTagsText;
    private ChipGroup tagChipGroup;

    // Visibility
    private Switch recipeVisibilitySwitch;
    private TextView privateLabel, publicLabel;

    // Instructions
    private Button btnAddInstruction;
    private ListView listViewInstructions;
    private ArrayList<String> instructionList = new ArrayList<>();
    private ArrayAdapter<String> instructionAdapter;

    // Ingredients
    private Button btnAddIngredient;
    private ListView listViewIngredients;
    private ArrayList<Ingredient> ingredientList = new ArrayList<>();
    private ArrayList<String> ingredientDisplayList = new ArrayList<>();
    private ArrayAdapter<String> ingredientAdapter;

    // Cookware
    private AutoCompleteTextView cookwareText;
    private ChipGroup cookwareChipGroup;

    // Save
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        myAuth = FirebaseAuth.getInstance();

        findViews();
        setupSpinners();
        setupTagInput();
        setupCookwareInput();
        setupInstructionList();
        setupIngredientList();
        setupVisibilitySwitch();
    }

    // -------------------------------------------------------------------------
    //  View binding
    // -------------------------------------------------------------------------

    private void findViews() {
        recipeTitle           = findViewById(R.id.recipeTitle);
        recipeDesc            = findViewById(R.id.recipeDesc);
        recipeCategory        = findViewById(R.id.recipeCategory);
        recipePrepTime        = findViewById(R.id.recipePrepTime);
        recipePrepTimeUnits   = findViewById(R.id.recipePrepTimeUnits);
        recipeCookTime        = findViewById(R.id.recipeCookTime);
        recipeCookTimeUnits   = findViewById(R.id.recipeCookTimeUnits);
        recipeServingSize     = findViewById(R.id.recipeServingSize);
        recipeDifficultyLevel = findViewById(R.id.recipeDifficultyLevel);
        customTagsText        = findViewById(R.id.customTagsText);
        tagChipGroup          = findViewById(R.id.tagChipGroup);
        recipeVisibilitySwitch = findViewById(R.id.recipeVisibilitySwitch);
        privateLabel          = findViewById(R.id.privateLabel);
        publicLabel           = findViewById(R.id.publicLabel);
        btnAddInstruction     = findViewById(R.id.btnAddInstruction);
        listViewInstructions  = findViewById(R.id.listViewInstructions);
        btnAddIngredient      = findViewById(R.id.btnAddIngredient);
        listViewIngredients   = findViewById(R.id.listViewIngredients);
        cookwareText          = findViewById(R.id.cookwareText);
        cookwareChipGroup     = findViewById(R.id.cookwareChipGroup);
        saveButton            = findViewById(R.id.addRecipeButton);

        // Numeric-only fields with length caps
        recipePrepTime.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        recipeCookTime.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        recipeServingSize.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        recipePrepTime.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(4) });
        recipeCookTime.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(4) });
        recipeServingSize.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(3) });

        saveButton.setOnClickListener(v -> saveRecipe());
    }

    // -------------------------------------------------------------------------
    //  Spinner setup
    // -------------------------------------------------------------------------

    private void setupSpinners() {
        ArrayAdapter<CharSequence> catAdapter = ArrayAdapter.createFromResource(
                this, R.array.categories, android.R.layout.simple_spinner_item);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recipeCategory.setAdapter(catAdapter);

        ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(
                this, R.array.time_units, android.R.layout.simple_spinner_item);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recipePrepTimeUnits.setAdapter(timeAdapter);
        recipeCookTimeUnits.setAdapter(timeAdapter);

        ArrayAdapter<CharSequence> diffAdapter = ArrayAdapter.createFromResource(
                this, R.array.difficulty_levels, android.R.layout.simple_spinner_item);
        diffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        recipeDifficultyLevel.setAdapter(diffAdapter);
    }

    // -------------------------------------------------------------------------
    //  Tag chips
    // -------------------------------------------------------------------------

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

    private void addTagChip(String tag) {
        Chip chip = new Chip(this);
        chip.setText(tag);
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setOnCloseIconClickListener(v -> tagChipGroup.removeView(chip));
        tagChipGroup.addView(chip);
    }

    // -------------------------------------------------------------------------
    //  Cookware chips
    // -------------------------------------------------------------------------

    private void setupCookwareInput() {
        ArrayAdapter<String> cwAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.cookware_items));
        cookwareText.setAdapter(cwAdapter);

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

    private void addCookwareChip(String text) {
        Chip chip = new Chip(this);
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setOnCloseIconClickListener(v -> cookwareChipGroup.removeView(chip));
        cookwareChipGroup.addView(chip);
    }

    private List<String> getCookwareFromChips() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < cookwareChipGroup.getChildCount(); i++) {
            list.add(((Chip) cookwareChipGroup.getChildAt(i)).getText().toString());
        }
        return list;
    }

    // -------------------------------------------------------------------------
    //  Instructions list
    // -------------------------------------------------------------------------

    private void setupInstructionList() {
        instructionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, new ArrayList<>());
        listViewInstructions.setAdapter(instructionAdapter);

        btnAddInstruction.setOnClickListener(v -> showAddInstructionDialog());

        listViewInstructions.setOnItemClickListener((parent, view, position, id) ->
                showEditInstructionDialog(position));
    }

    private void showAddInstructionDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText input = new EditText(this);
        input.setHint("Enter instruction step");
        layout.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Add Instruction")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        instructionList.add(text);
                        refreshInstructionDisplay();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditInstructionDialog(int index) {
        EditText input = new EditText(this);
        input.setText(instructionList.get(index));

        new AlertDialog.Builder(this)
                .setTitle("Edit Instruction")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) {
                        instructionList.set(index, text);
                        refreshInstructionDisplay();
                    }
                })
                .setNeutralButton("Delete", (dialog, which) -> {
                    instructionList.remove(index);
                    refreshInstructionDisplay();
                })
                .setNegativeButton("Cancel", null)
                .show();
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

    // -------------------------------------------------------------------------
    //  Ingredients list
    // -------------------------------------------------------------------------

    private void setupIngredientList() {
        ingredientAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, ingredientDisplayList);
        listViewIngredients.setAdapter(ingredientAdapter);

        btnAddIngredient.setOnClickListener(v -> showAddIngredientDialog());

        listViewIngredients.setOnItemClickListener((parent, view, position, id) ->
                showEditIngredientDialog(position));
    }

    private void showAddIngredientDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_ingredient, null);

        EditText nameEdit     = dialogView.findViewById(R.id.editIngredientName);
        EditText quantityEdit = dialogView.findViewById(R.id.editIngredientQuantity);
        Spinner unitSpinner   = dialogView.findViewById(R.id.spinnerIngredientUnit);
        EditText customUnit   = dialogView.findViewById(R.id.editCustomUnit);
        Spinner prepSpinner   = dialogView.findViewById(R.id.spinnerPreparation);
        EditText customPrep   = dialogView.findViewById(R.id.editCustomPreparation);

        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(
                this, R.array.ingredient_units, android.R.layout.simple_spinner_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);

        ArrayAdapter<CharSequence> prepAdapter = ArrayAdapter.createFromResource(
                this, R.array.preparation_styles, android.R.layout.simple_spinner_item);
        prepAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prepSpinner.setAdapter(prepAdapter);

        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                customUnit.setVisibility(
                        "Other".equals(p.getItemAtPosition(pos)) ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        prepSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                customPrep.setVisibility(
                        "Other".equals(p.getItemAtPosition(pos)) ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        builder.setView(dialogView)
                .setTitle("Add Ingredient")
                .setPositiveButton("Save", (dialog, which) -> {
                    String name     = nameEdit.getText().toString().trim();
                    String quantity = quantityEdit.getText().toString().trim();
                    String unit     = unitSpinner.getSelectedItem().toString();
                    String prep     = prepSpinner.getSelectedItem().toString();

                    if ("Other".equals(unit)) unit = customUnit.getText().toString().trim();
                    if ("Other".equals(prep)) prep = customPrep.getText().toString().trim();

                    if (name.isEmpty() || quantity.isEmpty()) {
                        Toast.makeText(this, "Name and quantity are required.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String id = String.valueOf(System.currentTimeMillis());
                    Ingredient ing = new Ingredient(id, name, quantity, unit, prep);
                    ingredientList.add(ing);

                    String display = quantity + " " + unit + " " + name;
                    if (!prep.isEmpty()) display += " (" + prep + ")";
                    ingredientDisplayList.add(display);
                    ingredientAdapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditIngredientDialog(int index) {
        Ingredient ing = ingredientList.get(index);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_ingredient, null);

        EditText nameEdit     = dialogView.findViewById(R.id.editIngredientName);
        EditText quantityEdit = dialogView.findViewById(R.id.editIngredientQuantity);
        Spinner unitSpinner   = dialogView.findViewById(R.id.spinnerIngredientUnit);
        EditText customUnit   = dialogView.findViewById(R.id.editCustomUnit);
        Spinner prepSpinner   = dialogView.findViewById(R.id.spinnerPreparation);
        EditText customPrep   = dialogView.findViewById(R.id.editCustomPreparation);

        ArrayAdapter<CharSequence> unitAdapter = ArrayAdapter.createFromResource(
                this, R.array.ingredient_units, android.R.layout.simple_spinner_item);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);

        ArrayAdapter<CharSequence> prepAdapter = ArrayAdapter.createFromResource(
                this, R.array.preparation_styles, android.R.layout.simple_spinner_item);
        prepAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prepSpinner.setAdapter(prepAdapter);

        nameEdit.setText(ing.name);
        quantityEdit.setText(ing.quantity);

        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                customUnit.setVisibility(
                        "Other".equals(p.getItemAtPosition(pos)) ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        prepSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                customPrep.setVisibility(
                        "Other".equals(p.getItemAtPosition(pos)) ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        int unitPos = unitAdapter.getPosition(ing.unit);
        if (unitPos >= 0) {
            unitSpinner.setSelection(unitPos);
        } else {
            int otherPos = unitAdapter.getPosition("Other");
            if (otherPos >= 0) unitSpinner.setSelection(otherPos);
            customUnit.setVisibility(View.VISIBLE);
            customUnit.setText(ing.unit);
        }

        int prepPos = prepAdapter.getPosition(ing.prep);
        if (prepPos >= 0) {
            prepSpinner.setSelection(prepPos);
        } else {
            int otherPos = prepAdapter.getPosition("Other");
            if (otherPos >= 0) prepSpinner.setSelection(otherPos);
            customPrep.setVisibility(View.VISIBLE);
            customPrep.setText(ing.prep);
        }

        builder.setView(dialogView)
                .setTitle("Edit Ingredient")
                .setPositiveButton("Save", (dialog, which) -> {
                    ing.name     = nameEdit.getText().toString().trim();
                    ing.quantity = quantityEdit.getText().toString().trim();
                    String unit  = unitSpinner.getSelectedItem().toString();
                    String prep  = prepSpinner.getSelectedItem().toString();
                    if ("Other".equals(unit)) unit = customUnit.getText().toString().trim();
                    if ("Other".equals(prep)) prep = customPrep.getText().toString().trim();
                    ing.unit = unit;
                    ing.prep = prep;

                    String display = ing.quantity + " " + ing.unit + " " + ing.name;
                    if (!ing.prep.isEmpty()) display += " (" + ing.prep + ")";
                    ingredientDisplayList.set(index, display);
                    ingredientAdapter.notifyDataSetChanged();
                })
                .setNeutralButton("Delete", (dialog, which) -> {
                    ingredientList.remove(index);
                    ingredientDisplayList.remove(index);
                    ingredientAdapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // -------------------------------------------------------------------------
    //  Visibility switch
    // -------------------------------------------------------------------------

    private void setupVisibilitySwitch() {
        // Default: private (unchecked)
        privateLabel.setTextColor(getResources().getColor(R.color.green, null));
        publicLabel.setTextColor(getResources().getColor(android.R.color.black, null));

        recipeVisibilitySwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                publicLabel.setTextColor(getResources().getColor(R.color.green, null));
                privateLabel.setTextColor(getResources().getColor(android.R.color.black, null));
            } else {
                privateLabel.setTextColor(getResources().getColor(R.color.green, null));
                publicLabel.setTextColor(getResources().getColor(android.R.color.black, null));
            }
        });
    }

    // -------------------------------------------------------------------------
    //  Save recipe
    // -------------------------------------------------------------------------

    private void saveRecipe() {
        saveButton.setEnabled(false);

        String title          = recipeTitle.getText().toString().trim();
        String desc           = recipeDesc.getText().toString().trim();
        String category       = recipeCategory.getSelectedItem() != null
                ? recipeCategory.getSelectedItem().toString() : "";
        String prepTimeText   = recipePrepTime.getText().toString().trim();
        String cookTimeText   = recipeCookTime.getText().toString().trim();
        String servingSizeText = recipeServingSize.getText().toString().trim();
        String prepTimeUnit   = recipePrepTimeUnits.getSelectedItem() != null
                ? recipePrepTimeUnits.getSelectedItem().toString() : "";
        String cookTimeUnit   = recipeCookTimeUnits.getSelectedItem() != null
                ? recipeCookTimeUnits.getSelectedItem().toString() : "";
        String difficultyText = recipeDifficultyLevel.getSelectedItem() != null
                ? recipeDifficultyLevel.getSelectedItem().toString() : "";

        if (!validateInputs(title, desc, category, prepTimeText, prepTimeUnit,
                cookTimeText, cookTimeUnit, servingSizeText, difficultyText)) {
            saveButton.setEnabled(true);
            return;
        }

        int prepTimeValue = Integer.parseInt(prepTimeText);
        int cookTimeValue = Integer.parseInt(cookTimeText);
        int servingSize   = Integer.parseInt(servingSizeText);
        int difficultyLevel;
        try {
            difficultyLevel = Integer.parseInt(difficultyText);
        } catch (NumberFormatException e) {
            // If difficulty_levels entries are labels (e.g. "Easy"), store position instead
            difficultyLevel = recipeDifficultyLevel.getSelectedItemPosition();
        }

        if (myAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not signed in.", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            return;
        }

        String uid = myAuth.getCurrentUser().getUid();
        DatabaseReference recipesRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("users").child(uid).child("recipes");

        String recipeId = recipesRef.push().getKey();
        if (recipeId == null) {
            Toast.makeText(this, "Could not generate recipe ID.", Toast.LENGTH_SHORT).show();
            saveButton.setEnabled(true);
            return;
        }

        List<String> tags = new ArrayList<>();
        for (int i = 0; i < tagChipGroup.getChildCount(); i++) {
            tags.add(((Chip) tagChipGroup.getChildAt(i)).getText().toString());
        }

        boolean isPublic = recipeVisibilitySwitch.isChecked();
        List<String> cookwareList  = getCookwareFromChips();
        List<Ingredient> ingList   = new ArrayList<>(ingredientList);
        List<String> instrList     = new ArrayList<>(instructionList);

        Recipe recipe = new Recipe(recipeId, title, desc, category,
                prepTimeValue, prepTimeUnit, cookTimeValue, cookTimeUnit,
                servingSize, difficultyLevel, isPublic, tags, ingList, cookwareList, instrList);

        recipesRef.child(recipeId).setValue(recipe)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Recipe saved!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Save failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    saveButton.setEnabled(true);
                });
    }

    // -------------------------------------------------------------------------
    //  Validation
    // -------------------------------------------------------------------------

    private boolean validateInputs(String title, String desc, String category,
                                   String prepTimeText, String prepTimeUnit,
                                   String cookTimeText, String cookTimeUnit,
                                   String servingSizeText, String difficultyText) {
        if (title.isEmpty()) {
            recipeTitle.setError("Title is required.");
            recipeTitle.requestFocus();
            return false;
        }
        if (desc.isEmpty()) {
            recipeDesc.setError("Description is required.");
            recipeDesc.requestFocus();
            return false;
        }
        if (category.isEmpty() || category.equalsIgnoreCase("Select category")) {
            Toast.makeText(this, "Please select a category.", Toast.LENGTH_SHORT).show();
            return false;
        }

        int prepVal;
        try { prepVal = Integer.parseInt(prepTimeText); }
        catch (NumberFormatException e) {
            recipePrepTime.setError("Enter a whole number (0+).");
            recipePrepTime.requestFocus();
            return false;
        }
        if (prepVal < 0) {
            recipePrepTime.setError("Prep time cannot be negative.");
            recipePrepTime.requestFocus();
            return false;
        }

        int cookVal;
        try { cookVal = Integer.parseInt(cookTimeText); }
        catch (NumberFormatException e) {
            recipeCookTime.setError("Enter a whole number (0+).");
            recipeCookTime.requestFocus();
            return false;
        }
        if (cookVal < 0) {
            recipeCookTime.setError("Cook time cannot be negative.");
            recipeCookTime.requestFocus();
            return false;
        }

        int sizeVal;
        try { sizeVal = Integer.parseInt(servingSizeText); }
        catch (NumberFormatException e) {
            recipeServingSize.setError("Enter a whole number (1+).");
            recipeServingSize.requestFocus();
            return false;
        }
        if (sizeVal < 1) {
            recipeServingSize.setError("Serving size must be at least 1.");
            recipeServingSize.requestFocus();
            return false;
        }

        if (difficultyText.isEmpty() || difficultyText.equalsIgnoreCase("Select difficulty")) {
            Toast.makeText(this, "Please select a difficulty level.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (prepTimeUnit.isEmpty()) {
            Toast.makeText(this, "Please select prep time units.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (cookTimeUnit.isEmpty()) {
            Toast.makeText(this, "Please select cook time units.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // -------------------------------------------------------------------------
    //  Navigation
    // -------------------------------------------------------------------------

    public void goBack(View view) {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}