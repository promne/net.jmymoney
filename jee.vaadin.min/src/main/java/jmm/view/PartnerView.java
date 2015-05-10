package jmm.view;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.Item;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import jee.vaadin.min.UserIdentity;
import jmm.component.DialogResultType;
import jmm.component.StringInputDialog;
import jmm.dialog.PartnerModifyDialog;
import jmm.entity.Payee;
import jmm.service.SplitPartnerService;

@CDIView(value=PartnerView.NAME)
public class PartnerView extends VerticalLayout implements View {

    public static final String NAME = "PartnerView";

    private static final String COLUMN_NAME = "Name";
    
    private static final String COLUMN_VALUE = "column_value";
    
    @Inject
    private UserIdentity userIdentity;
    
    @Inject
    private SplitPartnerService splitPartnerService;
    
    @Inject
    private Instance<PartnerModifyDialog> partnerModifyDialogInstance;
    
    private Table partnerTable;
    
    private Button deleteButton;

    private Button editButton;
    
    @PostConstruct
    private void init() {
        setSizeFull();
        setSpacing(true);

        
        partnerTable = new Table();
        partnerTable.setSizeFull();
        partnerTable.setSelectable(true);
        partnerTable.addContainerProperty(COLUMN_NAME, String.class, null);
//        partnerTable.addContainerProperty(COLUMN_BALANCE, BigDecimal.class, null);
        partnerTable.addContainerProperty(COLUMN_VALUE, Payee.class, null);
        partnerTable.setVisibleColumns(COLUMN_NAME);
        partnerTable.setSortContainerPropertyId(COLUMN_NAME);
        partnerTable.addValueChangeListener(event -> partnerChangedAction());
        addComponent(partnerTable);

        HorizontalLayout controlButtonsLayout = new HorizontalLayout();
        controlButtonsLayout.addComponent(new Button("New", (ClickListener) event -> newPartnerAction()));
        editButton = new Button("Edit", (ClickListener) event -> editPartnerAction());
        controlButtonsLayout.addComponent(editButton);
        
        deleteButton = new Button("Delete", (ClickListener) event -> deletePartnerAction());
        controlButtonsLayout.addComponent(deleteButton);
        
        addComponent(controlButtonsLayout );
        
        setExpandRatio(partnerTable, 1.0f);             
    }
    
    private void partnerChangedAction() {
        Object selectedId = partnerTable.getValue();
        deleteButton.setEnabled(selectedId!=null);
        editButton.setEnabled(selectedId!=null);
    }

    private void deletePartnerAction() {
        // TODO Auto-generated method stub
    }

    private void editPartnerAction() {
        Object itemId = partnerTable.getValue();
        if (itemId != null) {
                Item item = partnerTable.getItem(itemId);
                Payee payee = ((Payee) item.getItemProperty(COLUMN_VALUE).getValue());
                partnerModifyDialogInstance.get().renamePartner(payee, (closeType, resultValue) -> {
                    updateTableValues(item, payee);
                    partnerTable.sort();
                });
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

    @Override
    public void enter(ViewChangeEvent event) {
        refreshPartners();
    }

    private void refreshPartners() {
        List<Payee> partnerList = splitPartnerService.listPayees(userIdentity.getUserAccount());
        partnerTable.removeAllItems();
        
        partnerList.forEach(partner -> {
            Item newItem = partnerTable.addItem(partner.getId());
            newItem.getItemProperty(COLUMN_VALUE).setValue(partner);        
            updateTableValues(newItem, partner);
        });
        partnerChangedAction();
        partnerTable.sort();
    }
    
    private void updateTableValues(Item item, Payee payee) {
        item.getItemProperty(COLUMN_NAME).setValue(payee.getName());
    }
    
    private void updateTableValues(Object itemId) {
        Item item = partnerTable.getItem(itemId);
        Payee payee = (Payee)item.getItemProperty(COLUMN_VALUE).getValue();
        updateTableValues(item, payee);
    }
    
}

