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

package org.sakaiproject.event.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.AuthzGroupService;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.db.api.SqlReader;
import org.sakaiproject.db.api.SqlService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.event.api.SessionState;
import org.sakaiproject.event.api.SessionStateBindingListener;
import org.sakaiproject.event.api.UsageSession;
import org.sakaiproject.event.api.UsageSessionService;
import org.sakaiproject.id.api.IdManager;
import org.sakaiproject.thread_local.api.ThreadLocalManager;
import org.sakaiproject.time.api.Time;
import org.sakaiproject.time.api.TimeService;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.SessionBindingEvent;
import org.sakaiproject.tool.api.SessionBindingListener;
import org.sakaiproject.tool.api.SessionManager;
import org.sakaiproject.tool.api.ToolSession;
import org.sakaiproject.user.api.Authentication;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

/**
 * <p>
 * UsageSessionServiceAdaptor implements the UsageSessionService. The Session aspects are done as an adaptor to the SessionManager. UsageSession entities are handled as was in the ClusterUsageSessionService.
 * </p>
 */
public abstract class UsageSessionServiceAdaptor implements UsageSessionService
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(UsageSessionServiceAdaptor.class);

	/** Storage manager for this service. */
	protected Storage m_storage = null;

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Abstractions, etc.
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * Construct storage for this service.
	 */
	protected Storage newStorage()
	{
		return new ClusterStorage();
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Dependencies
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * @return the TimeService collaborator.
	 */
	protected abstract TimeService timeService();

	/** Dependency: SqlService. */
	/**
	 * @return the SqlService collaborator.
	 */
	protected abstract SqlService sqlService();

	/**
	 * @return the ServerConfigurationService collaborator.
	 */
	protected abstract ServerConfigurationService serverConfigurationService();

	/**
	 * @return the ThreadLocalManager collaborator.
	 */
	protected abstract ThreadLocalManager threadLocalManager();

	/**
	 * @return the SessionManager collaborator.
	 */
	protected abstract SessionManager sessionManager();
	/**
	 * @return the IdManager collaborator.
	 */
	protected abstract IdManager idManager();
	/**
	 * @return the EventTrackingService collaborator.
	 */
	protected abstract EventTrackingService eventTrackingService();
	/**
	 * @return the AuthzGroupService collaborator.
	 */
	protected abstract AuthzGroupService authzGroupService();

	/**
	 * @return the UserDirectoryService collaborator.
	 */
	protected abstract UserDirectoryService userDirectoryService();

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Configuration
	 *********************************************************************************************************************************************************************************************************************************************************/

	/** Configuration: to run the ddl on init or not. */
	protected boolean m_autoDdl = false;

	/**
	 * Configuration: to run the ddl on init or not.
	 * 
	 * @param value
	 *        the auto ddl value.
	 */
	public void setAutoDdl(String value)
	{
		m_autoDdl = new Boolean(value).booleanValue();
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Init and Destroy
	 *********************************************************************************************************************************************************************************************************************************************************/

	public UsageSessionServiceAdaptor()
	{
		m_storage = newStorage();
	}

	/**
	 * Final initialization, once all dependencies are set.
	 */
	public void init()
	{
		try
		{
			// open storage
			m_storage.open();

			M_log.info("init()");
		}
		catch (Throwable t)
		{
			M_log.warn("init(): ", t);
		}
	}

	/**
	 * Returns to uninitialized state.
	 */
	public void destroy()
	{
		m_storage.close();

		M_log.info("destroy()");
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * UsageSessionService implementation
	 *********************************************************************************************************************************************************************************************************************************************************/

	/**
	 * @inheritDoc
	 */
	public UsageSession startSession(String userId, String remoteAddress, String userAgent)
	{
		// do we have a current session?
		Session s = sessionManager().getCurrentSession();
		if (s != null)
		{
			UsageSession session = (UsageSession) s.getAttribute(USAGE_SESSION_KEY);
			if (session != null)
			{
				// If we have a session for this user, simply reuse
				if (userId != null && userId.equals(session.getUserId()))
				{
					return session;
				}

				// if it is for another user, we will create a new session, log a warning, and unbound/close the existing one
				s.setAttribute(USAGE_SESSION_KEY, null);
				M_log.warn("startSession: replacing existing UsageSession: " + session.getId() + " user: " + session.getUserId()
						+ " for new user: " + userId);
			}

			// create the usage session and bind it to the session
			session = new BaseUsageSession(idManager().createUuid(), serverConfigurationService().getServerIdInstance(), userId,
					remoteAddress, userAgent, null, null);

			// store
			if (m_storage.addSession(session))
			{
				// set as the current session
				s.setAttribute(USAGE_SESSION_KEY, session);

				return session;
			}
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public UsageSession getSession()
	{
		UsageSession rv = null;

		// do we have a current session?
		Session s = sessionManager().getCurrentSession();
		if (s != null)
		{
			// do we have a usage session in the session?
			rv = (BaseUsageSession) s.getAttribute(USAGE_SESSION_KEY);
		}

		else
		{
			M_log.warn("getSession: no current SessionManager session!");
		}

		return rv;
	}

	/**
	 * @inheritDoc
	 */
	public String getSessionId()
	{
		String rv = null;

		// See http://bugs.sakaiproject.org/jira/browse/SAK-1507
		// At server startup, when Spring is initializing components, there may not
		// be a session manager yet. This adaptor may be called before all components
		// are initialized since there are hidden dependencies (through static covers)
		// of which Spring is not aware. Therefore, check for and handle a null
		// sessionManager().
		if (sessionManager() == null) return null;

		// do we have a current session?
		Session s = sessionManager().getCurrentSession();
		if (s != null)
		{
			// do we have a usage session in the session?
			BaseUsageSession session = (BaseUsageSession) s.getAttribute(USAGE_SESSION_KEY);
			if (session != null)
			{
				rv = session.getId();
			}
		}

		// may be null, which indicates that there's no session
		return rv;
	}

	/**
	 * @inheritDoc
	 */
	public SessionState getSessionState(String key)
	{
		// map this to the sakai session's tool session concept, using key as the placement id
		Session s = sessionManager().getCurrentSession();
		if (s != null)
		{
			return new SessionStateWrapper(s.getToolSession(key));
		}

		M_log.warn("getSessionState(): no session:  key: " + key);
		return null;
	}

	/**
	 * @inheritDoc
	 */
	public UsageSession setSessionActive(boolean auto)
	{
		throw new UnsupportedOperationException();
		// BaseUsageSession session = (BaseUsageSession) getSession();
		// if (session == null) return null;
		//
		// if (session.isClosed()) return session;
		//
		// if (auto)
		// {
		// // do not mark the current session as having user activity
		// // but close it if it's timed out from no user activity
		// if (session.isInactive())
		// {
		// session.close();
		// }
		// }
		// else
		// {
		// // mark the current session as having user activity
		// session.setActivity();
		// }
		//
		// return session;
	}

	/**
	 * @inheritDoc
	 */
	public UsageSession getSession(String id)
	{
		UsageSession rv = m_storage.getSession(id);

		return rv;
	}

	/**
	 * @inheritDoc
	 */
	public List getSessions(List ids)
	{
		List rv = m_storage.getSessions(ids);

		return rv;
	}

	/**
	 * @inheritDoc
	 */
	public List getSessions(String joinTable, String joinAlias, String joinColumn, String joinCriteria, Object[] values)
	{
		List rv = m_storage.getSessions(joinTable, joinAlias, joinColumn, joinCriteria, values);

		return rv;
	}

	/**
	 * @inheritDoc
	 */
	public int getSessionInactiveTimeout()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * @inheritDoc
	 */
	public int getSessionLostTimeout()
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * @inheritDoc
	 */
	public List getOpenSessions()
	{
		return m_storage.getOpenSessions();
	}

	/**
	 * @inheritDoc
	 */
	public Map getOpenSessionsByServer()
	{
		List all = m_storage.getOpenSessions();

		Map byServer = new TreeMap();

		List current = null;
		String key = null;

		for (Iterator i = all.iterator(); i.hasNext();)
		{
			UsageSession s = (UsageSession) i.next();

			// to start, or when the server changes, create a new inner list and add to the map
			if ((key == null) || (!key.equals(s.getServer())))
			{
				key = s.getServer();
				current = new Vector();
				byServer.put(key, current);
			}

			current.add(s);
		}

		return byServer;
	}

	/**
	 * @inheritDoc
	 */
	public boolean login(Authentication authn, HttpServletRequest req)
	{
		// establish the user's session - this has been known to fail
		UsageSession session = startSession(authn.getUid(), req.getRemoteAddr(), req.getHeader("user-agent"));
		if (session == null)
		{
			return false;
		}

		// set the user information into the current session
		Session sakaiSession = sessionManager().getCurrentSession();
		sakaiSession.setUserId(authn.getUid());
		sakaiSession.setUserEid(authn.getEid());

		// update the user's externally provided realm definitions
		authzGroupService().refreshUser(authn.getUid());

		// post the login event
		eventTrackingService().post(eventTrackingService().newEvent(EVENT_LOGIN, null, true));

		return true;
	}

	/**
	 * @inheritDoc
	 */
	public void logout()
	{
		userDirectoryService().destroyAuthentication();

		// invalidate the sakai session, which makes it unavailable, unbinds all the bound objects,
		// including the session, which will close and generate the logout event
		Session sakaiSession = sessionManager().getCurrentSession();
		sakaiSession.invalidate();
	}

	/**
	 * Generate the logout event.
	 */
	protected void logoutEvent(UsageSession session)
	{
		if (session == null)
		{
			// generate a logout event (current session)
			eventTrackingService().post(eventTrackingService().newEvent(EVENT_LOGOUT, null, true));
		}
		else
		{
			// generate a logout event (this session)
			eventTrackingService().post(eventTrackingService().newEvent(EVENT_LOGOUT, null, true), session);
		}
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Storage
	 *********************************************************************************************************************************************************************************************************************************************************/

	protected interface Storage
	{
		/**
		 * Open.
		 */
		void open();

		/**
		 * Close.
		 */
		void close();

		/**
		 * Take this session into storage.
		 * 
		 * @param session
		 *        The usage session.
		 * @return true if added successfully, false if not.
		 */
		boolean addSession(UsageSession session);

		/**
		 * Access a session by id
		 * 
		 * @param id
		 *        The session id.
		 * @return The session object.
		 */
		UsageSession getSession(String id);

		/**
		 * Access a bunch of sessions by the List id session ids.
		 * 
		 * @param ids
		 *        The session id List.
		 * @return The List (UsageSession) of session objects for these ids.
		 */
		List getSessions(List ids);

		/**
		 * Access a List of usage sessions by *arbitrary criteria* for te session ids.
		 * 
		 * @param joinTable
		 *        the table name to (inner) join to
		 * @param joinAlias
		 *        the alias used in the criteria string for the joinTable
		 * @param joinColumn
		 *        the column name of the joinTable that is to match the session id in the join ON clause
		 * @param joinCriteria
		 *        the criteria of the select (after the where)
		 * @param fields
		 *        Optional values to go with the criteria in an implementation specific way.
		 * @return The List (UsageSession) of UsageSession object for these ids.
		 */
		List getSessions(String joinTable, String joinAlias, String joinColumn, String joinCriteria, Object[] values);

		/**
		 * This session is now closed.
		 * 
		 * @param session
		 *        The session which is closed.
		 */
		void closeSession(UsageSession session);

		/**
		 * Access a list of all open sessions.
		 * 
		 * @return a List (UsageSession) of all open sessions, ordered by server, then by start (asc)
		 */
		List getOpenSessions();
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * UsageSession
	 *********************************************************************************************************************************************************************************************************************************************************/

	protected class BaseUsageSession implements UsageSession, SessionBindingListener
	{
		/** The user id for this session. */
		protected String m_user = null;

		/** The unique id for this session. */
		protected String m_id = null;

		/** The server which is hosting the session. */
		protected String m_server = null;

		/** The IP Address from which this session originated. */
		protected String m_ip = null;

		/** The User Agent string describing the browser used in this session. */
		protected String m_userAgent = null;

		/** The BrowserID string describing the browser used in this session. */
		protected String m_browserId = null;

		/** The time the session was started */
		protected Time m_start = null;

		/** The time the session was closed. */
		protected Time m_end = null;

		/**
		 * Construct fully.
		 * 
		 * @param id
		 *        The session id.
		 * @param server
		 *        The server id which is hosting the session.
		 * @param user
		 *        The user id for this session.
		 * @param address
		 *        The IP Address from which this session originated.
		 * @param agent
		 *        The User Agent string describing the browser used in this session.
		 */
		public BaseUsageSession(String id, String server, String user, String address, String agent, Time start, Time end)
		{
			m_id = id;
			m_server = server;
			m_user = user;
			m_ip = address;
			m_userAgent = agent;
			if (start != null)
			{
				m_start = start;
				m_end = end;
			}
			else
			{
				m_start = timeService().newTime();
				m_end = m_start;
			}
			setBrowserId(agent);
		}

		/**
		 * Set the browser id for this session, decoded from the user agent string.
		 * 
		 * @param agent
		 *        The user agent string.
		 */
		protected void setBrowserId(String agent)
		{
			if (agent == null)
			{
				m_browserId = UNKNOWN;
			}

			// test whether agent is UserAgent value for a known browser.
			// should we also check version number?
			else if (agent.indexOf("Netscape") >= 0 && agent.indexOf("Mac") >= 0)
			{
				m_browserId = MAC_NN;
			}
			else if (agent.indexOf("Netscape") >= 0 && agent.indexOf("Windows") >= 0)
			{
				m_browserId = WIN_NN;
			}
			else if (agent.indexOf("MSIE") >= 0 && agent.indexOf("Mac") >= 0)
			{
				m_browserId = MAC_IE;
			}
			else if (agent.indexOf("MSIE") >= 0 && agent.indexOf("Windows") >= 0)
			{
				m_browserId = WIN_IE;
			}
			else if (agent.indexOf("Camino") >= 0 && agent.indexOf("Macintosh") >= 0)
			{
				m_browserId = MAC_CM;
			}
			else if (agent.startsWith("Mozilla") && agent.indexOf("Windows") >= 0)
			{
				m_browserId = WIN_MZ;
			}
			else if (agent.startsWith("Mozilla") && agent.indexOf("Macintosh") >= 0)
			{
				m_browserId = MAC_MZ;
			}
			else
			{
				m_browserId = UNKNOWN;
			}
		}

		/**
		 * Close the session.
		 */
		protected void close()
		{
			if (!isClosed())
			{
				m_end = timeService().newTime();
				m_storage.closeSession(this);
			}
		}

		/**
		 * @inheritDoc
		 */
		public boolean isClosed()
		{
			return (!(m_end.equals(m_start)));
		}

		/**
		 * @inheritDoc
		 */
		public String getUserId()
		{
			return m_user;
		}
		
		/**
		 * @inheritDoc
		 */
		public String getUserEid()
		{
			try
			{
				return userDirectoryService().getUserEid(m_user);
			}
			catch (UserNotDefinedException e)
			{
				return m_user;
			}
		}

		/**
		 * @inheritDoc
		 */
		public String getUserDisplayId()
		{
			try
			{
				User user = userDirectoryService().getUser(m_user);
				return user.getDisplayId();
			}
			catch (UserNotDefinedException e)
			{
				return m_user;
			}
		}

		/**
		 * @inheritDoc
		 */
		public String getId()
		{
			return m_id;
		}

		/**
		 * @inheritDoc
		 */
		public String getServer()
		{
			return m_server;
		}

		/**
		 * @inheritDoc
		 */
		public String getIpAddress()
		{
			return m_ip;
		}

		/**
		 * @inheritDoc
		 */
		public String getUserAgent()
		{
			return m_userAgent;
		}

		/**
		 * @inheritDoc
		 */
		public String getBrowserId()
		{
			return m_browserId;
		}

		/**
		 * @inheritDoc
		 */
		public Time getStart()
		{
			return timeService().newTime(m_start.getTime());
		}

		/**
		 * @inheritDoc
		 */
		public Time getEnd()
		{
			return timeService().newTime(m_end.getTime());
		}

		/**
		 * There's new user activity now.
		 */
		protected void setActivity()
		{
			throw new UnsupportedOperationException();
		}

		/**
		 * Has this session gone inactive?
		 * 
		 * @return True if the session has seen no activity in the last timeout period, false if it's still active.
		 */
		protected boolean isInactive()
		{
			throw new UnsupportedOperationException();
		}

		/**
		 * @inheritDoc
		 */
		public void valueBound(SessionBindingEvent sbe)
		{
		}

		/**
		 * @inheritDoc
		 */
		public void valueUnbound(SessionBindingEvent sbe)
		{
			// if we didn't close this already, close
			if (!isClosed())
			{
				// close the session
				close();

				// generate the logout event
				logoutEvent(this);
			}
		}

		/**
		 * @inheritDoc
		 */
		public int compareTo(Object obj)
		{
			if (!(obj instanceof UsageSession)) throw new ClassCastException();

			// if the object are the same, say so
			if (obj == this) return 0;

			// start the compare by comparing their users
			int compare = getUserId().compareTo(((UsageSession) obj).getUserId());

			// if these are the same
			if (compare == 0)
			{
				// sort based on (unique) id
				compare = getId().compareTo(((UsageSession) obj).getId());
			}

			return compare;
		}

		/**
		 * @inheritDoc
		 */
		public String toString()
		{
			return "[" + ((m_id == null) ? "" : m_id) + " | " + ((m_server == null) ? "" : m_server) + " | "
					+ ((m_user == null) ? "" : m_user) + " | " + ((m_ip == null) ? "" : m_ip) + " | "
					+ ((m_userAgent == null) ? "" : m_userAgent) + " | " + m_start.toStringGmtFull() + " ]";
		}
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * SessionState
	 *********************************************************************************************************************************************************************************************************************************************************/

	public class SessionStateWrapper implements SessionState
	{
		/** The ToolSession object wrapped. */
		protected ToolSession m_session = null;

		public SessionStateWrapper(ToolSession session)
		{
			m_session = session;
		}

		/**
		 * @inheritDoc
		 */
		public Object getAttribute(String name)
		{
			return m_session.getAttribute(name);
		}

		/**
		 * @inheritDoc
		 */
		public Object setAttribute(String name, Object value)
		{
			Object old = m_session.getAttribute(name);
			unBindAttributeValue(name, old);

			m_session.setAttribute(name, value);
			bindAttributeValue(name, value);

			return old;
		}

		/**
		 * @inheritDoc
		 */
		public Object removeAttribute(String name)
		{
			Object old = m_session.getAttribute(name);
			unBindAttributeValue(name, old);

			m_session.removeAttribute(name);

			return old;
		}

		/**
		 * @inheritDoc
		 */
		public void clear()
		{
			// unbind
			for (Enumeration e = m_session.getAttributeNames(); e.hasMoreElements();)
			{
				String name = (String) e.nextElement();
				Object value = m_session.getAttribute(name);
				unBindAttributeValue(name, value);
			}

			m_session.clearAttributes();
		}

		/**
		 * @inheritDoc
		 */
		public List getAttributeNames()
		{
			List rv = new Vector();
			for (Enumeration e = m_session.getAttributeNames(); e.hasMoreElements();)
			{
				String name = (String) e.nextElement();
				rv.add(name);
			}

			return rv;
		}

		/**
		 * @inheritDoc
		 */
		public int size()
		{
			throw new UnsupportedOperationException();
		}

		/**
		 * @inheritDoc
		 */
		public boolean isEmpty()
		{
			throw new UnsupportedOperationException();
		}

		/**
		 * @inheritDoc
		 */
		public boolean containsKey(Object key)
		{
			throw new UnsupportedOperationException();
		}

		/**
		 * @inheritDoc
		 */
		public boolean containsValue(Object value)
		{
			throw new UnsupportedOperationException();
		}

		/**
		 * @inheritDoc
		 */
		public Object get(Object key)
		{
			throw new UnsupportedOperationException();
		}

		/**
		 * @inheritDoc
		 */
		public Object put(Object key, Object value)
		{
			throw new UnsupportedOperationException();
		}

		/**
		 * @inheritDoc
		 */
		public Object remove(Object key)
		{
			throw new UnsupportedOperationException();
		}

		/**
		 * @inheritDoc
		 */
		public void putAll(Map t)
		{
			throw new UnsupportedOperationException();
		}

		/**
		 * @inheritDoc
		 */
		public Set keySet()
		{
			throw new UnsupportedOperationException();
		}

		/**
		 * @inheritDoc
		 */
		public Collection values()
		{
			throw new UnsupportedOperationException();
		}

		/**
		 * @inheritDoc
		 */
		public Set entrySet()
		{
			throw new UnsupportedOperationException();
		}

		/**
		 * If the object is a SessionStateBindingListener, unbind it
		 * 
		 * @param attributeName
		 *        The attribute name.
		 * @param attribute
		 *        The attribute object
		 */
		protected void unBindAttributeValue(String attributeName, Object attribute)
		{
			// if this object wants session binding notification
			if ((attribute != null) && (attribute instanceof SessionStateBindingListener))
			{
				try
				{
					((SessionStateBindingListener) attribute).valueUnbound(null, attributeName);
				}
				catch (Throwable e)
				{
					M_log.warn("unBindAttributeValue: unbinding exception: ", e);
				}
			}
		}

		/**
		 * If the object is a SessionStateBindingListener, bind it
		 * 
		 * @param attributeName
		 *        The attribute name.
		 * @param attribute
		 *        The attribute object
		 */
		protected void bindAttributeValue(String attributeName, Object attribute)
		{
			// if this object wants session binding notification
			if ((attribute != null) && (attribute instanceof SessionStateBindingListener))
			{
				try
				{
					((SessionStateBindingListener) attribute).valueBound(null, attributeName);
				}
				catch (Throwable e)
				{
					M_log.warn("bindAttributeValue: unbinding exception: ", e);
				}
			}
		}
	}

	/**********************************************************************************************************************************************************************************************************************************************************
	 * Storage component
	 *********************************************************************************************************************************************************************************************************************************************************/

	protected class ClusterStorage implements Storage
	{
		/**
		 * Open and be ready to read / write.
		 */
		public void open()
		{
			// if we are auto-creating our schema, check and create
			if (m_autoDdl)
			{
				sqlService().ddl(this.getClass().getClassLoader(), "sakai_session");
			}
		}

		/**
		 * Close.
		 */
		public void close()
		{
		}

		/**
		 * Take this session into storage.
		 * 
		 * @param session
		 *        The usage session.
		 * @return true if added successfully, false if not.
		 */
		public boolean addSession(UsageSession session)
		{
			// and store it in the db
			String statement = "insert into SAKAI_SESSION (SESSION_ID,SESSION_SERVER,SESSION_USER,SESSION_IP,SESSION_USER_AGENT,SESSION_START,SESSION_END) values (?, ?, ?, ?, ?, ?, ?)";

			// collect the fields
			Object fields[] = new Object[7];
			fields[0] = session.getId();
			fields[1] = session.getServer();
			fields[2] = session.getUserId();
			fields[3] = session.getIpAddress();
			fields[4] = session.getUserAgent();
			fields[5] = session.getStart();
			fields[6] = session.getEnd();

			// process the insert
			boolean ok = sqlService().dbWrite(statement, fields);
			if (!ok)
			{
				M_log.warn(".addSession(): dbWrite failed");
				return false;
			}

			return true;

		} // addSession

		/**
		 * Access a session by id
		 * 
		 * @param id
		 *        The session id.
		 * @return The session object.
		 */
		public UsageSession getSession(String id)
		{
			UsageSession rv = null;

			// check the db
			String statement = "select SESSION_ID,SESSION_SERVER,SESSION_USER,SESSION_IP,SESSION_USER_AGENT,SESSION_START,SESSION_END from SAKAI_SESSION where SESSION_ID = ?";

			// send in the last seq number parameter
			Object[] fields = new Object[1];
			fields[0] = id;

			List sessions = sqlService().dbRead(statement, fields, new SqlReader()
			{
				public Object readSqlResultRecord(ResultSet result)
				{
					try
					{
						// read the UsageSession
						String id = result.getString(1);
						String server = result.getString(2);
						String userId = result.getString(3);
						String ip = result.getString(4);
						String agent = result.getString(5);
						Time start = timeService().newTime(result.getTimestamp(6, sqlService().getCal()).getTime());
						Time end = timeService().newTime(result.getTimestamp(7, sqlService().getCal()).getTime());

						UsageSession session = new BaseUsageSession(id, server, userId, ip, agent, start, end);
						return session;
					}
					catch (SQLException ignore)
					{
						return null;
					}
				}
			});

			if (!sessions.isEmpty()) rv = (UsageSession) sessions.get(0);

			return rv;

		} // getSession

		/**
		 * @inheritDoc
		 */
		public List getSessions(List ids)
		{
			// TODO: do this in a single SQL call! -ggolden
			List rv = new Vector();
			for (Iterator i = ids.iterator(); i.hasNext();)
			{
				String id = (String) i.next();
				UsageSession s = getSession(id);
				if (s != null)
				{
					rv.add(s);
				}
			}

			return rv;
		}

		/**
		 * Access a List of usage sessions by *arbitrary criteria* for te session ids.
		 * 
		 * @param joinTable
		 *        the table name to (inner) join to
		 * @param joinAlias
		 *        the alias used in the criteria string for the joinTable
		 * @param joinColumn
		 *        the column name of the joinTable that is to match the session id in the join ON clause
		 * @param joinCriteria
		 *        the criteria of the select (after the where)
		 * @param fields
		 *        Optional values to go with the criteria in an implementation specific way.
		 * @return The List (UsageSession) of UsageSession object for these ids.
		 */
		public List getSessions(String joinTable, String joinAlias, String joinColumn, String joinCriteria, Object[] values)
		{
			UsageSession rv = null;

			// use an alias different from the alias given
			String alias = joinAlias + "X";

			// use criteria as the where clause
			String statement = "select " + alias + ".SESSION_ID," + alias + ".SESSION_SERVER," + alias
					+ ".SESSION_USER," + alias + ".SESSION_IP," + alias + ".SESSION_USER_AGENT," + alias + ".SESSION_START," + alias + ".SESSION_END"
					+ " from SAKAI_SESSION " + alias
					+ " inner join " + joinTable + " " + joinAlias
					+ " ON " + alias + ".SESSION_ID = " + joinAlias + "." + joinColumn
					+ " where " + joinCriteria;

			List sessions = sqlService().dbRead(statement, values, new SqlReader()
			{
				public Object readSqlResultRecord(ResultSet result)
				{
					try
					{
						// read the UsageSession
						String id = result.getString(1);
						String server = result.getString(2);
						String userId = result.getString(3);
						String ip = result.getString(4);
						String agent = result.getString(5);
						Time start = timeService().newTime(result.getTimestamp(6, sqlService().getCal()).getTime());
						Time end = timeService().newTime(result.getTimestamp(7, sqlService().getCal()).getTime());

						UsageSession session = new BaseUsageSession(id, server, userId, ip, agent, start, end);
						return session;
					}
					catch (SQLException ignore)
					{
						return null;
					}
				}
			});

			return sessions;
		}

		/**
		 * This session is now closed.
		 * 
		 * @param session
		 *        The session which is closed.
		 */
		public void closeSession(UsageSession session)
		{
			// close the session on the db
			String statement = "update SAKAI_SESSION set SESSION_END = ? where SESSION_ID = ?";

			// collect the fields
			Object fields[] = new Object[2];
			fields[0] = session.getEnd();
			fields[1] = session.getId();

			// process the statement
			boolean ok = sqlService().dbWrite(statement, fields);
			if (!ok)
			{
				M_log.warn(".closeSession(): dbWrite failed");
			}

		} // closeSession

		/**
		 * Access a list of all open sessions.
		 * 
		 * @return a List (UsageSession) of all open sessions, ordered by server, then by start (asc)
		 */
		public List getOpenSessions()
		{
			UsageSession rv = null;

			// check the db
			String statement = "select SESSION_ID,SESSION_SERVER,SESSION_USER,SESSION_IP,SESSION_USER_AGENT,SESSION_START,SESSION_END"
					+ " from SAKAI_SESSION where SESSION_START = SESSION_END ORDER BY SESSION_SERVER ASC, SESSION_START ASC";

			List sessions = sqlService().dbRead(statement, null, new SqlReader()
			{
				public Object readSqlResultRecord(ResultSet result)
				{
					try
					{
						// read the UsageSession
						String id = result.getString(1);
						String server = result.getString(2);
						String userId = result.getString(3);
						String ip = result.getString(4);
						String agent = result.getString(5);
						Time start = timeService().newTime(result.getTimestamp(6, sqlService().getCal()).getTime());
						Time end = timeService().newTime(result.getTimestamp(7, sqlService().getCal()).getTime());

						UsageSession session = new BaseUsageSession(id, server, userId, ip, agent, start, end);
						return session;
					}
					catch (SQLException ignore)
					{
						return null;
					}
				}
			});

			return sessions;
		}
	}
}
