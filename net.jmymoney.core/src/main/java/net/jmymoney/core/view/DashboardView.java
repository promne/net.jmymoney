package net.jmymoney.core.view;

import com.vaadin.cdi.CDIView;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import at.downdrown.vaadinaddons.highchartsapi.HighChartFactory;
import at.downdrown.vaadinaddons.highchartsapi.exceptions.HighChartsException;
import at.downdrown.vaadinaddons.highchartsapi.model.Axis.AxisValueType;
import at.downdrown.vaadinaddons.highchartsapi.model.ChartConfiguration;
import at.downdrown.vaadinaddons.highchartsapi.model.ChartType;
import at.downdrown.vaadinaddons.highchartsapi.model.plotoptions.LineChartPlotOptions;
import at.downdrown.vaadinaddons.highchartsapi.model.series.BarChartSeries;
import at.downdrown.vaadinaddons.highchartsapi.model.series.LineChartSeries;
import net.jmymoney.core.UserIdentity;
import net.jmymoney.core.domain.AccountMetadata;
import net.jmymoney.core.domain.CategoryReport;
import net.jmymoney.core.i18n.I18nResourceConstant;
import net.jmymoney.core.i18n.MessagesResourceBundle;
import net.jmymoney.core.service.AccountService;
import net.jmymoney.core.service.ReportingService;

@CDIView(value = DashboardView.NAME)
public class DashboardView extends CssLayout implements View {

    public static final String NAME = "DashboardView";

    private static final int REPORT_NUMBER_OF_DAYS = 30;
    
    @Inject
    private UserIdentity userIdentity;

    @Inject
    private AccountService accountService;
    
    @Inject
    private ReportingService reportingService;
    
    @Inject
    private MessagesResourceBundle messagesResourceBundle;

    @PostConstruct
    private void init() {
        setSizeFull();
    }

    @Override
    public void enter(ViewChangeEvent event) {
        refresh();
    }

    private void refresh() {
        removeAllComponents();
        
        Label accountsHeaderLabel = new Label("<h2>"+messagesResourceBundle.getString(I18nResourceConstant.DASHBOARD)+"</h2>", ContentMode.HTML);
        addComponent(accountsHeaderLabel);

        addComponent(getAccountBalanceChart());
        addComponent(getTopExpenseCategoriesChart());
        addComponent(getAccountBalanceHistoryChart());
        addComponent(getNetWorthHistoryChart());
    }
    
    private Component getAccountBalanceHistoryChart() {
        Map<Long, LineChartSeries> accountSeries = new HashMap<>();
              
        ChartConfiguration lineConfiguration = new ChartConfiguration();
        lineConfiguration.setTitle(messagesResourceBundle.getString(I18nResourceConstant.DASHBOARD_ACCOUNT_BALANCE_HISTORY, REPORT_NUMBER_OF_DAYS));
        lineConfiguration.setChartType(ChartType.LINE);
        lineConfiguration.getxAxis().setAxisValueType(AxisValueType.DATETIME);

        LineChartPlotOptions lineChartPlotOptions = new LineChartPlotOptions();
        lineConfiguration.setPlotOptions(lineChartPlotOptions);
        lineChartPlotOptions.setDataLabelsEnabled(false);
        
        Instant currentDate = Instant.now();
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        Instant iterationDate = currentDate.minus(REPORT_NUMBER_OF_DAYS-1, ChronoUnit.DAYS);
        while (!iterationDate.isAfter(currentDate)) {
            List<AccountMetadata> accountMetadatas = accountService.listAccountMetadatas(userIdentity.getUserAccount(), Date.from(iterationDate));
            for (AccountMetadata accountMetadata : accountMetadatas) {
                LineChartSeries series = accountSeries.get(accountMetadata.getAccount().getId());
                if (series==null) {
                    series = new LineChartSeries(accountMetadata.getAccount().getName());
                    accountSeries.put(accountMetadata.getAccount().getId(), series);
                    lineConfiguration.getSeriesList().add(series);
                }
                series.getData().add(accountMetadata.getBalance());
            }
            lineConfiguration.getxAxis().getCategories().add(dateFormatter.format(LocalDateTime.ofInstant(iterationDate, ZoneId.systemDefault())));
            iterationDate = iterationDate.plus(1, ChronoUnit.DAYS);
        }
        
        try {
            return HighChartFactory.renderChart(lineConfiguration);
        } catch (HighChartsException e) {
            throw new IllegalStateException(e);
        }
    }

