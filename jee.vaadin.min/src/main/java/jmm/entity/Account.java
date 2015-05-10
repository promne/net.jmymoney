package jmm.entity;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="accounts")
public class Account extends SplitPartner {

	public Account() {
		super();
	}

}
