package com.example.mainproject;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mainproject.Recipes.EditRecipeActivity;
import com.example.mainproject.ShareRecipeActivity;
import com.example.mainproject.entities.Ingredient;
import com.example.mainproject.entities.Recipe;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ViewRecipeActivity loads and displays full recipe details from Firebase.
 * Always reads fresh data so edits are immediately reflected.
 */
public class ViewRecipeActivity extends AppCompatActivity {

    private static final String DB_URL =
            "https://cookbook-d313f-default-rtdb.europe-west1.firebasedatabase.app/";

    private String recipeId;
    private String ownerUid;

    // Header
    private TextView tvTitle;

    // Info row
    private TextView tvCategory, tvPrepTime, tvCookTime, tvServings, tvDifficulty, tvVisibility;

    // Sections
    private TextView tvTags, tvCookware;
    private LinearLayout layoutInstructions, layoutIngredients;

    // Buttons
    private Button btnEdit, btnShare, btnDelete;

    private ValueEventListener recipeListener;
    private DatabaseReference recipeRef;

    // Overlay + floating view
    private FrameLayout timerOverlay;
    private View timerView;
    private TextView tvTimerTitle, tvTimerTime;
    private Button btnStartPause, btnReset;
    private LinearLayout timerTabsContainer;
    private ImageView btnCloseTimer;

    // Timer state
    private final Map<String, RecipeTimer> timers = new LinkedHashMap<>();
    private RecipeTimer currentTimer = null;
    private CountDownTimer countDownTimer = null;
    private boolean hasActiveTimer = false;

    private Recipe recipe;


    private static class RecipeTimer {
        String id;          // e.g. "prep", "cook", "step_3"
        String label;       // e.g. "Prep Time", "Step 3"
        long totalMillis;   // original duration
        long remainingMillis;
        boolean isRunning;

        RecipeTimer(String id, String label, long totalMillis) {
            this.id = id;
            this.label = label;
            this.totalMillis = totalMillis;
            this.remainingMillis = totalMillis;
            this.isRunning = false;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recipe);

        recipeId = getIntent().getStringExtra("RECIPE_ID");
        ownerUid = getIntent().getStringExtra("OWNER_UID");

        String currentUid = FirebaseAuth.getInstance().getUid();

        // If no ownerUid provided, assume it's the current user's recipe
        if (ownerUid == null) {
            ownerUid = currentUid;
        }

        bindViews();

        // Hide owner-only buttons if viewing someone else's recipe
        boolean isOwner = ownerUid.equals(currentUid);
        btnEdit.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        btnShare.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        btnDelete.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        btnEdit.setOnClickListener(v -> openEditActivity());
        btnShare.setOnClickListener(v -> {
            Intent intent = new Intent(this, ShareRecipeActivity.class);
            intent.putExtra("RECIPE_ID", recipeId);
            startActivity(intent);
        });
        btnDelete.setOnClickListener(v -> confirmDelete());

        // Load from Firebase
        recipeRef = FirebaseDatabase.getInstance(DB_URL)
                .getReference("users").child(ownerUid).child("recipes").child(recipeId);

        listenForRecipe();
    }

    private void bindViews() {
        tvTitle       = findViewById(R.id.tvTitle);
        tvCategory    = findViewById(R.id.tvCategory);
        tvPrepTime    = findViewById(R.id.tvPrepTime);
        tvCookTime    = findViewById(R.id.tvCookTime);
        tvServings    = findViewById(R.id.tvServings);
        tvDifficulty  = findViewById(R.id.tvDifficulty);
        tvVisibility  = findViewById(R.id.tvVisibility);
        tvTags        = findViewById(R.id.tvTags);
        tvCookware    = findViewById(R.id.tvCookware);
        layoutInstructions = findViewById(R.id.layoutInstructions);
        layoutIngredients  = findViewById(R.id.layoutIngredients);
        btnEdit   = findViewById(R.id.btnEdit);
        btnShare  = findViewById(R.id.btnShare);
        btnDelete = findViewById(R.id.btnDelete);
        timerOverlay = findViewById(R.id.timerOverlay);
    }

