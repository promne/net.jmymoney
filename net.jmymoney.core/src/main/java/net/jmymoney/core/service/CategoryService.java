package net.jmymoney.core.service;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;

import net.jmymoney.core.entity.Category;
import net.jmymoney.core.entity.Profile;
import net.jmymoney.core.entity.TransactionSplit;

@Stateless
public class CategoryService {

    @Inject
    private Logger log;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Category> listCategories(Profile profile) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Category> cq = cb.createQuery(Category.class);
        Root<Category> root = cq.from(Category.class);
        cq.where(cb.equal(root.get(Category.PROPERTY_PROFILE), profile));
        return entityManager.createQuery(cq).getResultList();
    }

    public void create(Category category) {
        entityManager.persist(category);
    }

    public void update(Category category) {
        entityManager.merge(category);
    }

    /**
     * @param categoryId
     *            id of category
     * @return true if delete was successful, false means the category is used
     *         and can't be deleted
     */
    public boolean delete(Long categoryId) {
        Deque<Long> categoryIds = new LinkedList<>();

        Queue<Long> childIds = new ConcurrentLinkedQueue<>();
        childIds.add(categoryId);

        while (!childIds.isEmpty()) {
            Long currentCategoryId = childIds.poll();
            categoryIds.add(currentCategoryId);

            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<Category> root = cq.from(Category.class);
            cq.select(root.get(Category.PROPERTY_ID));
            cq.where(cb.equal(root.get(Category.PROPERTY_PARENT).get(Category.PROPERTY_ID), currentCategoryId));

            List<Long> currentChilds = entityManager.createQuery(cq).getResultList();
            for (Long foundChildId : currentChilds) {
                if (!categoryIds.contains(foundChildId)) {
                    childIds.add(foundChildId);
                }
            }
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<TransactionSplit> cqRoot = cq.from(TransactionSplit.class);
        cq.where(cqRoot.get(TransactionSplit.PROPERTY_CATEGORY).get(Category.PROPERTY_ID).in(categoryIds));
        cq.select(cqRoot.get(TransactionSplit.PROPERTY_ID));
        boolean canDelete = entityManager.createQuery(cq).setMaxResults(1).getResultList().isEmpty();

        log.debug("Deleting category {} : {}", categoryId, canDelete);

        if (canDelete) {
            while (!categoryIds.isEmpty()) {
                Long deleteCategoryId = categoryIds.pollLast();
                CriteriaDelete<Category> cdc = cb.createCriteriaDelete(Category.class);
                Root<Category> cdcRoot = cdc.from(Category.class);
                cdc.where(cb.equal(cdcRoot.get(Category.PROPERTY_ID), deleteCategoryId));
                entityManager.createQuery(cdc).executeUpdate();
            }
        }

        return canDelete;
    }

}
