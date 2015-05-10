package jmm.entity.listener;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import jmm.entity.Transaction;
import jmm.entity.TransactionSplit;

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
