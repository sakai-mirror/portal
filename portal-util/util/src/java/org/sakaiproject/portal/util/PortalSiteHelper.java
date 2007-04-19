/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 The Sakai Foundation.
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.portal.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.announcement.cover.AnnouncementService;
import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.discussion.cover.DiscussionService;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.EntitySummary;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.api.Summary;
import org.sakaiproject.entity.cover.EntityManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.thread_local.cover.ThreadLocalManager;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.user.api.Preferences;
import org.sakaiproject.user.api.UserNotDefinedException;
import org.sakaiproject.user.cover.PreferencesService;
import org.sakaiproject.user.cover.UserDirectoryService;
import org.sakaiproject.util.ArrayUtil;
import org.sakaiproject.util.MapUtil;

/**
 * @author ieb
 * @since Sakai 2.4
 * @version $Rev$
 */

public class PortalSiteHelper
{

	private static final Log log = LogFactory.getLog(PortalSiteHelper.class);

	// Determine if we are to do multiple tabs for the anonymous view (Gateway)
	public boolean doGatewaySiteList()
	{
		String gatewaySiteListPref = ServerConfigurationService
				.getString("gatewaySiteList");
		if (gatewaySiteListPref == null) return false;
		return (gatewaySiteListPref.trim().length() > 0);
	}

	// Return the list of tabs for the anonymous view (Gateway)
	// If we have a list of sites, return that - if not simply pull in the
	// single
	// Gateway site
	private String[] getGatewaySiteList()
	{
		String gatewaySiteListPref = ServerConfigurationService
				.getString("gatewaySiteList");

		if (gatewaySiteListPref == null || gatewaySiteListPref.trim().length() < 1)
		{
			gatewaySiteListPref = ServerConfigurationService.getGatewaySiteId();
		}
		if (gatewaySiteListPref == null || gatewaySiteListPref.trim().length() < 1)
			return null;

		String[] gatewaySites = gatewaySiteListPref.split(",");
		if (gatewaySites.length < 1) return null;

		return gatewaySites;
	}

	// Get the sites which are to be displayed for the gateway
	private List<Site> getGatewaySites()
	{
		List<Site> mySites = new ArrayList<Site>();
		String[] gatewaySiteIds = getGatewaySiteList();
		if (gatewaySiteIds == null)
		{
			return mySites; // An empty list - deal with this higher up in the
			// food chain
		}

		// Loop throught the sites making sure they exist and are visitable
		for (int i = 0; i < gatewaySiteIds.length; i++)
		{
			String siteId = gatewaySiteIds[i];

			Site site = null;
			try
			{
				site = getSiteVisit(siteId);
			}
			catch (IdUnusedException e)
			{
				continue;
			}
			catch (PermissionException e)
			{
				continue;
			}

			if (site != null)
			{
				mySites.add(site);
			}
		}

		if (mySites.size() < 1)
		{
			log.warn("No suitable gateway sites found, gatewaySiteList preference had "
					+ gatewaySiteIds.length + " sites.");
		}
		return mySites;
	}

	/*
	 * Get All Sites for the current user. If the user is not logged in we
	 * return the list of publically viewable gateway sites. @param
	 * includeMyWorkspace When this is true - include the user's My Workspace as
	 * the first parameter. If false, do not include the MyWorkspace anywhere in
	 * the list. Some uses - such as the portlet styled portal or the rss styled
	 * portal simply want all of the sites with the MyWorkspace first. Other
	 * portals like the basic tabbed portal treats My Workspace separately from
	 * all of the rest of the workspaces.
	 */

	public List<Site> getAllSites(HttpServletRequest req, Session session,
			boolean includeMyWorkspace) throws IOException
	{

		boolean loggedIn = session.getUserId() != null;

		// Get the list of sites in the right order
		List<Site> mySites;
		if (!loggedIn)
		{
			// collect the Publically Viewable Sites
			mySites = getGatewaySites();
		}
		else
		{
			// collect the user's sites
			mySites = SiteService.getSites(
					org.sakaiproject.site.api.SiteService.SelectionType.ACCESS, null,
					null, null, org.sakaiproject.site.api.SiteService.SortType.TITLE_ASC,
					null);

			// collect the user's preferences
			List prefExclude = new ArrayList();
			List prefOrder = new ArrayList();
			if (session.getUserId() != null)
			{
				Preferences prefs = PreferencesService
						.getPreferences(session.getUserId());
				ResourceProperties props = prefs.getProperties("sakai:portal:sitenav");

				List l = props.getPropertyList("exclude");
				if (l != null)
				{
					prefExclude = l;
				}

				l = props.getPropertyList("order");
				if (l != null)
				{
					prefOrder = l;
				}
			}

			// remove all in exclude from mySites
			mySites.removeAll(prefExclude);

			// Prepare to put sites in the right order
			List<Site> ordered = new ArrayList<Site>();

			// First, place or remove MyWorkspace as requested
			Site myWorkspace = getMyWorkspace(session);
			if (myWorkspace != null)
			{
				if (includeMyWorkspace)
				{
					ordered.add(myWorkspace);
				}
				else
				{
					int pos = listIndexOf(myWorkspace.getId(), mySites);
					if (pos != -1) mySites.remove(pos);
				}
			}

			// re-order mySites to have order first, the rest later
			for (Iterator i = prefOrder.iterator(); i.hasNext();)
			{
				String id = (String) i.next();

				// find this site in the mySites list
				int pos = listIndexOf(id, mySites);
				if (pos != -1)
				{
					// move it from mySites to order
					Site s = mySites.get(pos);
					ordered.add(s);
					mySites.remove(pos);
				}
			}

			// pick up the rest of the sites
			ordered.addAll(mySites);
			mySites = ordered;
		} // End if ( loggedIn )

		/*
		 * for (Iterator i = mySites.iterator(); i.hasNext();) { Site s = (Site)
		 * i.next(); System.out.println("Site:"+Web.escapeHtml(s.getTitle())+"
		 * id="+s.getId()); }
		 */
		return mySites;
	}

