package org.sakaiproject.portal.charon.handlers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.portal.api.PortalHandlerException;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.util.Web;

public class RoleSwitchOutHandler extends BasePortalHandler
{
	public RoleSwitchOutHandler()
	{
		urlFragment = "role-switch-out";
	}
	
	@Override
	public int doGet(String[] parts, HttpServletRequest req, HttpServletResponse res,
			Session session) throws PortalHandlerException
	{
		if ((parts.length > 3) && (parts[1].equals("role-switch-out")))
		{
			try
			{
				String siteUrl = req.getContextPath() + "/directtool"
						+ Web.makePath(parts, 3, parts.length);
				// Make sure to add the parameters such as panel=Main
				String queryString = req.getQueryString();
				if (queryString != null)
				{
					siteUrl = siteUrl + "?" + queryString;
				}
				session.removeAttribute("roleswap/site/" + parts[2]); // remove the attribute from the session
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