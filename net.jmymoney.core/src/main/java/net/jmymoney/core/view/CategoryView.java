package net.jmymoney.core.view;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.util.BeanItem;
import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.event.dd.acceptcriteria.SourceIsTarget;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.shared.ui.dd.VerticalDropLocation;
import com.vaadin.ui.AbstractSelect.AbstractSelectTargetDetails;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table.TableDragMode;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import net.jmymoney.core.UserIdentity;
import net.jmymoney.core.component.DialogResultType;
import net.jmymoney.core.component.StringInputDialog;
import net.jmymoney.core.component.TransactionSplitGrid;
import net.jmymoney.core.data.CategoryContainer;
import net.jmymoney.core.data.TransactionSplitContainer;
import net.jmymoney.core.entity.Account;
import net.jmymoney.core.entity.Category;
import net.jmymoney.core.entity.SplitPartner;
import net.jmymoney.core.entity.Transaction;
import net.jmymoney.core.entity.TransactionSplit;
import net.jmymoney.core.service.CategoryService;
import net.jmymoney.core.service.TransactionService;
import net.jmymoney.core.util.PropertyResolver;

@CDIView(value = CategoryView.NAME)
public class CategoryView extends VerticalLayout implements View {

    public static final String NAME = "CategoryView";

    @Inject
    private UserIdentity userIdentity;

    @Inject
    private CategoryService categoryService;
    
    @Inject
    private TransactionService transactionService;

    private CategoryContainer categoryContainer = new CategoryContainer();
    private TreeTable categoryTree = new TreeTable(null, categoryContainer);

    private Button deleteButton;

    private Button editButton;
    
    private TransactionSplitContainer transactionSplitContainer = new TransactionSplitContainer();
    private TransactionSplitGrid transactionSplitGrid = new TransactionSplitGrid(transactionSplitContainer);

    @PostConstruct
    private void init() {
        setSizeFull();
        setSpacing(true);
        setMargin(true);

        categoryTree.setSizeFull();
        categoryTree.setSelectable(true);
        categoryTree.setVisibleColumns(Category.PROPERTY_NAME);
        categoryTree.setDragMode(TableDragMode.ROW);
        categoryTree.setDropHandler(new DropHandler() {

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return SourceIsTarget.get();
            }

            @Override
            public void drop(DragAndDropEvent event) {
                final Transferable transferable = event.getTransferable();
                final Object sourceItemId = ((DataBoundTransferable) transferable).getItemId();

                final AbstractSelectTargetDetails dropData = ((AbstractSelectTargetDetails) event.getTargetDetails());
                final Object targetItemId = dropData.getItemIdOver();

                Category candidateParent = null;
                Category dragCategory = categoryContainer.getItem(sourceItemId).getBean();

                if (targetItemId != null) {
                    candidateParent = categoryContainer.getItem(targetItemId).getBean();
                    if (Arrays.asList(VerticalDropLocation.BOTTOM, VerticalDropLocation.TOP).contains(dropData.getDropLocation())) {
                        candidateParent = candidateParent.getParent();
                    }
                }

                // check target is not a child of current item
                if (candidateParent != null) {
                    for (Category visitor = candidateParent; visitor != null; visitor = visitor.getParent()) {
                        if (visitor.getId().equals(dragCategory.getId())) {
                            return;
                        }
                    }
                }

                // and finally update
                dragCategory.setParent(candidateParent);
                categoryService.update(dragCategory);
                refreshCategories();
            }
        });
        categoryTree.addValueChangeListener(event -> categoryChangedAction());

        
        HorizontalLayout controlButtonsLayout = new HorizontalLayout();
        controlButtonsLayout.setSpacing(true);
        controlButtonsLayout.addComponent(new Button("New", (ClickListener) event -> newCategoryAction()));
        editButton = new Button("Edit", (ClickListener) event -> editCategoryAction());
        controlButtonsLayout.addComponent(editButton);
        
        deleteButton = new Button("Delete", (ClickListener) event -> deleteCategoryAction());
        controlButtonsLayout.addComponent(deleteButton);
        
        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setSpacing(true);
        contentLayout.addComponent(categoryTree);
        contentLayout.addComponent(controlButtonsLayout);
        contentLayout.setExpandRatio(categoryTree, 1.0f);
        addComponent(contentLayout);


