package com.account360i.enrichments;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.allsight.Party;
import com.allsight.Party.SocialHandle;
import com.allsight.enrichment.common.EnrichmentFunction;
import com.allsight.enrichment.common.ParamContext;
import com.allsight.entity.bean.BusinessObject;
import com.allsight.entity.bean.EnrichmentInfo;
import com.allsight.entity.impl.Entity;
import com.allsight.util.TextUtils;

/**
 * @author Aradhana Pandey
 * Class to standardize social handle
 */
public class SocialHandleEnrichment extends EnrichmentFunction {
	private static final Logger logger = Logger.getLogger(SocialHandleEnrichment.class);

	@Override
	public Object applyEnrichment(Entity<?> asParty) throws Exception {
		// TODO Auto-generated method stub


		if (asParty instanceof Party) {
			logger.debug("Enrichment to standardize SocialHandle");

			logger.debug("party before enrichment : "  + asParty);

			if (((Party) asParty).getDemographics() != null && ((Party) asParty).getDemographics().getSocialHandle()!= null) {

				Collection<SocialHandle> handles = ((Party) asParty).getDemographics().getSocialHandle();
				logger.debug("Collection : " + handles);

				for (SocialHandle bo : handles) {
					if(bo.getOriginalValue()!=null && !TextUtils.isNullOrEmpty(bo.getOriginalValue())) { 
						String socialHandle = bo.getOriginalValue().trim();
						int index = socialHandle.indexOf("/");
						logger.debug("Index : " + index);
						if(index == -1) {      // backslash is not present
							logger.debug("first");
							String value = new String(socialHandle);
							bo.setValue(value);
							bo.setValueStandardizationIndicator("1");
						}
						else if(index > 0 && index < socialHandle.length()-1) {
							logger.debug("sec");
							String value = socialHandle.substring(index+1);
							String type = socialHandle.substring(0, index);
							bo.setType(type);
							bo.setValue(value);
							bo.setValueStandardizationIndicator("1");
						}
						else if(index == 0){
							logger.debug("third");
							String value = socialHandle.substring(index+1);
							bo.setValue(value);
							bo.setValueStandardizationIndicator("1");
						}
						else if(index == socialHandle.length()-1) {
							logger.debug("fourth");
							String type = socialHandle.substring(0, index);
							bo.setType(type);
							bo.setValueStandardizationIndicator("1");
						}
						else {
							logger.debug("fifth");
							bo.setValueStandardizationIndicator("0");
						}
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
}
