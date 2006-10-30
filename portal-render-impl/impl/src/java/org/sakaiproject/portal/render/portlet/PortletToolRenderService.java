package org.sakaiproject.portal.render.portlet;

import org.sakaiproject.portal.render.portlet.services.state.PortletStateAccess;
import org.sakaiproject.portal.render.portlet.services.state.PortletState;
import org.sakaiproject.portal.render.portlet.services.state.encode.PortletStateEncoder;
import org.sakaiproject.portal.render.portlet.services.SakaiPortalCallbackService;
import org.sakaiproject.portal.render.portlet.services.SakaiPortletContainerServices;
import org.sakaiproject.portal.render.portlet.services.SakaiPortalContext;
import org.sakaiproject.portal.render.api.ToolRenderException;
import org.sakaiproject.portal.render.api.ToolRenderService;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.site.api.ToolConfiguration;
import org.apache.pluto.PortletContainerException;
import org.apache.pluto.PortletContainer;
import org.apache.pluto.PortletContainerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.portlet.PortletException;
import javax.portlet.PortalContext;
import java.io.IOException;

/**
 *
 */
public class PortletToolRenderService implements ToolRenderService {

    private static final String CONTAINER_PARAM =
        "org.sakaiproject.portal.PORTLET_CONTAINER";

    private PortletStateEncoder portletStateEncoder;

    public PortletStateEncoder getPortletStateEncoder() {
        return portletStateEncoder;
    }

    public void setPortletStateEncoder(PortletStateEncoder portletStateEncoder) {
        this.portletStateEncoder = portletStateEncoder;
    }


    public void preprocess(ToolConfiguration toolConfiguration,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        ServletContext context)
        throws IOException, ToolRenderException {
        String stateParam = request.getParameter(SakaiPortalCallbackService.PORTLET_STATE_QUERY_PARAM);

        Tool tool = toolConfiguration.getTool();
        Placement placement = toolConfiguration;

        if (stateParam != null) {
            PortletState state = portletStateEncoder.decode(stateParam);
            if (state.isAction() && state.getId().equals(tool.getId())) {
                PortletStateAccess.setPortletState(request, state);
                org.sakaiproject.portal.render.portlet.SakaiPortletWindow window = createPortletWindow(tool, placement);
                window.setState(state);
                try {
                    PortletContainer portletContainer = getPortletContainer(context);
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
                       HttpServletResponse response,
                       ServletContext context)
            throws IOException, ToolRenderException {

        Tool tool = toolConfiguration.getTool();
        Placement placement = toolConfiguration;
        SakaiPortletWindow window = createPortletWindow(tool, placement);
        PortletState state = PortletStateAccess.getPortletState(request, window.getId().getStringId());
        window.setState(state);
        try {
            PortletContainer portletContainer = getPortletContainer(context);
            portletContainer.doRender(window, request, response);

        } catch (PortletContainerException e) {
            throw new ToolRenderException(e.getMessage(), e);
        } catch (PortletException e) {
            throw new ToolRenderException(e.getMessage(), e);
        }
    }

    private SakaiPortletWindow createPortletWindow(Tool tool, Placement placement) {
        String contextPath = placement.getContext();
        String windowId = placement.getId();
        String portletName = tool.getId();
        return new SakaiPortletWindow(windowId, contextPath, portletName);
    }


    private PortletContainer getPortletContainer(ServletContext context)
    throws PortletContainerException {
        PortletContainer container = (PortletContainer)
            context.getAttribute(CONTAINER_PARAM);

        if(container == null) {
            container = createPortletContainer();
            container.init(context);
            context.setAttribute(CONTAINER_PARAM, container);
        }

        return container;
    }

    private PortletContainer createPortletContainer()
        throws PortletContainerException {
        SakaiPortletContainerServices services = new SakaiPortletContainerServices();
        services.setPortalCallbackService(new SakaiPortalCallbackService());
        services.setPortalContext(new SakaiPortalContext());
        return PortletContainerFactory.getInstance().createContainer("sakai", services);
    }
}
