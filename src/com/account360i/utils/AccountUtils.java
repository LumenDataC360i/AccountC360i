package com.account360i.utils;

import java.io.IOException;

import com.allsight.Party;
import com.allsight.dao.AllSightDao;
import com.allsight.dao.DaoFactory;
import com.allsight.dao.entity.helper.EIDHelper;
import com.allsight.dao.entity.helper.EIDHelper.EntityVertex;
import com.allsight.entity.impl.Entity;
import com.allsight.exception.X360iException;
import com.allsight.sie.PartyUtils;

/**
 * @author Aradhana Pandey
 *
 */
public class AccountUtils extends PartyUtils{

	final static AllSightDao allsightdao = DaoFactory.getDAOInstance();

	/**
	 * Returns true if organization bo is present in the party
	 * @param e
	 * @return
	 */
	public static boolean isOrganization(Entity<?> e) {
		Party p = null;

		if (e instanceof Party) {
			p = (Party)e;
		} 
		else {
			return false;
		} 
		if (((Party) p).getDemographics() != null && ((Party) p).getDemographics().getOrganization()!= null) {
			return true;
		} 
		return false;
	}

	/**
	 * Returns true if person bo is present in the party
	 * @param e
	 * @return
	 */
	public static boolean isPerson(Entity<?> e) {
		Party p = null;
		if (e instanceof Party) {
			p = (Party)e;
		} 
		else {
			return false;
		} 
		if (((Party) p).getDemographics() != null && ((Party) p).getDemographics().getPerson()!= null) {
			return true;
		} 
		return false;
	}

	/** Returns true is the anchor is org record
	 * @param e
	 * @return
	 * @throws IOException 
	 * @throws X360iException 
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isOrgEID(Entity <?> e) throws X360iException, IOException {

		EIDHelper eid = (EIDHelper) e;
		EntityVertex anchor = eid.getStartVertexAsEntityVertex();
		String anchorSK = anchor.getSourceKey();
		Entity party = 	allsightdao.getEntity("party", "15", anchorSK);

		if(party != null && party.getBusinessObjects("Organization") != null)
			return true;

		else
			return false;
	}

}
