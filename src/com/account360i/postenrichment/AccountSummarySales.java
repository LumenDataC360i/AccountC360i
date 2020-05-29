package com.account360i.postenrichment;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import com.allsight.Party;
import com.allsight.Party.AccountSummary;
import com.allsight.Party.CRM;
import com.allsight.Party.DealBehaviour;
import com.allsight.Party.InteractionBehaviour;
import com.allsight.Party.Organization;
import com.allsight.Party.PaymentBehaviour;
import com.allsight.Party.Transaction;
import com.allsight.enrichment.common.EnrichmentFunction;
import com.allsight.entity.impl.Entity;
import com.allsight.util.DateUtil;

/**
 * EID post enrichment to populate various account summary details for sales perspective
 * @author Garima Jain
 *
 */
@SuppressWarnings("deprecation")
public class AccountSummarySales extends EnrichmentFunction {
	private static final Logger logger = Logger.getLogger(AccountSummarySales.class);

	//private String ORGNAME = new String();
	private Collection<String> SUMMARYLIST = new ArrayList<>();
	Integer key = 1;

	@Override
	public Object applyEnrichment(Entity<?> entity) throws Exception {

		Party party = (Party) entity;
		String summary = new String(); 
		Collection<AccountSummary> summaryColl = new ArrayList<>();

		logger.info("Deriving Account summary");

		/*		if(party.getDemographics() != null && party.getDemographics().getOrganizationName() != null) {

			//get Organization Name
			for (OrganizationName orgName : party.getDemographics().getOrganizationName()) {
				if(orgName.getSourceName().equalsIgnoreCase("customerprofile") && orgName.getName() != null) {
					ORGNAME = orgName.getName();
				}
			}
		}*/

		summary = getBasicInfo(party);
		SUMMARYLIST.add(summary);

		summary = getPaymentDetails(party);
		SUMMARYLIST.add(summary);

		summary = getDefaultPaymentDetails(party);
		SUMMARYLIST.add(summary);

		summary = getMostOrders(party);
		SUMMARYLIST.add(summary);

		summary = getBestContact(party);
		SUMMARYLIST.add(summary);

		summary = getDealDetails(party);
		SUMMARYLIST.add(summary);

		summary = getDealRenewal(party);
		SUMMARYLIST.add(summary);

		summary = getTotalOrders(party);
		SUMMARYLIST.add(summary);

		summary = getFinanceInfo(party);
		SUMMARYLIST.add(summary);


		logger.info("Summary collection: " + SUMMARYLIST);

		if(!SUMMARYLIST.isEmpty()) {
			for(String str : SUMMARYLIST) {

				if(str.isEmpty())
					continue;

				AccountSummary aSummary = new AccountSummary();
				aSummary.setSummary(str);
				aSummary.setKey(key.toString() + ".");

				logger.info(aSummary.getKey() + ": "+ str);
				key+=1;
				summaryColl.add(aSummary);
			}
		}

		party.getInsights().setAccountSummary(summaryColl);
		return party;
	}


	/**
	 * To get the basic info like age, importance and engagement level
	 * @return
	 * @throws ParseException
	 */
	private String getBasicInfo(Party party) throws ParseException {

		StringBuilder str = new StringBuilder();
		String createDate = new String();
		Integer totalInteraction = null;
		Double revenue = null;

		if(party.getDemographics() != null && party.getDemographics().getOrganization() != null) {
			//get Create Date for Account
			for (Organization org : party.getDemographics().getOrganization()) {
				if(org.getSourceName().equalsIgnoreCase("customerprofile") && org.getCustomerCreateDate() != null) {
					createDate = org.getCustomerCreateDate();
					break;
				}
			}
		}

		Date date = new SimpleDateFormat("MM/dd/yyyy", 	Locale.ENGLISH).parse(createDate);
		Date threshold = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH).parse("12/31/2018");

		if(party.getInsights() != null && party.getInsights().getInteractionBehaviour() != null) {
			for(InteractionBehaviour interaction : party.getInsights().getInteractionBehaviour()) {
				if(interaction.getTotalInteractions() != null) {
					totalInteraction = Integer.parseInt(interaction.getTotalInteractions());
					logger.info("Total interactions: " + totalInteraction);
					break;
				}
			}
		}

