package com.bigjson.web;

import java.util.HashSet;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

@WebListener()
public class SessionListener implements HttpSessionListener{

	private HashSet<HttpSession> activeSessions = new HashSet<HttpSession>();
	
	@Override
	public void sessionCreated(HttpSessionEvent se) {
		System.out.println("Session is being created: " + se.getSession());
		activeSessions.add(se.getSession());
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		System.out.println("Session is being destroyed: " + se.getSession());
		activeSessions.remove(se.getSession());
		
	}

	
}
