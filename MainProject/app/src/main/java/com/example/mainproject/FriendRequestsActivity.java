package com.example.mainproject;

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
import java.util.List;

/**
 * Activity that displays incoming friend requests.
 * Users can accept or reject friend requests from this screen.
 */
public class FriendRequestsActivity extends AppCompatActivity {

    FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
    );

    private ListView friendRequestsListView;
    private List<User> incomingRequests;
    private ArrayAdapter<String> adapter;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_requests);

        friendRequestsListView = findViewById(R.id.friendRequestsList);
        incomingRequests = new ArrayList<>();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Initialize adapter with empty list
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        friendRequestsListView.setAdapter(adapter);

        // Load incoming friend requests
        loadFriendRequests();

        // Handle item click for request actions (long click to see options or use buttons in custom layout)
        friendRequestsListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < incomingRequests.size()) {
                showRequestOptions(incomingRequests.get(position));
            }
        });
    }

    /**
     * Load all incoming friend requests for the current user
     */
    private void loadFriendRequests() {
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference incomingRequestsRef = database.getReference("users")
                .child(currentUser.getUid())
                .child("incomingFriendRequests");

        incomingRequestsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot requestsSnapshot = task.getResult();
                incomingRequests.clear();

                if (requestsSnapshot.exists()) {
                    // Get each requester's ID
                    for (DataSnapshot requesterSnapshot : requestsSnapshot.getChildren()) {
                        String requesterId = requesterSnapshot.getKey();

                        // Fetch the requester's details
                        DatabaseReference requesterRef = database.getReference("users")
                                .child(requesterId);

                        requesterRef.get().addOnCompleteListener(requesterTask -> {
                            if (requesterTask.isSuccessful() && requesterTask.getResult().exists()) {
                                String requesterName = requesterTask.getResult().child("name").getValue(String.class);
                                String requesterEmail = requesterTask.getResult().child("email").getValue(String.class);

                                if (requesterName != null) {
                                    User requester = new User(requesterId, requesterName, requesterEmail);
                                    incomingRequests.add(requester);
                                    updateRequestsListView();
                                }
                            }
                        });
                    }
                } else {
                    updateRequestsListView();  // Empty list
                }
            } else {
                Toast.makeText(this, "Error loading friend requests", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateRequestsListView() {
        List<String> displayNames = new ArrayList<>();
        for (User requester : incomingRequests) {
            displayNames.add(requester.name);
        }

        adapter.clear();
        adapter.addAll(displayNames);
        adapter.notifyDataSetChanged();

        if (displayNames.isEmpty()) {
            Toast.makeText(this, "No friend requests", Toast.LENGTH_SHORT).show();
        }
    }

    private void showRequestOptions(User requester) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Friend Request from " + requester.name)
                .setMessage("Accept or reject this friend request?")
                .setPositiveButton("Accept", (dialog, which) -> {
                    acceptFriendRequest(requester);
                })
                .setNegativeButton("Reject", (dialog, which) -> {
                    rejectFriendRequest(requester);
                })
                .show();
    }

    /**
     * Accept a friend request and add both users as friends
     *
     * @param requester The user whose request we're accepting
     */
    private void acceptFriendRequest(User requester) {
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add to current user's friends list
        DatabaseReference currentUserFriendsRef = database.getReference("users")
                .child(currentUser.getUid())
                .child("friends");

        currentUserFriendsRef.child(requester.uid).setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Add current user to requester's friends list
                        DatabaseReference requesterFriendsRef = database.getReference("users")
                                .child(requester.uid)
                                .child("friends");

                        requesterFriendsRef.child(currentUser.getUid()).setValue(true)
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful()) {
                                        // Remove the request
                                        removeRequest(requester.uid);
                                    } else {
                                        Toast.makeText(FriendRequestsActivity.this,
                                            "Error accepting friend request",
                                            Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(FriendRequestsActivity.this,
                            "Error accepting friend request",
                            Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Reject a friend request
     *
     * @param requester The user whose request we're rejecting
     */
    private void rejectFriendRequest(User requester) {
        removeRequest(requester.uid);
    }

    /**
     * Remove a friend request from the database
     *
     * @param requesterId The ID of the user whose request we're removing
     */
    private void removeRequest(String requesterId) {
        if (currentUser == null) {
            return;
        }

        DatabaseReference incomingRequestsRef = database.getReference("users")
                .child(currentUser.getUid())
                .child("incomingFriendRequests");

        incomingRequestsRef.child(requesterId).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Reload the list
                        loadFriendRequests();
                    } else {
                        Toast.makeText(FriendRequestsActivity.this,
                            "Error updating friend request",
                            Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

