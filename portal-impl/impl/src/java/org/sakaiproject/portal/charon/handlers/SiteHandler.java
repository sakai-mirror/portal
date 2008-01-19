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

package org.sakaiproject.portal.charon.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.alias.cover.AliasService;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.entity.cover.EntityManager;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.portal.api.Portal;
import org.sakaiproject.portal.api.PortalHandlerException;
import org.sakaiproject.portal.api.PortalRenderContext;
import org.sakaiproject.portal.api.SiteView;
import org.sakaiproject.portal.api.StoredState;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.ToolException;
import org.sakaiproject.user.api.Preferences;
import org.sakaiproject.user.cover.PreferencesService;
import org.sakaiproject.util.Web;

/**
 * 
 * @author ieb
 * @since Sakai 2.4
 * @version $Rev$
 * 
 */
public class SiteHandler extends WorksiteHandler
{

	private static final String INCLUDE_SITE_NAV = "include-site-nav";

	private static final String INCLUDE_LOGO = "include-logo";

	private static final String INCLUDE_TABS = "include-tabs";

	private static final Log log = LogFactory.getLog(SiteHandler.class);

	private int configuredTabsToDisplay = 5;

	private boolean useDHTMLMore = false;

	public SiteHandler()
	{
		urlFragment = "site";
        configuredTabsToDisplay  = ServerConfigurationService.getInt(Portal.CONFIG_DEFAULT_TABS, 5);
        useDHTMLMore =  Boolean.valueOf(ServerConfigurationService.getBoolean("portal.use.dhtml.more", false));
	}


	
	@Override
	public int doGet(String[] parts, HttpServletRequest req, HttpServletResponse res,
			Session session) throws PortalHandlerException
	{
		if ((parts.length >= 2) && (parts[1].equals("site")))
		{
			// This is part of the main portal so we simply remove the attribute
			session.setAttribute("sakai-controlling-portal",null);
			try
			{
				// recognize an optional page/pageid
				String pageId = null;
				if ((parts.length == 5) && (parts[3].equals("page")))
				{
					pageId = parts[4];
				}

				// site might be specified
				String siteId = null;
				if (parts.length >= 3)
				{
					siteId = parts[2];
				}

				doSite(req, res, session, siteId, pageId, req.getContextPath()
						+ req.getServletPath());
				return END;
			}
			catch (Exception ex)
			{
				throw new PortalHandlerException(ex);
			}
		}
		else
		{
			return NEXT;
		}
	}

