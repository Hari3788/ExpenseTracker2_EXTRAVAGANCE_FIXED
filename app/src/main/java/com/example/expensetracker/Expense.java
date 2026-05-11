package com.example.expensetracker;

public class Expense {
    private int    id;
    private String title;
    private double amount;
    private String category;
    private String date;    // "YYYY-MM-DD"
    private String month;   // "YYYY-MM"  (derived from date)
    private String type;    // "INCOME" or "EXPENSE"
    private String note;

    public Expense(String title, double amount, String category,
                   String date, String type, String note) {
        this.title    = title;
        this.amount   = amount;
        this.category = category;
        this.date     = date;
        this.month    = (date != null && date.length() >= 7) ? date.substring(0, 7) : "";
        this.type     = type;
        this.note     = note;
    }

    // Getters
    public int    getId()         { return id; }
    public String getTitle()      { return title; }
    public double getAmount()     { return amount; }
    public String getCategory()   { return category; }
    public String getDate()       { return date; }
    public String getMonth()      { return month; }
    public String getType()       { return type; }
    public String getNote()       { return note; }

    // Setters
    public void setId(int id)             { this.id = id; }
    public void setTitle(String title)    { this.title = title; }
    public void setAmount(double amount)  { this.amount = amount; }
    public void setCategory(String cat)   { this.category = cat; }
    public void setDate(String date)      {
        this.date  = date;
        this.month = (date != null && date.length() >= 7) ? date.substring(0, 7) : "";
    }
    public void setType(String type)      { this.type = type; }
    public void setNote(String note)      { this.note = note; }
}
