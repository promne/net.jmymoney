package net.jmymoney.core.entity;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "invitation_profile")
public class InvitationProfile {

    @Id
    @Column(name = "id", length = 36)
    private String id;
    public static final String PROPERTY_ID = "id";

    @ManyToOne
    @NotNull
    @JoinColumn(name = "profile_id")
    Profile profile;
    public static final String PROPERTY_PROFILE = "profile";

    @NotNull
    @Column(name = "expiration")
    private Date expiration;
    public static final String PROPERTY_EXPIRATION = "expiration";

    public InvitationProfile() {
        super();
        id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

}
