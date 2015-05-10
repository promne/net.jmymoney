package jmm.service;

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

import jmm.domain.CategoryReport;
import jmm.entity.Transaction;
import jmm.entity.TransactionSplit;
import jmm.entity.UserAccount;

@Stateless
public class ReportingService {

	@Inject
	private Logger log;
	
	@Inject
	private CategoryService categoryService;
	
	@PersistenceContext
	private EntityManager entityManager;	

	public List<CategoryReport> getCategoryReport(UserAccount userAccount, Date dateFrom, Date dateTo, TemporalUnit groupByUnit) {
		assert dateFrom.before(dateTo);
		
		List<CategoryReport> result = categoryService.listCategories(userAccount).stream().map(category -> new CategoryReport(category)).collect(Collectors.toList());
		result.add(new CategoryReport(null)); //unassigned
		
		
		Date dateFrameStart = dateFrom;
		while (dateFrameStart.before(dateTo)) {
			Date dateFrameEnd = Date.from((dateFrameStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).plus(1, groupByUnit).toInstant()));
			dateFrameEnd = dateFrameEnd.after(dateTo) ? dateTo : dateFrameEnd;
			
	    	CriteriaBuilder cb = entityManager.getCriteriaBuilder();
	    	CriteriaQuery<Tuple> cq = cb.createQuery(Tuple.class);
	    	
	    	Root<Transaction> trRoot = cq.from(Transaction.class);
	    	Join<Transaction, TransactionSplit> trSplits = trRoot.join("splits");
	    	
	    	cq.multiselect(trSplits.get("category").get("id"), cb.sum(trSplits.<BigDecimal>get("amount")));
	    	cq.groupBy(trSplits.get("category").get("id"));
	    	cq.where(cb.and(cb.equal(trRoot.get("account").get("userAccount"), userAccount), cb.between(trRoot.get("timestamp"), dateFrameStart, dateFrameEnd)));
			
			log.info("Doing report for: {} - {}", dateFrameStart, dateFrameEnd);	    	
	    	
	    	List<Tuple> foundData = entityManager.createQuery(cq).getResultList();
	    	
	    	result.forEach(categoryReport -> {
	    		Optional<Tuple> categoryData = foundData.stream().filter(tuple -> Objects.equals(tuple.get(0), categoryReport.getCategory()==null ? null : categoryReport.getCategory().getId())).findFirst();
	    		categoryReport.getValues().add(categoryData.isPresent() ? (BigDecimal)categoryData.get().get(1) : BigDecimal.ZERO);
	    	});
	    	
	    	
			dateFrameStart = Date.from((dateFrameStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).plus(1, groupByUnit).toInstant()));			
		}
		return result;
	}

}
