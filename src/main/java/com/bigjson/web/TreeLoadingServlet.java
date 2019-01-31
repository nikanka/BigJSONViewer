package com.bigjson.web;
import java.io.*;
import java.util.List;

//import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.bigjson.parser.JSONInterface;
import com.bigjson.parser.JSONNode;

@SuppressWarnings("serial")
@WebServlet("/TreeLoadingServlet")
public class TreeLoadingServlet extends HttpServlet{
	String fileName = "/testFiles/SmallTest2.json";
	
	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		super.destroy();
		System.out.println("Servlet is being destroyed");
		
	}
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String state = req.getParameter("state");
		String dir = this.getClass().getClassLoader().getResource("").getPath();
		// Set response content type
	    resp.setContentType("text/html");
		if(state.equals("initLoad")){
			// load and parse the whole file and load top level
			HttpSession session = req.getSession(true);
			// use default file for now
			// TODO: load custom file
			// create parser interface
			File f = new File(dir, fileName);
			JSONInterface parser = new JSONInterface(f.getPath());
			session.setAttribute("parser", parser);
			JSONNode root = parser.getRoot();
			try(PrintWriter out = resp.getWriter()){
				out.print("[");
				writeNodeInJSON(out, root);
				out.print("]");
			}
		} else if(state.equals("loadChildren")){
			//load children
			String posString = req.getParameter("pos");
			if (posString == null) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
						"Request does not contain a file position of the to-be-expanded node");
			}
			Long pos = Long.parseLong(posString);
			HttpSession session = req.getSession(false);
			if(session == null){
				//TODO
			}
			JSONInterface parser = (JSONInterface)session.getAttribute("parser");
			List<JSONNode> children = parser.loadChildren(pos);
			try(PrintWriter out = resp.getWriter()){
				out.print("[");
				boolean isFirst = true;
				for(JSONNode node: children){
					if(!isFirst){
						out.print(",");
					} else {
						isFirst = false;
					}
					writeNodeInJSON(out, node);
					
				}
				out.println("]");
		    }	
		} else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"Unknown response state: '" + state + "'");
		}
	      
	}
	
	private static void writeNodeInJSON(PrintWriter out, JSONNode node){
		boolean lazy = node.getType() != JSONNode.TYPE_STRING && !node.isFullyLoaded();
		out.print("{\"title\": \"" + getNodeTitle(node) + "\", "
				+ "\"data\": {" 
					+ "\"pos\": " + node.getStartFilePosition() +"}, "
				+ "\"lazy\":" + (lazy?"true":"false") + "}");
	}
	
	private static String getNodeTitle(JSONNode node){
		if(node.getType() == JSONNode.TYPE_ARRAY || 
				node.getType() == JSONNode.TYPE_OBJECT){
			return node.getName()!=null?node.getName():"";
		}
		String ret =  node.getName()!=null?(node.getName()+" : "):"";
		if(node.getType() == JSONNode.TYPE_NUMBER || 
				node.getType() == JSONNode.TYPE_KEYWORD){
			return ret + node.getValue();
		} 
		if(node.getType() == JSONNode.TYPE_STRING){
			return ret + "\\\""+node.getValue() + (node.isFullyLoaded()?"":"...") + "\\\"";
		} else {
			throw new IllegalArgumentException("Unrecognized JSONNode type: '" + node.getType() + "'");
		}
	}
}
