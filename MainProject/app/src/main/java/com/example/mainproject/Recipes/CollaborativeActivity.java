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
import com.google.firebase.database.FirebaseDatabase;

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
        //Load public recipes here
    }

    public void goBack(View view){
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }
}