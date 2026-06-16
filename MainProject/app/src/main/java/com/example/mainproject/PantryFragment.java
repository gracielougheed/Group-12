package com.example.mainproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.app.DatePickerDialog;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;

/**
 * This fragment displays the user's pantry — a list of ingredients they have at home.
 * Users can add, edit, and delete ingredients. All data is saved to Firebase
 * so it stays available across sessions.
 */
public class PantryFragment extends Fragment {

    // The logged-in user's unique ID, used to access their personal pantry data
    private String uid;

    // Keeps track of ingredient names so we can prevent duplicates
    private HashSet<String> ingredientNames = new HashSet<>();

    // Connection to the Firebase database where pantry data is stored
    FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
    );

    /**
     * Called when the fragment's screen is being created.
     * Sets up the ingredient list, the "add" button, and starts
     * listening for any changes to the pantry in Firebase.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Get current users ID
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        View view = inflater.inflate(R.layout.fragment_pantry, container, false);

        Button btnAdd = view.findViewById(R.id.btnAddIngredient);
        ListView listView = view.findViewById(R.id.listViewIngredients);

        // Lists to hold what's displayed on screen and the matching Firebase keys
        ArrayList<String> ingredientList = new ArrayList<>();
        ArrayList<String> fbPantryKeyList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, ingredientList);
        listView.setAdapter(adapter);

        // Open the "add ingredient" popup when the + button is tapped
        btnAdd.setOnClickListener(v -> showAddIngredientDialog());

        // Open the "edit ingredient" popup when an item in the list is tapped
        listView.setOnItemClickListener((parent, v, position, id) -> {
            String key = fbPantryKeyList.get(position);
            showEditIngredientDialog(key);
        });

        // Listen for real-time updates from Firebase so the list always stays current
        DatabaseReference pantryRef = database.getReference("users").child(uid).child("pantry");
        pantryRef.addValueEventListener(new ValueEventListener() {

            // Clears both lists to avoid duplicates and then rebuilds from Firebase data
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ingredientList.clear();
                fbPantryKeyList.clear();
                ingredientNames.clear();
                for (DataSnapshot item : snapshot.getChildren()) {
                    String name = item.child("name").getValue(String.class);
                    String quantity = item.child("quantity").getValue(String.class);
                    String unit = item.child("unit").getValue(String.class);
                    String expiration = item.child("expiration").getValue(String.class);
                    ingredientList.add(name + " - " + quantity + " " + unit + " (expires: " + expiration + ")");
                    ingredientNames.add(name.toLowerCase());
                    fbPantryKeyList.add(item.getKey());
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
        return view;
    }

    /**
     * Shows a popup dialog that lets the user add a new ingredient to their pantry.
     * The user fills in the name, quantity, unit, and expiration date.
     * The ingredient is saved to Firebase after passing validation.
     */
    private void showAddIngredientDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialoge_pantry_item, null);
        EditText nameInput = dialogView.findViewById(R.id.input_name);
        EditText quantityInput = dialogView.findViewById(R.id.input_quantity);
        Spinner unitSpinner = dialogView.findViewById(R.id.spinner_unit);
        Button expirationButton = dialogView.findViewById(R.id.button_expiration);

        String[] units = {"grams", "kg", "lbs", "oz", "cups", "tbsp", "tsp", "ml", "liters"};
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, units);
        unitSpinner.setAdapter(unitAdapter);

        Calendar cal = Calendar.getInstance();
        String today = String.format("%02d-%02d-%02d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR) % 100);
        expirationButton.setText(today);
        expirationButton.setOnClickListener(view -> {
            DatePickerDialog dpd = new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
                String date = String.format("%02d-%02d-%02d", day, month + 1, year % 100);
                expirationButton.setText(date);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dpd.getDatePicker().setMinDate(System.currentTimeMillis());
            dpd.show();
        });

        // Show the dialog with save and cancel buttons
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Add Ingredient")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .show();

        // Custom save handler so we can validate before closing the dialog
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String quantity = quantityInput.getText().toString().trim();
            String unit = unitSpinner.getSelectedItem().toString();
            String expiration = expirationButton.getText().toString();

            // Validation: make sure all fields are filled in correctly
            if (name.isEmpty()) {
                nameInput.setError("Required");
                return;
            }

            if (quantity.isEmpty()) {
                quantityInput.setError("Required");
                return;
            }
            try {
                if (Double.parseDouble(quantity) <= 0) {
                    quantityInput.setError("Must be positive");
                    return;
                }
            } catch (NumberFormatException e) {
                quantityInput.setError("Must be a valid number");
                return;
            }
            if (ingredientNames.contains(name.toLowerCase())) {
                nameInput.setError("Already exists");
                return;
            }


            // Save the new ingredient to Firebase
            DatabaseReference pantryRef = database.getReference("users").child(uid).child("pantry");
            DatabaseReference newItem = pantryRef.push();
            newItem.child("name").setValue(name);
            newItem.child("quantity").setValue(quantity);
            newItem.child("unit").setValue(unit);
            newItem.child("expiration").setValue(expiration);
            dialog.dismiss();
        });
    }

    /**
     * Shows a popup dialog that lets the user edit or delete an existing ingredient.
     * The fields are pre-filled with the ingredient's current values.
     *
     * @param key the Firebase key that identifies this ingredient
     */
    private void showEditIngredientDialog(String key) {
        DatabaseReference itemRef = database.getReference("users").child(uid).child("pantry").child(key);

        // Fetch the ingredient's current data from Firebase
        itemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // Load the current values to pre-fill the form
                String currentName = snapshot.child("name").getValue(String.class);
                String currentQuantity = snapshot.child("quantity").getValue(String.class);
                String currentUnit = snapshot.child("unit").getValue(String.class);
                String currentExpiration = snapshot.child("expiration").getValue(String.class);

                View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialoge_pantry_item, null);
                EditText nameInput = dialogView.findViewById(R.id.input_name);
                EditText quantityInput = dialogView.findViewById(R.id.input_quantity);
                Spinner unitSpinner = dialogView.findViewById(R.id.spinner_unit);
                Button expirationButton = dialogView.findViewById(R.id.button_expiration);

                nameInput.setText(currentName);
                quantityInput.setText(currentQuantity);

                String[] units = {"grams", "kg", "lbs", "oz", "cups", "tbsp", "tsp", "ml", "liters"};
                ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_dropdown_item, units);
                unitSpinner.setAdapter(unitAdapter);
                for (int i = 0; i < units.length; i++) {
                    if (units[i].equals(currentUnit)) {
                        unitSpinner.setSelection(i);
                        break;
                    }
                }

                expirationButton.setText(currentExpiration);
                expirationButton.setOnClickListener(view -> {
                    Calendar cal = Calendar.getInstance();
                    DatePickerDialog dpd = new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
                        String date = String.format("%02d-%02d-%02d", day, month + 1, year % 100);
                        expirationButton.setText(date);
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
                    dpd.getDatePicker().setMinDate(System.currentTimeMillis());
                    dpd.show();
                });

                // Show the dialog with Save, Delete, and Cancel options
                AlertDialog dialog = new AlertDialog.Builder(requireContext())
                        .setTitle("Edit Ingredient")
                        .setView(dialogView)
                        .setPositiveButton("Save", null)
                        .setNeutralButton("Delete", (d, which) -> {
                            // Ask for confirmation before deleting
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Delete Ingredient")
                                    .setMessage("Are you sure?")
                                    .setPositiveButton("Yes", (d2, w) -> itemRef.removeValue())
                                    .setNegativeButton("No", null)
                                    .show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();

                // Custom save handler so we can validate before closing
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String name = nameInput.getText().toString().trim();
                    String quantity = quantityInput.getText().toString().trim();
                    String unit = unitSpinner.getSelectedItem().toString();
                    String expiration = expirationButton.getText().toString();

                    // Validation checks
                    if (name.isEmpty()) {
                        nameInput.setError("Required");
                        return;
                    }

                    if (quantity.isEmpty()) {
                        quantityInput.setError("Required");
                        return;
                    }
                    try {
                        if (Double.parseDouble(quantity) <= 0) {
                            quantityInput.setError("Must be positive");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        quantityInput.setError("Must be a valid number");
                        return;
                    }
                    // Only check for duplicates if the name was actually changed
                    if (!name.equalsIgnoreCase(currentName) && ingredientNames.contains(name.toLowerCase())) {
                        nameInput.setError("Already exists");
                        return;
                    }

                    // Update the ingredient in Firebase
                    itemRef.child("name").setValue(name);
                    itemRef.child("quantity").setValue(quantity);
                    itemRef.child("unit").setValue(unit);
                    itemRef.child("expiration").setValue(expiration);
                    dialog.dismiss();
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }
}