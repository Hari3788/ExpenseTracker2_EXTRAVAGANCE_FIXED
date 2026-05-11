package com.example.expensetracker;

import android.graphics.Color;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onEdit(Expense expense);
        void onDelete(Expense expense);
    }

    private List<Expense>       expenses;
    private OnItemClickListener listener;

    public ExpenseAdapter(List<Expense> expenses, OnItemClickListener l) {
        this.expenses = expenses;
        this.listener = l;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Expense e = expenses.get(position);
        h.tvTitle.setText(e.getTitle());
        h.tvCategory.setText(e.getCategory());
        h.tvDate.setText(e.getDate());
        String sign = "INCOME".equals(e.getType()) ? "+ ₹" : "- ₹";
        h.tvAmount.setText(String.format("%s%.2f", sign, e.getAmount()));
        h.tvAmount.setTextColor("INCOME".equals(e.getType())
                ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336"));

        h.itemView.setOnClickListener(v -> listener.onEdit(e));
        h.itemView.setOnLongClickListener(v -> { listener.onDelete(e); return true; });
    }

    @Override
    public int getItemCount() { return expenses.size(); }

    public void updateList(List<Expense> newList) {
        this.expenses = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory, tvDate, tvAmount;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tvItemTitle);
            tvCategory = itemView.findViewById(R.id.tvItemCategory);
            tvDate     = itemView.findViewById(R.id.tvItemDate);
            tvAmount   = itemView.findViewById(R.id.tvItemAmount);
        }
    }
}
