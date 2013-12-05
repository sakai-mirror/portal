package org.sakaiproject.portal.render.portlet.services.state;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: jbush
 * Date: 11/21/13
 * Time: 9:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class PortletStateContainer implements Serializable {

    private Map stateMap = new HashMap();

    public void add(PortletState state) {
        stateMap.put(state.getId(), state);
    }

    public PortletState get(String windowId) {
        return (PortletState) stateMap.get(windowId);
    }
}
