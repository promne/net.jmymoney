package net.jmymoney.core.entity;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name="payees")
public class Payee extends SplitPartner {
	
	public Payee() {
		super();
	}

}
