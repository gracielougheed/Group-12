package com.example.mainproject;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * ViewRecipeActivity displays details of a recipe and allows deletion.
 */
public class ViewRecipeActivity extends AppCompatActivity {

    private String recipeId;
    private String recipeTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recipe);

        // Find views
        TextView titleTextView = findViewById(R.id.recipeTitle);
        TextView categoryTextView = findViewById(R.id.recipeCategory);
        TextView prepTimeTextView = findViewById(R.id.recipePrepTime);
        TextView cookTimeTextView = findViewById(R.id.recipeCookTime);
        TextView servingsTextView = findViewById(R.id.recipeServingSize);
        TextView instructionsTextView = findViewById(R.id.recipeInstructions);
        Button deleteButton = findViewById(R.id.deleteRecipeButton);

        // Retrieve data from intent
        if (getIntent() != null) {
            recipeId = getIntent().getStringExtra("RECIPE_ID");
            recipeTitle = getIntent().getStringExtra("title");
            
            titleTextView.setText(recipeTitle);
            categoryTextView.setText(getIntent().getStringExtra("CATEGORY"));
            
            String prepTime = getIntent().getIntExtra("PREP_TIME", 0) + " " + getIntent().getStringExtra("PREP_UNIT");
            prepTimeTextView.setText(prepTime);
            
            String cookTime = getIntent().getIntExtra("COOK_TIME", 0) + " " + getIntent().getStringExtra("COOK_UNIT");
            cookTimeTextView.setText(cookTime);
            
            servingsTextView.setText(String.valueOf(getIntent().getIntExtra("SERVINGS", 1)));
            instructionsTextView.setText(getIntent().getStringExtra("instructions"));
        }

        // Set up delete button with confirmation dialog
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmationDialog();
            }
        });
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Recipe");
        builder.setMessage("Are you sure you want to delete '" + recipeTitle + "'?");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteRecipe();
            }
        });

        builder.setNegativeButton("Cancel", null);

        builder.create().show();
    }

    private void deleteRecipe() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || recipeId == null) {
            Toast.makeText(this, "Error: Could not find recipe", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference recipeRef = FirebaseDatabase.getInstance(
                "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
        ).getReference("users").child(uid).child("recipes").child(recipeId);

        recipeRef.removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(ViewRecipeActivity.this, "Recipe deleted", Toast.LENGTH_SHORT).show();
                    finish(); // Close activity and return to list
                } else {
                    Toast.makeText(ViewRecipeActivity.this, "Failed to delete recipe", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
