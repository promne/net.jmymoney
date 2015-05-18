package net.jmymoney.core.view;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.Item;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DateField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;

import java.text.DateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.collections.list.LazyList;
import org.slf4j.Logger;

import net.jmymoney.core.UserIdentity;
import net.jmymoney.core.domain.CategoryReport;
import net.jmymoney.core.domain.IncomeExpenseTouple;
import net.jmymoney.core.entity.Category;
import net.jmymoney.core.service.ReportingService;
import net.jmymoney.core.theme.ThemeResourceConstatns;
import net.jmymoney.core.util.DateMonthRoundConverter;

@CDIView(value = ReportView.NAME)
public class ReportView extends VerticalLayout implements View {

    public static final String NAME = "ReportView";

    private static final String COLUMN_DATE = "column_amount";
    private static final String COLUMN_NAME = "column_name";
    private static final String COLUMN_CATEGORY_REPORT = "column_category_report";
    private static final String COLUMN_SUM_ALL_TIME = "column_sum_all_time";
    

    private ComboBox granularityComboBox;
    private DateField filterFromDate;
    private DateField filterToDate;

    @Inject
    private Logger log;

    @Inject
    private UserIdentity userIdentity;

    @Inject
    private ReportingService reportingService;

    private TreeTable reportTable;

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

        addComponent(filterLayout);

        reportTable = new TreeTable("Report - incomes and expenses");
        reportTable.setSizeFull();
        reportTable.setSelectable(true);
        reportTable.addContainerProperty(COLUMN_CATEGORY_REPORT, CategoryReport.class, null);

        reportTable.addGeneratedColumn(COLUMN_NAME, (source, itemId, columnId) -> {
            Item item = reportTable.getItem(itemId);
            if (item.getItemProperty(COLUMN_CATEGORY_REPORT).getValue() != null) {
                String rowName = ((CategoryReportRow) item.getItemProperty(COLUMN_CATEGORY_REPORT).getValue()).getName();
                return rowName == null ? "Without category" : rowName;
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
                    return ThemeResourceConstatns.TABLE_CELL_STYLE_HIGHLIGHT;
                }
            }
            return null;
        } );        
        
        reportTable.setColumnCollapsingAllowed(false);
        reportTable.setSortEnabled(false);
        reportTable.setVisibleColumns(COLUMN_NAME, COLUMN_SUM_ALL_TIME);
        reportTable.setColumnHeaders("Category", "SUM");

        addComponent(reportTable);
        setExpandRatio(reportTable, 1.0f);
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

        List<CategoryReport> reports = reportingService.getCategoryReport(userIdentity.getUserAccount(), fromDate,
                toDate, temporalUnit);
        int reportSize = reports.get(0).getIncomesAndExpenses().size();
        Collections.sort(reports, (c1, c2) -> {
            if (c1.getCategory() == null) {
                return c2.getCategory() == null ? 0 : 1;
            }
            if (c2.getCategory() == null) {
                return c1.getCategory() == null ? 0 : -1;
            }
            return c1.getCategory().getName().compareToIgnoreCase(c2.getCategory().getName());
        } );

        reportTable.removeAllItems();

        // TODO: smarter replace
        for (int genColumnId = 0; reportTable.getColumnGenerator(COLUMN_DATE + genColumnId) != null; genColumnId++) {
            reportTable.removeGeneratedColumn(COLUMN_DATE + genColumnId);
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
            for (int i=0; i< report.getIncomesAndExpenses().size(); i++) {
                sumRow.getIncomesAndExpenses().get(i).add(report.getIncomesAndExpenses().get(i));
            }
        }
        Object newItemId = reportTable.addItem();
        Item newItem = reportTable.getItem(newItemId);
        newItem.getItemProperty(COLUMN_CATEGORY_REPORT).setValue(sumRow);
        
        
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
    
    @Override
    public void enter(ViewChangeEvent event) {
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
