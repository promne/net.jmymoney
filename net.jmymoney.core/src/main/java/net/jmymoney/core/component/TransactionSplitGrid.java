package net.jmymoney.core.component;

import com.vaadin.data.Item;
import com.vaadin.data.util.GeneratedPropertyContainer;
import com.vaadin.data.util.PropertyValueGenerator;
import com.vaadin.ui.Grid;

import net.jmymoney.core.component.transaction.CategoryCaptionGenerator;
import net.jmymoney.core.data.TransactionSplitContainer;
import net.jmymoney.core.entity.TransactionSplit;

public class TransactionSplitGrid extends Grid {

    public static final String PROPERTY_CATEGORY_LABEL = "categoryLabel";
    
    public TransactionSplitGrid(TransactionSplitContainer dataSource) {
        GeneratedPropertyContainer wrapperContainer= new GeneratedPropertyContainer(dataSource);
        wrapperContainer.addGeneratedProperty(PROPERTY_CATEGORY_LABEL, new PropertyValueGenerator<String>() {
            @Override
            public String getValue(Item item, Object itemId, Object propertyId) {
                String result = null;
                TransactionSplit transactionSplit = (TransactionSplit)itemId;
                if (transactionSplit.getCategory() != null) {
                    result = CategoryCaptionGenerator.getCaption(transactionSplit.getCategory());
                }
                return result;
            }

            @Override
            public Class<String> getType() {
                return String.class;
            }
        });
        setContainerDataSource(wrapperContainer);
    }

}
