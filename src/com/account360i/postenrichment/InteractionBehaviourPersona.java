package com.account360i.postenrichment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.allsight.Party;
import com.allsight.Party.EmailFromParty;
import com.allsight.Party.InteractionBehaviour;
import com.allsight.Party.ProductMention;
import com.allsight.Party.Webchat;
import com.allsight.enrichment.common.EnrichmentFunction;
import com.allsight.entity.impl.Entity;

/**
 * @author Aradhana Pandey
 * Eid post enrichment to get Interaction Behaviour
 *
 */
public class InteractionBehaviourPersona extends EnrichmentFunction {
	private static final Logger logger = Logger.getLogger(InteractionBehaviourPersona.class);
	private Map<String,Integer> ProductCountMap = new HashMap<String,Integer>();

	@Override
	public Object applyEnrichment(Entity<?> entity) throws Exception {
		// TODO Auto-generated method stub

		if (!getExecutionContext().isEIDRecord()) {
			logger.warn("This enrichment expects an EID record. Skipping enrichment");
			return null;
		} 

		Party asparty = interactionSummary(entity);
		logger.debug("asparty :: " + asparty);

		return asparty;
	}


	/**
	 * @param entity
	 * @return
	 * Function to interaction summary
	 */
	private Party interactionSummary(Entity<?> entity) {
		// TODO Auto-generated method stub
		Integer totalEmails = 0;
		Integer totalWebchats = 0;
		Integer totalInteractions = 0;
		String preferredMedium;

		if (((Party) entity).getInteractions() != null && ((Party) entity).getInteractions().getEmailFromParty()!= null) {
			Collection<EmailFromParty> emails = ((Party) entity).getInteractions().getEmailFromParty();
			totalEmails = emails.size();
		}
		if (((Party) entity).getInteractions() != null && ((Party) entity).getInteractions().getWebchat()!= null) {
			Collection<Webchat> webchats = ((Party) entity).getInteractions().getWebchat();
			totalWebchats = webchats.size();
		}

		if(totalEmails > totalWebchats) {
			preferredMedium = "Email";
		}
		else if(totalEmails < totalWebchats) {
			preferredMedium = "Webchat";
		}
		else if(totalEmails == totalWebchats && totalWebchats != 0){
			preferredMedium = "Email/Webchat";
		}
		else {
			preferredMedium = "no interactions";
		}

		totalInteractions = totalEmails + totalWebchats;

		//String sentiment = productSentimentAggregate(entity);
		String productName = mostTalkedProduct(entity);

		Party asparty = (Party)entity;
		Party.Insights insight = new Party.Insights();
		InteractionBehaviour interaction = new InteractionBehaviour();
		interaction.setKey("1");
		interaction.setPreferredMedium(preferredMedium);
		interaction.setTotalEmailInteractions(totalEmails.toString());
		interaction.setTotalWebchatInteractions(totalWebchats.toString());
		interaction.setTotalInteractions(totalInteractions.toString());
		if(productName != null) {
			interaction.setMostTalkedProduct(productName);
		}
		//interaction.setAvgInteractionProductSentiment(sentiment);
		Collection<InteractionBehaviour> coll = new ArrayList<InteractionBehaviour>();
		coll.add(interaction);
		insight.setInteractionBehaviour(coll);

		if(asparty.getInsights() == null)
			asparty.setInsights(insight);

		else
			asparty.getInsights().setInteractionBehaviour(coll);

		return asparty;
	}

	private String mostTalkedProduct(Entity<?> entity) {
		// TODO Auto-generated method stub
		
		if (((Party) entity).getSocialInteractions() != null && ((Party) entity).getSocialInteractions().getMentions()!= null && 
				((Party) entity).getSocialInteractions().getMentions().getProductMention()!= null) {
			Collection<ProductMention> mentions = ((Party) entity).getSocialInteractions().getMentions().getProductMention();

			for (ProductMention mention : mentions) {
				String pname = mention.getProductName().toUpperCase().trim();
				if(ProductCountMap.containsKey(pname)) {
					Integer count = ProductCountMap.get(pname);
					count = count + 1;
					ProductCountMap.put(pname, count);
				}
				else {
					ProductCountMap.put(pname, 1);
				}	
			}
			if(ProductCountMap.size() == 0) {    // if no mentions having product names then go for product category
				for (ProductMention mention : mentions) {
					String pcategory = mention.getProductCategory().toUpperCase();
					if(ProductCountMap.containsKey(pcategory)) {
						Integer count = ProductCountMap.get(pcategory);
						count = count + 1;
						ProductCountMap.put(pcategory, count);
					}
					else {
						ProductCountMap.put(pcategory, 1);
					}	
				}
			}
		}
		
		int maxCount = 0;
		String name = null;
		if(ProductCountMap.size() != 0) {
			Iterator iter = ProductCountMap.entrySet().iterator();
			while (iter.hasNext()) { 
				Map.Entry mapelement = (Map.Entry)iter.next();
				Integer count = (Integer)mapelement.getValue();
				if(count.intValue() > maxCount) {
					maxCount = count.intValue();
					name = (String) mapelement.getKey();
				}
			}
		}
		logger.debug("ProductCountMap : " + ProductCountMap);
		
		return name;
	}

	

	/**
	 * @param entity
	 * @return
	 * Function to calculate product sentiment aggregate
	 */
	/*public String productSentimentAggregate(Entity entity) {

		String finalSentiment = "";
		int positiveCount = 0;
		int negativeCount = 0;
		int neutralCount = 0;

		if (((Party) entity).getSocialInteractions() != null && ((Party) entity).getSocialInteractions().getMentions()!= null && 
				((Party) entity).getSocialInteractions().getMentions().getProductMention()!= null) {
			Collection<ProductMention> mentions = ((Party) entity).getSocialInteractions().getMentions().getProductMention();

			for (ProductMention mention : mentions) {

				String sentiment = mention.getSentiment();
				if(sentiment.equalsIgnoreCase("positive")) {
					positiveCount++;
				}
				else if(sentiment.equalsIgnoreCase("negative")) {
					negativeCount++;
				}
				else {
					neutralCount++;
				}
			}
		}

		if(positiveCount >= negativeCount && positiveCount >= neutralCount) {
			finalSentiment = "Positive";
		}
		else if(negativeCount >= neutralCount && negativeCount> positiveCount) {
			finalSentiment = "Negative";
		}
		else if(neutralCount >= negativeCount && neutralCount > positiveCount) {
			finalSentiment = "Neutral";
		}

		if(positiveCount == negativeCount && positiveCount == neutralCount && positiveCount == 0) {
			finalSentiment = "no sentiments recorded";
		}

		logger.debug("sentiment : " + finalSentiment);
		return finalSentiment;
	}*/

	@Override
	public String standardize(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}


}
