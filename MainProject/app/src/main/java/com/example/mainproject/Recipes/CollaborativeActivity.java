package com.example.mainproject.Recipes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mainproject.HomeActivity;
import com.example.mainproject.LoginActivity;
import com.example.mainproject.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class CollaborativeActivity extends AppCompatActivity {

    private LinearLayout collabRecipesContainer;

    private TextView emptyStateMessage;

    FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_collaborative);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        collabRecipesContainer = findViewById(R.id.collabRecipesContainer);

        // Create and add empty state message
        emptyStateMessage = new android.widget.TextView(this);
        emptyStateMessage.setText("No collaborative recipes found. Start collaborating with friends to see shared recipes here!");
        emptyStateMessage.setTextSize(16);
        emptyStateMessage.setGravity(android.view.Gravity.CENTER);
        emptyStateMessage.setTextColor(android.graphics.Color.GRAY);
        collabRecipesContainer.addView(emptyStateMessage);

        String uid = FirebaseAuth.getInstance().getUid();
        if(uid == null){
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }else{
            loadCollabRecipes();
        }
    }

    private void loadCollabRecipes(){
        String uid = FirebaseAuth.getInstance().getUid();
        if(uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }else{

            // Load user's own public recipes
            database.getReference("users").child(uid).child("recipes").get().addOnSuccessListener(ownRecipesSnapshot -> {
                for (DataSnapshot recipeSnapshot : ownRecipesSnapshot.getChildren()) {
                    Boolean isPublic = recipeSnapshot.child("isPublic").getValue(Boolean.class);
                    
                    // Only show public recipes
                    if (isPublic != null && isPublic) {
                        String recipeId = recipeSnapshot.getKey();
                        String recipeName = recipeSnapshot.child("title").getValue(String.class);
                        
                        if (collabRecipesContainer.indexOfChild(emptyStateMessage) != -1) {
                            collabRecipesContainer.removeView(emptyStateMessage);
                        }

                            TextView recipeTextView = new TextView(this);
                            recipeTextView.setText(recipeName + " (owned by You)");
                            recipeTextView.setTextSize(18);
                            recipeTextView.setPadding(16, 16, 16, 16);
                            recipeTextView.setBackgroundResource(android.R.drawable.list_selector_background);
                            recipeTextView.setClickable(true);
                            recipeTextView.setFocusable(true);

                        recipeTextView.setOnClickListener(v -> {
                            Intent intent = new Intent(this, ViewRecipeActivity.class);
                            intent.putExtra("RECIPE_ID", recipeSnapshot.getKey());
                            intent.putExtra("OWNER_UID", recipeSnapshot.child("ownerUid").getValue(String.class));
                            intent.putExtra("title", recipeName);
                            intent.putExtra("CATEGORY", recipeSnapshot.child("category").getValue(String.class));
                            intent.putExtra("PREP_TIME", recipeSnapshot.child("prepTimeValue").getValue(Integer.class));
                            intent.putExtra("PREP_UNIT", recipeSnapshot.child("prepTimeUnit").getValue(String.class));
                            intent.putExtra("COOK_TIME", recipeSnapshot.child("cookTimeValue").getValue(Integer.class));
                            intent.putExtra("COOK_UNIT", recipeSnapshot.child("cookTimeUnit").getValue(String.class));
                            intent.putExtra("SERVINGS", recipeSnapshot.child("servingSize").getValue(Integer.class));
                            intent.putExtra("instructions", recipeSnapshot.child("description").getValue(String.class));
                            startActivity(intent);
                        });
                            
                            collabRecipesContainer.addView(recipeTextView);
                    }
                }
            });
            
            // Load recipes where user is a collaborator
            database.getReference("users").child(uid).child("friends").get().addOnSuccessListener(friendsSnapshot -> {
                for (DataSnapshot friendSnapshot : friendsSnapshot.getChildren()) {
                    String friendUid = friendSnapshot.getKey();

                    database.getReference("users").child(friendUid).child("recipes").get().addOnSuccessListener(recipesSnapshot -> {
                        for (DataSnapshot recipeSnapshot : recipesSnapshot.getChildren()) {
                            String recipeId = recipeSnapshot.getKey();
                            String recipeName = recipeSnapshot.child("title").getValue(String.class);
                            String finalRecipeId = recipeId;
                            String finalFriendUid = friendUid;

                            List<String> collaborators = (List<String>) recipeSnapshot.child("collaborators").getValue();

                            if (collaborators != null && collaborators.contains(uid)) {

                                if (collabRecipesContainer.indexOfChild(emptyStateMessage) != -1) {
                                    collabRecipesContainer.removeView(emptyStateMessage);
                                }

                                database.getReference("users").child(finalFriendUid).child("name").get().addOnSuccessListener(nameSnapshot -> {
                                    String creatorName = nameSnapshot.getValue(String.class);
                                    if (creatorName == null) {
                                        creatorName = "Unknown";
                                    }

                                    TextView recipeTextView = new TextView(this);
                                    recipeTextView.setText(recipeName + " (owned by " + creatorName + ")");
                                    recipeTextView.setTextSize(18);
                                    recipeTextView.setPadding(16, 16, 16, 16);
                                    recipeTextView.setBackgroundResource(android.R.drawable.list_selector_background);
                                    recipeTextView.setClickable(true);
                                    recipeTextView.setFocusable(true);
                                    
                                    recipeTextView.setOnClickListener(v -> {
                                        Intent intent = new Intent(this, ViewRecipeActivity.class);
                                        intent.putExtra("RECIPE_ID", finalRecipeId);
                                        intent.putExtra("OWNER_UID", finalFriendUid);
                                        intent.putExtra("title", recipeName);
                                        intent.putExtra("CATEGORY", recipeSnapshot.child("category").getValue(String.class));
                                        intent.putExtra("PREP_TIME", recipeSnapshot.child("prepTimeValue").getValue(Integer.class));
                                        intent.putExtra("PREP_UNIT", recipeSnapshot.child("prepTimeUnit").getValue(String.class));
                                        intent.putExtra("COOK_TIME", recipeSnapshot.child("cookTimeValue").getValue(Integer.class));
                                        intent.putExtra("COOK_UNIT", recipeSnapshot.child("cookTimeUnit").getValue(String.class));
                                        intent.putExtra("SERVINGS", recipeSnapshot.child("servingSize").getValue(Integer.class));
                                        intent.putExtra("instructions", recipeSnapshot.child("description").getValue(String.class));
                                        startActivity(intent);
                                    });
                                    
                                    collabRecipesContainer.addView(recipeTextView);
                                });
                            }
                        }
                    });
                }
            });    
        }
    }

    public void goBack(View view){
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }
}