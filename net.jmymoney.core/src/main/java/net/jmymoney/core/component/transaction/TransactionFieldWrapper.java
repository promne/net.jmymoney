package net.jmymoney.core.component.transaction;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.util.converter.Converter.ConversionException;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomField;

import java.util.Date;
import java.util.stream.Collectors;

import javax.inject.Inject;

import net.jmymoney.core.entity.Transaction;
import net.jmymoney.core.entity.TransactionSplit;

public class TransactionFieldWrapper extends CustomField<Transaction> {

    @Inject
    private TransactionField complexField;

    @Inject
    private SimpleTransactionField simpleField;

    private CssLayout layout;

    @Override
    protected Component initContent() {
        complexField.addValueChangeListener(l -> fieldValueChanged());
        complexField.setVisible(false);
        simpleField.addValueChangeListener(l -> fieldValueChanged());
        layout = new CssLayout();
        layout.setWidth(100, Unit.PERCENTAGE);
        layout.addComponent(complexField);
        layout.addComponent(simpleField);
        return layout;
    }

    @Override
    public void commit() throws SourceException, InvalidValueException {
        super.commit();
        getCurrent().commit();
        copyTransactionValues(getCurrent().getValue(), getInternalValue());
    }

    @Override
    public void discard() throws SourceException {
        super.discard();
        setCurrentRight();
    }

    private TransactionSplit copySplit(TransactionSplit split, Transaction transaction) {
        TransactionSplit splitCopy = new TransactionSplit();
        splitCopy.setAmount(split.getAmount());
        splitCopy.setCategory(split.getCategory());
        splitCopy.setChildren(split.getChildren());
        splitCopy.setId(split.getId());
        splitCopy.setNote(split.getNote());
        splitCopy.setSplitPartner(split.getSplitPartner());
        splitCopy.setParent(split.getParent());
        splitCopy.setTransaction(transaction);
        return splitCopy;
    }
    
    private void copyTransactionValues(Transaction fromTransaction, Transaction toTransaction) {
        toTransaction.setId(fromTransaction.getId());
        toTransaction.setTimestamp(fromTransaction.getTimestamp() == null ? null : (Date) fromTransaction.getTimestamp().clone());
        toTransaction.setSplits(fromTransaction.getSplits().stream().sequential().map(s -> copySplit(s, toTransaction)).collect(Collectors.toList()));
        toTransaction.setAccount(fromTransaction.getAccount());        
    }
    
    @Override
    public void setValue(Transaction newFieldValue) throws ReadOnlyException, ConversionException {
        super.setValue(newFieldValue);
        setCurrentRight();
    }

    private void setCurrentRight() {
        Transaction transactionCopy = null;
        boolean visibleComplex = false;
        if (getInternalValue() != null) {
            transactionCopy = new Transaction();
            copyTransactionValues(getInternalValue(), transactionCopy);            
            visibleComplex = transactionCopy.getSplits().size()>1; 
        }
        complexField.setVisible(visibleComplex);
        simpleField.setVisible(!visibleComplex);
        
        getCurrent().setValue(transactionCopy);        
    }
    
    private void fieldValueChanged() {
        if (simpleField.isVisible() && simpleField.getValue().getSplits().size()>1) {
            Transaction passTransaction = simpleField.getValue();
            complexField.setValue(passTransaction);
            simpleField.setVisible(false);
            complexField.setVisible(true);
        }
        fireEvent(new AbstractField.ValueChangeEvent(this));
    }
    
    @Override
    public Class<? extends Transaction> getType() {
        return Transaction.class;
    }

    private CustomField<Transaction> getCurrent() {
        return simpleField.isVisible() ? simpleField : complexField;
    }
    
}
