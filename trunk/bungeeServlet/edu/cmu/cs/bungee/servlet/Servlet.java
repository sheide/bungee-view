package edu.cmu.cs.bungee.servlet;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.cmu.cs.bungee.javaExtensions.Util;

enum Command {
	CONNECT, CLOSE, getCountsIgnoringFacet, ABOUT_COLLECTION, getFilteredCounts, updateOnItems, prefetch, offsetItems, getThumbs, cluster, getDescAndImage, getItemInfo, ITEM_URL, itemIndex, itemIndexFromURL, restrict, baseFacets, getFilteredCountTypes, addItemsFacet, addChildFacet, removeItemFacet, reparent, addItemFacet, writeback, rotate, rename, removeItemsFacet, getNames, reorderItems, setItemDescription, opsSpec, getLetterOffsets, caremediaPlayArgs, caremediaGetItems, getPairCounts
}

public class Servlet extends HttpServlet {

	private Map<Integer, Database> sessions = new HashMap<Integer, Database>();

	// int sessionCounter = 1;
	private Random sessionGenerator = new Random();

	private static Command parseCommand(String command) {
		Command result = Command.valueOf(command);
		// Database.myAssert(result != null, "Unknown command: " + command);
		return result;
	}

	private static final long serialVersionUID = 8922913873736902656L;

	// This method is called by the servlet container just before this servlet
	// is put into service.
	// public void init() throws ServletException {
	// log("getinit init");
	// }

