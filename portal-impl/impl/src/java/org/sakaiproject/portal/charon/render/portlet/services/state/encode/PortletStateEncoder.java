package org.sakaiproject.portal.charon.render.portlet.services.state.encode;

import org.sakaiproject.portal.charon.render.portlet.services.state.PortletState;

/**
 * Created by IntelliJ IDEA.
 * User: ddewolf
 * Date: Oct 28, 2006
 * Time: 5:14:11 PM
 * To change this template use File | Settings | File Templates.
 */
public interface PortletStateEncoder {
    String encode(PortletState portletState);

    PortletState decode(String uri);
}
