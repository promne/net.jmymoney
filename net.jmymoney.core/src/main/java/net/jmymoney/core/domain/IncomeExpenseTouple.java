package net.jmymoney.core.domain;

import java.math.BigDecimal;

public class IncomeExpenseTouple {

    private BigDecimal income = BigDecimal.ZERO;
    private BigDecimal expense = BigDecimal.ZERO;

    public IncomeExpenseTouple() {
        super();
    }

    public IncomeExpenseTouple(BigDecimal income, BigDecimal expense) {
        super();
        this.income = income;
        this.expense = expense;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public IncomeExpenseTouple add(IncomeExpenseTouple incomeExpenseTouple) {
        income = income.add(incomeExpenseTouple.income);
        expense = expense.add(incomeExpenseTouple.expense);
        return this;
    }
    
    public void setIncome(BigDecimal income) {
        this.income = income;
    }

    public BigDecimal getExpense() {
        return expense;
    }

    public void setExpense(BigDecimal expense) {
        this.expense = expense;
    }

    @Override
    public String toString() {
        StringBuilder resultSB = new StringBuilder();
        if (!BigDecimal.ZERO.equals(income)) {
            resultSB.append(income.stripTrailingZeros().toPlainString());
            if (!BigDecimal.ZERO.equals(expense)) {
                resultSB.append(" (").append(expense.stripTrailingZeros().toPlainString()).append(")");
            }
        } else {
            resultSB.append(expense.stripTrailingZeros().toPlainString());
        }
        return resultSB.toString();
    }

    public BigDecimal getBalance() {
        return income.add(expense);
    }
    
    public String balanceToString() {
        return getBalance().stripTrailingZeros().toPlainString();
    }
    
}