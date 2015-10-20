package net.jmymoney.tools.highcharts;

import java.util.stream.Collectors;

import at.downdrown.vaadinaddons.highchartsapi.HighChart;
import at.downdrown.vaadinaddons.highchartsapi.exceptions.HighChartsException;

public class HighChartFactoryProxy {

    public static HighChart renderChart(ChartConfiguration configuration) throws HighChartsException {
        //TODO instead of HighChartFactory, because it puts step variable into quotes 
        
        HighChart tempChart = new HighChart();
        String highChartValue = configuration.getHighChartValue();
        
        highChartValue = highChartValue.replaceAll("step: 'FALSE'", "step: false");
        highChartValue = highChartValue.replaceAll("step: 'TRUE'", "step: true");
        
        if (!configuration.getyAxisPlotLines().isEmpty()) {
            StringBuilder sb = new StringBuilder("plotLines: [");
            sb.append(configuration.getyAxisPlotLines().stream().map(p -> String.format("{ color: '%s', value: %s, width: %s, zIndex: 3}", p.getColor(), p.getValue(), p.getWidth())).collect(Collectors.joining(", ")));
            sb.append("],");
            highChartValue = highChartValue.replaceAll("yAxis: \\{", "yAxis: { "+sb.toString());
        }
        
        tempChart.setChartoptions(highChartValue);
        tempChart.setChartConfiguration(configuration);
        return tempChart;
    }

}
