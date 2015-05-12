package net.jmymoney.core.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import net.jmymoney.core.entity.Category;

public class CategoryReport {

    private Category category;

    private List<BigDecimal> valuesPlus = new ArrayList<>();

    private List<BigDecimal> valuesMinus = new ArrayList<>();

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

    public List<BigDecimal> getValuesPlus() {
        return valuesPlus;
    }

    public void setValuesPlus(List<BigDecimal> values) {
        this.valuesPlus = values;
    }

    public List<BigDecimal> getValuesMinus() {
        return valuesMinus;
    }

    public void setValuesMinus(List<BigDecimal> valuesMinus) {
        this.valuesMinus = valuesMinus;
    }

}
