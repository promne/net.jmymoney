package net.jmymoney.core.domain;

import java.util.ArrayList;
import java.util.List;

import net.jmymoney.core.entity.Category;

public class CategoryReport {

    private Category category;

    private List<IncomeExpenseTouple> incomesAndExpenses = new ArrayList<>();
    
    public CategoryReport(Category category) {
        super();
        this.category = category;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<IncomeExpenseTouple> getIncomesAndExpenses() {
        return incomesAndExpenses;
    }

    public void setIncomesAndExpenses(List<IncomeExpenseTouple> incomesAndExpenses) {
        this.incomesAndExpenses = incomesAndExpenses;
    }
 
    public IncomeExpenseTouple getTotal() {
        return incomesAndExpenses.stream().reduce(new IncomeExpenseTouple(), IncomeExpenseTouple::add);
    }

    public boolean isEmpty() {
        return !incomesAndExpenses.stream().anyMatch(t -> t.getIncome().signum()!=0 || t.getExpense().signum()!=0);
    }
    
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "-" + category==null ? null : category.getName();
    }

}
