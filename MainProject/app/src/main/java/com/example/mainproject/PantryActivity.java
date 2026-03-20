package com.example.mainproject;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;

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
    }

    // Shows a window with input fields for adding a new ingredient
    private void showAddIngredientDialog() {

    }
}