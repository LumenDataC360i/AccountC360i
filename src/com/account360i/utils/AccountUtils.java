package com.account360i.utils;

import com.allsight.Party;
import com.allsight.entity.impl.Entity;
import com.allsight.sie.PartyUtils;

/**
 * @author Aradhana Pandey
 *
 */
public class AccountUtils extends PartyUtils{

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
	
	
}
