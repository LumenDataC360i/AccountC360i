package com.account360i.postenrichment;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.allsight.Party;
import com.allsight.Party.InteractionBehaviour;
import com.allsight.Party.InteractionBehaviourSummary;
import com.allsight.enrichment.common.EnrichmentFunction;
import com.allsight.entity.impl.Entity;


/**
 * @author Aradhana Pandey
 * Enrichment to show interaction summary
 */
@SuppressWarnings("deprecation")
public class InteractionSummary extends EnrichmentFunction {

	private static final Logger logger = Logger.getLogger(InteractionSummary.class);
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
		Collection<InteractionBehaviourSummary> summaryColl = new ArrayList<>();

		
		if (((Party) entity).getInsights() != null && ((Party) entity).getInsights().getInteractionBehaviour()!= null) {
			Collection<InteractionBehaviour> interactionBehaviour = ((Party) entity).getInsights().getInteractionBehaviour();
		
			InteractionBehaviour element = interactionBehaviour.iterator().next();
			summary = "a) Total number of interaction(s)         : " + element.getTotalInteractions();
			SUMMARYLIST.add(summary);
			
			summary = "b) Total number of email interaction(s)   : " + element.getTotalEmailInteractions();
			SUMMARYLIST.add(summary);
			
			summary = "c) Total number of webchat interaction(s) : " + element.getTotalWebchatInteractions();
			SUMMARYLIST.add(summary);
			
			summary = "d) Preferred Medium                       : " + element.getPreferredMedium();
			SUMMARYLIST.add(summary);
			
			summary = "e) Most Talked Product                    : " + element.getMostTalkedProduct();
			SUMMARYLIST.add(summary);
		}	
		
		if(!SUMMARYLIST.isEmpty()) {
			for(String str : SUMMARYLIST) {

				if(str.isEmpty())
					continue;

				InteractionBehaviourSummary isummary= new InteractionBehaviourSummary();
				isummary.setInteractionSummary(str);
				isummary.setKey(key.toString() + "_summary");

				logger.info(isummary.getKey() + ": "+ str);
				key+=1;
				summaryColl.add(isummary);
			}
		}

		party.getInsights().setInteractionBehaviourSummary(summaryColl);
		return party;
	}

	@Override
	public String standardize(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
