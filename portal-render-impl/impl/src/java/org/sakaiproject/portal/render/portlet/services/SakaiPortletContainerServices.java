package org.sakaiproject.portal.render.portlet.services;

import org.apache.pluto.RequiredContainerServices;
import org.apache.pluto.spi.PortalCallbackService;

import javax.portlet.PortalContext;

/**
 */
public class SakaiPortletContainerServices implements RequiredContainerServices{

    private PortalContext portalContext;
    private PortalCallbackService portalCallbackService;

    public PortalContext getPortalContext() {
        return portalContext;
    }

    public void setPortalContext(PortalContext portalContext) {
        this.portalContext = portalContext;
    }

    public PortalCallbackService getPortalCallbackService() {
        return portalCallbackService;
    }

    public void setPortalCallbackService(PortalCallbackService portalCallbackService) {
        this.portalCallbackService = portalCallbackService;
    }
}
