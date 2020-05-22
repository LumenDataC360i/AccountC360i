package com.account360i.postenrichment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.allsight.Party;
import com.allsight.Party.Link;
import com.allsight.Party.OrganizationName;
import com.allsight.dao.AllSightEntityDao;
import com.allsight.dao.DaoFactory;
import com.allsight.enrichment.common.EnrichmentFunction;
import com.allsight.entity.bean.BusinessObject;
import com.allsight.entity.impl.Entity;
import com.allsight.entity.impl.EntityMeta;
import com.allsight.exception.AllSightException;
import com.allsight.manager.ConfigurationManager;
import com.allsight.shaded.com.google.common.collect.Iterables;
import com.allsight.shaded.com.google.common.collect.Lists;

/**
 * @author Aradhana Pandey
 * Class for creation of organization hierarchy
 */
public class OrganizationHierarchy extends EnrichmentFunction {

	private static final Logger logger = Logger.getLogger(OrganizationHierarchy.class);
	private static ConfigurationManager manager = ConfigurationManager.getInstance();
	private static final AllSightEntityDao dao = (AllSightEntityDao)DaoFactory.getDAOInstance();
	private Set<String> childSet = new HashSet<String>(); 
	private String root;    //root of hierarchy
	private Integer key = 1;


	/* (non-Javadoc)
	 * @see com.allsight.enrichment.common.EnrichmentFunction#applyEnrichment(com.allsight.entity.impl.Entity)
	 */
	@Override
	public Object applyEnrichment(Entity<?> entity) throws Exception {
		// TODO Auto-generated method stub

		Boolean present = false;
		Party hierarchyRecord = new Party();
		List<Party.OrganizationHierarchy> orghierarchy = new ArrayList<>();
		Collection<Party.OrganizationHierarchy> finalorghierarchy = new ArrayList<>();
		Collection<Link> linkcollection = new ArrayList<>();

		logger.debug("Party : " + entity);

		if (((Party) entity).getRelationships() != null && ((Party) entity).getRelationships().getOrganizationRelations()!= null) {

			Collection<com.allsight.Party.OrganizationRelations> relations = ((Party) entity).getRelationships().getOrganizationRelations();

			for (com.allsight.Party.OrganizationRelations rel : relations) {

				if(rel.getRelationshipSubType().equalsIgnoreCase("parent")) {
					logger.debug("parent relation is present so not a root");
					present = true;
					break;
				}
			}
		}

		if(present == true){
			logger.debug("not a root so returning null");
			return null;
		}

		//for root
		String sname = entity.getEntityMeta().getSourceName();
		String skey = entity.getEntityMeta().getSourceKey();
		String parent = sname + ":" + skey;
		childSet.add(parent);

		if(childSet.size()!=0){
			logger.debug("child set is not equal to zero");
			root = getName(entity);

			logger.debug("root name : " + root);

			EntityMeta entityMeta = new EntityMeta();
			entityMeta.setSourceName(sname);
			entityMeta.setSourceKey(skey+"_hierarchy");
			entityMeta.setModel("party");
			entityMeta.setVersion(null);

			hierarchyRecord.setEntityMeta(entityMeta);

			while(childSet.size()!=0){
				String firstChild = childSet.iterator().next();
				childSet.remove(firstChild);

				String[] split = firstChild.split(":");
				String sourcename = split[0];
				String sourcekey = split[1];

				if(sourcename.compareTo(sname) == 0 && sourcekey.compareTo(skey) == 0){    //if root entity
					getChildSet(entity);
					Link link = createLink(hierarchyRecord.getEntityMeta().getSourceName(),hierarchyRecord.getEntityMeta().getSourceKey(),sourcename,sourcekey) ;
					linkcollection.add(link);
					orghierarchy = createHierarchy(entity, root);
					finalorghierarchy = (Collection<com.allsight.Party.OrganizationHierarchy>) mergeCollection(finalorghierarchy, orghierarchy);
				}
				else{   //if not root entity
					Entity<?> chlidEntityRecord = getEntityById(sourcename,sourcekey);
					if(chlidEntityRecord != null) {
						getChildSet(chlidEntityRecord);
						Link link = createLink(hierarchyRecord.getEntityMeta().getSourceName(),hierarchyRecord.getEntityMeta().getSourceKey(),sourcename,sourcekey) ;
						linkcollection.add(link);
						orghierarchy = createHierarchy(chlidEntityRecord, root);
						finalorghierarchy = (Collection<com.allsight.Party.OrganizationHierarchy>) mergeCollection(finalorghierarchy, orghierarchy);	
					}
				}
			}
		}

		hierarchyRecord.setBusinessObjects("Link", linkcollection);

		Party.HierarchyCategory cat = new Party.HierarchyCategory();
		cat.setOrganizationHierarchy(finalorghierarchy);
		hierarchyRecord.setHierarchyCategory(cat);

		if(hierarchyRecord != null){
			Short sourceId = manager.getDataSourceManager().getNidbyDS(sname);
			dao.saveOrUpdateEntity("party",sourceId.toString(),skey+"_hierarchy",hierarchyRecord);
			logger.debug("final hierarchy recrd :" + hierarchyRecord);
		}
		return entity;
	}


