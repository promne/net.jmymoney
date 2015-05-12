package net.jmymoney.core.service;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.mindrot.jbcrypt.BCrypt;

import net.jmymoney.core.entity.UserAccount;

@Stateless
public class UserAccountService {

    @PersistenceContext
    private EntityManager entityManager;

	public UserAccount login(String username, String password) {
    	CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    	CriteriaQuery<UserAccount> cq = cb.createQuery(UserAccount.class);
    	Root<UserAccount> root = cq.from(UserAccount.class);
    	cq.where(cb.equal(root.get("username"), username));
    	
    	UserAccount result = null;
    	List<UserAccount> resultList = entityManager.createQuery(cq).getResultList();
    	if (!resultList.isEmpty()) {
    		UserAccount candidate = resultList.get(0);
    		if (BCrypt.checkpw(password, candidate.getPasswordHash())) {
    			result = candidate;
    		}
    	}
		return result;
	}
	
}
