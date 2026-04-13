package com.example.mainproject;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mainproject.entities.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchFriendsActivity extends AppCompatActivity {

    FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
    );

    private ListView searchResultsListView;
    private List<User> searchResults;
    private ArrayAdapter<String> adapter;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_friends);

        searchResultsListView = findViewById(R.id.searchResultsList);
        searchResults = new ArrayList<>();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize adapter with empty list
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        searchResultsListView.setAdapter(adapter);

        // Handle item click to add friend
        searchResultsListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < searchResults.size()) {
                User selectedUser = searchResults.get(position);
                addFriend(selectedUser);
            }
        });

        // Handle the search intent from the intent that launched this activity
        handleSearchIntent(getIntent());
    }

    private void handleSearchIntent(Intent intent) {
        if (intent != null && Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (query != null && !query.isEmpty()) {
                performSearch(query);
            }
        }
    }
    private void performSearch(String query) {
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Searching for: " + query, Toast.LENGTH_SHORT).show();

        // Get all users from database
        DatabaseReference usersRef = database.getReference("users");
        usersRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot dataSnapshot = task.getResult();
                searchResults.clear();

                // Get current user's friends list
                DatabaseReference currentUserFriendsRef = database.getReference("users")
                        .child(currentUser.getUid())
                        .child("friends");

                currentUserFriendsRef.get().addOnCompleteListener(friendsTask -> {
                    List<String> currentUserFriends = new ArrayList<>();

                    if (friendsTask.isSuccessful() && friendsTask.getResult().exists()) {
                        for (DataSnapshot friendSnapshot : friendsTask.getResult().getChildren()) {
                            currentUserFriends.add(friendSnapshot.getKey());
                        }
                    }

                    // Filter and search through users
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userId = userSnapshot.getKey();
                        String userName = userSnapshot.child("name").getValue(String.class);

                        // Skip if user is null, is current user, or already a friend
                        if (userName == null || userId == null ||
                            userId.equals(currentUser.getUid()) ||
                            currentUserFriends.contains(userId)) {
                            continue;
                        }

                        // Check if user matches search query (case-insensitive)
                        if (userName.toLowerCase().contains(query.toLowerCase())) {
                            String email = userSnapshot.child("email").getValue(String.class);
                            User user = new User(userId, userName, email);
                            searchResults.add(user);
                        }
                    }

                    updateListView();
                });
            } else {
                Toast.makeText(SearchFriendsActivity.this, "Error searching users", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateListView() {
        List<String> displayNames = new ArrayList<>();
        for (User user : searchResults) {
            displayNames.add(user.name);
        }

        adapter.clear();
        adapter.addAll(displayNames);
        adapter.notifyDataSetChanged();

        if (displayNames.isEmpty()) {
            Toast.makeText(this, "No users found", Toast.LENGTH_SHORT).show();
            // Close activity after showing no results message
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                this::finish, 2000
            );
        }
    }
    private void addFriend(User selectedUser) {
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference currentUserIncomingRequestsRef = database.getReference("users")
                .child(currentUser.getUid())
                .child("incomingFriendRequests");

        currentUserIncomingRequestsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                // Check if selected user has already requested current user
                if (task.getResult().hasChild(selectedUser.uid)) {
                    Toast.makeText(SearchFriendsActivity.this,
                            "User has already requested you",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            sendFriendRequest(selectedUser);
        });
    }

    /**
     * Send a friend request to the selected user
     */
    private void sendFriendRequest(User selectedUser) {
        // Send friend request to the selected user
        DatabaseReference selectedUserRequestsRef = database.getReference("users")
                .child(selectedUser.uid)
                .child("incomingFriendRequests");

        // Add current user's ID to the selected user's incoming requests
        selectedUserRequestsRef.child(currentUser.getUid()).setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SearchFriendsActivity.this,
                                "Friend request sent to " + selectedUser.name + "!",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(SearchFriendsActivity.this,
                                "Error sending friend request",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

