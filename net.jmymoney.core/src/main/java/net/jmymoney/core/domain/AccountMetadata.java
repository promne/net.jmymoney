package net.jmymoney.core.domain;

import java.math.BigDecimal;

import net.jmymoney.core.entity.Account;

public class AccountMetadata {

	private Account account;
	
	private BigDecimal balance;

	public AccountMetadata(Account account) {
		super();
		this.account = account;
	}

	public AccountMetadata(Account account, BigDecimal balance) {
		super();
		this.account = account;
		this.balance = balance != null ? balance : BigDecimal.ZERO;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}
	
}
