package net.jmymoney.core.view;

import com.vaadin.addon.contextmenu.ContextMenu;
import com.vaadin.addon.contextmenu.ContextMenu.ContextMenuOpenListener;
import com.vaadin.cdi.CDIView;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.shared.ui.table.TableConstants.Section;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.TableContextClickEvent;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import net.jmymoney.core.UserIdentity;
import net.jmymoney.core.component.tools.TableHelper;
import net.jmymoney.core.component.transaction.TransactionFieldWrapper;
import net.jmymoney.core.entity.Account;
import net.jmymoney.core.entity.Payee;
import net.jmymoney.core.entity.SplitPartner;
import net.jmymoney.core.entity.Transaction;
import net.jmymoney.core.entity.TransactionSplit;
import net.jmymoney.core.i18n.I18nResourceConstant;
import net.jmymoney.core.i18n.MessagesResourceBundle;
import net.jmymoney.core.service.AccountService;
import net.jmymoney.core.service.TransactionService;
import net.jmymoney.core.theme.ThemeResourceConstatns;
import net.jmymoney.core.theme.ThemeStyles;

@CDIView(value = TransactionView.NAME)
public class TransactionView extends VerticalLayout implements View {

    public static final String NAME = "TransactionView";

    private static final String COLUMN_AMOUNT_RUNNING = "amountRunning";
    private static final String COLUMN_WITHDRAWAL = "withdrawal";
    private static final String COLUMN_DEPOSIT = "deposit";
    private static final String COLUMN_DATE = "date";
    private static final Object COLUMN_DETAIL = "detail";

    @Inject
    private UserIdentity userIdentity;

    @Inject
    private AccountService accountService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private TransactionFieldWrapper transactionField;
    
    @Inject
    private MessagesResourceBundle messagesResourceBundle;

    private BeanItemContainer<TransactionWrapper> transactionContainer;
    private Table transactionTable;

    private BeanItemContainer<Account> accountContainer;
    private ComboBox accountComboBox;
    private Label accountBalanceLabel;

    private Button saveButton;

    private Button createButton;

    private Button editButton;

    private Button deleteButton;

    private Button cancelButton;

