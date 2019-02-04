package com.bigjson.web;

import java.io.IOException;
import java.util.HashSet;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.bigjson.parser.JSONInterface;

@WebListener()
public class SessionListener implements HttpSessionListener{

	private HashSet<HttpSession> activeSessions = new HashSet<HttpSession>();
	
	@Override
	public void sessionCreated(HttpSessionEvent se) {
		System.out.println("Session is being created: " + se.getSession());
		activeSessions.add(se.getSession());
		System.out.println("Session " + se.getSession().getId() + " was created");
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		System.out.println("Session is being destroyed: " + se.getSession());
		HttpSession session = se.getSession();
		System.out.println("Session " + se.getSession().getId() + " is about to be destroyed");
		String fileName = (String)session.getAttribute(TreeLoadingServlet.ATTR_FILE_NAME);
		System.out.println("Parser of file "+fileName+" associated with session is about to be destroyed");
		try {
			JSONInterface parser = (JSONInterface) session.getAttribute(TreeLoadingServlet.ATTR_PARSER);
			parser.destroy();
			System.out.println("Parser destroyed");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		activeSessions.remove(session);
		
	}

	
}
