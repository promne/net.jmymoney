package net.jmymoney.core.data;

import com.vaadin.data.util.BeanItemContainer;

import net.jmymoney.core.entity.Transaction;
import net.jmymoney.core.entity.TransactionSplit;
import net.jmymoney.core.util.PropertyResolver;

public class TransactionSplitContainer extends BeanItemContainer<TransactionSplit> {

    private static final long serialVersionUID = 6919899708431610079L;

    public TransactionSplitContainer() {
        super(TransactionSplit.class);
        addNestedContainerBean(TransactionSplit.PROPERTY_TRANSACTION);
        addNestedContainerBean(PropertyResolver.chainPropertyName(TransactionSplit.PROPERTY_TRANSACTION, Transaction.PROPERTY_ACCOUNT));        
        addNestedContainerBean(TransactionSplit.PROPERTY_SPLIT_PARTNER);
    }

}
