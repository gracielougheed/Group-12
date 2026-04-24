package com.example.mainproject;

import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Displays a single meal plan's meals organised by date.
 * Each date in the plan range gets a header, with meals listed underneath.
 * Users can add new meals (picking date, meal type, and recipe) or tap a meal to remove it.
 */
public class MealPlannerActivity extends AppCompatActivity {

    private String uid;
    private String mealPlanId;
    private String planStartDate;
    private String planEndDate;
    private LinearLayout mealsContainer;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yy", Locale.getDefault());
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("EEE dd/MM", Locale.getDefault());

    FirebaseDatabase database = FirebaseDatabase.getInstance(
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_planner);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mealPlanId = getIntent().getStringExtra("MEAL_PLAN_ID");

        TextView titleView = findViewById(R.id.planTitle);
        TextView dateRangeView = findViewById(R.id.planDateRange);
        mealsContainer = findViewById(R.id.mealsContainer);
        Button btnAddMeal = findViewById(R.id.btnAddMeal);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnAddMeal.setOnClickListener(v -> showAddMealDialog());

        // Tap the title to edit the plan name and dates
        titleView.setOnClickListener(v -> showEditPlanDialog());

        // Listen for real-time updates to this meal plan
        DatabaseReference planRef = database.getReference("users").child(uid)
                .child("mealplans").child(mealPlanId);

        planRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String name = snapshot.child("name").getValue(String.class);
                planStartDate = snapshot.child("startDate").getValue(String.class);
                planEndDate = snapshot.child("endDate").getValue(String.class);

                titleView.setText(name);
                dateRangeView.setText(planStartDate + " to " + planEndDate);

