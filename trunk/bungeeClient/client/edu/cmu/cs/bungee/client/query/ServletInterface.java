package edu.cmu.cs.bungee.client.query;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.zip.InflaterInputStream;

import javax.swing.SwingUtilities;

import edu.cmu.cs.bungee.javaExtensions.MyResultSet;
import edu.cmu.cs.bungee.javaExtensions.Util;

final class ServletInterface {

	private final static boolean printOps = false;

	private final String host;

	private final String sessionID;

	private final String databaseDesc;

	final int facetCount;

	final int itemCount;

	final String itemDescriptionFields;

	final String label;

	final String doc;

	private final MyResultSet initPerspectives;

	private final MyResultSet init;

	private String status;

	private DescAndImage descAndImage;

	/**
	 * This caches answers for two functions: itemIndex: what is the itemOffset
	 * of item? offsetItems: what are the items for a range of offsets?
	 * 
	 * The answers are cached when calling itemIndex, itemIndexFromURL, and
	 * updateOnItems.
	 * 
	 */
	private ItemInfo itemInfo;

	private final class DescAndImage {
		DescAndImage(int _item, ResultSet _info) {
			item = _item;
			info = _info;
		}

		final int item;

		final ResultSet info;
	}

	private final class ItemInfo {
		final int item;

		final int minIndex;

		final ResultSet itemOffsets;

		final int itemIndex;

		ItemInfo(int _item, int _itemIndex, int _minIndex,
				ResultSet _itemOffsets) {
			item = _item;

			minIndex = _minIndex;

			itemOffsets = _itemOffsets;

			itemIndex = _itemIndex;
		}

		int maxIndex() {
			int _maxIndex = minIndex;
			if (itemOffsets != null) {
				try {
					_maxIndex += MyResultSet.nRows(itemOffsets);
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
			return _maxIndex;
		}

		public String toString() {
			return "onItems[" + itemIndex + "] = " + item + "; range "
					+ minIndex + "-" + maxIndex();
		}
	}

	ServletInterface(String codeBase, String dbName) {
		System.out.println(codeBase + " " + dbName);
		host = codeBase;
		String[] args = { dbName };
		if (dbName == null || dbName.length() == 0)
			args = null;

		DataInputStream in = getStream("CONNECT", args);
		if (in != null) {
			sessionID = MyResultSet.readString(in);
			// Util.print("session = '" + sessionID + "'");

			databaseDesc = MyResultSet.readString(in);

			facetCount = MyResultSet.readInt(in);
			itemCount = MyResultSet.readInt(in);
			itemDescriptionFields = MyResultSet.readString(in);
			label = MyResultSet.readString(in);
			doc = MyResultSet.readString(in);

			initPerspectives = new MyResultSet(in,
					MyResultSet.STRING_STRING_STRING_INT_INT_INT_INT);
			init = new MyResultSet(in, MyResultSet.INT);

			closeNcatch(in, "CONNECT");
		} else {
			databaseDesc = null;
			sessionID = null;
			initPerspectives = null;
			init = null;
			itemDescriptionFields = null;
			label = null;
			doc = null;
			facetCount = -1;
			itemCount = -1;
		}
	}

	void close() {
		doCommand("CLOSE");
		// sessionID = null;
	}

	// boolean isConnected() {
	// return sessionID != null;
	// }

	String errorMessage() {
		return status;
	}

	void dontGetStream(String command, String[] args) {
		closeNcatch(getStream(command, args), command);
	}

	DataInputStream getStream(String command, String[] args) {
		DataInputStream in = null;
		HttpURLConnection conn = null;
		status = null;
		try {
			StringBuffer s = new StringBuffer();
			s.append("?command=").append(command);
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					if (args[i] != null) {
						String arg = URLEncoder.encode(args[i], "UTF-8");
						s.append("&arg").append(i + 1).append("=").append(arg);
					}
				}
			}
			String actionString = flushUserActions();
			if (actionString != null)
				s.append("&userActions=").append(
						URLEncoder.encode(actionString, "UTF-8"));
			if (sessionID != null)
				s.append("&session=").append(sessionID);
			String url = s.toString();
			if (printOps) {
				System.out.println(url);
				if (SwingUtilities.isEventDispatchThread()) {
					// Note that during replay, most everything is called in the mouse process.
					System.err
							.println("Calling ServletInterface in event dispatch thread! "
									+ url);
					// new Throwable().printStackTrace();
				}
			}
			conn = (HttpURLConnection) (new URL(host + url)).openConnection();

			// These require Java 1.5
			// conn.setConnectTimeout(10000);
			// conn.setReadTimeout(10000);

			conn.setUseCaches(false);
			conn.setRequestMethod("POST");
			// if (!command.equals("printUserAction"))
			in = new DataInputStream(new InflaterInputStream(
					new BufferedInputStream(conn.getInputStream())));
			//           
			// BufferedWriter out =
			// new BufferedWriter( new OutputStreamWriter(
			// conn.getOutputStream() ) );
			// out.write("username=javaworld\r\n");
			// out.flush();
			// out.close();
		} catch (Throwable e) {
			if (conn != null)
				try {
					status = conn.getResponseMessage();
				} catch (IOException nested) {
					nested.printStackTrace();
				}
			if (status == null)
				status = e.toString();
		}

