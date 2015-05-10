package jmm.service;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import jmm.entity.Payee;
import jmm.entity.SplitPartner;
import jmm.entity.UserAccount;

@Stateless
public class SplitPartnerService {

	@PersistenceContext
	private EntityManager entityManager;

    public List<Payee> listPayees(UserAccount userAccount) {
    	CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    	CriteriaQuery<Payee> cq = cb.createQuery(Payee.class);
    	Root<Payee> root = cq.from(Payee.class);
    	cq.where(cb.equal(root.get("userAccount"), userAccount));
    	return entityManager.createQuery(cq).getResultList();
    }

	public void create(Payee newPartner) {
		entityManager.persist(newPartner);
	}

	public void update(SplitPartner partner) {
		entityManager.merge(partner);
	}
    
}
