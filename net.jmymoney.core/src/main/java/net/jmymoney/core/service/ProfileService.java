package net.jmymoney.core.service;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import net.jmymoney.core.entity.Profile;
import net.jmymoney.core.entity.UserAccount;

@Stateless
public class ProfileService {

    @PersistenceContext
    private EntityManager entityManager;
    
    public void create(Profile profile) {
        entityManager.persist(profile);
    }
    
    public List<Profile> list(UserAccount userAccount) {
        UserAccount find = entityManager.find(UserAccount.class, userAccount.getId());
        return new ArrayList<>(find.getProfiles());
    }

    public boolean add(UserAccount userAccount, Profile profile) {
        UserAccount latestAccount = entityManager.find(UserAccount.class, userAccount.getId());
        userAccount.setProfiles(latestAccount.getProfiles());
        if (userAccount.getProfiles().contains(profile)) {
            return false;
        }
        userAccount.getProfiles().add(profile);
        entityManager.merge(latestAccount);
        return true;
    }

    public List<UserAccount> listShares(Profile profile) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserAccount> cq = cb.createQuery(UserAccount.class);
        Root<UserAccount> root = cq.from(UserAccount.class);
        Join<UserAccount, Profile> profiles = root.join(UserAccount.PROPERTY_PROFILES);
        cq.where(cb.equal(profiles, profile));
        return entityManager.createQuery(cq).getResultList();
    }
    
}
