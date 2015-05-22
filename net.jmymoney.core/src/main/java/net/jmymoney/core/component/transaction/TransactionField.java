package net.jmymoney.core.component.transaction;

import com.vaadin.data.Buffered;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.Validator.InvalidValueException;
import com.vaadin.data.fieldgroup.BeanFieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.ObjectProperty;
import com.vaadin.data.util.PropertyValueGenerator;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.shared.ui.datefield.Resolution;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.DateField;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.BaseTheme;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.apache.commons.beanutils.BeanUtils;

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

@Dependent
public class TransactionField extends CustomField<Transaction> {

    private static final String COLUMN_AMOUNT = "amount";
    private static final String COLUMN_CATEGORY = "category";
    private static final String COLUMN_SPLIT_PARTNER = "splitPartner";
    private static final String COLUMN_NOTE = "note";
    private static final String COLUMN_ACTION_GENERATED = "column_action_generated";
    
    private RegexpValidator bigDecimalValidator;

    private BeanFieldGroup<Transaction> fieldGroup;
    private BeanItemContainer<TransactionSplit> splitsContainer;
    private Table splitsTable;
    private DateField dateField;
    private Label transactionAmountLabel;
    private ComboBox transactionTypeComboBox;

    @Inject
    private CategoryService categoryService;

    @Inject
    private SplitPartnerService splitPartnerService;

    @Inject
    private AccountService accountService;

    @Inject
    private UserIdentity userIdentity;

    public TransactionField() {
        super();
        fieldGroup = new BeanFieldGroup<>(Transaction.class);

        splitsContainer = new BeanItemContainer<>(TransactionSplit.class);

        splitsTable = new Table("Splits", splitsContainer);
    }

    @Override
    protected Component initContent() {

        bigDecimalValidator = new RegexpValidator("\\d{1,10}?(\\.\\d{0,4})?", "Value has to be a positive number");

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);

        fieldGroup.setBuffered(true);

        dateField = new DateField("Date");
        dateField.setResolution(Resolution.MINUTE);
        dateField.addValueChangeListener(event -> fieldValueChanged());
        fieldGroup.bind(dateField, "timestamp");

        transactionTypeComboBox = new ComboBox("Operation", Arrays.asList(TransactionType.values()));
        transactionTypeComboBox.setNullSelectionAllowed(false);
        transactionTypeComboBox.addValueChangeListener(event -> fieldValueChanged());

        HorizontalLayout smallFieldsLayout = new HorizontalLayout();
        smallFieldsLayout.setSpacing(true);

        smallFieldsLayout.addComponent(dateField);
        smallFieldsLayout.addComponent(transactionTypeComboBox);

        transactionAmountLabel = new Label();
        transactionAmountLabel.setCaption("Amount");
        transactionAmountLabel.setSizeUndefined();

        GridLayout g = new GridLayout(2, 1);
        g.setSizeFull();
        g.addComponent(smallFieldsLayout);
        g.addComponent(transactionAmountLabel);
        g.setColumnExpandRatio(0, 1);

        layout.addComponent(g);

        splitsTable.setPageLength(4);
        splitsTable.setSizeFull();

        splitsTable.addGeneratedColumn(COLUMN_ACTION_GENERATED, (source, itemId, columnId) -> {
            Button removeSplitButton = new Button(new ThemeResource(ThemeResourceConstatns.DELETE_MEDIUM));
            removeSplitButton.setStyleName(BaseTheme.BUTTON_LINK);
            removeSplitButton.setDescription("Deletes the split");
            removeSplitButton.addClickListener(event -> {
                source.getContainerDataSource().removeItem(itemId);
            } );
            return removeSplitButton;
        } );
        // splitsTable.setColumnWidth(COLUMN_ACTION_GENERATED, 30);

