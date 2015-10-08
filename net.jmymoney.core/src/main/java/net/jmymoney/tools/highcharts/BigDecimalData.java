package net.jmymoney.tools.highcharts;

import java.math.BigDecimal;

import at.downdrown.vaadinaddons.highchartsapi.model.data.base.HighChartsBaseData;

public class BigDecimalData implements HighChartsBaseData {

    private BigDecimal value;

    public BigDecimalData(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    @Override
    public String getHighChartValue() {
        return String.valueOf(this.value);
    }

    @Override
    public String toString() {
        return this.getHighChartValue();
    }

}
