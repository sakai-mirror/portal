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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.portal.api.Portal;
import org.sakaiproject.portal.api.PortalHandlerException;
import org.sakaiproject.portal.api.PortalRenderContext;
import org.sakaiproject.portal.api.StoredState;
import org.sakaiproject.portal.util.PortalSiteHelper;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SitePage;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.ToolException;

/**
 * 
 * @author ieb
 * @since Sakai 2.4
 * @version $Rev$
 * 
 */
public class PageHandler extends BasePortalHandler
{

	private static final String INCLUDE_PAGE = "include-page";

	private static final Log log = LogFactory.getLog(PageHandler.class);

	protected PortalSiteHelper siteHelper = new PortalSiteHelper();

	public PageHandler()
	{
		urlFragment = "page";
	}

	@Override
	public int doPost(String[] parts, HttpServletRequest req, HttpServletResponse res,
			Session session) throws PortalHandlerException
	{
		return doGet(parts, req, res, session);
	}

	@Override
	public int doGet(String[] parts, HttpServletRequest req, HttpServletResponse res,
			Session session) throws PortalHandlerException
	{
		if ((parts.length == 3) && (parts[1].equals("page")))
		{
			try
			{
				// Resolve the placements of the form
				// /portal/page/sakai.resources?sakai.site=~csev
				String pagePlacement = portal.getPlacement(req, res, session, parts[2],
						true);
				if (pagePlacement == null)
				{
					return ABORT;
				}
				parts[2] = pagePlacement;

				doPage(req, res, session, parts[2], req.getContextPath()
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

	public void doPage(HttpServletRequest req, HttpServletResponse res, Session session,
			String pageId, String toolContextPath) throws ToolException, IOException
	{
		// find the page from some site
		SitePage page = SiteService.findPage(pageId);
		if (page == null)
		{
			portal.doError(req, res, session, Portal.ERROR_WORKSITE);
			return;
		}

		// permission check - visit the site
		Site site = null;
		try
		{
			site = SiteService.getSiteVisit(page.getSiteId());
		}
		catch (IdUnusedException e)
		{
			portal.doError(req, res, session, Portal.ERROR_WORKSITE);
			return;
		}
		catch (PermissionException e)
		{
			// if not logged in, give them a chance
			if (session.getUserId() == null)
			{

				StoredState ss = portalService.newStoredState("", "");
				ss.setRequest(req);
				ss.setToolContextPath(toolContextPath);
				portalService.setStoredState(ss);
				portal.doLogin(req, res, session, req.getPathInfo(), false);
			}
			else
			{
				portal.doError(req, res, session, Portal.ERROR_WORKSITE);
			}
			return;
		}

		// form a context sensitive title
		String title = ServerConfigurationService.getString("ui.service") + " : "
				+ site.getTitle() + " : " + page.getTitle();

		String siteType = portal.calcSiteType(site.getId());
		// start the response
		PortalRenderContext rcontext = portal.startPageContext(siteType, title, page
				.getSkin(), req);

		includePage(rcontext, res, req, page, toolContextPath, "contentFull");

		portal.sendResponse(rcontext, res, "page", null);
		StoredState ss = portalService.getStoredState();
		if (ss != null && toolContextPath.equals(ss.getToolContextPath()))
		{
			// This request is the destination of the request
			portalService.setStoredState(null);
		}

	}

	public void includePage(PortalRenderContext rcontext, HttpServletResponse res,
			HttpServletRequest req, SitePage page, String toolContextPath,
			String wrapperClass) throws IOException
	{
		if (rcontext.uses(INCLUDE_PAGE))
		{

			// divs to wrap the tools
			rcontext.put("pageWrapperClass", wrapperClass);
			rcontext
					.put("pageColumnLayout",
							(page.getLayout() == SitePage.LAYOUT_DOUBLE_COL) ? "col1of2"
									: "col1");
			Site site = null;
			try
			{
				site = SiteService.getSite(page.getSiteId());
			}
			catch (Exception ignoreMe)
			{
				// Non fatal - just assume null
				if (log.isTraceEnabled())
					log.trace("includePage unable to find site for page " + page.getId());
			}
			{
				List<Map> toolList = new ArrayList<Map>();
				List tools = page.getTools(0);
				for (Iterator i = tools.iterator(); i.hasNext();)
				{
					ToolConfiguration placement = (ToolConfiguration) i.next();

					if (site != null)
					{
						boolean thisTool = siteHelper.allowTool(site, placement);
						// System.out.println(" Allow Tool Display -" +
						// placement.getTitle() + " retval = " + thisTool);
						if (!thisTool) continue; // Skip this tool if not
						// allowed
					}

					Map m = portal.includeTool(res, req, placement);
					if (m != null)
					{
						toolList.add(m);
					}
				}
				rcontext.put("pageColumn0Tools", toolList);
			}

			rcontext.put("pageTwoColumn", Boolean
					.valueOf(page.getLayout() == SitePage.LAYOUT_DOUBLE_COL));

			// do the second column if needed
			if (page.getLayout() == SitePage.LAYOUT_DOUBLE_COL)
			{
				List<Map> toolList = new ArrayList<Map>();
				List tools = page.getTools(1);
				for (Iterator i = tools.iterator(); i.hasNext();)
				{
					ToolConfiguration placement = (ToolConfiguration) i.next();
					Map m = portal.includeTool(res, req, placement);
					if (m != null)
					{
						toolList.add(m);
					}
				}
				rcontext.put("pageColumn1Tools", toolList);
			}
		}
	}

}
