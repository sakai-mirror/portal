package org.sakaiproject.portal.charon.render;

import org.sakaiproject.tool.api.Tool;
import org.sakaiproject.site.api.ToolConfiguration;
import org.sakaiproject.site.api.Site;


public class RenderContext {

    private Tool tool;
    private ToolConfiguration toolConfiguration;
    private Site site;


    public Tool getTool() {
        return tool;
    }

    public void setTool(Tool tool) {
        this.tool = tool;
    }

    public ToolConfiguration getToolConfiguration() {
        return toolConfiguration;
    }

    public void setToolConfiguration(ToolConfiguration toolConfiguration) {
        this.toolConfiguration = toolConfiguration;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }
}
