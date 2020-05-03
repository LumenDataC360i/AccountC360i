package com.account360i.enrichments;

import java.util.Collection;

import org.apache.log4j.Logger;

import com.allsight.Party;
import com.allsight.Party.Organization;
import com.allsight.enrichment.common.EnrichmentFunction;
import com.allsight.entity.impl.Entity;
import com.allsight.util.TextUtils;

public class URLEnrichment extends EnrichmentFunction {
	private static final Logger logger = Logger.getLogger(URLEnrichment.class);

	@Override
	public Object applyEnrichment(Entity<?> asParty) throws Exception {
		// TODO Auto-generated method stub
		
		if (asParty instanceof Party) {
			logger.debug("Enrichment to standardize URL");

			logger.debug("party before enrichment : "  + asParty);
			
			if (((Party) asParty).getDemographics() != null && ((Party) asParty).getDemographics().getOrganization()!= null) {
				
				Collection<Organization> orgs = ((Party) asParty).getDemographics().getOrganization();
				logger.debug("Collection : " + orgs);
				
				for (Organization org : orgs) {
					if(org.getOriginalWebsiteURL()!=null && !TextUtils.isNullOrEmpty(org.getOriginalWebsiteURL())) { 
						
						String url = org.getOriginalWebsiteURL().trim();
						String newUrl = url.replaceAll("[\\%\\[\\]^{|}<>`]","");
						String finalUrl = newUrl.replaceAll(" ","");
						org.setWebsiteURL(finalUrl);
					}
				}
			}
		}
		logger.debug("final party : " + asParty);
		return asParty;
	}

	@Override
	public String standardize(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public static void main(String[] args) {
		
		String str = "[]a<b}c|d`!%^^^";
		String newUrl = str.replaceAll("[\\%\\[\\]^{|}<>`]", " ");
		String socialHandle ="**/***ab*";
		int index = socialHandle.indexOf("/");
		String val = socialHandle.substring(0,index);
		
		System.out.println("val : " + val);
	}

}
