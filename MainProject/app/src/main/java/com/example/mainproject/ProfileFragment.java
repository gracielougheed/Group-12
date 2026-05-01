package com.example.mainproject;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private static final String DB_URL =
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/";

    private TextView tvUsername, tvEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvUsername = view.findViewById(R.id.tvUsername);
        tvEmail = view.findViewById(R.id.tvEmail);
        Button btnLogout = view.findViewById(R.id.btnLogout);
        Button btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount);

        loadUserDetails();

        btnLogout.setOnClickListener(v -> logout());
        btnDeleteAccount.setOnClickListener(v -> confirmDeleteAccount());

        return view;
    }

    private void loadUserDetails() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Show email from FirebaseAuth directly
        tvEmail.setText(user.getEmail());

        // Load username from the database
        FirebaseDatabase.getInstance(DB_URL)
                .getReference("users").child(user.getUid()).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.getValue(String.class);
                        tvUsername.setText(name != null ? name : "No username");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvUsername.setText("Failed to load");
                    }
                });
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        goToLogin();
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure? This will permanently delete your account and all your data.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        // Delete user data from the database first, then delete the auth account
        FirebaseDatabase.getInstance(DB_URL)
                .getReference("users").child(uid)
                .removeValue()
                .addOnCompleteListener(dbTask -> {
                    user.delete().addOnCompleteListener(authTask -> {
                        if (authTask.isSuccessful()) {
                            Toast.makeText(requireContext(), "Account deleted.", Toast.LENGTH_SHORT).show();
                            goToLogin();
                        } else {
                            Toast.makeText(requireContext(),
                                    "Failed to delete account. You may need to log in again.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
