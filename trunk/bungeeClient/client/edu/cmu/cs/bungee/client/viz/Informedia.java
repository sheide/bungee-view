package edu.cmu.cs.bungee.client.viz;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import edu.cmu.cs.bungee.client.query.Query.Item;
import edu.cmu.cs.bungee.javaExtensions.MyResultSet;
import edu.cmu.cs.bungee.javaExtensions.URLQuery;
import edu.cmu.cs.bungee.javaExtensions.Util;

// To seal jar (password is 8 digit insecure)
// cd C:\Program Files\Apache Software Foundation\Tomcat 6.0.16\webapps\bungee
// "C:\Program Files\Java\jdk1.5.0_06\bin\jarsigner" -keystore myKeys bungeeClient.jar BungeeView
// mv bungeeClientSigned.jar bungeeClientSignedOLD.jar
// mv bungeeClient.jar bungeeClientSigned.jar

// To renew certificate
// cd C:\Program Files\Apache Software Foundation\Tomcat 6.0.16\webapps\bungee
// "C:\Program Files\Java\jdk1.5.0_06\bin\keytool" -keystore myKeys -delete -alias bungeeview
// "C:\Program Files\Java\jdk1.5.0_06\bin\keytool" -keystore myKeys -genkey -alias bungeeview

public class Informedia extends Thread {

	int port;
	Bungee art;
	PrintWriter out;

	// Set serverThreads = new HashSet();

	Informedia(Bungee _art, int _port) {
		super("InformediaSocketRequestListener");
		port = _port;
		art = _art;
		Util.print("Starting Informedia thread for port " + port);
	}

	public void run() {
		boolean listening = true;
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("Could not listen on port: " + port);
			return;
		}

		while (listening) {
			try {
				ServerThread serverThread = new ServerThread(serverSocket
						.accept());
				serverThread.start();
				// serverThreads.add(serverThread);
				// Util.print("adding socket");
			} catch (IOException e) {
				System.err.println("Accept client socket failed because\n" + e);
				return;
			}
		}
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// void playSegment(Item item) {
	// Util.print("playSegment " + item + serverThreads.size());
	// for (Iterator it = serverThreads.iterator(); it.hasNext();) {
	// ServerThread thread = (ServerThread) it.next();
	// thread.playSegment(item);
	// }
	// }
	//
	// void newVideoSet() {
	// Util.print("newVideoSet " + serverThreads.size());
	// for (Iterator it = serverThreads.iterator(); it.hasNext();) {
	// ServerThread thread = (ServerThread) it.next();
	// thread.newVideoSet();
	// }
	// }

