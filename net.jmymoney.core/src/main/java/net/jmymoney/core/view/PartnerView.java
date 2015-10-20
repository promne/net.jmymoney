package net.jmymoney.core.view;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.Item;
import com.vaadin.data.fieldgroup.FieldGroup.CommitEvent;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.fieldgroup.FieldGroup.CommitHandler;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.GeneratedPropertyContainer;
import com.vaadin.data.util.PropertyValueGenerator;
import com.vaadin.data.util.filter.SimpleStringFilter;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.Grid.HeaderCell;
import com.vaadin.ui.Grid.HeaderRow;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.renderers.ButtonRenderer;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import net.jmymoney.core.UserIdentity;
import net.jmymoney.core.component.DialogResultType;
import net.jmymoney.core.component.StringInputDialog;
import net.jmymoney.core.component.TransactionSplitGrid;
import net.jmymoney.core.data.TransactionSplitContainer;
import net.jmymoney.core.entity.Account;
import net.jmymoney.core.entity.Payee;
import net.jmymoney.core.entity.Transaction;
import net.jmymoney.core.entity.TransactionSplit;
import net.jmymoney.core.service.SplitPartnerService;
import net.jmymoney.core.service.TransactionService;
import net.jmymoney.core.util.PropertyResolver;

@CDIView(value=PartnerView.NAME)
public class PartnerView extends VerticalLayout implements View {

    public static final String NAME = "PartnerView";

    @Inject
    private UserIdentity userIdentity;
    
    @Inject
    private SplitPartnerService splitPartnerService;
    
    @Inject
    private TransactionService transactionService;
    
    private Grid partnerGrid;
    private BeanContainer<Long, Payee> partnerContainer = new BeanContainer<>(Payee.class);
    private GeneratedPropertyContainer gpc = new GeneratedPropertyContainer(partnerContainer);
    private static final String PROPERTY_DELETE = "action_delete";

    private TransactionSplitContainer partnerTransactionContainer = new TransactionSplitContainer();
    private TransactionSplitGrid partnerTransactionGrid = new TransactionSplitGrid(partnerTransactionContainer);
    
    
    @PostConstruct
    private void init() {
        setSizeFull();
        setSpacing(true);
        setMargin(true);

        partnerContainer.setBeanIdProperty(Payee.PROPERTY_ID);
        
        gpc.addGeneratedProperty(PROPERTY_DELETE, new PropertyValueGenerator<String>() {
            @Override
            public String getValue(Item item, Object itemId, Object propertyId) {
                return "Delete";
            }

            @Override
            public Class<String> getType() {
                return String.class;
            }
        });
        
        partnerGrid = new Grid(gpc);
        partnerGrid.setSizeFull();
        partnerGrid.setEditorEnabled(true);
        partnerGrid.addSelectionListener(e -> refreshPartnerTransactions());

        partnerGrid.removeAllColumns();
        partnerGrid.addColumn(Payee.PROPERTY_NAME).setExpandRatio(1);
        partnerGrid.addColumn(Payee.PROPERTY_DESCRIPTION).setExpandRatio(1);
        partnerGrid.addColumn(PROPERTY_DELETE)
            .setHeaderCaption("Delete")
            .setRenderer((new ButtonRenderer(e -> this.deleteItem(e.getItemId()))))
            .setEditable(false);
        partnerGrid.sort(Payee.PROPERTY_NAME);
        
        //filtering
        HeaderRow filterRow = partnerGrid.appendHeaderRow();        
        for (Object pid : partnerContainer.getContainerPropertyIds()) {
            Column column = partnerGrid.getColumn(pid);
            if (column != null) {
                HeaderCell headerFilterCell = filterRow.getCell(pid);
                HorizontalLayout cellContent = new HorizontalLayout();
                cellContent.setSpacing(true);
                cellContent.addComponent(new Label("Filter"));
                
                TextField filterField = new TextField();
                filterField.addTextChangeListener(change -> {
                    // (Re)create the filter if necessary
                    partnerContainer.removeContainerFilters(pid);
                    if (!change.getText().isEmpty()) {
                        partnerContainer.addContainerFilter(new SimpleStringFilter(pid, change.getText(), true, false));
                    }
                });
                cellContent.addComponent(filterField);
                headerFilterCell.setComponent(cellContent);
            }
        }
        
        partnerGrid.getEditorFieldGroup().addCommitHandler(new CommitHandler() {
            @Override
            public void preCommit(CommitEvent commitEvent) throws CommitException {
                // nothing
            }
            
            @Override
            public void postCommit(CommitEvent commitEvent) throws CommitException {
                BeanItem<Payee> item = partnerContainer.getItem(commitEvent.getFieldBinder().getItemDataSource().getItemProperty(Payee.PROPERTY_ID).getValue());
                splitPartnerService.update(item.getBean());
            }
        });
        
        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setSpacing(true);
        contentLayout.addComponent(partnerGrid);
        contentLayout.addComponent(new Button("New", (ClickListener) event -> newPartnerAction()));
        contentLayout.setExpandRatio(partnerGrid, 1.0f);
        
        partnerTransactionGrid.setSizeFull();
        partnerTransactionGrid.removeAllColumns();
        partnerTransactionGrid.addColumn(PropertyResolver.chainPropertyName(TransactionSplit.PROPERTY_TRANSACTION, Transaction.PROPERTY_TIMESTAMP));
        partnerTransactionGrid.addColumn(PropertyResolver.chainPropertyName(TransactionSplit.PROPERTY_TRANSACTION, Transaction.PROPERTY_ACCOUNT, Account.PROPERTY_NAME));
        partnerTransactionGrid.addColumn(TransactionSplitGrid.PROPERTY_CATEGORY_LABEL);
        partnerTransactionGrid.addColumn(TransactionSplit.PROPERTY_AMOUNT);
        partnerTransactionGrid.addColumn(TransactionSplit.PROPERTY_NOTE);
        partnerTransactionGrid.sort(PropertyResolver.chainPropertyName(TransactionSplit.PROPERTY_TRANSACTION, Transaction.PROPERTY_TIMESTAMP), SortDirection.DESCENDING);
        
        VerticalSplitPanel splitPanel = new VerticalSplitPanel(contentLayout, partnerTransactionGrid);
        splitPanel.setSplitPosition(66, Unit.PERCENTAGE);
        addComponent(splitPanel);
    }
    
