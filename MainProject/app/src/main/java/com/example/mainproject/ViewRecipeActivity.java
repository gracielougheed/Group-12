package com.example.mainproject;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ViewRecipeActivity extends AppCompatActivity {

    private String recipeId;
    private String title;
    
    // UI elements
    private TextView textViewTitle, textViewCategory, textViewDescription;
    private TextView textViewPrepTime, textViewCookTime, textViewServings;
    private Button buttonDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recipe);

        // Initialize UI
        textViewTitle = findViewById(R.id.textViewTitle);
        textViewCategory = findViewById(R.id.textViewCategory);
        textViewDescription = findViewById(R.id.textViewDescription);
        textViewPrepTime = findViewById(R.id.textViewPrepTime);
        textViewCookTime = findViewById(R.id.textViewCookTime);
        textViewServings = findViewById(R.id.textViewServings);
        buttonDelete = findViewById(R.id.buttonDelete);

        // Get data from Intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            recipeId = extras.getString("RECIPE_ID");
            title = extras.getString("TITLE");
            
            textViewTitle.setText(title);
            textViewCategory.setText(extras.getString("CATEGORY"));
            textViewDescription.setText(extras.getString("DESCRIPTION"));
            
            String prep = extras.getInt("PREP_TIME") + " " + extras.getString("PREP_UNIT");
            textViewPrepTime.setText(prep);
            
            String cook = extras.getInt("COOK_TIME") + " " + extras.getString("COOK_UNIT");
            textViewCookTime.setText(cook);
            
            textViewServings.setText(String.valueOf(extras.getInt("SERVINGS")));
        }

        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmDeletion();
            }
        });
    }

    private void confirmDeletion() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Recipe")
            .setMessage("Are you sure you want to delete '" + title + "'?")
            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteFromFirebase();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteFromFirebase() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || recipeId == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance(
                "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
        ).getReference("users").child(uid).child("recipes").child(recipeId);

        ref.removeValue().addOnSuccessListener(aVoid -> {
            Toast.makeText(ViewRecipeActivity.this, "Recipe deleted", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(ViewRecipeActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
