package net.jmymoney.core.view;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.VerticalLayout;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;

import net.jmymoney.core.UserIdentity;
import net.jmymoney.core.service.UserAccountService;

@CDIView(value = UserAccountView.NAME)
public class UserAccountView extends VerticalLayout implements View {

    public static final String NAME = "UserAccountView";

    private PasswordField passwordCurrentField;
    private PasswordField passwordFirstField;
    private PasswordField passwordSecondField;

    @Inject
    private Logger log;

    @Inject
    private UserIdentity userIdentity;

    @Inject
    private UserAccountService userAccountService;

    @PostConstruct
    private void init() {
        setMargin(true);
        setSpacing(true);

        addComponent(new Label("<h2>Edit profile<h2>", ContentMode.HTML));

        
        FormLayout changePasswordForm = new FormLayout();
        addComponent(changePasswordForm);
        changePasswordForm.setMargin(false);
        changePasswordForm.setWidth(100, Unit.PERCENTAGE);
        
        changePasswordForm.addComponent(new Label("<h3>Change password<h3>", ContentMode.HTML));

        passwordCurrentField = new PasswordField("Current password");
        passwordCurrentField.setValidationVisible(false);
        passwordCurrentField.addValidator(v -> {
            if (!userAccountService.passwordMatches(passwordCurrentField.getValue(), userIdentity.getUserAccount())) {
                throw new InvalidValueException("Password is wrong");
            }
        });
        changePasswordForm.addComponent(passwordCurrentField);

        passwordFirstField = new PasswordField("New password");
        passwordFirstField.setValidationVisible(false);
        passwordFirstField.addValidator(new StringLengthValidator("Password has to be 1-60 characters long", 1, 60, false));
        changePasswordForm.addComponent(passwordFirstField);

        passwordSecondField = new PasswordField("New password repeat");
        passwordSecondField.setValidationVisible(false);
        passwordSecondField.addValidator(v -> {
            if (v!=null && !v.equals(passwordFirstField.getValue())) {
                throw new InvalidValueException("Password has to match");                
            }
        });
        changePasswordForm.addComponent(passwordSecondField);

        Button changePasswordButton = new Button("Change", this::changePassword);
        changePasswordForm.addComponent(changePasswordButton);

    }

    private void changePassword(ClickEvent clickEvent) {
        passwordCurrentField.setValidationVisible(false);
        passwordFirstField.setValidationVisible(false);
        passwordSecondField.setValidationVisible(false);
        try {
            passwordCurrentField.setValidationVisible(true);
            passwordCurrentField.validate();
            
            passwordFirstField.setValidationVisible(true);
            passwordFirstField.validate();
            
            passwordSecondField.setValidationVisible(true);
            passwordSecondField.validate();
        } catch (InvalidValueException e) {
            return;
        }
        
        userIdentity.getUserAccount().setPasswordHash(userAccountService.hashPassword(passwordFirstField.getValue()));
        userAccountService.update(userIdentity.getUserAccount());
        log.info("Password changed");

        Notification.show("Password was changed", Type.HUMANIZED_MESSAGE);
        
        passwordCurrentField.setValidationVisible(false);
        passwordFirstField.setValidationVisible(false);
        passwordSecondField.setValidationVisible(false);        
        passwordCurrentField.setValue("");
        passwordFirstField.setValue("");
        passwordSecondField.setValue("");
    }

    @Override
    public void enter(ViewChangeEvent event) {
        // TODO Auto-generated method stub

    }

}
