/**
 * 
 */
package com.account360i.postenrichment;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import com.allsight.Party;
import com.allsight.Party.CRM;
import com.allsight.Party.DealBehaviour;
import com.allsight.Party.Transaction;
import com.allsight.enrichment.common.EnrichmentFunction;
import com.allsight.entity.impl.Entity;


/**
 * @author Kumar
 *
 */
@SuppressWarnings("deprecation")
public class DealBehavior extends EnrichmentFunction{

	private static final Logger logger =  Logger.getLogger(DealBehavior.class);
	List<Long> timeDurations = new ArrayList<Long>();
	private Map<String,Long> productTimeTaken = new HashMap<String,Long>();
	private Collection<String> productList = new ArrayList<>();

	public Object applyEnrichment(Entity<?> entity) throws Exception {
		// TODO Auto-generated method stub
		if (!getExecutionContext().isEIDRecord()) {
			logger.warn("This enrichment expects an EID record. Skipping enrichment");
			return null;
		} 


		Party asparty = dealBehavior(entity);

		//logger.debug("asparty :: " + asparty);

		return asparty;
	}

	/**
	 * 
	 * To Derive the Deal Behavior Insights
	 * @param asParty
	 * @return
	 * @throws Exception
	 */
	public Party dealBehavior(Entity<?> asParty) throws Exception {

		if(asParty instanceof Party) {

			logger.debug("Enrichment to find DealBehavior");

			//logger.debug("party before enrichment : "  + asParty);

			if (((Party) asParty).getCRMS()!= null && ((Party) asParty).getCRMS().getCRM() != null) {

				Collection<CRM> crmBoCollection = ((Party) asParty).getCRMS().getCRM();
				//logger.debug("crmBoCollection: "  + crmBoCollection);

				for (CRM crmBo   :crmBoCollection) {

					if(crmBo.getProductName()!=null) 
					{

						if(crmBo.getDealStartDate()!= null && crmBo.getDealEndDate()!=null) {

							SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
							//logger.debug("dateFormat: "  + dateFormat);
							Date dealStartDate = dateFormat.parse(crmBo.getDealStartDate());
							Date dealEndDate = dateFormat.parse(crmBo.getDealEndDate());
							Timestamp starTimestamp = new java.sql.Timestamp(dealStartDate.getTime());
							Timestamp endTimeStamp = new java.sql.Timestamp(dealEndDate.getTime());
							long diffInMilliSec = endTimeStamp.getTime() -  starTimestamp.getTime() ;
							//logger.debug("diffInMilliSec: "  + diffInMilliSec);
							timeDurations.add(diffInMilliSec);
							//logger.debug("timeDurations: "  + timeDurations);
							String productName = crmBo.getProductName().toLowerCase().trim();
							populateMap(productName,diffInMilliSec);

						}

					}

				}

			}

			String[] mostAndLeastOrderedProductStr = mostAndLeastOrderedProduct(asParty).split(":");
			String mostOrderedProduct = mostAndLeastOrderedProductStr[0];
			logger.debug("mostOrderedProduct: "  + mostOrderedProduct);
			String leastOrderedProduct = mostAndLeastOrderedProductStr[1];
			logger.debug("leastOrderedProduct: "  + leastOrderedProduct);
			String avgDealClosureTimeStr =  avgDealClosureTime();
			logger.debug("avgDealClosureTimeStr: "  + avgDealClosureTimeStr);
			String maxTimeDurationStr = maxTimeDuration();
			logger.debug("maxTimeDurationStr: "  + maxTimeDurationStr);
			String maxTimeTakenProductStr = maxProductTimeTaken();
			logger.debug("maxTimeTakenProductStr: "  + maxTimeTakenProductStr);


			Party asparty = (Party)asParty;
			Party.Insights insight = new Party.Insights();
			Party.DealBehaviour dealBehaviour= new Party.DealBehaviour();
			dealBehaviour.setKey("1");
			dealBehaviour.setAverageDealClosureTime(avgDealClosureTimeStr);
			dealBehaviour.setMaxTimeDeal(maxTimeDurationStr);
			dealBehaviour.setMostlyOrderedProduct(mostOrderedProduct);
			dealBehaviour.setLeastOrderedProduct(leastOrderedProduct);
			dealBehaviour.setMaxTimeDeal(maxTimeDurationStr);
			dealBehaviour.setProductMaxDealTime(maxTimeTakenProductStr);
			//logger.debug("dealBehaviour: "  + dealBehaviour);

			Collection<DealBehaviour> coll = new ArrayList<DealBehaviour>();
			coll.add(dealBehaviour);
			//logger.debug("coll: "  + coll);
			insight.setDealBehaviour(coll);
			//logger.debug("insight  "  + insight);


			if(asparty.getInsights() == null)
				asparty.setInsights(insight);

			else
				asparty.getInsights().setDealBehaviour(coll);

			logger.debug("asparty : " + asparty);


			return asparty;
		}

		return null;

	}


