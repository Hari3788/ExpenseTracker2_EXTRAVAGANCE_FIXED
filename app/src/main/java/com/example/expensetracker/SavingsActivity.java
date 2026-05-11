package com.example.expensetracker;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SavingsActivity extends AppCompatActivity {

    private static final String[] CATEGORIES = {
            "Food", "Transport", "Shopping", "Bills", "Health", "Entertainment", "Other"
    };

    private DatabaseHelper db;
    private String         currentMonth;
    private TextView       tvTotalPotential, tvMonthSaved, tvGoalStatus;
    private EditText       etSavingsGoal;
    private LinearLayout   llCategoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Savings Plan");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db           = new DatabaseHelper(this);
        currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        tvTotalPotential = findViewById(R.id.tvTotalPotential);
        tvMonthSaved     = findViewById(R.id.tvMonthSaved);
        tvGoalStatus     = findViewById(R.id.tvGoalStatus);
        etSavingsGoal    = findViewById(R.id.etSavingsGoal);
        llCategoryList   = findViewById(R.id.llCategoryList);

        // Pre-fill saved goal
        double savedGoal = db.getSavingsGoal();
        if (savedGoal > 0) {
            etSavingsGoal.setText(String.valueOf((int) savedGoal));
        }

        findViewById(R.id.btnSetGoal).setOnClickListener(v -> saveGoal());

        buildAnalysis();
    }

    private void saveGoal() {
        String val = etSavingsGoal.getText().toString().trim();
        if (val.isEmpty()) {
            Toast.makeText(this, "Enter a savings goal amount", Toast.LENGTH_SHORT).show();
            return;
        }
        double goal = Double.parseDouble(val);
        db.setSavingsGoal(goal);
        Toast.makeText(this, "Savings goal saved!", Toast.LENGTH_SHORT).show();
        buildAnalysis();
    }

    private void buildAnalysis() {
        llCategoryList.removeAllViews();

        double totalPotentialSaving = 0;
        double monthIncome  = db.getMonthlyIncomeTotal(currentMonth);
        double monthExpenses = db.getMonthlyExpenseTotal(currentMonth);
        double monthSaved   = monthIncome - monthExpenses;

        for (String category : CATEGORIES) {
            double spent = db.getMonthlySpendByCategory(category, currentMonth);
            if (spent <= 0) continue;

            double avg       = db.getHistoricalAvgByCategory(category, currentMonth);
            int    pastCount = db.getPastMonthCountForCategory(category, currentMonth);

            double saving = 0;
            String status;
            int    cardColor;

            if (pastCount < 1 || avg <= 0) {
                // No history — just show what was spent
                status    = String.format("Spent ₹%.0f  (no prior data to compare)", spent);
                cardColor = android.R.color.white;
            } else {
                double pct = (spent / avg) * 100;
                saving     = Math.max(0, spent - avg);

                if (spent >= avg * 1.5) {
                    status    = String.format("⚠ Extravagant! ₹%.0f spent vs ₹%.0f avg  →  save ₹%.0f",
                            spent, avg, saving);
                    cardColor = R.color.alert_red_bg;
                } else if (spent >= avg * 1.3) {
                    status    = String.format("▲ Running high: ₹%.0f spent vs ₹%.0f avg  →  save ₹%.0f",
                            spent, avg, saving);
                    cardColor = R.color.alert_orange_bg;
                } else if (spent <= avg * 0.9) {
                    status    = String.format("✓ Great! ₹%.0f spent vs ₹%.0f avg  (%.0f%% of normal)",
                            spent, avg, pct);
                    cardColor = R.color.alert_green_bg;
                } else {
                    status    = String.format("~ Normal: ₹%.0f spent vs ₹%.0f avg (%.0f%%)",
                            spent, avg, pct);
                    cardColor = android.R.color.white;
                }
            }

            totalPotentialSaving += saving;
            addCategoryCard(category, status, cardColor);
        }

        // Summary
        tvMonthSaved.setText(String.format("This month: Income ₹%.0f  −  Expenses ₹%.0f  =  Saved ₹%.0f",
                monthIncome, monthExpenses, monthSaved));

        if (totalPotentialSaving > 0) {
            tvTotalPotential.setText(String.format(
                    "Potential savings if you cut high categories: ₹%.0f", totalPotentialSaving));
            tvTotalPotential.setVisibility(View.VISIBLE);
        } else {
            tvTotalPotential.setVisibility(View.GONE);
        }

        // Goal status
        double goal = db.getSavingsGoal();
        if (goal > 0) {
            double remaining = goal - monthSaved;
            if (remaining <= 0) {
                tvGoalStatus.setText(String.format(
                        "Goal of ₹%.0f reached! You've saved ₹%.0f this month.", goal, monthSaved));
                tvGoalStatus.setTextColor(getResources().getColor(R.color.income_green, null));
            } else {
                tvGoalStatus.setText(String.format(
                        "Goal: ₹%.0f/month. Need ₹%.0f more savings this month.", goal, remaining));
                tvGoalStatus.setTextColor(getResources().getColor(R.color.budget_warning, null));
            }
            tvGoalStatus.setVisibility(View.VISIBLE);
        } else {
            tvGoalStatus.setVisibility(View.GONE);
        }
    }

    private void addCategoryCard(String category, String statusText, int bgColorRes) {
        // Inflate a simple card programmatically
        CardView card = new CardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dpToPx(10));
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(8));
        card.setCardElevation(dpToPx(2));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(14);
        inner.setPadding(pad, pad, pad, pad);

        try {
            inner.setBackgroundColor(getResources().getColor(bgColorRes, null));
        } catch (Exception ignored) {}

        TextView tvCat = new TextView(this);
        tvCat.setText(category);
        tvCat.setTextSize(15);
        tvCat.setTypeface(null, android.graphics.Typeface.BOLD);
        tvCat.setTextColor(getResources().getColor(R.color.text_dark, null));

        TextView tvStatus = new TextView(this);
        tvStatus.setText(statusText);
        tvStatus.setTextSize(13);
        tvStatus.setTextColor(getResources().getColor(R.color.text_secondary, null));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dpToPx(4), 0, 0);
        tvStatus.setLayoutParams(p);

        inner.addView(tvCat);
        inner.addView(tvStatus);
        card.addView(inner);
        llCategoryList.addView(card);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
