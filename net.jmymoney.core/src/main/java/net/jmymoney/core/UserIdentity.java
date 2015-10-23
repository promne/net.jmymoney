package net.jmymoney.core;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

import net.jmymoney.core.entity.Profile;
import net.jmymoney.core.entity.UserAccount;
import net.jmymoney.core.service.UserAccountService;

@SessionScoped
public class UserIdentity implements Serializable {

    @Inject
    private UserAccountService userAccountService;
    
    private UserAccount userAccount;

    private Profile profile;

    public UserIdentity() {
        super();
    }

    public UserAccount getUserAccount() {
        return userAccount;
    }

    public void setUserAccount(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public void refreshUserAccount() {
        if (userAccount != null) {
            userAccount = userAccountService.find(userAccount.getId());
        }
    }
    
}
