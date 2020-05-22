package com.account360i.postenrichment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import org.apache.log4j.Logger;

import com.allsight.Party;
import com.allsight.Party.AccountSummary;
import com.allsight.Party.InteractionBehaviour;
import com.allsight.Party.Organization;
import com.allsight.Party.OrganizationName;
import com.allsight.Party.PaymentBehaviour;
import com.allsight.enrichment.common.EnrichmentFunction;
import com.allsight.entity.impl.Entity;

/**
 * EID post enrichment to populate various account summary details for sales perspective
 * @author Garima Jain
 *
 */
@SuppressWarnings("deprecation")
public class AccountSummarySales extends EnrichmentFunction {
	private static final Logger logger = Logger.getLogger(AccountSummarySales.class);

	private String ORGNAME = new String();
	private Collection<String> SUMMARYLIST = new ArrayList<>();
	Integer key = 1;

	@Override
	public Object applyEnrichment(Entity<?> entity) throws Exception {

		Party party = (Party) entity;
		String summary = new String(); 
		AccountSummary aSummary = new AccountSummary();
		Collection<AccountSummary> summaryColl = new ArrayList<>();

		logger.info("Deriving Account summary");

		if(party.getDemographics() != null && party.getDemographics().getOrganizationName() != null) {

			//get Organization Name
			for (OrganizationName orgName : party.getDemographics().getOrganizationName()) {
				if(orgName.getSourceName().equalsIgnoreCase("customerprofile") && orgName.getName() != null) {
					ORGNAME = orgName.getName();
				}
			}
		}

		summary = getOldNewValue(party);
		logger.info("summary 1: " + summary);
		SUMMARYLIST.add(summary);

		summary = getImportance(party);
		SUMMARYLIST.add(summary);
		logger.info("summary 2: " + summary);
		
		summary = getEngagementLevel(party);
		SUMMARYLIST.add(summary);
		logger.info("summary 3: " + summary);

		if(!SUMMARYLIST.isEmpty()) {
			for(String str : SUMMARYLIST) {
				aSummary.setSummary(str);
				aSummary.setKey(key.toString() + "_summary");
				key++;

				summaryColl.add(aSummary);
			}
		}

		party.getInsights().setAccountSummary(summaryColl);
		return party;
	}

	/**
	 * To get the age (old/new) of the account
	 * @return
	 * @throws ParseException
	 */
	private String getOldNewValue(Party party) throws ParseException {

		StringBuilder str = new StringBuilder();
		String createDate = new String();
		if(party.getDemographics() != null && party.getDemographics().getOrganization() != null) {
			//get Create Date for Account
			for (Organization org : party.getDemographics().getOrganization()) {
				if(org.getSourceName().equalsIgnoreCase("customerprofile") && org.getCustomerCreateDate() != null) {
					createDate = org.getCustomerCreateDate();
					break;
				}
			}
		}

		if(!ORGNAME.isEmpty())
			str.append(ORGNAME);

		else
			str.append("This account");

		if(createDate.isEmpty()) {
			str.append(" have no create date");
			return str.toString();
		}

		Date date = new SimpleDateFormat("MM/dd/yyyy", 	Locale.ENGLISH).parse(createDate);
		Date threshold = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).parse("12/31/2018");

		if(date.after(threshold)) 
			str.append(" is one of our new Accounts.");

		else
			str.append(" is one of our old Accounts.");

		return str.toString();
	}


	/**
	 * Get engagement level based on the total interactions
	 * @param party
	 * @return
	 */
	private String getEngagementLevel(Party party) {

		StringBuilder str = new StringBuilder();
		Integer totalInteraction = new Integer(0);

		if(party.getInsights() != null && party.getInsights().getInteractionBehaviour() != null) {
			for(InteractionBehaviour interaction : party.getInsights().getInteractionBehaviour()) {
				if(interaction.getTotalInteractions() != null) {
					totalInteraction = Integer.parseInt(interaction.getTotalInteractions());
					break;
				}

				else {
					str.append("No interaction information available");
				}
			}
		}

		else {
			str.append("No interaction information available");
		}

		if(totalInteraction > 100 )
			str.append(ORGNAME + " has high engagement level.");

		else if(totalInteraction > 50)
			str.append(ORGNAME + " has medium engagement level.");

		else
			str.append(ORGNAME + " has low engagement level.");

		return str.toString();
	}

	/**
	 * To get the importance value of the account
	 * @param party
	 * @return
	 */
	private String getImportance(Party party) {

		StringBuilder str = new StringBuilder();

		if(party.getInsights() != null && party.getInsights().getPaymentBehaviour() != null) {
			for(PaymentBehaviour pay : party.getInsights().getPaymentBehaviour()) {
				if(pay.getTotalRevenue() != null) {
					Double revenue = pay.getTotalRevenue();

					if(Double.compare(revenue, 20000.0) > 0) 
						str.append(ORGNAME + " is one of our very important accounts.");

					else
						str.append(ORGNAME + " is one of our very lesser accounts.");

					break;
				}

				else {
					str.append("No revenue information is available for " + ORGNAME);
				}
			}
		}

		else {
			str.append("No revenue information is available for " + ORGNAME);
		}

		return str.toString();
	}


	@Override
	public String standardize(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public static void main(String[] args) throws ParseException {

		Date date = new SimpleDateFormat("MM/dd/yyyy", 	Locale.ENGLISH).parse("5/19/2007");
		Date threshold = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).parse("12/31/2018");

		System.out.println("Date " + date);
		System.out.println("Th " + threshold);

	}


}
