package org.sakaiproject.portal.render.iframe;

import org.sakaiproject.portal.render.api.ToolRenderService;
import org.sakaiproject.portal.render.api.ToolRenderException;
import org.sakaiproject.util.Web;
import org.sakaiproject.util.ResourceLoader;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.component.cover.ServerConfigurationService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.PrintWriter;

public class IFrameToolRenderService implements ToolRenderService {

    private static ResourceLoader rb = new ResourceLoader("sitenav");

    public void preprocess(ToolConfiguration toolConfiguration,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           ServletContext context)
        throws IOException, ToolRenderException {
    }

    public void render(ToolConfiguration configuration,
                       HttpServletRequest request,
                       HttpServletResponse response,
                       ServletContext context)
        throws IOException, ToolRenderException {

        PrintWriter out = response.getWriter();
        String titleString = Web.escapeHtml(configuration.getTitle());
        		String toolUrl = ServerConfigurationService.getToolUrl() + "/"
				+ Web.escapeUrl(configuration.getId());

        out.println("<iframe");
		out.println("	name=\""
				+ Web.escapeJavascript("Main" + configuration.getId()) + "\"");
		out.println("	id=\"" + Web.escapeJavascript("Main" + configuration.getId())
				+ "\"");
		out.println("	title=\"" + titleString + " "
				+ Web.escapeHtml(rb.getString("sit.contentporttit")) + "\"");
		out.println("	class =\"portletMainIframe\"");
		out.println("	height=\"50\"");
		out.println("	width=\"100%\"");
		out.println("	frameborder=\"0\"");
		out.println("	marginwidth=\"0\"");
		out.println("	marginheight=\"0\"");
		out.println("	scrolling=\"auto\"");
		out.println("	src=\"" + toolUrl + "?panel=Main\">");
		out.println("</iframe>");


    }
}