    @PostConstruct
    private void init() {
        setSizeFull();
        setSpacing(true);
        setMargin(true);

        HorizontalLayout accountInfoLayout = new HorizontalLayout();
        accountInfoLayout.setSpacing(true);
        
        accountContainer = new BeanItemContainer<>(Account.class);

        accountComboBox = new ComboBox(messagesResourceBundle.getString(I18nResourceConstant.TRANSACTIONS_ACCOUNT), accountContainer);
        accountComboBox.setItemCaptionPropertyId("name");
        accountComboBox.setItemCaptionMode(ItemCaptionMode.PROPERTY);

        accountComboBox.setInputPrompt(messagesResourceBundle.getString(I18nResourceConstant.TRANSACTIONS_ACTION_ACCOUNT_SELECT));
        accountComboBox.setNullSelectionAllowed(false);
        accountComboBox.setFilteringMode(FilteringMode.CONTAINS);
        accountComboBox.setImmediate(true);

        accountComboBox.addValueChangeListener(event -> loadTransactions((Account) event.getProperty().getValue()));
        accountInfoLayout.addComponent(accountComboBox);
        
        accountBalanceLabel = new Label();
        accountBalanceLabel.setCaption(messagesResourceBundle.getString(I18nResourceConstant.UNIVERSAL_BALANCE));
        accountBalanceLabel.setSizeUndefined();
        accountBalanceLabel.setImmediate(true);
        accountInfoLayout.addComponent(accountBalanceLabel);
        
        addComponent(accountInfoLayout);

        transactionContainer = new BeanItemContainer<>(TransactionWrapper.class);
        transactionContainer.addNestedContainerProperty("transaction.timestamp");

        transactionTable = new Table(messagesResourceBundle.getString(I18nResourceConstant.TRANSACTIONS_TRANSACTIONS), transactionContainer);
        transactionTable.setSizeFull();
        transactionTable.setSelectable(true);
        transactionTable.addStyleName(ThemeStyles.TABLE_COLORED);

        
        
        ContextMenuOpenListener tableOpenListener = event -> {
            event.getContextMenu().removeItems();

            TableContextClickEvent tableEvent = (TableContextClickEvent) event.getContextClickEvent();
            TransactionWrapper selectedItem =  (TransactionWrapper) tableEvent.getItemId();
            
            if (Section.BODY==tableEvent.getSection() && selectedItem!=null) {
                
                event.getContextMenu().addItem(messagesResourceBundle.getString(I18nResourceConstant.TRANSACTIONS_ACTION_CREATE), ThemeResourceConstatns.CREATE, e1 -> createTransaction());
                if (!selectedItem.getTransaction().isChild()) {
                    event.getContextMenu().addItem(messagesResourceBundle.getString(I18nResourceConstant.TRANSACTIONS_ACTION_COPY), ThemeResourceConstatns.COPY, e2 -> copySelectedTransaction());
                }
                event.getContextMenu().addItem(messagesResourceBundle.getString(I18nResourceConstant.TRANSACTIONS_ACTION_EDIT), ThemeResourceConstatns.EDIT, e3 -> editSelectedTransaction());
                event.getContextMenu().addItem(messagesResourceBundle.getString(I18nResourceConstant.TRANSACTIONS_ACTION_DELETE), ThemeResourceConstatns.DELETE, e4 -> deleteSelectedTransaction());
                event.getContextMenu().addSeparator();
                
                for (SplitPartner splitPartner : selectedItem.getTransaction().getSplits().stream().map(sp -> sp.getSplitPartner()).collect(Collectors.toSet())) {
                    if (splitPartner instanceof Payee) {
                        event.getContextMenu().addItem(String.format("Go to payee \"%s\"", splitPartner.getName()), e5 -> PartnerView.navigateWithPartner((Payee)splitPartner));
                    }
                    if (splitPartner instanceof Account) {
                        event.getContextMenu().addItem(String.format("Go to account \"%s\"", splitPartner.getName()), e6 -> TransactionView.navigate((Account)splitPartner));
                    }
                }
                
                for (TransactionSplit split : selectedItem.getTransaction().getSplits()) {
                    if (split.getParent() != null) {
                        TransactionSplit splitParent = split.getParent();
                        event.getContextMenu().addItem(String.format("Go to parent transaction of \"%s\"", split.getSplitPartner().getName()), e7 -> TransactionView.navigate(splitParent.getTransaction()));
                    } else if (split.getChildren()!=null) {
                        for (TransactionSplit childSplit : split.getChildren()) {
                            event.getContextMenu().addItem(String.format("Go to child transaction of \"%s\"", split.getSplitPartner().getName()), e8 -> TransactionView.navigate(childSplit.getTransaction()));                            
                        }
                    }
                }
                selectedItem.getTransaction().getSplits().stream().filter(s1 -> s1.getCategory() != null).map(s2 -> s2.getCategory())
                        .forEach(c -> event.getContextMenu().addItem(String.format("Go to category \"%s\"", c.getName()), e9 -> CategoryView.navigate(c)));
            }
        };

        ContextMenu contextMenu = new ContextMenu(this, false);
        contextMenu.setAsContextMenuOf(transactionTable);
        contextMenu.addContextMenuOpenListener(tableOpenListener);        
        
        
        transactionTable.addGeneratedColumn(COLUMN_DATE, (source, itemId, columnId) -> {
            return DateFormat.getDateInstance(DateFormat.MEDIUM, messagesResourceBundle.getLocale()).format(((TransactionWrapper) itemId).getTransaction().getTimestamp());
        } );
        transactionTable.setColumnHeader(COLUMN_DATE, messagesResourceBundle.getString(I18nResourceConstant.TRANSACTIONS_DATE));
        
        transactionTable.addGeneratedColumn(COLUMN_DETAIL, (source, itemId, columnId) -> {
            Transaction transaction = ((TransactionWrapper) itemId).getTransaction();
            StringBuilder sb = new StringBuilder();

            if (transaction.isChild()) {
                sb.append("[X] ");
            }

            boolean firstSplit = true;
            for (TransactionSplit split : transaction.getSplits()) {
                if (!firstSplit) {
                    sb.append(", ");
                }
                firstSplit &= false;

                boolean hasPartner = split.getSplitPartner() != null;
                if (hasPartner) {
                    sb.append(split.getSplitPartner().getName());
                }
                boolean hasCategory = split.getCategory() != null;
                boolean hasNote = split.getNote() != null;
                if (hasPartner && (hasCategory || hasNote)) {
                    sb.append(" (");
                }
                if (hasCategory) {
                    sb.append(split.getCategory().getName());
                }
                if (hasCategory && hasNote) {
                    sb.append(" // ");
                }
                if (hasNote) {
                    sb.append(split.getNote());
                }
                if (hasPartner && (hasCategory || hasNote)) {
                    sb.append(")");
                }
            }
            return sb.toString();
        } );
        transactionTable.setColumnHeader(COLUMN_DETAIL, messagesResourceBundle.getString(I18nResourceConstant.TRANSACTIONS_DETAIL));
        
        transactionTable.addGeneratedColumn(COLUMN_DEPOSIT, (source, itemId, columnId) -> {
            BigDecimal amount = ((TransactionWrapper) itemId).getTransaction().getAmount();
            return amount.signum() != -1 ? amount.stripTrailingZeros().toPlainString() : null;
        } );
        transactionTable.setColumnHeader(COLUMN_DEPOSIT, messagesResourceBundle.getString(I18nResourceConstant.TRANSACTIONS_DEPOSIT));
        
        transactionTable.addGeneratedColumn(COLUMN_WITHDRAWAL, (source, itemId, columnId) -> {
            BigDecimal amount = ((TransactionWrapper) itemId).getTransaction().getAmount();
            return amount.signum() == -1 ? amount.negate().stripTrailingZeros().toPlainString() : null;
        } );
        transactionTable.setColumnHeader(COLUMN_WITHDRAWAL, messagesResourceBundle.getString(I18nResourceConstant.TRANSACTIONS_WITHDRAWL));

        transactionTable.setColumnHeader(COLUMN_AMOUNT_RUNNING, messagesResourceBundle.getString(I18nResourceConstant.UNIVERSAL_BALANCE));

        transactionTable.setColumnCollapsingAllowed(false);
        transactionTable.setSortEnabled(false);
        transactionTable.setVisibleColumns(COLUMN_DATE, COLUMN_DETAIL, COLUMN_DEPOSIT, COLUMN_WITHDRAWAL,
                COLUMN_AMOUNT_RUNNING);
        transactionTable.setColumnExpandRatio(COLUMN_DETAIL, 1);
        transactionTable.addValueChangeListener(event -> loadTransaction(event.getProperty().getValue() == null ? null
                : ((TransactionWrapper) event.getProperty().getValue()).getTransaction()));

        transactionTable.setCellStyleGenerator((source, itemId, propertyId) -> {
            if (((TransactionWrapper) itemId).getTransaction().getSplits().stream().anyMatch(split -> split.getCategory() == null)) {
                return ThemeStyles.TABLE_CELL_STYLE_HIGHLIGHT;
            }
            return null;
        } );

        
        addComponent(transactionTable);

        HorizontalLayout transactionActionLayout = new HorizontalLayout();
        transactionActionLayout.setSpacing(true);

        createButton = new Button(messagesResourceBundle.getString(I18nResourceConstant.TRANSACTIONS_ACTION_CREATE), ThemeResourceConstatns.CREATE);
        createButton.addClickListener(event -> createTransaction());
        transactionActionLayout.addComponent(createButton);

        editButton = new Button(messagesResourceBundle.getString(I18nResourceConstant.TRANSACTIONS_ACTION_EDIT), ThemeResourceConstatns.EDIT);
        editButton.addClickListener(event -> editSelectedTransaction());
        transactionActionLayout.addComponent(editButton);

        saveButton = new Button(messagesResourceBundle.getString(I18nResourceConstant.TRANSACTIONS_ACTION_SAVE), ThemeResourceConstatns.SAVE);
        saveButton.addClickListener(event -> saveTransaction());
        transactionActionLayout.addComponent(saveButton);

        cancelButton = new Button(messagesResourceBundle.getString(I18nResourceConstant.TRANSACTIONS_ACTION_CANCEL), ThemeResourceConstatns.CANCEL);
        cancelButton.addClickListener(event -> cancelButtonClick());
        transactionActionLayout.addComponent(cancelButton);

        deleteButton = new Button(messagesResourceBundle.getString(I18nResourceConstant.TRANSACTIONS_ACTION_DELETE), ThemeResourceConstatns.DELETE);
        deleteButton.addClickListener(event -> deleteSelectedTransaction());
        transactionActionLayout.addComponent(deleteButton);

        addComponent(transactionActionLayout);

        addComponent(transactionField);
        transactionField.setBuffered(true);
        transactionField.addValueChangeListener(event -> transactionValueChanged(event));
        transactionField.setEnabled(false);

        setExpandRatio(transactionTable, 1.0f);
    }

