package com.example.mainproject;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Displays the user's meal plans in a list.
 * Users can create new plans, tap a plan to view its meals,
 * or long-press to delete a plan. All data is saved to Firebase.
 */
public class MealPlannerFragment extends Fragment {

    private String uid;

    FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
    );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        View view = inflater.inflate(R.layout.fragment_meal_planner, container, false);

        Button btnCreate = view.findViewById(R.id.btnCreateMealPlan);
        ListView listView = view.findViewById(R.id.listViewMealPlans);

        // Lists to hold what's displayed and the matching Firebase keys
        ArrayList<String> planDisplayList = new ArrayList<>();
        ArrayList<String> planKeyList = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, planDisplayList);
        listView.setAdapter(adapter);

        // Open the create plan dialog
        btnCreate.setOnClickListener(v -> showCreatePlanDialog());

        // Tap a plan to view its meals
        listView.setOnItemClickListener((parent, v, position, id) -> {
            String key = planKeyList.get(position);
            Intent intent = new Intent(getActivity(), MealPlannerActivity.class);
            intent.putExtra("MEAL_PLAN_ID", key);
            startActivity(intent);
        });

        // Long press to delete a plan
        listView.setOnItemLongClickListener((parent, v, position, id) -> {
            String key = planKeyList.get(position);
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Meal Plan")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Yes", (d, w) -> {
                        database.getReference("users").child(uid)
                                .child("mealplans").child(key).removeValue();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });

        // Listen for real-time updates from Firebase
        DatabaseReference plansRef = database.getReference("users").child(uid).child("mealplans");
        plansRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                planDisplayList.clear();
                planKeyList.clear();
                for (DataSnapshot item : snapshot.getChildren()) {
                    String name = item.child("name").getValue(String.class);
                    String startDate = item.child("startDate").getValue(String.class);
                    String endDate = item.child("endDate").getValue(String.class);
                    planDisplayList.add(name + "\n" + startDate + " to " + endDate);
                    planKeyList.add(item.getKey());
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });

        return view;
    }

    /**
     * Shows a dialog to create a new meal plan.
     * User enters a name and picks start/end dates.
     */
    private void showCreatePlanDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_meal_plan, null);
        EditText nameInput = dialogView.findViewById(R.id.input_plan_name);
        Button startDateBtn = dialogView.findViewById(R.id.button_start_date);
        Button endDateBtn = dialogView.findViewById(R.id.button_end_date);

        // Default both dates to today
        Calendar cal = Calendar.getInstance();
        String today = String.format("%02d-%02d-%02d",
                cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR) % 100);
        startDateBtn.setText(today);
        endDateBtn.setText(today);

        startDateBtn.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
                startDateBtn.setText(String.format("%02d-%02d-%02d", day, month + 1, year % 100));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        endDateBtn.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (picker, year, month, day) -> {
                endDateBtn.setText(String.format("%02d-%02d-%02d", day, month + 1, year % 100));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Create Meal Plan")
                .setView(dialogView)
                .setPositiveButton("Create", null)
                .setNegativeButton("Cancel", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                nameInput.setError("Required");
                return;
            }

            String startDate = startDateBtn.getText().toString();
            String endDate = endDateBtn.getText().toString();

            // Save the new meal plan to Firebase
            DatabaseReference plansRef = database.getReference("users").child(uid).child("mealplans");
            DatabaseReference newPlan = plansRef.push();
            newPlan.child("name").setValue(name);
            newPlan.child("startDate").setValue(startDate);
            newPlan.child("endDate").setValue(endDate);
            dialog.dismiss();
            Toast.makeText(requireContext(), "Meal plan created", Toast.LENGTH_SHORT).show();
        });
    }
}
