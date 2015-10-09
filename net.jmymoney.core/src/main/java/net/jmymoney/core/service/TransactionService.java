package net.jmymoney.core.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import net.jmymoney.core.entity.Account;
import net.jmymoney.core.entity.SplitPartner;
import net.jmymoney.core.entity.Transaction;
import net.jmymoney.core.entity.TransactionSplit;

@Stateless
public class TransactionService {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Transaction> list(Account account) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Transaction> cq = cb.createQuery(Transaction.class);
        Root<Transaction> root = cq.from(Transaction.class);
        cq.where(cb.equal(root.get("account"), account));
        cq.orderBy(cb.asc(root.get("timestamp")));

        return entityManager.createQuery(cq).getResultList();
    }

    public Transaction update(Transaction transaction) {
        Transaction originalTransaction = entityManager.find(Transaction.class, transaction.getId());

        // unused - remove childs + remove by default
        List<TransactionSplit> unusedSplits = originalTransaction.getSplits().stream()
                .filter(orig -> !transaction.getSplits().stream().anyMatch(split -> orig.getId().equals(split.getId()))).collect(Collectors.toList());

        // new - deal with childs + save by default
        List<TransactionSplit> newSplits = transaction.getSplits().stream().filter(split -> split.getId() == null).collect(Collectors.toList());

        // changed - deal with childs
        List<TransactionSplit> changedSplits = new ArrayList<>();
        for (TransactionSplit split : transaction.getSplits()) {
            if (split.getId() == null) {
                continue;
            }
            TransactionSplit originalSplit = originalTransaction.getSplits().stream().filter(os -> os.getId().equals(split.getId())).findFirst().get();
            if (originalSplit.getSplitPartner() instanceof Account) {
                if (split.getSplitPartner() instanceof Account) {
                    // update - a la bruteforce
                    unusedSplits.add(originalSplit);
                    newSplits.add(split);
                } else {
                    // remove
                    unusedSplits.add(originalSplit);
                }
            } else {
                if (split.getSplitPartner() instanceof Account) {
                    // new
                    newSplits.add(split);
                }
                // update by default
            }

        }

        // deal witch childs
        deleteCounterparts(unusedSplits);
        for (TransactionSplit split : newSplits) {
            if (split.getId() == null) {
                split.setTransaction(transaction);
                entityManager.persist(split);
            } else {
                entityManager.merge(split);
            }
        }

        Transaction merge = entityManager.merge(transaction);
        createCounterparts(transaction, newSplits);

        return merge;
    }

    public List<TransactionSplit> listTransactionSplit(Transaction transaction) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Transaction> cq = cb.createQuery(Transaction.class);
        Root<Transaction> tr = cq.from(Transaction.class);
        tr.fetch("splits");
        cq.where(cb.equal(tr, transaction));

        return entityManager.createQuery(cq).getSingleResult().getSplits();
    }

    private void deleteCounterparts(Collection<TransactionSplit> splits) {
        for (TransactionSplit split : splits) {
            for (TransactionSplit child : split.getChildren()) {
                entityManager.remove(child.getTransaction());
            }
        }
    }

    public void delete(Long id) {
        Transaction transaction = entityManager.find(Transaction.class, id);
        deleteCounterparts(transaction.getSplits());
        entityManager.remove(transaction);
    }

    private void createCounterparts(Transaction parentTransaction, Collection<TransactionSplit> splits) {
        // create matching counterpart for transfers
        for (TransactionSplit split : splits) {
            if (split.getSplitPartner() instanceof Account) {
                Account pairAccount = (Account) split.getSplitPartner();
                Transaction pairTransaction = new Transaction();
                pairTransaction.setAccount(pairAccount);
                pairTransaction.setTimestamp(parentTransaction.getTimestamp());

                TransactionSplit pairSplit = new TransactionSplit();
                pairSplit.setAmount(split.getAmount().negate());
                pairSplit.setCategory(split.getCategory());
                pairSplit.setParent(split);
                pairSplit.setSplitPartner(parentTransaction.getAccount());

                pairTransaction.getSplits().add(pairSplit);
                entityManager.persist(pairTransaction);
            }
        }
    }

    public void create(Transaction transaction) {
        createCounterparts(transaction, transaction.getSplits());
        entityManager.persist(transaction);
    }

    public List<TransactionSplit> listTransactionSplit(SplitPartner splitPartner) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TransactionSplit> cq = cb.createQuery(TransactionSplit.class);
        Root<TransactionSplit> tr = cq.from(TransactionSplit.class);
        tr.fetch(TransactionSplit.PROPERTY_TRANSACTION);
        cq.where(cb.equal(tr.get(TransactionSplit.PROPERTY_SPLIT_PARTNER), splitPartner));
        
        return entityManager.createQuery(cq).getResultList();        
    }
}
