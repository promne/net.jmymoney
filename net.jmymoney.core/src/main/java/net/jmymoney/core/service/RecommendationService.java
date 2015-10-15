package net.jmymoney.core.service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.MapUtils;

import net.jmymoney.core.entity.Account;
import net.jmymoney.core.entity.Category;
import net.jmymoney.core.entity.SplitPartner;
import net.jmymoney.core.entity.Transaction;

public class RecommendationService {

    @Inject
    TransactionService transactionService;
    
    @Inject
    CategoryService categoryService;
    
    /*
     * Tries to guess best category for selected account, split partner and date.
     */
    public Optional<Category> getCategoryForPartner(Account account, SplitPartner splitPartner, Date date) {
        List<Transaction> list = transactionService.list(account);

        for (int i=1; i<3; i++) {
            Date dateRangeStart = new Date(date.toInstant().minusSeconds(3600*24*35*i).toEpochMilli());
            Map<Category, Long> collect = list.stream()
                    .filter(t -> t.getTimestamp().before(date) && t.getTimestamp().after(dateRangeStart))
                    .map(t -> t.getSplits())
                    .flatMap(ts -> ts.stream())
                    .filter(ts -> splitPartner.getId() == (ts.getSplitPartner()==null ? null : ts.getSplitPartner().getId()))
                    .map(ts -> ts.getCategory())
                    .filter(c -> c!=null)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            
            if (!collect.isEmpty()) {
                TreeMap<Long, Category> treeMap = new TreeMap<>(MapUtils.invertMap(collect));
                return Optional.of(treeMap.lastEntry().getValue());
            }
        }
            
        return Optional.empty();
    }
    
}