    private void refreshPartnerTransactions() {
        Collection<Object> selectedRows = partnerGrid.getSelectedRows();
        partnerTransactionContainer.removeAllItems();
        if (!selectedRows.isEmpty()) {
            for (Object itemId : selectedRows) {
                BeanItem<Payee> item = partnerContainer.getItem(itemId);
                List<TransactionSplit> transactionSplits = transactionService.listTransactionSplit(item.getBean());
                partnerTransactionContainer.addAll(transactionSplits);
            }
        }
    }
    
    private void newPartnerAction() {
        StringInputDialog newCategoryDialog = new StringInputDialog("New Partner", "Enter a name of the new partner");
        newCategoryDialog.setDialogResultListener((closeType, resultValue) -> {
                if (DialogResultType.OK.equals(closeType)) {
                        Payee payee = new Payee();
                        payee.setName((String) resultValue);
                        payee.setUserAccount(userIdentity.getUserAccount());

                        splitPartnerService.create(payee);
                        refreshPartners();
                }
        });
        newCategoryDialog.show();
    }
    

    public void deleteItem(Object itemId) {
        Payee payee = partnerContainer.getItem(itemId).getBean();
        boolean deleted = splitPartnerService.delete(payee.getId());
        if (deleted) {
            Notification.show(String.format("Partner \"%s\" was deleted", payee.getName()));
            refreshPartners();
        } else {
            Notification.show(String.format("Unable to delete partner \"%s\" because it is used", payee.getName()), Type.WARNING_MESSAGE);
        }        
    }

    @Override
    public void enter(ViewChangeEvent event) {
        refreshPartners();
        if (event.getParameters() != null && !event.getParameters().isEmpty()) {
            try {
                Long partnerId = Long.parseLong(event.getParameters().toString());
                partnerGrid.select(partnerId);
                partnerGrid.scrollTo(partnerId);
            } catch (NumberFormatException e) {
                refreshPartnerTransactions();
            }
        } else {
            refreshPartnerTransactions();
        }
    }
   
    private void refreshPartners() {
        partnerContainer.removeAllItems();
        partnerContainer.addAll(splitPartnerService.listPayees(userIdentity.getUserAccount()));
    }

    public static void navigateWithPartner(Payee payee) {
        UI.getCurrent().getNavigator().navigateTo(NAME + "/" + payee.getId());
    }
    
}

