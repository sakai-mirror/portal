/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation.
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

package org.sakaiproject.event.api;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * SessionState is a collection of named attributes associated with the current session. SessionState implements Map, but the Map methods should only be used to get attributes, not to set or remove attributes.
 * </p>
 */
public interface SessionState extends Map
{
	/**
	 * Access the named attribute.
	 * 
	 * @param name
	 *        The attribute name.
	 * @return The named attribute value.
	 */
	Object getAttribute(String name);

	/**
	 * Set the named attribute value to the provided object.
	 * 
	 * @param name
	 *        The attribute name.
	 * @param value
	 *        The value of the attribute (any object type).
	 * @return The previous value of the named attribute (or null if no previous value).
	 */
	Object setAttribute(String name, Object value);

	/**
	 * Remove the named attribute, if it exists.
	 * 
	 * @param name
	 *        The attribute name.
	 * @return The previous value of the removed named attribute (or null if no previous value).
	 */
	Object removeAttribute(String name);

	/**
	 * Remove all attributes.
	 */
	void clear();

	/**
	 * Access a List of all names of attributes stored in the SessionState.
	 * 
	 * @return A List of all names of attribute stored in the SessionState.
	 */
	List getAttributeNames();
}
