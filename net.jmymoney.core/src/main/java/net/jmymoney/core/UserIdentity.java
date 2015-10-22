package net.jmymoney.core;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;

import net.jmymoney.core.entity.Profile;
import net.jmymoney.core.entity.UserAccount;

@SessionScoped
public class UserIdentity implements Serializable {

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

}
