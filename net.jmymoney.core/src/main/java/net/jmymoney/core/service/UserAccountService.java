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
        cq.where(cb.equal(root.get(UserAccount.PROPERTY_USERNAME), username));

        UserAccount result = null;
        List<UserAccount> resultList = entityManager.createQuery(cq).getResultList();
        if (!resultList.isEmpty()) {
            UserAccount candidate = resultList.get(0);
            if (passwordMatches(password, candidate)) {
                result = candidate;
            }
        }
        return result;
    }
    
    public UserAccount find(Long id) {
        return entityManager.find(UserAccount.class, id);
    }
    
    public boolean passwordMatches(String password, UserAccount userAccount) {
        return BCrypt.checkpw(password, userAccount.getPasswordHash());
    }

    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(5));
    }
    
    public void update(UserAccount userAccount) {
        entityManager.merge(userAccount);
    }

}
