package org.sakaiproject.portal.charon.render;

import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.site.api.ToolConfiguration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface ToolRenderService {

    /**
     * Perfrorm any preperatory processing
     * for the specified tool.
     *
     * @param toolConfiguration
     * @param request
     * @param response
     * @throws IOException
     * @throws ToolRenderException
     */
    void preprocess(ToolConfiguration toolConfiguration,
                    HttpServletRequest request, HttpServletResponse response)
        throws IOException, ToolRenderException;

    /**
     * Render the tool.
     *
     * @param toolConfiguration
     * @param request
     * @param response
     * @throws IOException
     * @throws ToolRenderException
     */
    void render(ToolConfiguration toolConfiguration,
                HttpServletRequest request, HttpServletResponse response)
        throws IOException, ToolRenderException;


}
