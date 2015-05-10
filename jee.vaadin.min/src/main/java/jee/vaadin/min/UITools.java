package jee.vaadin.min;

import com.vaadin.server.Page;
import com.vaadin.server.VaadinServlet;

public class UITools {

	public static String getStartLocation() {
		final Page page = Page.getCurrent();
		final StringBuilder sbuf = new StringBuilder();
		sbuf.append(getServerLocation(page));
		// ContextPath already has / infront
		sbuf.append(VaadinServlet.getCurrent().getServletContext().getContextPath());
		return sbuf.toString();
	}

	public static String getServerLocation(final Page page) {
		final StringBuilder sbuf = new StringBuilder();
		sbuf.append(page.getLocation().getScheme());
		sbuf.append("://");
		sbuf.append(page.getLocation().getHost());
		sbuf.append(":");
		sbuf.append(page.getLocation().getPort());
		return sbuf.toString();
	}
	
}
