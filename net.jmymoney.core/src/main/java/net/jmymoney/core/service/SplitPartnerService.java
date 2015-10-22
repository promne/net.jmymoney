package net.jmymoney.core.service;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;

import net.jmymoney.core.entity.Payee;
import net.jmymoney.core.entity.Profile;
import net.jmymoney.core.entity.SplitPartner;
import net.jmymoney.core.entity.TransactionSplit;

@Stateless
public class SplitPartnerService {

    @Inject
    private Logger log;
    
    @PersistenceContext
    private EntityManager entityManager;

    public List<Payee> listPayees(Profile profile) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Payee> cq = cb.createQuery(Payee.class);
        Root<Payee> root = cq.from(Payee.class);
        cq.where(cb.equal(root.get(Payee.PROPERTY_PROFILE), profile));
        return entityManager.createQuery(cq).getResultList();
    }

    public void create(SplitPartner splitPartner) {
        entityManager.persist(splitPartner);
    }

    public void update(SplitPartner splitPartner) {
        entityManager.merge(splitPartner);
    }

    public boolean delete(Long splitPartnerId) {
        //check if any of them is used
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<TransactionSplit> cqRoot = cq.from(TransactionSplit.class);
        cq.where(cqRoot.get(TransactionSplit.PROPERTY_SPLIT_PARTNER).get(SplitPartner.PROPERTY_ID).in(splitPartnerId));
        cq.select(cqRoot.get(SplitPartner.PROPERTY_ID));
        boolean canDelete = entityManager.createQuery(cq).setMaxResults(1).getResultList().isEmpty();
        
        log.debug("Deleting split partner {} : {}", splitPartnerId, canDelete);
        if (canDelete) {
            entityManager.remove(entityManager.find(SplitPartner.class, splitPartnerId));
        }
        return canDelete;
    }

}
