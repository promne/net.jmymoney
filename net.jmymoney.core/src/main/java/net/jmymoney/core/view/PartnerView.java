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
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ButtonRenderer;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import net.jmymoney.core.UserIdentity;
import net.jmymoney.core.component.DialogResultType;
import net.jmymoney.core.component.StringInputDialog;
import net.jmymoney.core.entity.Payee;
import net.jmymoney.core.service.SplitPartnerService;

@CDIView(value=PartnerView.NAME)
public class PartnerView extends VerticalLayout implements View {

    public static final String NAME = "PartnerView";

    @Inject
    private UserIdentity userIdentity;
    
    @Inject
    private SplitPartnerService splitPartnerService;
    
    private Grid partnerGrid;
    
    private BeanContainer<Long, Payee> partnerContainer = new BeanContainer<Long, Payee>(Payee.class);
    private GeneratedPropertyContainer gpc = new GeneratedPropertyContainer(partnerContainer);
    private static final String PROPERTY_DELETE = "action_delete";
    
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

        partnerGrid.removeAllColumns();
        partnerGrid.addColumn(Payee.PROPERTY_NAME).setExpandRatio(1);
        partnerGrid.addColumn(Payee.PROPERTY_DESCRIPTION).setExpandRatio(1);
        partnerGrid.addColumn(PROPERTY_DELETE)
            .setHeaderCaption("Delete")
            .setRenderer((new ButtonRenderer(e -> this.deleteItem(e.getItemId()))))
            .setEditable(false);
        
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
        
        addComponent(partnerGrid);
        setExpandRatio(partnerGrid, 1.0f);
        
        addComponent(new Button("New", (ClickListener) event -> newPartnerAction()));
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
    }
   
    private void refreshPartners() {
        partnerContainer.removeAllItems();
        partnerContainer.addAll(splitPartnerService.listPayees(userIdentity.getUserAccount()));
        partnerGrid.sort(Payee.PROPERTY_NAME);
    }
    
    
}

