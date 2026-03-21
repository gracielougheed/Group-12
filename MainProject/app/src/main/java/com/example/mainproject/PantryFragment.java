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

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class PantryFragment extends Fragment {

    private String uid;
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
                for (DataSnapshot item : snapshot.getChildren()) {
                    String name = item.child("name").getValue(String.class);
                    String quantity = item.child("quantity").getValue(String.class);
                    String unit = item.child("unit").getValue(String.class);
                    String expiration = item.child("expiration").getValue(String.class);
                    ingredientList.add(name + " - " + quantity + " " + unit + " (expires: " + expiration + ")");
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
        layout.addView(nameInput);

        EditText quantityInput = new EditText(requireContext());
        quantityInput.setHint("Quantity");
        layout.addView(quantityInput);

        EditText unitInput = new EditText(requireContext());
        unitInput.setHint("Unit (e.g. lbs, cups)");
        layout.addView(unitInput);

        EditText expirationInput = new EditText(requireContext());
        expirationInput.setHint("Expiration date");
        layout.addView(expirationInput);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Ingredient")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String quantity = quantityInput.getText().toString().trim();
                    String unit = unitInput.getText().toString().trim();
                    String expiration = expirationInput.getText().toString().trim();

                    if (name.isEmpty()) {
                        return;
                    }

                    DatabaseReference pantryRef =  database.getReference("users").child(uid).child("pantry");
                    DatabaseReference newItem = pantryRef.push();
                    newItem.child("name").setValue(name);
                    newItem.child("quantity").setValue(quantity);
                    newItem.child("unit").setValue(unit);
                    newItem.child("expiration").setValue(expiration);
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                layout.addView(nameInput);

                EditText quantityInput = new EditText(requireContext());
                quantityInput.setText(currentQuantity);
                layout.addView(quantityInput);

                EditText unitInput = new EditText(requireContext());
                unitInput.setText(currentUnit);
                layout.addView(unitInput);

                EditText expirationInput = new EditText(requireContext());
                expirationInput.setText(currentExpiration);
                layout.addView(expirationInput);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Edit Ingredient")
                        .setView(layout)
                        .setPositiveButton("Save", (dialog, which) -> {
                            String name = nameInput.getText().toString().trim();
                            String quantity = quantityInput.getText().toString().trim();
                            String unit = unitInput.getText().toString().trim();
                            String expiration = expirationInput.getText().toString().trim();

                            if (name.isEmpty()) {
                                return;
                            }

                            itemRef.child("name").setValue(name);
                            itemRef.child("quantity").setValue(quantity);
                            itemRef.child("unit").setValue(unit);
                            itemRef.child("expiration").setValue(expiration);
                        })
                        // Delete function
                        .setNeutralButton("Delete", (dialog, which) -> {
                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Delete Ingredient")
                                    .setMessage("Are you sure?")
                                    .setPositiveButton("Yes", (d, w) -> {
                                        itemRef.removeValue();
                                    })
                                    .setNegativeButton("No", null)
                                    .show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

}