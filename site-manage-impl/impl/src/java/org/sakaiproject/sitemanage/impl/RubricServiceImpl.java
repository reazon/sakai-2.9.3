package org.sakaiproject.sitemanage.impl;

import java.lang.Object;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Criteria;
import org.hibernate.Session;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.orm.hibernate3.HibernateCallback;

import org.sakaiproject.tool.gradebook.iRubric.GradableObjectRubric;
import com.reazon.tool.irubric.IRubricManager;
import org.sakaiproject.sitemanage.api.RubricService;

public class RubricServiceImpl extends HibernateDaoSupport implements RubricService {
	
	private final static Log Log = LogFactory.getLog(RubricServiceImpl.class);
	
	/**
	 * Init
	 */
	public void init()
	{
		Log.info("init()");
	}
	   
	/**
	* Destroy
	*/
	public void destroy()
	{
		Log.info("destroy()");
	}
	
	//use for get funtion of gradebook1
	private IRubricManager rubricManager;
	public void setRubricManager(IRubricManager rubricManager) {
		this.rubricManager = rubricManager;
	}
	
	public IRubricManager getRubricManager() {
		return rubricManager;
	}
	
	//DN 2012-08-13: transfer rubric from old site to new site
	//siteid = gradebookUid
	public void transferRubric(final String formGradebookUid, String toGradebookUid) {
		//get gradableobjects
		List objs = getGradableObjects(formGradebookUid);
		
		//check gradableobject of old site if have rubric attached then save rubric in new site
		if(objs.size() > 0) {
			
			for(int i=0; i< objs.size(); i++)
			{
				
				//Log.info("Value gradableobject:" + ((Object[])objs.get(i))[0] +" ,: "+ ((Object[])objs.get(i))[1] +" ,: "+ ((Object[])objs.get(i))[2]);
				Object[] obj = (Object[])objs.get(i);
				
				//get rubric for each gradableobjectid
				GradableObjectRubric objRubric = getGradableObjectRubric((Long)obj[0]);
				
				//if have rubric attached in old site then save rubric for new gradableobjectid of new site(new course)
				if(objRubric != null) { 
					
					//get gradableobjectid of new site(new course)
					Long newGradableObjectId = rubricManager.getGradableObjectId(obj[1].toString(), toGradebookUid);
	
					//if have value new gradableobjectid(gradableobjectid of copy) then save into table Rubric
					if(newGradableObjectId != null) {
						//create obj rubric
						//GradableObjectRubric newobjRubric = new GradableObjectRubricImpl(newGradableObjectId, objRubric.getiRubricId(), objRubric.getiRubricTitle());
						GradableObjectRubric newobjRubric = new GradableObjectRubric(newGradableObjectId, objRubric.getiRubricId(), objRubric.getiRubricTitle());
						
						//save value new gradableobject copy into table rubric
						getHibernateTemplate().saveOrUpdate(newobjRubric);
												
						Log.info("save successful "+ newGradableObjectId +" to new site:"+toGradebookUid);
					}
				}
				
			}
			
		}

	}
	//DN 2012-08-14:get rubric by gradableobjectid
	private GradableObjectRubric getGradableObjectRubric(final Long gradableObjectId) {

		GradableObjectRubric objRubric = rubricManager.getGradableObjectRubric(gradableObjectId);
		return objRubric;
		
	}
	
	//DN 2012-09-24: get gradableobjects
	private List getGradableObjects(final String formGradebookUid) {
		
		//get gradableobject by gradebookUid
		HibernateCallback hcbObj = new HibernateCallback() 
		{
			public Object doInHibernate(Session session) throws HibernateException {
		    	Query q = session.createQuery("select g.id, g.name from GradableObject as g where g.gradebook.uid=? and g.removed=false");
		    	q.setParameter(0, formGradebookUid, Hibernate.STRING);
		    	return q.list();
		    }
		};
		List objs = (List) getHibernateTemplate().execute(hcbObj);	
		return objs;
		
	}
	
	//DN 2012-08-14: check attach rubric(use for show checkbox copy rurbic in duplicate site of site info)
	public boolean checkAttachRubric(final String formGradebookUid) {
		//get gradableobjects
		List objs = getGradableObjects(formGradebookUid);
		
		if(objs.size() > 0) {
			
			//for each gradableobjectid, check gradableobject have attached rubric,
			//if have attached then return true
			for(int i=0; i< objs.size(); i++) { 
				//get object
				Object[] obj = (Object[])objs.get(i);
				
				//get gradable object Id
				Long gradableObjectId = (Long)obj[0];
				
				//get record from table Rubric
				GradableObjectRubric objRubric = getGradableObjectRubric(gradableObjectId);
				
				//if objRubric not null is have attached rubric then return true
				if(objRubric != null) {
					return true;
				}
			}

		} 
		return false;
	}
	public boolean isShowiRubricLink(){
		return rubricManager.isShowiRubricLink();
	}
}
