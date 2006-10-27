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

package org.sakaiproject.entity.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.entity.api.EntityManager;
import org.sakaiproject.entity.api.EntityProducer;
import org.sakaiproject.entity.api.Reference;
import org.sakaiproject.util.Validator;

/**
 * <p>
 * EntityManagerComponent is an implementation of the EntityManager.
 * </p>
 */
public class EntityManagerComponent implements EntityManager
{
	/** Our logger. */
	protected static Log M_log = LogFactory.getLog(EntityManagerComponent.class);

	/** Set of EntityProducer services. */
	protected Set m_producers = new HashSet();

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Constructors, Dependencies and their setter methods
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Init and Destroy
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			M_log.info("init()");
		}
		catch (Throwable t)
		{
		}
	}

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		M_log.info("destroy()");
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * EntityManager implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * @inheritDoc
	 */
	public List getEntityProducers()
	{
		List rv = new Vector();
		rv.addAll(m_producers);

		return rv;
	}

	/**
	 * @inheritDoc
	 */
	public void registerEntityProducer(EntityProducer manager, String referenceRoot)
	{
		// TODO: referenceRoot
		m_producers.add(manager);
	}

	/**
	 * @inheritDoc
	 */
	public Reference newReference(String refString)
	{
		return new ReferenceComponent(refString);
	}

	/**
	 * @inheritDoc
	 */
	public Reference newReference(Reference copyMe)
	{
		return new ReferenceComponent(copyMe);
	}

	/**
	 * @inheritDoc
	 */
	public List newReferenceList()
	{
		return new ReferenceVectorComponent();
	}

	/**
	 * @inheritDoc
	 */
	public List newReferenceList(List copyMe)
	{
		return new ReferenceVectorComponent(copyMe);
	}

	/**
	 * @inheritDoc
	 */
	public boolean checkReference(String ref)
	{
		// the rules:
		//	Null is rejected
		//	all blank is rejected
		//	INVALID_CHARS_IN_RESOURCE_ID characters are rejected
		
		Reference r = newReference(ref);
		
		// just check the id... %%% need more? -ggolden
		String id = r.getId();

		if (id == null) return false;
		if (id.trim().length() == 0) return false;

		// we must reject certain characters that we cannot even escape and get into Tomcat via a URL
		for (int i = 0; i < id.length(); i++)
		{
			if (Validator.INVALID_CHARS_IN_RESOURCE_ID.indexOf(id.charAt(i)) != -1)
				return false;
		}

		return true;
	}
}