	public Site getMyWorkspace(Session session)
	{
		String siteId = SiteService.getUserSiteId(session.getUserId());

		// Make sure we can visit
		Site site = null;
		try
		{
			site = getSiteVisit(siteId);
		}
		catch (IdUnusedException e)
		{
			site = null;
		}
		catch (PermissionException e)
		{
			site = null;
		}

		return site;
	}

	protected final static String CURRENT_PLACEMENT = "sakai:ToolComponent:current.placement";

	/*
	 * Temporarily set a placement with the site id as the context - we do not
	 * set a tool ID this will not be a rich enough placement to do *everything*
	 * but for those services which call
	 * ToolManager.getCurrentPlacement().getContext() to contextualize their
	 * information - it wil be sufficient.
	 */

	public boolean setTemporaryPlacement(Site site)
	{
		if (site == null) return false;

		Placement ppp = ToolManager.getCurrentPlacement();
		if (ppp != null && site.getId().equals(ppp.getContext()))
		{
			return true;
		}

		// Create a site-only placement
		Placement placement = new org.sakaiproject.util.Placement("portal-temporary", /* toolId */
		null, /* tool */null,
		/* config */null, /* context */site.getId(), /* title */null);

		ThreadLocalManager.set(CURRENT_PLACEMENT, placement);

		// Debugging
		ppp = ToolManager.getCurrentPlacement();
		if (ppp == null)
		{
			System.out.println("WARNING portal-temporary placement not set - null");
		}
		else
		{
			String cont = ppp.getContext();
			if (site.getId().equals(cont))
			{
				return true;
			}
			else
			{
				System.out.println("WARNING portal-temporary placement mismatch site="
						+ site.getId() + " context=" + cont);
			}
		}
		return false;
	}

	public boolean summarizePage(Map m, Site site, SitePage page)
	{
		List pTools = page.getTools();
		Iterator iPt = pTools.iterator();
		while (iPt.hasNext())
		{
			ToolConfiguration placement = (ToolConfiguration) iPt.next();

			if (summarizeTool(m, site, placement.getToolId()))
			{
				return true;
			}
		}
		return false;
	}

	/*
	 * There must be a better way of doing this as this hard codes the services
	 * in surely there should be some whay of looking up the serivce and making
	 * the getSummary part of an interface. TODO: Add an interface beside
	 * EntityProducer to generate summaries Make this discoverable
	 */
	public boolean summarizeTool(Map m, Site site, String toolIdentifier)
	{
		if (site == null) return false;

		setTemporaryPlacement(site);
		Map newMap = null;
		if ("sakai.motd".equals(toolIdentifier))
		{
			try
			{
				String channel = "/announcement/channel/!site/motd";
				newMap = AnnouncementService.getSummary(channel, 5, 30);
			}
			catch (Exception e)
			{
				newMap = null;
			}
		}
		/*
		 * else if ("sakai.chat".equals(toolIdentifier)) { //This is now being
		 * done by the EntitySummary stuff - chmaurer 3/2007 try { String
		 * channel = ChatService.getInstance().channelReference(site.getId(),
		 * SiteService.MAIN_CONTAINER); newMap = ChatService.getSummary(channel,
		 * 5, 30); } catch (Exception e) { newMap = null; } }
		 */
		else if ("sakai.discussion".equals(toolIdentifier))
		{
			try
			{
				String channel = DiscussionService.getInstance().channelReference(
						site.getId(), SiteService.MAIN_CONTAINER);
				newMap = DiscussionService.getSummary(channel, 5, 30);
			}
			catch (Exception e)
			{
				newMap = null;
			}
		}
		else if ("sakai.announcements".equals(toolIdentifier))
		{
			try
			{
				String channel = AnnouncementService.getInstance().channelReference(
						site.getId(), SiteService.MAIN_CONTAINER);
				newMap = AnnouncementService.getSummary(channel, 5, 30);
			}
			catch (Exception e)
			{
				newMap = null;
			}
		}

		/*
		 * This is a new, cooler way to do this (I hope) chmaurer...
		 */

		// offer to all EntityProducers
		for (Iterator i = EntityManager.getEntityProducers().iterator(); i.hasNext();)
		{
			EntityProducer ep = (EntityProducer) i.next();
			if (ep instanceof EntitySummary)
			{
				try
				{
					EntitySummary es = (EntitySummary) ep;

					// if this producer claims this tool id
					if (ArrayUtil.contains(es.summarizableToolIds(), toolIdentifier))
					{
						String summarizableReference = es.getSummarizableReference(site
								.getId());
						es.getSummary(summarizableReference, 5, 30);
					}
				}
				catch (Throwable t)
				{
					log.warn(
							"Error encountered while asking EntitySummary to getSummary() for: "
									+ toolIdentifier, t);
				}
			}
		}

		if (newMap != null)
		{
			return (MapUtil.copyHtml(m, "rssDescription", newMap,
					Summary.PROP_DESCRIPTION) && MapUtil.copy(m, "rssPubdate", newMap,
					Summary.PROP_PUBDATE));
		}
		else
		{
			m.put("rssPubDate", (site.getModifiedTime().toStringRFC822Local()));
			return false;
		}

	}

