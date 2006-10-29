package org.sakaiproject.portal.charon.render;

import java.io.IOException;

public class ToolRenderException extends IOException {

    private Throwable t;

    public ToolRenderException(String string, Throwable throwable) {
        super(string);
        this.t = throwable;
    }
}
