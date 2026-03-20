package com.example.mainproject;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.mainproject.entities.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
    );

    private FirebaseAuth myAuth;
    private EditText emailInput;
    private EditText nameInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialise Firebase Authentication and UI elements
        myAuth = FirebaseAuth.getInstance();
        emailInput = findViewById(R.id.emailInput);
        nameInput = findViewById(R.id.nameInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.register_button);
    }

    /**
     * Handles the registration process when the user clicks the "Register" button.
     * Validates the email, username, password, and confirm password inputs.
     * If all validations pass, it checks if the username is already taken in the Firebase Realtime Database.
     * If the username is available, it creates a new user in Firebase Authentication and saves the user's profile in the database.
     * Displays appropriate error messages for validation failures or registration errors.
     *
     * @param view The view that was clicked, the "Register" button.
     */
    public void registerValidation(View view) {
        String email = emailInput.getText().toString().trim();
        String name = nameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (!emailValidation(email) || !passwordValidation(password, confirmPassword) || !userNameValidation(name)) {
            return;
        }
        registerButton.setEnabled(false);

        DatabaseReference usersRef = database.getReference("users");

        // Check if the username is already taken
        usersRef.orderByChild("name").equalTo(name)
                .get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        if (task.getResult().exists()) {
                            registerButton.setEnabled(true);
                            nameInput.setError("Username already taken");
                            nameInput.requestFocus();
                            return;
                        }
                        createFirebaseUser(email, password, name);
                    } else {
                        registerButton.setEnabled(true);
                        Toast.makeText(this, "Error checking username", Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Validates the email input.
     * Checks if the email is not empty and follows a valid email pattern.
     * If invalid, sets an error message on the email input field and requests focus.
     *
     * @param email The email string to validate.
     * @return true if the email is valid, false otherwise.
     */
    public boolean emailValidation(String email) {
        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email");
            emailInput.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Validates the username input.
     * Checks if the username is not empty.
     * If invalid, sets an error message on the name input field and requests focus.
     *
     * @param name The username string to validate.
     * @return true if the username is valid, false otherwise.
     */
    public boolean userNameValidation(String name) {
        if (name.isEmpty()) {
            nameInput.setError("Name is required");
            nameInput.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Validates the password and confirm password inputs.
     * Checks if the password is not empty, has a minimum length of 8 characters, has at least one letter, has at least one number and matches the confirm password.
     * If invalid, sets appropriate error messages on the password or confirm password input fields and requests focus.
     *
     * @param password The password string to validate.
     * @param confirmPassword The confirm password string to validate against the password.
     * @return true if the password is valid and matches the confirm password, false otherwise.
     */
    public boolean passwordValidation(String password, String confirmPassword) {
        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return false;
        }
        if (password.length() < 8 || !password.matches(".*\\d.*") || !password.matches(".*[a-zA-Z].*")) {
            passwordInput.setError("Password must be at least 8 characters longs\nPassword must contain at least one letter\nPassword must contain at least one number");
            passwordInput.requestFocus();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Creates a new user in Firebase Authentication with the provided email and password.
     * If the registration is successful, it also creates a user profile in the Firebase Realtime Database with the user's UID, name, and email.
     * Displays appropriate success or error messages based on the outcome of the registration and database operations.
     *
     * @param email The email address for the new user.
     * @param password The password for the new user.
     * @param name The name of the new user to be stored in the database.
     */
    private void createFirebaseUser(String email, String password, String name) {
        // Create user with Firebase Authentication
        myAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    registerButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        // Create user profile in Firebase Realtime Database
                        FirebaseUser firebaseUser = myAuth.getCurrentUser();
                        if(firebaseUser != null){
                            DatabaseReference usersRef = database.getReference("users").child(firebaseUser.getUid());
                            User user = new User(firebaseUser.getUid(), name, email);
                            usersRef.setValue(user).addOnCompleteListener(dbTask -> {
                                if (dbTask.isSuccessful()) {
                                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(this, HomeActivity.class);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(this, "Failed to save profile", Toast.LENGTH_LONG).show();
                                }
                            });
                        }else {
                            Toast.makeText(this, "Failed to obtain user information", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Registeration failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Navigates back to the LoginActivity when the user clicks on the "Login" button.
     * This method finishes the current activity, which will return the user to the previous activity (LoginActivity).
     *
     * @param view The view that was clicked, the "Login" button.
     */
    public void goToLogin(View view) {
        finish(); // Go back to the LoginActivity
    }
}