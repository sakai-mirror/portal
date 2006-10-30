package org.sakaiproject.portal.render.portlet.services.state;

import javax.portlet.PortletMode;
import javax.portlet.WindowState;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.HashMap;

/**
 *
 */
public class PortletState implements Serializable {

    private String id;
    private boolean action;
    private boolean secure;
    private Map parameters;

    private transient PortletMode portletMode;
    private transient WindowState windowState;


    public PortletState(String id) {
        this.id = id;
        portletMode = PortletMode.VIEW;
        windowState = WindowState.NORMAL;
        parameters = new HashMap();
    }


    public PortletState(PortletState currentState) {
        this(currentState.getId());
        setAction(currentState.isAction());
        setSecure(currentState.isSecure());
        getParameters().putAll(currentState.getParameters());
        setPortletMode(currentState.getPortletMode());
        setWindowState(currentState.getWindowState());
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isAction() {
        return action;
    }

    public void setAction(boolean action) {
        this.action = action;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public Map getParameters() {
        return parameters;
    }

    public void setParameters(Map parameters) {
        this.parameters = parameters;
    }

    public void clearParameters() {
        this.parameters.clear();
    }

    public PortletMode getPortletMode() {
        return portletMode;
    }

    public void setPortletMode(PortletMode portletMode) {
        this.portletMode = portletMode;
    }

    public WindowState getWindowState() {
        return windowState;
    }

    public void setWindowState(WindowState windowState) {
        this.windowState = windowState;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PortletState that = (PortletState) o;

        if (action != that.action) return false;
        if (secure != that.secure) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (parameters != null ? !parameters.equals(that.parameters) : that.parameters != null) return false;
        if (portletMode != null ? !portletMode.equals(that.portletMode) : that.portletMode != null) return false;
        if (windowState != null ? !windowState.equals(that.windowState) : that.windowState != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (id != null ? id.hashCode() : 0);
        result = 31 * result + (action ? 1 : 0);
        result = 31 * result + (secure ? 1 : 0);
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        result = 31 * result + (portletMode != null ? portletMode.hashCode() : 0);
        result = 31 * result + (windowState != null ? windowState.hashCode() : 0);
        return result;
    }

// Serialization

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(id);
        out.writeBoolean(action);
        out.writeBoolean(secure);
        out.writeObject(parameters);
        out.writeObject(portletMode.toString());
        out.writeObject(windowState.toString());
    }

    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        id = in.readObject().toString();
        action = in.readBoolean();
        secure = in.readBoolean();
        parameters = (Map)in.readObject();
        portletMode = new PortletMode(in.readObject().toString());
        windowState = new WindowState(in.readObject().toString());
    }
}
