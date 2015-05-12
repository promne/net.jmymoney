package net.jmymoney.core;

import com.vaadin.annotations.Theme;
import com.vaadin.cdi.CDIUI;
import com.vaadin.cdi.CDIViewProvider;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.Navigator.EmptyView;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;

import javax.inject.Inject;

import net.jmymoney.core.component.LoginWindow;
import net.jmymoney.core.component.LoginWindow.LoginResultType;
import net.jmymoney.core.component.NavigationButton;
import net.jmymoney.core.entity.UserAccount;
import net.jmymoney.core.service.UserAccountService;
import net.jmymoney.core.util.ManifestUtils;
import net.jmymoney.core.view.AccountView;
import net.jmymoney.core.view.CategoryView;
import net.jmymoney.core.view.PartnerView;
import net.jmymoney.core.view.ReportView;
import net.jmymoney.core.view.TransactionView;

@CDIUI
@Theme(value = "jmm")
// @PreserveOnRefresh
public class OneUI extends UI {

    @Inject
    private CDIViewProvider navigatorViewProvider;

    @Inject
    private UserIdentity userIdentity;

    @Inject
    private UserAccountService userAccountService;

    private Navigator navigator;

    @Override
    protected void init(VaadinRequest request) {
        // check javadoc what and when this method should do
        if (userIdentity.getUserAccount() == null) {
            doLogin();
        } else {
            loadPage();
        }
    }

    private void doLogin() {
        CssLayout emptyLoginLayout = new CssLayout();
        emptyLoginLayout.setSizeFull();
        setContent(emptyLoginLayout);
        final LoginWindow loginWindow = new LoginWindow();
        loginWindow.addCloseListener(closeEvent -> {
            UserAccount userAccount = null;
            if (loginWindow.getLoginResult().getType() == LoginResultType.LOGIN) {
                userAccount = userAccountService.login(loginWindow.getLoginResult().getUsername(),
                        loginWindow.getLoginResult().getPassword());
                if (userAccount == null) {
                    Notification.show("Username or password are invalid.", Type.ERROR_MESSAGE);
                }
            } else if (loginWindow.getLoginResult().getType() == LoginResultType.REGISTER) {
                // TODO
            } else {
                Notification.show("Error during login. Please try again later.", Type.ERROR_MESSAGE);
            }

            if (userAccount == null) {
                UI.getCurrent().addWindow(loginWindow);
                loginWindow.focus();
            } else {
                userIdentity.setUserAccount(userAccount);
                loadPage();
                if (Page.getCurrent().getUriFragment() != null) {
                    navigator.navigateTo(Page.getCurrent().getUriFragment());
                }
            }
        } );
        UI.getCurrent().addWindow(loginWindow);
        loginWindow.focus();
    }

    private void loadPage() {
        HorizontalSplitPanel pageLayout = new HorizontalSplitPanel();
        pageLayout.setSplitPosition(200, Unit.PIXELS);
        pageLayout.setSizeFull();
        
        
        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setMargin(true);
        pageLayout.setSecondComponent(contentLayout);
        
        
        VerticalLayout navigationLayout = new VerticalLayout();
        navigationLayout.setMargin(true);
        navigationLayout.setSpacing(true);
        pageLayout.setFirstComponent(navigationLayout);
        
        
        navigator = new Navigator(this, contentLayout);
        navigator.addView("", new EmptyView()); // DEFAULT
        
        navigator.addProvider(navigatorViewProvider);
        //the views are registered by annotation
        
        //now add trigger to switch to the view
        navigationLayout.addComponent(new NavigationButton("Accounts", AccountView.NAME));
        navigationLayout.addComponent(new NavigationButton("Transactions", TransactionView.NAME));
        navigationLayout.addComponent(new NavigationButton("Categories", CategoryView.NAME));
        navigationLayout.addComponent(new NavigationButton("Partners", PartnerView.NAME));
        navigationLayout.addComponent(new NavigationButton("Reports", ReportView.NAME));        
        
        for (Component navigatorComponent : navigationLayout) {
        	navigatorComponent.setWidth(100, Unit.PERCENTAGE);
        }

        navigator.addViewChangeListener(new ViewChangeListener() {
            
            @Override
            public boolean beforeViewChange(ViewChangeEvent event) {
                return true;
            }
            
            @Override
            public void afterViewChange(ViewChangeEvent event) {
                for (Component navigatorComponent : navigationLayout) {
                    if (NavigationButton.class.isInstance(navigatorComponent)) {
                        NavigationButton naviButton = (NavigationButton) navigatorComponent;
                        String toggleStyle = "v-navigation-selected";
                        if (event.getViewName().equals(naviButton.getNavigationState())) {
                            naviButton.addStyleName(toggleStyle);
                        } else {
                            naviButton.removeStyleName(toggleStyle);
                        }
                    }
                }
            }
        });        
        
        VerticalLayout wholePage = new VerticalLayout();
        wholePage.setSizeFull();
        wholePage.addComponent(pageLayout);
        wholePage.setExpandRatio(pageLayout, 1.0f);
        
        
        HorizontalLayout footerLayout = new HorizontalLayout();
        footerLayout.setMargin(true);
        footerLayout.setSpacing(true);
        
        Label labelLoggedAs = new Label(String.format("You are logged as \"%s\"", userIdentity.getUserAccount().getUsername()));
        footerLayout.addComponent(labelLoggedAs);
        footerLayout.setComponentAlignment(labelLoggedAs, Alignment.MIDDLE_CENTER);
        
        Button logoutButton = new Button("logout");
        logoutButton.addStyleName(BaseTheme.BUTTON_LINK);
        logoutButton.addClickListener(listener -> {
        	for (UI ui: VaadinSession.getCurrent().getUIs()) {
				ui.access(() -> {
                    ui.getPage().setLocation(UITools.getStartLocation());
                });
			}
        	close();
            getSession().close();
            getSession().getSession().invalidate();
        });
        footerLayout.addComponent(logoutButton);
        
        
        String version = ManifestUtils.getVersion();
        if (version != null) {
            labelLoggedAs.setDescription(String.format("Version %s", version));
        }
              
        wholePage.addComponent(footerLayout);
        
        setContent(wholePage);    	
    }

}