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

package org.sakaiproject.portal.charon;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.portal.api.PortalService;
import org.sakaiproject.tool.api.Session;

/**
 * @author ieb
 */
public interface PortalHandler
{

	public static final int ABORT = 0;

	public static final int END = 1;

	public static final int NEXT = 2;

	public static final int RESET_DONE = 3;

	/**
	 * @param parts
	 * @param req
	 * @param res
	 * @param session
	 * @return
	 * @throws IOException
	 * @throws ServletException
	 * @throws ToolHandlerException
	 */
	int doGet(String[] parts, HttpServletRequest req, HttpServletResponse res, Session session) throws PortalHandlerException;

	/**
	 * @return
	 */
	String getUrlFragment();

	/**
	 * @param portal
	 */
	void deregister(Portal portal);

	/**
	 * @param portal
	 * @param portalService
	 * @param servletContext
	 */
	void register(Portal portal, PortalService portalService, ServletContext servletContext);

	/**
	 * @param parts
	 * @param req
	 * @param res
	 * @param session
	 * @return
	 * @throws PortalHandlerException 
	 */
	int doPost(String[] parts, HttpServletRequest req, HttpServletResponse res, Session session) throws PortalHandlerException;
}
