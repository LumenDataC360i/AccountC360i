package com.account360i.postenrichment;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;
import com.allsight.Party;
import com.allsight.Party.AccountSummary;
import com.allsight.Party.CRM;
import com.allsight.Party.DealBehaviour;
import com.allsight.Party.FinancialPotentialSummary;
import com.allsight.Party.OrganizationName;
import com.allsight.enrichment.common.EnrichmentFunction;
import com.allsight.entity.impl.Entity;

/**
 * EID post enrichment to populate financial summary details for sales perspective
 * @author Garima Jain
 *
 */
@SuppressWarnings("deprecation")
public class FinancialPotentialSummarySales extends EnrichmentFunction {

	private static final Logger logger = Logger.getLogger(FinancialPotentialSummarySales.class);

	private String ORGNAME = new String();
	private Collection<String> SUMMARYLIST = new ArrayList<>();
	Integer key = 1;
	@Override
	public Object applyEnrichment(Entity<?> entity) throws Exception {
		Party party = (Party) entity;
		String summary = new String(); 
		Collection<FinancialPotentialSummary> summaryColl = new ArrayList<>();
		Collection<AccountSummary> asummaryColl;

		if(party.getInsights().getAccountSummary() != null)
			asummaryColl = party.getInsights().getAccountSummary();

		else
			asummaryColl = new ArrayList<>();

		logger.info("Deriving Account summary");

		if(party.getDemographics() != null && party.getDemographics().getOrganizationName() != null) {

			//get Organization Name
			for (OrganizationName orgName : party.getDemographics().getOrganizationName()) {
				if(orgName.getSourceName().equalsIgnoreCase("customerprofile") && orgName.getName() != null) {
					ORGNAME = orgName.getName();
				}
			}
		}

		summary = getMostOrders(party);
		SUMMARYLIST.add(summary);

		summary = getFinanceInfo(party);
		SUMMARYLIST.add(summary);

		logger.info("Summary collection: " + SUMMARYLIST);

		if(!SUMMARYLIST.isEmpty()) {
			for(String str : SUMMARYLIST) {

				if(str.isEmpty())
					continue;

				FinancialPotentialSummary fSummary = new FinancialPotentialSummary();
				fSummary.setFinancialSummary(str);
				fSummary.setKey(key.toString() + ".");

				AccountSummary aSummary = new AccountSummary();
				aSummary.setSummary(str);
				aSummary.setKey(key.toString() + ".");


				logger.info(fSummary.getKey() + ": "+ str);
				key+=1;
				summaryColl.add(fSummary);
				asummaryColl.add(aSummary);
			}
		}

		party.getInsights().setFinancialPotentialSummary(summaryColl);
		party.getInsights().setAccountSummary(asummaryColl);
		return party;

	}

	/**
	 * Get the mostly ordered product
	 * @param party
	 * @return
	 */
	private String getMostOrders(Party party) {

		String str = new String();

		if(party.getInsights() != null && party.getInsights().getDealBehaviour() != null) {

			for(DealBehaviour deal : party.getInsights().getDealBehaviour()) {
				if(deal.getMostlyOrderedProduct() != null) {
					str = "We have mostly sold " + deal.getMostlyOrderedProduct() + " to " + ORGNAME;
				}
			}
		}

		return str;
	}

	/**
	 * Get Finance Summary
	 * @param party
	 * @return
	 */
	private String getFinanceInfo(Party party) {
		StringBuilder str = new StringBuilder();

		Double minAmt = 999999.0;
		Double maxAmt = 0.0;
		Double totalAmount = 0.0;
		int totalDeals = 0;

		if(party.getCRMS() != null && party.getCRMS().getCRM() != null) {

			for(CRM crm : party.getCRMS().getCRM()) {
				if("done".equalsIgnoreCase(crm.getDealStatus())) {

					Double estimateAmount = crm.getEstimatedDealAmount();
					//set the max deal amount
					if(estimateAmount != null && Double.compare(estimateAmount, maxAmt) > 0 )
						maxAmt = estimateAmount;

					//set minimum deal amount
					if(estimateAmount != null && Double.compare(estimateAmount, minAmt) < 0 )
						minAmt = estimateAmount;

					totalDeals ++;

					if(estimateAmount != null)
						totalAmount += estimateAmount;
				}
			}
		}

		else {
			str.append("No finance information available");
			return str.toString();
		}

		Double avgAmount = totalAmount / totalDeals;

		str.append(ORGNAME + " signed an order of an average amount of $" + avgAmount + "/-.");
		str.append(" And the amount range in which it has placed orders is $" + minAmt + "-$" + maxAmt + ".");

		return str.toString();
	}


	@Override
	public String standardize(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
