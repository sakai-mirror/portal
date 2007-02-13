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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sakaiproject.portal.charon.PortalHandlerException;
import org.sakaiproject.portal.charon.PortalRenderContext;
import org.sakaiproject.tool.api.Session;

/**
 * @author ieb
 */
public class NavLoginGalleryHandler extends GalleryHandler
{

	public NavLoginGalleryHandler() {
		urlFragment = "nav_login_gallery";
	}

	@Override
	public int doGet(String[] parts, HttpServletRequest req, HttpServletResponse res, Session session)
			throws PortalHandlerException
	{
		if ((parts.length == 3) && (parts[1].equals("nav_login_gallery")))
		{
			try
			{
				doNavLoginGallery(req, res, session, parts[2]);
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

	public void doNavLoginGallery(HttpServletRequest req, HttpServletResponse res, Session session, String siteId)
			throws IOException
	{
		// start the response

		PortalRenderContext rcontext = portal.startPageContext("", "Login", null, req);

		includeGalleryLogin(rcontext, req, session, siteId);
		// end the response
		portal.sendResponse(rcontext, res, "gallery-login", null);
	}

}
