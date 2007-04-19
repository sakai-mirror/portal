/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2005, 2006 The Sakai Foundation.
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

package org.sakaiproject.portal.render.portlet.servlet;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.sakaiproject.portal.api.PortalService;
import org.sakaiproject.portal.render.portlet.services.state.PortletState;

/**
 * @author ddwolf
 * @author ieb
 * @since Sakai 2.4
 * @version $Rev$
 */
public class SakaiServletActionRequest extends HttpServletRequestWrapper
{

	private PortletState state;

	public SakaiServletActionRequest(HttpServletRequest servletRequest, PortletState state)
	{
		super(servletRequest);
		this.state = state;
	}

	@Override
	public boolean isUserInRole(String string)
	{
		boolean retval = SakaiServletUtil.isUserInRole(string, state);
		return retval;
	}

	@Override
	public String getParameter(String string)
	{
		String param = super.getParameter(string);
		if (isEmpty(param))
		{
			return (String) state.getParameters().get(string);
		}
		return param;
	}

	/**
	 * @param param
	 * @return
	 */
	private boolean isEmpty(String param)
	{
		return (param == null || param.length() == 0);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map getParameterMap()
	{
		Map paramMap = new HashMap(state.getParameters());
		Map requestParamMap = super.getParameterMap();
		for (Iterator<String> i = requestParamMap.keySet().iterator(); i.hasNext();)
		{
			String s = i.next();
			paramMap.put(s, requestParamMap.get(s));
		}
		return paramMap;
	}

	@Override
	public Enumeration getParameterNames()
	{
		final Iterator i = getParameterMap().keySet().iterator();
		return new Enumeration()
		{

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
		String[] v = super.getParameterValues(string);
		if (v == null)
		{
			return (String[]) state.getParameters().get(string);
		}
		return v;
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