	public void doSite(HttpServletRequest req, HttpServletResponse res, Session session,
			String siteId, String pageId, String toolContextPath) throws ToolException,
			IOException
	{

		boolean doFrameTop = "true".equals(req.getParameter("sakai.frame.top"));
		boolean doFrameSuppress = "true".equals(req.getParameter("sakai.frame.suppress"));

		// default site if not set
		if (siteId == null)
		{
			if (session.getUserId() == null)
			{
				siteId = ServerConfigurationService.getGatewaySiteId();
			}
			else
			{
				siteId = SiteService.getUserSiteId(session.getUserId());
			}
		}

		// if no page id, see if there was a last page visited for this site
		// if we are coming back from minimized navigation - go to the default tool
		// Not the previous tool
		if (pageId == null && ! doFrameSuppress )
		{
			pageId = (String) session.getAttribute(Portal.ATTR_SITE_PAGE + siteId);
		}

		// find the site, for visiting
		Site site = null;
		try
		{
			site = portal.getSiteHelper().getSiteVisit(siteId);
		}
		catch (IdUnusedException e)
		{
			// continue on to alias check
		}
		catch (PermissionException e)
		{
			// if not logged in, give them a chance
			if (session.getUserId() == null)
			{
				StoredState ss = portalService.newStoredState("directtool", "tool");
				ss.setRequest(req);
				ss.setToolContextPath(toolContextPath);
				portalService.setStoredState(ss);
				portal.doLogin(req, res, session, req.getPathInfo(), false);
				return;
			}
			// otherwise continue on to alias check
		}

		// Now check for site alias
		if ( site == null )
		{
			try
			{
				// First check for site alias
				if ( siteId!= null && !siteId.equals("") && !SiteService.siteExists(siteId) )
				{
					String refString = AliasService.getTarget(siteId);
					siteId = EntityManager.newReference(refString).getContainer();
				}
				
				site = portal.getSiteHelper().getSiteVisit(siteId);
			}
			catch (IdUnusedException e)
			{
				portal.doError(req, res, session, Portal.ERROR_SITE);
				return;
			}
			catch (PermissionException e)
			{
				// if not logged in, give them a chance
				if (session.getUserId() == null)
				{
					StoredState ss = portalService.newStoredState("directtool", "tool");
					ss.setRequest(req);
					ss.setToolContextPath(toolContextPath);
					portalService.setStoredState(ss);
					portal.doLogin(req, res, session, req.getPathInfo(), false);
				}
				else
				{
					portal.doError(req, res, session, Portal.ERROR_SITE);
				}
				return;
			}
		}

		// Try to lookup alias if pageId not found
		if (pageId != null && !pageId.equals("") && site.getPage(pageId) == null)
		{
			try
			{
				String refString = AliasService.getTarget(pageId);
				pageId = EntityManager.newReference(refString).getId();
			}
			catch (IdUnusedException e) {
				log.debug("Alias does not resolve "+e.getMessage());
			}
		}
		
		// Lookup the page in the site - enforcing access control
		// business rules
		SitePage page = portal.getSiteHelper().lookupSitePage(pageId, site);
		if (page == null)
		{
			portal.doError(req, res, session, Portal.ERROR_SITE);
			return;
		}

		// store the last page visited
		session.setAttribute(Portal.ATTR_SITE_PAGE + siteId, page.getId());

		// form a context sensitive title
		String title = ServerConfigurationService.getString("ui.service") + " : "
				+ site.getTitle() + " : " + page.getTitle();

		// start the response
		String siteType = portal.calcSiteType(siteId);
		PortalRenderContext rcontext = portal.startPageContext(siteType, title, site
				.getSkin(), req);

		// the 'full' top area
		includeSiteNav(rcontext, req, session, siteId);

		if ( ! doFrameTop ) 
		{
			includeWorksite(rcontext, res, req, session, site, page, toolContextPath, "site");

			// Include sub-sites if appropriate
			// TODO: Thing through whether we want reset tools or not
			portal.includeSubSites(rcontext, req, session,
				siteId,  req.getContextPath() + req.getServletPath(), "site",
				/* resetTools */ false );

			portal.includeBottom(rcontext);
		}

		rcontext.put("currentUrlPath",Web.serverUrl(req) + req.getContextPath() + req.getPathInfo());

		// Indicate that no matter what - we are to suppress the use of the top frame
		// This allows us to generate a link where we see the tool buttons - this is
		// set on site URLs when in the frame top frame
		rcontext.put("sakaiFrameSuppress",req.getParameter("sakai.frame.suppress"));

		// TODO: Make behavior conditional on a property - Move this to includeTool
		// Retrieve the maximized URL and clear it from the global session
		String maximizedUrl = (String) session.getAttribute("sakai-maximized-url");
		if (maximizedUrl != null ) rcontext.put("frameMaximizedUrl",maximizedUrl);
		session.setAttribute("sakai-maximized-url",null);

		// end the response
		if ( doFrameTop ) 
		{
			// Place the proper values in context for the Frame Top panel
			rcontext.put("sakaiFrameEdit",req.getParameter("sakai.frame.edit"));
			rcontext.put("sakaiFrameTitle",req.getParameter("sakai.frame.title"));
			rcontext.put("sakaiFrameReset",req.getParameter("sakai.frame.reset"));
			rcontext.put("sakaiFramePortlet",req.getParameter("sakai.frame.portlet"));
			rcontext.put("sakaiSinglePage",req.getParameter("sakai.frame.single.page"));

			portal.sendResponse(rcontext, res, "site-frame-top", null);
		}
		else
		{
			portal.sendResponse(rcontext, res, "site", null);
		}

		StoredState ss = portalService.getStoredState();
		if (ss != null && toolContextPath.equals(ss.getToolContextPath()))
		{
			// This request is the destination of the request
			portalService.setStoredState(null);
		}
	}

	protected void includeSiteNav(PortalRenderContext rcontext, HttpServletRequest req,
			Session session, String siteId)
	{
		if (rcontext.uses(INCLUDE_SITE_NAV))
		{

			boolean loggedIn = session.getUserId() != null;
			boolean topLogin = ServerConfigurationService.getBoolean("top.login", true);

			String siteNavUrl = null;
			int height = 0;
			String siteNavClass = null;

			if (loggedIn)
			{
				siteNavUrl = Web.returnUrl(req, "/site_tabs/" + Web.escapeUrl(siteId));
				height = 104;
				siteNavClass = "sitenav-max";
			}
			else
			{
				siteNavUrl = Web.returnUrl(req, "/nav_login/" + Web.escapeUrl(siteId));
				height = 80;
				siteNavClass = "sitenav-log";
			}

			String accessibilityURL = ServerConfigurationService
					.getString("accessibility.url");
			rcontext.put("siteNavHasAccessibilityURL", Boolean
					.valueOf((accessibilityURL != null && accessibilityURL != "")));
			rcontext.put("siteNavAccessibilityURL", accessibilityURL);
			// rcontext.put("siteNavSitAccessability",
			// Web.escapeHtml(rb.getString("sit_accessibility")));
			// rcontext.put("siteNavSitJumpContent",
			// Web.escapeHtml(rb.getString("sit_jumpcontent")));
			// rcontext.put("siteNavSitJumpTools",
			// Web.escapeHtml(rb.getString("sit_jumptools")));
			// rcontext.put("siteNavSitJumpWorksite",
			// Web.escapeHtml(rb.getString("sit_jumpworksite")));

			rcontext.put("siteNavLoggedIn", Boolean.valueOf(loggedIn));

			try
			{
				if (loggedIn)
				{
					includeLogo(rcontext, req, session, siteId);
					includeTabs(rcontext, req, session, siteId, "site", false);
				}
				else
				{
					includeLogo(rcontext, req, session, siteId);
					if (portal.getSiteHelper().doGatewaySiteList())
						includeTabs(rcontext, req, session, siteId, "site", false);
				}
			}
			catch (Exception any)
			{
			}
		}
	}

