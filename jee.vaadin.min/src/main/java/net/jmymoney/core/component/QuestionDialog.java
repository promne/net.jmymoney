package net.jmymoney.core.component;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class QuestionDialog extends AbstractDialog {

	private DialogResultListener dialogResultListener;

	public QuestionDialog(String caption, String text) {
		super(caption);
		
		VerticalLayout layout = new VerticalLayout();
		layout.setSizeUndefined();
		layout.setMargin(true);
		layout.setSpacing(true);
		
		
		layout.addComponent(new Label(text, ContentMode.HTML));

		GridLayout buttonsLayout = new GridLayout(2, 1);
		buttonsLayout.setSizeFull();
		buttonsLayout.addComponent(new Button("Yes", (ClickListener) event -> {
			QuestionDialog.this.close();
			if (dialogResultListener!=null) {
				dialogResultListener.dialogClosed(DialogResultType.OK, null);
			}
		}));
		buttonsLayout.addComponent(new Button("No", (ClickListener) event -> {
			QuestionDialog.this.close();
			if (dialogResultListener!=null) {
				dialogResultListener.dialogClosed(DialogResultType.CANCEL, null);
			}
		}));
		
		layout.addComponent(buttonsLayout);
		setContent(layout);
	}
	
	public void setDialogResultListener(DialogResultListener dialogResultListener) {
		this.dialogResultListener = dialogResultListener;
	}

}