        transactionSplitGrid.setSizeFull();
        transactionSplitGrid.removeAllColumns();
        transactionSplitGrid.addColumn(PropertyResolver.chainPropertyName(TransactionSplit.PROPERTY_TRANSACTION, Transaction.PROPERTY_TIMESTAMP));
        transactionSplitGrid.addColumn(PropertyResolver.chainPropertyName(TransactionSplit.PROPERTY_TRANSACTION, Transaction.PROPERTY_ACCOUNT, Account.PROPERTY_NAME));
        transactionSplitGrid.addColumn(PropertyResolver.chainPropertyName(TransactionSplit.PROPERTY_SPLIT_PARTNER, SplitPartner.PROPERTY_NAME));
        transactionSplitGrid.addColumn(TransactionSplit.PROPERTY_AMOUNT);
        transactionSplitGrid.addColumn(TransactionSplit.PROPERTY_NOTE);
        transactionSplitGrid.sort(PropertyResolver.chainPropertyName(TransactionSplit.PROPERTY_TRANSACTION, Transaction.PROPERTY_TIMESTAMP), SortDirection.DESCENDING);
                
        
        VerticalSplitPanel splitPanel = new VerticalSplitPanel(contentLayout, transactionSplitGrid);
        splitPanel.setSplitPosition(66, Unit.PERCENTAGE);
        addComponent(splitPanel);
    }

    private void deleteCategoryAction() {
        Category curCategory = categoryContainer.getItem(categoryTree.getValue()).getBean();
        boolean deleted = categoryService.delete(curCategory.getId());

        if (deleted) {
            Notification.show(String.format("Category \"%s\" was deleted", curCategory.getName()));
            refreshCategories();
        } else {
            Notification.show(String.format("Unable to delete category \"%s\" because it is used", curCategory.getName()), Type.WARNING_MESSAGE);
        }
    }

    private void categoryChangedAction() {
        Object selectedId = categoryTree.getValue();
        deleteButton.setEnabled(selectedId != null);
        editButton.setEnabled(selectedId != null);
        refreshCategoryTransactions();
    }

    private void editCategoryAction() {
        Object itemId = categoryTree.getValue();
        if (itemId != null) {
            Category curCategory = categoryContainer.getItem(itemId).getBean();
            StringInputDialog categoryNameDialog = new StringInputDialog("Edit category", "Enter name of category");
            categoryNameDialog.setValue(curCategory.getName());
            categoryNameDialog.setDialogResultListener((closeType, resultValue) -> {
                if (DialogResultType.OK.equals(closeType)) {
                    curCategory.setName((String) resultValue);
                    categoryService.update(curCategory);
                    refreshCategories();
                }
            });

            categoryNameDialog.show();
        }
    }

    protected void newCategoryAction() {
        StringInputDialog newCategoryDialog = new StringInputDialog("New category", "Enter name of category");
        newCategoryDialog.setDialogResultListener((closeType, resultValue) -> {
            if (DialogResultType.OK.equals(closeType)) {
                Category category = new Category();
                category.setName((String) resultValue);
                category.setProfile(userIdentity.getProfile());
                categoryService.create(category);
                refreshCategories();
            }
        });
        newCategoryDialog.show();
    }

    @Override
    public void enter(ViewChangeEvent event) {
        refreshCategories();
        if (event.getParameters() != null && !event.getParameters().isEmpty()) {
            try {
                Long categoryId = Long.parseLong(event.getParameters().toString());
                Optional<Category> category = categoryContainer.getCategory(categoryId);
                if (category.isPresent()) {
                    categoryTree.select(category.get());                    
                }
            } catch (NumberFormatException e) {
                //nothing, go on
            }
        }
    }

    private void refreshCategoryTransactions() {
        BeanItem<Category> item = categoryContainer.getItem(categoryTree.getValue());
        transactionSplitContainer.removeAllItems();
        if (item!=null) {
            transactionSplitContainer.addAll(transactionService.listTransactionSplit(item.getBean()));
        }
    }
    
    private void refreshCategories() {
        categoryContainer.removeAllItems();
        List<Category> listCategories = categoryService.listCategories(userIdentity.getProfile());
        categoryContainer.addAll(listCategories);
        categoryContainer.getItemIds().forEach(it -> categoryTree.setCollapsed(it, false));

        categoryChangedAction();
    }

    public static void navigate(Category category) {
        UI.getCurrent().getNavigator().navigateTo(NAME + "/" + category.getId());
    }
    
}
