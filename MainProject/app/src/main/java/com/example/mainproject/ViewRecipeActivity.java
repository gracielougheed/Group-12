package com.example.mainproject;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    private FrameLayout timerOverlay;
    private final Map<String, TimerCard> activeTimerCards = new LinkedHashMap<>();

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
        tvPrepTime.setTextColor(Color.parseColor("#007E6E"));
        tvPrepTime.setPaintFlags(tvPrepTime.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        tvCookTime.setText(r.cookTimeValue + " " + (r.cookTimeUnit != null ? r.cookTimeUnit : ""));
        tvCookTime.setTextColor(Color.parseColor("#007E6E"));
        tvCookTime.setPaintFlags(tvCookTime.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

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

                ParsedTime pt = (ParsedTime) parseTimesFromText(step);
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

    private class TimerCard {
        View view;
        RecipeTimer timer;
        CountDownTimer countDown;
    }

    private void showOrAddTimer(String id, String label, long durationMillis) {

        // If timer already exists, bring it to front
        if (activeTimerCards.containsKey(id)) {
            TimerCard existing = activeTimerCards.get(id);
            existing.view.bringToFront();
            return;
        }

        // Create new timer model
        RecipeTimer timer = new RecipeTimer(id, label, durationMillis);

        // Inflate a new floating card
        LayoutInflater inflater = LayoutInflater.from(this);
        View cardView = inflater.inflate(R.layout.timer_floating_view, timerOverlay, false);

        // Bind UI elements
        TextView tvTitle = cardView.findViewById(R.id.tvTimerTitle);
        TextView tvTime = cardView.findViewById(R.id.tvTimerTime);
        Button btnStartPause = cardView.findViewById(R.id.btnStartPause);
        Button btnReset = cardView.findViewById(R.id.btnReset);
        ImageView btnClose = cardView.findViewById(R.id.btnCloseTimer);

        tvTitle.setText(label);
        tvTime.setText(formatTime(durationMillis));

        // Create TimerCard object
        TimerCard card = new TimerCard();
        card.view = cardView;
        card.timer = timer;

        // Add to overlay
        timerOverlay.addView(cardView);
        activeTimerCards.put(id, card);

        // Make draggable
        makeTimerDraggable(cardView);

        // Start/Pause logic
        btnStartPause.setOnClickListener(v -> {
            if (timer.isRunning) {
                pauseTimer(card, tvTime, btnStartPause);
            } else {
                startTimer(card, tvTime, btnStartPause);
            }
        });

        // Reset logic
        btnReset.setOnClickListener(v -> {
            resetTimer(card, tvTime, btnStartPause);
        });

        // Close logic
        btnClose.setOnClickListener(v -> {
            stopTimer(card);
            timerOverlay.removeView(cardView);
            activeTimerCards.remove(id);
        });
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

    private void startTimer(TimerCard card, TextView tvTime, Button btnStartPause) {
        RecipeTimer timer = card.timer;
        timer.isRunning = true;
        btnStartPause.setText("Pause");

        card.countDown = new CountDownTimer(timer.remainingMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timer.remainingMillis = millisUntilFinished;
                tvTime.setText(formatTime(millisUntilFinished));
            }

            @Override
            public void onFinish() {
                timer.remainingMillis = 0;
                timer.isRunning = false;
                tvTime.setText(formatTime(0));
                btnStartPause.setText("Start");
            }
        }.start();
    }

    private void pauseTimer(TimerCard card, TextView tvTime, Button btnStartPause) {
        card.timer.isRunning = false;
        btnStartPause.setText("Start");
        stopTimer(card);
    }

    private void resetTimer(TimerCard card, TextView tvTime, Button btnStartPause) {
        stopTimer(card);
        card.timer.remainingMillis = card.timer.totalMillis;
        card.timer.isRunning = false;
        tvTime.setText(formatTime(card.timer.remainingMillis));
        btnStartPause.setText("Start");
    }

    private void stopTimer(TimerCard card) {
        if (card.countDown != null) {
            card.countDown.cancel();
            card.countDown = null;
        }
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }


    @Override
    public void onBackPressed() {
        if (!activeTimerCards.isEmpty()) {
            showLeaveTimerWarning();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (!activeTimerCards.isEmpty()) {
            showLeaveTimerWarning();
            return true;
        }
        return super.onSupportNavigateUp();
    }

    private void showLeaveTimerWarning() {
        new AlertDialog.Builder(this)
                .setTitle("Active Timers")
                .setMessage("You have active timers. If you leave this screen, they will stop. Do you want to leave?")
                .setPositiveButton("Leave", (d, w) -> {

                    // Stop all timers
                    for (TimerCard card : activeTimerCards.values()) {
                        stopTimer(card);
                    }

                    // Remove all floating cards
                    timerOverlay.removeAllViews();
                    activeTimerCards.clear();

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

    private List<ParsedTime> parseTimesFromText(String text) {
        String lower = text.toLowerCase(Locale.US);
        List<ParsedTime> results = new ArrayList<>();

        // Convert unicode fractions to numeric
        lower = lower.replace("½", "1/2")
                .replace("¼", "1/4")
                .replace("¾", "3/4");

        // FRACTION pattern (1/2, 3/2, 1 1/2)
        String fraction = "(?:\\d+\\s+)?\\d+/\\d+";

        // DECIMAL pattern (1.5)
        String decimal = "\\d+\\.\\d+";

        // INTEGER pattern (1, 20)
        String integer = "\\d+";

        // Combined number pattern
        String number = "(" + fraction + "|" + decimal + "|" + integer + ")";

        // Units
        String hours = "(hour|hours|hr|hrs)";
        String minutes = "(minute|minutes|min|mins)";
        String seconds = "(second|seconds)";

        // Patterns
        Pattern hoursMinutes = Pattern.compile(number + "\\s+" + hours + "\\s+(?:and\\s+)?"
                + number + "\\s+" + minutes + "\\b");

        Pattern minutesSeconds = Pattern.compile(number + "\\s+" + minutes + "\\s+(?:and\\s+)?"
                + number + "\\s+" + seconds + "\\b");

        Pattern hoursSeconds = Pattern.compile(number + "\\s+" + hours + "\\s+(?:and\\s+)?"
                + number + "\\s+" + seconds + "\\b");

        Pattern hoursOnly = Pattern.compile(number + "\\s+" + hours + "\\b");

        Pattern minutesOnly = Pattern.compile(number + "\\s+" + minutes + "\\b");

        Pattern secondsOnly = Pattern.compile(number + "\\s+" + seconds + "\\b");

        // Helper to convert number string to double
        java.util.function.Function<String, Double> parseNum = (s) -> {
            if (s.contains("/")) {
                if (s.contains(" ")) {
                    // mixed fraction: "1 1/2"
                    String[] parts = s.split(" ");
                    double whole = Double.parseDouble(parts[0]);
                    String[] frac = parts[1].split("/");
                    return whole + (Double.parseDouble(frac[0]) / Double.parseDouble(frac[1]));
                } else {
                    // simple fraction: "1/2"
                    String[] frac = s.split("/");
                    return Double.parseDouble(frac[0]) / Double.parseDouble(frac[1]);
                }
            }
            return Double.parseDouble(s);
        };

        // Apply patterns (each may match multiple times)
        Matcher m;

        // hours + minutes
        m = hoursMinutes.matcher(lower);
        while (m.find()) {
            double h = parseNum.apply(m.group(1));
            double min = parseNum.apply(m.group(3));
            long millis = (long)((h * 60 + min) * 60_000L);
            ParsedTime pt = new ParsedTime();
            pt.millis = millis;
            pt.startIndex = m.start();
            pt.endIndex = m.end();
            results.add(pt);
        }

        // minutes + seconds
        m = minutesSeconds.matcher(lower);
        while (m.find()) {
            double min = parseNum.apply(m.group(1));
            double sec = parseNum.apply(m.group(3));
            long millis = (long)(min * 60_000L + sec * 1000L);
            ParsedTime pt = new ParsedTime();
            pt.millis = millis;
            pt.startIndex = m.start();
            pt.endIndex = m.end();
            results.add(pt);
        }

        // hours + seconds
        m = hoursSeconds.matcher(lower);
        while (m.find()) {
            double h = parseNum.apply(m.group(1));
            double sec = parseNum.apply(m.group(3));
            long millis = (long)(h * 60L * 60_000L + sec * 1000L);
            ParsedTime pt = new ParsedTime();
            pt.millis = millis;
            pt.startIndex = m.start();
            pt.endIndex = m.end();
            results.add(pt);
        }

        // hours only
        m = hoursOnly.matcher(lower);
        while (m.find()) {
            double h = parseNum.apply(m.group(1));
            long millis = (long)(h * 60L * 60_000L);
            ParsedTime pt = new ParsedTime();
            pt.millis = millis;
            pt.startIndex = m.start();
            pt.endIndex = m.end();
            results.add(pt);
        }

        // minutes only
        m = minutesOnly.matcher(lower);
        while (m.find()) {
            double min = parseNum.apply(m.group(1));
            long millis = (long)(min * 60_000L);
            ParsedTime pt = new ParsedTime();
            pt.millis = millis;
            pt.startIndex = m.start();
            pt.endIndex = m.end();
            results.add(pt);
        }

        // seconds only
        m = secondsOnly.matcher(lower);
        while (m.find()) {
            double sec = parseNum.apply(m.group(1));
            long millis = (long)(sec * 1000L);
            ParsedTime pt = new ParsedTime();
            pt.millis = millis;
            pt.startIndex = m.start();
            pt.endIndex = m.end();
            results.add(pt);
        }

        return results;
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