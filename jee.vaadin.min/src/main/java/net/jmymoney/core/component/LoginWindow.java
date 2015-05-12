package net.jmymoney.core.component;

import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.event.ShortcutAction;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.Runo;

public class LoginWindow extends Window implements ClickListener {
	
	public enum LoginResultType {
		LOGIN,
		REGISTER,
		FAIL;
	}
	
	public class LoginResult {
		String username = "";
		String password = "";
		LoginResultType type;
		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
		public LoginResultType getType() {
			return type;
		}
		public void setType(LoginResultType type) {
			this.type = type;
		}
	}

	private Button buttonLogin = new Button();
	private Button buttonRegister = new Button();
	
	private LoginResult loginResult = new LoginResult();
	
	private final BeanFieldGroup<LoginResult> loginResultBeanFieldGroup = new BeanFieldGroup<>(LoginResult.class);
	private TextField usernameField;
	
	public LoginWindow() {
		super("Login");
		
		setModal(true);
		setResizable(false);
		setClosable(false);
		center();

		addShortcutListener(new ShortcutListener("login",ShortcutAction.KeyCode.ENTER, null) {
			@Override
			public void handleAction(Object sender, Object target) {
				setResultAndClose(LoginResultType.LOGIN);
			}
		});
		
		buttonLogin.setCaption("Login");
		buttonLogin.focus();
//		buttonLogin.setIcon(new ThemeResource(ThemeResourceConstants.ICON_KEY_SMALL));		
		buttonLogin.addClickListener(this);
		buttonLogin.setClickShortcut(KeyCode.ENTER);
		buttonLogin.addStyleName(Runo.BUTTON_DEFAULT);
		
		
		buttonRegister.setCaption("Register");
//		buttonRegister.setIcon(new ThemeResource(ThemeResourceConstants.ICON_USER_ADD_SMALL));		
		buttonRegister.addClickListener(this);		
		
		HorizontalLayout buttonsLayout = new HorizontalLayout();
		buttonsLayout.setWidth(100, Unit.PERCENTAGE);
		buttonsLayout.setMargin(true);		
		buttonsLayout.addComponent(buttonLogin);
		buttonsLayout.setComponentAlignment(buttonLogin, Alignment.MIDDLE_CENTER);
		buttonsLayout.addComponent(buttonRegister);
		buttonsLayout.setComponentAlignment(buttonRegister, Alignment.MIDDLE_CENTER);

		
		FormLayout loginForm = new FormLayout();
		usernameField = loginResultBeanFieldGroup.buildAndBind("Username", "username", TextField.class);
		loginForm.addComponent(usernameField);
		PasswordField passwordField = loginResultBeanFieldGroup.buildAndBind("Password", "password", PasswordField.class);
		loginForm.addComponent(passwordField);
		loginResultBeanFieldGroup.setItemDataSource(loginResult);
		
		
		HorizontalLayout upperLogin = new HorizontalLayout();
//		upperLogin.addComponent(new Image(null, new ThemeResource(ThemeResourceConstants.ICON_LOCK_MEDIUM)));
		upperLogin.addComponent(loginForm);		
		
		VerticalLayout windowLayout = new VerticalLayout();
		windowLayout.addComponent(upperLogin);
		windowLayout.addComponent(buttonsLayout);
		windowLayout.setMargin(true);
		
		setContent(windowLayout);				
		
	}
	
	@Override
	public void focus() {
		super.focus();
		usernameField.focus();
	}

	@Override
	public void buttonClick(ClickEvent event) {
		setResultAndClose(event.getButton().equals(buttonRegister) ? LoginResultType.REGISTER : LoginResultType.LOGIN);
	}

	protected void setResultAndClose(LoginResultType loginResultType) {
		try {
			loginResultBeanFieldGroup.commit();
			loginResult = loginResultBeanFieldGroup.getItemDataSource().getBean();			
			loginResult.setType(loginResultType);			
		} catch (CommitException e) {
			loginResult.setType(LoginResultType.FAIL);
		}
		close();		
	}


	public LoginResult getLoginResult() {
		return loginResult;
	}

	public void setLoginResult(LoginResult loginResult) {
		this.loginResult = loginResult;
		loginResultBeanFieldGroup.setItemDataSource(loginResult);
	}

	
}