    private void cancelButtonClick() {
        transactionField.discard();
        transactionField.setEnabled(false);

        boolean editableTransactionSelected = transactionTable.getValue() != null;
        editableTransactionSelected = editableTransactionSelected && !((TransactionWrapper) transactionTable.getValue()).getTransaction().isChild();

        createButton.setEnabled(accountComboBox.getValue() != null);
        editButton.setEnabled(editableTransactionSelected);
        saveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        deleteButton.setEnabled(editableTransactionSelected);
    }

    private void createTransaction() {
        Transaction newTransaction = new Transaction();
        newTransaction.setAccount((Account) accountComboBox.getValue());
        
        Optional<TransactionWrapper> selectedTransaction = getSelectedTransactionWrapper();
        if (selectedTransaction.isPresent()) {
            newTransaction.setTimestamp(selectedTransaction.get().getTransaction().getTimestamp());
        } else {
            newTransaction.setTimestamp(Page.getCurrent().getWebBrowser().getCurrentDate());
        }
        
        newTransaction.getSplits().add(new TransactionSplit());
        loadTransaction(newTransaction);
        transactionField.setEnabled(true);
        transactionField.focus();

        createButton.setEnabled(false);
        editButton.setEnabled(false);
        saveButton.setEnabled(true);
        cancelButton.setEnabled(true);
        deleteButton.setEnabled(false);
    }

