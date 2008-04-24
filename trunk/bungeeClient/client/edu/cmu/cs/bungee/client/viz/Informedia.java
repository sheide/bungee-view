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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import edu.cmu.cs.bungee.client.query.Query.Item;
import edu.cmu.cs.bungee.javaExtensions.URLQuery;
import edu.cmu.cs.bungee.javaExtensions.Util;

// To seal jar (password is 8 digit insecure)

// cd C:\Program Files\Apache Software Foundation\Tomcat 6.0\webapps\bungee
// "C:\Program Files\Java\jdk1.5.0_06\bin\jarsigner" -keystore myKeys bungeeClient.jar BungeeView

public class Informedia extends Thread {

	int port;
	Bungee art;
	PrintWriter out;

	// Set serverThreads = new HashSet();

	Informedia(Bungee _art, int _port) {
		super("InformediaSocketRequestListener");
		port = _port;
		art = _art;
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
			String command = "command=play&segment=" + item.getId() + ";";
			Util.print("To Informedia server: " + command);
			clientOut.println(command);
			clientOut.close();
			socket.close();
			// } catch (UnknownHostException e) {
			// e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void newVideoSet() {
		Util.print("newVideoSet ");

		Map args = new TreeMap();  // Use TreeMap so "command" comes first
		args.put("command", "newVideoSet");
		args.put("name", art.query.getName());
		args.put("segments", art.getFacetIDs(0, art.query.getOnCount()));
		String outputLine = argify(args);
		Util.print("To Client: " + outputLine);

		try {
			Socket socket = getSendSocket();
			PrintWriter clientOut = new PrintWriter(socket.getOutputStream(),
					true);
			clientOut.println(outputLine);
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
						clientOut.println(outputLine);
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
			result.put("segments", art.getFacetIDs(startIndex, endIndex));
			if (selectedItem != null)
				result.put("selectedSegment", String.valueOf(selectedItem
						.getId()));
			return result;
		}

		Map xport(URLQuery args) {
			String nameArg = args.getArgument("name");
			final String name = nameArg.length() > 0 ? nameArg
					: "Informedia query";

			String selectedItemArg = args.getArgument("selectedSegment");
			final int selectedItem = selectedItemArg.length() > 0 ? Integer
					.parseInt(selectedItemArg) : -1;

			String itemsArg = args.getArgument("segments");
			final String items = itemsArg.length() > 0 ? itemsArg : null;

			Util.print("export " + selectedItem + " " + name);
			Util.print(" " + items);

			Runnable doExport = new Runnable() {

				public void run() {
					art.clearQuery();
					if (items != null)
						art.addItemList(name, items);
					if (selectedItem > 0) {
						art.grid.clickThumb(art.ensureItem(selectedItem), 5);
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