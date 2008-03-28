package org.sakaiproject.portal.charon.handlers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.portal.api.PortalHandlerException;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.util.Web;

public class RoleSwitchHandler extends BasePortalHandler
{
	private static final String URL_FRAGMENT = "role-switch";

	public RoleSwitchHandler()
	{
		setUrlFragment(RoleSwitchHandler.URL_FRAGMENT);
	}
	
	@Override
	public int doGet(String[] parts, HttpServletRequest req, HttpServletResponse res,
			Session session) throws PortalHandlerException
	{
		if ((parts.length > 3) && (parts[1].equals("role-switch")) && SiteService.allowRoleSwap(parts[2]))
		{
			try
			{
				String siteUrl = req.getContextPath() + "/site"
						+ Web.makePath(parts, 2, parts.length-1);
				// Make sure to add the parameters such as panel=Main
				String queryString = req.getQueryString();
				if (queryString != null)
				{
					siteUrl = siteUrl + "?" + queryString;
				}
				portalService.setResetState("true");
				session.removeAttribute("roleswap/exit/" + parts[2]); // remove the attribute from the session
				session.setAttribute("roleswap/site/" + parts[2] , parts[3]); // set the session attribute with the roleid
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