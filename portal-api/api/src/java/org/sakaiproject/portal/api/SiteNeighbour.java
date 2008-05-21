package org.sakaiproject.portal.api;

import org.sakaiproject.site.api.Site;

public interface SiteNeighbour
{
	
	// Was called direction (but member doesn't make sense).
	enum Relationship {
		MYWORKSITE, // My Workspace site		
		MEMBER, // Used for traditional sites your a member of.
		CURRENT, // The current site
		UP, // Used in hierarchy (parents)
		DOWN, // Used in hierarchy (childred)
		NONE; // Others that don't fit.
		
	};
	
	public Site getSite();
	
	public Relationship getRelationship();
	
	public int getDistance();

}
