package org.sakaiproject.portal.charon.handlers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.authz.cover.SecurityService;
import org.sakaiproject.event.api.NotificationService;
import org.sakaiproject.event.cover.EventTrackingService;
import org.sakaiproject.portal.api.PortalHandlerException;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.thread_local.cover.ThreadLocalManager;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.util.Web;

public class RoleSwitchOutHandler extends BasePortalHandler
{
	private static final String URL_FRAGMENT = "role-switch-out";

	public static final String EVENT_ROLESWAP_EXIT = "roleswap.exit";

	public RoleSwitchOutHandler()
	{
		setUrlFragment(RoleSwitchOutHandler.URL_FRAGMENT);
	}
	
	@Override
	public int doGet(String[] parts, HttpServletRequest req, HttpServletResponse res,
			Session session) throws PortalHandlerException
	{
		if (parts == null || req == null || res == null || session == null)
			throw new IllegalStateException("null pointers while swapping out of student view");
		if ((parts.length > 2) && "role-switch-out".equals(parts[1]))
		{
			try
			{
				Site site = portal.getSiteHelper().getSiteVisit(parts[2]);
				ThreadLocalManager.set("sakai:portal:hierarchy", Boolean.TRUE);
				String siteUrl = req.getContextPath() + "/hierarchy/"
						+ portal.getSiteHelper().getSiteEffectiveId(site) 
						+ ((parts.length > 4)?Web.makePath(parts, 3, parts.length-1):"");
				// Make sure to add the parameters such as panel=Main
				String queryString = req.getQueryString();
				if (queryString != null)
				{
					siteUrl = siteUrl + "?" + queryString;
				}
				portalService.setResetState("true"); // flag the portal to reset
				SecurityService.clearUserEffectiveRole(SiteService.siteReference(parts[2]));
				
				// Post an event
				EventTrackingService.post(EventTrackingService.newEvent(EVENT_ROLESWAP_EXIT, null, parts[2], false, NotificationService.NOTI_NONE));				

				res.sendRedirect(siteUrl);
				return RESET_DONE;
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

}
