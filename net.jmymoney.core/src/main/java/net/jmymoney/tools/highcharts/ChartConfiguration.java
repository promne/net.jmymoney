package net.jmymoney.tools.highcharts;

import java.util.ArrayList;
import java.util.List;

public class ChartConfiguration extends at.downdrown.vaadinaddons.highchartsapi.model.ChartConfiguration {

    public static class Color {
        public static final String LIGHT_SALMON_PINK = "#FF9999";
    }
    
    public static class PlotLine {
        private String color;
        private Float value;
        private Integer width;
        public PlotLine() {
            super();
        }
        public String getColor() {
            return color;
        }
        public PlotLine setColor(String color) {
            this.color = color;
            return this;
        }
        public Float getValue() {
            return value;
        }
        public PlotLine setValue(Float value) {
            this.value = value;
            return this;
        }
        public Integer getWidth() {
            return width;
        }
        public PlotLine setWidth(Integer width) {
            this.width = width;
            return this;
        }        
    }
    
    private List<PlotLine> yAxisPlotLines = new ArrayList<>();

    public ChartConfiguration() {
        super();
    }

    public List<PlotLine> getyAxisPlotLines() {
        return yAxisPlotLines;
    }

    public void setyAxisPlotLines(List<PlotLine> yAxisPlotLines) {
        this.yAxisPlotLines = yAxisPlotLines;
    }
    
}
