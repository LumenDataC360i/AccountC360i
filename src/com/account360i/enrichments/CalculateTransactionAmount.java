package com.account360i.enrichments;

import java.util.Collection;

import org.apache.log4j.Logger;

import com.allsight.Party;
import com.allsight.Party.Transaction;
import com.allsight.enrichment.common.EnrichmentFunction;
import com.allsight.entity.impl.Entity;

/**
 * @author Aradhana Pandey
 * Class for the calculation of amount in a transaction
 *
 */
public class CalculateTransactionAmount extends EnrichmentFunction {

	private static final Logger logger = Logger.getLogger(CalculateTransactionAmount.class);
	@Override
	public Object applyEnrichment(Entity<?> asParty) throws Exception {
		// TODO Auto-generated method stub
		
		if (asParty instanceof Party) {
			
			logger.debug("Enrichment to Calculate transaction amount");

			logger.debug("party before enrichment : "  + asParty);
			
			if (((Party) asParty).getTransactions() != null && ((Party) asParty).getTransactions().getTransaction()!= null) {
				
				Collection<Transaction> transactions = ((Party) asParty).getTransactions().getTransaction();
				logger.debug("Collection : " + transactions);
				
				for (Transaction transaction : transactions) {
					
					String unitPrice = transaction.getUnitAmount();
					Double unitPriceDouble = Double.parseDouble(unitPrice);
					
					String quantity = transaction.getQuantity();
					Integer quantityInt = Integer.parseInt(quantity);
					
					Double amount = unitPriceDouble * quantityInt;
					String amountString = amount.toString();
					
					logger.debug("Amount : " + amount);
					
					transaction.setAmount(amountString);      //Amount value
					
					String discountPer = transaction.getDiscountPercentage();
					Double discountPerStr = Double.parseDouble(discountPer);
					
					Double discount = amount * discountPerStr / 100;
					String discountStr = discount.toString();
					
					logger.debug("discount : " + discount);
					
					transaction.setUnitAmountDouble(unitPriceDouble);
					transaction.setDiscountAmountTotalDouble(discount);
					
					transaction.setDiscountAmountTotal(discountStr);  // discount value
					
					Double totalAmount = amount - discount;
					String totalAmountStr = totalAmount.toString();
					
					logger.debug("totalAmount : " + totalAmount);
					
					transaction.setTotalAmount(totalAmountStr);     //Total Amount		
				}
			}		
		}
		logger.debug("party after enrichment : "  + asParty);
		return asParty;
	}

	@Override
	public String standardize(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	

}
