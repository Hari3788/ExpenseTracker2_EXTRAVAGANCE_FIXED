package com.example.expensetracker;

import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ChartsActivity extends AppCompatActivity {

    private PieChart       pieChart;
    private BarChart       barChart;
    private DatabaseHelper db;
    private String         currentMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Charts & Analytics");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db           = new DatabaseHelper(this);
        currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);

        setupSavingsSummary();
        setupPieChart();
        setupBarChart();
    }

    private void setupSavingsSummary() {
        // This month
        double monthIncome  = db.getMonthlyIncomeTotal(currentMonth);
        double monthExpense = db.getMonthlyExpenseTotal(currentMonth);
        double monthSaved   = monthIncome - monthExpense;
        double monthPct     = (monthIncome > 0) ? (monthSaved / monthIncome) * 100 : 0;

        // All time - show month by month history from bar chart data
        TextView tvThisMonth = findViewById(R.id.tvThisMonthSaving);
        if (monthIncome <= 0) {
            tvThisMonth.setText("No income recorded this month\nAdd income to see saving rate");
            tvThisMonth.setTextColor(0xFF888888);
        } else if (monthSaved >= 0) {
            tvThisMonth.setText(String.format(
                    "Saved ₹%.0f out of ₹%.0f income\nSaving rate: %.1f%%",
                    monthSaved, monthIncome, monthPct));
            tvThisMonth.setTextColor(monthPct >= 20
                    ? getResources().getColor(R.color.income_green, null)
                    : getResources().getColor(R.color.budget_warning, null));
        } else {
            tvThisMonth.setText(String.format(
                    "Overspent by ₹%.0f this month!\nExpenses exceed income",
                    Math.abs(monthSaved)));
            tvThisMonth.setTextColor(getResources().getColor(R.color.expense_red, null));
        }

        // Monthly savings history (last 6 months)
        StringBuilder history = new StringBuilder();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat keyFmt   = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        SimpleDateFormat labelFmt = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        double totalSavedAllTime = 0;
        int monthsWithData = 0;

        for (int i = 5; i >= 0; i--) {
            Calendar m = (Calendar) cal.clone();
            m.add(Calendar.MONTH, -i);
            String key   = keyFmt.format(m.getTime());
            String label = labelFmt.format(m.getTime());
            double inc = db.getMonthlyIncomeTotal(key);
            double exp = db.getMonthlyExpenseTotal(key);
            double sav = inc - exp;
            double pct = (inc > 0) ? (sav / inc) * 100 : 0;

            if (inc > 0 || exp > 0) {
                totalSavedAllTime += Math.max(0, sav);
                monthsWithData++;
                history.append(label).append(": ");
                if (inc > 0) {
                    history.append(String.format("Saved ₹%.0f (%.0f%%)", sav, pct));
                } else {
                    history.append(String.format("Spent ₹%.0f (no income)", exp));
                }
                history.append("\n");
            }
        }

        TextView tvOverall = findViewById(R.id.tvOverallSaving);
        if (monthsWithData > 0) {
            tvOverall.setText("Last 6 months history:\n" + history.toString().trim()
                    + "\n\nTotal saved: ₹" + String.format("%.0f", totalSavedAllTime));
        } else {
            tvOverall.setText("No data yet. Add income and expenses to see history.");
        }
        tvOverall.setTextColor(0xFF333333);

        // Goal
        double goal = db.getSavingsGoal();
        TextView tvGoal = findViewById(R.id.tvGoalProgress);
        if (goal > 0) {
            double pctOfGoal = (monthIncome > 0) ? (monthSaved / goal) * 100 : 0;
            if (monthSaved >= goal) {
                tvGoal.setText(String.format("Goal of ₹%.0f ACHIEVED!\nSaved ₹%.0f this month",
                        goal, monthSaved));
                tvGoal.setTextColor(getResources().getColor(R.color.income_green, null));
            } else {
                tvGoal.setText(String.format("%.0f%% of ₹%.0f goal reached\nNeed ₹%.0f more",
                        Math.max(0, pctOfGoal), goal, Math.max(0, goal - monthSaved)));
                tvGoal.setTextColor(getResources().getColor(R.color.budget_warning, null));
            }
            findViewById(R.id.cardGoal).setVisibility(android.view.View.VISIBLE);
        } else {
            findViewById(R.id.cardGoal).setVisibility(android.view.View.GONE);
        }
    }

    private void setupPieChart() {
        List<PieEntry> entries = new ArrayList<>();
        Cursor c = db.getCategoryTotals();
        while (c.moveToNext()) {
            String cat   = c.getString(0);
            float  total = c.getFloat(1);
            if (total > 0) entries.add(new PieEntry(total, cat));
        }
        c.close();

        if (entries.isEmpty()) { pieChart.setNoDataText("No expense data yet"); return; }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(3f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));
        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);
        pieChart.setCenterText("Spending\nby category");
        pieChart.setCenterTextSize(12f);
        pieChart.getLegend().setEnabled(true);
        pieChart.animateY(1200);
        pieChart.invalidate();
    }

    private void setupBarChart() {
        String[] monthKeys   = new String[6];
        String[] monthLabels = new String[6];
        Calendar cal      = Calendar.getInstance();
        SimpleDateFormat keyFmt   = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        SimpleDateFormat labelFmt = new SimpleDateFormat("MMM",     Locale.getDefault());

        for (int i = 5; i >= 0; i--) {
            Calendar m = (Calendar) cal.clone();
            m.add(Calendar.MONTH, -i);
            monthKeys[5 - i]   = keyFmt.format(m.getTime());
            monthLabels[5 - i] = labelFmt.format(m.getTime());
        }

        List<BarEntry> incomeEntries  = new ArrayList<>();
        List<BarEntry> expenseEntries = new ArrayList<>();
        List<BarEntry> savingEntries  = new ArrayList<>();

        for (int i = 0; i < monthKeys.length; i++) {
            float income  = (float) db.getMonthlyTotalByType("INCOME",  monthKeys[i]);
            float expense = (float) db.getMonthlyTotalByType("EXPENSE", monthKeys[i]);
            float saved   = Math.max(0, income - expense);
            incomeEntries.add(new BarEntry(i, income));
            expenseEntries.add(new BarEntry(i, expense));
            savingEntries.add(new BarEntry(i, saved));
        }

        BarDataSet incomeSet  = new BarDataSet(incomeEntries,  "Income");
        BarDataSet expenseSet = new BarDataSet(expenseEntries, "Expense");
        BarDataSet savingSet  = new BarDataSet(savingEntries,  "Saved");

        incomeSet.setColor(Color.parseColor("#4CAF50"));
        expenseSet.setColor(Color.parseColor("#F44336"));
        savingSet.setColor(Color.parseColor("#2196F3"));
        incomeSet.setValueTextSize(9f);
        expenseSet.setValueTextSize(9f);
        savingSet.setValueTextSize(9f);

        BarData barData    = new BarData(incomeSet, expenseSet, savingSet);
        float   barWidth   = 0.22f;
        float   barSpace   = 0.03f;
        float   groupSpace = 0.28f;
        barData.setBarWidth(barWidth);

        barChart.setData(barData);
        barChart.groupBars(0f, groupSpace, barSpace);
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(monthKeys.length);

        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(true);
        barChart.animateY(1200);
        barChart.invalidate();
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
