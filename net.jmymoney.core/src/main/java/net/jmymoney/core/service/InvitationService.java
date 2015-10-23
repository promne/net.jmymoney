package net.jmymoney.core.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import net.jmymoney.core.entity.InvitationProfile;
import net.jmymoney.core.entity.Profile;
import net.jmymoney.core.entity.UserAccount;

@Stateless
public class InvitationService {

    @PersistenceContext
    private EntityManager entityManager;
    
    public List<InvitationProfile> listInvitations(UserAccount userAccount) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<InvitationProfile> cq = cb.createQuery(InvitationProfile.class);
        Root<InvitationProfile> root = cq.from(InvitationProfile.class);
        cq.where(cb.equal(root.get(InvitationProfile.PROPERTY_PROFILE).get(Profile.PROPERTY_USER_ACCOUNT), userAccount));
        return entityManager.createQuery(cq).getResultList();
    }
    
    public Optional<InvitationProfile> find(String id) {
        return Optional.ofNullable(entityManager.find(InvitationProfile.class, id));        
    }
    
    public void create(InvitationProfile invitationProfile) {
        entityManager.persist(invitationProfile);
    }
 
    @Schedule(second="1", minute="*/10", hour="*", persistent=false)
    public void removeExpired() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaDelete<InvitationProfile> cd = cb.createCriteriaDelete(InvitationProfile.class);
        Root<InvitationProfile> root = cd.from(InvitationProfile.class);
        cd.where(cb.lessThanOrEqualTo(root.get(InvitationProfile.PROPERTY_EXPIRATION), new Date()));
        entityManager.createQuery(cd).executeUpdate();
    }    
}
