package net.jmymoney.core.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Inheritance(strategy=InheritanceType.JOINED)
@Table(name="split_partners")
public abstract class SplitPartner {
    
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	private Long id;
	public static final String PROPERTY_ID = "id";

	@Column(name="name")
	private String name;
	public static final String PROPERTY_NAME = "name";
	
	@ManyToOne
	@JoinColumn(name="parent_id")
	private SplitPartner parent;
	public static final String PROPERTY_PARENT = "parent";
	
	@Lob
	@Column(name="description")
	private String description;
	public static final String PROPERTY_DESCRIPTION = "description";
	
	@ManyToOne
	@NotNull
	@JoinColumn(name="profile_id")
	Profile profile;
	public static final String PROPERTY_PROFILE = "profile";
	
	public SplitPartner() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public SplitPartner getParent() {
		return parent;
	}

	public void setParent(SplitPartner parent) {
		this.parent = parent;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Profile getProfile() {
		return profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
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
        SplitPartner other = (SplitPartner) obj;
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