	public void includeLogo(PortalRenderContext rcontext, HttpServletRequest req,
			Session session, String siteId) throws IOException
	{
		if (rcontext.uses(INCLUDE_LOGO))
		{

			String skin = getSiteSkin(siteId);

			if (skin == null)
			{
				skin = ServerConfigurationService.getString("skin.default");
			}
			String skinRepo = ServerConfigurationService.getString("skin.repo");
			rcontext.put("logoSkin", skin);
			rcontext.put("logoSkinRepo", skinRepo);
			String siteType = portal.calcSiteType(siteId);
			String cssClass = (siteType != null) ? siteType : "undeterminedSiteType";
			rcontext.put("logoSiteType", siteType);
			rcontext.put("logoSiteClass", cssClass);
			portal.includeLogin(rcontext, req, session);
		}
	}

	private String getSiteSkin(String siteId)
	{
		// First, try to get the skin the default way
		String skin = SiteService.getSiteSkin(siteId);
		// If this fails, try to get the real site id if the site is a user site
		if (skin == null && SiteService.isUserSite(siteId))
		{
			try
			{
				String userId = SiteService.getSiteUserId(siteId);
				String alternateSiteId = SiteService.getUserSiteId(userId);
				skin = SiteService.getSiteSkin(alternateSiteId);
			}
			catch (Exception e)
			{
				// Ignore
			}
		}
		return skin;
	}

	public void includeTabs(PortalRenderContext rcontext, HttpServletRequest req,
			Session session, String siteId, String prefix, boolean addLogout)
			throws IOException
	{

		if (rcontext.uses(INCLUDE_TABS))
		{

			// for skinning
			String siteType = portal.calcSiteType(siteId);
			String origPrefix = prefix;

			// If we have turned on auto-state reset on navigation, we generate
			// the "site-reset" "worksite-reset" and "gallery-reset" urls
			if ("true".equals(ServerConfigurationService
					.getString(Portal.CONFIG_AUTO_RESET)))
			{
				prefix = prefix + "-reset";
			}

			boolean loggedIn = session.getUserId() != null;

            int tabsToDisplay = configuredTabsToDisplay;

			
			
			
			
			
			if (!loggedIn)
			{
				tabsToDisplay = ServerConfigurationService.getInt(
						"gatewaySiteListDisplayCount", tabsToDisplay);
			}
			else
			{
				Preferences prefs = PreferencesService
						.getPreferences(session.getUserId());
				ResourceProperties props = prefs.getProperties("sakai:portal:sitenav");
				try
				{
					tabsToDisplay = (int) props.getLongProperty("tabs");
				}
				catch (Exception any)
				{
				}
			}
			
			
			rcontext.put("useDHTMLMore", useDHTMLMore);
			if ( useDHTMLMore ) {
				SiteView siteView = portal.getSiteHelper().getSitesView(SiteView.View.DHTML_MORE_VIEW, req, session, siteId);
				siteView.setPrefix(prefix);
				siteView.setToolContextPath(null);
				rcontext.put("tabsSites", siteView.getRenderContextObject());
			} else {
				SiteView siteView = portal.getSiteHelper().getSitesView(SiteView.View.DEFAULT_SITE_VIEW, req, session, siteId);
				siteView.setPrefix(prefix);
				siteView.setToolContextPath(null);
				rcontext.put("tabsSites", siteView.getRenderContextObject());
			}

			
			String cssClass = (siteType != null) ? "siteNavWrap " + siteType
					: "siteNavWrap";

			rcontext.put("tabsCssClass", cssClass);

         
			rcontext.put("tabsAddLogout", Boolean.valueOf(addLogout));
			if (addLogout)
			{
				String logoutUrl = Web.serverUrl(req)
						+ ServerConfigurationService.getString("portalPath")
						+ "/logout_gallery";
				rcontext.put("tabsLogoutUrl", logoutUrl);
				// rcontext.put("tabsSitLog",
				// Web.escapeHtml(rb.getString("sit_log")));
			}

			
		
			
			
			
		

			rcontext.put("tabsCssClass", cssClass);

			rcontext.put("tabsAddLogout", Boolean.valueOf(addLogout));
			if (addLogout)
			{
				String logoutUrl = Web.serverUrl(req)
						+ ServerConfigurationService.getString("portalPath")
						+ "/logout_gallery";
				rcontext.put("tabsLogoutUrl", logoutUrl);
				// rcontext.put("tabsSitLog",
				// Web.escapeHtml(rb.getString("sit_log")));
			}
		}
	}

}
