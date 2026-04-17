package com.example.mainproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class ShareRecipeActivity extends AppCompatActivity {

    private LinearLayout friendSelectionContainer;
    private Button shareRecipe;
    private DatabaseReference friendsRef;
    private String recipeId;

    FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_share_recipe_activity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        friendSelectionContainer = findViewById(R.id.friendSelectionContainer);
        shareRecipe = findViewById(R.id.btnShareRecipe);

        recipeId = getIntent().getStringExtra("RECIPE_ID");

        String uid = FirebaseAuth.getInstance().getUid();
        if(uid == null){
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }else{
            friendsRef = database.getReference("users").child(uid).child("friends");
            loadFriendsForSelection();
        }
        
        shareRecipe.setOnClickListener(v -> shareRecipeWithFriends());
    }

    private void loadFriendsForSelection(){
        friendsRef.get().addOnCompleteListener(task -> {
            if(!task.isSuccessful()){
                Toast.makeText(this, "Error loading friends", Toast.LENGTH_SHORT).show();
                return;
            }

            for(DataSnapshot friendSnapshot : task.getResult().getChildren()){
                String friendUid = friendSnapshot.getKey();
                if(friendUid == null) {
                    Toast.makeText(this, "Error loading friends", Toast.LENGTH_SHORT).show();
                    return;
                }
                DatabaseReference friendInfoRef = database.getReference("users").child(friendUid);
                        friendInfoRef.get().addOnCompleteListener(friendTask -> {
                            if(!friendTask.isSuccessful()){
                                Toast.makeText(this, "Error loading friend info", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String friendName = friendTask.getResult().child("name").getValue(String.class);
                            if(friendName != null){
                                CheckBox checkBox = new CheckBox(this);
                                checkBox.setText(friendName);
                                checkBox.setTag(friendUid);
                                friendSelectionContainer.addView(checkBox);
                            }else{
                                Toast.makeText(this, "Error loading friend name", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    private void shareRecipeWithFriends(){
        String uid = FirebaseAuth.getInstance().getUid();

        // Get all selected friends
        List<String> selectedFriendUids = new ArrayList<>();

        for(int i = 0; i < friendSelectionContainer.getChildCount(); i++){
            View child = friendSelectionContainer.getChildAt(i);
            if(child instanceof CheckBox){
                CheckBox checkBox = (CheckBox) child;
                if(checkBox.isChecked()){
                    String friendUid = (String) checkBox.getTag();
                    selectedFriendUids.add(friendUid);
                }
            }
        }

        if(selectedFriendUids.isEmpty()){
            Toast.makeText(this, "Please select at least one friend", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the recipe and read existing viewers
        DatabaseReference recipeRef = database.getReference("users").child(uid).child("recipes").child(recipeId);

        recipeRef.child("viewers").get().addOnCompleteListener(task -> {
            List<String> allViewers = new ArrayList<>();

            // If viewers already exist, add them to the list of all viewers
            if(task.isSuccessful() && task.getResult().getValue() != null){
                try {
                    List<String> existingViewers = (List<String>) task.getResult().getValue();
                    if(existingViewers != null){
                        allViewers.addAll(existingViewers);
                    }
                } catch (Exception e) {
                    Toast.makeText(ShareRecipeActivity.this, "Error reading existing viewers", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Checks if there are any new viewers to add and adds them to the list of all viewers
            int newViewersCount = 0;
            for(String friendUid : selectedFriendUids){
                if(!allViewers.contains(friendUid)){
                    allViewers.add(friendUid);
                    newViewersCount++;
                }
            }

            if(newViewersCount == 0){
                Toast.makeText(ShareRecipeActivity.this, "All selected friends are already viewers", Toast.LENGTH_SHORT).show();
                return;
            }

            int finalNewViewersCount = newViewersCount;
            recipeRef.child("viewers").setValue(allViewers).addOnCompleteListener(writeTask -> {
                if(writeTask.isSuccessful()){
                    Toast.makeText(ShareRecipeActivity.this, "Recipe shared with " + finalNewViewersCount + " new friend(s)", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ShareRecipeActivity.this, "Failed to share recipe", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    public void goBack(View view){
        finish();
    }
}