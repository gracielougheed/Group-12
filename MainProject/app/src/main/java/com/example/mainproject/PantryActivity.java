package com.example.mainproject;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.util.ArrayList;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class PantryActivity extends AppCompatActivity {

    FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Pantry");
        setContentView(R.layout.activity_pantry);

        Button btnAdd = findViewById(R.id.btnAddIngredient);

        btnAdd.setOnClickListener(v -> showAddIngredientDialog());


        // Find the list view from the layout
        ListView listView = findViewById(R.id.listViewIngredients);

        // List to hold ingredient strings for display
        ArrayList<String> ingredientList = new ArrayList<>();

        // Adapter connects the list data to the ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, ingredientList);
        listView.setAdapter(adapter);

        // Listen for changes in the "pantry" node in Firebase
        DatabaseReference pantryRef = database.getReference("pantry");
        pantryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // Clear the list and re-populate with fresh data
                ingredientList.clear();
                for (DataSnapshot item : snapshot.getChildren()) {
                    String name = item.child("name").getValue(String.class);
                    String quantity = item.child("quantity").getValue(String.class);
                    String unit = item.child("unit").getValue(String.class);
                    String expiration = item.child("expiration").getValue(String.class);
                    ingredientList.add(name + " - " + quantity + " " + unit + " (expires: " + expiration + ")");
                }
                // Tell the adapter the data changed so the list updates
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                //required for ValueEventListener. I left it empty for now, so if firebase fails to read data
                //nothing happens. Don't remove as it wont compile then
            }
        });
    }

    // Shows a window to add ingredients via input fields, and saves it to firebase
    //Might have to be changed later to integrate with recipes and automatic updates
    private void showAddIngredientDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Ingredient name");
        layout.addView(nameInput);

        EditText quantityInput = new EditText(this);
        quantityInput.setHint("Quantity");
        layout.addView(quantityInput);

        EditText unitInput = new EditText(this);
        unitInput.setHint("Unit (e.g. lbs, cups)");
        layout.addView(unitInput);

        EditText expirationInput = new EditText(this);
        expirationInput.setHint("Expiration date");
        layout.addView(expirationInput);

        // Build and show the window and save to firebase
        new AlertDialog.Builder(this)
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

                    DatabaseReference pantryRef = database.getReference("pantry");

                    DatabaseReference newItem = pantryRef.push();

                    newItem.child("name").setValue(name);
                    newItem.child("quantity").setValue(quantity);
                    newItem.child("unit").setValue(unit);
                    newItem.child("expiration").setValue(expiration);
                })

                .setNegativeButton("Cancel", null)
                .show();
    }
}