    private void copySelectedTransaction() {
        Transaction transactionCopy = new Transaction();
        Transaction.copyTransactionValues(getSelectedTransaction().get(), transactionCopy);
        transactionCopy.setId(null);
        transactionCopy.getSplits().stream().forEach(e -> e.setId(null));
        transactionCopy.setTimestamp(Page.getCurrent().getWebBrowser().getCurrentDate());
        
        transactionService.create(transactionCopy);
        refreshTransactions();
        selectTransaction(transactionCopy);
    }
    
    private void deleteSelectedTransaction() {
        transactionService.delete(((TransactionWrapper) transactionTable.getValue()).getTransaction().getId());
        refreshTransactions();

        createButton.setEnabled(true);
        editButton.setEnabled(false);
        saveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        deleteButton.setEnabled(false);
    }

    private void editSelectedTransaction() {
        transactionField.setEnabled(true);
        transactionField.focus();

        createButton.setEnabled(true);
        editButton.setEnabled(false);
        saveButton.setEnabled(true);
        cancelButton.setEnabled(true);
        deleteButton.setEnabled(true);
    }

    private void saveTransaction() {
        transactionField.commit();
        Transaction value = transactionField.getValue();
        if (value.getId() != null) {
            transactionService.update(value);
        } else {
            transactionService.create(value);
        }
        refreshTransactions();
        selectTransaction(value);

        transactionField.setEnabled(false);

        createButton.setEnabled(true);
        editButton.setEnabled(true);
        saveButton.setEnabled(false);
        cancelButton.setEnabled(false);
        deleteButton.setEnabled(true);
    }

    private void transactionValueChanged(ValueChangeEvent event) {
        saveButton.setEnabled(true);
    }

