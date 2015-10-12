package net.jmymoney.tools.highcharts;

import at.downdrown.vaadinaddons.highchartsapi.HighChart;
import at.downdrown.vaadinaddons.highchartsapi.exceptions.HighChartsException;
import at.downdrown.vaadinaddons.highchartsapi.model.ChartConfiguration;

public class HighChartFactoryProxy {

    public static HighChart renderChart(ChartConfiguration configuration) throws HighChartsException {
        //TODO instead of HighChartFactory, because it puts step variable into quotes 
        
        HighChart tempChart = new HighChart();
        String highChartValue = configuration.getHighChartValue();
        
        highChartValue = highChartValue.replaceAll("step: 'FALSE'", "step: false");
        highChartValue = highChartValue.replaceAll("step: 'TRUE'", "step: true");
        
        tempChart.setChartoptions(highChartValue);
        tempChart.setChartConfiguration(configuration);
        return tempChart;
    }

}
