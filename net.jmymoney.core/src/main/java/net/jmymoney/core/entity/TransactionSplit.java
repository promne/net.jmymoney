package net.jmymoney.core.entity;

import java.math.BigDecimal;
import java.util.Collection;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "transaction_splits")
public class TransactionSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    public static final String PROPERTY_ID = "id";

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
    public static final String PROPERTY_CATEGORY = "category";

    @NotNull
    @Column(name = "amount")
    private BigDecimal amount = BigDecimal.ZERO;
    public static final String PROPERTY_AMOUNT = "amount";

    @ManyToOne
    @NotNull
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;
    public static final String PROPERTY_TRANSACTION = "transaction";

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private TransactionSplit parent;
    public static final String PROPERTY_PARENT = "parent";

    @OneToMany(mappedBy = "parent", orphanRemoval = true, fetch=FetchType.EAGER)
    private Collection<TransactionSplit> children;

    @Column(name = "note")
    private String note;
    public static final String PROPERTY_NOTE = "note";

    @ManyToOne
    @JoinColumn(name = "split_partner_id")
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
        TransactionSplit other = (TransactionSplit) obj;
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
