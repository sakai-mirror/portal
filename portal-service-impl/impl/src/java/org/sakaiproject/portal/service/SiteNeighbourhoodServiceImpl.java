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

package org.sakaiproject.portal.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;	

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.alias.api.Alias;
import org.sakaiproject.alias.api.AliasService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.portal.api.SiteNeighbour;
import org.sakaiproject.portal.api.SiteNeighbour.Relationship;
import org.sakaiproject.portal.api.SiteNeighbourhoodService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.user.api.Preferences;
import org.sakaiproject.user.api.PreferencesService;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

/**
 * @author ieb
 */
public class SiteNeighbourhoodServiceImpl implements SiteNeighbourhoodService
{

	private static final String SITE_ALIAS = "/sitealias/";

	private static final Log log = LogFactory.getLog(SiteNeighbourhoodServiceImpl.class);

	private SiteService siteService;

	private PreferencesService preferencesService;

	private UserDirectoryService userDirectoryService;

	private ServerConfigurationService serverConfigurationService;
	
	private AliasService aliasService;
	
	/** Should all site aliases have a prefix */
	private boolean useAliasPrefix = false;

	public void init()
	{

	}

	public void destroy()
	{

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.portal.api.SiteNeighbourhoodService#getSitesAtNode(javax.servlet.http.HttpServletRequest,
	 *      org.sakaiproject.tool.api.Session, boolean)
	 */
	public List<SiteNeighbour> getSitesAtNode(HttpServletRequest request, Session session, String context,
			boolean includeMyWorkspace)
	{
		return getAllSites(request, session, context, includeMyWorkspace);
	}

