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

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mainproject.HomeActivity;
import com.example.mainproject.R;
import com.example.mainproject.entities.Ingredient;
import com.example.mainproject.entities.Recipe;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class RecipeActivity extends AppCompatActivity {
    // Firebase
    FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
    );
    private FirebaseAuth myAuth;
    private DatabaseReference instructionsRef;

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
    private AutoCompleteTextView customTagsText;
    private ChipGroup tagChipGroup;
    private Switch recipeVisibilitySwitch;
    private TextView privateLabel;
    private TextView publicLabel;
    private Button btnAddInstruction;
    private ListView listViewInstructions;
    private ArrayList<String> instructionList = new ArrayList<>();
    private ArrayList<String> instructionKeyList = new ArrayList<>();
    private ArrayAdapter<String> instructionAdapter;
    private Button btnAddIngredient;
    private ListView listviewIngredients;
    private ArrayList<Ingredient> ingredientList = new ArrayList<>();
    private ArrayList<String> ingredientDisplayList = new ArrayList<>();
    private ArrayAdapter<String> ingredientAdapter;
    private DatabaseReference ingredientsRef;
    private AutoCompleteTextView cookwareText;
    private ChipGroup cookwareChipGroup;

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
        customTagsText = findViewById(R.id.customTagsText);
        tagChipGroup = findViewById(R.id.tagChipGroup);
        recipeVisibilitySwitch = findViewById(R.id.recipeVisibilitySwitch);
        privateLabel = findViewById(R.id.privateLabel);
        publicLabel = findViewById(R.id.publicLabel);
        saveButton = findViewById(R.id.addRecipeButton);

        recipePrepTime.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        recipeCookTime.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        recipeServingSize.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        recipePrepTime.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(4) });
        recipeCookTime.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(4) });
        recipeServingSize.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(3) });

        ArrayAdapter<String> tagAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.suggested_tags));
        customTagsText.setAdapter(tagAdapter);

        customTagsText.setOnItemClickListener((parent, view, position, id) -> {
            String tag = parent.getItemAtPosition(position).toString();
            addTagChip(tag);
            customTagsText.setText("");
        });

        customTagsText.setOnEditorActionListener((v, actionId, event) -> {
            String tag = customTagsText.getText().toString().trim();
            if(!tag.isEmpty()) {
                addTagChip(tag);
                customTagsText.setText("");
            }
            return true;
        });

        recipeVisibilitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                publicLabel.setTextColor(getResources().getColor(R.color.green));
                privateLabel.setTextColor(getResources().getColor(R.color.black));
            } else {
                privateLabel.setTextColor(getResources().getColor(R.color.green));
                publicLabel.setTextColor(getResources().getColor(R.color.black));
            }
        });

        // Set up Spinners
        setupCategorySpinner();
        setupTimeUnitSpinners();
        setupDifficultySpinner();

        // Instructions UI
        btnAddInstruction = findViewById(R.id.btnAddInstruction);
        listViewInstructions = findViewById(R.id.listViewInstructions);

        instructionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, instructionList);
        listViewInstructions.setAdapter(instructionAdapter);
        instructionsRef = null;
        btnAddInstruction.setOnClickListener(v -> showAddInstructionDialog());
        listViewInstructions.setOnItemClickListener((parent, view, position, id) -> {
            String key = instructionKeyList.get(position);
            showEditInstructionDialog(key);
        });

        //Ingredients UI
        btnAddIngredient = findViewById(R.id.btnAddIngredient);
        listviewIngredients = findViewById(R.id.listViewIngredients);

        ingredientAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, ingredientDisplayList);
        listviewIngredients.setAdapter(ingredientAdapter);

        btnAddIngredient.setOnClickListener(v -> showAddIngredientDialog());
        listviewIngredients.setOnItemClickListener((parent, view, position, id) -> {
            Ingredient ing = ingredientList.get(position);
            showEditIngredientDialog(ing);
        });

        //Cookware UI
        cookwareText = findViewById(R.id.cookwareText);
        cookwareChipGroup = findViewById(R.id.cookwareChipGroup);

        ArrayAdapter<String> cookwareAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.cookware_items));
        cookwareText.setAdapter(cookwareAdapter);

        cookwareText.setOnItemClickListener((parent, view, position, id) -> {
            String item = parent.getItemAtPosition(position).toString();
            addCookwareChip(item);
            cookwareText.setText("");
        });

        cookwareText.setOnEditorActionListener((v, actionId, event) -> {
            String item = cookwareText.getText().toString().trim();
            if(!item.isEmpty()) {
                addCookwareChip(item);
                cookwareText.setText("");
            }
            return true;
        });
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

    private void addTagChip(String tag) {
        Chip chip = new Chip(this);
        chip.setText(tag);
        chip.setCloseIconVisible(true);
        chip.setCheckable(false);
        chip.setClickable(false);

        chip.setOnCloseIconClickListener(v -> tagChipGroup.removeView(chip));

        tagChipGroup.addView(chip);
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

        List<String> tags = new ArrayList<>();
        for (int i = 0; i < tagChipGroup.getChildCount(); i++) {
            Chip chip =(Chip) tagChipGroup.getChildAt(i);
            tags.add(chip.getText().toString());
        }

        boolean isPublic = recipeVisibilitySwitch.isChecked();
        List<String> cookwareList = getCookwareFromChips();
        ArrayList<Ingredient> ingredientsList = new ArrayList<>(ingredientList);

        // Build recipe object
        Recipe recipe = new Recipe(recipeId, title, desc, category, prepTimeValue,
                prepTimeUnit, cookTimeValue, cookTimeUnit, servingSize, difficultyLevel,
                isPublic, tags, ingredientsList, cookwareList);

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

    private void showAddInstructionDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText instructionInput = new EditText(this);
        instructionInput.setHint("Enter instruction step");
        layout.addView(instructionInput);

        new AlertDialog.Builder(this)
                .setTitle("Add Instruction")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String text = instructionInput.getText().toString().trim();
                    if (text.isEmpty()) return;

                    int nextOrder = instructionList.size() + 1;

                    // LOCAL ONLY — no Firebase here
                    instructionList.add(nextOrder + ". " + text);
                    instructionKeyList.add(String.valueOf(nextOrder));

                    instructionAdapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void showEditInstructionDialog(String key) {
        DatabaseReference itemRef = instructionsRef.child(key);

        itemRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;

            String currentText = task.getResult().child("text").getValue(String.class);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 40, 50, 10);

            EditText instructionInput = new EditText(this);
            instructionInput.setText(currentText);
            layout.addView(instructionInput);

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Edit Instruction").setView(layout)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String newText = instructionInput.getText().toString().trim();
                        if (newText.isEmpty()) return;

                        itemRef.child("text").setValue(newText);
                    }).setNeutralButton("Delete", (dialog, which) -> {
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle("Delete Step").setMessage("Are you sure?")
                                .setPositiveButton("Yes", (d, w) -> {
                                    itemRef.removeValue().addOnCompleteListener(done -> {
                                        renumberInstructions();
                                    });
                                }).setNegativeButton("No", null).show();
                    }).setNegativeButton("Cancel", null).show();
        });
    }

    private void renumberInstructions() {
        instructionsRef.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) return;

            ArrayList<DataSnapshot> steps = new ArrayList<>();
            for (DataSnapshot step : task.getResult().getChildren()) {
                steps.add(step);
            }

            // Sort by current order
            steps.sort((a, b) -> {
                Long orderA = a.child("order").getValue(Long.class);
                Long orderB = b.child("order").getValue(Long.class);
                return Long.compare(orderA, orderB);
            });

            int newOrder = 1;
            for (DataSnapshot step : steps) {
                step.getRef().child("order").setValue(newOrder);
                newOrder++;
            }
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
            Chip chip = (Chip) cookwareChipGroup.getChildAt(i);
            list.add(chip.getText().toString());
        }
        return list;
    }

    private void showAddIngredientDialog() {
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
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);

        ArrayAdapter<CharSequence> prepAdapter = ArrayAdapter.createFromResource(
                this, R.array.preparation_styles, android.R.layout.simple_spinner_item);
        prepAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prepSpinner.setAdapter(prepAdapter);

        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                customUnitEdit.setVisibility(
                        "Other".equals(parent.getItemAtPosition(position)) ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        prepSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                customPrepEdit.setVisibility(
                        "Other".equals(parent.getItemAtPosition(position)) ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        builder.setView(dialogView)
                .setTitle("Add Ingredient")
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameEdit.getText().toString().trim();
                    String quantity = quantityEdit.getText().toString().trim();
                    String unit = unitSpinner.getSelectedItem().toString();
                    String prep = prepSpinner.getSelectedItem().toString();

                    if ("Other".equals(unit)) unit = customUnitEdit.getText().toString().trim();
                    if ("Other".equals(prep)) prep = customPrepEdit.getText().toString().trim();

                    if (name.isEmpty() || quantity.isEmpty()) {
                        Toast.makeText(this, "Name and quantity required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // LOCAL ONLY — no Firebase here
                    String id = String.valueOf(System.currentTimeMillis());
                    Ingredient ing = new Ingredient(id, name, quantity, unit, prep);

                    ingredientList.add(ing);

                    String display = quantity + " " + unit + " " + name;
                    if (!prep.isEmpty()) display += " (" + prep + ")";
                    ingredientDisplayList.add(display);

                    ingredientAdapter.notifyDataSetChanged();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void showEditIngredientDialog(Ingredient ing) {
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

        builder.setView(dialogView)
                .setTitle("Edit Ingredient")
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameEdit.getText().toString().trim();
                    String quantity = quantityEdit.getText().toString().trim();
                    String unit = unitSpinner.getSelectedItem().toString();
                    String prep = prepSpinner.getSelectedItem().toString();

                    if ("Other".equals(unit)) unit = customUnitEdit.getText().toString().trim();
                    if ("Other".equals(prep)) prep = customPrepEdit.getText().toString().trim();

                    ing.name = name;
                    ing.quantity = quantity;
                    ing.unit = unit;
                    ing.prep = prep;

                    ingredientsRef.child(ing.ingredientId).setValue(ing);
                })
                .setNegativeButton("Delete", (dialog, which) -> {
                    ingredientsRef.child(ing.ingredientId).removeValue();
                })
                .show();
    }


    public void goBack(View view){
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }
}