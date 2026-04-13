package com.example.mainproject.Friends;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.mainproject.R;
import com.example.mainproject.entities.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment {

    private SearchView searchView;
    private boolean isSearching = false;
    private ArrayAdapter<String> friendsAdapter;
    private List<User> friendsList;
    private FirebaseUser currentUser;

    FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
    );

    public FriendsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        // Initialize SearchView
        searchView = view.findViewById(R.id.searchViewFriends);
        setupSearchView();

        // Initialize Friend Requests icon button
        ImageView btnFriendRequests = view.findViewById(R.id.btnFriendRequests);
        btnFriendRequests.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FriendRequestsActivity.class);
            startActivity(intent);
        });

        ListView listViewFriends = view.findViewById(R.id.listViewFriends);
        friendsList = new ArrayList<>();
        friendsAdapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_list_item_1, new ArrayList<>());
        listViewFriends.setAdapter(friendsAdapter);

        // Load friends list
        listFriends();
        return view;
    }

    /**
     * Set up the SearchView to handle search queries
     */
    private void setupSearchView() {
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    //Check to ensure no searches are already running, and query is not empty
                    if (!isSearching && !query.isEmpty()) {
                        isSearching = true;
                        performSearch(query);
                        // Clear the search view
                        searchView.setQuery("", false);
                        searchView.clearFocus(); //Removes focus and closes keyboard
                    }
                    return true;
                }
                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
        }
    }

    /**
     * Perform a search for friends using the provided query
     * Redirects to SearchFriendsActivity to handle the search
     *
     * @param query The search query input
     */
    private void performSearch(String query) {
        Intent intent = new Intent(getActivity(), SearchFriendsActivity.class);
        intent.setAction(Intent.ACTION_SEARCH); //Search operation
        intent.putExtra(SearchManager.QUERY, query); //Add query data to intent
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //Use new fresh activity
        startActivity(intent);

        // Reset search flag after launching (allows activity to launch before triggering another search)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> isSearching = false, 500);
    }

    /**
     * Load all friends for the current user
     * Fetches each friends details and adds them to friendsList
     */
    public void listFriends() {
        if (currentUser == null) {
            Toast.makeText(getActivity(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get reference to current user's friends
        DatabaseReference currentUserRef = database.getReference("users")
                .child(currentUser.getUid())
                .child("friends");

        currentUserRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot friendsSnapshot = task.getResult();
                friendsList.clear(); //Clear previous list to avoid duplicates

                if (friendsSnapshot.exists()) {
                    // Get each friend's UID
                    for (DataSnapshot friendIdSnapshot : friendsSnapshot.getChildren()) {
                        String friendId = friendIdSnapshot.getKey();

                        // Fetch the friend's details using ID
                        assert friendId != null;
                        DatabaseReference friendRef = database.getReference("users")
                                .child(friendId);

                        friendRef.get().addOnCompleteListener(friendTask -> {
                            if (friendTask.isSuccessful() && friendTask.getResult().exists()) {
                                String friendName = friendTask.getResult().child("name").getValue(String.class);
                                String friendEmail = friendTask.getResult().child("email").getValue(String.class);

                                if (friendName != null) {
                                    User friend = new User(friendId, friendName, friendEmail);
                                    friendsList.add(friend);
                                    updateFriendsListView();
                                }
                            }
                        });
                    }
                } else {
                    updateFriendsListView();  // Empty list
                }
            } else {
                Toast.makeText(getActivity(), "Error loading friends", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Loops through each friend in the friendsList and adds their name to the adapter
     */
    private void updateFriendsListView() {
        List<String> friendNames = new ArrayList<>();
        for (User friend : friendsList) {
            friendNames.add(friend.name);
        }

        friendsAdapter.clear();
        friendsAdapter.addAll(friendNames);
        friendsAdapter.notifyDataSetChanged();
    }
}