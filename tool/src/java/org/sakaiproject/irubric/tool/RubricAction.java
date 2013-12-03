package org.sakaiproject.irubric.tool;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.AuthzGroup;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.authz.api.PermissionsHelper;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.authz.cover.AuthzGroupService;
import org.sakaiproject.cheftool.Context;
import org.sakaiproject.cheftool.ControllerState;
import org.sakaiproject.cheftool.JetspeedRunData;
import org.sakaiproject.cheftool.PagedResourceActionII;
import org.sakaiproject.cheftool.RunData;
import org.sakaiproject.cheftool.VelocityPortlet;
import org.sakaiproject.cheftool.api.Alert;
import org.sakaiproject.cheftool.AlertImpl;
import org.sakaiproject.cheftool.api.Menu;
import org.sakaiproject.cheftool.menu.MenuImpl;
import org.sakaiproject.event.api.SessionState;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.component.cover.ComponentManager;
// import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.exception.IdInvalidException;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.IdUsedException;
import org.sakaiproject.exception.InUseException;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.util.StringUtil;
import org.sakaiproject.util.ParameterParser;
import org.sakaiproject.javax.PagingPosition;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.ToolSession;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.irubric.api.RubricToolService;
import org.sakaiproject.tool.gradebook.Assignment;
import org.sakaiproject.tool.gradebook.GradableObject;
import org.sakaiproject.component.api.ServerConfigurationService;

