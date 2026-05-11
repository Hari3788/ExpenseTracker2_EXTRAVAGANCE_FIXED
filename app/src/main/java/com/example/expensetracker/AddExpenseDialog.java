package com.example.expensetracker;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.Calendar;

public class AddExpenseDialog extends BottomSheetDialogFragment {

    public interface OnExpenseSavedListener {
        void onExpenseSaved(Expense expense);
    }

    private OnExpenseSavedListener listener;
    private Expense expenseToEdit = null;

    public void setExpenseToEdit(Expense e)              { this.expenseToEdit = e; }
    public void setOnExpenseSavedListener(OnExpenseSavedListener l) { this.listener = l; }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_add_expense, container, false);

        RadioGroup rgType  = view.findViewById(R.id.rgType);
        EditText etTitle   = view.findViewById(R.id.etTitle);
        EditText etAmount  = view.findViewById(R.id.etAmount);
        Spinner  spinCat   = view.findViewById(R.id.spinnerCategory);
        EditText etDate    = view.findViewById(R.id.etDate);
        EditText etNote    = view.findViewById(R.id.etNote);
        Button   btnSave   = view.findViewById(R.id.btnSave);

        String[] categories = {"Food", "Transport", "Shopping",
                "Bills", "Health", "Entertainment", "Other"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categories);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinCat.setAdapter(catAdapter);

        etDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(requireContext(),
                    (dp, y, m, d) -> etDate.setText(String.format("%04d-%02d-%02d", y, m + 1, d)),
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Pre-fill for editing
        if (expenseToEdit != null) {
            etTitle.setText(expenseToEdit.getTitle());
            etAmount.setText(String.valueOf(expenseToEdit.getAmount()));
            etDate.setText(expenseToEdit.getDate());
            etNote.setText(expenseToEdit.getNote());
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equals(expenseToEdit.getCategory()))
                    spinCat.setSelection(i);
            }
            if ("INCOME".equals(expenseToEdit.getType()))
                view.findViewById(R.id.rbIncome).performClick();
            btnSave.setText("Update Expense");
        }

        btnSave.setOnClickListener(v -> {
            String title  = etTitle.getText().toString().trim();
            String amtStr = etAmount.getText().toString().trim();
            String date   = etDate.getText().toString().trim();
            String note   = etNote.getText().toString().trim();
            String cat    = spinCat.getSelectedItem().toString();
            String type   = (rgType.getCheckedRadioButtonId() == R.id.rbIncome)
                    ? "INCOME" : "EXPENSE";

            if (title.isEmpty())  { etTitle.setError("Required");   return; }
            if (amtStr.isEmpty()) { etAmount.setError("Required");  return; }
            if (date.isEmpty())   { etDate.setError("Pick a date"); return; }

            double amount  = Double.parseDouble(amtStr);
            Expense expense = new Expense(title, amount, cat, date, type, note);
            if (expenseToEdit != null) expense.setId(expenseToEdit.getId());

            if (listener != null) listener.onExpenseSaved(expense);
            dismiss();
        });

        return view;
    }
}
