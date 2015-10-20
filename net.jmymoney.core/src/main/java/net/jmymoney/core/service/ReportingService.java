package net.jmymoney.core.service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.slf4j.Logger;

import net.jmymoney.core.domain.CategoryReport;
import net.jmymoney.core.domain.IncomeExpenseTouple;
import net.jmymoney.core.entity.Account;
import net.jmymoney.core.entity.Category;
import net.jmymoney.core.entity.SplitPartner;
import net.jmymoney.core.entity.Transaction;
import net.jmymoney.core.entity.TransactionSplit;
import net.jmymoney.core.entity.UserAccount;

@Stateless
public class ReportingService {

    @Inject
    private Logger log;

    @Inject
    private CategoryService categoryService;
    
    @Inject AccountService accountService;

    @PersistenceContext
    private EntityManager entityManager;

    public List<CategoryReport> getCategoryReport(UserAccount userAccount, Date dateFrom, Date dateTo, TemporalUnit groupByUnit, Collection<Account> includeAccounts, boolean excludeTransfers, boolean includeSubCategories, Collection<Category> includeCategories, boolean includeWithoutCategory) {
        assert dateFrom.before(dateTo);

        List<CategoryReport> result = categoryService.listCategories(userAccount).stream().map(category -> new CategoryReport(category)).collect(Collectors.toList());
        if (includeWithoutCategory) {
            result.add(new CategoryReport(null)); // unassigned
        }

        Date dateFrameStart = dateFrom;
        while (dateFrameStart.before(dateTo)) {
            Date dateFrameEnd = Date.from((dateFrameStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).plus(1, groupByUnit).toInstant()));
            dateFrameEnd = dateFrameEnd.after(dateTo) ? dateTo : dateFrameEnd;

            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Tuple> cq = cb.createQuery(Tuple.class);

            Root<Transaction> trRoot = cq.from(Transaction.class);
            Join<Transaction, TransactionSplit> trSplits = trRoot.join(Transaction.PROPERTY_SPLITS);

            cq.multiselect(trSplits.get(TransactionSplit.PROPERTY_CATEGORY).get(Category.PROPERTY_ID), cb.sum(trSplits.<BigDecimal> get(TransactionSplit.PROPERTY_AMOUNT)));
            cq.groupBy(trSplits.get(TransactionSplit.PROPERTY_CATEGORY).get(Category.PROPERTY_ID));
            
            Predicate basePredicate = cb.and(cb.equal(trRoot.get(Transaction.PROPERTY_ACCOUNT).get(SplitPartner.PROPERTY_USER_ACCOUNT), userAccount), cb.between(trRoot.get(Transaction.PROPERTY_TIMESTAMP), dateFrameStart, dateFrameEnd), trRoot.get(Transaction.PROPERTY_ACCOUNT).in(includeAccounts));
            if (excludeTransfers) {
                Subquery<Account> subquery = cq.subquery(Account.class);
                Root<Account> subAccount = subquery.from(Account.class);
                subquery.select(subAccount);
                subquery.where(cb.and(cb.equal(subAccount, trSplits.get(TransactionSplit.PROPERTY_SPLIT_PARTNER)), subAccount.in(includeAccounts)));
                basePredicate = cb.and(basePredicate, cb.exists(subquery).not());
            }
            if (!includeWithoutCategory) {
                basePredicate = cb.and(basePredicate, cb.isNotNull(trSplits.get(TransactionSplit.PROPERTY_CATEGORY)));
            }
            
            
            cq.where(cb.and(basePredicate, cb.ge(trSplits.<BigDecimal> get(TransactionSplit.PROPERTY_AMOUNT), BigDecimal.ZERO)));
            List<Tuple> foundDataPlus = entityManager.createQuery(cq).getResultList();

            cq.where(cb.and(basePredicate, cb.le(trSplits.<BigDecimal> get(TransactionSplit.PROPERTY_AMOUNT), BigDecimal.ZERO)));
            List<Tuple> foundDataMinus = entityManager.createQuery(cq).getResultList();
            
            
            result.forEach(categoryReport -> {
                Optional<Tuple> categoryDataPlus = foundDataPlus.stream()
                        .filter(tuple -> Objects.equals(tuple.get(0), categoryReport.getCategory() == null ? null : categoryReport.getCategory().getId()))
                        .findFirst();
                Optional<Tuple> categoryDataMinus = foundDataMinus.stream()
                        .filter(tuple -> Objects.equals(tuple.get(0),categoryReport.getCategory() == null ? null : categoryReport.getCategory().getId()))
                        .findFirst();
                
                IncomeExpenseTouple incomeExpenseTouple = new IncomeExpenseTouple(
                        categoryDataPlus.isPresent() ? (BigDecimal) categoryDataPlus.get().get(1) : BigDecimal.ZERO, 
                        categoryDataMinus.isPresent() ? (BigDecimal) categoryDataMinus.get().get(1) : BigDecimal.ZERO
                    );
                categoryReport.getIncomesAndExpenses().add(incomeExpenseTouple);
                
            } );


            dateFrameStart = Date.from((dateFrameStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault()).plus(1, groupByUnit).toInstant()));
        }
        
        result.stream().filter(e -> e.getCategory()!=null && !includeCategories.contains(e.getCategory())).flatMap(e -> e.getIncomesAndExpenses().stream()).forEach(e -> {e.setExpense(BigDecimal.ZERO); e.setIncome(BigDecimal.ZERO);});
        if (includeSubCategories) {
            result.stream().filter(e -> e.getCategory()!=null && e.getCategory().getParent()==null).forEach(e -> includeSubCategories(e, result));
        }

        Set<CategoryReport> removeCategoriesFromReport = result.stream().filter(e -> e.getCategory()!=null && !includeCategories.contains(e.getCategory()) && e.getTotal().getBalance().equals(BigDecimal.ZERO)).collect(Collectors.toSet());
        result.removeAll(removeCategoriesFromReport);
        
        return result;        
    }
    
    private void includeSubCategories(CategoryReport categoryReport, List<CategoryReport> reportsAvailable) {
        reportsAvailable.stream().filter(e -> e.getCategory()!=null && e.getCategory().getParent()==categoryReport.getCategory()).forEach(e -> includeSubCategories(e, reportsAvailable));
        
        BinaryOperator<List<IncomeExpenseTouple>> asdsa = (t, u) -> {
            List<IncomeExpenseTouple> result = new ArrayList<>(t);
            for (int i=0; i<result.size(); i++) {
                result.set(i, result.get(i).add(u.get(i)));
            }
            return result;
        };
        categoryReport.setIncomesAndExpenses(reportsAvailable.stream().filter(e -> e.getCategory()!=null && e.getCategory().getParent()==categoryReport.getCategory()).map(CategoryReport::getIncomesAndExpenses).reduce(categoryReport.getIncomesAndExpenses(), asdsa));        
    }
    
    
    public List<CategoryReport> getCategoryReport(UserAccount userAccount, Date dateFrom, Date dateTo, TemporalUnit groupByUnit, boolean excludeTransfers) {
        return getCategoryReport(userAccount, dateFrom, dateTo, groupByUnit, accountService.list(userAccount), excludeTransfers, false, categoryService.listCategories(userAccount), true);
    }

}