public class RubricAction extends PagedResourceActionII
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(RubricAction.class);
		
	/** Resource bundle using current language locale */
	private static ResourceLoader rb = new ResourceLoader("irubric");
	
	private Boolean allowShowiRubricLink = false;

	//template
	private static final String TEMPLATE_LIST_IRUBRICS_MANAGE = "_manage";
	
	private static final String TEMPLATE_LIST_IRUBRICS_GRADEALL = "_gradeall";

	// private static final String TEMPLATE_PERMISSION = "_permissions";

	/** The total list item before paging */
	private static final String STATE_PAGEING_TOTAL_ITEMS = "state_paging_total_items";
	
	/** DN 2012-11-20: The context string */
	private static final String STATE_CONTEXT_STRING = "irubric.context_string";
	
	// mode(main page/myrurbic/gallery/build/grade all)
	private static final String STATE_MODE = "iRubric.mode";
	
	/** The selected view */
	private static final String STATE_SELECTED_VIEW = "state_selected_view";

	/*mode(use link to module) */
	private static final String MODE_MAIN_PAGE = "mainpage";
	private static final String MODE_MY_RUBRIC = "myr";
	private static final String MODE_GALLERY = "gly";
	private static final String MODE_BUILD = "bld";
	private static final String MODE_GRADEALL = "gradeall"; 
	//DN 2013-10_28: Issue 181: eport link
	private static final String MODE_MY_EPORT = "myepo";
	private static final String MODE_BUILD_EPORT = "bldepo";
	private static final String MODE_IRUBRIC_HOME = "iRubHome";

	// mode permission
	// private static final String MODE_PERMISSION = "permissions";

	private static String gradebookUId;
	private static String gbToolPlacement;
	private static String userId;
	
	/** state sort * */
	private static final String SORTED_BY = "iRubric.sorted_by";

	/** state sort ascendingly * */
	private static final String SORTED_ASC = "iRubric.sorted_asc";

	/** default sorting */
	private static final String SORTED_BY_DEFAULT = "default";

	/** sort by assignment title */
	private static final String SORTED_BY_TITLE = "title";

	/** sort by assignment due date */
	private static final String SORTED_BY_DUEDATE = "duedate";

	/** The user */
	private static final String STATE_USER = "iRubric.user";
	/**
	 * Default is to use when Portal starts up
	 */
	private RubricToolService rubService = (RubricToolService) ComponentManager.get("org.sakaiproject.irubric.api.RubricToolService");
	
    public final static String TOOL_ID_GRADEBOOK = "sakai.gradebook.tool";

	
	public String buildMainPanelContext(VelocityPortlet portlet, Context context, RunData data, SessionState state)
	{
		String template = null;

		//set resource bundle into .vm
		context.put("tlang", rb);

		if(allowShowiRubricLink) {
			//avoid error when save/cancel permission page
			hackcode(state);

			//set siteid into state_context_string(need for check permission)
			String siteId = ToolManager.getCurrentPlacement().getContext();
			if (state.getAttribute(STATE_CONTEXT_STRING) == null || 
				((String) state.getAttribute(STATE_CONTEXT_STRING)).length() == 0)
			{
				state.setAttribute(STATE_CONTEXT_STRING, siteId);
			} // if context string is null

			//check show function permission
			boolean isAllowUpdateSite = SiteService.allowUpdateSite((String) state.getAttribute(STATE_CONTEXT_STRING));
			context.put("isAllowUpdateSite", Boolean.valueOf(isAllowUpdateSite));
			
			//DN 2012-09-21: get gradebookUId
			gradebookUId = ToolManager.getInstance().getCurrentPlacement().getContext();
			gbToolPlacement = getToolPlacement(ToolManager.getInstance().getCurrentPlacement().getContext(), TOOL_ID_GRADEBOOK);
			
			//set var gradebookUId in file vm
			context.put("gradebookUId",gradebookUId);
			context.put("gbToolPlacementId",gbToolPlacement);

			//check permission
			boolean isAllowBuild, isAllowAccessGallery, isAllowReports, isAllowAssessment, isAllowMyRubrics, isAllowGrades = false;
		
			//get site reference
			String siteRef = SiteService.siteReference(siteId);

			//permission build irubric
			isAllowBuild = rubService.allowBuildiRubric(userId, siteRef);
			context.put("isAllowBuild",isAllowBuild);
			
			//permission access gallery
			isAllowAccessGallery = rubService.allowAccessGalleryiRubric(userId, siteRef);
			context.put("isAllowAccessGallery",isAllowAccessGallery);

			//permission access assessment irubric
			isAllowAssessment = rubService.allowAssessmentiRubric(userId, siteRef);
			context.put("isAllowAssessment",isAllowAssessment);

			//permission access iRubric report
			isAllowReports = rubService.allowiRubricReports(userId, siteRef);
			context.put("isAllowReports",isAllowReports);

			//permission access my rubric
			isAllowMyRubrics = rubService.allowMyRubrics(userId, siteRef);
			context.put("isAllowMyRubrics",isAllowMyRubrics);

			//permission access grades
			isAllowGrades = rubService.allowGrades(userId, siteRef);
			context.put("isAllowGrades",isAllowGrades);


			//permission access iRubric home
			boolean isAllowiRubricHome = rubService.allowiRubricHome(userId, siteRef);
			context.put("isAllowiRubricHome",isAllowiRubricHome);

			//permission access My ePortfolios
			boolean isAllowMyePortfolios = rubService.allowMyePortfolios(userId, siteRef);
			context.put("isAllowMyePortfolios",isAllowMyePortfolios);

			//permission access Build ePortfolios
			boolean isAllowBuildePortfolios = rubService.allowBuildePortfolios(userId, siteRef);
			context.put("isAllowBuildePortfolios",isAllowBuildePortfolios);

			//permission access Assignment matrices
			boolean isAllowAssMatrices = rubService.allowAssMatrices(userId, siteRef);
			context.put("isAllowAssMatrices",isAllowAssMatrices);
			//end check permission

			// Get the server config object
			// ServerConfigurationService serverConfig = rubService.getServerconfigurationService();

			// // Get the eportfolio value from property file
			// String eportfolio = serverConfig.getString("irubric.eportfolio");

			// // Put the eportfolio value to show/hide permission
			// context.put("eportfolio", eportfolio);

		}	
		//set to .vm(use for check show/hide irubric tool)	
		context.put("allowShowiRubricLink",allowShowiRubricLink.toString());

		String mode = (String) state.getAttribute(STATE_MODE);
		
		//main page or my rubric or gallery or build
		if(MODE_MAIN_PAGE.equals(mode) || MODE_MY_RUBRIC.equals(mode) || 
				MODE_GALLERY.equals(mode) || 
				MODE_BUILD.equals(mode) || MODE_MY_EPORT.equals(mode) || 
				MODE_BUILD_EPORT.equals(mode) || MODE_IRUBRIC_HOME.equals(mode)) { //DN 2013-10-28: irubric home and eport
			
			if(!MODE_MAIN_PAGE.equals(mode))
				//set action
				context.put("purpose",mode);

			//template file .vm
			template = build_irubrics_instructor_list_manange_context(portlet, context, data, state);

		} else if (MODE_GRADEALL.equals(mode)) {
			//grade all page
			template = build_irubrics_instructor_grade_all_context(portlet, context, data, state);

			} 
			// else if (MODE_PERMISSION.equals(mode))
			// 		template = buildPermissionTemplate(portlet, context, data, state);

		return template;

	} // buildMainPanelContext
	
	/**
	 * build the main page
	 */
	protected String build_irubrics_instructor_list_manange_context(VelocityPortlet portlet, Context context, RunData data, SessionState state)
	{
		
		String template = (String) getContext(data).get("template");
		
		//main page
		return template + TEMPLATE_LIST_IRUBRICS_MANAGE;
	}
	
	/**
	 * build the view assignments list
	 */
	protected String build_irubrics_instructor_grade_all_context(VelocityPortlet portlet, Context context, RunData data, SessionState state) {
		//state.setAttribute(STATE_PAGESIZE, 5);
		String template = (String) getContext(data).get("template");

		//process paging and get list assignment in function sizeResources
		List assignments = prepPage(state);
			
		//use set hide link gradeall(when user is selected)	
		context.put("view", MODE_GRADEALL);

		//set into file .vm
		context.put("assignments", assignments.iterator());

		//set Info for paging
		pagingInfoToContext(state, context);

		//DN 2013-09-12: able to grade
		Boolean ableGrade = rubService.ableToGrade(gradebookUId);
		context.put("isAbleGrade", ableGrade.toString());

		context.put("userId", userId);

		return template + TEMPLATE_LIST_IRUBRICS_GRADEALL;
	}


	// protected String buildPermissionTemplate(VelocityPortlet portlet, Context context, RunData data, SessionState state) {
	
	// 	String template = (String) getContext(data).get("template");
		
	// 	return template + TEMPLATE_PERMISSION;
	// }


	protected int sizeResources(SessionState state) {
		String mode = (String) state.getAttribute(STATE_MODE);
		// all the resources for paging
		List returnResources = new ArrayList();

		// grade all
		if(MODE_GRADEALL.equals(mode)) {
			// sort them all
	
			String ascending = (String) state.getAttribute(SORTED_ASC);
			String sort = (String) state.getAttribute(SORTED_BY);
			
			//get gradebook items(is sorted)
			returnResources = rubService.getAssignmentsByGradebookUId(gradebookUId, sort, ascending);
		}

		// record the total item number
		state.setAttribute(STATE_PAGEING_TOTAL_ITEMS, returnResources);
		return returnResources.size();		
	}

	/**
	 * Implement this to return alist of all the resources that there are to page. Sort them as appropriate.
	 */
	protected List readResourcesPage(SessionState state, int first, int last) {
		List returnResources = (List) state.getAttribute(STATE_PAGEING_TOTAL_ITEMS);

		PagingPosition page = new PagingPosition(first, last);
		page.validate(returnResources.size());
		returnResources = returnResources.subList(page.getFirst() - 1, page.getLast());
		return returnResources;

	} // readAllResources
	

	/**
	 * Fire up the permissions editor
	 */
	public void doPermissions(RunData data) {
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		
		// check have allow update Permissions
		if (SiteService.allowUpdateSite((String) state.getAttribute(STATE_CONTEXT_STRING))) {

			//avoid error when open Permission page
            hackcode(state);
            
			// get into helper mode with this helper tool
			startHelper(data.getRequest(), "sakai.permissions.helper");

			String contextString = (String) state.getAttribute(STATE_CONTEXT_STRING);
			String siteRef = SiteService.siteReference(contextString);
			
			// setup for editing the permissions of the site for this tool, using the roles of this site, too
			state.setAttribute(PermissionsHelper.TARGET_REF, siteRef);

			// ... with this description
			state.setAttribute(PermissionsHelper.DESCRIPTION, rb.getString("permissions.description") + " "
					+ SiteService.getSiteDisplay(contextString));

			// ... showing only locks that are prpefixed with this
			state.setAttribute(PermissionsHelper.PREFIX, "irubric.");
			
			
			// ... pass the resource loader object
			ResourceLoader pRb = new ResourceLoader("permissions");
			HashMap<String, String> pRbValues = new HashMap<String, String>();
			//set file resource permissions.properties into Map
			for (Iterator<Map.Entry<String, Object>> iEntries = pRb.entrySet().iterator();iEntries.hasNext();) {
				Map.Entry<String, Object> entry = iEntries.next();

				// skip element in the permission page
				// the values get from persission .properties resources
				// if ("desc-irubric.assmatrices".equals(entry.getKey()) ||
				// 	"desc-irubric.buildeportfolios".equals(entry.getKey()) ||
				// 	"desc-irubric.eportfolios".equals(entry.getKey()))
				// 	continue;

				// else 
				pRbValues.put(entry.getKey(), (String) entry.getValue());
				
			}

			//set resource permission
			state.setAttribute("permissionDescriptions",  pRbValues);
		
		}

		// state.setAttribute(STATE_MODE, MODE_PERMISSION);
		// resetPaging(state);
	} // doPermissions
	
	public void doiRubricHome(RunData data) {
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		//set mode main page
		state.setAttribute(STATE_MODE,MODE_IRUBRIC_HOME);

	}

	public void doMyrubric(RunData data) {
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		//set mode main page
		state.setAttribute(STATE_MODE,MODE_MY_RUBRIC);

	}
	public void doGallery(RunData data) {
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		//set mode main page
		state.setAttribute(STATE_MODE,MODE_GALLERY);

	}
	
	public void doBuild(RunData data) {
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		//set mode main page
		state.setAttribute(STATE_MODE,MODE_BUILD);

	}
	
	/**
	 * Grade for all student of gradebook item which is attched rubric 
	 */
	public void doGradeAll(RunData data) {
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());
		//set mode gradeall
		state.setAttribute(STATE_MODE, MODE_GRADEALL);
		resetPaging(state);
	}

	/**
	 * Populate the state object, if needed - override to do something!
	 */
	protected void initState(SessionState state, VelocityPortlet portlet, JetspeedRunData data)
	{
		super.initState(state, portlet, data);

		// show the main page first
		if (state.getAttribute(STATE_MODE) == null)
		{
			state.setAttribute(STATE_MODE, MODE_MAIN_PAGE);
		}
		//use for paging
		if (state.getAttribute(STATE_TOP_PAGE_MESSAGE) == null)
		{
			state.setAttribute(STATE_TOP_PAGE_MESSAGE, Integer.valueOf(0));
		}

		//DN 2013-09-12: allow show irubric tool
		allowShowiRubricLink = rubService.allowShowiRubricLink();

		//get user id
		userId = StringUtil.trimToZero(SessionManager.getCurrentSessionUserId());
		/*
		if (state.getAttribute(STATE_USER) == null)
		{
			state.setAttribute(STATE_USER, UserDirectoryService.getCurrentUser());
		}*/
	}

	/**
	 * Sort based on the given property
	 */
	public void doSort(RunData data)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// we are changing the sort, so start from the first page again
		resetPaging(state);

		setupSort(data, data.getParameters().getString("criteria"));

	}

	/**
	 * setup sorting parameters
	 *
	 * @param criteria
	 *        String for sortedBy
	 */
	private void setupSort(RunData data, String criteria)
	{
		SessionState state = ((JetspeedRunData) data).getPortletSessionState(((JetspeedRunData) data).getJs_peid());

		// current sorting sequence
		String asc = "";
		if (!criteria.equals(state.getAttribute(SORTED_BY)))
		{
			state.setAttribute(SORTED_BY, criteria);
			asc = Boolean.TRUE.toString();
			state.setAttribute(SORTED_ASC, asc);
		}
		else
		{
			// current sorting sequence
			asc = (String) state.getAttribute(SORTED_ASC);

			// toggle between the ascending and descending sequence
			if (asc.equals(Boolean.TRUE.toString()))
			{
				asc = Boolean.FALSE.toString();
			}
			else
			{
				asc = Boolean.TRUE.toString();
			}
			state.setAttribute(SORTED_ASC, asc);
		}

	} // doSort

	/*
	* function hackcode 
	* Purpose: avoid error when open/save/cancel a Permission page
	* @param sessionstate
	*/
	private void hackcode(SessionState state){
		Alert alert = null;
        state.setAttribute(ALERT_ATTR, alert);
        Menu menu = null;
		state.setAttribute(MENU_ATTR,menu);
	}
	
	private String getToolPlacement(String contextId, String toolId) {
		String placement = null;

		Site site = null;

		try {
			site = SiteService.getSite(contextId);
		} catch (IdUnusedException e) {
			// TODO Auto-generated catch block
			M_log.info("Error: " + e);
		}

		ToolConfiguration tool = site.getToolForCommonId(toolId);
		if (tool != null) {
			placement = tool.getId();
		}

		return placement;
	}
}
