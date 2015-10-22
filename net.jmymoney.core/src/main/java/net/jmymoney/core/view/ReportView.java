package net.jmymoney.core.view;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.MultiSelectMode;
import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.PopupView;
import com.vaadin.ui.PopupView.Content;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.collections.list.LazyList;
import org.slf4j.Logger;

import at.downdrown.vaadinaddons.highchartsapi.HighChart;
import at.downdrown.vaadinaddons.highchartsapi.exceptions.HighChartsException;
import at.downdrown.vaadinaddons.highchartsapi.model.Axis.AxisValueType;
import at.downdrown.vaadinaddons.highchartsapi.model.ChartType;
import at.downdrown.vaadinaddons.highchartsapi.model.data.HighChartsData;
import at.downdrown.vaadinaddons.highchartsapi.model.plotoptions.HighChartsPlotOptionsImpl.Steps;
import at.downdrown.vaadinaddons.highchartsapi.model.series.HighChartsSeries;
import net.jmymoney.core.UserIdentity;
import net.jmymoney.core.data.CategoryContainer;
import net.jmymoney.core.domain.CategoryReport;
import net.jmymoney.core.domain.IncomeExpenseTouple;
import net.jmymoney.core.entity.Account;
import net.jmymoney.core.entity.Category;
import net.jmymoney.core.service.AccountService;
import net.jmymoney.core.service.CategoryService;
import net.jmymoney.core.service.ReportingService;
import net.jmymoney.core.theme.ThemeResourceConstatns;
import net.jmymoney.core.theme.ThemeStyles;
import net.jmymoney.core.util.DateMonthRoundConverter;
import net.jmymoney.tools.highcharts.BigDecimalData;
import net.jmymoney.tools.highcharts.ChartConfiguration;
import net.jmymoney.tools.highcharts.ChartConfiguration.PlotLine;
import net.jmymoney.tools.highcharts.HighChartFactoryProxy;
import net.jmymoney.tools.highcharts.SPLineChartPlotOptions;
import net.jmymoney.tools.highcharts.SPLineChartSeries;

@CDIView(value = ReportView.NAME)
public class ReportView extends VerticalLayout implements View {

    public static final String NAME = "ReportView";

    private static final String COLUMN_DATE = "column_amount";
    private static final String COLUMN_NAME = "column_name";
    private static final String COLUMN_CATEGORY_REPORT = "column_category_report";
    private static final String COLUMN_SUM_ALL_TIME = "column_sum_all_time";
    
    private static final String TEXT_WITHOUT_CATEGORY = "Without category";

    private ComboBox granularityComboBox;
    private DateField filterFromDate;
    private DateField filterToDate;

    @Inject
    private Logger log;

    @Inject
    private UserIdentity userIdentity;

    @Inject
    private ReportingService reportingService;
    
    @Inject
    private AccountService accountService;

    @Inject
    private CategoryService categoryService;

    private TreeTable reportTable;
    private Layout categoryChartLayout;
    private Layout balanceChartLayout;

    private OptionGroup filterAccounts;

    private OptionGroup filterCategoriesType;
    
    private TreeTable filterCategoryTree = new TreeTable(null, new CategoryContainer()); 
    
    private CheckBox includeWithoutCategory;

    private CheckBox excludeEmptyRows;

    private TabSheet reportTabs;

