package org.sakaiproject.portal.charon.render.compat;

import org.sakaiproject.portal.charon.render.ToolRenderService;
import org.sakaiproject.portal.charon.render.ToolRenderException;
import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.site.api.ToolConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.io.IOException;

/**
 * Render serivice used to support both Portlet and
 * iframe based tools.
 *
 */
public class CompatibilityToolRenderService implements ToolRenderService {

    private static final Log LOG =
        LogFactory.getLog(CompatibilityToolRenderService.class);

    private ToolRenderService portletRenderService;
    private ToolRenderService iframeRenderService;
    private ServletContext context;


    public ServletContext getContext() {
        return context;
    }

    public void setServletContext(ServletContext context) {
        this.context = context;
    }


    public ToolRenderService getPortletRenderService() {
        return portletRenderService;
    }

    public void setPortletRenderService(ToolRenderService portletRenderService) {
        this.portletRenderService = portletRenderService;
    }

    public ToolRenderService getIframeRenderService() {
        return iframeRenderService;
    }

    public void setIframeRenderService(ToolRenderService iframeRenderService) {
        this.iframeRenderService = iframeRenderService;
    }

    public void preprocess(ToolConfiguration configuration,
                           HttpServletRequest request,
                           HttpServletResponse response)
            throws IOException, ToolRenderException {

    }

    public void render(ToolConfiguration configuration,
                       HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ToolRenderException {

        ServletContext crossContext = context.getContext(configuration.getContext());
        if(crossContext != null &&
           crossContext.getResource("WEB-INF/portlet.xml") != null) {
           portletRenderService.render(configuration, request, response);
        }
        else {
            iframeRenderService.render(configuration, request, response);
        }

    }
}
