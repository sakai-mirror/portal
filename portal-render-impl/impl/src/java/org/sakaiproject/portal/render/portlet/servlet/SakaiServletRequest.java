package org.sakaiproject.portal.render.portlet.servlet;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.sakaiproject.portal.api.PortalService;
import org.sakaiproject.portal.render.portlet.services.state.PortletState;

public class SakaiServletRequest extends HttpServletRequestWrapper
{

	private PortletState state;

	public SakaiServletRequest(HttpServletRequest servletRequest, PortletState state)
	{
		super(servletRequest);
		this.state = state;
	}

	@Override
	public boolean isUserInRole(String string)
	{
		boolean retval = SakaiServletUtil.isUserInRole(string,state);
                return retval;
	}

	@Override
	public String getParameter(String string)
	{
		return (String) state.getParameters().get(string);
	}

	@Override
	public Map getParameterMap()
	{
		return new HashMap(state.getParameters());
	}

	@Override
	public Enumeration getParameterNames()
	{
		final Iterator i = state.getParameters().keySet().iterator();
		return new Enumeration() {

			public boolean hasMoreElements()
			{
				return i.hasNext();
			}

			public Object nextElement()
			{
				return i.next();
			}
		
		};
	}

	@Override
	public String[] getParameterValues(String string)
	{
		return (String[]) state.getParameters().get(string);
	}

	/**
	 * This causes the placement ID to be retrievabl from the request
	 */
	@Override
	public Object getAttribute(String attributeName)
	{
		if (PortalService.PLACEMENT_ATTRIBUTE.equals(attributeName))
		{
			return state.getId();
		}
		else
		{
			return super.getAttribute(attributeName);
		}
	}
}
