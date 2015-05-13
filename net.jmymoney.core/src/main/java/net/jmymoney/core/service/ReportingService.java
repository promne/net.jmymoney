package net.jmymoney.core.service;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;

import net.jmymoney.core.domain.CategoryReport;
import net.jmymoney.core.domain.IncomeExpenseTouple;
import net.jmymoney.core.entity.Transaction;
import net.jmymoney.core.entity.TransactionSplit;
import net.jmymoney.core.entity.UserAccount;

@Stateless
public class ReportingService {

    @Inject
    private Logger log;

    @Inject
    private CategoryService categoryService;

    @PersistenceContext
    private EntityManager entityManager;

    public List<CategoryReport> getCategoryReport(UserAccount userAccount, Date dateFrom, Date dateTo,
            TemporalUnit groupByUnit) {
        assert dateFrom.before(dateTo);

        List<CategoryReport> result = categoryService.listCategories(userAccount).stream()
                .map(category -> new CategoryReport(category)).collect(Collectors.toList());
        result.add(new CategoryReport(null)); // unassigned

        Date dateFrameStart = dateFrom;
        while (dateFrameStart.before(dateTo)) {
            Date dateFrameEnd = Date.from((dateFrameStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault()).plus(1, groupByUnit).toInstant()));
            dateFrameEnd = dateFrameEnd.after(dateTo) ? dateTo : dateFrameEnd;

            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Tuple> cq = cb.createQuery(Tuple.class);

            Root<Transaction> trRoot = cq.from(Transaction.class);
            Join<Transaction, TransactionSplit> trSplits = trRoot.join("splits");

            cq.multiselect(trSplits.get("category").get("id"), cb.sum(trSplits.<BigDecimal> get("amount")));
            cq.groupBy(trSplits.get("category").get("id"));
            cq.where(cb.and(cb.equal(trRoot.get("account").get("userAccount"), userAccount),
                    cb.between(trRoot.get("timestamp"), dateFrameStart, dateFrameEnd),
                    cb.ge(trSplits.<BigDecimal> get("amount"), BigDecimal.ZERO)));

            List<Tuple> foundDataPlus = entityManager.createQuery(cq).getResultList();

            cq.where(cb.and(cb.equal(trRoot.get("account").get("userAccount"), userAccount),
                    cb.between(trRoot.get("timestamp"), dateFrameStart, dateFrameEnd),
                    cb.le(trSplits.<BigDecimal> get("amount"), BigDecimal.ZERO)));

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
        return result;
    }

}
