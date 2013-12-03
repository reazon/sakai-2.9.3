
package org.sakaiproject.sitemanage.api;

public interface RubricService {
	
	//DN 2012-08-14:function transferRubric to new site
	public void transferRubric(String formGradebookUid, String toGradebookUid);
	
	//DN 2012-08-14:function check rubric have been attached.
	public boolean checkAttachRubric(String formGradebookUid);
	//DN 2013-04-22: check show irubric
	public boolean isShowiRubricLink();
}
