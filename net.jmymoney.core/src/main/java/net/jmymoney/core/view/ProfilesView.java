package net.jmymoney.core.view;

import com.vaadin.cdi.CDIView;
import com.vaadin.data.Item;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.GeneratedPropertyContainer;
import com.vaadin.data.util.PropertyValueGenerator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ButtonRenderer;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import net.jmymoney.core.OneUI;
import net.jmymoney.core.UserIdentity;
import net.jmymoney.core.entity.InvitationProfile;
import net.jmymoney.core.entity.Profile;
import net.jmymoney.core.entity.UserAccount;
import net.jmymoney.core.service.InvitationService;
import net.jmymoney.core.service.ProfileService;
import net.jmymoney.core.util.PropertyResolver;

@CDIView(value = ProfilesView.NAME)
public class ProfilesView extends VerticalLayout implements View {

    public static final String NAME = "ProfilesView";

    private static final String PROPERTY_INVITATION = "invitationLink";
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
                return invitationContainer.getItemIds().stream().filter(p -> p.getProfile().equals(profile)).map(p -> getInvitationUrl(p) + " expires " + p.getExpiration()).collect(Collectors.joining(" , "));
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
        
        if (invitationContainer.getItemIds().stream().anyMatch(p -> p.getProfile().equals(profile))) {
            Notification.show("Invitation link created", Type.WARNING_MESSAGE);
            return;
        }
        
        InvitationProfile invitation = new InvitationProfile();
        invitation.setExpiration(new Date(new Date().getTime() + 48*3600*1000));
        invitation.setProfile(profile);
        invitationService.create(invitation);
        
        Notification.show("Invitation link created");
        
        refreshProfiles();
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
                        Notification.show("Linking profile " + find.get().getProfile().getName() + " was successfull");
                        OneUI.getCurrent().refreshData();
                    }
                }
            } catch (NumberFormatException e) {
                //nothing, go on
            }
        }
        refreshProfiles();
    }
    
}
