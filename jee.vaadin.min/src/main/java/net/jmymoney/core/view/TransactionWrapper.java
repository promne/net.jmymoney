package net.jmymoney.core.view;

import java.math.BigDecimal;

import net.jmymoney.core.entity.Transaction;

public class TransactionWrapper {
	
	private Transaction transaction;
	
	private BigDecimal amountRunning;

	public TransactionWrapper(Transaction transaction, BigDecimal amountRunning) {
		super();
		this.transaction = transaction;
		this.amountRunning = amountRunning;
	}

	public Transaction getTransaction() {
		return transaction;
	}

	public void setTransaction(Transaction transaction) {
		this.transaction = transaction;
	}

	public BigDecimal getAmountRunning() {
		return amountRunning;
	}

	public void setAmountRunning(BigDecimal amountRunning) {
		this.amountRunning = amountRunning;
	}

}
