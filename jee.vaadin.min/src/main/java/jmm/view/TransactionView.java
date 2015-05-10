package jmm.view;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import jee.vaadin.min.UserIdentity;
import jmm.component.TransactionField;
import jmm.entity.Account;
import jmm.entity.Transaction;
import jmm.entity.TransactionSplit;
import jmm.service.AccountService;
import jmm.service.TransactionService;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.CellStyleGenerator;
import com.vaadin.ui.VerticalLayout;

@CDIView(value=TransactionView.NAME)
public class TransactionView extends VerticalLayout implements View {

	private static final MathContext MC_ROUND = new MathContext(2);

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
    private TransactionField transactionField;
    
    private BeanItemContainer<TransactionWrapper> transactionContainer;
    private Table transactionTable;

    private BeanItemContainer<Account> accountContainer;
	private ComboBox accountComboBox;

	private Button saveButton;

	private Button createButton;

	private Button editButton;

	private Button deleteButton;

	private Button cancelButton;


	@PostConstruct
	private void init() {
		setSizeFull();
		setSpacing(true);
		
		accountContainer = new BeanItemContainer<>(Account.class);
		
		accountComboBox = new ComboBox("Accounts", accountContainer);
		accountComboBox.setItemCaptionPropertyId("name");
		accountComboBox.setItemCaptionMode(ItemCaptionMode.PROPERTY);
		
		accountComboBox.setInputPrompt("Select account");
		accountComboBox.setNullSelectionAllowed(false);
		accountComboBox.setFilteringMode(FilteringMode.CONTAINS);
		accountComboBox.setImmediate(true);

		accountComboBox.addValueChangeListener(event -> loadTransactions((Account) event.getProperty().getValue()));
		
		addComponent(accountComboBox);
		
		
		transactionContainer = new BeanItemContainer<>(TransactionWrapper.class);	
		transactionContainer.addNestedContainerProperty("transaction.timestamp");
		
		transactionTable = new Table("Transactions", transactionContainer);
		transactionTable.setSizeFull();
		transactionTable.setSelectable(true);
		transactionTable.addStyleName("colored-table");

		transactionTable.addGeneratedColumn(COLUMN_DATE, (source, itemId, columnId) -> {
			return DateFormat.getDateInstance().format(((TransactionWrapper)itemId).getTransaction().getTimestamp());
		});
		transactionTable.addGeneratedColumn(COLUMN_DETAIL, (source, itemId, columnId) -> {
			Transaction transaction = ((TransactionWrapper)itemId).getTransaction();
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
				
				boolean hasPartner = split.getSplitPartner()!=null; 
				if (hasPartner) {
					sb.append(split.getSplitPartner().getName());
				}
				boolean hasCategory = split.getCategory()!=null;
				boolean hasNote = split.getNote()!=null;
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
		});
		transactionTable.addGeneratedColumn(COLUMN_DEPOSIT, (source, itemId, columnId) -> {
			BigDecimal amount = ((TransactionWrapper)itemId).getTransaction().getAmount();
			return amount.signum()!=-1 ? amount.setScale(2, RoundingMode.HALF_UP) : null;
		});
		transactionTable.addGeneratedColumn(COLUMN_WITHDRAWAL, (source, itemId, columnId) -> {
			BigDecimal amount = ((TransactionWrapper)itemId).getTransaction().getAmount();
			return amount.signum()==-1 ? amount.negate().setScale(2, RoundingMode.HALF_UP) : null;
		});
		
		
		transactionTable.setColumnCollapsingAllowed(false);
		transactionTable.setSortEnabled(false);
		transactionTable.setVisibleColumns(COLUMN_DATE, COLUMN_DETAIL, COLUMN_DEPOSIT, COLUMN_WITHDRAWAL, COLUMN_AMOUNT_RUNNING);
		transactionTable.setColumnHeaders("Date", "Detail", "Deposit", "Withdrawal", "Balance");
		transactionTable.setColumnExpandRatio(COLUMN_DETAIL, 1);
		transactionTable.addValueChangeListener(event -> loadTransaction(event.getProperty().getValue()==null ? null : ((TransactionWrapper) event.getProperty().getValue()).getTransaction()));
		
		transactionTable.setCellStyleGenerator(new CellStyleGenerator() {
			@Override
			public String getStyle(Table source, Object itemId, Object propertyId) {
				if (((TransactionWrapper)itemId).getTransaction().getSplits().stream().anyMatch(split -> split.getCategory()==null)) {
					return "lightyellow";
				}
				return null;
			}
		});
		
		
		addComponent(transactionTable);
		
		HorizontalLayout transactionActionLayout = new HorizontalLayout();
		transactionActionLayout.setSpacing(true);
		
		createButton = new Button("Create");
		createButton.addClickListener(event -> createButtonClick());
		transactionActionLayout.addComponent(createButton);

		editButton = new Button("Edit");
		editButton.addClickListener(event -> editButtonClick());
		transactionActionLayout.addComponent(editButton);
		
		saveButton = new Button("Save");
		saveButton.addClickListener(event -> saveButtonClick());
		transactionActionLayout.addComponent(saveButton);

		cancelButton = new Button("Cancel");
		cancelButton.addClickListener(event -> cancelButtonClick());
		transactionActionLayout.addComponent(cancelButton);

		deleteButton = new Button("Delete");
		deleteButton.addClickListener(event -> deleteButtonClick());
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
		editableTransactionSelected = editableTransactionSelected && !((TransactionWrapper)transactionTable.getValue()).getTransaction().isChild();
		
		createButton.setEnabled(accountComboBox.getValue() != null);
		editButton.setEnabled(editableTransactionSelected);
		saveButton.setEnabled(false);
		cancelButton.setEnabled(false);
		deleteButton.setEnabled(editableTransactionSelected);
	}


