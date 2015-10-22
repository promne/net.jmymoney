package net.jmymoney.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jmymoney.core.entity.Account;
import net.jmymoney.core.entity.Category;
import net.jmymoney.core.entity.Payee;
import net.jmymoney.core.entity.Profile;
import net.jmymoney.core.entity.Transaction;
import net.jmymoney.core.entity.TransactionSplit;
import net.jmymoney.core.entity.UserAccount;

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

			Profile profile = new Profile();
			profile.setName(username);
			profile.setOwnerUserAccount(userAccount);
			userAccount.getProfiles().add(profile);
			em.persist(profile);

			profile = new Profile();
			profile.setName(username + "2");
			profile.setOwnerUserAccount(userAccount);
			userAccount.getProfiles().add(profile);
			em.persist(profile);
			
			em.persist(userAccount);
			
			
			simpleAccount(profile);
//			complexAccount(profile);
		}
	}

	public void simpleAccount(Profile profile) {
		Category category = new Category();
		category.setProfile(profile);
		category.setName("Category 1");
		em.persist(category);

		Category category2 = new Category();
		category2.setProfile(profile);
		category2.setName("Category 2");
		em.persist(category2);
		
		Account account = new Account();
		account.setName("aaaa");
		account.setProfile(profile);
		em.persist(account);
		
		Account account2 = new Account();
		account2.setName("ssss");
		account2.setProfile(profile);
		em.persist(account2);
		
		Account account3 = new Account();
		account3.setName("dddd");
		account3.setProfile(profile);
		em.persist(account3);

		Payee ts = new Payee();
		ts.setProfile(profile);
		ts.setName("unknown");
		em.persist(ts);
		
	}
	
	
	
	public void complexAccount(Profile profile) {
		long runningDate;
		List<Category> categoryList = new ArrayList<>();
		for (int i=0; i<15; i++) {
			Category category = new Category();
			category.setProfile(profile);
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
			ts.setProfile(profile);
			ts.setName("Subject "+i);
			em.persist(ts);
			transactionSubjectList.add(ts);
		}
		
		runningDate = 0;
		for (int a=1; a<2; a++) {
			Account account = new Account();
			account.setName("test " + a);
			account.setProfile(profile);
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
