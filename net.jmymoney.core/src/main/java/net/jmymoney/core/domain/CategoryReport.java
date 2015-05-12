package net.jmymoney.core.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import net.jmymoney.core.entity.Category;

public class CategoryReport {

	private Category category;
	
	private List<BigDecimal> values = new ArrayList<>();

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

	public List<BigDecimal> getValues() {
		return values;
	}

	public void setValues(List<BigDecimal> values) {
		this.values = values;
	}
	
}
