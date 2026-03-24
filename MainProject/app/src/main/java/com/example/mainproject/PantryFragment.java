package com.example.mainproject;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.app.DatePickerDialog;
import android.text.InputType;
import android.text.InputFilter;

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

public class PantryFragment extends Fragment {

    private String uid;
    private HashSet<String> ingredientNames = new HashSet<>();

    FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
    );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        // Get current users ID
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        View view = inflater.inflate(R.layout.fragment_pantry, container, false);

        Button btnAdd = view.findViewById(R.id.btnAddIngredient);
        ListView listView = view.findViewById(R.id.listViewIngredients);
        
        ArrayList<String> ingredientList = new ArrayList<>();
        ArrayList<String> fbPantryKeyList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, ingredientList);
        listView.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> showAddIngredientDialog());

        listView.setOnItemClickListener((parent, v, position, id) -> {
            String key = fbPantryKeyList.get(position);
            showEditIngredientDialog(key);
        });

        DatabaseReference pantryRef =  database.getReference("users").child(uid).child("pantry");
        pantryRef.addValueEventListener(new ValueEventListener() {

            // Clears both lists to avoid duplicates and then rebuilds
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

    private void showAddIngredientDialog() {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText nameInput = new EditText(requireContext());
        nameInput.setHint("Ingredient name");
        nameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});
        layout.addView(nameInput);

        EditText quantityInput = new EditText(requireContext());
        quantityInput.setHint("Quantity");
        layout.addView(quantityInput);
        quantityInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);


        String[] units = {"grams", "kg", "lbs", "oz", "cups", "tbsp", "tsp", "ml", "liters"};
        Spinner unitSpinner = new Spinner(requireContext());
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, units);
        unitSpinner.setAdapter(unitAdapter);
        layout.addView(unitSpinner);

        Button expirationButton = new Button(requireContext());
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

        layout.addView(expirationButton);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Add Ingredient")
                .setView(layout)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String quantity = quantityInput.getText().toString().trim();
            String unit = unitSpinner.getSelectedItem().toString();
            String expiration = expirationButton.getText().toString();

            if (name.isEmpty()) {
                nameInput.setError("Required");
                return;
            }

            if (quantity.isEmpty()) {
                quantityInput.setError("Required");
                return;
            }
            if (Double.parseDouble(quantity) <= 0) {
                quantityInput.setError("Must be positive");
                return;
            }
            if (ingredientNames.contains(name.toLowerCase())) {
                nameInput.setError("Already exists");
                return;
            }


            DatabaseReference pantryRef = database.getReference("users").child(uid).child("pantry");
            DatabaseReference newItem = pantryRef.push();
            newItem.child("name").setValue(name);
            newItem.child("quantity").setValue(quantity);
            newItem.child("unit").setValue(unit);
            newItem.child("expiration").setValue(expiration);
            dialog.dismiss();
        });
    }

    private void showEditIngredientDialog(String key) {
        DatabaseReference itemRef = database.getReference("users").child(uid).child("pantry").child(key);

        itemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String currentName = snapshot.child("name").getValue(String.class);
                String currentQuantity = snapshot.child("quantity").getValue(String.class);
                String currentUnit = snapshot.child("unit").getValue(String.class);
                String currentExpiration = snapshot.child("expiration").getValue(String.class);

                LinearLayout layout = new LinearLayout(requireContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(50, 40, 50, 10);

                EditText nameInput = new EditText(requireContext());
                nameInput.setText(currentName);
                nameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});
                layout.addView(nameInput);

                EditText quantityInput = new EditText(requireContext());
                quantityInput.setText(currentQuantity);
                layout.addView(quantityInput);
                quantityInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

                String[] units = {"grams", "kg", "lbs", "oz", "cups", "tbsp", "tsp", "ml", "liters"};
                Spinner unitSpinner = new Spinner(requireContext());
                ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_dropdown_item, units);
                unitSpinner.setAdapter(unitAdapter);
                // Pre-select the saved unit
                for (int i = 0; i < units.length; i++) {
                    if (units[i].equals(currentUnit)) {
                        unitSpinner.setSelection(i);
                        break;
                    }
                }
                layout.addView(unitSpinner);

                Button expirationButton = new Button(requireContext());
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

                layout.addView(expirationButton);

                AlertDialog dialog = new AlertDialog.Builder(requireContext())
                        .setTitle("Edit Ingredient")
                        .setView(layout)
                        .setPositiveButton("Save", null)
                        .setNeutralButton("Delete", (d, which) -> {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Delete Ingredient")
                                    .setMessage("Are you sure?")
                                    .setPositiveButton("Yes", (d2, w) -> itemRef.removeValue())
                                    .setNegativeButton("No", null)
                                    .show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String name = nameInput.getText().toString().trim();
                    String quantity = quantityInput.getText().toString().trim();
                    String unit = unitSpinner.getSelectedItem().toString();
                    String expiration = expirationButton.getText().toString();

                    if (name.isEmpty()) {
                        nameInput.setError("Required");
                        return;
                    }

                    if (quantity.isEmpty()) {
                        quantityInput.setError("Required");
                        return;
                    }
                    if (Double.parseDouble(quantity) <= 0) {
                        quantityInput.setError("Must be positive");
                        return;
                    }
                    if (!name.equalsIgnoreCase(currentName) && ingredientNames.contains(name.toLowerCase())) {
                        nameInput.setError("Already exists");
                        return;
                    }

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