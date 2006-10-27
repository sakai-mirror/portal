/**********************************************************************************
 * $URL:  $
 * $Id:  $
 ***********************************************************************************
 *
 * Copyright (c) 2006 The Sakai Foundation.
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

package org.sakaiproject.content.api;

import java.util.Collection;
import org.sakaiproject.javax.Filter;

/**
 * <p>
 * ResourceTypeRegistry is the API for managing definitions types of resources for the Resources tool.
 * </p>
 */
public interface ResourceTypeRegistry 
{
	/**
	 *  Access the definition of a particular resource type.
	 * @param typeId The id of the resource type.
	 * @return The ResourceType object which defines the requested type, or null if the type is not defined.
	 */
	public ResourceType getType(String typeId);
	
	/**
	 * Access a collection (possibly empty) of all resource types that have been defined.
	 * @return
	 */
	public Collection getTypes();
	
	/**
	 * Access a subset of the resource types that have been defined where membership in the subset is 
	 * determined by whether the filter indicates that the ResourceType is accepted.  The filter can 
	 * accept a ResourceType object based on any attribute of the type that can be determined from the
	 * ResourceType object itself.
	 * @param filter
	 * @return
	 */
	public Collection getTypes(Filter filter);
	
	/**
	 * Register a ResourceType object to indicate that resources of that type can be defined in  
	 * the Resources tool.  If the InteractionAction object is null or if the type object's getId()  
	 * method returns a null value, no type is registered. 
	 * @param type
	 */
	public void register(ResourceType type);
		
}