        splitsTable.setVisibleColumns(COLUMN_SPLIT_PARTNER, COLUMN_CATEGORY, COLUMN_AMOUNT, COLUMN_NOTE, COLUMN_ACTION_GENERATED);
        splitsTable.setColumnHeaders("Partner", "Category", "Amount", "Note", "Action");
        splitsTable.addValueChangeListener(event -> fieldValueChanged());
        splitsTable.addItemSetChangeListener(event -> fieldValueChanged());
        splitsTable.setTableFieldFactory(new DefaultFieldFactory() {

            @Override
            public Field createField(Container container, Object itemId, Object propertyId, Component uiContext) {
                Field createField = null;
                if (COLUMN_CATEGORY.equals(propertyId)) {
                    ComboBox categoryCombo = createCategoryComboBox();
                    Category category = (Category) container.getContainerProperty(itemId, propertyId).getValue();
                    if (category != null) {
                        categoryCombo.setValue(category.getId());
                    }
                    createField = categoryCombo;
                }
                if (COLUMN_SPLIT_PARTNER.equals(propertyId)) {
                    ComboBox partnerCombo = createPartnerComboBox();
                    SplitPartner partner = (SplitPartner) container.getContainerProperty(itemId, propertyId).getValue();
                    if (partner != null) {
                        partnerCombo.setValue(partner.getId());
                    }
                    createField = partnerCombo;
                }

                if (createField == null) {
                    createField = super.createField(container, itemId, propertyId, uiContext);
                    if (createField instanceof AbstractTextField) {
                        ((AbstractTextField)createField).setNullRepresentation("");
                    }
                    createField.setSizeFull();
                }
                createField.addValueChangeListener(event -> fieldValueChanged());
                return createField;
            }

        });
        splitsTable.setEditable(true);

        HorizontalLayout splitsTableLayout = new HorizontalLayout();
        splitsTableLayout.setSpacing(true);
        splitsTableLayout.setSizeFull();
        splitsTableLayout.addComponent(splitsTable);

        Button addSplitButton = new Button("Add", (ClickListener) event -> {
            splitsContainer.addBean(new TransactionSplit());
        } );
        splitsTableLayout.addComponent(addSplitButton);

        splitsTableLayout.setExpandRatio(splitsTable, 1.0f);

        layout.addComponent(splitsTableLayout);
        layout.setExpandRatio(splitsTableLayout, 1.0f);

