/**
 * 
 */
package com.account360i.postenrichment;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.allsight.Party;
import com.allsight.Party.CRM;
import com.allsight.Party.DealBehaviourProduct;
import com.allsight.enrichment.common.EnrichmentFunction;
import com.allsight.entity.impl.Entity;

/**
 * @author Kumar
 *
 */
@SuppressWarnings("deprecation")
public class DealBehaviorProduct  extends EnrichmentFunction{

	private static final Logger logger =  Logger.getLogger(DealBehaviorProduct.class);


	public Object applyEnrichment(Entity<?> entity) throws Exception {
		// TODO Auto-generated method stub

		if (!getExecutionContext().isEIDRecord()) {
			logger.warn("This enrichment expects an EID record. Skipping enrichment");
			return null;
		} 


		Party asparty = dealBehaviorProduct(entity);

		logger.debug("asparty :: " + asparty);

		return asparty;
	}

	/**
	 * 
	 * To derive the DealBehaviorProduct of EID record 
	 * @param asParty
	 * @return
	 * @throws Exception
	 */
	public Party dealBehaviorProduct(Entity<?> asParty) throws Exception {

		if(asParty instanceof Party) {

			logger.debug("Enrichment to find DealBehaviorProduct");

			ArrayList<String> productNames = new ArrayList<String>();
			Party asparty = (Party)asParty;	

			//logger.debug("party before enrichment : "  + asParty);

			if (((Party) asParty).getCRMS()!= null && ((Party) asParty).getCRMS().getCRM() != null) {

				Collection<CRM> crmBoCollection = ((Party) asParty).getCRMS().getCRM();
				//logger.debug(" crmBoCollection :  "  + crmBoCollection);
				for ( CRM crmBo : crmBoCollection ) {
					if(crmBo.getProductName()!= null) {
						productNames.add(crmBo.getProductName().toUpperCase().toString());
					}
				}

				logger.debug(" productNames :  "  + productNames);
				/*To iterate over the unique products */
				Set<String> uniqueProductNames = new HashSet<String>(productNames);	 

				logger.debug(" uniqueProductNames :   "  + uniqueProductNames);
				Party.Insights insight = new Party.Insights();
				Collection<DealBehaviourProduct> dealBehaviorProductColl = new ArrayList<DealBehaviourProduct>();

				for (String uniquePrds : uniqueProductNames ) {
					//logger.debug(" uniquePrds :   "  + uniquePrds);
					String productId = "";
					List<Long> timeDurations = new ArrayList<Long>();

					for ( CRM crmBo : crmBoCollection ) {


						if(crmBo.getProductName()!= null && crmBo.getProductName().equalsIgnoreCase(uniquePrds)) {
							productId = crmBo.getProductID();
							logger.debug(" crmBo.getProductName() :   "  + crmBo.getProductName().toString());
							if(crmBo.getDealStartDate()!= null && crmBo.getDealEndDate()!=null) { 

								SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
								Date dealStartDate = dateFormat.parse(crmBo.getDealStartDate());
								Date dealEndDate = dateFormat.parse(crmBo.getDealEndDate());
								Timestamp startTimestamp = new java.sql.Timestamp(dealStartDate.getTime());
								Timestamp endTimeStamp = new java.sql.Timestamp(dealEndDate.getTime());
								long diffInMilliSec = endTimeStamp.getTime() -  startTimestamp.getTime() ;	
								timeDurations.add(diffInMilliSec);
								logger.debug("timeDurations" + timeDurations);

							}

						}
					}

					String maxDealTimeStr = maxDealTime(timeDurations);
					String minDealTimeStr = minDealTime(timeDurations);
					String noOfDeals = numberOfDeals(timeDurations); 
					logger.debug(" noOfDeals :   "  + noOfDeals);
					String avgDealClosureTimeStr = avgDealClosureTime(timeDurations);
					logger.debug(" avgDealClosureTimeStr :   "  + avgDealClosureTimeStr);
					Party.DealBehaviourProduct dealBehaviorProduct= new Party.DealBehaviourProduct();
					dealBehaviorProduct.setKey(uniquePrds+" "+productId); //* Setting the unique Key by combining Product Name and Product ID*//
					logger.debug(" key :   "  + dealBehaviorProduct.getKey().toString());
					dealBehaviorProduct.setProductName(uniquePrds);
					dealBehaviorProduct.setProductID(productId);
					dealBehaviorProduct.setNumberOfDeals(noOfDeals);
					dealBehaviorProduct.setAvgDealTime(avgDealClosureTimeStr);
					dealBehaviorProduct.setMaxDealTime(maxDealTimeStr);
					dealBehaviorProduct.setMinDealTime(minDealTimeStr);
					/*Added the this condition not to populate the dealBehavior BO itself if the no Of Deals and avgDealClosureTimeStr is null */
					if(noOfDeals!= null && avgDealClosureTimeStr!= null)
					{
					dealBehaviorProductColl.add(dealBehaviorProduct);
					logger.debug(" dealBehaviorProductColl :   "  + dealBehaviorProductColl);
					}
				}
				insight.setDealBehaviourProduct(dealBehaviorProductColl);
				logger.debug("insight" + insight.toString() );

				if(asparty.getInsights() == null)
					asparty.setInsights(insight);

				else
					asparty.getInsights().setDealBehaviourProduct(dealBehaviorProductColl);

				logger.debug("asparty : " + asparty);

			}
			logger.debug("asparty before return : " + asparty);
			return asparty;
		}
		return null;
	}

	/**
	 * 
	 * Minimum Deal Time Duration for a Product 
	 * @param timeDurations
	 * @return
	 */
	private String minDealTime(List<Long> timeDurations) {
		if(timeDurations.size() != 0) {
			long time = Collections.min(timeDurations);
			//logger.debug("max time in long : " + time);
			String timeInStandard = timeStandard(time);
			logger.debug("min Deal time in days/week  : " + time);
			return timeInStandard;
		}
		return null;
		
	}

	/**
	 * 
	 * Maximum Deal Time Duration for a Product
	 * @param timeDurations
	 * @return
	 */
	private String maxDealTime(List<Long> timeDurations) {
		if(timeDurations.size() != 0) {
			long time = Collections.max(timeDurations);
			//logger.debug("max time in long : " + time);
			String timeInStandard = timeStandard(time);
			logger.debug("max Deal time in days/week  : " + time);
			return timeInStandard;
		}
		return null;

	}

	
	/**
	 * Calculate the number of deals
	 * @param timeDurations
	 * @return
	 */
	private String numberOfDeals(List<Long> timeDurations) {

		if(timeDurations.size()!=0) {
			String numberOfDeals = Integer.toString(timeDurations.size()); 
			logger.debug(" numberOfDeals :   "  + numberOfDeals);
			return numberOfDeals;
		}
		else
		{
			return null;
		}
	}

	/**
	 * To calculate the average DealClosure Time for the product
	 * @param prdName
	 * @param timeDuration
	 * @return
	 */
	private String avgDealClosureTime(List<Long> timeDuration) {		
		if(timeDuration.size() != 0) {
			long average = (long) 0; 
			for(int i = 0; i < timeDuration.size(); i++) {
				average += timeDuration.get(i);
			}
			if(average > 0) {
				average = average / timeDuration.size();

			}
			String time = timeStandard(average);
			logger.debug(" time :   "  + time);
			return time;
		}
		return null;
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
			time = week + " weeks";
		}
		else if (week == 1) {
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
		logger.debug("time in timeStandard:   " + time);
		return time;	

	}

	@Override
	public String standardize(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}
}