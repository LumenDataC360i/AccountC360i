package com.account360i.postenrichment.outputhandler;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.allsight.Party;
import com.allsight.enrichment.common.EnrichmentOutputHelper;
import com.allsight.entity.bean.EnrichmentInfo;
import com.allsight.entity.impl.Entity;

/**
 * @author Aradhana Pandey
 * Output handler for Eid post enrichments
 * Note : Perspective records business objects can not be part of eid post enrichment output
 */
public class SummaryOutputHandler implements EnrichmentOutputHelper {
	  private static final Logger logger = Logger.getLogger(SummaryOutputHandler.class);

	@Override
	public List<Entity> applyResult(Object resultObject, EnrichmentInfo enrichmentInfo, Entity entity) {
		// TODO Auto-generated method stub
		
		if(!(resultObject instanceof Party)) {
			 logger.error("Output Object is not of acceptable type. skipping enrichment");
			return null;
		}
		
		Party asparty = new Party();
		Party result = (Party)resultObject;
		asparty.setInsights(result.getInsights());
		
		List<Entity> resultEntities = new ArrayList<Entity>();
		resultEntities.add((Entity) asparty);
		
		if(resultEntities != null) {
			return resultEntities;
		}
		
		return null;
	}
}
