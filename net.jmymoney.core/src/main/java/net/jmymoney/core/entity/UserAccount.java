package net.jmymoney.core.entity;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    public static final String PROPERTY_ID = "id";

    @NotNull
    @Column(name = "username")
    private String username;
    public static final String PROPERTY_USERNAME = "username";

    @NotNull
    @Column(name = "password_hash")
    private String passwordHash;
    public static final String PROPERTY_PASSWORD_HASH = "passwordHash";

    @ManyToMany
    @JoinTable(name = "user_account_profile", joinColumns={@JoinColumn(name="user_account_id", referencedColumnName=PROPERTY_ID)}, inverseJoinColumns={@JoinColumn(name="profile_id", referencedColumnName=Profile.PROPERTY_ID)})
    private Set<Profile> profiles = new HashSet<>();
    public static final String PROPERTY_PROFILES = "profiles";

    public UserAccount() {
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Set<Profile> getProfiles() {
        return profiles;
    }

    public void setProfiles(Set<Profile> profiles) {
        this.profiles = profiles;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UserAccount other = (UserAccount) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    
}
