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
		if (parts == null || req == null || res == null || session == null)
			throw new IllegalStateException("null pointers while swapping out of student view");
		if ((parts.length > 2) && "role-switch-out".equals(parts[1]))
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
				portalService.setResetState("true");
				session.removeAttribute("roleswap/site/" + parts[2]); // remove the attribute from the session
				session.setAttribute("roleswap/exit/" + parts[2], "true"); // set this so sakai security will know we were in a swapped view and now we're not
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