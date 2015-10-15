package net.jmymoney.core;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.cdi.CDIUI;
import com.vaadin.cdi.CDIViewProvider;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

import javax.inject.Inject;

import net.jmymoney.core.component.LoginWindow;
import net.jmymoney.core.component.LoginWindow.LoginResultType;
import net.jmymoney.core.component.NavigationButton;
import net.jmymoney.core.entity.UserAccount;
import net.jmymoney.core.i18n.I18nResourceConstant;
import net.jmymoney.core.i18n.MessagesResourceBundle;
import net.jmymoney.core.service.UserAccountService;
import net.jmymoney.core.theme.ThemeResourceConstatns;
import net.jmymoney.core.theme.ThemeStyles;
import net.jmymoney.core.util.ManifestUtils;
import net.jmymoney.core.view.AccountView;
import net.jmymoney.core.view.CategoryView;
import net.jmymoney.core.view.DashboardView;
import net.jmymoney.core.view.PartnerView;
import net.jmymoney.core.view.ReportView;
import net.jmymoney.core.view.TransactionView;
import net.jmymoney.core.view.UserAccountView;

@CDIUI
@Theme(value = "jmm")
// @PreserveOnRefresh
@Widgetset("net.jmymoney.core.MyAppWidgetset")
public class OneUI extends UI {

    @Inject
    private CDIViewProvider navigatorViewProvider;

    @Inject
    private UserIdentity userIdentity;

    @Inject
    private UserAccountService userAccountService;

    @Inject
    private MessagesResourceBundle messagesResourceBundle;
    
    private Navigator navigator;