    private Component getNetWorthHistoryChart() {
        ChartConfiguration lineConfiguration = new ChartConfiguration();
        lineConfiguration.setTitle(messagesResourceBundle.getString(I18nResourceConstant.DASHBOARD_NET_WORTH, REPORT_NUMBER_OF_DAYS));
        lineConfiguration.setChartType(ChartType.LINE);
        lineConfiguration.setLegendEnabled(false);
        
        LineChartPlotOptions lineChartPlotOptions = new LineChartPlotOptions();
        lineConfiguration.setPlotOptions(lineChartPlotOptions);
        lineChartPlotOptions.setDataLabelsEnabled(false);
        
        
        Instant currentDate = Instant.now().plusMillis(Page.getCurrent().getWebBrowser().getRawTimezoneOffset());
        final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        Instant iterationDate = currentDate.minus(REPORT_NUMBER_OF_DAYS-1, ChronoUnit.DAYS);
        LineChartSeries series = new LineChartSeries("net worth");
        lineConfiguration.getSeriesList().add(series);
        
        lineConfiguration.getxAxis().setAxisValueType(AxisValueType.DATETIME);
        
        while (!iterationDate.isAfter(currentDate)) {
            List<AccountMetadata> accountMetadatas = accountService.listAccountMetadatas(userIdentity.getUserAccount(), Date.from(iterationDate));
            
            series.getData().add(accountMetadatas.stream().map(m -> m.getBalance()).reduce(BigDecimal.ZERO, BigDecimal::add));
            
            lineConfiguration.getxAxis().getCategories().add(dateFormatter.format(LocalDateTime.ofInstant(iterationDate, ZoneId.systemDefault())));
            iterationDate = iterationDate.plus(1, ChronoUnit.DAYS);
        }
        
        try {
            return HighChartFactory.renderChart(lineConfiguration);
        } catch (HighChartsException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private Component getTopExpenseCategoriesChart() {
        int numberOfCategories = 5;
        
        ChartConfiguration chartConfiguration = new ChartConfiguration();
        chartConfiguration.setTitle(messagesResourceBundle.getString(I18nResourceConstant.DASHBOARD_TOP_CATEGORIES, numberOfCategories, REPORT_NUMBER_OF_DAYS));
        chartConfiguration.setChartType(ChartType.BAR);
        chartConfiguration.getxAxis().setLabelsEnabled(false);
        
        List<CategoryReport> categoryReport = reportingService.getCategoryReport(userIdentity.getUserAccount(), new Date(new Date().getTime() - REPORT_NUMBER_OF_DAYS*24*60*60*1000L), new Date(), ChronoUnit.MONTHS, true);
        categoryReport.sort((i,j) -> i.getTotal().getExpense().compareTo(j.getTotal().getExpense()));
        
        categoryReport = categoryReport.stream().filter(
                p -> p.getTotal().getExpense().compareTo(BigDecimal.ZERO) < 0
            ).limit(numberOfCategories).sorted((i,j) -> j.getTotal().getExpense().compareTo(i.getTotal().getExpense())).collect(Collectors.toList());
        
        for (CategoryReport cr : categoryReport) {
            String seriesName = cr.getCategory() != null ? cr.getCategory().getName() : "Without category";
            BarChartSeries series = new BarChartSeries(seriesName, Arrays.asList(new BigDecimal[] {cr.getTotal().getExpense().abs()}));
            chartConfiguration.getSeriesList().add(series);
        }
        
        try {
            return HighChartFactory.renderChart(chartConfiguration);
        } catch (HighChartsException e) {
            throw new IllegalStateException(e);
        }
    }    
    
    private Component getAccountBalanceChart() {
        List<AccountMetadata> accountMetadatas = accountService.listAccountMetadatas(userIdentity.getUserAccount());
        ChartConfiguration lineConfiguration = new ChartConfiguration();
        lineConfiguration.setTitle(messagesResourceBundle.getString(I18nResourceConstant.DASHBOARD_ACCOUNT_BALANCE));
        lineConfiguration.setChartType(ChartType.BAR);
        lineConfiguration.getxAxis().setLabelsEnabled(false);

        
        accountMetadatas.forEach(m -> lineConfiguration.getSeriesList().add(
                new BarChartSeries(m.getAccount().getName(), Arrays.asList(new BigDecimal[] {m.getBalance()}))
              ));
        try {
            return HighChartFactory.renderChart(lineConfiguration);
        } catch (HighChartsException e) {
            throw new IllegalStateException(e);
        }
    }
}
