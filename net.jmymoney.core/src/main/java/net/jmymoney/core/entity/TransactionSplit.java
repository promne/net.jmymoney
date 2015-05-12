package net.jmymoney.core.entity;

import java.math.BigDecimal;
import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name="transaction_splits")
public class TransactionSplit {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	private Long id;

	@ManyToOne
	@JoinColumn(name="category_id")
	private Category category;

	@NotNull
	@Column(name="amount")
	private BigDecimal amount = BigDecimal.ZERO;
	
	@ManyToOne
	@NotNull
	@JoinColumn(name="transaction_id")
	private Transaction transaction;
	
	@ManyToOne
	@JoinColumn(name="parent_id")
	private TransactionSplit parent;

	@OneToMany(mappedBy="parent", orphanRemoval=true)
	private Collection<TransactionSplit> children;

	@Column(name="note")
	private String note;
	
	@ManyToOne
	@JoinColumn(name="split_partner_id")
	private SplitPartner splitPartner;
	public static final String PROPERTY_SPLIT_PARTNER = "splitPartner";

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public SplitPartner getSplitPartner() {
		return splitPartner;
	}

	public void setSplitPartner(SplitPartner splitPartner) {
		this.splitPartner = splitPartner;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public TransactionSplit getParent() {
		return parent;
	}

	public void setParent(TransactionSplit parent) {
		this.parent = parent;
	}

	public Collection<TransactionSplit> getChildren() {
		return children;
	}

	public void setChildren(Collection<TransactionSplit> children) {
		this.children = children;
	}

	public Transaction getTransaction() {
		return transaction;
	}

	public void setTransaction(Transaction transaction) {
		this.transaction = transaction;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

}
