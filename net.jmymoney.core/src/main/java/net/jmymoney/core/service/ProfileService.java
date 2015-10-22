package net.jmymoney.core.service;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

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
    
}