	private void createButtonClick() {
		Transaction newTransaction = new Transaction();
		newTransaction.setAccount((Account) accountComboBox.getValue());
		newTransaction.setTimestamp(new Date());
		newTransaction.getSplits().add(new TransactionSplit());
		loadTransaction(newTransaction);
		transactionField.setEnabled(true);
		
		createButton.setEnabled(false);
		editButton.setEnabled(false);
		saveButton.setEnabled(true);
		cancelButton.setEnabled(true);
		deleteButton.setEnabled(false);
	}


	private void deleteButtonClick() {
		transactionService.delete(((TransactionWrapper)transactionTable.getValue()).getTransaction().getId());
		refreshTransactions();
		
		createButton.setEnabled(true);
		editButton.setEnabled(false);
		saveButton.setEnabled(false);
		cancelButton.setEnabled(false);
		deleteButton.setEnabled(false);		
	}


	private void editButtonClick() {
		transactionField.setEnabled(true);

		createButton.setEnabled(true);
		editButton.setEnabled(false);
		saveButton.setEnabled(true);
		cancelButton.setEnabled(true);
		deleteButton.setEnabled(true);
	}
	
	private void saveButtonClick() {
		transactionField.commit();
		Transaction value = transactionField.getValue();
		if (value.getId() != null) {
			transactionService.update(value);			
		} else {
			transactionService.create(value);
		}
		selectTransaction(value);
		refreshTransactions();

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
		//keep current selection if any
		Account selectedAccount = (Account) accountComboBox.getValue();
		
		accountContainer.removeAllItems();
		accountContainer.addAll(accountService.list(userIdentity.getUserAccount()));

		if (selectedAccount != null) {
			for (Account account : accountContainer.getItemIds()) {
				if (account.getId().equals(selectedAccount.getId())) {
					accountComboBox.select(account);
					break;
				}
			}
		}
		refreshTransactions();
	}
	
	private void refreshTransactions() {
		//keep current selection if any
		TransactionWrapper selectedItemId = (TransactionWrapper) transactionTable.getValue();

		Account account = (Account) accountComboBox.getValue();
		if (account==null) {
			return;
		}
		loadTransactions(account);

		if (selectedItemId != null) {
			selectTransaction(selectedItemId.getTransaction());
			transactionTable.setCurrentPageFirstItemId(transactionTable.getValue());
		}
	}
	
	private void selectTransaction(Transaction transaction) {
		Transaction foundItem = null;
		for (TransactionWrapper item : transactionContainer.getItemIds()) {
			if (item.getTransaction().getId().equals(transaction.getId())) {
				transactionTable.select(item);
				foundItem = item.getTransaction();
				break;
			}
		}
		loadTransaction(foundItem);
	}
	
	private void loadTransactions(Account account) {
		transactionContainer.removeAllItems();
		if (account==null) {
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
		loadTransaction(null);
	}


	private void loadTransaction(Transaction transaction) {
		//TODO prevent change while edit
		cancelButtonClick();
		
		if (transaction == null) {
			transactionField.setValue(new Transaction());
		} else {
			transactionField.setValue(transaction);			
		}
		saveButton.setEnabled(false);
	}


	@Override
	public void enter(ViewChangeEvent event) {
		//nothing to do
		refreshAccounts();
		cancelButtonClick();
	}
	
}
