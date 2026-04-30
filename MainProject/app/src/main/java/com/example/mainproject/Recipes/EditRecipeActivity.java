package com.example.mainproject.Recipes;

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

import com.example.mainproject.R;
import com.example.mainproject.entities.Ingredient;
import com.example.mainproject.entities.Recipe;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditRecipeActivity extends AppCompatActivity {

    // Firebase
    private final FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/");
    private FirebaseAuth myAuth;

    // UI references
    private EditText recipeTitle, recipeDesc, recipePrepTime, recipeCookTime, recipeServingSize;
    private Spinner recipeCategory, recipePrepTimeUnits, recipeCookTimeUnits, recipeDifficultyLevel;
    private AutoCompleteTextView customTagsText, cookwareText;
    private ChipGroup tagChipGroup, cookwareChipGroup;
    private Switch recipeVisibilitySwitch;
    private TextView privateLabel, publicLabel;
    private Button btnAddInstruction, btnAddIngredient, updateButton;
    private ListView listViewInstructions, listViewIngredients;

    // Data
    private final ArrayList<String> instructionList = new ArrayList<>();
    private ArrayAdapter<String> instructionAdapter;
    private final ArrayList<Ingredient> ingredientList = new ArrayList<>();
    private final ArrayList<String> ingredientDisplayList = new ArrayList<>();
    private ArrayAdapter<String> ingredientAdapter;

    private String recipeId;
    private Recipe originalRecipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recipe);

        myAuth = FirebaseAuth.getInstance();
        recipeId = getIntent().getStringExtra("RECIPE_ID");

        if (recipeId == null) {
            Toast.makeText(this, "No recipe ID provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        findViews();
        setupSpinners();
        setupTagInput();
        setupCookwareInput();
        setupInstructionList();
        setupIngredientList();
        setupVisibilitySwitch();
        loadRecipeFromFirebase();
    }

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
        updateButton          = findViewById(R.id.updateRecipeButton);

        updateButton.setOnClickListener(v -> updateRecipe());
    }

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

        recipePrepTime.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        recipeCookTime.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        recipeServingSize.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        recipePrepTime.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        recipeCookTime.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
        recipeServingSize.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)});
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

    private void setupInstructionList() {
        instructionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, new ArrayList<>());
        listViewInstructions.setAdapter(instructionAdapter);

        btnAddInstruction.setOnClickListener(v -> showAddInstructionDialog());

        listViewInstructions.setOnItemClickListener((parent, view, position, id) ->
                showEditInstructionDialog(position));
    }

    private void setupIngredientList() {
        ingredientAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, ingredientDisplayList);
        listViewIngredients.setAdapter(ingredientAdapter);

        btnAddIngredient.setOnClickListener(v -> showAddIngredientDialog());

        listViewIngredients.setOnItemClickListener((parent, view, position, id) ->
                showEditIngredientDialog(position));
    }

    private void setupVisibilitySwitch() {
        recipeVisibilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                publicLabel.setTextColor(getResources().getColor(R.color.green, getTheme()));
                privateLabel.setTextColor(getResources().getColor(android.R.color.black, getTheme()));
            } else {
                privateLabel.setTextColor(getResources().getColor(R.color.green, getTheme()));
                publicLabel.setTextColor(getResources().getColor(android.R.color.black, getTheme()));
            }
        });
    }

    // ──────────────── Firebase load ────────────────

    private void loadRecipeFromFirebase() {
        String uid = myAuth.getCurrentUser() != null ? myAuth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DatabaseReference ref = database.getReference("users")
                .child(uid).child("recipes").child(recipeId);

        ref.get().addOnSuccessListener(snapshot -> {
            originalRecipe = snapshot.getValue(Recipe.class);
            if (originalRecipe == null) {
                Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            populateUI(originalRecipe);
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load recipe: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void populateUI(Recipe r) {
        recipeTitle.setText(r.title);
        recipeDesc.setText(r.description);

        // Set category spinner
        ArrayAdapter catAdapter = (ArrayAdapter) recipeCategory.getAdapter();
        int catPos = catAdapter.getPosition(r.category);
        if (catPos >= 0) recipeCategory.setSelection(catPos);

        // Set prep time
        recipePrepTime.setText(String.valueOf(r.prepTimeValue));
        ArrayAdapter prepUnitAdapter = (ArrayAdapter) recipePrepTimeUnits.getAdapter();
        int prepUnitPos = prepUnitAdapter.getPosition(r.prepTimeUnit);
        if (prepUnitPos >= 0) recipePrepTimeUnits.setSelection(prepUnitPos);

        // Set cook time
        recipeCookTime.setText(String.valueOf(r.cookTimeValue));
        ArrayAdapter cookUnitAdapter = (ArrayAdapter) recipeCookTimeUnits.getAdapter();
        int cookUnitPos = cookUnitAdapter.getPosition(r.cookTimeUnit);
        if (cookUnitPos >= 0) recipeCookTimeUnits.setSelection(cookUnitPos);

        // Set serving size
        recipeServingSize.setText(String.valueOf(r.servingSize));

        // Set difficulty (stored as int, spinner holds strings "1"-"5")
        ArrayAdapter diffAdapter = (ArrayAdapter) recipeDifficultyLevel.getAdapter();
        int diffPos = diffAdapter.getPosition(String.valueOf(r.difficultyLevel));
        if (diffPos >= 0) recipeDifficultyLevel.setSelection(diffPos);

        // Set visibility switch
        recipeVisibilitySwitch.setChecked(r.isPublic);

        // Populate tags
        tagChipGroup.removeAllViews();
        if (r.tags != null) {
            for (String tag : r.tags) addTagChip(tag);
        }

        // Populate cookware
        cookwareChipGroup.removeAllViews();
        if (r.cookware != null) {
            for (String item : r.cookware) addCookwareChip(item);
        }

        // Populate instructions
        instructionList.clear();
        if (r.instructions != null) instructionList.addAll(r.instructions);
        refreshInstructionDisplay();

        // Populate ingredients
        ingredientList.clear();
        ingredientDisplayList.clear();
        if (r.ingredients != null) {
            for (Ingredient ing : r.ingredients) {
                ingredientList.add(ing);
                String display = ing.quantity + " " + ing.unit + " " + ing.name;
                if (ing.prep != null && !ing.prep.isEmpty()) display += " (" + ing.prep + ")";
                ingredientDisplayList.add(display);
            }
        }
        ingredientAdapter.notifyDataSetChanged();
    }

    // ──────────────── Firebase update ────────────────

    private void updateRecipe() {
        updateButton.setEnabled(false);

        String title           = recipeTitle.getText().toString().trim();
        String desc            = recipeDesc.getText().toString().trim();
        String category        = (String) recipeCategory.getSelectedItem();
        String prepTimeText    = recipePrepTime.getText().toString().trim();
        String cookTimeText    = recipeCookTime.getText().toString().trim();
        String servingSizeText = recipeServingSize.getText().toString().trim();
        String prepTimeUnit    = (String) recipePrepTimeUnits.getSelectedItem();
        String cookTimeUnit    = (String) recipeCookTimeUnits.getSelectedItem();
        String difficultyText  = (String) recipeDifficultyLevel.getSelectedItem();

        // Basic validation
        if (title.isEmpty()) {
            recipeTitle.setError("Title is required"); recipeTitle.requestFocus();
            updateButton.setEnabled(true); return;
        }
        if (desc.isEmpty()) {
            recipeDesc.setError("Description is required"); recipeDesc.requestFocus();
            updateButton.setEnabled(true); return;
        }

        int prepTimeValue, cookTimeValue, servingSize, difficultyLevel;
        try {
            prepTimeValue = Integer.parseInt(prepTimeText);
        } catch (NumberFormatException e) {
            recipePrepTime.setError("Enter a whole number"); recipePrepTime.requestFocus();
            updateButton.setEnabled(true); return;
        }
        try {
            cookTimeValue = Integer.parseInt(cookTimeText);
        } catch (NumberFormatException e) {
            recipeCookTime.setError("Enter a whole number"); recipeCookTime.requestFocus();
            updateButton.setEnabled(true); return;
        }
        try {
            servingSize = Integer.parseInt(servingSizeText);
            if (servingSize < 1) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            recipeServingSize.setError("Must be at least 1"); recipeServingSize.requestFocus();
            updateButton.setEnabled(true); return;
        }
        try {
            difficultyLevel = Integer.parseInt(difficultyText);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please select a difficulty", Toast.LENGTH_SHORT).show();
            updateButton.setEnabled(true); return;
        }

        List<String> tags         = getTagsFromChips();
        List<String> cookwareList = getCookwareFromChips();
        List<String> instructions = new ArrayList<>(instructionList);
        List<Ingredient> ingredients = new ArrayList<>(ingredientList);
        boolean isPublic = recipeVisibilitySwitch.isChecked();

        String uid = myAuth.getCurrentUser().getUid();
        DatabaseReference ref = database.getReference("users")
                .child(uid).child("recipes").child(recipeId);

        // Build full updated recipe object (write all fields, not just diffs)
        // This is simpler, safer, and avoids partial-update bugs
        Map<String, Object> updates = new HashMap<>();
        updates.put("recipeId", recipeId);
        updates.put("title", title);
        updates.put("description", desc);
        updates.put("category", category);
        updates.put("prepTimeValue", prepTimeValue);
        updates.put("prepTimeUnit", prepTimeUnit);
        updates.put("cookTimeValue", cookTimeValue);
        updates.put("cookTimeUnit", cookTimeUnit);
        updates.put("servingSize", servingSize);
        updates.put("difficultyLevel", difficultyLevel);
        updates.put("isPublic", isPublic);
        updates.put("tags", tags);
        updates.put("cookware", cookwareList);
        updates.put("instructions", instructions);
        updates.put("ingredients", ingredients);

        ref.updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Recipe updated!", Toast.LENGTH_SHORT).show();
                    finish(); // Returns to ViewRecipeActivity which will reload from Firebase
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    updateButton.setEnabled(true);
                });
    }

    // ──────────────── Instruction helpers ────────────────

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
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText input = new EditText(this);
        input.setText(instructionList.get(index));
        layout.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Edit Instruction")
                .setView(layout)
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

    // ──────────────── Ingredient helpers ────────────────

    private void showAddIngredientDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_ingredient, null);

        EditText nameEdit       = dialogView.findViewById(R.id.editIngredientName);
        EditText quantityEdit   = dialogView.findViewById(R.id.editIngredientQuantity);
        Spinner unitSpinner     = dialogView.findViewById(R.id.spinnerIngredientUnit);
        EditText customUnitEdit = dialogView.findViewById(R.id.editCustomUnit);
        Spinner prepSpinner     = dialogView.findViewById(R.id.spinnerPreparation);
        EditText customPrepEdit = dialogView.findViewById(R.id.editCustomPreparation);

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
                customUnitEdit.setVisibility(
                        "Other".equals(p.getItemAtPosition(pos)) ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        prepSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                customPrepEdit.setVisibility(
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

                    if ("Other".equals(unit)) unit = customUnitEdit.getText().toString().trim();
                    if ("Other".equals(prep)) prep = customPrepEdit.getText().toString().trim();

                    if (name.isEmpty() || quantity.isEmpty()) {
                        Toast.makeText(this, "Name and quantity are required", Toast.LENGTH_SHORT).show();
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

        EditText nameEdit       = dialogView.findViewById(R.id.editIngredientName);
        EditText quantityEdit   = dialogView.findViewById(R.id.editIngredientQuantity);
        Spinner unitSpinner     = dialogView.findViewById(R.id.spinnerIngredientUnit);
        EditText customUnitEdit = dialogView.findViewById(R.id.editCustomUnit);
        Spinner prepSpinner     = dialogView.findViewById(R.id.spinnerPreparation);
        EditText customPrepEdit = dialogView.findViewById(R.id.editCustomPreparation);

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

        int unitPos = unitAdapter.getPosition(ing.unit);
        if (unitPos >= 0) {
            unitSpinner.setSelection(unitPos);
        } else {
            unitSpinner.setSelection(unitAdapter.getPosition("Other"));
            customUnitEdit.setVisibility(View.VISIBLE);
            customUnitEdit.setText(ing.unit);
        }

        int prepPos = prepAdapter.getPosition(ing.prep);
        if (prepPos >= 0) {
            prepSpinner.setSelection(prepPos);
        } else {
            prepSpinner.setSelection(prepAdapter.getPosition("Other"));
            customPrepEdit.setVisibility(View.VISIBLE);
            customPrepEdit.setText(ing.prep);
        }

        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                customUnitEdit.setVisibility(
                        "Other".equals(p.getItemAtPosition(pos)) ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        prepSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                customPrepEdit.setVisibility(
                        "Other".equals(p.getItemAtPosition(pos)) ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        builder.setView(dialogView)
                .setTitle("Edit Ingredient")
                .setPositiveButton("Save", (dialog, which) -> {
                    ing.name     = nameEdit.getText().toString().trim();
                    ing.quantity = quantityEdit.getText().toString().trim();

                    String unit = unitSpinner.getSelectedItem().toString();
                    if ("Other".equals(unit)) unit = customUnitEdit.getText().toString().trim();
                    ing.unit = unit;

                    String prep = prepSpinner.getSelectedItem().toString();
                    if ("Other".equals(prep)) prep = customPrepEdit.getText().toString().trim();
                    ing.prep = prep;

                    String display = ing.quantity + " " + ing.unit + " " + ing.name;
                    if (ing.prep != null && !ing.prep.isEmpty()) display += " (" + ing.prep + ")";
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

    // ──────────────── Chip helpers ────────────────

    private void addTagChip(String tag) {
        Chip chip = new Chip(this);
        chip.setText(tag);
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);
        chip.setClickable(false);
        chip.setOnCloseIconClickListener(v -> tagChipGroup.removeView(chip));
        tagChipGroup.addView(chip);
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

    private List<String> getTagsFromChips() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < tagChipGroup.getChildCount(); i++) {
            list.add(((Chip) tagChipGroup.getChildAt(i)).getText().toString());
        }
        return list;
    }

    private List<String> getCookwareFromChips() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < cookwareChipGroup.getChildCount(); i++) {
            list.add(((Chip) cookwareChipGroup.getChildAt(i)).getText().toString());
        }
        return list;
    }

    // ──────────────── Navigation ────────────────

    public void goBack(View view) {
        finish();
    }
}