package com.account360i.postenrichment;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.allsight.Party;
import com.allsight.Party.PaymentBehaviour;
import com.allsight.Party.Transaction;
import com.allsight.enrichment.common.EnrichmentFunction;
import com.allsight.entity.impl.Entity;


/**
 * @author Aradhana Pandey
 * Class to find payment summary of an organization
 *
 */
public class PaymentBehaviourSummary extends EnrichmentFunction {
	private static final Logger logger = Logger.getLogger(PaymentBehaviourSummary.class);
	private List<Long> TimeTaken = new ArrayList<Long>();
	private Double Revenue = 0.0;
	private Map<String,Long> ProductTimeTaken = new HashMap<String,Long>();
	//ListMultimap<String, Long> productTimeTaken = ArrayListMultimap.create();


	@Override
	public Object applyEnrichment(Entity<?> entity) throws Exception {
		// TODO Auto-generated method stub

		if (!getExecutionContext().isEIDRecord()) {
			logger.warn("This enrichment expects an EID record. Skipping enrichment");
			return null;
		} 

		Party asparty = paymentSummary(entity);

		logger.debug("asparty :: " + asparty);

		return asparty;
	}

	/**
	 * @param entity
	 * Function to populate payment behaviour business object
	 */
	public Party paymentSummary(Entity<?> entity) {
		// TODO Auto-generated method stub

		logger.debug("Eid Party : " + entity);

		if (((Party) entity).getTransactions() != null && ((Party) entity).getTransactions().getTransaction()!= null) {
			Collection<Transaction> transactions = ((Party) entity).getTransactions().getTransaction();
			logger.debug("Transaction collection : " + transactions);

			for (Transaction transaction : transactions) {
				if(transaction.getEndTimestamp() != null && transaction.getStartTimestamp() != null) {
					Timestamp endtime = transaction .getEndTimestamp();
					Timestamp starttime = transaction.getStartTimestamp();
					long milliseconds = endtime.getTime() - starttime.getTime();
					Long millisec = new Long(milliseconds);
					TimeTaken.add(millisec);

					String productName = transaction.getProductName().toLowerCase().trim();
					populateMap(productName,millisec);
				}

				if(transaction.getTransactionAmountDouble() != null) {
					Revenue += transaction.getTransactionAmountDouble();  // total revenue for an account
				}	
			}
			String productName = maxProductTimeTaken();
			String maxTimetaken = maxTimeTaken();
			String avgTimeTaken = avgTimeTaken();

			Party asparty = (Party)entity;
			Party.Insights insight = new Party.Insights();
			PaymentBehaviour pay = new PaymentBehaviour();
			pay.setKey("1");
			pay.setAvgPaymentTime(avgTimeTaken);
			pay.setMaxPaymentTime(maxTimetaken);
			pay.setTotalRevenue(Revenue);
			pay.setMaxTimePaymentProduct(productName);


			Collection<PaymentBehaviour> coll = new ArrayList<PaymentBehaviour>();
			coll.add(pay);
			insight.setPaymentBehaviour(coll);
			asparty.setInsights(insight);

			logger.debug("asparty : " + asparty);

			return asparty;
		}
		return null;
	}

	/**
	 * @return
	 * Function to get maximum time
	 */
	public String maxTimeTaken() {
		if(TimeTaken.size() != 0) {
			long time = Collections.max(TimeTaken);
			logger.debug("Maximum element : " + time);
			logger.debug("max time : " + time);
			String stime = standardTime(time);
			return stime;
		}
		return "not available";
	}

	/**
	 * @return
	 * Function to get average time
	 */
	public String avgTimeTaken() {

		if(TimeTaken.size() != 0) {
			long avg = (long) 0;
			int size = TimeTaken.size();

			for(int i = 0; i < size; i++) {
				avg += TimeTaken.get(i);
			}
			if(avg > 0) {
				avg = avg / size;
			}
			logger.debug("avg : " + avg);
			String time = standardTime(avg);

			return time;
		}
		return "not available";
	}

	/**
	 * @param milliseconds
	 * @return
	 * Function to convert milliseconds to days and weeks
	 */
	public String standardTime(long milliseconds) {

		long hrs = TimeUnit.MILLISECONDS.toHours(milliseconds);
		String time = "";

		long days = 0;
		long week = 0;

		if(hrs >= 24) {
			days = hrs /24;
		}
		if(days >= 7) {
			week = days / 7;
			days = days % 7;
		}

		if(days >= 5) {
			week = week+1;
			days = 0;
		}

		if(week != 0) {
			time = week + " week";
		}
		else {
			if(days != 0) {
				time = days + " days";
			}
			else {
				time = "1 day";
			}
		}

		return time;
	}


	/**
	 * @param productName
	 * @param millisec
	 * Function to populate product-key, TotalTimeTaken-value map
	 */
	public void populateMap(String productName, Long millisec) {

		if(ProductTimeTaken.containsKey(productName) == true) {
			Long time = ProductTimeTaken.get(productName);
			Long timevalue = time + millisec;
			ProductTimeTaken.put(productName, timevalue);
		}
		else {
			ProductTimeTaken.put(productName, millisec);
		}
		logger.debug("productTimeTaken : " + ProductTimeTaken);
	}

	/**
	 * @return
	 * Function to get product name which took maximum time for payment 
	 */
	public String maxProductTimeTaken() {

		String maxproductName = null;
		Long maxtime = (long) 0;

		if(ProductTimeTaken.size() == 0) {
			Iterator iter = ProductTimeTaken.entrySet().iterator();
			while (iter.hasNext()) { 
				Map.Entry mapelement = (Map.Entry)iter.next(); 
				if((long)mapelement.getValue() > (long)maxtime) {
					maxtime = (Long) mapelement.getValue();
					maxproductName = (String) mapelement.getKey();
				}	 
			}
			logger.debug("maxproductName : " + maxproductName);
			return maxproductName;	
		}
		return "not applicable";
	}

	@Override
	public String standardize(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