    @PostConstruct
    private void init() {
        setSizeFull();
        setSpacing(true);
        setMargin(true);

        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setSpacing(true);

        granularityComboBox = new ComboBox("Granularity",
                Arrays.asList(new ChronoUnit[] { ChronoUnit.MONTHS, ChronoUnit.WEEKS }));
        granularityComboBox.setBuffered(false);
        granularityComboBox.setNullSelectionAllowed(false);
        granularityComboBox.select(ChronoUnit.MONTHS);
        granularityComboBox.addValueChangeListener(event -> filterChanged());
        filterLayout.addComponent(granularityComboBox);

        filterFromDate = new DateField("From");
        filterFromDate.setValue(Date.from(
                LocalDate.now().minus(6, ChronoUnit.MONTHS).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        filterFromDate.setResolution(Resolution.MONTH);
        filterFromDate.setConverter(new DateMonthRoundConverter());
        filterFromDate.addValueChangeListener(event -> filterChanged());
        filterLayout.addComponent(filterFromDate);

        filterToDate = new DateField("To");
        filterToDate.setValue(new Date());
        filterToDate.setResolution(Resolution.MONTH);
        filterToDate.setConverter(new DateMonthRoundConverter());
        filterToDate.addValueChangeListener(event -> filterChanged());
        filterLayout.addComponent(filterToDate);

        
        //accounts filter
        String selectedAccounts = "Click to select accounts";
        VerticalLayout accountsSelectionLayout = new VerticalLayout(new Label(selectedAccounts));
        accountsSelectionLayout.setSpacing(true);
        
        HorizontalLayout accountGroupOperation = new HorizontalLayout();
        accountGroupOperation.setSpacing(true);
        accountGroupOperation.addComponent(new Button("All", event -> filterAccounts.getItemIds().stream().forEach(i -> filterAccounts.select(i))));
        accountGroupOperation.addComponent(new Button("None", event -> filterAccounts.getItemIds().stream().forEach(i -> filterAccounts.unselect(i))));
        accountsSelectionLayout.addComponent(accountGroupOperation);
        
        filterAccounts = new OptionGroup();
        filterAccounts.setItemCaptionPropertyId(Account.PROPERTY_NAME);
        filterAccounts.setMultiSelect(true);
        accountsSelectionLayout.addComponent(filterAccounts);
        
        
        PopupView accountSelectionView = new PopupView(new Content() {
            
            @Override
            public Component getPopupComponent() {
                return accountsSelectionLayout;
            }
            
            @Override
            public String getMinimizedValueAsHTML() {
                Collection<Account> selected = (Collection<Account>) filterAccounts.getValue();
                String result = "None";
                if (!selected.isEmpty()) {
                    if (selected.size()==filterAccounts.getItemIds().size()) {
                        result = "All";
                    } else {
                        result = selected.stream().map(Account::getName).collect(Collectors.joining(", "));
                        if (result.length() > 25) {
                            result = result.substring(0, 25) + "...";
                        }
                    }
                }
                return selectedAccounts + "<br/>" + result;
            }
        });
        accountSelectionView.addPopupVisibilityListener(event -> {if (!event.isPopupVisible()) {
            filterChanged();
        }});
        accountSelectionView.setHideOnMouseOut(false);
        accountsSelectionLayout.addComponent(new Button("Apply", event -> accountSelectionView.setPopupVisible(false)));
        filterLayout.addComponent(accountSelectionView);

        
        // categories filter
        String selectedCategories = "Click to select categories";
        VerticalLayout categoriesSelectionLayout = new VerticalLayout(new Label(selectedCategories));
        categoriesSelectionLayout.setSpacing(true);

        HorizontalLayout categoryGroupOperation = new HorizontalLayout();
        categoryGroupOperation.setSpacing(true);
        categoryGroupOperation.addComponent(new Button("All", event -> filterCategoryTree.getItemIds().stream().forEach(i -> filterCategoryTree.select(i))));
        categoryGroupOperation.addComponent(new Button("None", event -> filterCategoryTree.getItemIds().stream().forEach(i -> filterCategoryTree.unselect(i))));
        categoriesSelectionLayout.addComponent(categoryGroupOperation);
        
        filterCategoryTree.setVisibleColumns(Category.PROPERTY_NAME);
        filterCategoryTree.setSelectable(true);
        filterCategoryTree.setMultiSelect(true);
        filterCategoryTree.setMultiSelectMode(MultiSelectMode.SIMPLE);
        filterCategoryTree.setSizeFull();
        categoriesSelectionLayout.addComponent(filterCategoryTree);
        
        filterCategoriesType = new OptionGroup();
        filterCategoriesType.setMultiSelect(false);
        final String categoryTypeFilterNameProperty = "name";
        filterCategoriesType.addContainerProperty(categoryTypeFilterNameProperty, String.class, "");
        
        filterCategoriesType.addItem(Boolean.TRUE).getItemProperty(categoryTypeFilterNameProperty).setValue("Include subcategories");
        filterCategoriesType.addItem(Boolean.FALSE).getItemProperty(categoryTypeFilterNameProperty).setValue("Standalone categories");
        filterCategoriesType.setItemCaptionPropertyId(categoryTypeFilterNameProperty);
        filterCategoriesType.select(Boolean.FALSE);
        categoriesSelectionLayout.addComponent(filterCategoriesType);

        PopupView categorySelectionView = new PopupView(new Content() {
            
            @Override
            public Component getPopupComponent() {
                return categoriesSelectionLayout;
            }
            
            @Override
            public String getMinimizedValueAsHTML() {
                Collection<Category> selected = (Collection<Category>) filterCategoryTree.getValue();
                String result = "None";
                if (!selected.isEmpty()) {
                    if (selected.size()==filterCategoryTree.getItemIds().size()) {
                        result = "All";
                    } else {
                        result = selected.stream().map(Category::getName).collect(Collectors.joining(", "));
                        if (result.length() > 25) {
                            result = result.substring(0, 25) + "...";
                        }
                    }
                }
                return selectedCategories + "<br/>" + result + "<br/>" + filterCategoriesType.getItemCaption(filterCategoriesType.getValue());
            }
        });
        categorySelectionView.addPopupVisibilityListener(event -> {if (!event.isPopupVisible()) {
            filterChanged();
        }});
        categorySelectionView.setHideOnMouseOut(false);
        
        categoriesSelectionLayout.addComponent(includeWithoutCategory = new CheckBox("Include empty category items", true));
        categoriesSelectionLayout.addComponent(new Button("Apply", event -> categorySelectionView.setPopupVisible(false)));
        filterLayout.addComponent(categorySelectionView);
        
        excludeEmptyRows = new CheckBox("Exclude empty rows", true);
        excludeEmptyRows.addValueChangeListener(e -> filterChanged());
        filterLayout.addComponent(excludeEmptyRows);
        
        addComponent(filterLayout);

        reportTable = new TreeTable("Report - incomes and expenses");
        reportTable.setSizeFull();
        reportTable.setSelectable(true);
        reportTable.addContainerProperty(COLUMN_CATEGORY_REPORT, CategoryReport.class, null);

        reportTable.addGeneratedColumn(COLUMN_NAME, (source, itemId, columnId) -> {
            Item item = reportTable.getItem(itemId);
            if (item.getItemProperty(COLUMN_CATEGORY_REPORT).getValue() != null) {
                String rowName = ((CategoryReportRow) item.getItemProperty(COLUMN_CATEGORY_REPORT).getValue()).getName();
                return rowName == null ? TEXT_WITHOUT_CATEGORY : rowName;
            }
            return null;
        } );

        reportTable.addGeneratedColumn(COLUMN_SUM_ALL_TIME, (source, itemId, columnId) -> {
            Item item = reportTable.getItem(itemId);
            if (item.getItemProperty(COLUMN_CATEGORY_REPORT).getValue() != null) {
                CategoryReportRow categoryReportRow = (CategoryReportRow) item.getItemProperty(COLUMN_CATEGORY_REPORT).getValue();
                return createLabelForIncomeExpense(categoryReportRow.getTotal());
            }
            return null;
        } );

        reportTable.addStyleName("colored-table");
        reportTable.setCellStyleGenerator((source, itemId, propertyId) -> {
            Item item = reportTable.getItem(itemId);
            if (item.getItemProperty(COLUMN_CATEGORY_REPORT).getValue() != null) {
                if (((CategoryReportRow) item.getItemProperty(COLUMN_CATEGORY_REPORT).getValue()).isGenerated() || COLUMN_SUM_ALL_TIME.equals(propertyId)) {
                    return ThemeStyles.TABLE_CELL_STYLE_HIGHLIGHT;
                }
            }
            return null;
        } );        
        
        reportTable.setColumnCollapsingAllowed(false);
        reportTable.setSortEnabled(false);
        reportTable.setVisibleColumns(COLUMN_NAME, COLUMN_SUM_ALL_TIME);
        reportTable.setColumnHeaders("Category", "SUM");

        
        categoryChartLayout = new VerticalLayout();
        categoryChartLayout.setSizeFull();
        
        balanceChartLayout = new VerticalLayout();
        balanceChartLayout.setSizeFull();
        
        reportTabs = new TabSheet();
        reportTabs.setSizeFull();
        reportTabs.addTab(reportTable, "Table", ThemeResourceConstatns.TABLE);
        reportTabs.addTab(categoryChartLayout, "Category chart", ThemeResourceConstatns.CHART);
        reportTabs.addTab(balanceChartLayout, "Balance chart", ThemeResourceConstatns.CHART);
        
        reportTabs.addSelectedTabChangeListener(e -> filterChanged());
        
        addComponent(reportTabs);
        setExpandRatio(reportTabs, 1.0f);
    }

    private void refreshReportTable(List<CategoryReport> reports, Date fromDate, TemporalUnit temporalUnit) {
        reportTable.removeAllItems();
        // TODO: smarter replace
        for (int genColumnId = 0; reportTable.getColumnGenerator(COLUMN_DATE + genColumnId) != null; genColumnId++) {
            reportTable.removeGeneratedColumn(COLUMN_DATE + genColumnId);
        }
        
        if (excludeEmptyRows.getValue()) {
            reports = filterOutEmptyRows(reports, false);
        }
        
        if (reports.isEmpty()) {
            return;
        }

        // actually add. Be careful because this triggers generated columns
        reports.forEach(categoryReport -> {
            Object newItemId = reportTable.addItem();
            Item newItem = reportTable.getItem(newItemId);
            newItem.getItemProperty(COLUMN_CATEGORY_REPORT).setValue(new CategoryReportRow(categoryReport));
        } );

        // sum ALL categories row
        CategoryReportRow sumRow = new CategoryReportRow();
        sumRow.setGenerated(true);
        sumRow.setName("SUM");
        for (CategoryReport report : reports) {
            if (filterCategoriesType.getValue()==Boolean.TRUE && report.getCategory()!=null && report.getCategory().getParent()!=null) {
                continue;
            }
            for (int i=0; i< report.getIncomesAndExpenses().size(); i++) {
                sumRow.getIncomesAndExpenses().get(i).add(report.getIncomesAndExpenses().get(i));
            }
        }
        Object newItemId = reportTable.addItem();
        Item newItem = reportTable.getItem(newItemId);
        newItem.getItemProperty(COLUMN_CATEGORY_REPORT).setValue(sumRow);
        
        
        int reportSize = reports.get(0).getIncomesAndExpenses().size();
        for (int gcId = 0; reportSize > gcId; gcId++) {
            final int gcIdFinal = gcId;
            String genColumnId = COLUMN_DATE + gcIdFinal;
            reportTable.addGeneratedColumn(genColumnId, (source, itemId, columnId) -> {
                Item item = source.getItem(itemId);
                if (item.getItemProperty(COLUMN_CATEGORY_REPORT).getValue() != null) {
                    CategoryReport value = (CategoryReport) item.getItemProperty(COLUMN_CATEGORY_REPORT).getValue();
                    IncomeExpenseTouple incomeExpenseTouple = value.getIncomesAndExpenses().get(gcIdFinal);
                    return createLabelForIncomeExpense(incomeExpenseTouple);
                }
                return null;
            } );
            Date dateFrameStart = Date.from((fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault()).plus(gcId, temporalUnit).toInstant()));
            Date dateFrameEnd = Date.from((fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault()).plus(gcId + 1, temporalUnit).minusDays(1).toInstant()));
            String header = String.format("%s - %s", DateFormat.getDateInstance().format(dateFrameStart),
                    DateFormat.getDateInstance().format(dateFrameEnd));
            reportTable.setColumnHeader(genColumnId, header);
        }
        
        //reorder columns to put sum on the end
        List<Object> columnList = new ArrayList<>(Arrays.asList(reportTable.getVisibleColumns()));
        for (int i=0; i<columnList.size(); i++) {
            if (COLUMN_SUM_ALL_TIME.equals(columnList.get(i))) {
                columnList.add(columnList.get(i));
                columnList.remove(i);
                break;
            }
        }
        reportTable.setVisibleColumns(columnList.toArray());
        
        
        // compose into tree hierarchy
        reportTable.getItemIds().forEach(itemId -> {
            Item item = reportTable.getItem(itemId);
            CategoryReport categoryReport = (CategoryReport) item.getItemProperty(COLUMN_CATEGORY_REPORT).getValue();
            Category category = categoryReport.getCategory() != null ? categoryReport.getCategory() : null;
            if (category != null && category.getParent() != null) {
                for (Object parentItemIdCandidate : reportTable.getItemIds()) {
                    CategoryReport parentReportCandidate = (CategoryReport) reportTable.getItem(parentItemIdCandidate)
                            .getItemProperty(COLUMN_CATEGORY_REPORT).getValue();
                    if (parentReportCandidate.getCategory() != null
                            && category.getParent().getId().equals(parentReportCandidate.getCategory().getId())) {
                        reportTable.setParent(itemId, parentItemIdCandidate);
                    }
                }
            }
            reportTable.setCollapsed(itemId, false);
        } );
        reportTable.getItemIds().forEach(itemId -> {
            if (reportTable.getChildren(itemId) == null) {
                reportTable.setChildrenAllowed(itemId, false);
            }
        } );        
    }
    
