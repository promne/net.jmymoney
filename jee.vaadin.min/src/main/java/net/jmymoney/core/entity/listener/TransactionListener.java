package net.jmymoney.core.entity.listener;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import net.jmymoney.core.entity.Transaction;
import net.jmymoney.core.entity.TransactionSplit;

public class TransactionListener {

	@PrePersist
	@PreUpdate
	public void doMaping(Transaction transaction) {
		for (TransactionSplit split : transaction.getSplits()) {
			if (split.getTransaction()==null) {
				split.setTransaction(transaction);
			}
		}
	}
	
}
