package org.sakaiproject.portal.charon.render.portlet;

import org.sakaiproject.portal.charon.render.portlet.services.state.PortletStateAccess;
import org.sakaiproject.portal.charon.render.portlet.services.state.PortletState;
import org.sakaiproject.portal.charon.render.portlet.services.state.encode.PortletStateEncoder;
import org.sakaiproject.portal.charon.render.portlet.services.SakaiPortalCallbackService;
import org.sakaiproject.portal.charon.render.ToolRenderException;
import org.sakaiproject.portal.charon.render.ToolRenderService;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.site.api.ToolConfiguration;
import org.apache.pluto.PortletContainer;
import org.apache.pluto.PortletContainerException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.portlet.PortletException;
import java.io.IOException;

/**
 *
 */
public class PortletToolRenderService implements ToolRenderService {

    private PortletContainer portletContainer;
    private PortletStateEncoder encoder;

    public PortletContainer getPortletContainer() {
        return portletContainer;
    }

    public void setPortletContainer(PortletContainer portletContainer) {
        this.portletContainer = portletContainer;
    }


    public PortletStateEncoder getEncoder() {
        return encoder;
    }

    public void setEncoder(PortletStateEncoder encoder) {
        this.encoder = encoder;
    }

    public void setServletContext(ServletContext context) {

    }

    public void preprocess(ToolConfiguration toolConfiguration,
                        HttpServletRequest request,
                        HttpServletResponse response)
        throws IOException, ToolRenderException {
        String stateParam = request.getParameter(SakaiPortalCallbackService.PORTLET_STATE_QUERY_PARAM);

        Tool tool = toolConfiguration.getTool();
        Placement placement = toolConfiguration;

        if (stateParam != null) {
            PortletState state = encoder.decode(stateParam);
            if (state.isAction() && state.getId().equals(tool.getId())) {
                PortletStateAccess.setPortletState(request, state);
                org.sakaiproject.portal.charon.render.portlet.SakaiPortletWindow window = createPortletWindow(tool, placement);
                window.setState(state);
                try {
                    portletContainer.doAction(window, request, response);
                } catch (PortletException e) {
                    throw new ToolRenderException(e.getMessage(), e);
                } catch (PortletContainerException e) {
                    throw new ToolRenderException(e.getMessage(), e);
                }
            }
        }
    }

    public void render(ToolConfiguration toolConfiguration,
                       HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ToolRenderException {

        Tool tool = toolConfiguration.getTool();
        Placement placement = toolConfiguration;
        SakaiPortletWindow window = createPortletWindow(tool, placement);
        PortletState state = PortletStateAccess.getPortletState(request, window.getId().getStringId());
        window.setState(state);
        try {
            portletContainer.doRender(window, request, response);

        } catch (PortletContainerException e) {
            throw new ToolRenderException(e.getMessage(), e);
        } catch (PortletException e) {
            throw new ToolRenderException(e.getMessage(), e);
        }
    }

    private org.sakaiproject.portal.charon.render.portlet.SakaiPortletWindow createPortletWindow(Tool tool, Placement placement) {
        String contextPath = placement.getContext();
        String windowId = placement.getId();
        String portletName = tool.getId();
        return new SakaiPortletWindow(windowId, contextPath, portletName);
    }


}