	// This method is called by the servlet container just after this servlet
	// is removed from service.
	@Override
	synchronized public void destroy() {
		// log("destroy");
		for (Iterator<Database> iterator = sessions.values().iterator(); iterator
				.hasNext();) {
			Database db = iterator.next();
			try {
				db.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		sessions.clear();
	}

	private synchronized void close(Integer session) throws SQLException {
		Database db = sessions.get(session);
		if (db != null) {
			// log("close database");
			db.close();
		}
		sessions.remove(session);
	}

	private synchronized void addSession(Integer xsession, Database db)
			throws SQLException {
		if (xsession != null) {
			close(xsession);
		}
		// log("open database");
		sessions.put(xsession, db);
	}

	private synchronized Integer getSession(HttpServletRequest request) {
		String sessionName = request.getParameter("session");
		int n = sessionName == null ? sessionGenerator.nextInt() : Integer
				.parseInt(sessionName);
		return Integer.valueOf(n);

	}

	private int handleItemIndex(int item, int table, int nNeighbors,
			Database db, DataOutputStream out) throws ServletException,
			SQLException, IOException {
		int itemOffset = db.itemOffset(item, table);

		// servletInterface will subtract 1 from the result.
		Database.writeInt(itemOffset + 1, out);

		if (nNeighbors > 1) {
			int base = Math.max(0, itemOffset);
			int minOffset = Math.max(0, base - nNeighbors + 1);
			int maxOffset = base + nNeighbors;

			Database.writeInt(minOffset, out);
			// Database.writeInt(maxOffset, out);
			db.offsetItems(minOffset, maxOffset, table, out);
		}
		// log("itemIndex " + intResult);
		return itemOffset;
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		// Called when you go to a bookmark in a browser
		log("Who is sending doGet??? Calling doPost: ");
		logRequest(request);
		doPost(request, response);
	}

	@SuppressWarnings("unchecked")
	void logRequest(HttpServletRequest request) {
		log("Request info: " + request.getRequestURL().toString() + " "
				+ request.getQueryString() + " " + request.getRemoteHost());
		Enumeration<String> e = request.getHeaderNames();
		while (e.hasMoreElements()) {
			String s = e.nextElement();
			log(s + ": " + request.getHeader(s));
		}
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		// This doesn't work - make sure the Tomcat Connector is configured for
		// UTF_8, e.g. <Connector port="80" URIEncoding="UTF-8"/>. Otherwise
		// getParameter will decode wrong.
		//
		// if (request.getCharacterEncoding() == null)
		// request.setCharacterEncoding("UTF-8");

		String errMsg = null;
		DataOutputStream out = null;
		try {
			Integer xsession = getSession(request);
			Command command = parseCommand(request.getParameter("command"));
			// if (command != Command.prefetch)
			// log("doPost " + request.getQueryString());
			if (command == Command.CONNECT) {
				String dbName = request.getParameter("arg1");
				ServletConfig config = getServletConfig();
				String server = config.getInitParameter("server");
				String user = config.getInitParameter("user");
				String pass = config.getInitParameter("pwd");
				String[] dbNames = config.getInitParameter("dbs").split(",");
				log(Util.valueOfDeep(dbNames)+" "+dbName+" "+request.getRemoteHost()+" "+config.getInitParameter(
				"IPpermissions"));
				if (dbName == null) {
					dbName = dbNames[0];
				} else if (!Util.isMember(dbNames, dbName)) {
					String requestIP = dbName + request.getRemoteHost();
					boolean isAuthorized = false;
					String[] authorizedIPs = config.getInitParameter(
							"IPpermissions").split(",");
					for (int i = 0; i < authorizedIPs.length && !isAuthorized; i++) {
						isAuthorized = requestIP.startsWith(authorizedIPs[i]);
						log(i+" "+isAuthorized+" "+requestIP+" "+authorizedIPs[i]);
					}
					if (!isAuthorized)
						errMsg = "Your IP address, " + request.getRemoteHost()
								+ ", is not authorized to use database "
								+ dbName;
				}
				logRequest(request);
				if (errMsg == null) {
					log("Connect to " + dbName + " session = " + xsession);
					Database db;
					try {
						db = new Database(server, dbName, user, pass, this);
						// db.jdbc.servlet = this;
						addSession(xsession, db);
					} catch (Exception e) {
						errMsg = "Could not connect to database " + dbName
								+ " because\n" + e.getMessage() + "\nbecause\n"
								+ e.getCause() + "\n"
								+ Util.join(e.getStackTrace(), "\n");
					}
				}
			}
			Database db = sessions.get(xsession);
			if (errMsg == null && db == null)
				errMsg = "No database associated with session '" + xsession
						+ "'";

			if (errMsg == null) {
				response.setContentType("application/octet-stream");
				response.setHeader("pragma", "no-cache");
				out = new DataOutputStream(new DeflaterOutputStream(
						new BufferedOutputStream(response.getOutputStream()),
						new Deflater(Deflater.BEST_COMPRESSION)));
				// response.setContentLength(999);

				// log("...doPost " + command + " to db");
				try {
					doPostInternal(xsession, command, out, request);
					// } catch (SQLException e) {
					// errMsg = "Could not " + request.getQueryString() + "
					// because
					// "
					// + e;
				} catch (Throwable e) {
					errMsg = "Could not " + request.getQueryString()
							+ " because\n" + e + "\n"
							+ Util.join(e.getStackTrace(), "\n") + "\n"
							+ e.getCause();
				}
			}
			if (errMsg != null) {
				log(errMsg);
				if (!response.isCommitted()) {
					response.sendError(
							HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							errMsg);
				}
			}
		} finally {
			if (out != null)
				out.close();
		}
		// log("...doPost " + command + " done");
	}

	private void doPostInternal(Integer xsession, Command command,
			DataOutputStream out, HttpServletRequest request)
			throws IOException, ServletException, SQLException {
		handleUserActions(xsession, request);
		Database db = sessions.get(xsession);
		switch (command) {
		case CONNECT:
			// log("session = '" + xsession.toString() + "'");
			Database.writeString(xsession.toString(), out);
			Database.writeString(db.dbDescs(getServletConfig()
					.getInitParameter("dbs")), out);
			// Database.writeString(getServletConfig().getInitParameter("dbs"),
			// out);
			Database.writeInt(db.facetCount(), out);
			Database.writeInt(db.itemCount(), out);
			String[] globals = db.getGlobals();
			for (int i = 0; i < globals.length; i++)
				Database.writeString(globals[i], out);
			db.initPerspectives(out);
			db.init(out);
			break;
		case ABOUT_COLLECTION:
			Database.writeString(db.aboutCollection(), out);
			break;
		case getCountsIgnoringFacet:
			db.getCountsIgnoringFacet(request.getParameter("arg1"), request
					.getParameter("arg2"), out);
			break;
		case ITEM_URL:
			Database.writeString(db
					.getItemURL(getIntParameter(request, "arg1")), out);
			break;
		case getFilteredCounts:
			db.getFilteredCounts(request.getParameter("arg1"), request
					.getParameter("arg2"), out);
			break;
		case getFilteredCountTypes:
			db.getFilteredCountTypes(out);
			break;
		case prefetch:
			db.prefetch(getIntParameter(request, "arg1"), getIntParameter(
					request, "arg2"), out);
			break;
		case getLetterOffsets:
			db.getLetterOffsets(getIntParameter(request, "arg1"), request
					.getParameter("arg2"), out);
			break;
		case getNames:
			db.getNames(request.getParameter("arg1"), out);
			break;
		case offsetItems:
			db.offsetItems(getIntParameter(request, "arg1"), getIntParameter(
					request, "arg2"), getIntParameter(request, "arg3"), out);
			break;
		case reorderItems:
			db.reorderItems(getIntParameter(request, "arg1"));
			break;
		case getThumbs:
			db.getThumbs(request.getParameter("arg1"), getIntParameter(request,
					"arg2"), getIntParameter(request, "arg3"), getIntParameter(
					request, "arg4"), out);
			break;
		case cluster:
			int maxClusters = getIntParameter(request, "arg1");
			int maxClusterSize = getIntParameter(request, "arg2");
			String facetRestriction = request.getParameter("arg3");
			double pValue = getDoubleParameter(request, "arg4");
			db.cluster(maxClusters, maxClusterSize, facetRestriction, pValue,
					out);
			break;
		case getDescAndImage:
			int arg1 = getIntParameter(request, "arg1");
			db.getItemInfo(arg1, out);
			db.getDescAndImage(arg1, getIntParameter(request, "arg2"),
					getIntParameter(request, "arg3"), getIntParameter(request,
							"arg4"), out);
			break;
		case updateOnItems:
			String subQuery = request.getParameter("arg1");
			int item = getIntParameter(request, "arg2");
			int table = getIntParameter(request, "arg3");
			int nNeighbors = getIntParameter(request, "arg4");
			int nItems = db.updateOnItems(subQuery);
			if (Database.writeInt(nItems, out) > 0) {
				// int index =
				handleItemIndex(item, table, nNeighbors, db, out);
				// servletInterface will subtract 1 from the result.
				// Database.writeInt(index + 1, out);
			}
			break;
		case itemIndexFromURL:
			item = db.getItemFromURL(request.getParameter("arg1"));
			Database.writeInt(item, out);
			table = getIntParameter(request, "arg2");
			nNeighbors = getIntParameter(request, "arg3");

			// servletInterface will subtract 1 from the result.
			// Database.writeInt(
			handleItemIndex(item, table, nNeighbors, db, out);
			// +1, out);

			break;
		case itemIndex:
			item = getIntParameter(request, "arg1");
			table = getIntParameter(request, "arg2");
			nNeighbors = getIntParameter(request, "arg3");

			// servletInterface will subtract 1 from the result.
			// Database.writeInt(
			handleItemIndex(item, table, nNeighbors, db, out);
			// +1, out);

			break;
		case setItemDescription:
			item = getIntParameter(request, "arg1");
			String description = request.getParameter("arg2");
			db.setItemDescription(item, description);
			break;
		case getPairCounts:
			String facets = request.getParameter("arg1");
			String universe = request.getParameter("arg2");
			db.getPairCounts(facets, universe, out);
			break;
		case opsSpec:
			int session = getIntParameter(request, "arg1");
			db.opsSpec(session, out);
			break;
		case restrict:
			db.restrict();
			break;
		// case baseFacets:
		// // printRecords(db.baseFacets(out), MyResultSet.SNMINT_PINT);
		// db.baseFacets(out);
		// break;
		case addItemFacet:
			int facet = getIntParameter(request, "arg1");
			item = getIntParameter(request, "arg2");
			db.addItemFacet(facet, item, out);
			break;
		case addItemsFacet:
			facet = getIntParameter(request, "arg1");
			db.addItemsFacet(facet, out);
			break;
		case removeItemsFacet:
			facet = getIntParameter(request, "arg1");
			db.removeItemsFacet(facet, out);
			break;
		case addChildFacet:
			facet = getIntParameter(request, "arg1");
			String name = request.getParameter("arg2");
			db.addChildFacet(facet, name, out);
			break;
		case removeItemFacet:
			facet = getIntParameter(request, "arg1");
			item = getIntParameter(request, "arg2");
			db.removeItemFacet(facet, item, out);
			break;
		case reparent:
			int parent = getIntParameter(request, "arg1");
			int child = getIntParameter(request, "arg2");
			db.reparent(parent, child, out);
			break;
		case writeback:
			db.writeBack();
			break;
		case rotate:
			item = getIntParameter(request, "arg1");
			int theta = getIntParameter(request, "arg2");
			db.rotate(item, theta);
			break;
		case rename:
			facet = getIntParameter(request, "arg1");
			name = request.getParameter("arg2");
			db.rename(facet, name);
			break;
		case caremediaPlayArgs:
			String items = request.getParameter("arg1");
			db.caremediaPlayArgs(items, out);
			break;
		case caremediaGetItems:
			String segments = request.getParameter("arg1");
			db.caremediaGetItems(segments, out);
			break;
		case CLOSE:
			close(xsession);
			break;
		default:
			throw (new ServletException("Unknown command: " + command));
		}
		// log("...doPost " + command + " writing");
	}

	private void handleUserActions(Integer xsession, HttpServletRequest request)
			throws ServletException, SQLException {
		String actionsString = request.getParameter("userActions");
		if (actionsString != null) {
			Database db = sessions.get(xsession);
			String[] actions = Util.splitSemicolon(actionsString);
			for (int i = 0; i < actions.length; i++) {
				String[] actionString = Util.splitComma(actions[i]);
				Database.myAssert(actionString.length == 4, "Bad argString: '"
						+ actions[i] + "' in '" + actionsString + "'");
				int actionIndex = Integer.parseInt(actionString[0]);
				int location = Integer.parseInt(actionString[1]);
				String object = actionString[2];
				int modifiers = Integer.parseInt(actionString[3]);
				db.printUserAction(request.getRemoteHost(),
						xsession.intValue(), actionIndex, location, object,
						modifiers);
			}
		}
	}

	private static int getIntParameter(HttpServletRequest request,
			String argSpec) {
		String arg = request.getParameter(argSpec);
		return Integer.parseInt(arg);
	}

	private static double getDoubleParameter(HttpServletRequest request,
			String argSpec) {
		String arg = request.getParameter(argSpec);
		return Double.parseDouble(arg);
	}

	// private static boolean getBooleanParameter(HttpServletRequest request,
	// String argSpec) {
	// String arg = request.getParameter(argSpec);
	// return Boolean.valueOf(arg).booleanValue();
	// }

}