	/**
	 * To find the most and least ordered product
	 *
	/*
	 * @param asParty
	 * @return
	 */
	private String mostAndLeastOrderedProduct(Entity<?> asParty) {
		Map<String, Integer> productNameCountMap = new HashMap<>(); 
		if (((Party) asParty).getTransactions() != null && ((Party) asParty).getTransactions().getTransaction()!= null) {
			Collection<Transaction> transactions = ((Party) asParty).getTransactions().getTransaction();
			//logger.debug("Transaction collection : " + transactions);

			for (Transaction transaction : transactions) {
				String productName = transaction.getProductName();
				productList.add(productName);				

			} 
			//logger.debug("productList : " + productList);
			// Traverse through array elements and 
			// count frequencies 
			for(String prdName : productList )
			{ 
				if (productNameCountMap.containsKey(prdName))  
				{ 
					productNameCountMap.put(prdName, productNameCountMap.get(prdName) + 1); 
				}  
				else
				{ 
					productNameCountMap.put(prdName, 1); 
				} 
			}
			//return (sortByCountValues(productNameCountMap));

			logger.debug("productNameCountMap : " + productNameCountMap);

		}
		return (sortByCountValues(productNameCountMap));
	}


	/**
	 * To sort the productNameCountMap and find out the most and least ordered product
	 * @param productNameCountMap
	 * @return
	 */
	private String sortByCountValues(Map<String, Integer> productNameCountMap) {

		Set<Map.Entry<String, Integer> > prdCountSet = productNameCountMap.entrySet(); 
		String prdWithMaxCount = ""; 
		String prdWithMinCount="";
		int count = 0; 
		int valueHighest =0;


		for (Map.Entry<String, Integer> prdName : prdCountSet) { 
			// Check for word having highest frequency 
			if (prdName.getValue() > count) { 
				//logger.debug(" me.getValue()   "+me.getValue());
				//logger.debug("value " + value);
				count = prdName.getValue(); 
				prdWithMaxCount = prdName.getKey(); 
			} 

		} 
		valueHighest = count;

		for (Map.Entry<String, Integer> prdName : prdCountSet) { 
			// Check for word having least frequency 
			if (prdName.getValue() < valueHighest ) { 
				//logger.debug("valueHighest  : " + valueHighest);
				//logger.debug("value " + count);
				//logger.debug(count);
				valueHighest = prdName.getValue(); 
				prdWithMinCount = prdName.getKey(); 
			} 
		} 

		// Return word having highest and lowest frequency 
		logger.debug("max+min key   " + prdWithMaxCount+":"+prdWithMinCount );
		return prdWithMaxCount+":"+prdWithMinCount; 
	} 		        



	/**
	 * Return the average Deal closure time for a deal 
	 */
	private String avgDealClosureTime() {

		if(timeDurations.size() != 0) {
			long average = (long) 0; 
			for(int i = 0; i < timeDurations.size(); i++) {
				average += timeDurations.get(i);
			}
			if(average > 0) {
				average = average / timeDurations.size();
			}
			String time = timeStandard(average);

			return time;
		}
		return "not available";
	}


	/**
	 * 
	 * Maximum Time Duration for a deal 
	 * @return
	 */
	private String maxTimeDuration() {

		if(timeDurations.size() != 0) {
			long time = Collections.max(timeDurations);
			//logger.debug("max time in long : " + time);
			String timeInStandard = timeStandard(time);
			logger.debug("max time in days/week  : " + time);
			return timeInStandard;
		}
		return "not available";
	}



	/**
	 * Convert the milli seconds to days, Weeks
	 * 
	 * @param average
	 * @return 
	 */
	private String timeStandard(long average) {
		long hrs = TimeUnit.MILLISECONDS.toHours(average);
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
			time = week + " week(s)";
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

		if(productTimeTaken.containsKey(productName) == true) {
			Long time = productTimeTaken.get(productName);
			if(time < millisec ) {
				productTimeTaken.put(productName, millisec);
			}
			else
			{
				productTimeTaken.put(productName, time);
			}
		}
		else 
		{
			productTimeTaken.put(productName, millisec);
		}
		logger.debug("productTimeTaken : " + productTimeTaken);
	}

	/**
	 * @return
	 * Function to get product name which took maximum time for Deal Sign off 
	 */
	@SuppressWarnings("rawtypes")
	public String maxProductTimeTaken() {

		String maxproductName = null;
		Long maxtime = (long) 0;

		if( productTimeTaken.size() != 0) {
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
		return "not applicable";
	}

	@Override
	public String standardize(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
