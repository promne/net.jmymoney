package jmm.component;

import com.vaadin.ui.Button;
import com.vaadin.ui.UI;

public class NavigationButton extends Button {

    private final String navigationState;

    public NavigationButton(String caption, String navigationState) {
        super(caption);
        this.navigationState = navigationState;
        this.addClickListener(this::switchView);
    }

    private void switchView(ClickEvent clickEvent) {
        UI.getCurrent().getNavigator().navigateTo(navigationState);        
    }

    public String getNavigationState() {
        return navigationState;
    }    
    
}
