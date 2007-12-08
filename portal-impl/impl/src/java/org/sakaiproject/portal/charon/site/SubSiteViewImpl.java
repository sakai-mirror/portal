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

package org.sakaiproject.portal.charon.site;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.user.api.PreferencesService;

/**
 * @author ieb
 *
 */
public class SubSiteViewImpl extends AbstractSiteViewImpl
{

	/**
	 * @param siteHelper
	 * @param request
	 * @param session
	 * @param currentSiteId
	 * @param siteService
	 * @param serverConfigurationService
	 * @param preferencesService
	 */
	public SubSiteViewImpl(PortalSiteHelperImpl siteHelper, HttpServletRequest request,
			Session session, String currentSiteId, SiteService siteService,
			ServerConfigurationService serverConfigurationService,
			PreferencesService preferencesService)
	{
		super(siteHelper, request, session, currentSiteId, siteService,
				serverConfigurationService, preferencesService);
	}
	/* (non-Javadoc)
	 * @see org.sakaiproject.portal.api.SiteView#getRenderContextObject()
	 */
	public Object getRenderContextObject()
	{
		List l = siteHelper.convertSitesToMaps(request, mySites, prefix, currentSiteId, 
				/* myWorkspaceSiteId */ null,
				/* includeSummary */ false, 
				/* expandSite */ false, 
				resetTools , 
				/* doPages */ false, 
				toolContextPath,
				loggedIn);
		return l;
	}


}