    /** Attaches a realtime listener so the screen refreshes after an edit. */
    private void listenForRecipe() {
        recipeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recipe = snapshot.getValue(Recipe.class);
                if (recipe != null) {
                    populateUI(recipe);
                } else {
                    Toast.makeText(ViewRecipeActivity.this,
                            "Recipe not found.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ViewRecipeActivity.this,
                        "Failed to load recipe.", Toast.LENGTH_SHORT).show();
            }
        };
        recipeRef.addValueEventListener(recipeListener);
    }

    private void populateUI(Recipe r) {
        tvTitle.setText(r.title != null ? r.title : "");
        tvCategory.setText(r.category != null ? r.category : "");

        tvPrepTime.setText(r.prepTimeValue + " " + (r.prepTimeUnit != null ? r.prepTimeUnit : ""));
        tvCookTime.setText(r.cookTimeValue + " " + (r.cookTimeUnit != null ? r.cookTimeUnit : ""));
        tvServings.setText(String.valueOf(r.servingSize));

        // Difficulty stored as int 1-5 (or whatever your spinner maps to)
        tvDifficulty.setText("Difficulty: " + r.difficultyLevel);
        tvVisibility.setText(r.isPublic ? "Public" : "Private");

        // Tags
        if (r.tags != null && !r.tags.isEmpty()) {
            tvTags.setText(android.text.TextUtils.join(", ", r.tags));
        } else {
            tvTags.setText("None");
        }

        // Cookware
        if (r.cookware != null && !r.cookware.isEmpty()) {
            tvCookware.setText(android.text.TextUtils.join(", ", r.cookware));
        } else {
            tvCookware.setText("None");
        }

        tvPrepTime.setOnClickListener(v -> {
            long millis = recipe.prepTimeValue;
            if ("Hours".equalsIgnoreCase(recipe.prepTimeUnit)) {
                millis = recipe.prepTimeValue * 60L * 60_000L;
            } else {
                millis = recipe.prepTimeValue * 60_000L;
            }
            showOrAddTimer("prep", "Prep Time", millis);
        });

        tvCookTime.setOnClickListener(v -> {
            long millis;
            if ("Hours".equalsIgnoreCase(recipe.cookTimeUnit)) {
                millis = recipe.cookTimeValue * 60L * 60_000L;
            } else {
                millis = recipe.cookTimeValue * 60_000L;
            }
            showOrAddTimer("cook", "Cook Time", millis);
        });


        // Instructions
// Instructions
        layoutInstructions.removeAllViews();
        if (r.instructions != null) {
            for (int i = 0; i < recipe.instructions.size(); i++) {

                final int index = i;   // <-- REQUIRED FIX

                String step = recipe.instructions.get(index);
                String fullText = (index + 1) + ". " + step;

                TextView tv = new TextView(this);
                tv.setTextSize(14f);
                tv.setTextColor(Color.BLACK);

                ParsedTime pt = parseTimeFromText(step);
                if (pt != null) {
                    SpannableString spannable = new SpannableString(fullText);

                    int offset = (index + 1 + ". ").length();
                    int start = offset + pt.startIndex;
                    int end = offset + pt.endIndex;

                    ClickableSpan span = new ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            showOrAddTimer(
                                    "step_" + index,          // use index, not i
                                    "Step " + (index + 1),
                                    pt.millis
                            );
                        }

                        @Override
                        public void updateDrawState(@NonNull TextPaint ds) {
                            super.updateDrawState(ds);
                            ds.setColor(Color.parseColor("#007E6E"));
                            ds.setUnderlineText(true);
                        }
                    };

                    spannable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    tv.setText(spannable);
                    tv.setMovementMethod(LinkMovementMethod.getInstance());
                } else {
                    tv.setText(fullText);
                }

                layoutInstructions.addView(tv);
            }
        }

        // Ingredients
        layoutIngredients.removeAllViews();
        if (r.ingredients != null) {
            for (Ingredient ing : r.ingredients) {
                String line = ing.quantity + " " + ing.unit + " " + ing.name;
                if (ing.prep != null && !ing.prep.isEmpty()) line += " (" + ing.prep + ")";
                layoutIngredients.addView(makeBodyTextView("• " + line));
            }
        }
    }

    private TextView makeBodyTextView(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(15f);
        tv.setPadding(0, 6, 0, 6);
        return tv;
    }

    private void openEditActivity() {
        Intent intent = new Intent(this, EditRecipeActivity.class);
        intent.putExtra("RECIPE_ID", recipeId);
        startActivity(intent);
        // No finish() — user returns here; the realtime listener will refresh data automatically.
    }

    private void showOrAddTimer(String id, String label, long durationMillis) {
        // If timer already exists, just switch to it
        RecipeTimer existing = timers.get(id);
        if (existing == null) {
            RecipeTimer timer = new RecipeTimer(id, label, durationMillis);
            timers.put(id, timer);
            hasActiveTimer = true;
        }

        if (timerView == null) {
            inflateTimerView();
        }

        switchToTimer(id);
    }

    private void inflateTimerView() {
        LayoutInflater inflater = LayoutInflater.from(this);
        timerView = inflater.inflate(R.layout.timer_floating_view, timerOverlay, false);

        tvTimerTitle = timerView.findViewById(R.id.tvTimerTitle);
        tvTimerTime = timerView.findViewById(R.id.tvTimerTime);
        btnStartPause = timerView.findViewById(R.id.btnStartPause);
        btnReset = timerView.findViewById(R.id.btnReset);
        timerTabsContainer = timerView.findViewById(R.id.timerTabsContainer);
        btnCloseTimer = timerView.findViewById(R.id.btnCloseTimer);

        // Add to overlay
        timerOverlay.addView(timerView);

        // Make it draggable
        makeTimerDraggable(timerView);

        // Close button
        btnCloseTimer.setOnClickListener(v -> {
            stopCurrentTimer();
            timers.clear();
            currentTimer = null;
            hasActiveTimer = false;
            timerOverlay.removeView(timerView);
            timerView = null;
        });

        // Start/Pause
        btnStartPause.setOnClickListener(v -> {
            if (currentTimer == null) return;
            if (currentTimer.isRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        // Reset
        btnReset.setOnClickListener(v -> {
            if (currentTimer == null) return;
            resetTimer();
        });

        rebuildTimerTabs();
    }

    private void makeTimerDraggable(View view) {
        view.setOnTouchListener(new View.OnTouchListener() {
            float dX, dY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = v.getX() - event.getRawX();
                        dY = v.getY() - event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float newX = event.getRawX() + dX;
                        float newY = event.getRawY() + dY;

                        // Optional: clamp within screen bounds
                        v.setX(newX);
                        v.setY(newY);
                        return true;
                }
                return false;
            }
        });
    }

    private void rebuildTimerTabs() {
        timerTabsContainer.removeAllViews();

        for (RecipeTimer timer : timers.values()) {
            TextView tab = new TextView(this);
            tab.setText(timer.label);
            tab.setPadding(16, 8, 16, 8);
            tab.setTextSize(12f);
            tab.setBackgroundResource(
                    timer == currentTimer
                            ? android.R.color.darker_gray
                            : android.R.color.transparent
            );
            tab.setOnClickListener(v -> switchToTimer(timer.id));
            timerTabsContainer.addView(tab);
        }
    }

    private void switchToTimer(String id) {
        RecipeTimer timer = timers.get(id);
        if (timer == null) return;

        // Stop any running CountDownTimer
        stopCurrentTimer();

        currentTimer = timer;
        tvTimerTitle.setText(timer.label);
        updateTimerDisplay(timer.remainingMillis);

        // Update Start/Pause button text
        btnStartPause.setText(timer.isRunning ? "Pause" : "Start");

        rebuildTimerTabs();
    }

    private void startTimer() {
        if (currentTimer == null) return;

        currentTimer.isRunning = true;
        btnStartPause.setText("Pause");

        countDownTimer = new CountDownTimer(currentTimer.remainingMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                currentTimer.remainingMillis = millisUntilFinished;
                updateTimerDisplay(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                currentTimer.remainingMillis = 0;
                currentTimer.isRunning = false;
                updateTimerDisplay(0);
                btnStartPause.setText("Start");
                // Optional: sound/vibration/toast
            }
        }.start();
    }

    private void pauseTimer() {
        if (currentTimer == null) return;
        currentTimer.isRunning = false;
        btnStartPause.setText("Start");
        stopCurrentTimer();
    }

    private void resetTimer() {
        if (currentTimer == null) return;
        stopCurrentTimer();
        currentTimer.remainingMillis = currentTimer.totalMillis;
        currentTimer.isRunning = false;
        updateTimerDisplay(currentTimer.remainingMillis);
        btnStartPause.setText("Start");
    }

    private void stopCurrentTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void updateTimerDisplay(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        tvTimerTime.setText(String.format("%02d:%02d", minutes, seconds));
    }

    @Override
    public void onBackPressed() {
        if (hasActiveTimer) {
            showLeaveTimerWarning();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (hasActiveTimer) {
            showLeaveTimerWarning();
            return true;
        }
        return super.onSupportNavigateUp();
    }

    private void showLeaveTimerWarning() {
        new AlertDialog.Builder(this)
                .setTitle("Active Timer")
                .setMessage("You have an active timer. If you leave this screen, the timer will stop. Do you want to leave?")
                .setPositiveButton("Leave", (d, w) -> {
                    stopCurrentTimer();
                    timers.clear();
                    hasActiveTimer = false;
                    super.onBackPressed();
                })
                .setNegativeButton("Stay", null)
                .show();
    }

    private static class ParsedTime {
        long millis;
        int startIndex;
        int endIndex;
    }

    private ParsedTime parseTimeFromText(String text) {
        // Normalize
        String lower = text.toLowerCase(Locale.US);

        // Patterns:
        // 1) hours + minutes: "1 hour 30 minutes", "1h 30m", "1 hr 30 min"
        Pattern hoursMinutes = Pattern.compile(
                "(\\d+)\\s*(h|hr|hour|hours)\\s*(\\d+)\\s*(m|min|mins|minute|minutes)"
        );

        // 2) hours only: "1 hour", "2h", "2 hr"
        Pattern hoursOnly = Pattern.compile(
                "(\\d+)\\s*(h|hr|hour|hours)"
        );

        // 3) minutes only (including ranges): "10-12 minutes", "10–12 min", "10m"
        Pattern minutesRange = Pattern.compile(
                "(\\d+)\\s*[-–]\\s*\\d+\\s*(m|min|mins|minute|minutes)"
        );
        Pattern minutesOnly = Pattern.compile(
                "(\\d+)\\s*(m|min|mins|minute|minutes)"
        );

        Matcher m;

        // hours + minutes
        m = hoursMinutes.matcher(lower);
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            int min = Integer.parseInt(m.group(3));
            long millis = (h * 60L + min) * 60_000L;
            ParsedTime pt = new ParsedTime();
            pt.millis = millis;
            pt.startIndex = m.start();
            pt.endIndex = m.end();
            return pt;
        }

        // hours only
        m = hoursOnly.matcher(lower);
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            long millis = h * 60L * 60_000L;
            ParsedTime pt = new ParsedTime();
            pt.millis = millis;
            pt.startIndex = m.start();
            pt.endIndex = m.end();
            return pt;
        }

        // minutes range
        m = minutesRange.matcher(lower);
        if (m.find()) {
            int min = Integer.parseInt(m.group(1)); // first number
            long millis = min * 60_000L;
            ParsedTime pt = new ParsedTime();
            pt.millis = millis;
            pt.startIndex = m.start();
            pt.endIndex = m.end();
            return pt;
        }

        // minutes only
        m = minutesOnly.matcher(lower);
        if (m.find()) {
            int min = Integer.parseInt(m.group(1));
            long millis = min * 60_000L;
            ParsedTime pt = new ParsedTime();
            pt.millis = millis;
            pt.startIndex = m.start();
            pt.endIndex = m.end();
            return pt;
        }

        return null;
    }


    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Recipe")
                .setMessage("Are you sure you want to permanently delete this recipe?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRecipe())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecipe() {
        recipeRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Recipe deleted.", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Failed to delete recipe.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Go back arrow in header */
    public void goBack(View view) {
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recipeRef != null && recipeListener != null) {
            recipeRef.removeEventListener(recipeListener);
        }
    }
}