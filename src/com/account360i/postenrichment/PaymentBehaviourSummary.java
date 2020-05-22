package com.account360i.postenrichment;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.allsight.Party;
import com.allsight.Party.PaymentBehaviour;
import com.allsight.Party.Transaction;
import com.allsight.enrichment.common.EnrichmentFunction;
import com.allsight.entity.impl.Entity;


public class PaymentBehaviourSummary extends EnrichmentFunction {
	private static final Logger logger = Logger.getLogger(PaymentBehaviourSummary.class);
	private List<Long> timeTaken = new ArrayList<Long>();
	private Double revenue;
	private Map<String,Long> productTimeTaken = new HashMap<String,Long>();
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
	 */
	public Party paymentSummary(Entity<?> entity) {
		// TODO Auto-generated method stub
		
		logger.debug("Eid Party : " + entity);
		
		if (((Party) entity).getTransactions() != null && ((Party) entity).getTransactions().getTransaction()!= null) {
			
			Collection<Transaction> transactions = ((Party) entity).getTransactions().getTransaction();
			
			logger.debug("Transaction collction : " + transactions);
			
			for (Transaction transaction : transactions) {
				
				Timestamp endtime = transaction .getEndTimestamp();
				Timestamp starttime = transaction.getStartTimestamp();
				//Timestamp end = Timestamp.valueOf("2020-07-31 14:12:01");
				long milliseconds = endtime.getTime() - starttime.getTime();
				//long milliseconds = end.getTime() - starttime.getTime();
				Long millisec = new Long(milliseconds);
				logger.debug("diff : " + millisec);
				timeTaken.add(millisec);
				
				String productName = transaction.getProductName().toLowerCase().trim();
				populateMap(productName,millisec);
				
				revenue += transaction.getTransactionAmountDouble();  // total revenue for an account
			}
			logger.debug("productTimeTaken : " + productTimeTaken);
			logger.debug("timeTaken : " + timeTaken);
			
			String productName = maxProductTimeTaken();
			String maxTimetaken = maxTimeTaken();
			String avgTimeTaken = avgTimeTaken();
			
			Party asparty = (Party)entity;
			Party.Insights insight = new Party.Insights();
			PaymentBehaviour pay = new PaymentBehaviour();
			pay.setKey("1");
			pay.setAvgPaymentTime(avgTimeTaken);
			pay.setMaxPaymentTime(maxTimetaken);
			pay.setTotalRevenue(revenue);
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
	
	public String maxTimeTaken() {
		
		long time = Collections.max(timeTaken);
		logger.debug("Maximum element : " + time);
		logger.debug("max time : " + time);
		String stime = standardTime(time);
		return stime;
	}
	
	public String avgTimeTaken() {
		
		long avg = (long) 0;
		int size = timeTaken.size();
		
		for(int i = 0; i < size; i++) {
			avg += timeTaken.get(i);
		}
	    if(avg > 0) {
	    	avg = avg / size;
	    }
	    
	    logger.debug("avg : " + avg);
	    String time = standardTime(avg);
	    
		return time;
	}
	
	public String standardTime(long milliseconds) {
		
		int seconds = (int) milliseconds / 1000;
		int hours = seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		seconds = (seconds % 3600) % 60;
		String time;
		
		if(hours != 0) {
			if(minutes != 0) {
				if(seconds != 0) {
					time = hours + "hrs " + minutes + "mins " + seconds + "secs";
				}
				else {
					time = hours + "hrs " + minutes + "mins ";
				}
			}
			else {
				if(seconds != 0) {
					time = hours + "hrs " + seconds + "secs";
				}
				else {
					time = hours + "hrs ";
				}
			}	
		}
		else {
			if(minutes != 0) {
				if(seconds != 0) {
					time = minutes + "mins " + seconds + "secs";
				}
				else {
					time = minutes + "mins ";
				}
			}
			else {
				if(seconds != 0) {
					time = seconds + "secs";
				}
				else {
					time = milliseconds + "millisecs";
				}
			}
		}
		logger.debug("time : " + time);
		return time;
	}
	
	public void populateMap(String productName, Long millisec) {
		
		if(productTimeTaken.containsKey(productName) == true) {
			Long time = productTimeTaken.get(productName);
			Long timevalue = time + millisec;
			
			productTimeTaken.put(productName, timevalue);
		}
		else {
			productTimeTaken.put(productName, millisec);
		}
		
		logger.debug("productTimeTaken : " + productTimeTaken);
	}
	
	public String maxProductTimeTaken() {
		
		String maxproductName = null;
		Long maxtime = (long) 0;
		
		Iterator iter = productTimeTaken.entrySet().iterator();
		
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

	@Override
	public String standardize(String arg0) {
		// TODO Auto-generated method stub
		
		
		return null;
	}
	
}