		if(party.getInsights() != null && party.getInsights().getPaymentBehaviour() != null) {
			for(PaymentBehaviour pay : party.getInsights().getPaymentBehaviour()) {
				if(pay.getTotalRevenue() != null) {
					revenue = pay.getTotalRevenue();
					logger.info("Total Revenue: " + revenue);
					break;
				}
			}
		}

		if(!createDate.isEmpty()) {
			if(date.after(threshold)) 
				str.append("This is one of our new");

			else
				str.append("This is one of our old");
		}

		else
			str.append("This account is");

		if(!createDate.isEmpty() && revenue != null)
			str.append("and");

		if(revenue != null) {
			if(Double.compare(revenue, 20000.0) > 0)
				str.append(" very important account. ");

			else
				str.append(" very lesser account. ");
		}

		else
			str.append(" account. ");

		if(totalInteraction == null)
			return str.toString();

		if(totalInteraction > 100 )
			str.append("The engagement level is high for this account.");

		else if(totalInteraction > 50)
			str.append("The engagement level is meduim for this account.");

		else
			str.append("The engagement level is low for this account.");

		return str.toString();
	}


	/**
	 * To get the average payment time details
	 * @param party
	 * @return
	 */
	private String getPaymentDetails(Party party) {

		String str = null;
		logger.info("Payment Behaviour collection: " + party.getInsights().getPaymentBehaviour());

		if(party.getInsights() != null && party.getInsights().getPaymentBehaviour() != null) {
			for(PaymentBehaviour pay : party.getInsights().getPaymentBehaviour()) {
				if(pay.getAvgPaymentTime() != null) {
					String avgTime = pay.getAvgPaymentTime();
					logger.info("Average payment time: " + avgTime);
					str = new String("For orders placed usually the payment is done within " + avgTime + " of the closure.");
				}

				else {
					str = new String("No payment information is available.");
				}
			}
		}

		else {
			str = new String("No revenue information is available.");
		}

		return str;
	}

	/**
	 * Get the defaulted payment status
	 * @param party
	 * @return
	 */
	private String getDefaultPaymentDetails(Party party) {

		String str = new String();
		int count = 0;
		Timestamp today = DateUtil.getCurrentTimestamp();


		if(party.getTransactions() != null && party.getTransactions().getTransaction() != null) {

			for (Transaction trans : party.getTransactions().getTransaction()) {

				//when the status is pending
				if("pending".equalsIgnoreCase(trans.getPaymentStatus())) {
					Timestamp ts = trans.getStartTimestamp();
					if(ts == null)
						continue;

					long difference = today.getTime() - ts.getTime();
					long dayDiff = difference / (1000 * 60 * 60 * 24);

					if(dayDiff > 30) {
						count ++;
					}
				}
			}
		}

		else{
			str = "No payment detail available";
			return str;
		}

		//no defaulted payments
		if(count == 0) 
			str = "Meanwhile there has never been defaulted payment in the orders placed till date";

		if(count == 1) 
			str = "Meanwhile there has been only one defaulted payment in the orders placed till date";

		else
			str = "Meanwhile there has been " + count + " defaulted payments in the orders placed till date";

		return str;
	}


	/**
	 * Get the mostly ordered product
	 * @param party
	 * @return
	 */
	private String getMostOrders(Party party) {

		String str = new String();

		logger.info("Deal Behaviour collection: " + party.getInsights().getDealBehaviour());
		if(party.getInsights() != null && party.getInsights().getDealBehaviour() != null) {

			for(DealBehaviour deal : party.getInsights().getDealBehaviour()) {
				if(deal.getMostlyOrderedProduct() != null) {
					str = "We have mostly sold " + deal.getMostlyOrderedProduct() + " to this account.";
				}
			}
		}

		return str;
	}

	/** Best contact person for the account
	 * @param party
	 * @return
	 */
	private String getBestContact(Party party) {
		String str = new String();
		Map <String,Integer> contactMap = new HashMap<>();
		int max = 0;
		String bestSeller = "";

		if(party.getCRMS() != null && party.getCRMS().getCRM() != null) {
			for(CRM crm : party.getCRMS().getCRM()) {

				if("done".equalsIgnoreCase(crm.getDealStatus()) && crm.getContactPerson() != null) {
					
					String contactName = crm.getContactPerson();
					if(contactMap.containsKey(contactName)) {
						int value = contactMap.get(contactName);
						value+=1;
						contactMap.put(contactName, value);
					}
					
					else
						contactMap.put(contactName, 1);
				}
			}
		}

		
		for(Map.Entry<String, Integer> pair : contactMap.entrySet()) {
			if(max < pair.getValue()) {
				bestSeller = new String(pair.getKey());
				max = pair.getValue();
			}
		}
		
		if(!bestSeller.isEmpty()) {
			str = "Best contact person for this account is " + bestSeller;
		}
		
		return str;
	}

	/**
	 * Get the deal renewal information
	 * @param party
	 * @return
	 */
	private String getDealRenewal(Party party) {
		String str = new String();
		int renewCount = 0;
		int renewDoneCount = 0;

		if(party.getCRMS() != null && party.getCRMS().getCRM() != null) {

			for(CRM crm : party.getCRMS().getCRM()) {

				//count renewal deals
				if("renewal".equalsIgnoreCase(crm.getOpportunityType())) {
					renewCount++;

					//when renewal deal is completed
					if("done".equalsIgnoreCase(crm.getDealStatus())) {
						renewDoneCount++;
					}
				}
			}
		}

		if(renewCount == 0)
			str = "There are no renewal orders with us.";

		else {
			int prob = renewDoneCount * 100 / renewCount;
			str = "This account closes the renewal orders in " + prob + "% of the cases.";
		}

		return str;
	}

	/**
	 * Get Deal details
	 * @param party
	 * @return
	 */
	private String getDealDetails(Party party) {
		StringBuilder str = new StringBuilder();

		String mostOrdered = null;
		String leastOrdered = null;
		int mostDone = 0;
		int mostTotal = 0;
		int leastDone = 0;
		int leastTotal = 0;

		Double mostProb = 0.0;
		Double leastProb = 0.0;

		if(party.getInsights() != null && party.getInsights().getDealBehaviour() != null) {
			for(DealBehaviour deal : party.getInsights().getDealBehaviour()) {
				mostOrdered = deal.getMostlyOrderedProduct();
				leastOrdered = deal.getLeastOrderedProduct();

				logger.info("Most Ordered: " + mostOrdered);
				logger.info("Least Ordered: " + leastOrdered);
			}
		}

		if(party.getCRMS() != null && party.getCRMS().getCRM() != null) {
			for(CRM crm : party.getCRMS().getCRM()) {

				//for most ordered product
				if(mostOrdered != null && mostOrdered.equalsIgnoreCase(crm.getProductName())) {
					mostTotal++;

					if("done".equalsIgnoreCase(crm.getDealStatus()))
						mostDone++;
				}

				//for least ordered product
				if(leastOrdered != null && leastOrdered.equalsIgnoreCase(crm.getProductName())) {
					leastTotal++;

					if("done".equalsIgnoreCase(crm.getDealStatus()))
						leastDone++;
				}
			}
		}

		else{
			str.append("No deal information available");
			return str.toString();
		}

		//logger.info("Values needed: " + mostTotal + " : " + mostDone + " : " + leastTotal + " : " + leastDone);

		if(mostTotal != 0) 
			mostProb = (double) (mostDone * 100 / mostTotal);

		if(leastTotal != 0) 
			leastProb = (double) (leastDone *100 / leastTotal);

		if(mostOrdered != null) 
			str.append(mostProb + "% of the cases closes the deal for " + mostOrdered + " positively");

		if(leastOrdered != null) 
			str.append(" whereas for the " + leastOrdered + " it positively closed only " + leastProb + "% of the time.");

		return str.toString();
	}

	/**
	 * Get the total ordered product
	 * @param party
	 * @return
	 */
	private String getTotalOrders(Party party) {

		String str = new String();

		if(party.getTransactions() != null && party.getTransactions().getTransaction() != null) {

			Collection<Transaction> trans = party.getTransactions().getTransaction();
			str = "This account has placed a total of " + trans.size() + " orders with us";
		}

		else 
			str = "This account has placed no orders with us";

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

		str.append("This account has signed an order of an average amount of $" + avgAmount + "/-.");
		str.append(" And the amount range in which it has placed orders is $" + minAmt + "-$" + maxAmt + ".");

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
