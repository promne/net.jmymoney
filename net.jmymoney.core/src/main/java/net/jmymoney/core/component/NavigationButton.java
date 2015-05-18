package net.jmymoney.core.component;

import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;

import net.jmymoney.core.theme.ThemeStyles;

public class NavigationButton extends Button {

    private final String navigationState;

    public NavigationButton(String caption, String navigationState) {
        super(caption);
        this.navigationState = navigationState;
        this.addClickListener(this::switchView);
        super.setPrimaryStyleName(ThemeStyles.MENU_ITEM);
    }

    public NavigationButton(String caption, Resource icon, String navigationState) {
        this(caption, navigationState);
        setIcon(icon);
    }
    
    private void switchView(ClickEvent clickEvent) {
        UI.getCurrent().getNavigator().navigateTo(navigationState);        
    }

    public String getNavigationState() {
        return navigationState;
    }    
    
}
