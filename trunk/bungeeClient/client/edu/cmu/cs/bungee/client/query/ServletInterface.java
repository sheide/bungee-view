package edu.cmu.cs.bungee.client.query;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.zip.InflaterInputStream;

import javax.swing.SwingUtilities;

import edu.cmu.cs.bungee.javaExtensions.*;


class ServletInterface implements Serializable {

	private static final long serialVersionUID = -1377682793600916283L;

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

	private ItemInfo itemInfo;

	private class DescAndImage {
		DescAndImage(int _item, ResultSet _info) {
			item = _item;
			info = _info;
		}

		final int item;

		final ResultSet info;
	}

	private class ItemInfo {
		final int item;

		final int minIndex;

		final int maxIndex;

		final ResultSet itemOffsets;

		final int itemIndex;

		ItemInfo(int _item, int _itemIndex, int _minIndex, int _maxIndex,
				ResultSet _itemOffsets) {
			item = _item;

			minIndex = _minIndex;

			maxIndex = _maxIndex;

			itemOffsets = _itemOffsets;

			itemIndex = _itemIndex;
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

	public void close() {
		doCommand("CLOSE");
		// sessionID = null;
	}

	// public boolean isConnected() {
	// return sessionID != null;
	// }

	public String errorMessage() {
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
			if (sessionID != null)
				s.append("&session=").append(sessionID);
			String url = s.toString();
			if (printOps) {
				System.out.println(url);
				if (SwingUtilities.isEventDispatchThread()) {
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

	// public int SQLupdate(String command, String[] args) {
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

	public ResultSet getResultSet(String command, String[] args,
			List columnTypes) {
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

	// public int getInt(String command) {
	// return getInt(command, (String[]) null);
	// }
	//
	// public int getInt(String command, int arg1) {
	// String[] args = { Integer.toString(arg1) };
	// return getInt(command, args);
	// }
	//
	// public int getInt(String command, int arg1, boolean arg2) {
	// String[] args = { Integer.toString(arg1), Boolean.toString(arg2) };
	// return getInt(command, args);
	// }
	//
	// public int getInt(String command, String arg1) {
	// String[] args = { arg1 };
	// return getInt(command, args);
	// }

	public String getString(String command) {
		return getString(command, null);
	}

	public String getString(String command, int arg) {
		String[] args = { Integer.toString(arg) };
		return getString(command, args);
	}

	// alphabetical order
	//
	// INT
	// public ResultSet getResultSet(String command, List columnTypes, int arg)
	// {
	// String[] args = { Integer.toString(arg) };
	// return getResultSet(command, args, columnTypes);
	// }

	// INT, BOOLEAN
	public ResultSet getResultSet(String command, List columnTypes, int arg1,
			boolean arg2) {
		String[] args = { Integer.toString(arg1), Boolean.toString(arg2) };
		return getResultSet(command, args, columnTypes);
	}

	// INT, BOOLEAN, BOOLEAN
	public ResultSet getResultSet(String command, List columnTypes, int arg1,
			boolean arg2, boolean arg3) {
		String[] args = { Integer.toString(arg1), Boolean.toString(arg2),
				Boolean.toString(arg3) };
		return getResultSet(command, args, columnTypes);
	}

	// INT
	public ResultSet getResultSet(String command, List columnTypes, int arg1) {
		String[] args = { Integer.toString(arg1) };
		return getResultSet(command, args, columnTypes);
	}

	// INT, INT
	public ResultSet getResultSet(String command, List columnTypes, int arg1,
			int arg2) {
		String[] args = { Integer.toString(arg1), Integer.toString(arg2) };
		return getResultSet(command, args, columnTypes);
	}

	// INT, INT, INT
	public ResultSet getResultSet(String command, List columnTypes, int arg1,
			int arg2, int arg3) {
		String[] args = { Integer.toString(arg1), Integer.toString(arg2),
				Integer.toString(arg3) };
		return getResultSet(command, args, columnTypes);
	}

	// STRING
	// public ResultSet getResultSet(String command, List columnTypes, String
	// arg1) {
	// String[] args = { arg1 };
	// return getResultSet(command, args, columnTypes);
	// }

	// STRING, INT
	public ResultSet getResultSet(String command, List columnTypes,
			String arg1, int arg2) {
		String[] args = { arg1, Integer.toString(arg2) };
		return getResultSet(command, args, columnTypes);
	}

	// STRING, INT, INT, INT
	public ResultSet getResultSet(String command, List columnTypes,
			String arg1, int arg2, int arg3, int arg4) {
		String[] args = { arg1, Integer.toString(arg2), Integer.toString(arg3),
				Integer.toString(arg4) };
		return getResultSet(command, args, columnTypes);
	}

	// STRING, STRING
	public ResultSet getResultSet(String command, List columnTypes,
			String arg1, String arg2) {
		String[] args = { arg1, arg2 };
		return getResultSet(command, args, columnTypes);
	}

	public String aboutCollection() {
		return getString("ABOUT_COLLECTION");
	}

	// public int itemCount() {
	// // "SELECT COUNT(*) FROM item"
	// return item_count;
	// }

	public String getItemURL(int item) {
		return getString("ITEM_URL", item);
	}

	public ResultSet getCountsIgnoringFacet(String subQuery, int facetID) {
		return getResultSet("getCountsIgnoringFacet", MyResultSet.SINT_PINT,
				subQuery, facetID);
	}

	public ResultSet getFilteredCounts(String perspectivesToAdd,
			String perspectivesToRemove) {
		return getResultSet("getFilteredCounts", MyResultSet.SINT_PINT,
				perspectivesToAdd, perspectivesToRemove);
	}

	public ResultSet getFilteredCountTypes() {
		return getResultSet("getFilteredCountTypes", null,
				MyResultSet.SINT_PINT);
	}

	public ResultSet initPerspectives() {
		return initPerspectives;
	}

	void decacheOffsets() {
		itemInfo = null;
	}

	public int updateOnItems(String subQuery, int item, int table,
			int nNeighbors) {
		decacheOffsets();
		assert (subQuery == null) == (table == 1) : subQuery;
		String[] args = { subQuery, Integer.toString(item),
				Integer.toString(table), Integer.toString(nNeighbors) };
		DataInputStream in = getStream("updateOnItems", args);
		int result = MyResultSet.readInt(in);
		if (result > 0 && nNeighbors > 1) {
			itemIndexInternal(in, item, nNeighbors);
		}
		closeNcatch(in, "updateOnItems");
		return result;
	}

	// Field order count, nChildren, childrenOffset, name
	public ResultSet prefetch(int facetID, int args) {
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

	public ResultSet init() {
		return init;
	}

	public ResultSet offsetItems(int minOffset, int maxOffset, int table) {
		if (itemInfo != null && minOffset >= itemInfo.minIndex
				&& maxOffset <= itemInfo.maxIndex) {
			try {
				itemInfo.itemOffsets.absolute(minOffset - itemInfo.minIndex);
			} catch (SQLException e) {
				Util.err("Caching is messed up: " + minOffset + "-" + maxOffset
						+ " " + itemInfo);
				e.printStackTrace();
			}
			return itemInfo.itemOffsets;
		}
		ResultSet result = getResultSet("offsetItems", MyResultSet.INT,
				minOffset, maxOffset, table);
		return result;
	}

	public ResultSet getThumbs(String items, int imageW, int imageH, int quality) {
		// long start = new Date().getTime();
		ResultSet result = getResultSet("getThumbs",
				MyResultSet.SINT_IMAGE_INT_INT, items, imageW, imageH, quality);
		// System.out.println("getThumbs took " + (new Date().getTime() - start)
		// + " ms");
		return result;
	}

	// public ResultSet getThumbSizes(int facet) {
	// return getResultSet("getThumbSizes", MyResultSet.SINT_INT_INT, facet);
	// }

	public ResultSet getDescAndImage(int item, int imageW, int imageH,
			int quality) {
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

	public int itemIndex(int item, int table, int nNeighbors) {
		if (itemInfo.item == item && itemInfo.itemIndex >= 0) {
			if (printOps)
				Util.print("Using cached itemIndex for " + item + ", "
						+ itemInfo.itemIndex);
			return itemInfo.itemIndex;
		}
		String[] args = { Integer.toString(item), Integer.toString(table),
				Integer.toString(nNeighbors) };
		DataInputStream in = getStream("itemIndex", args);
		int result = itemIndexInternal(in, item, nNeighbors);
		closeNcatch(in, "itemIndex");
		return result;
	}

	public int[] itemIndexFromURL(String URL, int table, int nNeighbors) {
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
		int maxIndex = -1;
		ResultSet itemOffsets = null;
		int itemIndex = MyResultSet.readInt(in) - 1;
		if (nNeighbors > 1) {
			minIndex = MyResultSet.readInt(in);
			maxIndex = MyResultSet.readInt(in);
			itemOffsets = new MyResultSet(in, MyResultSet.INT);
			// System.out.println("itemIndexInternal " + result + " " + minIndex
			// + " " +
			// maxIndex);
		} 
			itemInfo = new ItemInfo(item, itemIndex, minIndex, maxIndex, itemOffsets);
		return itemIndex;
	}

	public String[][] getDatabases() {
		String[] s = Util.splitSemicolon(databaseDesc);
		String[][] databases = new String[s.length][];
		for (int i = 0; i < s.length; i++) {
			databases[i] = Util.splitComma(s[i]);
			// System.out.println(s[i]);
		}
		return databases;
	}

	public void printUserAction(int location, String object, int modifiers) {
		String[] args = { Integer.toString(location), object,
				Integer.toString(modifiers) };
		dontGetStream("printUserAction", args);
	}

	public void reorderItems(int facetID) {
		String[] args = { Integer.toString(facetID) };
		dontGetStream("reorderItems", args);
		decacheOffsets();
	}

	public void restrict() {
		dontGetStream("restrict", null);
	}

	public ResultSet[] cluster(int maxClusters, int maxClusterSize,
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

	public ResultSet addItemFacet(int facet, int item) {
		String[] args = { Integer.toString(facet), Integer.toString(item) };
		ResultSet result = getResultSet("addItemFacet", args,
				MyResultSet.SINT_INT_INT_INT_INT);
		return result;
	}

	public ResultSet addItemsFacet(int facet) {
		String[] args = { Integer.toString(facet) };
		ResultSet result = getResultSet("addItemsFacet", args,
				MyResultSet.SINT_INT_INT_INT_INT);
		return result;
	}

	public ResultSet removeItemsFacet(int facet) {
		String[] args = { Integer.toString(facet) };
		ResultSet result = getResultSet("removeItemsFacet", args,
				MyResultSet.SINT_INT_INT_INT_INT);
		return result;
	}

	public ResultSet addChildFacet(int facet, String name) {
		String[] args = { Integer.toString(facet), name };
		ResultSet result = getResultSet("addChildFacet", args,
				MyResultSet.SINT_INT_INT_INT_INT);
		return result;
	}

	public ResultSet removeItemFacet(int facet, int item) {
		String[] args = { Integer.toString(facet), Integer.toString(item) };
		ResultSet result = getResultSet("removeItemFacet", args,
				MyResultSet.SINT_INT_INT_INT_INT);
		return result;
	}

	public ResultSet reparent(int parent, int child) {
		String[] args = { Integer.toString(parent), Integer.toString(child) };
		ResultSet result = getResultSet("reparent", args,
				MyResultSet.SINT_INT_INT_INT_INT);
		return result;
	}

	public void writeback() {
		String[] args = {};
		dontGetStream("writeback", args);
	}

	public void rotate(int item, String theta) {
		String[] args = { Integer.toString(item), theta };
		dontGetStream("rotate", args);
	}

	public void rename(int facetID, String newName) {
		String[] args = { Integer.toString(facetID), newName };
		dontGetStream("rename", args);
	}

	public ResultSet getNames(String facets) {
		String[] args = { facets };
		ResultSet result = getResultSet("getNames", args, MyResultSet.STRING);
		return result;
	}

	public void setItemDescription(int currentItem, String description) {
		String[] args = { Integer.toString(currentItem), description };
		dontGetStream("setItemDescription", args);
	}

	public String[] opsSpec(String replay) {
		String[] args = { replay };
		MyResultSet result = (MyResultSet) getResultSet("opsSpec", args,
				MyResultSet.STRING);
		return (String[]) result.getValues(1);
	}

	public String getSession() {
		return sessionID;
	}

}
