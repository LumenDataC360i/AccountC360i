package com.account360i.postenrichment;

import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import com.allsight.Party;
import com.allsight.Party.AccountSummary;
import com.allsight.Party.CRM;
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

		summary = getMostSoldOrders(party);
		SUMMARYLIST.add(summary);

		summary = getBestContact(party);
		SUMMARYLIST.add(summary);

		summary = getRevenueDetails(party);
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
				str.append("This is one of our new ");

			else
				str.append("This is one of our old ");
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
			str.append("account. ");

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
		int total = 0;
		Timestamp today = DateUtil.getCurrentTimestamp();


		if(party.getTransactions() != null && party.getTransactions().getTransaction() != null) {

			for (Transaction trans : party.getTransactions().getTransaction()) {

				total++;
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
			str = "There has never been defaulted payment in the orders placed till date";

		if(count == 1) 
			str = "There has been only one defaulted payment in the orders placed till date";

		else {
			int prob = count * 100 /total;
			str = "There has been " + count + " defaulted payments in the orders placed till date. ";
			str = str + "These defaulted payments accounts for " + prob + "% of the total transactions.";

		}

		return str;
	}


	/**
	 * Get the mostly ordered product
	 * @param party
	 * @return
	 */
	private String getMostSoldOrders(Party party) {

		StringBuilder str = new StringBuilder();
		Map <String,Integer> productCountMap = new HashMap<String, Integer>();

		if(party.getTransactions() != null && party.getTransactions().getTransaction() != null) {

			for(Transaction trans : party.getTransactions().getTransaction()) {
				if(trans.getProductName() != null) {
					String product = trans.getProductName().toUpperCase();
					if(trans.getQuantity() != null) {
						Integer quantity = Integer.parseInt(trans.getQuantity());

						if(productCountMap.containsKey(product)) {
							productCountMap.put(product, productCountMap.get(product) + quantity);
						}

						else
							productCountMap.put(product, quantity);
					}
				}
			}
		}

		if(!productCountMap.isEmpty()) {
			List<Map.Entry<String,Integer>> list = new LinkedList<>(productCountMap.entrySet());
			Comparator<Entry<String, Integer>> c = new Comparator<Map.Entry<String,Integer>>() {

				//sort in descending order
				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
					return (o2.getValue().compareTo(o1.getValue()));
				}
			};

			Collections.sort(list, c);

			str.append("We have moslty sold " + list.get(0).getKey() + " to this account.");
			str.append(" Following is the list of products sold (desceding order): ");
			for(int i = 1; i < list.size()-1 ; i++) {
				str.append(list.get(i).getKey() + ", ");
			}

			str.append(list.get(list.size()-1).getKey());
		}

		return str.toString();
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
			str = "Best contact person for this account is " + bestSeller + ".";
		}

		return str;
	}

	/**
	 * Get revenue details for the products
	 * @param party
	 * @return
	 */
	private String getRevenueDetails(Party party) {
		StringBuilder str = new StringBuilder();

		Map <String,Integer> productAmountMap = new HashMap<String, Integer>();

		if(party.getTransactions() != null && party.getTransactions().getTransaction() != null) {

			for(Transaction trans : party.getTransactions().getTransaction()) {
				if(trans.getProductName() != null) {
					String product = trans.getProductName().toUpperCase();
					if(trans.getQuantity() != null) {
						Integer amount = (int) Double.parseDouble(trans.getTotalAmount());


						if(productAmountMap.containsKey(product)) {
							productAmountMap.put(product, productAmountMap.get(product) + amount);
						}

						else
							productAmountMap.put(product, amount);
					}
				}
			}
		}

		if(!productAmountMap.isEmpty()) {
			List<Map.Entry<String,Integer>> list = new LinkedList<>(productAmountMap.entrySet());
			Comparator<Entry<String, Integer>> c = new Comparator<Map.Entry<String,Integer>>() {

				//sort in descending order
				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
					return (o2.getValue().compareTo(o1.getValue()));
				}
			};

			Collections.sort(list, c);

			str.append(list.get(0).getKey() + "  have generated revenue of $" + NumberFormat.getNumberInstance(Locale.US).format(list.get(0).getValue()) + " for this account.");
			str.append(" Revenue from rest of the products is as follows (desceding order): ");
			for(int i = 1; i < list.size()-1 ; i++) {
				str.append(list.get(i).getKey() + ": $" + NumberFormat.getNumberInstance(Locale.US).format(list.get(i).getValue()) + ", ");
			}

			str.append(list.get(list.size()-1).getKey() + ": $" + NumberFormat.getNumberInstance(Locale.US).format(list.get(list.size()-1).getValue()));
		}

		return str.toString();
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
		int totalDone = 0;
		int totalDeal = 0;

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

				totalDeal++;
				if("done".equalsIgnoreCase(crm.getDealStatus())) 
					totalDone++;
			}
		}

		if(totalDeal == 0) {
			str = "There are no deals for this account";
			return str;
		}

		else {

			int totalProb = totalDone * 100 / totalDeal;

			str = "This account closes the deals in " + totalProb + "% of the cases.";

			if(renewCount != 0) {
				int prob = renewDoneCount * 100 / renewCount;
				str = str + " Also the renewal deals are closed in " + prob + "% of the cases.";
			}
		}

		return str;
	}

	/**
	 * Get Deal closure details
	 * @param party
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String getDealDetails(Party party) {
		StringBuilder str = new StringBuilder();

		Map<String,List<Integer>> productBasedDeals = new HashMap<>();
		Map<String,Integer> productDealClosure = new HashMap<>();

		if(party.getCRMS() != null && party.getCRMS().getCRM() != null) {
			for(CRM crm : party.getCRMS().getCRM()) {

				if(crm.getProductName() != null) {

					String prodId = crm.getProductName().toUpperCase();

					if(productBasedDeals.containsKey(prodId)) {

						List<Integer> values = productBasedDeals.get(prodId);
						values.set(0, values.get(0)+1);

						if("done".equalsIgnoreCase(crm.getDealStatus())) {
							values.set(1, values.get(1)+1);
						}
					}

					//add new entry in the map
					else {
						List<Integer> values = new ArrayList();

						//total deal is one for new entry
						values.add(0, 1);
						if("done".equalsIgnoreCase(crm.getDealStatus())) 
							values.add(1, 1);

						else
							values.add(1, 0);

						productBasedDeals.put(prodId, values);
					}
				}
			}
		}

		else{
			str.append("No deal information available");
			return str.toString();
		}

		logger.info("Value Map: " + productBasedDeals);

		if(!productBasedDeals.isEmpty()) {
			for(Map.Entry<String,List<Integer>> entry : productBasedDeals.entrySet()) {

				int total = entry.getValue().get(0);
				int done = entry.getValue().get(1);

				if(total == 0)
					continue;

				int closureRate = done * 100 / total;
				productDealClosure.put(entry.getKey(), closureRate);
			}
		}

		if(!productDealClosure.isEmpty()) {
			List<Map.Entry<String,Integer>> list = new LinkedList<>(productDealClosure.entrySet());
			Comparator<Entry<String, Integer>> c = new Comparator<Map.Entry<String,Integer>>() {

				//sort in descending order
				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
					return (o2.getValue().compareTo(o1.getValue()));
				}
			};

			Collections.sort(list, c);

			str.append("Deals for " + list.get(0).getKey() + " is closed with " + list.get(0).getValue() + "% rate.");
			str.append(" Deal closure rate for other prodcuts is as follows: ");

			for(int i = 1 ; i<list.size()-1 ; i++) {
				str.append(list.get(i).getKey() + "- " + list.get(i).getValue() + "%, ");
			}

			str.append(list.get(list.size()-1).getKey() + "- " + list.get(list.size()-1).getValue() + "%");

		}

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
			str = "This account has placed no orders with us.";

		return str;
	}

	/**
	 * Get Finance Summary
	 * @param party
	 * @return
	 */
	private String getFinanceInfo(Party party) {
		StringBuilder str = new StringBuilder();

		int minAmt = 9999999;
		int maxAmt = 0;
		int totalAmount = 0;
		int totalDeals = 0;

		if(party.getCRMS() != null && party.getCRMS().getCRM() != null) {

			for(CRM crm : party.getCRMS().getCRM()) {
				if("done".equalsIgnoreCase(crm.getDealStatus()) && crm.getEstimatedDealAmount() != null) {

					int estimateAmount = crm.getEstimatedDealAmount().intValue();
					//set the max deal amount
					if(estimateAmount > maxAmt)
						maxAmt = estimateAmount;

					//set minimum deal amount
					if(estimateAmount < minAmt)
						minAmt = estimateAmount;

					totalDeals ++;
					totalAmount += estimateAmount;
				}
			}
		}

		else {
			str.append("No finance information available");
			return str.toString();
		}

		int avgAmount = totalAmount / totalDeals;

		str.append("This account has signed an order of an average amount of $" + NumberFormat.getNumberInstance(Locale.US).format(avgAmount) + ".");
		str.append(" And the amount range in which it has placed orders is $" + NumberFormat.getNumberInstance(Locale.US).format(minAmt) + "-$");
		str.append(NumberFormat.getNumberInstance(Locale.US).format(maxAmt) + ".");

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


		Map <String,Integer> productCountMap = new HashMap<String, Integer>();

		productCountMap.put("Wire", 1200);
		productCountMap.put("Rods", 1300);
		productCountMap.put("Ropes", 1100);
		productCountMap.put("Paste", 900);
		productCountMap.put("Sensors", 1150);

		if(!productCountMap.isEmpty()) {
			List<Map.Entry<String,Integer>> list = new LinkedList<>(productCountMap.entrySet());
			Comparator<Entry<String, Integer>> c = new Comparator<Map.Entry<String,Integer>>() {

				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
					return (o2.getValue().compareTo(o1.getValue()));
				}
			};

			Collections.sort(list, c);
			for(Map.Entry<String,Integer> a : list) {
				System.out.println(a);
			}

		}

		String amt = "1247.04";
		int x = (int) Double.parseDouble(amt);
		System.out.println(x);



	}
}
