package net.jmymoney.core.service;

import java.math.BigDecimal;
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
import net.jmymoney.core.entity.Transaction;
import net.jmymoney.core.entity.TransactionSplit;
import net.jmymoney.core.entity.UserAccount;

@Stateless
public class AccountService {

    @Inject
    private Logger log;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Account> list(UserAccount userAccount) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Account> cq = cb.createQuery(Account.class);
        Root<Account> root = cq.from(Account.class);
        cq.where(cb.equal(root.get("userAccount"), userAccount));
        return entityManager.createQuery(cq).getResultList();
    }

    public List<AccountMetadata> listAccountMetadatas(UserAccount userAccount) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AccountMetadata> cq = cb.createQuery(AccountMetadata.class);
        Root<Account> account = cq.from(Account.class);

        Subquery<BigDecimal> sq = cq.subquery(BigDecimal.class);
        Root<Transaction> transaction = sq.from(Transaction.class);
        Join<Transaction, TransactionSplit> joinSplits = transaction.join("splits");
        sq.select(cb.sum(joinSplits.get("amount")));
        sq.where(cb.equal(account, transaction.get("account")));

        cq.select(cb.construct(AccountMetadata.class, account, sq.getSelection()));
        cq.where(cb.equal(account.get("userAccount"), userAccount));
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
            cq.select(root.get("id"));
            cq.where(cb.equal(root.get("parent").get("id"), currentAccountId));

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
        cq.where(cqRoot.get("account").get("id").in(accountIds));
        cq.select(cqRoot.get("id"));
        boolean canDelete = entityManager.createQuery(cq).setMaxResults(1).getResultList().isEmpty();

        log.debug("Deleting account {} : {}", accountId, canDelete);

        if (canDelete) {
            // delete them in order to avoid FK constraint failure
            while (!accountIds.isEmpty()) {
                Long deleteAccountId = accountIds.pollLast();
                CriteriaDelete<Account> cdc = cb.createCriteriaDelete(Account.class);
                Root<Account> cdcRoot = cdc.from(Account.class);
                cdc.where(cb.equal(cdcRoot.get("id"), deleteAccountId));
                entityManager.createQuery(cdc).executeUpdate();
            }
        }

        return canDelete;
    }

}