    private void refreshAccounts() {
        // keep current selection if any
        Account selectedAccount = (Account) accountComboBox.getValue();

        accountComboBox.setValue(null);
        accountBalanceLabel.setValue("0");
        accountContainer.removeAllItems();
        accountContainer.addAll(accountService.list(userIdentity.getProfile()));

        if (selectedAccount != null) {
            for (Account account : accountContainer.getItemIds()) {
                if (account.getId().equals(selectedAccount.getId())) {
                    accountComboBox.select(account);
                    break;
                }
            }
        }
    }

    private void refreshTransactions() {
        // keep current selection if any
        TransactionWrapper selectedItemId = (TransactionWrapper) transactionTable.getValue();

        Account account = (Account) accountComboBox.getValue();
        loadTransactions(account);

        if (selectedItemId != null) {
            selectTransaction(selectedItemId.getTransaction());
        }        
    }

    private void selectTransaction(Transaction transaction) {
        selectTransaction(transaction.getId());
    }
    
    private void selectTransaction(Long transactionId) {
        TransactionWrapper foundItem = null;
        for (TransactionWrapper item : transactionContainer.getItemIds()) {
            if (item.getTransaction().getId().equals(transactionId)) {
                transactionTable.select(item);
                foundItem = item;
                break;
            }
        }
        if (foundItem!=null) {
            TableHelper.putItemInViewport(foundItem, transactionTable);
            loadTransaction(foundItem.getTransaction());
        }
    }

    private void loadTransactions(Account account) {
        transactionContainer.removeAllItems();
        loadTransaction(null);
        if (account == null) {
            return;
        }
        List<Transaction> listTransactions = transactionService.list(account);
        List<TransactionWrapper> list = new ArrayList<>(listTransactions.size());
        BigDecimal runningTotal = BigDecimal.ZERO;
        for (Transaction tr : listTransactions) {
            runningTotal = runningTotal.add(tr.getAmount());
            list.add(new TransactionWrapper(tr, runningTotal));
        }
        Collections.reverse(list);
        transactionContainer.addAll(list);
        transactionTable.refreshRowCache();
        
        String accountBalance = "0";
        Optional<TransactionWrapper> lastTransaction = transactionContainer.getItemIds().stream().sorted( (i,j) -> j.getTransaction().getTimestamp().compareTo(i.getTransaction().getTimestamp())).findFirst();        
        if (lastTransaction.isPresent()) {
            accountBalance = lastTransaction.get().getAmountRunning().stripTrailingZeros().toPlainString();
        }
        accountBalanceLabel.setValue(accountBalance);
        accountBalanceLabel.markAsDirty();        
    }

    private void loadTransaction(Transaction transaction) {
        // TODO prevent change while edit
        cancelButtonClick();

        if (transaction == null) {
            transactionField.setValue(new Transaction());
        } else {
            transactionField.setValue(transaction);
        }
        saveButton.setEnabled(false);
    }

    private Optional<TransactionWrapper> getSelectedTransactionWrapper() {
        return Optional.ofNullable((TransactionWrapper)transactionTable.getValue());
    }

    private Optional<Transaction> getSelectedTransaction() {
        TransactionWrapper transactionWrapper = (TransactionWrapper)transactionTable.getValue();
        if (transactionWrapper == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(transactionWrapper.getTransaction());
    }
    
    @Override
    public void enter(ViewChangeEvent event) {
        if (event.getParameters() != null && !event.getParameters().isEmpty()) {
            String[] navigationParam = event.getParameters().split("-");
            try {
                Long accountId = Long.parseLong(navigationParam[0]);
                for (Account account : accountContainer.getItemIds()) {
                    if (account.getId().equals(accountId)) {
                        accountComboBox.select(account);
                        break;
                    }
                }
                if (navigationParam.length>1) {
                    Long transactionId = Long.parseLong(navigationParam[1]);
                    selectTransaction(transactionId);
                }
            } catch (NumberFormatException e) {
                refreshAccounts();
            }
        } else {
            refreshAccounts();
        }
        
        cancelButtonClick();
    }

    public static void navigate(Account account) {
        UI.getCurrent().getNavigator().navigateTo(NAME + "/" + account.getId());
    }

    public static void navigate(Transaction transaction) {
        UI.getCurrent().getNavigator().navigateTo(NAME + "/" + transaction.getAccount().getId() + "-" + transaction.getId());
    }
    
}
