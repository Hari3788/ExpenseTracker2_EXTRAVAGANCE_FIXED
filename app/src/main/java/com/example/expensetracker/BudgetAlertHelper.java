package com.example.expensetracker;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BudgetAlertHelper {

    private final Context context;

    public BudgetAlertHelper(Context context) {
        this.context = context;
    }

    public void checkBudget(String category, DatabaseHelper db) {
        double limit = db.getBudget(category);
        if (limit <= 0) return;

        String yearMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        double spent   = db.getMonthlySpendByCategory(category, yearMonth);
        double percent = (spent / limit) * 100;

        if (spent >= limit) {
            new AlertDialog.Builder(context)
                    .setTitle("Budget Exceeded!")
                    .setMessage(String.format(
                            "You have exceeded your %s budget for this month.\n\n" +
                            "Budget:  ₹%.2f\nSpent:   ₹%.2f\nOver by: ₹%.2f",
                            category, limit, spent, spent - limit))
                    .setPositiveButton("OK", null)
                    .show();

        } else if (percent >= 80) {
            new AlertDialog.Builder(context)
                    .setTitle("Budget Warning")
                    .setMessage(String.format(
                            "You have used %.0f%% of your %s budget.\n\n" +
                            "Budget: ₹%.2f\nSpent:  ₹%.2f\nLeft:   ₹%.2f",
                            percent, category, limit, spent, limit - spent))
                    .setPositiveButton("OK", null)
                    .show();
        }
    }
}
