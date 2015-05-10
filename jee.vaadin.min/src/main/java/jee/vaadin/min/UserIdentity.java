package jee.vaadin.min;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;

import jmm.entity.UserAccount;

@SessionScoped
public class UserIdentity implements Serializable {

	private UserAccount userAccount;

	public UserIdentity() {
		super();
	}

	public UserAccount getUserAccount() {
		return userAccount;
	}

	public void setUserAccount(UserAccount userAccount) {
		this.userAccount = userAccount;
	}
	
}
