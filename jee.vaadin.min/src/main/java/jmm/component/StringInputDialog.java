package jmm.component;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Runo;

public class StringInputDialog extends AbstractDialog {

	private TextField valueTextField;
	
	private DialogResultListener dialogResultListener;

	public StringInputDialog(String caption, String text) {
		super(caption);
		
		VerticalLayout layout = new VerticalLayout();
		layout.setSizeUndefined();
		layout.setMargin(true);
		layout.setSpacing(true);
		
		
		layout.addComponent(new Label(text, ContentMode.HTML));
		valueTextField = new TextField();
		valueTextField.setSizeFull();
		valueTextField.setRequired(true);
		layout.addComponent(valueTextField);

		GridLayout buttonsLayout = new GridLayout(2, 1);
		buttonsLayout.setSizeFull();
		Button submitButton = new Button("Submit", (ClickListener) event -> {
			try {
				valueTextField.validate();
				StringInputDialog.this.close();
				if (dialogResultListener!=null) {
					dialogResultListener.dialogClosed(DialogResultType.OK ,valueTextField.getValue());
				}
			} catch (InvalidValueException e) {
				//well .. nothing happens this time
			}
		});
		submitButton.setClickShortcut(KeyCode.ENTER);
		submitButton.addStyleName(Runo.BUTTON_DEFAULT);
		
		
		buttonsLayout.addComponent(submitButton);
		buttonsLayout.addComponent(new Button("Cancel", (ClickListener) event -> {
			StringInputDialog.this.close();
			if (dialogResultListener!=null) {
				dialogResultListener.dialogClosed(DialogResultType.CANCEL ,null);
			}
		}));
		
		layout.addComponent(buttonsLayout);
		setContent(layout);
	}
	
	public void setValue(String value) {
		valueTextField.setValue(value);
	}
	
	public void setDialogResultListener(DialogResultListener dialogResultListener) {
		this.dialogResultListener = dialogResultListener;
	}

	@Override
	public void focus() {
		super.focus();
		valueTextField.focus();
	}
	
}