    @Override
    protected void init(VaadinRequest request) {
        Responsive.makeResponsive(this);
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
        final LoginWindow loginWindow = new LoginWindow(messagesResourceBundle.getLocale());
        loginWindow.addCloseListener(closeEvent -> {
            UserAccount userAccount = null;
            if (loginWindow.getLoginResult().getType() == LoginResultType.LOGIN) {
                userAccount = userAccountService.login(loginWindow.getLoginResult().getUsername(),
                        loginWindow.getLoginResult().getPassword());
                if (userAccount == null) {
                    Notification.show(messagesResourceBundle.getString(I18nResourceConstant.LOGIN_ERROR_INVALID_CREDENTIALS), Type.ERROR_MESSAGE);
                }
            } else if (loginWindow.getLoginResult().getType() == LoginResultType.REGISTER) {
                Notification.show("Registration is not allowed yet (beta).", Type.HUMANIZED_MESSAGE);
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
        HorizontalLayout pageLayout = new HorizontalLayout();
        pageLayout.setSizeFull();
        
        CssLayout menuArea = new CssLayout();
        menuArea.setPrimaryStyleName(ValoTheme.MENU_ROOT);
        pageLayout.addComponent(menuArea);
        
        CssLayout menuContent = new CssLayout();
        menuContent.addStyleName(ThemeStyles.MENU_PART);
        menuArea.addComponent(menuContent);
        
        final HorizontalLayout menuTitleLayout = new HorizontalLayout();
        menuTitleLayout.setWidth("100%");
        menuTitleLayout.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
        menuTitleLayout.addStyleName(ThemeStyles.MENU_TITLE);
        final Label title = new Label("<h3>JMyMoney</h3>", ContentMode.HTML);
        title.setSizeUndefined();
        title.setDescription(ManifestUtils.getVersion());
        menuTitleLayout.addComponent(title);
        menuTitleLayout.setExpandRatio(title, 1);
        menuContent.addComponent(menuTitleLayout);
        
        final Button showMenu = new Button("Menu", (ClickListener) event -> {
            String toggleStyle = ThemeStyles.MENU_VISIBLE;
            if (menuContent.getStyleName().contains(toggleStyle)) {
                menuContent.removeStyleName(toggleStyle);
            } else {
                menuContent.addStyleName(toggleStyle);
            }
        });
        showMenu.addStyleName(ValoTheme.BUTTON_PRIMARY);
        showMenu.addStyleName(ValoTheme.BUTTON_SMALL);
        showMenu.addStyleName(ThemeStyles.MENU_TOGGLE);
        showMenu.setIcon(FontAwesome.LIST);  
        menuContent.addComponent(showMenu);
        
        CssLayout contentArea = new CssLayout();
        contentArea.setSizeFull();
        contentArea.setPrimaryStyleName(ThemeStyles.CONTENT);
        contentArea.addStyleName(ThemeStyles.SCROLLABLE);
        pageLayout.addComponent(contentArea);
        pageLayout.setExpandRatio(contentArea, 1);
        
        navigator = new Navigator(this, contentArea);
        
        navigator.addView("", navigatorViewProvider.getView(DashboardView.NAME));
        navigator.addProvider(navigatorViewProvider);
        //the views are registered by annotation
        
        CssLayout navigationItemsLayout = new CssLayout();
        navigationItemsLayout.setPrimaryStyleName(ThemeStyles.MENU_ITEMS);
        menuContent.addComponent(navigationItemsLayout);
        
        //now add trigger to switch to the view
        navigationItemsLayout.addComponent(new NavigationButton(messagesResourceBundle.getString(I18nResourceConstant.DASHBOARD), FontAwesome.HOME, DashboardView.NAME));
        navigationItemsLayout.addComponent(new NavigationButton(messagesResourceBundle.getString(I18nResourceConstant.UNIVERSAL_ACCOUNTS), FontAwesome.BANK, AccountView.NAME));
        navigationItemsLayout.addComponent(new NavigationButton(messagesResourceBundle.getString(I18nResourceConstant.NAVIGATION_TRANSACTIONS), FontAwesome.MONEY, TransactionView.NAME));
        navigationItemsLayout.addComponent(new NavigationButton(messagesResourceBundle.getString(I18nResourceConstant.UNIVERSAL_CATEGORIES), FontAwesome.FILTER, CategoryView.NAME));
        navigationItemsLayout.addComponent(new NavigationButton(messagesResourceBundle.getString(I18nResourceConstant.UNIVERSAL_PARTNERS), FontAwesome.USERS, PartnerView.NAME));
        navigationItemsLayout.addComponent(new NavigationButton(messagesResourceBundle.getString(I18nResourceConstant.UNIVERSAL_REPORTS), FontAwesome.BAR_CHART_O, ReportView.NAME));        
        
        final MenuBar settings = new MenuBar();
        settings.addStyleName(ThemeStyles.USER_MENU);
        final MenuItem settingsItem = settings.addItem(userIdentity.getUserAccount().getUsername(), ThemeResourceConstatns.PROFILE_PIC_300, null);
        settingsItem.addItem("Edit Profile", c -> {
            navigator.navigateTo(UserAccountView.NAME);
        }).setIcon(FontAwesome.EDIT);
//        settingsItem.addItem("Preferences", null);
        settingsItem.addSeparator();
        settingsItem.addItem("Sign Out", menuItem -> {
            for (UI ui : VaadinSession.getCurrent().getUIs()) {
                ui.access(() -> {
                    ui.getPage().setLocation(UITools.getStartLocation());
                } );
            }
            close();
            getSession().close();
            getSession().getSession().invalidate();
        }).setIcon(FontAwesome.BAN);
        menuContent.addComponent(settings);
        
        navigator.addViewChangeListener(new ViewChangeListener() {
            
            @Override
            public boolean beforeViewChange(ViewChangeEvent event) {
                return true;
            }
            
            @Override
            public void afterViewChange(ViewChangeEvent event) {
                String toggleStyle = ThemeStyles.SELECTED;
                for (Component navigatorComponent : navigationItemsLayout) {
                    navigatorComponent.removeStyleName(toggleStyle);
                }
                for (Component navigatorComponent : navigationItemsLayout) {
                    if (NavigationButton.class.isInstance(navigatorComponent)) {
                        NavigationButton naviButton = (NavigationButton) navigatorComponent;
                        if (event.getViewName().equals(naviButton.getNavigationState())) {
                            naviButton.addStyleName(toggleStyle);
                            break;
                        }
                    }
                }
                menuContent.removeStyleName(ThemeStyles.MENU_VISIBLE);
            }
        });
        
        setContent(pageLayout);
        addStyleName(ValoTheme.UI_WITH_MENU);
    }

}