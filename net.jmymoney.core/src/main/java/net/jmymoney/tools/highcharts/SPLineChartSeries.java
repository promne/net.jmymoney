package net.jmymoney.tools.highcharts;

import java.lang.reflect.Field;
import java.util.List;

import at.downdrown.vaadinaddons.highchartsapi.model.ChartType;
import at.downdrown.vaadinaddons.highchartsapi.model.data.HighChartsData;
import at.downdrown.vaadinaddons.highchartsapi.model.series.HighChartsSeriesImpl;

public class SPLineChartSeries extends HighChartsSeriesImpl {

    public SPLineChartSeries(String name, List<HighChartsData> data) {
        try {
            Field typeField = HighChartsSeriesImpl.class.getDeclaredField("chartType");
            typeField.setAccessible(true);
            typeField.set(this, ChartType.SPLINE);

            Field nameField = HighChartsSeriesImpl.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(this, name);

            Field dataField = HighChartsSeriesImpl.class.getDeclaredField("data");
            dataField.setAccessible(true);
            dataField.set(this, data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
