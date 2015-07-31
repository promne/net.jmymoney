package net.jmymoney.core.component.transaction;

import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.data.util.converter.StringToBigDecimalConverter;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.DateField;
import com.vaadin.ui.TextField;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.inject.Inject;

import net.jmymoney.core.UserIdentity;
import net.jmymoney.core.component.DialogResultType;
import net.jmymoney.core.component.QuestionDialog;
import net.jmymoney.core.entity.Account;
import net.jmymoney.core.entity.Category;
import net.jmymoney.core.entity.Payee;
import net.jmymoney.core.entity.SplitPartner;
import net.jmymoney.core.entity.Transaction;
import net.jmymoney.core.entity.TransactionSplit;
import net.jmymoney.core.service.AccountService;
import net.jmymoney.core.service.CategoryService;
import net.jmymoney.core.service.SplitPartnerService;
import net.jmymoney.core.theme.ThemeResourceConstatns;

public class SimpleTransactionField extends CustomField<Transaction> {

    private static final Object PROPERTY_CATEGORY_DISPLAY_NAME = "property_display_name";
    
    private BeanFieldGroup<Transaction> transactionFieldGroup;
    private BeanFieldGroup<TransactionSplit> splitFieldGroup;
    
    private DateField simpleDateField;
    private ComboBox simpleTransactionTypeComboBox;
    private ComboBox simplePartnerComboBox;
    private ComboBox simpleCategoryComboBox;
    private TextField simpleAmountTextField;
    private TextField simpleNoteTextField;

    @Inject
    private CategoryService categoryService;

    @Inject
    private SplitPartnerService splitPartnerService;

    @Inject
    private AccountService accountService;

    @Inject
    private UserIdentity userIdentity;
    
    
    public SimpleTransactionField() {
        super();
        transactionFieldGroup = new BeanFieldGroup<Transaction>(Transaction.class);
        splitFieldGroup = new BeanFieldGroup<TransactionSplit>(TransactionSplit.class);
    }

    @Override
    protected Component initContent() {
        CssLayout layout = new CssLayout();
        layout.addStyleName("ordered-layout");

        simpleDateField = new DateField("Date");
        simpleDateField.setResolution(Resolution.MINUTE);
        transactionFieldGroup.bind(simpleDateField, Transaction.PROPERTY_TIMESTAMP);
        simpleDateField.addValueChangeListener(event -> fieldValueChanged());
        layout.addComponent(simpleDateField);

        simpleTransactionTypeComboBox = new ComboBox("Operation", Arrays.asList(TransactionType.values()));
        simpleTransactionTypeComboBox.setNullSelectionAllowed(false);
        simpleTransactionTypeComboBox.addValueChangeListener(event -> fieldValueChanged());
        layout.addComponent(simpleTransactionTypeComboBox);

        simplePartnerComboBox = createPartnerComboBox();
        simplePartnerComboBox.setCaption("Partner");
        refreshPartners();
        splitFieldGroup.bind(simplePartnerComboBox, TransactionSplit.PROPERTY_SPLIT_PARTNER);
        layout.addComponent(simplePartnerComboBox);
        
        simpleCategoryComboBox = createCategoryComboBox();
        simpleCategoryComboBox.setCaption("Category");
        refreshCategories();
        splitFieldGroup.bind(simpleCategoryComboBox, TransactionSplit.PROPERTY_CATEGORY);
        layout.addComponent(simpleCategoryComboBox);
        
        simpleAmountTextField = new TextField("Amount");
        simpleAmountTextField.setConverter(new StringToBigDecimalConverter() {

            @Override
            public BigDecimal convertToModel(String value, Class<? extends BigDecimal> targetType, Locale locale) throws ConversionException {
                BigDecimal convertToModel = super.convertToModel(value, targetType, locale);
                if (convertToModel == null) {
                    convertToModel = BigDecimal.ZERO;
                }
                if ((BigDecimal.ZERO.compareTo(convertToModel) <= 0) && (TransactionType.WITHDRAWAL == simpleTransactionTypeComboBox.getValue())) {
                    convertToModel = convertToModel.negate();
                }
                return convertToModel;
            }

            @Override
            public String convertToPresentation(BigDecimal value, Class<? extends String> targetType, Locale locale) throws ConversionException {
                return super.convertToPresentation(value.abs(), targetType, locale);
            }

            
        });
        splitFieldGroup.bind(simpleAmountTextField, TransactionSplit.PROPERTY_AMOUNT);
        layout.addComponent(simpleAmountTextField);
        
        simpleNoteTextField = new TextField("Description");
        simpleNoteTextField.setNullRepresentation("");
        splitFieldGroup.bind(simpleNoteTextField, TransactionSplit.PROPERTY_NOTE);
        layout.addComponent(simpleNoteTextField);
        
        Button addSplitButton = new Button("Split", event -> {
            Transaction passTransaction = getValue();
            passTransaction.getSplits().add(new TransactionSplit());
            commit(); //nasty autocommit
            fieldValueChanged();
        });
        addSplitButton.setIcon(FontAwesome.RANDOM);
        layout.addComponent(addSplitButton);
        
        return layout;
    }

