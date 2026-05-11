package com.example.expensetracker;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BudgetActivity extends AppCompatActivity {

    private static final String[] CATS = {
            "Food", "Transport", "Shopping", "Bills", "Health", "Entertainment"
    };
    private static final int[] EDIT_IDS = {
            R.id.etFood, R.id.etTransport, R.id.etShopping,
            R.id.etBills, R.id.etHealth, R.id.etEntertainment
    };
    private static final int[] HINT_IDS = {
            R.id.tvHintFood, R.id.tvHintTransport, R.id.tvHintShopping,
            R.id.tvHintBills, R.id.tvHintHealth, R.id.tvHintEntertainment
    };

    private DatabaseHelper db;
    private EditText[]     fields = new EditText[6];
    private TextView[]     hints  = new TextView[6];
    private String         currentMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Budget Limits");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db           = new DatabaseHelper(this);
        currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        for (int i = 0; i < 6; i++) {
            fields[i] = findViewById(EDIT_IDS[i]);
            hints[i]  = findViewById(HINT_IDS[i]);
        }

        loadBudgetsAndHints();

        findViewById(R.id.btnAutoSuggest).setOnClickListener(v -> autoFill());
        findViewById(R.id.btnSaveBudget).setOnClickListener(v -> save());
    }

    private void loadBudgetsAndHints() {
        for (int i = 0; i < 6; i++) {
            double saved = db.getBudget(CATS[i]);
            if (saved > 0) fields[i].setText(String.valueOf((int) saved));

            double avg  = db.getHistoricalAvgByCategory(CATS[i], currentMonth);
            int    past = db.getPastMonthCountForCategory(CATS[i], currentMonth);
            if (avg > 0 && past > 0) {
                hints[i].setText("Avg spend: ₹" + (int) avg + " → suggested limit: ₹" + (int)(avg * 0.9));
                hints[i].setTextColor(getResources().getColor(R.color.colorPrimary, null));
            } else {
                hints[i].setText("No past data");
                hints[i].setTextColor(0xFF888888);
            }
        }
    }

    private void autoFill() {
        boolean any = false;
        for (int i = 0; i < 6; i++) {
            double avg  = db.getHistoricalAvgByCategory(CATS[i], currentMonth);
            int    past = db.getPastMonthCountForCategory(CATS[i], currentMonth);
            if (avg > 0 && past > 0) {
                fields[i].setText(String.valueOf((int)(avg * 0.9)));
                any = true;
            }
        }
        Toast.makeText(this, any
                ? "Limits set to 90% of your avg — saves 10% automatically!"
                : "No history found. Add expenses from past months first.",
                Toast.LENGTH_LONG).show();
    }

    private void save() {
        for (int i = 0; i < 6; i++) {
            String val   = fields[i].getText().toString().trim();
            double limit = val.isEmpty() ? 0 : Double.parseDouble(val);
            db.setBudget(CATS[i], limit);
        }
        Toast.makeText(this, "Budgets saved!", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
