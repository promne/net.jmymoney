package net.jmymoney.tools.highcharts;

import java.lang.reflect.Field;

import at.downdrown.vaadinaddons.highchartsapi.model.ChartType;
import at.downdrown.vaadinaddons.highchartsapi.model.plotoptions.HighChartsPlotOptionsImpl;

public class SPLineChartPlotOptions extends HighChartsPlotOptionsImpl {
    public SPLineChartPlotOptions() {
        try {
            Field typeField = HighChartsPlotOptionsImpl.class.getDeclaredField("chartType");
            typeField.setAccessible(true);
            typeField.set(this, ChartType.SPLINE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
