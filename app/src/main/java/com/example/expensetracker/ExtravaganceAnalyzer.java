package com.example.expensetracker;

/**
 * Analyzes whether an expense or a monthly category total is extravagant.
 *
 * Logic:
 *  - NO_DATA (< 1 prior month): still alert if single expense > ₹2000 (absolute threshold)
 *  - SINGLE_EXTRAVAGANT: this expense alone is > 150% of the monthly avg.
 *  - MONTHLY_HIGH:       this month's running total for the category is > 130% of avg.
 *  - NORMAL:             spending looks fine.
 */
public class ExtravaganceAnalyzer {

    public enum Verdict { NO_DATA, NORMAL, SINGLE_EXTRAVAGANT, MONTHLY_HIGH }

    // Absolute threshold: alert even with no history if a single spend exceeds this
    private static final double ABSOLUTE_HIGH_THRESHOLD = 2000.0;

    public static class Result {
        public final Verdict verdict;
        public final String  category;
        public final double  thisAmount;
        public final double  monthlyTotal;
        public final double  historicalAvg;
        public final double  suggestedSaving;

        Result(Verdict verdict, String category, double thisAmount,
               double monthlyTotal, double historicalAvg) {
            this.verdict         = verdict;
            this.category        = category;
            this.thisAmount      = thisAmount;
            this.monthlyTotal    = monthlyTotal;
            this.historicalAvg   = historicalAvg;
            this.suggestedSaving = Math.max(0, monthlyTotal - historicalAvg);
        }
    }

    public static Result analyze(Expense expense,
                                  double monthlyTotal,
                                  double historicalAvg,
                                  int pastMonthCount) {

        if (!"EXPENSE".equals(expense.getType())) {
            return new Result(Verdict.NORMAL, expense.getCategory(),
                    expense.getAmount(), monthlyTotal, 0);
        }

        // No history yet — still alert on absolute high spends
        if (pastMonthCount < 1 || historicalAvg <= 0) {
            if (expense.getAmount() >= ABSOLUTE_HIGH_THRESHOLD) {
                // Reuse SINGLE_EXTRAVAGANT verdict so tip is shown
                return new Result(Verdict.SINGLE_EXTRAVAGANT, expense.getCategory(),
                        expense.getAmount(), monthlyTotal, 0);
            }
            return new Result(Verdict.NO_DATA, expense.getCategory(),
                    expense.getAmount(), monthlyTotal, 0);
        }

        // Single expense > 150% of historical monthly avg
        if (expense.getAmount() >= historicalAvg * 1.5) {
            return new Result(Verdict.SINGLE_EXTRAVAGANT, expense.getCategory(),
                    expense.getAmount(), monthlyTotal, historicalAvg);
        }

        // Monthly total running > 130% of historical avg
        if (monthlyTotal >= historicalAvg * 1.3) {
            return new Result(Verdict.MONTHLY_HIGH, expense.getCategory(),
                    expense.getAmount(), monthlyTotal, historicalAvg);
        }

        return new Result(Verdict.NORMAL, expense.getCategory(),
                expense.getAmount(), monthlyTotal, historicalAvg);
    }

    public static String getSavingsTip(Result r) {
        switch (r.verdict) {
            case SINGLE_EXTRAVAGANT:
                if (r.historicalAvg <= 0) {
                    // No history — absolute threshold triggered
                    return String.format(
                            "You just spent ₹%.0f on %s — that's a large amount! " +
                            "Keep an eye on this category to stay within budget.",
                            r.thisAmount, r.category);
                }
                return String.format(
                        "This ₹%.0f spend on %s is unusually high compared to your " +
                        "average of ₹%.0f/month. Consider if this was necessary.",
                        r.thisAmount, r.category, r.historicalAvg);

            case MONTHLY_HIGH:
                return String.format(
                        "You've spent ₹%.0f on %s this month — that's %.0f%% above your " +
                        "usual ₹%.0f. Cut back by ₹%.0f to stay on track.",
                        r.monthlyTotal, r.category,
                        ((r.monthlyTotal / r.historicalAvg) - 1) * 100,
                        r.historicalAvg, r.suggestedSaving);

            default:
                return null;
        }
    }
}