	/**
	 * If this is a user site, return an id based on the user EID, otherwise
	 * just return the site id.
	 * 
	 * @param site
	 *        The site.
	 * @return The effective site id.
	 */
	public String getSiteEffectiveId(Site site)
	{
		if (SiteService.isUserSite(site.getId()))
		{
			try
			{
				String userId = SiteService.getSiteUserId(site.getId());
				String eid = UserDirectoryService.getUserEid(userId);
				return SiteService.getUserSiteId(eid);
			}
			catch (UserNotDefinedException e)
			{
				log.warn("getSiteEffectiveId: user eid not found for user site: "
						+ site.getId());
			}
		}

		return site.getId();
	}

	/**
	 * Do the getSiteVisit, but if not found and the id is a user site, try
	 * translating from user EID to ID.
	 * 
	 * @param siteId
	 *        The Site Id.
	 * @return The Site.
	 * @throws PermissionException
	 *         If not allowed.
	 * @throws IdUnusedException
	 *         If not found.
	 */
	public Site getSiteVisit(String siteId) throws PermissionException, IdUnusedException
	{
		try
		{
			return SiteService.getSiteVisit(siteId);
		}
		catch (IdUnusedException e)
		{
			if (SiteService.isUserSite(siteId))
			{
				try
				{
					String userEid = SiteService.getSiteUserId(siteId);
					String userId = UserDirectoryService.getUserId(userEid);
					String alternateSiteId = SiteService.getUserSiteId(userId);
					return SiteService.getSiteVisit(alternateSiteId);
				}
				catch (UserNotDefinedException ee)
				{
				}
			}

			// re-throw if that didn't work
			throw e;
		}
	}

	/**
	 * Find the site in the list that has this id - return the position.
	 * 
	 * @param value
	 *        The site id to find.
	 * @param siteList
	 *        The list of Site objects.
	 * @return The index position in siteList of the site with site id = value,
	 *         or -1 if not found.
	 */
	private int listIndexOf(String value, List siteList)
	{
		for (int i = 0; i < siteList.size(); i++)
		{
			Site site = (Site) siteList.get(i);
			if (site.equals(value))
			{
				return i;
			}
		}

		return -1;
	}

	public boolean allowTool(Site site, Placement placement)
	{
		// No way to render an opinion
		if (placement == null || site == null) return true;

		// The site owner sees all pages !
		if (SecurityService.unlock(SiteService.SECURE_UPDATE_SITE, site.getReference()))
		{
			return true;
		}

		boolean retval = true;

		String TOOL_CFG_FUNCTIONS = "functions.require";
		Properties roleConfig = placement.getConfig();
		String roleList = roleConfig.getProperty(TOOL_CFG_FUNCTIONS);

		// allow by default, when no config keys are present
		if (roleList != null && roleList.trim().length() > 0)
		{
			String[] result = roleConfig.getProperty(TOOL_CFG_FUNCTIONS).split("\\,");
			for (int x = 0; x < result.length; x++)
			{
				if (!SecurityService.unlock(result[x].trim(), site.getReference()))
					retval = false;
			}
		}
		return retval;
	}

	/*
	 * Retrieve the list of pages in this site, checking to see if the user has
	 * permission to see the page - by checking the permissions of tools on the
	 * page.
	 */

	// TODO: Move this into Site
	public List getPermittedPagesInOrder(Site site)
	{
		// Get all of the pages
		List pages = site.getOrderedPages();

		// The site owner sees all pages !
		if (SecurityService.unlock(SiteService.SECURE_UPDATE_SITE, site.getReference()))
		{
			return pages;
		}

		List newPages = new ArrayList();

		for (Iterator i = pages.iterator(); i.hasNext();)
		{
			// check if current user has permission to see page
			SitePage p = (SitePage) i.next();
			List pTools = p.getTools();
			Iterator iPt = pTools.iterator();

			boolean allowPage = false;
			while (iPt.hasNext())
			{
				ToolConfiguration placement = (ToolConfiguration) iPt.next();

				boolean thisTool = allowTool(site, placement);
				if (thisTool) allowPage = true;
			}
			if (allowPage) newPages.add(p);
		}
		return newPages;
	}
}
