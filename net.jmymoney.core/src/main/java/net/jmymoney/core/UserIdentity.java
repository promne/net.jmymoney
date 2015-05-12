package net.jmymoney.core;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;

import net.jmymoney.core.entity.UserAccount;

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
