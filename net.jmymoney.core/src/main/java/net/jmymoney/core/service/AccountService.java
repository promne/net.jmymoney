package net.jmymoney.core.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.slf4j.Logger;

import net.jmymoney.core.domain.AccountMetadata;
import net.jmymoney.core.entity.Account;
import net.jmymoney.core.entity.Profile;
import net.jmymoney.core.entity.Transaction;
import net.jmymoney.core.entity.TransactionSplit;

@Stateless
public class AccountService {

    @Inject
    private Logger log;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Account> list(Profile profile) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Account> cq = cb.createQuery(Account.class);
        Root<Account> root = cq.from(Account.class);
        cq.where(cb.equal(root.get(Account.PROPERTY_PROFILE), profile));
        return entityManager.createQuery(cq).getResultList();
    }

    public List<AccountMetadata> listAccountMetadatas(Profile profile) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AccountMetadata> cq = cb.createQuery(AccountMetadata.class);
        Root<Account> account = cq.from(Account.class);

        Subquery<BigDecimal> sq = cq.subquery(BigDecimal.class);
        Root<Transaction> transaction = sq.from(Transaction.class);
        Join<Transaction, TransactionSplit> joinSplits = transaction.join(Transaction.PROPERTY_SPLITS);
        sq.select(cb.sum(joinSplits.get(TransactionSplit.PROPERTY_AMOUNT)));
        sq.where(cb.equal(account, transaction.get(Transaction.PROPERTY_ACCOUNT)));

        cq.select(cb.construct(AccountMetadata.class, account, sq.getSelection()));
        cq.where(cb.equal(account.get(Account.PROPERTY_PROFILE), profile));
        cq.groupBy(account);

        return entityManager.createQuery(cq).getResultList();
    }

    public List<AccountMetadata> listAccountMetadatas(Profile profile, Date date) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AccountMetadata> cq = cb.createQuery(AccountMetadata.class);
        Root<Account> account = cq.from(Account.class);
        
        Subquery<BigDecimal> sq = cq.subquery(BigDecimal.class);
        Root<Transaction> transaction = sq.from(Transaction.class);
        Join<Transaction, TransactionSplit> joinSplits = transaction.join(Transaction.PROPERTY_SPLITS);
        sq.select(cb.sum(joinSplits.get(TransactionSplit.PROPERTY_AMOUNT)));
        sq.where(cb.and(cb.equal(account, transaction.get(Transaction.PROPERTY_ACCOUNT)), cb.lessThanOrEqualTo(transaction.get(Transaction.PROPERTY_TIMESTAMP), date)));
        
        cq.select(cb.construct(AccountMetadata.class, account, sq.getSelection()));
        cq.where(cb.equal(account.get(Account.PROPERTY_PROFILE), profile));
        cq.groupBy(account);
        
        return entityManager.createQuery(cq).getResultList();
    }

    public void create(Account account) {
        entityManager.persist(account);
    }

    public void update(Account account) {
        entityManager.merge(account);
    }

    public boolean delete(Long accountId) {
        Deque<Long> accountIds = new LinkedList<>();

        Queue<Long> childIds = new ConcurrentLinkedQueue<>();
        childIds.add(accountId);

        // find ids for the whole hierarchy
        while (!childIds.isEmpty()) {
            Long currentAccountId = childIds.poll();
            accountIds.add(currentAccountId);

            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<Account> root = cq.from(Account.class);
            cq.select(root.get(Account.PROPERTY_ID));
            cq.where(cb.equal(root.get(Account.PROPERTY_PARENT).get(Account.PROPERTY_ID), currentAccountId));

            List<Long> currentChilds = entityManager.createQuery(cq).getResultList();
            for (Long foundChildId : currentChilds) {
                if (!accountIds.contains(foundChildId)) {
                    childIds.add(foundChildId);
                }
            }
        }

        // check if any of them is used
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Transaction> cqRoot = cq.from(Transaction.class);
        cq.where(cqRoot.get(Transaction.PROPERTY_ACCOUNT).get(Account.PROPERTY_ID).in(accountIds));
        cq.select(cqRoot.get(Transaction.PROPERTY_ID));
        boolean canDelete = entityManager.createQuery(cq).setMaxResults(1).getResultList().isEmpty();

        log.debug("Deleting account {} : {}", accountId, canDelete);

        if (canDelete) {
            // delete them in order to avoid FK constraint failure
            while (!accountIds.isEmpty()) {
                Long deleteAccountId = accountIds.pollLast();
                CriteriaDelete<Account> cdc = cb.createCriteriaDelete(Account.class);
                Root<Account> cdcRoot = cdc.from(Account.class);
                cdc.where(cb.equal(cdcRoot.get(Account.PROPERTY_ID), deleteAccountId));
                entityManager.createQuery(cdc).executeUpdate();
            }
        }

        return canDelete;
    }

}
