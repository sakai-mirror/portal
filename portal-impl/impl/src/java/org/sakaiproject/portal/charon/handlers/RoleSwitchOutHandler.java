package org.sakaiproject.portal.charon.handlers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.portal.api.PortalHandlerException;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.util.Web;

public class RoleSwitchOutHandler extends BasePortalHandler
{
	private static final String URL_FRAGMENT = "role-switch-out";

	public RoleSwitchOutHandler()
	{
		setUrlFragment(RoleSwitchOutHandler.URL_FRAGMENT);
	}
	
	@Override
	public int doGet(String[] parts, HttpServletRequest req, HttpServletResponse res,
			Session session) throws PortalHandlerException
	{
		if ((parts.length > 2) && (parts[1].equals("role-switch-out")))
		{
			try
			{
				String siteUrl = req.getContextPath() + "/site"
						+ Web.makePath(parts, 2, parts.length);
				// Make sure to add the parameters such as panel=Main
				String queryString = req.getQueryString();
				if (queryString != null)
				{
					siteUrl = siteUrl + "?" + queryString;
				}
				session.removeAttribute("roleswap/site/" + parts[2]); // remove the attribute from the session
				session.setAttribute("roleswap/exit/" + parts[2], "true");
				res.sendRedirect(siteUrl);
				return NEXT;
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