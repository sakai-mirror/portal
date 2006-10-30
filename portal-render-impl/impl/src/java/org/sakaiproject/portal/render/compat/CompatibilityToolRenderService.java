package org.sakaiproject.portal.render.compat;

import org.sakaiproject.portal.render.api.ToolRenderService;
import org.sakaiproject.portal.render.api.ToolRenderException;
import org.sakaiproject.site.api.ToolConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Render serivice used to support both Portlet and
 * iframe based tools.
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
                           HttpServletResponse response,
                           ServletContext context)
            throws IOException, ToolRenderException {
        if (isIn168TestMode(request) || isPortletApplication(context, configuration.getContext())) {
            portletRenderService.preprocess(configuration, request, response, context);
        } else {
            iframeRenderService.preprocess(configuration, request, response, context);
        }
    }

    public void render(ToolConfiguration configuration,
                       HttpServletRequest request,
                       HttpServletResponse response,
                       ServletContext context)
            throws IOException, ToolRenderException {

        if (isIn168TestMode(request) || isPortletApplication(context, configuration.getContext())) {
            portletRenderService.render(configuration, request, response, context);
        } else {
            iframeRenderService.render(configuration, request, response, context);
        }
    }

    private boolean isIn168TestMode(HttpServletRequest request) {
        return Boolean.TRUE.toString().equalsIgnoreCase(request.getParameter("test168"));
    }

    private boolean isPortletApplication(ServletContext context, String toolContext)
            throws MalformedURLException {
        ServletContext crossContext = context.getContext(toolContext);
        return crossContext != null &&
                crossContext.getResource("WEB-INF/portlet.xml") != null;
    }
}
