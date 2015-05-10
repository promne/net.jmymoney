package jmm.component;

import com.vaadin.ui.Component;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

public abstract class AbstractDialog extends Window {

	public AbstractDialog() {
		this("", null);
	}

	public AbstractDialog(String caption, Component content) {
		super(caption, content);
	}

	public AbstractDialog(String caption) {
		super(caption, null);
		center();
		setClosable(false);
		setModal(true);
		setResizable(false);
		setSizeUndefined();
	}

	public void show() {	
		UI.getCurrent().addWindow(this);
		this.focus();
	}
	
}