    private void refreshCategoryReportChart(List<CategoryReport> reports, Date fromDate, TemporalUnit temporalUnit) {
        categoryChartLayout.removeAllComponents();

        if (excludeEmptyRows.getValue()) {
            reports = filterOutEmptyRows(reports, true);
        }
        if (reports.isEmpty()) {
            return;
        }
        
        ChartConfiguration lineConfiguration = new ChartConfiguration();
        lineConfiguration.setChartType(ChartType.SPLINE);
        lineConfiguration.setLegendEnabled(true);
        lineConfiguration.getxAxis().setAxisValueType(AxisValueType.DATETIME);
        lineConfiguration.getyAxisPlotLines().add(new PlotLine().setColor(ChartConfiguration.Color.LIGHT_SALMON_PINK).setValue(0f).setWidth(1));
        
        SPLineChartPlotOptions lineChartPlotOptions = new SPLineChartPlotOptions();
        lineChartPlotOptions.setSteps(Steps.FALSE);
        lineConfiguration.setPlotOptions(lineChartPlotOptions);
        lineChartPlotOptions.setDataLabelsEnabled(false);
        
        for (CategoryReport report : reports) {
            Category reportCategory = report.getCategory();
            if (reportCategory == null) {
                reportCategory = new Category();
                reportCategory.setId(0L);
                reportCategory.setName(TEXT_WITHOUT_CATEGORY);
            }
            List<HighChartsData> seriesData = new ArrayList<>();
            
            HighChartsSeries series = new SPLineChartSeries(reportCategory.getName(), seriesData);
            lineConfiguration.getSeriesList().add(series);
            
            boolean negateSeries = report.getTotal().getBalance().compareTo(BigDecimal.ZERO) > 0;
            for (IncomeExpenseTouple incomeAndExpense : report.getIncomesAndExpenses()) {
                seriesData.add(new BigDecimalData(negateSeries ? incomeAndExpense.getBalance() : incomeAndExpense.getBalance().negate()));
            }
        }
        
        for (int gcId = 0; reports.get(0).getIncomesAndExpenses().size() > gcId; gcId++) {
            Date dateFrameStart = Date.from((fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault()).plus(gcId, temporalUnit).toInstant()));
            Date dateFrameEnd = Date.from((fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault()).plus(gcId + 1, temporalUnit).minusDays(1).toInstant()));
            String header = String.format("%s - %s", DateFormat.getDateInstance().format(dateFrameStart),
                    DateFormat.getDateInstance().format(dateFrameEnd));
            lineConfiguration.getxAxis().getCategories().add(header);            
        }        
        
        try {
            HighChart renderChart = HighChartFactoryProxy.renderChart(lineConfiguration);
            renderChart.setSizeFull();
            categoryChartLayout.addComponent(renderChart);
        } catch (HighChartsException e) {
            throw new IllegalStateException(e);
        }
    }

    private void refreshBalanceReportChart(List<CategoryReport> reports, Date fromDate, TemporalUnit temporalUnit) {
        balanceChartLayout.removeAllComponents();
        
        if (reports.isEmpty()) {
            return;
        }
        
        ChartConfiguration lineConfiguration = new ChartConfiguration();
        lineConfiguration.setChartType(ChartType.SPLINE);
        lineConfiguration.setLegendEnabled(true);
        lineConfiguration.getxAxis().setAxisValueType(AxisValueType.DATETIME);
        lineConfiguration.getyAxisPlotLines().add(new PlotLine().setColor(ChartConfiguration.Color.LIGHT_SALMON_PINK).setValue(0f).setWidth(1));
        
        SPLineChartPlotOptions lineChartPlotOptions = new SPLineChartPlotOptions();
        lineChartPlotOptions.setSteps(Steps.FALSE);
        lineConfiguration.setPlotOptions(lineChartPlotOptions);
        lineChartPlotOptions.setDataLabelsEnabled(false);
        
        
        List<IncomeExpenseTouple> balance = reports.stream().map(CategoryReport::getIncomesAndExpenses).reduce((List) new ArrayList<>(), (t, u) -> {
            int resultSize = Math.max(t.size(), u.size());
            List<IncomeExpenseTouple> result = new ArrayList<>(resultSize);
            for (int i=0; i<resultSize; i++) {
                IncomeExpenseTouple current = new IncomeExpenseTouple();
                if (t.size()>i) {
                    current.add(t.get(i));
                }
                if (u.size()>i) {
                    current.add(u.get(i));
                }
                result.add(current);
            }
            return result;
        });
        
        List<HighChartsData> seriesExpenseData = new ArrayList<>();
        List<HighChartsData> seriesIncomeData = new ArrayList<>();
        List<HighChartsData> seriesBalanceData = new ArrayList<>();
        for (IncomeExpenseTouple incomeAndExpense : balance) {
            seriesExpenseData.add(new BigDecimalData(incomeAndExpense.getExpense().negate()));
            seriesIncomeData.add(new BigDecimalData(incomeAndExpense.getIncome()));
            seriesBalanceData.add(new BigDecimalData(incomeAndExpense.getBalance()));
        }
        
        lineConfiguration.getSeriesList().add(new SPLineChartSeries("Expenses", seriesExpenseData));
        lineConfiguration.getSeriesList().add(new SPLineChartSeries("Income", seriesIncomeData));
        lineConfiguration.getSeriesList().add(new SPLineChartSeries("Balance", seriesBalanceData));
        
        for (int gcId = 0; reports.get(0).getIncomesAndExpenses().size() > gcId; gcId++) {
            Date dateFrameStart = Date.from((fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault()).plus(gcId, temporalUnit).toInstant()));
            Date dateFrameEnd = Date.from((fromDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault()).plus(gcId + 1, temporalUnit).minusDays(1).toInstant()));
            String header = String.format("%s - %s", DateFormat.getDateInstance().format(dateFrameStart),
                    DateFormat.getDateInstance().format(dateFrameEnd));
            lineConfiguration.getxAxis().getCategories().add(header);            
        }        
        
        try {
            HighChart renderChart = HighChartFactoryProxy.renderChart(lineConfiguration);
            renderChart.setSizeFull();
            balanceChartLayout.addComponent(renderChart);
        } catch (HighChartsException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private Label createLabelForIncomeExpense(IncomeExpenseTouple incomeExpenseTouple) {
        Label label = new Label(incomeExpenseTouple.toString());
        label.setDescription(incomeExpenseTouple.balanceToString());
        return label ;
    }
    
    private void filterChanged() {
        TemporalUnit temporalUnit = (TemporalUnit) granularityComboBox.getValue();
        Date fromDate = Date.from(filterFromDate.getValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                .with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(filterToDate.getValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                .with(TemporalAdjusters.firstDayOfNextMonth()).atStartOfDay(ZoneId.systemDefault()).toInstant());

        
        Collection<Account> selectedAccountsForReport = (Collection<Account>)filterAccounts.getValue();
        Collection<Category> selectedCategoriesForReport = (Collection<Category>)filterCategoryTree.getValue();
        if (selectedAccountsForReport.isEmpty() || (selectedCategoriesForReport.isEmpty() && !includeWithoutCategory.getValue())) {
            reportTabs.setVisible(false);
            return;
        }
        reportTabs.setVisible(true);
        
        List<CategoryReport> reports = reportingService.getCategoryReport(userIdentity.getProfile(), fromDate, toDate, temporalUnit, selectedAccountsForReport, true, filterCategoriesType.getValue()==Boolean.TRUE, selectedCategoriesForReport, includeWithoutCategory.getValue());
        Collections.sort(reports, (c1, c2) -> {
            if (c1.getCategory() == null) {
                return c2.getCategory() == null ? 0 : 1;
            }
            if (c2.getCategory() == null) {
                return c1.getCategory() == null ? 0 : -1;
            }
            return c1.getCategory().getName().compareToIgnoreCase(c2.getCategory().getName());
        } );

        Component selectedTab = reportTabs.getSelectedTab();
        if (selectedTab==reportTable) {
            refreshReportTable(reports, fromDate, temporalUnit);
        } else if (selectedTab==categoryChartLayout) {
            refreshCategoryReportChart(reports, fromDate, temporalUnit);
        } else if (selectedTab==balanceChartLayout) {
            refreshBalanceReportChart(reports, fromDate, temporalUnit);
        } else {
            throw new IllegalStateException("Unknown tab" + selectedTab);
        }
    }
    
    /**
     * @param reports input reports
     * @param filterOutParents removes parent category if empty leaving children 
     * @return
     */
    private List<CategoryReport> filterOutEmptyRows(List<CategoryReport> reports, boolean filterOutParents) {
        Predicate<CategoryReport> isEmptyPredicate;
        if (filterOutParents) {
            isEmptyPredicate = CategoryReport::isEmpty;
        } else {
            isEmptyPredicate = i -> {
                BiFunction<BiFunction, CategoryReport, Boolean> h = (f,d) -> {
                    boolean result = d.isEmpty();
                    if (result) {
                        result &= reports.stream()
                                .filter(p -> p.getCategory()!=null && p.getCategory().getParent()!=null && p.getCategory().getParent().equals(d.getCategory()))
                                .map(p -> (Boolean)f.apply(f, p))
                                .reduce(Boolean.TRUE, (x,y) -> x && y);
                    }
                    return result;                
                };
                return h.apply(h,i);
            };
        }
        return reports.stream().filter(p -> !isEmptyPredicate.test(p)).collect(Collectors.toList());
    }
    
    @Override
    public void enter(ViewChangeEvent event) {
        //refreshAccounts
        List<Account> accounts = accountService.list(userIdentity.getProfile());
        if (filterAccounts.getContainerDataSource().size()!=accounts.size() || accounts.stream().anyMatch(i -> filterAccounts.getItem(i)==null)) {
            filterAccounts.setContainerDataSource(new BeanItemContainer<>(Account.class, accounts));
            filterAccounts.getItemIds().stream().forEach(i -> filterAccounts.select(i));            
        }

        List<Category> listCategories = categoryService.listCategories(userIdentity.getProfile());
        if (filterCategoryTree.getContainerDataSource().size()!=listCategories.size() || listCategories.stream().anyMatch(i -> filterCategoryTree.getItem(i)==null)) {
            CategoryContainer filterCategoryContainer = new CategoryContainer();
            filterCategoryContainer.addAll(listCategories);
            filterCategoryTree.setContainerDataSource(filterCategoryContainer);
            listCategories.stream().forEach(i -> filterCategoryTree.select(i));
            listCategories.forEach(it -> filterCategoryTree.setCollapsed(it, false));
        }
        
        filterChanged();
    }

}

class CategoryReportRow extends CategoryReport {
    
    private String name;
    
    private boolean generated;
    
    public CategoryReportRow(CategoryReport categoryReport) {
        super(categoryReport.getCategory());
        if (categoryReport.getCategory() != null) {
            this.name = categoryReport.getCategory().getName();
        }
        this.setIncomesAndExpenses(categoryReport.getIncomesAndExpenses());
    }

    public CategoryReportRow() {
        super(null);
        this.setIncomesAndExpenses(LazyList.decorate(new ArrayList<>(), IncomeExpenseTouple::new));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isGenerated() {
        return generated;
    }

    public void setGenerated(boolean generated) {
        this.generated = generated;
    }

}
