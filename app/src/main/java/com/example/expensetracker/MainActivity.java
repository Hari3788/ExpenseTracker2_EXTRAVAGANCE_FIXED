package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity
        implements AddExpenseDialog.OnExpenseSavedListener,
                   ExpenseAdapter.OnItemClickListener {

    private DatabaseHelper    db;
    private ExpenseAdapter    adapter;
    private TextView          tvIncome, tvExpenses, tvBalance;
    private TextView          tvSavingRate, tvSavingsGoal, tvMonthLabel;
    private BudgetAlertHelper alertHelper;
    private String            currentMonth;
    private String            currentMonthLabel;
    private List<Expense>     allExpenses = new ArrayList<>();
    private String            currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Current month key e.g. "2026-04" and label e.g. "April 2026"
        currentMonth      = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        currentMonthLabel = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date());

        db          = new DatabaseHelper(this);
        alertHelper = new BudgetAlertHelper(this);

        tvIncome      = findViewById(R.id.tvIncome);
        tvExpenses    = findViewById(R.id.tvExpenses);
        tvBalance     = findViewById(R.id.tvBalance);
        tvSavingRate  = findViewById(R.id.tvSavingRate);
        tvSavingsGoal = findViewById(R.id.tvSavingsGoal);
        tvMonthLabel  = findViewById(R.id.tvMonthLabel);

        tvMonthLabel.setText(currentMonthLabel);

        // Tabs
        TabLayout tabs = findViewById(R.id.tabLayout);
        tabs.addTab(tabs.newTab().setText("All"));
        tabs.addTab(tabs.newTab().setText("Income"));
        tabs.addTab(tabs.newTab().setText("Expenses"));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentFilter = "ALL";     break;
                    case 1: currentFilter = "INCOME";  break;
                    case 2: currentFilter = "EXPENSE"; break;
                }
                filterList();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        RecyclerView rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        allExpenses = db.getAllExpenses();
        adapter     = new ExpenseAdapter(allExpenses, this);
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showAddDialog(null));

        updateSummary();
    }

    private void filterList() {
        List<Expense> filtered = new ArrayList<>();
        for (Expense e : allExpenses) {
            if (currentFilter.equals("ALL") || e.getType().equals(currentFilter))
                filtered.add(e);
        }
        adapter.updateList(filtered);
    }

    private void showAddDialog(Expense expenseToEdit) {
        AddExpenseDialog dialog = new AddExpenseDialog();
        if (expenseToEdit != null) dialog.setExpenseToEdit(expenseToEdit);
        dialog.setOnExpenseSavedListener(this);
        dialog.show(getSupportFragmentManager(), "AddExpense");
    }

    @Override
    public void onExpenseSaved(Expense expense) {
        if (expense.getId() == 0) db.insertExpense(expense);
        else                      db.updateExpense(expense);
        refreshList();
        if ("EXPENSE".equals(expense.getType())) {
            alertHelper.checkBudget(expense.getCategory(), db);
            runExtravaganceCheck(expense);
        }
    }

    private void runExtravaganceCheck(Expense expense) {
        // Always use currentMonth — expense.getMonth() can be empty for older entries
        String monthKey = currentMonth;

        double monthlyTotal  = db.getMonthlySpendByCategory(expense.getCategory(), monthKey);
        double historicalAvg = db.getHistoricalAvgByCategory(expense.getCategory(), monthKey);
        int    pastMonths    = db.getPastMonthCountForCategory(expense.getCategory(), monthKey);

        ExtravaganceAnalyzer.Result result =
                ExtravaganceAnalyzer.analyze(expense, monthlyTotal, historicalAvg, pastMonths);

        String tip = ExtravaganceAnalyzer.getSavingsTip(result);
        if (tip != null) {
            String title;
            if (result.verdict == ExtravaganceAnalyzer.Verdict.SINGLE_EXTRAVAGANT) {
                title = "⚠️ High Spend Detected!";
            } else {
                title = "📈 Monthly Spending is High!";
            }
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(tip + "\n\nWant to see your full savings plan?")
                    .setPositiveButton("View Savings", (d, w) ->
                            startActivity(new Intent(this, SavingsActivity.class)))
                    .setNegativeButton("Dismiss", null)
                    .show();
        }
    }

    @Override public void onEdit(Expense expense) { showAddDialog(expense); }

    @Override
    public void onDelete(Expense expense) {
        new AlertDialog.Builder(this)
                .setTitle("Delete?")
                .setMessage("Delete \"" + expense.getTitle() + "\"?")
                .setPositiveButton("Delete", (d, w) -> {
                    db.deleteExpense(expense.getId());
                    refreshList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshList() {
        allExpenses = db.getAllExpenses();
        filterList();
        updateSummary();
    }

    private void updateSummary() {
        // ── THIS MONTH ONLY ──────────────────────────────────
        double monthIncome  = db.getMonthlyIncomeTotal(currentMonth);
        double monthExpense = db.getMonthlyExpenseTotal(currentMonth);
        double monthBalance = monthIncome - monthExpense;
        double savingRate   = (monthIncome > 0) ? (monthBalance / monthIncome) * 100 : 0;

        tvIncome.setText(String.format("₹%.0f", monthIncome));
        tvExpenses.setText(String.format("₹%.0f", monthExpense));
        tvBalance.setText(String.format("₹%.0f", monthBalance));

        // Balance color
        tvBalance.setTextColor(monthBalance >= 0
                ? getResources().getColor(R.color.income_green, null)
                : getResources().getColor(R.color.expense_red, null));

        // Saving rate
        if (monthIncome > 0) {
            tvSavingRate.setText(String.format(
                    "Saving rate: %.1f%%  (₹%.0f saved)", savingRate, Math.max(0, monthBalance)));
            tvSavingRate.setTextColor(savingRate >= 20
                    ? getResources().getColor(R.color.income_green, null)
                    : savingRate >= 0
                    ? 0xFFBBDEFB
                    : getResources().getColor(R.color.expense_red, null));
        } else {
            tvSavingRate.setText("Add this month's income to see saving rate");
            tvSavingRate.setTextColor(0xFFBBDEFB);
        }

        // Savings goal progress
        double goal = db.getSavingsGoal();
        if (goal > 0) {
            double remaining = goal - monthBalance;
            if (remaining <= 0) {
                tvSavingsGoal.setText("Goal ✓ ₹" + String.format("%.0f", monthBalance)
                        + " / ₹" + String.format("%.0f", goal));
                tvSavingsGoal.setTextColor(getResources().getColor(R.color.income_green, null));
            } else {
                tvSavingsGoal.setText("Goal: ₹" + String.format("%.0f", Math.max(0, monthBalance))
                        + " / ₹" + String.format("%.0f", goal)
                        + "  (₹" + String.format("%.0f", remaining) + " more)");
                tvSavingsGoal.setTextColor(0xFFBBDEFB);
            }
            tvSavingsGoal.setVisibility(android.view.View.VISIBLE);
        } else {
            tvSavingsGoal.setVisibility(android.view.View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Charts");
        menu.add(0, 2, 1, "Budget");
        menu.add(0, 3, 2, "Savings Plan");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if      (id == 1) startActivity(new Intent(this, ChartsActivity.class));
        else if (id == 2) startActivity(new Intent(this, BudgetActivity.class));
        else if (id == 3) startActivity(new Intent(this, SavingsActivity.class));
        return true;
    }
}
