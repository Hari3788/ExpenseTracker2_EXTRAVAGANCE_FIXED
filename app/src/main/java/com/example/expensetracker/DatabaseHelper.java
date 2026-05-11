package com.example.expensetracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "expense_tracker.db";
    private static final int    DB_VERSION = 2;

    // Expenses table
    public static final String TABLE_EXPENSES = "expenses";
    public static final String COL_ID         = "id";
    public static final String COL_TITLE      = "title";
    public static final String COL_AMOUNT     = "amount";
    public static final String COL_CATEGORY   = "category";
    public static final String COL_DATE       = "date";
    public static final String COL_MONTH      = "month";   // NEW: "YYYY-MM"
    public static final String COL_TYPE       = "type";
    public static final String COL_NOTE       = "note";

    // Budget table
    public static final String TABLE_BUDGET  = "budgets";
    public static final String COL_CAT_NAME  = "category_name";
    public static final String COL_LIMIT     = "budget_limit";

    // Settings table (savings goal)
    public static final String TABLE_SETTINGS = "settings";
    public static final String COL_KEY        = "setting_key";
    public static final String COL_VALUE      = "setting_value";
    public static final String KEY_SAVINGS_GOAL = "savings_goal";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_EXPENSES + " (" +
                COL_ID       + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE    + " TEXT NOT NULL, " +
                COL_AMOUNT   + " REAL NOT NULL, " +
                COL_CATEGORY + " TEXT, " +
                COL_DATE     + " TEXT, " +
                COL_MONTH    + " TEXT, " +
                COL_TYPE     + " TEXT, " +
                COL_NOTE     + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_BUDGET + " (" +
                COL_CAT_NAME + " TEXT PRIMARY KEY, " +
                COL_LIMIT    + " REAL)");

        db.execSQL("CREATE TABLE " + TABLE_SETTINGS + " (" +
                COL_KEY   + " TEXT PRIMARY KEY, " +
                COL_VALUE + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add month column to existing table (migration)
            try {
                db.execSQL("ALTER TABLE " + TABLE_EXPENSES + " ADD COLUMN " + COL_MONTH + " TEXT");
                // Backfill month from existing date values
                db.execSQL("UPDATE " + TABLE_EXPENSES +
                        " SET " + COL_MONTH + " = substr(" + COL_DATE + ", 1, 7)" +
                        " WHERE " + COL_DATE + " IS NOT NULL AND " + COL_DATE + " != ''");
            } catch (Exception ignored) {}

            // Create settings table
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS + " (" +
                    COL_KEY   + " TEXT PRIMARY KEY, " +
                    COL_VALUE + " TEXT)");
        }
    }

    // ── INSERT ──────────────────────────────────────────────
    public long insertExpense(Expense e) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE,    e.getTitle());
        cv.put(COL_AMOUNT,   e.getAmount());
        cv.put(COL_CATEGORY, e.getCategory());
        cv.put(COL_DATE,     e.getDate());
        cv.put(COL_MONTH,    e.getMonth());
        cv.put(COL_TYPE,     e.getType());
        cv.put(COL_NOTE,     e.getNote());
        long id = db.insert(TABLE_EXPENSES, null, cv);
        db.close();
        return id;
    }

    // ── GET ALL ─────────────────────────────────────────────
    public List<Expense> getAllExpenses() {
        List<Expense> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT * FROM " + TABLE_EXPENSES + " ORDER BY " + COL_DATE + " DESC", null);
        if (c.moveToFirst()) {
            do {
                Expense e = new Expense(
                        c.getString(c.getColumnIndexOrThrow(COL_TITLE)),
                        c.getDouble(c.getColumnIndexOrThrow(COL_AMOUNT)),
                        c.getString(c.getColumnIndexOrThrow(COL_CATEGORY)),
                        c.getString(c.getColumnIndexOrThrow(COL_DATE)),
                        c.getString(c.getColumnIndexOrThrow(COL_TYPE)),
                        c.getString(c.getColumnIndexOrThrow(COL_NOTE))
                );
                e.setId(c.getInt(c.getColumnIndexOrThrow(COL_ID)));
                list.add(e);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }

    // ── UPDATE ──────────────────────────────────────────────
    public int updateExpense(Expense e) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE,    e.getTitle());
        cv.put(COL_AMOUNT,   e.getAmount());
        cv.put(COL_CATEGORY, e.getCategory());
        cv.put(COL_DATE,     e.getDate());
        cv.put(COL_MONTH,    e.getMonth());
        cv.put(COL_TYPE,     e.getType());
        cv.put(COL_NOTE,     e.getNote());
        int rows = db.update(TABLE_EXPENSES, cv, COL_ID + "=?",
                new String[]{String.valueOf(e.getId())});
        db.close();
        return rows;
    }

    // ── DELETE ──────────────────────────────────────────────
    public void deleteExpense(int id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_EXPENSES, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    // ── TOTALS ──────────────────────────────────────────────
    public double getTotalByType(String type) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES +
                        " WHERE " + COL_TYPE + "=?", new String[]{type});
        double total = 0;
        if (c.moveToFirst()) total = c.getDouble(0);
        c.close();
        db.close();
        return total;
    }

    // ── CATEGORY TOTALS (pie chart) ──────────────────────────
    public Cursor getCategoryTotals() {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
                "SELECT " + COL_CATEGORY + ", SUM(" + COL_AMOUNT + ") as total " +
                        "FROM " + TABLE_EXPENSES + " WHERE " + COL_TYPE + "='EXPENSE' " +
                        "GROUP BY " + COL_CATEGORY, null);
    }

    // ── MONTHLY TOTAL BY TYPE (bar chart) ────────────────────
    public double getMonthlyTotalByType(String type, String yearMonth) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES +
                        " WHERE " + COL_TYPE + "=? AND " + COL_MONTH + "=?",
                new String[]{type, yearMonth});
        double total = 0;
        if (c.moveToFirst()) total = c.getDouble(0);
        c.close();
        db.close();
        return total;
    }

    // ── MONTHLY SPEND BY CATEGORY ────────────────────────────
    public double getMonthlySpendByCategory(String category, String yearMonth) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT SUM(" + COL_AMOUNT + ") FROM " + TABLE_EXPENSES +
                        " WHERE " + COL_CATEGORY + "=? AND " + COL_TYPE + "='EXPENSE'" +
                        " AND " + COL_MONTH + "=?",
                new String[]{category, yearMonth});
        double total = 0;
        if (c.moveToFirst()) total = c.getDouble(0);
        c.close();
        db.close();
        return total;
    }

    // ── HISTORICAL AVERAGE SPEND BY CATEGORY ─────────────────
    // Returns average monthly spend for this category EXCLUDING currentMonth
    public double getHistoricalAvgByCategory(String category, String currentMonth) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT AVG(monthly_total) FROM (" +
                        "  SELECT SUM(" + COL_AMOUNT + ") AS monthly_total" +
                        "  FROM " + TABLE_EXPENSES +
                        "  WHERE " + COL_CATEGORY + "=? AND " + COL_TYPE + "='EXPENSE'" +
                        "    AND " + COL_MONTH + " != ? AND " + COL_MONTH + " IS NOT NULL" +
                        "  GROUP BY " + COL_MONTH +
                        ")",
                new String[]{category, currentMonth});
        double avg = 0;
        if (c.moveToFirst()) avg = c.getDouble(0);
        c.close();
        db.close();
        return avg;
    }

    // ── COUNT PAST MONTHS FOR CATEGORY ───────────────────────
    public int getPastMonthCountForCategory(String category, String currentMonth) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(DISTINCT " + COL_MONTH + ") FROM " + TABLE_EXPENSES +
                        " WHERE " + COL_CATEGORY + "=? AND " + COL_TYPE + "='EXPENSE'" +
                        " AND " + COL_MONTH + " != ? AND " + COL_MONTH + " IS NOT NULL",
                new String[]{category, currentMonth});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        db.close();
        return count;
    }

    // ── TOTAL MONTHLY EXPENSES ────────────────────────────────
    public double getMonthlyExpenseTotal(String yearMonth) {
        return getMonthlyTotalByType("EXPENSE", yearMonth);
    }

    // ── TOTAL MONTHLY INCOME ──────────────────────────────────
    public double getMonthlyIncomeTotal(String yearMonth) {
        return getMonthlyTotalByType("INCOME", yearMonth);
    }

    // ── BUDGET ───────────────────────────────────────────────
    public void setBudget(String category, double limit) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_CAT_NAME, category);
        cv.put(COL_LIMIT, limit);
        db.insertWithOnConflict(TABLE_BUDGET, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public double getBudget(String category) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COL_LIMIT + " FROM " + TABLE_BUDGET +
                        " WHERE " + COL_CAT_NAME + "=?", new String[]{category});
        double limit = 0;
        if (c.moveToFirst()) limit = c.getDouble(0);
        c.close();
        db.close();
        return limit;
    }

    // ── SAVINGS GOAL ─────────────────────────────────────────
    public void setSavingsGoal(double goal) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_KEY,   KEY_SAVINGS_GOAL);
        cv.put(COL_VALUE, String.valueOf(goal));
        db.insertWithOnConflict(TABLE_SETTINGS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public double getSavingsGoal() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT " + COL_VALUE + " FROM " + TABLE_SETTINGS +
                        " WHERE " + COL_KEY + "=?", new String[]{KEY_SAVINGS_GOAL});
        double goal = 0;
        if (c.moveToFirst()) {
            try { goal = Double.parseDouble(c.getString(0)); } catch (Exception ignored) {}
        }
        c.close();
        db.close();
        return goal;
    }

    // ── ALL DISTINCT MONTHS ───────────────────────────────────
    public List<String> getAllMonths() {
        List<String> months = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT DISTINCT " + COL_MONTH + " FROM " + TABLE_EXPENSES +
                        " WHERE " + COL_MONTH + " IS NOT NULL ORDER BY " + COL_MONTH + " DESC",
                null);
        if (c.moveToFirst()) {
            do { months.add(c.getString(0)); } while (c.moveToNext());
        }
        c.close();
        db.close();
        return months;
    }
}
