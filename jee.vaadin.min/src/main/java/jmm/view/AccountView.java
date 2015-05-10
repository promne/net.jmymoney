package jmm.view;

import java.math.BigDecimal;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import jee.vaadin.min.UserIdentity;
import jmm.component.DialogResultType;
import jmm.component.StringInputDialog;
import jmm.domain.AccountMetadata;
import jmm.entity.Account;
import jmm.service.AccountService;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.Item;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

@CDIView(value=AccountView.NAME)
public class AccountView extends VerticalLayout implements View {

	public static final String NAME = "AccountView";

    private static final String COLUMN_NAME = "Name";

	private static final String COLUMN_VALUE = "column_value";
	private static final String COLUMN_BALANCE = "Balance";
		
    @Inject
	private UserIdentity userIdentity;
    
    @Inject
    private AccountService accountService;

    private Table accountTable;
    
	private Button deleteButton;

	private Button editButton;
    
	@PostConstruct
	private void init() {
		setSizeFull();
		setSpacing(true);

		accountTable = new Table();
		accountTable.setSizeFull();
		accountTable.setSelectable(true);
		accountTable.addContainerProperty(COLUMN_NAME, String.class, null);
		accountTable.addContainerProperty(COLUMN_BALANCE, BigDecimal.class, null);
		accountTable.addContainerProperty(COLUMN_VALUE, AccountMetadata.class, null);
		accountTable.setVisibleColumns(COLUMN_NAME, COLUMN_BALANCE);
		accountTable.setSortContainerPropertyId(COLUMN_NAME);
		accountTable.addValueChangeListener(event -> accountChangedAction());

		addComponent(accountTable);

		HorizontalLayout controlButtonsLayout = new HorizontalLayout();
		controlButtonsLayout.addComponent(new Button("New", (ClickListener) event -> newAccountAction()));
		editButton = new Button("Edit", (ClickListener) event -> editAccountAction());
		controlButtonsLayout.addComponent(editButton);
		
		deleteButton = new Button("Delete", (ClickListener) event -> deleteAccountAction());
		controlButtonsLayout.addComponent(deleteButton);
		
		addComponent(controlButtonsLayout );
		
		setExpandRatio(accountTable, 1.0f);	
	}    
	
	private void newAccountAction() {
		StringInputDialog newCategoryDialog = new StringInputDialog("New Account", "Enter name of the new account");
		newCategoryDialog.setDialogResultListener((closeType, resultValue) -> {
			if (DialogResultType.OK.equals(closeType)) {
				Account account = new Account();
				account.setUserAccount(userIdentity.getUserAccount());
				account.setName((String) resultValue);

				accountService.create(account);
				refreshAccounts();
			}
		});
		newCategoryDialog.show();
	}

	private void deleteAccountAction() {
		AccountMetadata curAccount = (AccountMetadata) accountTable.getItem(accountTable.getValue()).getItemProperty(COLUMN_VALUE).getValue();
		boolean deleted = accountService.delete(curAccount.getAccount().getId());
		
		if (deleted) {
			Notification.show(String.format("Account \"%s\" was deleted", curAccount.getAccount().getName()));
			refreshAccounts();
		} else {
			Notification.show(String.format("Unable to delete Account \"%s\" because it is used", curAccount.getAccount().getName()), Type.WARNING_MESSAGE);
		}
	}

	private void editAccountAction() {
		Object itemId = accountTable.getValue();
		if (itemId != null) {
			Account curAccount = ((AccountMetadata) accountTable.getItem(itemId).getItemProperty(COLUMN_VALUE).getValue()).getAccount();
			StringInputDialog accountNameDialog = new StringInputDialog("Edit account", "Enter name of the account");
			accountNameDialog.setValue(curAccount.getName());
			accountNameDialog.setDialogResultListener((closeType, resultValue) -> {
				if (DialogResultType.OK.equals(closeType)) {
					curAccount.setName((String) resultValue);
					accountService.update(curAccount);
					refreshAccounts();					
				}
			});
			
			accountNameDialog.show();
		}
	}

	@Override
	public void enter(ViewChangeEvent event) {
		refreshAccounts();
	}

	private void accountChangedAction() {
		Object selectedId = accountTable.getValue();
		deleteButton.setEnabled(selectedId!=null);
		editButton.setEnabled(selectedId!=null);
	}
	
	private void refreshAccounts() {
		accountTable.removeAllItems();
		List<AccountMetadata> accountMetadatas = accountService.listAccountMetadatas(userIdentity.getUserAccount());
		
		accountMetadatas.forEach(metadata -> {
			Item newItem = accountTable.addItem(metadata.getAccount().getId());
			newItem.getItemProperty(COLUMN_NAME).setValue(metadata.getAccount().getName());
			newItem.getItemProperty(COLUMN_BALANCE).setValue(metadata.getBalance());
			newItem.getItemProperty(COLUMN_VALUE).setValue(metadata);
		});

		accountChangedAction();
	}
	
}