		if (status == null && conn != null) {
			try {
				status = conn.getResponseMessage();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (status != null && !status.equals("OK"))
			System.err.println("getStream status: " + status);

		return in;
	}

	// int SQLupdate(String command, String[] args) {
	// return getInt(command, args);
	// }

	void doCommand(String command) {
		doCommand(command, null);
	}

	void doCommand(String command, String[] args) {
		try {
			DataInputStream s = getStream(command, args);
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	String getString(String command, String[] args) {
		DataInputStream in = getStream(command, args);
		String result = MyResultSet.readString(in);
		closeNcatch(in, command);
		return result;
	}

	// int getInt(String command, String[] args) {
	// DataInputStream in = getStream(command, args);
	// int result = MyResultSet.readInt(in);
	// closeNcatch(in, command);
	// return result;
	// }

	ResultSet getResultSet(String command, String[] args, List columnTypes) {
		DataInputStream in = getStream(command, args);
		ResultSet result = new MyResultSet(in, columnTypes);
		closeNcatch(in, command);
		return result;
	}

	void closeNcatch(DataInputStream s, String command) {
		if (printOps)
			System.out.println("...done " + command);
		try {
			while (s.read() != -1) {
				// Must read to the end so s can be closed
			}
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// int getInt(String command) {
	// return getInt(command, (String[]) null);
	// }
	//
	// int getInt(String command, int arg1) {
	// String[] args = { Integer.toString(arg1) };
	// return getInt(command, args);
	// }
	//
	// int getInt(String command, int arg1, boolean arg2) {
	// String[] args = { Integer.toString(arg1), Boolean.toString(arg2) };
	// return getInt(command, args);
	// }
	//
	// int getInt(String command, String arg1) {
	// String[] args = { arg1 };
	// return getInt(command, args);
	// }

	String getString(String command) {
		return getString(command, null);
	}

	String getString(String command, int arg) {
		String[] args = { Integer.toString(arg) };
		return getString(command, args);
	}

	// alphabetical order
	//
	// INT
	// ResultSet getResultSet(String command, List columnTypes, int arg)
	// {
	// String[] args = { Integer.toString(arg) };
	// return getResultSet(command, args, columnTypes);
	// }

	// INT, BOOLEAN
	ResultSet getResultSet(String command, List columnTypes, int arg1,
			boolean arg2) {
		String[] args = { Integer.toString(arg1), Boolean.toString(arg2) };
		return getResultSet(command, args, columnTypes);
	}

	// INT, BOOLEAN, BOOLEAN
	ResultSet getResultSet(String command, List columnTypes, int arg1,
			boolean arg2, boolean arg3) {
		String[] args = { Integer.toString(arg1), Boolean.toString(arg2),
				Boolean.toString(arg3) };
		return getResultSet(command, args, columnTypes);
	}

	// INT
	ResultSet getResultSet(String command, List columnTypes, int arg1) {
		String[] args = { Integer.toString(arg1) };
		return getResultSet(command, args, columnTypes);
	}

	// INT, INT
	ResultSet getResultSet(String command, List columnTypes, int arg1, int arg2) {
		String[] args = { Integer.toString(arg1), Integer.toString(arg2) };
		return getResultSet(command, args, columnTypes);
	}

	// INT, INT, INT
	ResultSet getResultSet(String command, List columnTypes, int arg1,
			int arg2, int arg3) {
		String[] args = { Integer.toString(arg1), Integer.toString(arg2),
				Integer.toString(arg3) };
		return getResultSet(command, args, columnTypes);
	}

	// STRING
	// ResultSet getResultSet(String command, List columnTypes, String
	// arg1) {
	// String[] args = { arg1 };
	// return getResultSet(command, args, columnTypes);
	// }

	// STRING, INT
	ResultSet getResultSet(String command, List columnTypes, String arg1,
			int arg2) {
		String[] args = { arg1, Integer.toString(arg2) };
		return getResultSet(command, args, columnTypes);
	}

	// STRING, INT, INT, INT
	ResultSet getResultSet(String command, List columnTypes, String arg1,
			int arg2, int arg3, int arg4) {
		String[] args = { arg1, Integer.toString(arg2), Integer.toString(arg3),
				Integer.toString(arg4) };
		return getResultSet(command, args, columnTypes);
	}

	// STRING, STRING
	ResultSet getResultSet(String command, List columnTypes, String arg1,
			String arg2) {
		String[] args = { arg1, arg2 };
		return getResultSet(command, args, columnTypes);
	}

	String aboutCollection() {
		return getString("ABOUT_COLLECTION");
	}

	// int itemCount() {
	// // "SELECT COUNT(*) FROM item"
	// return item_count;
	// }

	String getItemURL(int item) {
		return getString("ITEM_URL", item);
	}

	ResultSet getCountsIgnoringFacet(String subQuery, int facetID) {
		return getResultSet("getCountsIgnoringFacet", MyResultSet.SINT_PINT,
				subQuery, facetID);
	}

	ResultSet getFilteredCounts(String perspectivesToAdd,
			String perspectivesToRemove) {
		return getResultSet("getFilteredCounts", MyResultSet.SINT_PINT,
				perspectivesToAdd, perspectivesToRemove);
	}

	ResultSet getFilteredCountTypes() {
		return getResultSet("getFilteredCountTypes", null,
				MyResultSet.SINT_PINT);
	}

	ResultSet initPerspectives() {
		return initPerspectives;
	}

	void decacheOffsets() {
		itemInfo = null;
	}

	int updateOnItems(String subQuery, int item, int table, int nNeighbors) {
		decacheOffsets();
		if (subQuery != null) {
			String[] args = { subQuery, Integer.toString(item),
					Integer.toString(table), Integer.toString(nNeighbors) };
			DataInputStream in = getStream("updateOnItems", args);
			int onCount = MyResultSet.readInt(in);
			if (onCount > 0 && nNeighbors > 1) {
				itemIndexInternal(in, item, nNeighbors);
			}
			closeNcatch(in, "updateOnItems");
			return onCount;
		} else {
			return -1;
		}
	}

	// Field order count, nChildren, childrenOffset, name
	ResultSet prefetch(int facetID, int args) {
		List types = null;
		switch (args) {
		case 1:
		case 5:
			types = MyResultSet.INT_INT_INT_STRING;
			break;
		case 2:
		case 6:
			types = MyResultSet.INT_INT_INT;
			break;
		case 3:
			types = MyResultSet.INT_INT_STRING;
			break;
		case 4:
			types = MyResultSet.INT_INT;
			break;
		default:
			assert false : "prefetch args=" + args;
		}
		return getResultSet("prefetch", types, facetID, args);
	}

	ResultSet init() {
		return init;
	}

	ResultSet offsetItems(int minOffset, int maxOffset, int table) {
		if (itemInfo != null && minOffset >= itemInfo.minIndex
				&& maxOffset <= itemInfo.maxIndex()) {
			try {
				itemInfo.itemOffsets.absolute(minOffset - itemInfo.minIndex);
			} catch (SQLException e) {
				Util.err("Caching is messed up: " + itemInfo);
				e.printStackTrace();
			}
			if (printOps)
				Util.print("Using cached rs for offsetItems. " + itemInfo);
			return itemInfo.itemOffsets;
		}
		ResultSet result = getResultSet("offsetItems", MyResultSet.INT,
				minOffset, maxOffset, table);
		return result;
	}

	ResultSet getThumbs(String items, int imageW, int imageH, int quality) {
		// long start = new Date().getTime();
		ResultSet result = getResultSet("getThumbs",
				MyResultSet.SINT_IMAGE_INT_INT, items, imageW, imageH, quality);
		// System.out.println("getThumbs took " + (new Date().getTime() - start)
		// + " ms");
		return result;
	}

	// ResultSet getThumbSizes(int facet) {
	// return getResultSet("getThumbSizes", MyResultSet.SINT_INT_INT, facet);
	// }

	ResultSet getDescAndImage(int item, int imageW, int imageH, int quality) {
		String[] args = { Integer.toString(item), Integer.toString(imageW),
				Integer.toString(imageH), Integer.toString(quality) };
		DataInputStream in = getStream("getDescAndImage", args);
		descAndImage = new DescAndImage(item, new MyResultSet(in,
				MyResultSet.PINT_SINT_STRING_INT_INT_INT));
		ResultSet result = new MyResultSet(in, MyResultSet.STRING_IMAGE_INT_INT);

		closeNcatch(in, "getDescAndImage");

		return result;
	}

	// Always called right after getDescAndImage
	ResultSet getItemInfo(int item) {
		assert item == descAndImage.item;
		return descAndImage.info;
	}

	int itemIndex(int item, int table, int nNeighbors) {
		if (itemInfo != null && itemInfo.item == item
		// && itemInfo.itemIndex >= 0
		) {
			if (printOps)
				Util.print("Using cached itemIndex for " + item + ", "
						+ itemInfo.itemIndex);
			return itemInfo.itemIndex;
		} else if (printOps) {
			Util.print("NOT using cached itemIndex for " + item + ", "
					+ itemInfo);
		}
		String[] args = { Integer.toString(item), Integer.toString(table),
				Integer.toString(nNeighbors) };
		DataInputStream in = getStream("itemIndex", args);
		int result = itemIndexInternal(in, item, nNeighbors);
		closeNcatch(in, "itemIndex");
		return result;
	}

	int[] itemIndexFromURL(String URL, int table) {
		int nNeighbors = 0;
		String[] args = { URL, Integer.toString(table),
				Integer.toString(nNeighbors) };
		DataInputStream in = getStream("itemIndexFromURL", args);
		int[] result = new int[2];
		int item = MyResultSet.readInt(in);
		result[0] = item;
		result[1] = itemIndexInternal(in, item, nNeighbors);
		closeNcatch(in, "itemIndexFromURL");
		return result;
	}

	int itemIndexInternal(DataInputStream in, int item, int nNeighbors) {
		int minIndex = -1;
		// int maxIndex = -1;
		ResultSet itemOffsets = null;
		int itemIndex = MyResultSet.readInt(in) - 1;
		// Util.print("itemIndexInternal " + itemIndex);

		// nNeighbors > 1 except when called from Bungee.clickThumb on startup.
		if (nNeighbors > 1) {
			minIndex = MyResultSet.readInt(in);
			// maxIndex = MyResultSet.readInt(in);
			itemOffsets = new MyResultSet(in, MyResultSet.INT);
			// System.out.println("itemIndexInternal " + result + " " + minIndex
			// + " " +
			// maxIndex);
		}
		itemInfo = new ItemInfo(item, itemIndex, minIndex, itemOffsets);
		return itemIndex;
	}

	String[][] getDatabases() {
		String[] s = Util.splitSemicolon(databaseDesc);
		String[][] databases = new String[s.length][];
		for (int i = 0; i < s.length; i++) {
			databases[i] = Util.splitComma(s[i]);
			// System.out.println(s[i]);
		}
		return databases;
	}

	StringBuffer processedQueue = new StringBuffer();

	void printUserAction(String[] x) {
		assert x.length == 4 : Util.valueOfDeep(x);
		if (processedQueue.length() > 0)
			processedQueue.append(";");
		processedQueue.append(x[0]).append(",");
		processedQueue.append(x[1]).append(",");
		processedQueue.append(x[2]).append(",");
		processedQueue.append(x[3]);
	}

	private String flushUserActions() {
		String result = null;
		if (processedQueue.length() > 0) {
			result = processedQueue.toString();
			processedQueue = new StringBuffer();
		}
		return result;
	}

	void reorderItems(int facetID) {
		String[] args = { Integer.toString(facetID) };
		dontGetStream("reorderItems", args);
		decacheOffsets();
	}

	void restrict() {
		dontGetStream("restrict", null);
	}

	ResultSet[] cluster(int maxClusters, int maxClusterSize,
			String facetRestriction, double p) {
		String[] args = { Integer.toString(maxClusters),
				Integer.toString(maxClusterSize), facetRestriction,
				Double.toString(p) };
		DataInputStream in = getStream("cluster", args);
		int nClusters = MyResultSet.readInt(in);
		// Util.print("# clusters = " + nClusters);
		ResultSet[] result = new ResultSet[nClusters];
		for (int i = 0; i < nClusters; i++)
			result[i] = new MyResultSet(
					in,
					MyResultSet.INT_PINT_STRING_INT_INT_INT_INT_DOUBLE_PINT_PINT);
		closeNcatch(in, "cluster");
		return result;
	}

	ResultSet addItemFacet(int facet, int item) {
		String[] args = { Integer.toString(facet), Integer.toString(item) };
		ResultSet result = getResultSet("addItemFacet", args,
				MyResultSet.SINT_INT_INT_INT_INT);
		return result;
	}

	ResultSet addItemsFacet(int facet) {
		String[] args = { Integer.toString(facet) };
		ResultSet result = getResultSet("addItemsFacet", args,
				MyResultSet.SINT_INT_INT_INT_INT);
		return result;
	}

	ResultSet removeItemsFacet(int facet) {
		String[] args = { Integer.toString(facet) };
		ResultSet result = getResultSet("removeItemsFacet", args,
				MyResultSet.SINT_INT_INT_INT_INT);
		return result;
	}

	ResultSet addChildFacet(int facet, String name) {
		String[] args = { Integer.toString(facet), name };
		ResultSet result = getResultSet("addChildFacet", args,
				MyResultSet.SINT_INT_INT_INT_INT);
		return result;
	}

	ResultSet removeItemFacet(int facet, int item) {
		String[] args = { Integer.toString(facet), Integer.toString(item) };
		ResultSet result = getResultSet("removeItemFacet", args,
				MyResultSet.SINT_INT_INT_INT_INT);
		return result;
	}

	ResultSet reparent(int parent, int child) {
		String[] args = { Integer.toString(parent), Integer.toString(child) };
		ResultSet result = getResultSet("reparent", args,
				MyResultSet.SINT_INT_INT_INT_INT);
		return result;
	}

	void writeback() {
		String[] args = {};
		dontGetStream("writeback", args);
	}

	void rotate(int item, String theta) {
		String[] args = { Integer.toString(item), theta };
		dontGetStream("rotate", args);
	}

	void rename(int facetID, String newName) {
		String[] args = { Integer.toString(facetID), newName };
		dontGetStream("rename", args);
	}

	ResultSet getNames(String facets) {
		String[] args = { facets };
		ResultSet result = getResultSet("getNames", args, MyResultSet.STRING);
		return result;
	}

	void setItemDescription(int currentItem, String description) {
		String[] args = { Integer.toString(currentItem), description };
		dontGetStream("setItemDescription", args);
	}

	String[] opsSpec(String replay) {
		String[] args = { replay };
		MyResultSet result = (MyResultSet) getResultSet("opsSpec", args,
				MyResultSet.STRING);
		return (String[]) result.getValues(1);
	}

	String getSession() {
		return sessionID;
	}

}