	/**
	 * Get All Sites for the current user. If the user is not logged in we
	 * return the list of publically viewable gateway sites.
	 * 
	 * @param includeMyWorkspace
	 *        When this is true - include the user's My Workspace as the first
	 *        parameter. If false, do not include the MyWorkspace anywhere in
	 *        the list. Some uses - such as the portlet styled portal or the rss
	 *        styled portal simply want all of the sites with the MyWorkspace
	 *        first. Other portals like the basic tabbed portal treats My
	 *        Workspace separately from all of the rest of the workspaces.
	 * @see org.sakaiproject.portal.api.PortalSiteHelper#getAllSites(javax.servlet.http.HttpServletRequest,
	 *      org.sakaiproject.tool.api.Session, boolean)
	 */
	public List<SiteNeighbour> getAllSites(HttpServletRequest req, Session session, String context,
			boolean includeMyWorkspace)
	{

		boolean loggedIn = session.getUserId() != null;
		List<Site> mySites;
		List<SiteNeighbour> ordered = new ArrayList<SiteNeighbour>();

		int distance = 0;
		
		try
		{
			Site currentSite = siteService.getSiteVisit(context);
			ordered.add(new SiteNeighbourImpl(currentSite, null, Relationship.CURRENT, 0));
		}
		catch (IdUnusedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (PermissionException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// collect the Publically Viewable Sites
		if (!loggedIn)
		{
			mySites = getGatewaySites();
		}
		else
		{

			// collect the user's sites
			mySites = siteService.getSites(
					org.sakaiproject.site.api.SiteService.SelectionType.ACCESS, null, null,
					null, org.sakaiproject.site.api.SiteService.SortType.TITLE_ASC, null);

			// collect the user's preferences
			List prefExclude = new ArrayList();
			List prefOrder = new ArrayList();
			if (session.getUserId() != null)
			{
				Preferences prefs = preferencesService.getPreferences(session.getUserId());
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

			// TODO Bad code as it relies on bad Site.equals()
			// remove all in exclude from mySites
			mySites.removeAll(prefExclude);

			// First, place or remove MyWorkspace as requested
			Site myWorkspace = getMyWorkspace(session);
			if (myWorkspace != null)
			{
				if (includeMyWorkspace)
				{
					ordered.add(new SiteNeighbourImpl(myWorkspace, null, SiteNeighbour.Relationship.MYWORKSITE, 0));
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
					// move it from mySites to order, ignoring child sites
					Site s = mySites.get(pos);
					String ourParent = s.getProperties().getProperty(SiteService.PROP_PARENT_ID);
					// System.out.println("Pref Site:"+s.getTitle()+"
					// parent="+ourParent);
					if (ourParent == null)
					{
						ordered.add(new SiteNeighbourImpl(s, null, SiteNeighbour.Relationship.MEMBER, distance++));
					}
				}
			}

			
		}
	
		for (Site site: mySites)
		{
			//if (added.contains(site.getId())) continue;
			String parent = site.getProperties().getProperty(SiteService.PROP_PARENT_ID);
			if (context.equals(parent))
			{
				ordered.add(new SiteNeighbourImpl(site, null, SiteNeighbour.Relationship.DOWN, 1));
			}
			else 
			{
				ordered.add(new SiteNeighbourImpl(site, null, SiteNeighbour.Relationship.MEMBER, distance));
			}
		}
		
		// Get the parents for this request
		ordered.addAll(getParents(context));

		return ordered;

	}

	private Collection<SiteNeighbour> getParents(String siteId)
	{

		Queue<SiteNeighbour> queue = new LinkedList<SiteNeighbour>();

		String currentSiteId = siteId;
			try
			{
				while (currentSiteId != null && queue.size() < 8) 
				{
					Site site = siteService.getSiteVisit(currentSiteId);
					if (queue.contains(site))
					{
						log.warn("Loop detected; Current Site: "+ site.getId()+ " Head Site: "+ siteId);
						break;
					}
					queue.add(new SiteNeighbourImpl(site, null, SiteNeighbour.Relationship.UP, queue.size()));
					currentSiteId = site.getProperties().getProperty(SiteService.PROP_PARENT_ID);
				}
			}
			catch (IdUnusedException iue)
			{
				log.debug("Couldn't find parent site: "+ currentSiteId);
			}
			catch (PermissionException pe)
			{
				log.debug("Current user doesn't have access to: "+ currentSiteId);
			}
			return queue;
		}

	
	// Get the sites which are to be displayed for the gateway
	/**
	 * @return
	 */
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

	/**
	 * @see org.sakaiproject.portal.api.PortalSiteHelper#getMyWorkspace(org.sakaiproject.tool.api.Session)
	 */
	private Site getMyWorkspace(Session session)
	{
		String siteId = siteService.getUserSiteId(session.getUserId());

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
	private int listIndexOf(String value, List<Site> siteList)
	{
		for (int i = 0; i < siteList.size(); i++)
		{
			Site site = siteList.get(i);
			if (site.equals(value))
			{
				return i;
			}
		}

		return -1;
	}

	// Return the list of tabs for the anonymous view (Gateway)
	// If we have a list of sites, return that - if not simply pull in the
	// single
	// Gateway site
	/**
	 * @return
	 */
	private String[] getGatewaySiteList()
	{
		String gatewaySiteListPref = serverConfigurationService
				.getString("gatewaySiteList");

		if (gatewaySiteListPref == null || gatewaySiteListPref.trim().length() < 1)
		{
			gatewaySiteListPref = serverConfigurationService.getGatewaySiteId();
		}
		if (gatewaySiteListPref == null || gatewaySiteListPref.trim().length() < 1)
			return null;

		String[] gatewaySites = gatewaySiteListPref.split(",");
		if (gatewaySites.length < 1) return null;

		return gatewaySites;
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
			return siteService.getSiteVisit(siteId);
		}
		catch (IdUnusedException e)
		{
			if (siteService.isUserSite(siteId))
			{
				try
				{
					String userEid = siteService.getSiteUserId(siteId);
					String userId = userDirectoryService.getUserId(userEid);
					String alternateSiteId = siteService.getUserSiteId(userId);
					return siteService.getSiteVisit(alternateSiteId);
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
	 * @return the preferencesService
	 */
	public PreferencesService getPreferencesService()
	{
		return preferencesService;
	}

	/**
	 * @param preferencesService
	 *        the preferencesService to set
	 */
	public void setPreferencesService(PreferencesService preferencesService)
	{
		this.preferencesService = preferencesService;
	}

	/**
	 * @return the serverConfigurationService
	 */
	public ServerConfigurationService getServerConfigurationService()
	{
		return serverConfigurationService;
	}

	/**
	 * @param serverConfigurationService
	 *        the serverConfigurationService to set
	 */
	public void setServerConfigurationService(
			ServerConfigurationService serverConfigurationService)
	{
		this.serverConfigurationService = serverConfigurationService;
	}

	/**
	 * @return the siteService
	 */
	public SiteService getSiteService()
	{
		return siteService;
	}

	/**
	 * @param siteService
	 *        the siteService to set
	 */
	public void setSiteService(SiteService siteService)
	{
		this.siteService = siteService;
	}

	/**
	 * @return the userDirectoryService
	 */
	public UserDirectoryService getUserDirectoryService()
	{
		return userDirectoryService;
	}

	/**
	 * @param userDirectoryService
	 *        the userDirectoryService to set
	 */
	public void setUserDirectoryService(UserDirectoryService userDirectoryService)
	{
		this.userDirectoryService = userDirectoryService;
	}

	public String lookupSiteAlias(String id, String context)
	{
		List<Alias> aliases = aliasService.getAliases(id);
		if (aliases.size() > 0) 
		{
			if (aliases.size() > 1 && log.isInfoEnabled())
			{
				if (log.isDebugEnabled())
				{
					log.debug("More than one alias for "+ id+ " sorting.");
				}
				Collections.sort(aliases, new Comparator<Alias>()
				{
					public int compare(Alias o1, Alias o2)
					{
						return o1.getId().compareTo(o2.getId());
					}
					
				});
			}
			for (Alias alias : aliases)
			{
				String aliasId = alias.getId();
				boolean startsWithPrefix = aliasId.startsWith(SITE_ALIAS);
				if (startsWithPrefix)
				{
					if (useAliasPrefix)
					{
						return aliasId.substring(SITE_ALIAS.length());
					}
				}
				else
				{
					if (!useAliasPrefix)
					{
						return aliasId;
					}
				}
			}
		}
		return null;
	}

	public String parseSiteAlias(String url)
	{
		String id = ((useAliasPrefix)?SITE_ALIAS:"")+url;
		try
		{
			String reference = aliasService.getTarget(id);
			return reference;
		}
		catch (IdUnusedException e)
		{
			if (log.isDebugEnabled())
			{
				log.debug("No alias found for "+ id);
			}
		}
		return null;
	}

	public void setAliasService(AliasService aliasService) {
		this.aliasService = aliasService;
	}

	public boolean isUseAliasPrefix()
	{
		return useAliasPrefix;
	}

	public void setUseAliasPrefix(boolean useAliasPrefix)
	{
		this.useAliasPrefix = useAliasPrefix;
	}
	

}