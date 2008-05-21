package org.sakaiproject.portal.service;

import org.sakaiproject.portal.api.SiteNeighbour;
import org.sakaiproject.site.api.Site;

public class SiteNeighbourImpl implements SiteNeighbour
{
	private Site site;
	private Site current;
	private Relationship direction;
	private int distance;
	
	public SiteNeighbourImpl(Site site, Site current, Relationship direction, int distance)
	{
		this.site = site;
		this.current = current;
		this.direction = direction;
		this.distance = distance;
	}

	public Site getCurrentSite()
	{
		return current;
	}

	public Relationship getRelationship()
	{
		return direction;
	}

	public int getDistance()
	{
		return distance;
	}

	public Site getSite()
	{
		return site;
	}
	

	public String toString() 
	{
		return
			"Site: "+ site.getId()+ ", "+
			"Relationship: "+ direction + ", "+
			"Distance: "+ distance;
	}


}
