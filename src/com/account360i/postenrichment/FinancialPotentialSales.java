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

public class FinancialPotentialSales extends EnrichmentFunction {
	private static final Logger logger = Logger.getLogger(FinancialPotentialSales.class);
	private Map<String,ArrayList<Double>> ContactDealAmountMap = new HashMap<String,ArrayList<Double>>();
	private Map<String,String> ContactMap = new HashMap<String,String>();

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
				int key = 1;
				while (iter.hasNext()) { 
					Map.Entry mapelement = (Map.Entry)iter.next(); 
					ArrayList<Double> amountList= (ArrayList<Double>) mapelement.getValue();
					Double maxAmount = Collections.max(amountList);
					Double minAmount = Collections.min(amountList);
					Double avgAmount = avg(amountList);
					String contactID = (String) mapelement.getKey();
					String contactPersonName = ContactMap.get(contactID);
					
					logger.debug("max amount : " + maxAmount);
					logger.debug("min amount : " + minAmount);
					logger.debug("avg amount : " + avgAmount);
					logger.debug("id : " + contactID);
					logger.debug("name : " + contactPersonName);
					
					FinancialPotential finance = new FinancialPotential();
					finance.setKey(Integer.toString(key));
					key++;
					finance.setAmountRange(minAmount+"-"+maxAmount);
					finance.setAvgAmount(avgAmount.toString());
					finance.setContactID(contactID);
					finance.setContactName(contactPersonName);
					coll.add(finance);
				}
				insight.setFinancialPotential(coll);
				asparty.setInsights(insight);
			}
			return asparty;
		}
		
		return null;
	}

	private void populateContactMap(String contactPersonID, String contactPerson) {
		// TODO Auto-generated method stub
		if(!ContactMap.containsKey(contactPersonID)) {
			ContactMap.put(contactPersonID, contactPerson);
		}			
	}

	private Double avg(ArrayList<Double> amountList) {
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
	}

	private void populateAmountMap(String contactPersonID, Double estimatedDealAmount) {
		// TODO Auto-generated method stub
		 
		  ArrayList<Double> list = ContactDealAmountMap.get(contactPersonID); 
		   if(list == null)
		    {
		         list = new ArrayList<Double>();
		    }
		  list.add(estimatedDealAmount);
		  ContactDealAmountMap.put(contactPersonID,list);   
	}

	@Override
	public String standardize(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
