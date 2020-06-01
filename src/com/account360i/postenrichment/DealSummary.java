/**
 * 
 */
package com.account360i.postenrichment;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.allsight.Party;
import com.allsight.Party.DealBehaviour;
import com.allsight.Party.DealBehaviourSummary;
import com.allsight.enrichment.common.EnrichmentFunction;
import com.allsight.entity.impl.Entity;

/**
 * @author Kumar
 *
 */
@SuppressWarnings("deprecation")
public class DealSummary extends EnrichmentFunction {

	private static final Logger logger = Logger.getLogger(DealBehaviourSummary.class);
	private Collection<String> SUMMARYLIST = new ArrayList<>();
	Integer key = 1;


	@Override
	public Object applyEnrichment(Entity<?> entity) throws Exception {
		// TODO Auto-generated method stub

		if (!getExecutionContext().isEIDRecord()) {
			logger.warn("This enrichment expects an EID record. Skipping enrichment");
			return null;
		} 
		Party asparty = dealSummaryExtraction(entity);
		logger.debug("asparty :: " + asparty);
		return asparty;
	}


	@SuppressWarnings("rawtypes")
	private Party dealSummaryExtraction(Entity entity) {

		
		String summary = new String();
		Party party = (Party) entity;
		Collection<DealBehaviourSummary> summaryColl = new ArrayList<>();

		if (((Party) entity).getInsights() != null && ((Party) entity).getInsights().getDealBehaviour()!= null) {
			Collection<DealBehaviour> dealBehaviour = ((Party) entity).getInsights().getDealBehaviour();

			DealBehaviour element = dealBehaviour.iterator().next();
			if ( element.getAverageDealClosureTime()!= null) {
			summary = "Avg Deal Closure Time : " + element.getAverageDealClosureTime();
			SUMMARYLIST.add(summary);
			}
			
			if(element.getMaxTimeDeal()!= null) {
			summary = "Maximum Time Taken For a Deal : " + element.getMaxTimeDeal();
			SUMMARYLIST.add(summary);
			}

			if(element.getProductMaxDealTime()!=null) {
			summary = "Deal Taken Most Time For : " + element.getProductMaxDealTime();
			SUMMARYLIST.add(summary);
			}
			
			if(element.getLeastOrderedProduct()!= null) {
			summary = "Least Ordered Product : " + element.getLeastOrderedProduct();
			SUMMARYLIST.add(summary);
			}

            if(element.getMostlyOrderedProduct()!=null) {			
			summary = "Most Ordered Product : " + element.getMostlyOrderedProduct();
			SUMMARYLIST.add(summary);
            }
		}	

		if(!SUMMARYLIST.isEmpty()) {
			for(String str : SUMMARYLIST) {

				if(str.isEmpty())
					continue;

				DealBehaviourSummary dealSummary = new DealBehaviourSummary();

				dealSummary.setDealSummary(str);
				dealSummary.setKey(key.toString() + "_summary");

				logger.info(dealSummary.getKey() + ": "+ str);
				key+=1;
				summaryColl.add(dealSummary);
			}
		}

		party.getInsights().setDealBehaviourSummary(summaryColl);
		return party;
	}


	@Override
	public String standardize(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}


	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