	/**
	 * @param col1
	 * @param col2
	 * @return
	 * Merge two collections
	 */
	Collection<?> mergeCollection(Collection<?> col1, Collection<?> col2) {

		Iterable<?> combinedIterables = Iterables.unmodifiableIterable(Iterables.concat(col1, col2));
		col1 = Lists.newArrayList(combinedIterables);

		return col1;
	}

	/**
	 * @param entity
	 * @return
	 * Get organization name of an org entity
	 */
	String getName(Entity<?> entity)
	{
		String name="";

		if (((Party) entity).getDemographics() != null && ((Party) entity).getDemographics().getOrganizationName()!= null) {

			Collection<OrganizationName> names = ((Party) entity).getDemographics().getOrganizationName();
			name = names.iterator().next().getName();
		}

		return name;
	}


	/**
	 * @param entity
	 * Get child source keys for organization relations 
	 */
	void getChildSet(Entity<?> entity)
	{
		if (((Party) entity).getRelationships() != null && ((Party) entity).getRelationships().getOrganizationRelations()!= null) {
			Collection<com.allsight.Party.OrganizationRelations> relations = ((Party) entity).getRelationships().getOrganizationRelations();

			for (com.allsight.Party.OrganizationRelations rel : relations) {

				if(rel.getRelationshipSubType().equalsIgnoreCase("child")){
					String targetSourceName = rel.getTargetSourceName();
					String targetSourceKey = rel.getTargetSourceKey();
					String childinfo = targetSourceName + ":" + targetSourceKey;
					childSet.add(childinfo);
				}
			}
		}
	}


	/**
	 * @param entity
	 * @param root
	 * @return
	 * Create hierarchy with a particular parent
	 */
	List<com.allsight.Party.OrganizationHierarchy> createHierarchy (Entity<?> entity, String root){

		List<Party.OrganizationHierarchy> orgHierarchy= new ArrayList<>();	

		if (((Party) entity).getRelationships() != null && ((Party) entity).getRelationships().getOrganizationRelations()!= null) {
			Collection<com.allsight.Party.OrganizationRelations> relations = ((Party) entity).getRelationships().getOrganizationRelations();

			for (com.allsight.Party.OrganizationRelations rel : relations) {

				Party.OrganizationHierarchy hierarchy = null;

				if(rel.getRelationshipSubType().equalsIgnoreCase("child")){

					hierarchy = new Party.OrganizationHierarchy();

					String tskey = rel.getTargetSourceKey();
					String tsname = rel.getTargetSourceName();
					Entity tEntity = getEntityById(tsname, tskey);


					hierarchy.setChild(getName(tEntity));
					hierarchy.setRoot(root);
					hierarchy.setParent(getName(entity));
					hierarchy.setKey(key.toString());

					key++;
				}

				if(hierarchy != null) {
					orgHierarchy.add(hierarchy);
				}			
			}
		}

		return orgHierarchy;
	}


	/**
	 * @param sourcename
	 * @param sourceKey
	 * @param targetSourceName
	 * @param targetSourceKey
	 * @return
	 * Create link business object from new hierarchy record to records which are part of hierarchy
	 */
	Link createLink (String sourcename, String sourceKey, String targetSourceName, String targetSourceKey){

		Link link= new Link();

		link.setSourceKey(sourceKey);
		link.setSourceName(sourcename);
		link.setTargetSourceKey(targetSourceKey);
		link.setTargetSourceName(targetSourceName);
		link.setRelationshipSubType("Hierarchy");
		return link;
	}


	/**
	 * @param sourceName
	 * @param sourceKey
	 * @return
	 * Get entity from source name and source key
	 */
	Entity<?> getEntityById(String sourceName, String sourceKey){

		Entity<?> entityRecord = null;
		Short sourceId = manager.getDataSourceManager().getNidbyDS(sourceName);
		logger.debug("source id : " + sourceId);

		try {
			entityRecord = dao.getEntity("party", sourceId.toString(), sourceKey);
			if (logger.isDebugEnabled())
				logger.debug("Entity : " + entityRecord); 
		} catch (AllSightException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return entityRecord;
	}


	@Override
	public String standardize(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}
}
