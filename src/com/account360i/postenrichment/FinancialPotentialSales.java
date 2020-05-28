package com.account360i.postenrichment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.allsight.Party;
import com.allsight.Party.CRM;
import com.allsight.Party.FinancialPotential;
import com.allsight.enrichment.common.EnrichmentFunction;
import com.allsight.entity.impl.Entity;
import com.ibm.icu.text.DecimalFormat;

/**
 * @author Aradhana Pandey
 * Enrichment to Calculate financial potential of an organization
 *
 */
@SuppressWarnings("deprecation")
public class FinancialPotentialSales extends EnrichmentFunction {
	private static final Logger logger = Logger.getLogger(FinancialPotentialSales.class);
	private Map<String,ArrayList<Double>> ContactDealAmountMap = new HashMap<String,ArrayList<Double>>();
	private Map<String,String> ContactMap = new HashMap<String,String>();
	private Map<String,Double> ContactSumAmount = new HashMap<String,Double>();

	@Override
	public Object applyEnrichment(Entity<?> entity) throws Exception {
		// TODO Auto-generated method stub

		if (!getExecutionContext().isEIDRecord()) {
			logger.warn("This enrichment expects an EID record. Skipping enrichment");
			return null;
		} 
		Party asparty = financialPotentialSales(entity);
		logger.debug("asparty :: " + asparty);
		return asparty;
	}

	/**
	 * @param entity
	 * @return
	 * Function to calculate financial potential
	 */
	private Party financialPotentialSales(Entity<?> entity) {
		// TODO Auto-generated method stub

		logger.debug("Eid Party : " + entity);

		if (((Party) entity).getCRMS() != null && ((Party) entity).getCRMS().getCRM()!= null) {
			Collection<CRM> crms = ((Party) entity).getCRMS().getCRM();
			logger.debug("crm collection : " + crms);

			for (CRM crm : crms) {
				if(crm.getContactPersonID() != null && !crm.getContactPersonID().isEmpty() 
						&& crm.getEstimatedDealAmount() != null && !crm.getEstimatedDealAmount().toString().isEmpty()) {
					populateAmountMap(crm.getContactPersonID(),crm.getEstimatedDealAmount());
					populateContactMap(crm.getContactPersonID(),crm.getContactPerson());
				}
			}

			logger.debug("ContactDealAmountMap : " + ContactDealAmountMap);
			logger.debug("ContactMap : " + ContactMap);

			Party asparty = (Party)entity;
			Collection<FinancialPotential> coll = new ArrayList<FinancialPotential>();
			Party.Insights insight = new Party.Insights();

			if(ContactDealAmountMap != null) {
				Iterator iter = ContactDealAmountMap.entrySet().iterator();
				while (iter.hasNext()) { 
					Map.Entry mapelement = (Map.Entry)iter.next(); 
					ArrayList<Double> amountList= (ArrayList<Double>) mapelement.getValue();
					int numberOfOrders = amountList.size();
					Double maxAmount = Collections.max(amountList);
					Double minAmount = Collections.min(amountList);
					String contactID = (String) mapelement.getKey();
					String contactPersonName = ContactMap.get(contactID);
					Double avgAmount = ContactSumAmount.get(contactID) / numberOfOrders;
					DecimalFormat df = new DecimalFormat("0.00");
					//double avg = (double) avgAmount;
					String avgAmountString = df.format(avgAmount);

					logger.debug("max amount : " + maxAmount);
					logger.debug("min amount : " + minAmount);
					logger.debug("avg amount : " + avgAmountString);
					logger.debug("id : " + contactID);
					logger.debug("name : " + contactPersonName);
					logger.debug("numberOfOrders : " + numberOfOrders);

					FinancialPotential finance = new FinancialPotential();
					finance.setKey(contactID.toUpperCase()+"~"+contactPersonName.toUpperCase());
					if(minAmount.compareTo(maxAmount) == 0)
					{
						finance.setAmountRange("$"+minAmount);
					}
					else {
						finance.setAmountRange("$"+minAmount+ " - $"+maxAmount);
					}
					finance.setAvgAmount(avgAmountString);
					finance.setContactID(contactID);
					finance.setContactName(contactPersonName);
					finance.setNumberOfOrders(Integer.toString(numberOfOrders));
					coll.add(finance);
				}
				insight.setFinancialPotential(coll);

				if(asparty.getInsights() == null)
					asparty.setInsights(insight);

				else
					asparty.getInsights().setFinancialPotential(coll);
			}
			return asparty;
		}

		return null;
	}

	/**
	 * @param contactPersonID
	 * @param contactPerson
	 * Function to populate map where key is contact id and value is contact name
	 */
	private void populateContactMap(String contactPersonID, String contactPerson) {
		// TODO Auto-generated method stub
		if(!ContactMap.containsKey(contactPersonID)) {
			ContactMap.put(contactPersonID, contactPerson);
		}			
	}

	/**
	 * @param amountList
	 * @return
	 * Function to return avg of element in a list
	 */
	/*private Double avg(ArrayList<Double> amountList) {
		// TODO Auto-generated method stub

		Double avg = 0.0;
		int size = amountList.size();
		for(int i = 0; i < size; i++) {
			avg += amountList.get(i);
		}
		if(avg > 0.0) {
			avg = avg / size;
		}

		return avg;
	}*/

	/**
	 * @param contactPersonID
	 * @param estimatedDealAmount
	 * Function to populate a map where key is contact id and value is list of all the amounts in all the orders
	 * and another map where key is contact id and value is sum of all the order values
	 **/
	private void populateAmountMap(String contactPersonID, Double estimatedDealAmount) {
		// TODO Auto-generated method stub

		ArrayList<Double> list = ContactDealAmountMap.get(contactPersonID); 
		if(list == null)
		{
			list = new ArrayList<Double>();
		}
		list.add(estimatedDealAmount);
		ContactDealAmountMap.put(contactPersonID,list);   

		if(ContactSumAmount.containsKey(contactPersonID)) {
			Double sum = ContactSumAmount.get(contactPersonID);
			sum = sum + estimatedDealAmount;
			ContactSumAmount.put(contactPersonID, sum);
		}
		else {
			ContactSumAmount.put(contactPersonID, estimatedDealAmount);
		}
	}

	@Override
	public String standardize(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