	void playSegment(Item item) {
		Util.print("playSegment " + item);
		try {
			Socket socket = getSendSocket();
			PrintWriter clientOut = new PrintWriter(socket.getOutputStream(),
					true);
			String command = playArgs(item);
			Util.print("To Informedia server: " + command);
			clientOut.print(command);
			clientOut.close();
			socket.close();
			// } catch (UnknownHostException e) {
			// e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	String playArgs(Item item) {
		String command = "command=play&segment=";

		if (art.dbName.equalsIgnoreCase("historymakers")) {
			command += item.getId();
		} else if (art.dbName.equalsIgnoreCase("cm")) {
			try {
				Item[] items = { item };
				ResultSet rs = art.query.caremediaPlayArgs(items);
				assert MyResultSet.nRows(rs) == 1;
				rs.next();
				int segment = rs.getInt(1);
				int start = rs.getInt(2);
				int stop = rs.getInt(3);
				command += segment + "&start=" + start + "&end=" + stop;
				rs.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			assert false : art.dbName;
		}
		command += ";";
		return command;
	}

	int getSegment(Item item) {
		Item[] items = { item };
		return getSegments(items)[0];
	}

	int[] getSegments(Item[] items) {
		if (art.dbName.equalsIgnoreCase("historymakers")) {
			int[] result = new int[items.length];
			for (int i = 0; i < items.length; i++) {
				result[i++] = items[i].getId();
			}
			return result;
		} else if (art.dbName.equalsIgnoreCase("cm")) {
			try {
				ResultSet rs = art.query.caremediaPlayArgs(items);
				Set segments = new HashSet(MyResultSet.nRows(rs));
				while (rs.next()) {
					segments.add(new Integer(rs.getInt(1)));
				}
				rs.close();
				int[] result = new int[segments.size()];
				int i = 0;
				for (Iterator it = segments.iterator(); it.hasNext();) {
					Integer segment = (Integer) it.next();
					result[i++] = segment.intValue();
				}
				return result;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			assert false : art.dbName;
		}
		return null;
	}

	Item getItem(int segment) {
		int[] segments = { segment };
		Item[] items = getItems(segments);
		// Might not have any items for this segment
		return items.length > 0 ? items[0] : null;
	}

	Item[] getItems(int[] segments) {
		if (art.dbName.equalsIgnoreCase("historymakers")) {
			Item[] result = new Item[segments.length];
			for (int i = 0; i < segments.length; i++) {
				result[i] = art.ensureItem(segments[i]);
			}
			return result;
		} else if (art.dbName.equalsIgnoreCase("cm")) {
			try {
				ResultSet rs = art.query.caremediaGetItems(segments);
				Item[] result = new Item[MyResultSet.nRows(rs)];
				int i = 0;
				while (rs.next()) {
					result[i++] = art.ensureItem(rs.getInt(1));
				}
				rs.close();
				return result;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			assert false : art.dbName;
		}
		return null;
	}

	void newVideoSet() {
		Util.print("newVideoSet ");

		Map args = new TreeMap(); // Use TreeMap so "command" comes first
		args.put("command", "newVideoSet");
		args.put("name", art.query.getName());
		args.put("segments", art.getItems(0, art.query.getOnCount()));
		String outputLine = argify(args);
		Util.print("To Client: " + outputLine);

		try {
			Socket socket = getSendSocket();
			PrintWriter clientOut = new PrintWriter(socket.getOutputStream(),
					true);
			clientOut.print(outputLine);
			clientOut.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Socket getSendSocket() throws UnknownHostException, IOException {
		return new Socket("localhost", port + 1);
	}

	static String argify(Map map) {
		StringBuffer buf = new StringBuffer();
		for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			if (buf.length() > 0)
				buf.append("&");
			try {
				buf.append(entry.getKey()).append("=").append(
						URLEncoder.encode((String) entry.getValue(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return buf.toString();
	}

	private class ServerThread extends Thread {

		Socket clientSocket;
		PrintWriter clientOut;

		ServerThread(Socket _socket) {
			super("Informedia Server");
			clientSocket = _socket;
		}

		public void run() {
			try {
				clientOut = new PrintWriter(clientSocket.getOutputStream(),
						true);
				BufferedReader in = new BufferedReader(new InputStreamReader(
						clientSocket.getInputStream()));

				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					Util.print("From Client: " + inputLine);
					Map output = processInput(inputLine);
					if (output != null) {
						String outputLine = argify(output);
						Util.print("To Client: " + outputLine);
						clientOut.print(outputLine);
					}
				}
				clientOut.close();
				in.close();
				clientSocket.close();
				clientSocket = null;
				// serverThreads.remove(this);
				// Util.print("removing socket");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private Map processInput(String commandString) {
			URLQuery args = new URLQuery(commandString);
			String command = args.getArgument("command");
			if ("import".equals(command)) {
				return mport(args);
			} else if ("export".equals(command)) {
				return xport(args);
			} else {
				assert false : commandString;
				return null;
			}
		}

		// void newVideoSet() {
		// Map args = new Hashtable();
		// args.put("command", "newVideoSet");
		// args.put("name", art.query.getName());
		// args.put("segments", art.getFacetIDs(0, art.query.getOnCount()));
		//
		// String outputLine = argify(args);
		// Util.print("To Client: " + outputLine);
		// clientOut.println(outputLine);
		// }

		Map mport(URLQuery args) {
			int startIndex = 0;
			int endIndex = art.query.getOnCount();
			String start = args.getArgument("startIndex");
			if (start.length() > 0)
				startIndex = Integer.parseInt(start);
			String end = args.getArgument("endIndex");
			if (end.length() > 0)
				endIndex = Integer.parseInt(end);
			Item selectedItem = art.selectedItem();
			Util.print("import " + startIndex + "-" + endIndex + " "
					+ selectedItem);
			Map result = new Hashtable();
			result.put("name", art.query.getName());
			result.put("segments", getSegments(art.getItems(startIndex,
					endIndex)));
			if (selectedItem != null)
				result.put("selectedSegment", String
						.valueOf(getSegment(selectedItem)));
			return result;
		}

		Map xport(URLQuery args) {
			String nameArg = args.getArgument("name");
			final String name = nameArg.length() > 0 ? nameArg
					: "Informedia query";

			String selectedsegmentArg = args.getArgument("selectedSegment");
			final int selectedsegment = selectedsegmentArg.length() > 0 ? Integer
					.parseInt(selectedsegmentArg)
					: -1;

			String segmentsArg = args.getArgument("segments");
			final String segmentList = segmentsArg.length() > 0 ? segmentsArg
					: null;

			Util.print("export " + selectedsegment + " " + name);
			Util.print(" " + segmentList);

			Runnable doExport = new Runnable() {

				public void run() {
					art.clearQuery();
					if (segmentList != null) {
						String[] segmentNames = segmentList.split(",");
						int[] segments = new int[segmentNames.length];
						for (int i = 0; i < segmentNames.length; i++) {
							segments[i] = Integer.parseInt(segmentNames[i]);
						}
						art.addItemList(name, getItems(segments));
					}
					if (selectedsegment > 0) {
						Item selectedItem = getItem(selectedsegment);
						if (selectedItem != null)
							art.grid.clickThumb(selectedItem, 5);
					}
				}
			};

			javax.swing.SwingUtilities.invokeLater(doExport);
			return null;
		}

		// void playSegment(Item item) {
		// Util.print("playSegment " + clientSocket);
		// if (clientSocket != null) {
		// String command = "command=play&segment=" + item.getId();
		// Util.print("To Informedia server: " + command);
		// clientOut.println(command);
		// }
		// }
	}
}