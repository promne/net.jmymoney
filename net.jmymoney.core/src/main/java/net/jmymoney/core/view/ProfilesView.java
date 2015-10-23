package net.jmymoney.core.view;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.GeneratedPropertyContainer;
import com.vaadin.data.util.PropertyValueGenerator;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.themes.Runo;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import net.jmymoney.core.OneUI;
import net.jmymoney.core.UserIdentity;
import net.jmymoney.core.component.AbstractDialog;
import net.jmymoney.core.entity.InvitationProfile;
import net.jmymoney.core.entity.Profile;
import net.jmymoney.core.entity.UserAccount;
import net.jmymoney.core.service.InvitationService;
import net.jmymoney.core.service.ProfileService;
import net.jmymoney.core.util.PropertyResolver;

@CDIView(value = ProfilesView.NAME)
public class ProfilesView extends VerticalLayout implements View {

    public static final String NAME = "ProfilesView";

    private static final String PROPERTY_INVITATION = "invitationExpiration";
    private static final String PROPERTY_INVITE = "invite";
    private static final String PROPERTY_SHARED = "sharedWith";
    
    private BeanItemContainer<Profile> profileContainer = new BeanItemContainer<>(Profile.class);
    private BeanItemContainer<InvitationProfile> invitationContainer = new BeanItemContainer<>(InvitationProfile.class);
    private Grid profilesGrid;
    
    @Inject
    private UserIdentity userIdentity;
    
    @Inject
    private ProfileService profileService;

    @Inject
    private InvitationService invitationService;
    
    @PostConstruct
    private void init() {
        setSizeFull();
        setSpacing(true);
        setMargin(true);

        profileContainer.addNestedContainerBean(Profile.PROPERTY_USER_ACCOUNT);
        GeneratedPropertyContainer wrapperContainer= new GeneratedPropertyContainer(profileContainer);
        wrapperContainer.addGeneratedProperty(PROPERTY_INVITE, new PropertyValueGenerator<String>() {
            @Override
            public String getValue(Item item, Object itemId, Object propertyId) {
                return "Invite";
            }

            @Override
            public Class<String> getType() {
                return String.class;
            }
        });
        wrapperContainer.addGeneratedProperty(PROPERTY_INVITATION, new PropertyValueGenerator<String>() {
            @Override
            public String getValue(Item item, Object itemId, Object propertyId) {
                Profile profile = (Profile) itemId;
                return invitationContainer.getItemIds().stream().filter(p -> p.getProfile().equals(profile)).map(p -> "expires " + p.getExpiration()).findFirst().orElse("No invitation");
            }

            @Override
            public Class<String> getType() {
                return String.class;
            }
        });
        wrapperContainer.addGeneratedProperty(PROPERTY_SHARED, new PropertyValueGenerator<String>() {
            @Override
            public String getValue(Item item, Object itemId, Object propertyId) {
                Profile profile = (Profile) itemId;
                List<UserAccount> userAccounts = profileService.listShares(profile);
                return userAccounts.stream().filter(ua -> !userIdentity.getUserAccount().equals(ua)).map(UserAccount::getUsername).collect(Collectors.joining(", "));
            }
            
            @Override
            public Class<String> getType() {
                return String.class;
            }
        });
        
        
        profilesGrid = new Grid(wrapperContainer);
        profilesGrid.removeAllColumns();
        profilesGrid.addColumn(Profile.PROPERTY_NAME).setExpandRatio(1);
        profilesGrid.addColumn(PropertyResolver.chainPropertyName(Profile.PROPERTY_USER_ACCOUNT, UserAccount.PROPERTY_USERNAME)).setHeaderCaption("Owner").setExpandRatio(1);
        profilesGrid.addColumn(PROPERTY_SHARED).setExpandRatio(1);
        profilesGrid.addColumn(PROPERTY_INVITE)
            .setHeaderCaption("Invite")
            .setRenderer((new ButtonRenderer(this::invite)))
            .setEditable(false);
        profilesGrid.addColumn(PROPERTY_INVITATION);
        profilesGrid.setSizeFull();
        
        addComponent(profilesGrid);
    }

    private void invite(RendererClickEvent rendererClickEvent) {
        Profile profile = (Profile) rendererClickEvent.getItemId();
        
        if (!profile.getOwnerUserAccount().equals(userIdentity.getUserAccount())) {
            Notification.show("Unable to create an invitation for a profile you don't own.", Type.ERROR_MESSAGE);
            return;
        }
        
        Optional<InvitationProfile> existingInvitation = invitationContainer.getItemIds().stream().filter(p -> p.getProfile().equals(profile)).findAny();
        
        InvitationProfile invitation;
        if (existingInvitation.isPresent()) {
            invitation = existingInvitation.get();
        } else {
            invitation = new InvitationProfile();
            invitation.setExpiration(new Date(new Date().getTime() + 48*3600*1000));
            invitation.setProfile(profile);
            invitationService.create(invitation);
            refreshProfiles();
        }
        
        // show dialog
        Window inviteDialog = new AbstractDialog("Profile invitation link");
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSpacing(true);
        
        layout.addComponent(new Label("Copy following URL and send it to the person you want to share the profile with", ContentMode.HTML));
        TextField valueTextField = new TextField(null, getInvitationUrl(invitation));
        valueTextField.setWidth(100, Unit.PERCENTAGE);
        layout.addComponent(valueTextField);
        
        Button closeButton = new Button("Close", event -> inviteDialog.close());
        closeButton.setClickShortcut(KeyCode.ENTER);
        closeButton.addStyleName(Runo.BUTTON_DEFAULT);
        
        layout.addComponent(closeButton);
        layout.setComponentAlignment(closeButton, Alignment.MIDDLE_CENTER);
        inviteDialog.setContent(layout);
        UI.getCurrent().addWindow(inviteDialog);
        valueTextField.focus();
        valueTextField.selectAll();
    }
    
    private void refreshProfiles() {
        profileContainer.removeAllItems();
        profileContainer.addAll(profileService.list(userIdentity.getUserAccount()));
        invitationContainer.removeAllItems();
        invitationContainer.addAll(invitationService.listInvitations(userIdentity.getUserAccount()));
        profilesGrid.recalculateColumnWidths();        
    }
    
    private String getInvitationUrl(InvitationProfile invitationProfile) {
        return UI.getCurrent().getPage().getLocation().toString() + "/" + invitationProfile.getId();
    }
    
    @Override
    public void enter(ViewChangeEvent event) {
        if (event.getParameters() != null && !event.getParameters().isEmpty()) {
            try {
                String string = event.getParameters().toString();
                string = string.startsWith("/") ? string.substring(1) : string;
                Optional<InvitationProfile> find = invitationService.find(string);
                if (find.isPresent()) {
                    if (profileService.add(userIdentity.getUserAccount(), find.get().getProfile())) {
                        OneUI.getCurrent().refreshData();
                        Notification.show("Linking profile " + find.get().getProfile().getName() + " was successfull");
                    }
                }
                UI.getCurrent().getNavigator().navigateTo(NAME);
            } catch (NumberFormatException e) {
                //nothing, go on
            }
        }
        refreshProfiles();
    }
    
}
