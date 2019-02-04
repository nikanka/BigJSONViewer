package com.bigjson.web;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

//import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import com.bigjson.parser.JSONInterface;
import com.bigjson.parser.JSONNode;

@SuppressWarnings("serial")
@WebServlet("/TreeLoadingServlet")
@MultipartConfig
public class TreeLoadingServlet extends HttpServlet {
	static final String ATTR_FILE_NAME = "JSONFileName";
	static final String ATTR_PARSER = "parser";
	Path tempDir = null;
	List<JSONInterface> activeParsers = new ArrayList<JSONInterface>();

	@Override
	public void destroy() {
		super.destroy();
		System.out.println("Servlet is being destroyed");
		try {
			for (JSONInterface parser : activeParsers) {
				parser.destroy();
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}

	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		System.out.println("Get to the Servlet Post!");
		Part filePart = req.getPart("file"); // Retrieves <input type="file"
												// name="file">
		if (filePart == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No file part found" + req.getAttributeNames());
		}
		String fileName = getFileName(filePart);// Paths.get(filePart.getSubmittedFileName()).getFileName().toString();
												// // MSIE fix.
		if (tempDir == null) {
			tempDir = Files.createTempDirectory("tmp1");
		}
		File file = new File(tempDir.toFile(), fileName);
		HttpSession session = req.getSession();
		session.setAttribute(ATTR_FILE_NAME, file.getAbsolutePath());
		System.out.println("Saved file name '" + file.getAbsolutePath() + "' to the session");
		try (OutputStream out = new FileOutputStream(file);
				InputStream filecontent = filePart.getInputStream();
				PrintWriter writer = resp.getWriter();) {

			int read = 0;
			final byte[] bytes = new byte[1024];

			while ((read = filecontent.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			writer.println("Your file " + fileName + " saved to " + tempDir);
		} catch (FileNotFoundException fne) {
			System.out.println("Problems during file upload. Error: " + fne.getMessage());
		}
	};

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String state = req.getParameter("state");
		if (state == null) {
			System.out.println("'state' paremeter is null");
			return;
		}
		// Set response content type
		resp.setContentType("text/html");
		if (state.equals("initLoad")) {
			// load and parse the whole file and load top level
			HttpSession session = req.getSession(false);
			if (session == null) {
				// TODO
			}

			File f = new File((String) session.getAttribute(ATTR_FILE_NAME));
			// TODO: if no file name attr?
			// create parser interface
			JSONInterface parser = new JSONInterface(f.getPath());
			activeParsers.add(parser);// make parser manager maybe?
			session.setAttribute(ATTR_PARSER, parser);
			JSONNode root = parser.getRoot();
			try (PrintWriter out = resp.getWriter()) {
				out.print("[");
				writeNodeInJSON(out, root);
				out.print("]");
			}
		} else if (state.equals("loadChildren")) {
			// load children
			String posString = req.getParameter("pos");
			if (posString == null) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
						"Request does not contain a file position of the to-be-expanded node");
			}
			Long pos = Long.parseLong(posString);
			HttpSession session = req.getSession(false);
			if (session == null) {
				// TODO
			}
			JSONInterface parser = (JSONInterface) session.getAttribute(ATTR_PARSER);
			List<JSONNode> children = parser.loadChildren(pos);
			try (PrintWriter out = resp.getWriter()) {
				out.print("[");
				boolean isFirst = true;
				for (JSONNode node : children) {
					if (!isFirst) {
						out.print(",");
					} else {
						isFirst = false;
					}
					writeNodeInJSON(out, node);

				}
				out.println("]");
			}
		} else if (state.equals("destroySession")) {
			HttpSession session = req.getSession(false);
			if (session != null) {
				session.getAttribute(ATTR_FILE_NAME);
				session.invalidate();
				try (PrintWriter out = resp.getWriter()) {
					out.println("Session " + session.getId() + "is sent to invalidation");
				}
			}
		} else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown response state: '" + state + "'");
		}

	}

	private String getFileName(final Part part) {
		final String partHeader = part.getHeader("content-disposition");
		System.out.println("Part Header = {0}" + partHeader);
		for (String content : part.getHeader("content-disposition").split(";")) {
			if (content.trim().startsWith("filename")) {
				return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
			}
		}
		return null;
	}

	private static void writeNodeInJSON(PrintWriter out, JSONNode node) {
		boolean lazy = node.getType() != JSONNode.TYPE_STRING && !node.isFullyLoaded();
		out.print("{\"title\": \"" + getNodeTitle(node) + "\", " + "\"data\": {" + "\"pos\": "
				+ node.getStartFilePosition() + "}, " + "\"lazy\":" + (lazy ? "true" : "false") + "}");
	}

	private static String getNodeTitle(JSONNode node) {
		if (node.getType() == JSONNode.TYPE_ARRAY || node.getType() == JSONNode.TYPE_OBJECT) {
			return node.getName() != null ? node.getName() : "";
		}
		String ret = node.getName() != null ? (node.getName() + " : ") : "";
		if (node.getType() == JSONNode.TYPE_NUMBER || node.getType() == JSONNode.TYPE_KEYWORD) {
			return ret + node.getValue();
		}
		if (node.getType() == JSONNode.TYPE_STRING) {
			return ret + "\\\"" + node.getValue() + (node.isFullyLoaded() ? "" : "...") + "\\\"";
		} else {
			throw new IllegalArgumentException("Unrecognized JSONNode type: '" + node.getType() + "'");
		}
	}
}
