package jee.vaadin.min;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import jmm.entity.Account;
import jmm.entity.Category;
import jmm.entity.Payee;
import jmm.entity.Transaction;
import jmm.entity.TransactionSplit;
import jmm.entity.UserAccount;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@Singleton
//@Startup
public class DemoDataInsert {

	private static final Logger LOG = LoggerFactory.getLogger(DemoDataInsert.class);
	
	@PersistenceContext
	EntityManager em;
	
	public DemoDataInsert() {
		super();
	}

	@PostConstruct
	private void init() {
		for (String username : new String[]{"a"}) {
			UserAccount userAccount = new UserAccount();
			userAccount.setUsername(username);
			userAccount.setPasswordHash(BCrypt.hashpw(username, BCrypt.gensalt()));
			em.persist(userAccount);
			
			simpleAccount(userAccount);
//			complexAccount(userAccount);
		}
	}

	public void simpleAccount(UserAccount userAccount) {
		Category category = new Category();
		category.setUserAccount(userAccount);
		category.setName("Category 1");
		em.persist(category);

		Category category2 = new Category();
		category2.setUserAccount(userAccount);
		category2.setName("Category 2");
		em.persist(category2);
		
		Account account = new Account();
		account.setName("aaaa");
		account.setUserAccount(userAccount);
		em.persist(account);
		
		Account account2 = new Account();
		account2.setName("ssss");
		account2.setUserAccount(userAccount);
		em.persist(account2);
		
		Account account3 = new Account();
		account3.setName("dddd");
		account3.setUserAccount(userAccount);
		em.persist(account3);

		Payee ts = new Payee();
		ts.setUserAccount(userAccount);
		ts.setName("unknown");
		em.persist(ts);
		
	}
	
	
	
	public void complexAccount(UserAccount userAccount) {
		long runningDate;
		List<Category> categoryList = new ArrayList<>();
		for (int i=0; i<15; i++) {
			Category category = new Category();
			category.setUserAccount(userAccount);
			category.setName("Category " + i);
			em.persist(category);
			categoryList.add(category);
		}
		for (int i=0; i<categoryList.size(); i++) {
			Category category = categoryList.get(i);
			if (i%5 != 0) {
				int parentId = (i/3) + (i/2);
				category.setParent(em.find(Category.class, categoryList.get(parentId).getId()));
				LOG.info("Adding category {} with parent {}", i, category.getParent()!=null ? category.getParent().getId() : "null");
			}
			em.merge(category);
		}
		
		List<Payee> transactionSubjectList = new ArrayList<>();
		for (int i=0; i<5; i++) {
			Payee ts = new Payee();
			ts.setUserAccount(userAccount);
			ts.setName("Subject "+i);
			em.persist(ts);
			transactionSubjectList.add(ts);
		}
		
		runningDate = 0;
		for (int a=1; a<2; a++) {
			Account account = new Account();
			account.setName("test " + a);
			account.setUserAccount(userAccount);
			em.persist(account);
			
			for (int i=1; i<a*10; i++) {
				Transaction tr = new Transaction();
				tr.setAccount(account);
				tr.setTimestamp(new Date(runningDate));
				runningDate += Math.round(987654 * Math.random());
				String minus = i%3==1 ? "-" : ""; 
				for (int j=0; j<i; j++) {
					TransactionSplit sp = new TransactionSplit();
					sp.setAmount(new BigDecimal(minus + i + "." + j));
					if (a != (i+j)%4) {
						sp.setCategory(em.find(Category.class,new Long((i+j) % categoryList.size())));
					}
					if (0 == (i+j)%a) {
						sp.setSplitPartner(transactionSubjectList.get((i+j)%transactionSubjectList.size()));
					}
					tr.getSplits().add(sp);				
				}
				em.persist(tr);
			}			
		}
	}
	
}
