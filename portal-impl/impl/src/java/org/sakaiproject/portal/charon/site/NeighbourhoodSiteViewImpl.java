package org.sakaiproject.portal.charon.site;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.entity.api.ResourceProperties;
import org.sakaiproject.portal.api.SiteNeighbour;
import org.sakaiproject.portal.api.SiteNeighbourhoodService;
import org.sakaiproject.portal.api.SiteNeighbour.Relationship;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.user.api.PreferencesService;

public class NeighbourhoodSiteViewImpl extends AbstractSiteViewImpl
{

	public NeighbourhoodSiteViewImpl(PortalSiteHelperImpl siteHelper,
			SiteNeighbourhoodService siteNeighbourhoodService,
			HttpServletRequest request, Session session, String currentSiteId,
			SiteService siteService,
			ServerConfigurationService serverConfigurationService,
			PreferencesService preferencesService)
	{
		super(siteHelper, siteNeighbourhoodService, request, session, currentSiteId,
				siteService, serverConfigurationService, preferencesService);
		// TODO Auto-generated constructor stub
	}

	private boolean isSubSiteEnabled(String siteId)
	{
		// Check the setting as to whether we are to do this
		String pref = serverConfigurationService.getString("portal.includesubsites");
		if ( "never".equals(pref) ) return false;
		Site site = null;
		try
		{
			site = siteHelper.getSiteVisit(siteId);
		}
		catch (Exception e)
		{
		}
		if ( site == null ) return false;

		// Should be in site view.
		ResourceProperties rp = site.getProperties();
		String showSub = rp.getProperty(SiteService.PROP_SHOW_SUBSITES);
		if ( "false".equals(showSub) ) return false;

		if ( "false".equals(pref) )
		{
			if ( ! "true".equals(showSub) ) return false;
		}
		
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.sakaiproject.portal.api.SiteView#getRenderContextObject()
	 */
	public Object getRenderContextObject()
	{
		// Subsites should be a list of sites, with this site as their parent.
		if ( currentSiteId == null || currentSiteId.trim().length() == 0 ) {
			return null;
		}
		
		if (!isSubSiteEnabled(currentSiteId)) {
			return null;
		}
		List<Site> parentSites = new ArrayList<Site>();
		List<Site> childSites = new ArrayList<Site>();
		
		// Find the parent and child sites.
		for (SiteNeighbour neighbour: myNeighbours) {
			if (Relationship.UP.equals(neighbour.getRelationship())) {
				parentSites.add(neighbour.getSite());
			} else if (Relationship.DOWN.equals(neighbour.getRelationship())) {
				childSites.add(neighbour.getSite());
			}
		}
		
		Map m = new HashMap();
		
		Collections.reverse(parentSites);
		List<Map> childSitesMap = siteHelper.convertSitesToMaps(request, childSites, prefix, currentSiteId, 
				/* myWorkspaceSiteId */ null,
				/* includeSummary */ false, 
				/* expandSite */ false, 
				resetTools , 
				/* doPages */ false, 
				toolContextPath,
				loggedIn);
		
		List<Map> parentSitesMap = siteHelper.convertSitesToMaps(request, parentSites, prefix, currentSiteId, 
				/* myWorkspaceSiteId */ null,
				/* includeSummary */ false, 
				/* expandSite */ false, 
				resetTools , 
				/* doPages */ false, 
				toolContextPath,
				loggedIn);
		
		m.put("subsites", childSitesMap);
		m.put("parentsites", parentSitesMap);

			
		return m;
	}
	
	/**
	 * Gets the path of sites back to the root of the tree.
	 * @param s
	 * @param ourParent
	 * @return
	 */
	private List<Site> getPwd(Site s, String ourParent)
	{
		if (ourParent == null) return null;

		// System.out.println("Getting Current Working Directory for
		// "+s.getId()+" "+s.getTitle());

		int depth = 0;
		Vector<Site> pwd = new Vector<Site>();
		Set<String> added = new HashSet<String>();

		// Add us to the list at the top (will become the end)
		pwd.add(s);
		added.add(s.getId());

		// Make sure we don't go on forever
		while (ourParent != null && depth < 8)
		{
			depth++;
			Site site = null;
			try
			{
				site = siteService.getSiteVisit(ourParent);
			}
			catch (Exception e)
			{
				break;
			}
			// We have no patience with loops
			if (added.contains(site.getId())) break;

			pwd.insertElementAt(site, 0); // Push down stack
			added.add(site.getId());

			ResourceProperties rp = site.getProperties();
			ourParent = rp.getProperty(SiteService.PROP_PARENT_ID);
		}

		// PWD is only defined for > 1 site
		if (pwd.size() < 2) return null;
		return pwd;
	}
	

}
