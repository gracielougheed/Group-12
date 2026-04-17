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
import com.example.mainproject.ViewRecipeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ViewFriendsRecipesActivity extends AppCompatActivity {

    private LinearLayout friendsRecipesContainer;
    private TextView emptyStateMessage;

    FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_view_friends_recipes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        friendsRecipesContainer = findViewById(R.id.friendsRecipesContainer);

        // Create and add empty state message
        emptyStateMessage = new android.widget.TextView(this);
        emptyStateMessage.setText("No friends have shared recipes with you yet.");
        emptyStateMessage.setTextSize(16);
        emptyStateMessage.setGravity(android.view.Gravity.CENTER);
        emptyStateMessage.setTextColor(android.graphics.Color.GRAY);
        friendsRecipesContainer.addView(emptyStateMessage);

        String uid = FirebaseAuth.getInstance().getUid();
        if(uid == null){
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }else{
            loadFriendsRecipes();
        }
    }

    private void loadFriendsRecipes() {
        String uid = FirebaseAuth.getInstance().getUid();
        if(uid == null){
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }else{
            // Get all friends
            database.getReference("users").child(uid).child("friends").get().addOnSuccessListener(friendsSnapshot -> {
                for(DataSnapshot friendSnapshot : friendsSnapshot.getChildren()){
                    String friendUid = friendSnapshot.getKey();

                    // For each friend, get their recipes
                    database.getReference("users").child(friendUid).child("recipes").get().addOnSuccessListener(recipesSnapshot -> {
                        for(DataSnapshot recipeSnapshot : recipesSnapshot.getChildren()){
                            String recipeId = recipeSnapshot.getKey();
                            String recipeName = recipeSnapshot.child("title").getValue(String.class);
                            String finalRecipeId = recipeId;
                            String finalFriendUid = friendUid;

                            // Check if current user is in viewers list
                            List<String> viewers = (List<String>) recipeSnapshot.child("viewers").getValue();

                            if(viewers != null && viewers.contains(uid)){
                                // Remove empty state message on first recipe found
                                if(friendsRecipesContainer.indexOfChild(emptyStateMessage) != -1){
                                    friendsRecipesContainer.removeView(emptyStateMessage);
                                }

                                // Get the friend's (creator's) name from their user profile
                                    database.getReference("users").child(finalFriendUid).child("name").get().addOnSuccessListener(nameSnapshot -> {
                                        String creatorName = nameSnapshot.getValue(String.class);
                                        if(creatorName == null){
                                            creatorName = "Unknown";
                                        }

                                        // Create and display recipe item
                                        android.widget.TextView recipeItem = new android.widget.TextView(ViewFriendsRecipesActivity.this);
                                        recipeItem.setText(recipeName + " - By " + creatorName);
                                        recipeItem.setTextSize(16);
                                        recipeItem.setPadding(10, 20, 10, 20);
                                        recipeItem.setBackgroundResource(android.R.drawable.list_selector_background);
                                        recipeItem.setClickable(true);
                                        recipeItem.setFocusable(true);

                                        // Set click listener to navigate to ViewRecipeActivity
                                        recipeItem.setOnClickListener(v -> {
                                            Intent intent = new Intent(ViewFriendsRecipesActivity.this, ViewRecipeActivity.class);
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

                                        friendsRecipesContainer.addView(recipeItem);
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