    private void fieldValueChanged() {
        fireEvent(new AbstractField.ValueChangeEvent(this));
    }
    
    @Override
    public Class<? extends Transaction> getType() {
        return Transaction.class;
    }

    @Override
    protected void setInternalValue(Transaction newValue) {
        super.setInternalValue(newValue);
        
        TransactionSplit splitItem = null;
        if (!newValue.getSplits().isEmpty()) {
            splitItem = newValue.getSplits().get(0);
        }

        refreshCategories();
        refreshPartners();
        
        transactionFieldGroup.setItemDataSource(newValue);
        splitFieldGroup.setItemDataSource(splitItem);
        
        TransactionType transactionType = TransactionType.WITHDRAWAL;
        if (newValue != null && BigDecimal.ZERO.compareTo(newValue.getAmount()) < 0) {
            transactionType = TransactionType.DEPOSIT;
        }
        simpleTransactionTypeComboBox.setValue(transactionType);
    }

    
    @Override
    public void commit() throws SourceException, InvalidValueException {
        super.commit();
        try {
            splitFieldGroup.commit();
            transactionFieldGroup.commit();
        } catch (CommitException e) {
            throw new InvalidValueException("Errors", (InvalidValueException[])e.getInvalidFields().values().toArray());
        }
    }

    @Override
    public void discard() throws SourceException {
        super.discard();
        splitFieldGroup.discard();
        transactionFieldGroup.discard();
    }

    private List<SplitPartner> listSplitPartners() {
        List<SplitPartner> result = new ArrayList<>();
        result.addAll(splitPartnerService.listPayees(userIdentity.getUserAccount()));
        if (getInternalValue() != null) {
            List<Account> accounts = accountService.list(userIdentity.getUserAccount());
            if (getInternalValue().getAccount() != null) {
                Long actualAccountId = getInternalValue().getAccount().getId();
                accounts = accounts.stream().filter(account -> !account.getId().equals(actualAccountId)).collect(Collectors.toList());
            }
            result.addAll(accounts);
        }
        result.sort((i, j) -> i.getName().compareToIgnoreCase(j.getName()));
        return result;
    }
    
    private void refreshCategories() {
        BeanContainer<Long, Category> categoryContainer = new BeanContainer<>(Category.class);
        categoryContainer.setBeanIdProperty(Category.PROPERTY_ID);
        
        for (Category category : categoryService.listCategories(userIdentity.getUserAccount())) {
            BeanItem<Category> item = categoryContainer.addBean(category);
            String content = CategoryCaptionGenerator.getCaption(category);
            item.addItemProperty(PROPERTY_CATEGORY_DISPLAY_NAME, new ObjectProperty<String>(content));            
        }

        simpleCategoryComboBox.setContainerDataSource(categoryContainer);
    }
    
