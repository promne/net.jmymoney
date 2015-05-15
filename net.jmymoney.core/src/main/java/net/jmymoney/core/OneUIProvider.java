package net.jmymoney.core;

import com.vaadin.cdi.CDIUIProvider;
import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.ui.UI;

public class OneUIProvider extends CDIUIProvider {

    @Override
    public Class<? extends UI> getUIClass(UIClassSelectionEvent event) {
        return super.getUIClass(event);
    }

}
