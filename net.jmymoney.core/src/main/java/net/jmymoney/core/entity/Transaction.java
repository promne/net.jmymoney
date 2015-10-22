package net.jmymoney.core.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import net.jmymoney.core.entity.listener.TransactionListener;

@Entity
@EntityListeners({ TransactionListener.class })
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    public static final String PROPERTY_ID = "id";

    @NotNull
    @Column(name = "time_stamp")
    private Date timestamp;
    public static final String PROPERTY_TIMESTAMP = "timestamp";

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<TransactionSplit> splits = new ArrayList<>();
    public static final String PROPERTY_SPLITS = "splits";

    @ManyToOne
    @NotNull
    @JoinColumn(name = "account_id")
    private Account account;
    public static final String PROPERTY_ACCOUNT = "account";

    public Transaction() {
        super();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public List<TransactionSplit> getSplits() {
        return splits;
    }

    public void setSplits(List<TransactionSplit> splits) {
        this.splits = splits;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public BigDecimal getAmount() {
        return splits.stream().map(TransactionSplit::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isChild() {
        return splits.stream().anyMatch(split -> split.getParent() != null);
    }
    
    public static void copyTransactionValues(Transaction fromTransaction, Transaction toTransaction) {
        toTransaction.setId(fromTransaction.getId());
        toTransaction.setTimestamp(fromTransaction.getTimestamp() == null ? null : (Date) fromTransaction.getTimestamp().clone());
        toTransaction.setSplits(fromTransaction.getSplits().stream().sequential().map(s -> copySplit(s, toTransaction)).collect(Collectors.toList()));
        toTransaction.setAccount(fromTransaction.getAccount());        
    }
    
    public static TransactionSplit copySplit(TransactionSplit split, Transaction transaction) {
        TransactionSplit splitCopy = new TransactionSplit();
        splitCopy.setAmount(split.getAmount());
        splitCopy.setCategory(split.getCategory());
        if (split.getChildren()!=null) {
            splitCopy.setChildren(new HashSet<>(split.getChildren()));
        }
        splitCopy.setId(split.getId());
        splitCopy.setNote(split.getNote());
        splitCopy.setSplitPartner(split.getSplitPartner());
        splitCopy.setParent(split.getParent());
        splitCopy.setTransaction(transaction);
        return splitCopy;
    }
    
}