    private void refreshPartners() {
        BeanContainer<Long, SplitPartner> partnerContainer = new BeanContainer<>(SplitPartner.class);
        partnerContainer.setBeanIdProperty(SplitPartner.PROPERTY_ID);
        partnerContainer.addAll(listSplitPartners());
        simplePartnerComboBox.setContainerDataSource(partnerContainer);
    }
    
    private ComboBox createCategoryComboBox() {
        ComboBox categoryCombo = new ComboBox();
        categoryCombo.setConverter(new Converter<Object, Category>() {

            @Override
            public Category convertToModel(Object value, Class<? extends Category> targetType, Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
                if (value != null) {
                    return ((BeanContainer<Long, Category>)categoryCombo.getContainerDataSource()).getItem(value).getBean();
                }
                return null;
            }

            @Override
            public Object convertToPresentation(Category value, Class<? extends Object> targetType, Locale locale) throws ConversionException {
                if (value != null) {
                    return value.getId();
                }
                return null;
            }

            @Override
            public Class<Category> getModelType() {
                return Category.class;
            }

            @Override
            public Class<Object> getPresentationType() {
                return Object.class;
            }

        });

        categoryCombo.setFilteringMode(FilteringMode.CONTAINS);
        categoryCombo.setItemCaptionPropertyId(PROPERTY_CATEGORY_DISPLAY_NAME);
        return categoryCombo;
    }

    private ComboBox createPartnerComboBox() {
        ComboBox partnerCombo = new ComboBox() {

            @Override
            public Resource getItemIcon(Object itemId) {
                if (((BeanItem<SplitPartner>)getItem(itemId)).getBean() instanceof Account) {
                    return ThemeResourceConstatns.BANK;
                }
                return null;
            }
            
        }; 
        partnerCombo.setConverter(new Converter<Object, SplitPartner>() {

            @Override
            public SplitPartner convertToModel(Object value, Class<? extends SplitPartner> targetType, Locale locale)
                    throws com.vaadin.data.util.converter.Converter.ConversionException {
                if (value != null) {
                    return ((BeanContainer<Long, SplitPartner>)partnerCombo.getContainerDataSource()).getItem(value).getBean();
                }
                return null;
            }

            @Override
            public Object convertToPresentation(SplitPartner value, Class<? extends Object> targetType, Locale locale) throws ConversionException {
                if (value != null) {
                    return value.getId();
                }
                return null;
            }

            @Override
            public Class<SplitPartner> getModelType() {
                return SplitPartner.class;
            }

            @Override
            public Class<Object> getPresentationType() {
                return Object.class;
            }

        });
        partnerCombo.setNewItemsAllowed(true);
        partnerCombo.setNewItemHandler(newItemCaption -> {
            for (final Object itemId : partnerCombo.getItemIds()) {
                if (newItemCaption.equalsIgnoreCase(partnerCombo.getItemCaption(itemId))) {
                    partnerCombo.select(itemId);
                    return;
                }
            }            
            
            QuestionDialog createPartnerDialog = new QuestionDialog("Add new partner",
                    String.format("Partner <em>%s</em> was not found. Do you want to create it?", newItemCaption));
            createPartnerDialog.setDialogResultListener((dialogResultType, resultValue) -> {
                if (DialogResultType.OK.equals(dialogResultType)) {
                    Payee newPartner = new Payee();
                    newPartner.setName(newItemCaption);
                    newPartner.setUserAccount(userIdentity.getUserAccount());
                    splitPartnerService.create(newPartner);
                    refreshPartners();
                    // TODO I know, it's necessary to update all
                    // combo boxes. Screw that at the moment
                    for (Object itemId1 : partnerCombo.getItemIds()) {
                        if (newItemCaption.equalsIgnoreCase(partnerCombo.getItemCaption(itemId1))) {
                            partnerCombo.select(itemId1);
                            break;
                        }
                    }

                }
            } );
            createPartnerDialog.show();
        } );

        partnerCombo.setFilteringMode(FilteringMode.CONTAINS);
        partnerCombo.setItemCaptionPropertyId("name");
        return partnerCombo;
    }

}
