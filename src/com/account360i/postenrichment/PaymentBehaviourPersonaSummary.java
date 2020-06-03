package com.account360i.postenrichment;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.apache.log4j.Logger;

import com.allsight.Party;
import com.allsight.Party.PaymentBehaviour;
import com.allsight.Party.PaymentBehaviourSummary;
import com.allsight.enrichment.common.EnrichmentFunction;
import com.allsight.entity.impl.Entity;

/**
 * @author Aradhana Pandey
 * Post Enrichment to show payment summary
 */
public class PaymentBehaviourPersonaSummary extends EnrichmentFunction {

	private static final Logger logger = Logger.getLogger(PaymentBehaviourPersonaSummary.class);
	private Collection<String> SUMMARYLIST = new ArrayList<>();
	Integer key = 1;
	
	@Override
	public Object applyEnrichment(Entity<?> entity) throws Exception {
		// TODO Auto-generated method stub
		
		if (!getExecutionContext().isEIDRecord()) {
			logger.warn("This enrichment expects an EID record. Skipping enrichment");
			return null;
		} 
		Party asparty = summaryExtraction(entity);
		logger.debug("asparty :: " + asparty);
		return asparty;
	}

	private Party summaryExtraction(Entity<?> entity) {
		// TODO Auto-generated method stub
		
		String summary = new String();
		Party party = (Party) entity;
		Collection<PaymentBehaviourSummary> summaryColl = new ArrayList<>();

		
		if (((Party) entity).getInsights() != null && ((Party) entity).getInsights().getPaymentBehaviour()!= null) {
			Collection<PaymentBehaviour> paymentBehaviour = ((Party) entity).getInsights().getPaymentBehaviour();
		
			PaymentBehaviour element = paymentBehaviour.iterator().next();
			summary = "a) Avg Time for payment         : " + element.getAvgPaymentTime();
			SUMMARYLIST.add(summary);
			
			summary = "b) Max time taken for payment is " + element.getMaxPaymentTime() + " for " + element.getMaxTimePaymentProduct();
			SUMMARYLIST.add(summary);
			
			summary = "c) Total Revenue (USD)         		: " + NumberFormat.getNumberInstance(Locale.US).format(element.getTotalRevenue());
			SUMMARYLIST.add(summary);
		}	
		
		if(!SUMMARYLIST.isEmpty()) {
			for(String str : SUMMARYLIST) {

				if(str.isEmpty())
					continue;

				PaymentBehaviourSummary isummary= new PaymentBehaviourSummary();
				isummary.setPaymentSummary(str);
				isummary.setKey(key.toString() + "_summary");

				logger.info(isummary.getKey() + ": "+ str);
				key+=1;
				summaryColl.add(isummary);
			}
		}

		party.getInsights().setPaymentBehaviourSummary(summaryColl);
		return party;
	}

	@Override
	public String standardize(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