                buildMealsView(snapshot.child("meals"));
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    /**
     * Builds the meal list UI grouped by date.
     * For each date in the plan range, shows a header and the meals for that day.
     */
    private void buildMealsView(DataSnapshot mealsSnapshot) {
        mealsContainer.removeAllViews();

        // Group meals by date
        Map<String, List<DataSnapshot>> mealsByDate = new HashMap<>();
        for (DataSnapshot meal : mealsSnapshot.getChildren()) {
            String date = meal.child("date").getValue(String.class);
            if (date != null) {
                if (!mealsByDate.containsKey(date)) {
                    mealsByDate.put(date, new ArrayList<>());
                }
                mealsByDate.get(date).add(meal);
            }
        }

        // Iterate through each date in the plan range and display meals
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(dateFormat.parse(planStartDate));
            Date endDate = dateFormat.parse(planEndDate);

            while (!cal.getTime().after(endDate)) {
                String dateStr = dateFormat.format(cal.getTime());
                String displayDate = displayFormat.format(cal.getTime());

                // Date header — tap to clear all meals for this date
                TextView header = new TextView(this);
                header.setText(displayDate);
                header.setTextSize(18);
                header.setTypeface(null, Typeface.BOLD);
                header.setPadding(0, 24, 0, 8);
                header.setBackgroundResource(android.R.drawable.list_selector_background);
                header.setClickable(true);
                header.setFocusable(true);

                String dateToDelete = dateStr;
                header.setOnClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Clear " + displayDate)
                            .setMessage("Remove all meals for this date?")
                            .setPositiveButton("Yes", (d, w) -> {
                                DatabaseReference mealsRef = database.getReference("users").child(uid)
                                        .child("mealplans").child(mealPlanId).child("meals");
                                mealsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot snapshot) {
                                        for (DataSnapshot meal : snapshot.getChildren()) {
                                            if (dateToDelete.equals(meal.child("date").getValue(String.class))) {
                                                meal.getRef().removeValue();
                                            }
                                        }
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError error) {}
                                });
                            })
                            .setNegativeButton("No", null)
                            .show();
                });

                mealsContainer.addView(header);

                // Meals for this date
                List<DataSnapshot> meals = mealsByDate.get(dateStr);
                if (meals != null) {
                    // Sort by meal type order: Breakfast, Lunch, Dinner, Snack
                    meals.sort((a, b) -> getMealTypeOrder(a.child("mealType").getValue(String.class))
                            - getMealTypeOrder(b.child("mealType").getValue(String.class)));

                    for (DataSnapshot meal : meals) {
                        String mealType = meal.child("mealType").getValue(String.class);
                        String recipeTitle = meal.child("recipeTitle").getValue(String.class);
                        String mealKey = meal.getKey();

                        TextView mealView = new TextView(this);
                        mealView.setText("  " + mealType + ": " + recipeTitle);
                        mealView.setTextSize(16);
                        mealView.setPadding(20, 12, 10, 12);
                        mealView.setBackgroundResource(android.R.drawable.list_selector_background);
                        mealView.setClickable(true);
                        mealView.setFocusable(true);

                        // Tap to remove a meal
                        mealView.setOnClickListener(v -> {
                            new AlertDialog.Builder(this)
                                    .setTitle("Remove Meal")
                                    .setMessage("Remove " + recipeTitle + " from " + mealType + "?")
                                    .setPositiveButton("Yes", (d, w) -> {
                                        database.getReference("users").child(uid)
                                                .child("mealplans").child(mealPlanId)
                                                .child("meals").child(mealKey).removeValue();
                                    })
                                    .setNegativeButton("No", null)
                                    .show();
                        });

                        mealsContainer.addView(mealView);
                    }
                } else {
                    TextView empty = new TextView(this);
                    empty.setText("  No meals planned");
                    empty.setTextSize(14);
                    empty.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    empty.setPadding(20, 8, 10, 8);
                    mealsContainer.addView(empty);
                }

                cal.add(Calendar.DAY_OF_MONTH, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getMealTypeOrder(String mealType) {
        if (mealType == null) return 99;
        switch (mealType) {
            case "Breakfast": return 1;
            case "Lunch": return 2;
            case "Dinner": return 3;
            case "Snack": return 4;
            default: return 99;
        }
    }

    /**
     * Shows a dialog to edit the plan name and date range.
     * Reuses the same layout as creating a plan, with fields pre-filled.
     */
    private void showEditPlanDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_meal_plan, null);
        EditText nameInput = dialogView.findViewById(R.id.input_plan_name);
        Button startDateBtn = dialogView.findViewById(R.id.button_start_date);
        Button endDateBtn = dialogView.findViewById(R.id.button_end_date);

        // Pre-fill with current values
        TextView titleView = findViewById(R.id.planTitle);
        nameInput.setText(titleView.getText().toString());
        startDateBtn.setText(planStartDate);
        endDateBtn.setText(planEndDate);

        startDateBtn.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (picker, year, month, day) -> {
                startDateBtn.setText(String.format("%02d-%02d-%02d", day, month + 1, year % 100));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        endDateBtn.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (picker, year, month, day) -> {
                endDateBtn.setText(String.format("%02d-%02d-%02d", day, month + 1, year % 100));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Meal Plan")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                nameInput.setError("Required");
                return;
            }

            DatabaseReference planRef = database.getReference("users").child(uid)
                    .child("mealplans").child(mealPlanId);
            planRef.child("name").setValue(name);
            planRef.child("startDate").setValue(startDateBtn.getText().toString());
            planRef.child("endDate").setValue(endDateBtn.getText().toString());
            dialog.dismiss();
            Toast.makeText(this, "Meal plan updated", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Loads the user's recipes from Firebase, then shows the add meal dialog.
     * The dialog lets the user pick a date, meal type, and recipe.
     */
    private void showAddMealDialog() {
        if (planStartDate == null || planEndDate == null) return;

        DatabaseReference recipesRef = database.getReference("users").child(uid).child("recipes");
        recipesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ArrayList<String> recipeTitles = new ArrayList<>();
                ArrayList<String> recipeIds = new ArrayList<>();

                for (DataSnapshot recipe : snapshot.getChildren()) {
                    String title = recipe.child("title").getValue(String.class);
                    if (title != null) {
                        recipeTitles.add(title);
                        recipeIds.add(recipe.getKey());
                    }
                }

                if (recipeTitles.isEmpty()) {
                    Toast.makeText(MealPlannerActivity.this,
                            "Add some recipes first!", Toast.LENGTH_SHORT).show();
                    return;
                }

                showAddMealDialogWithRecipes(recipeTitles, recipeIds);
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }

    private void showAddMealDialogWithRecipes(ArrayList<String> recipeTitles, ArrayList<String> recipeIds) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_meal, null);
        Button dateBtn = dialogView.findViewById(R.id.button_meal_date);
        Spinner mealTypeSpinner = dialogView.findViewById(R.id.spinner_meal_type);
        Spinner recipeSpinner = dialogView.findViewById(R.id.spinner_recipe);

        // Default to the plan's start date
        dateBtn.setText(planStartDate);

        // Date picker constrained to the plan's date range
        dateBtn.setOnClickListener(v -> {
            try {
                Calendar startCal = Calendar.getInstance();
                startCal.setTime(dateFormat.parse(planStartDate));

                DatePickerDialog dpd = new DatePickerDialog(this, (picker, year, month, day) -> {
                    dateBtn.setText(String.format("%02d-%02d-%02d", day, month + 1, year % 100));
                }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH),
                        startCal.get(Calendar.DAY_OF_MONTH));

                dpd.getDatePicker().setMinDate(dateFormat.parse(planStartDate).getTime());
                dpd.getDatePicker().setMaxDate(dateFormat.parse(planEndDate).getTime());
                dpd.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Meal type spinner
        String[] mealTypes = {"Breakfast", "Lunch", "Dinner", "Snack"};
        mealTypeSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, mealTypes));

        // Recipe spinner
        recipeSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, recipeTitles));

        new AlertDialog.Builder(this)
                .setTitle("Add Meal")
                .setView(dialogView)
                .setPositiveButton("Add", (d, w) -> {
                    String date = dateBtn.getText().toString();
                    String mealType = mealTypeSpinner.getSelectedItem().toString();
                    int recipeIndex = recipeSpinner.getSelectedItemPosition();
                    String recipeTitle = recipeTitles.get(recipeIndex);
                    String recipeId = recipeIds.get(recipeIndex);

                    // Save the meal to Firebase under this plan
                    DatabaseReference mealsRef = database.getReference("users").child(uid)
                            .child("mealplans").child(mealPlanId).child("meals");
                    DatabaseReference newMeal = mealsRef.push();
                    newMeal.child("date").setValue(date);
                    newMeal.child("mealType").setValue(mealType);
                    newMeal.child("recipeId").setValue(recipeId);
                    newMeal.child("recipeTitle").setValue(recipeTitle);

                    Toast.makeText(this, "Meal added", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