        return layout;
    }

    private void fieldValueChanged() {
        BigDecimal transactionAmount = splitsContainer.getItemIds().stream().filter(t -> t.getAmount() != null).map(TransactionSplit::getAmount).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        if (TransactionType.WITHDRAWAL == transactionTypeComboBox.getValue()) {
            // show as negative
            transactionAmount = transactionAmount.negate();
        }
        transactionAmountLabel.setValue(transactionAmount.stripTrailingZeros().toPlainString());
        fireEvent(new AbstractField.ValueChangeEvent(this));
    }

    @Override
    public Class<? extends Transaction> getType() {
        return Transaction.class;
    }

    @Override
    protected void setInternalValue(Transaction newValue) {
        super.setInternalValue(newValue);

        BeanItem<Transaction> beanItem = new BeanItem<Transaction>(newValue);
        fieldGroup.setItemDataSource(beanItem);
        setInternalSplits(beanItem);

        TransactionType transactionType = TransactionType.WITHDRAWAL;
        if (newValue != null && BigDecimal.ZERO.compareTo(newValue.getAmount()) < 0) {
            transactionType = TransactionType.DEPOSIT;
        }
        transactionTypeComboBox.setValue(transactionType);
    }

    private void setInternalSplits(BeanItem<Transaction> transactionBeanItem) {
        if (transactionBeanItem != null) {
            splitsContainer.removeAllItems();
            for (TransactionSplit split : (Collection<TransactionSplit>) transactionBeanItem.getItemProperty("splits").getValue()) {
                try {
                    TransactionSplit cloneSplit = (TransactionSplit) BeanUtils.cloneBean(split);
                    if (BigDecimal.ZERO.compareTo(cloneSplit.getAmount()) > 0) {
                        cloneSplit.setAmount(cloneSplit.getAmount().negate());
                    }
                    splitsContainer.addItem(cloneSplit);
                } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    @Override
    public void commit() throws SourceException, InvalidValueException {
        super.commit();
        try {
            fieldGroup.commit();
        } catch (CommitException e) {
            SourceException sourceException = new Buffered.SourceException(this, e);
            setCurrentBufferedSourceException(sourceException);
            throw sourceException;
        }
        splitsTable.commit();
        List<TransactionSplit> commitSplits = new ArrayList<>();
        for (TransactionSplit split : splitsContainer.getItemIds()) {
            TransactionSplit cloneSplit;
            try {
                cloneSplit = (TransactionSplit) BeanUtils.cloneBean(split);
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
            if ((BigDecimal.ZERO.compareTo(cloneSplit.getAmount()) > 0) ^ (TransactionType.WITHDRAWAL == transactionTypeComboBox.getValue())) {
                cloneSplit.setAmount(cloneSplit.getAmount().negate());
            }
            commitSplits.add(cloneSplit);
        }
        getInternalValue().setSplits(commitSplits);
    }

    @Override
    public void discard() throws SourceException {
        super.discard();
        fieldGroup.discard();
        setInternalSplits(fieldGroup.getItemDataSource());
        splitsTable.discard();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        markAsDirtyRecursive();
    }

    private ComboBox createCategoryComboBox() {
        BeanContainer<Long, Category> categoryContainer = new BeanContainer<>(Category.class);
        categoryContainer.setBeanIdProperty("id");
        categoryContainer.addAll(categoryService.listCategories(userIdentity.getUserAccount()));

        PropertyValueGenerator<String> generator = new PropertyValueGenerator<String>() {

            @Override
            public String getValue(Item item, Object itemId, Object propertyId) {
                StringBuilder sb = new StringBuilder();
                buildFullName(sb, ((BeanItem<Category>)item).getBean());
                return sb.toString();
            }

            private void buildFullName(StringBuilder sb, Category category) {
                if (category.getParent() != null) {
                    buildFullName(sb, category.getParent());
                    sb.append(" > ");
                }
                sb.append(category.getName());
            }
            
            @Override
            public Class<String> getType() {
                return String.class;
            }
        };
        
        Object propertyDisplayName = "property_display_name";
        for (Long itemId : categoryContainer.getItemIds()) {
            BeanItem<Category> item = categoryContainer.getItem(itemId);
            String content = generator.getValue(item, null, null);
            item.addItemProperty(propertyDisplayName, new ObjectProperty<String>(content));
        }
        
        ComboBox categoryCombo = new ComboBox();
        categoryCombo.setContainerDataSource(categoryContainer);
        categoryCombo.setConverter(new Converter<Object, Category>() {

            @Override
            public Category convertToModel(Object value, Class<? extends Category> targetType, Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
                if (value != null) {
                    return categoryContainer.getItem(value).getBean();
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
        categoryCombo.setItemCaptionPropertyId(propertyDisplayName);
        categoryCombo.setSizeFull();
        return categoryCombo;
    }

    private ComboBox createPartnerComboBox() {
        BeanContainer<Long, SplitPartner> partnerContainer = new BeanContainer<>(SplitPartner.class);
        partnerContainer.setBeanIdProperty(SplitPartner.PROPERTY_ID);
        refreshSplitPartners(partnerContainer);

        ComboBox partnerCombo = new ComboBox() {

            @Override
            public Resource getItemIcon(Object itemId) {
                if (partnerContainer.getItem(itemId).getBean() instanceof Account) {
                    return FontAwesome.BANK;
                }
                return null;
            }
            
        };
        partnerCombo.setContainerDataSource(partnerContainer);
        partnerCombo.setConverter(new Converter<Object, SplitPartner>() {

            @Override
            public SplitPartner convertToModel(Object value, Class<? extends SplitPartner> targetType, Locale locale)
                    throws com.vaadin.data.util.converter.Converter.ConversionException {
                if (value != null) {
                    return partnerContainer.getItem(value).getBean();
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
            QuestionDialog createPartnerDialog = new QuestionDialog("Add new partner", String.format("Partner <em>%s</em> was not found. Do you want to create it?", newItemCaption));
            createPartnerDialog.setDialogResultListener((dialogResultType, resultValue) -> {
                if (DialogResultType.OK.equals(dialogResultType)) {
                    Payee newPartner = new Payee();
                    newPartner.setName(newItemCaption);
                    newPartner.setUserAccount(userIdentity.getUserAccount());
                    splitPartnerService.create(newPartner);
                    refreshSplitPartners(partnerContainer);
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
        partnerCombo.setSizeFull();
        return partnerCombo;
    }

    private void refreshSplitPartners(BeanContainer<Long, SplitPartner> partnerContainer) {
        partnerContainer.removeAllItems();
        partnerContainer.addAll(listSplitPartners());
    }

    private List<SplitPartner> listSplitPartners() {
        List<SplitPartner> result = new ArrayList<>();
        result.addAll(splitPartnerService.listPayees(userIdentity.getUserAccount()));
        if (getInternalValue() != null) {
            result.addAll(accountService.list(userIdentity.getUserAccount()).stream().filter(splitPartner -> !splitPartner.getId().equals(getInternalValue().getAccount().getId()))
                    .collect(Collectors.toList()));
        }
        result.sort((i, j) -> i.getName().compareToIgnoreCase(j.getName()));
        return result;
    }
    
}
