package edu.cmu.cs.bungee.javaExtensions;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.swing.SwingUtilities;

public class JDBCSample {

	static Map statements = new HashMap();

	/**
	 * Print queries that take longer than this. Print all queries and extra
	 * info if slowQueryTime = 0 Never print if slowQueryTime < 0.
	 */
	private final static int slowQueryTime = -1;

	/**
	 * Only used by showSlow.
	 */
	private int nQueriesInProgress = 0;

	private static Object driver; // We don't need the driver;

	// this just ensures we only do it once.

	private Connection con;

	private String connectString;

	private GenericServlet servlet; // Used to write to log file. Call print

	private String dbName;

	// instead of using this directly.

	public JDBCSample(String server, String db, String user, String pass)
			throws SQLException, InstantiationException,
			IllegalAccessException, ClassNotFoundException {
		this(server, db, user, pass, null);
	}

	public JDBCSample(String server, String db, String user, String pass,
			GenericServlet _servlet) throws SQLException,
			InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		print("Connect to " + server + db + " at " + new Date());
		servlet = _servlet;
		dbName = db;
		String compression = server.indexOf("localhost") >= 0 ? "&useCompression=false"
				: "&useCompression=true";
		// You get an error trying to use compression for localhost.
		// Plus it's not important in that case.
		// String speedUpResultSetParsing =
		// "&useUnicode=false&characterEncoding=US-ASCII&characterSetResults=US-ASCII";
		// String cache = "&cacheResultSetMetadata=true&cachePrepStmts=true";
		connectString = server + db + "?user=" + user;
		if (pass != null)
			connectString += "&password=" + pass;

		connectString += compression + "&useUnicode=true"
				+ "&characterEncoding=UTF-8" + "&characterSetResults=UTF-8"
				+ "&connectionCollation=utf8_general_ci";
		if (driver == null)
			driver = Class.forName("com.mysql.jdbc.Driver").newInstance();
		con = DriverManager.getConnection(connectString);
	}

	public void close() throws SQLException {
		if (slowQueryTime >= 0) {
			print(statements.size() + " SQL statements were not closed:");
			// if (statements.size() > 0) {
			for (Iterator it = statements.values().iterator(); it.hasNext();) {
				String SQL = (String) it.next();
				print(SQL);
			}
			statements.clear();
		}
		// }
		if (con != null) {
			// try {
			con.close();
			con = null;
			// } catch (SQLException se) {
			// System.err.println("SQL Exception: " + se.getMessage());
			// se.printStackTrace();
			// }
		}
	}

	public static String quote(String s) {
		s = s.replaceAll("\\\\", "\\\\\\\\");
		s = s.replaceAll("'", "\\\\'");
		// Util.print(s);
		return "'" + s + "'";
	}

	public void print(String s) {
		if (servlet != null)
			servlet.log(s);
		else
			System.out.println(s);
	}

	private Date initSlow(String[] desc) {
		String concat = slowQueryTime == 0 ? Util.join(desc, "; ") : null;
		return initSlow(concat);
	}

	private Date initSlow(String desc) {
		Date start = null;
		if (slowQueryTime >= 0) {
			if (SwingUtilities.isEventDispatchThread())
				print("Calling DB in EventDispatchThread: " + desc);
			if (slowQueryTime == 0) {
				StringBuffer buf = new StringBuffer(desc.length() + 60);
				if (nQueriesInProgress > 0)
					buf.append("\n** ").append(nQueriesInProgress).append(
							" in progress **  ");
				buf.append("\n           starting query: ").append(desc);
				print(buf.toString());
			}
			start = new Date();
			nQueriesInProgress++;
		}
		return start;
	}

	private void showSlow(Date start, int[] nRows, String[] desc) {
		if (slowQueryTime >= 0) {
			nQueriesInProgress--;
			long delay = new Date().getTime() - start.getTime();
			if (delay >= slowQueryTime)
				showSlowInternal(delay, Util.join(nRows), Util.join(desc, "; "));
		}
	}

	private void showSlow(Date start, int nRows, String desc) {
		if (slowQueryTime >= 0) {
			nQueriesInProgress--;
			long delay = new Date().getTime() - start.getTime();
			if (delay >= slowQueryTime)
				showSlowInternal(delay, nRows, desc);
		}
	}

	private void showSlow(Date start, ResultSet rs, String desc)
			throws SQLException {
		if (slowQueryTime >= 0) {
			nQueriesInProgress--;
			long delay = new Date().getTime() - start.getTime();
			if (delay >= slowQueryTime) {
				// try {
				showSlowInternal(delay, MyResultSet.nRows(rs), desc);
				// } catch (SQLException e) {
				// e.printStackTrace();
				// }
			}
		}
	}

	private void showSlowInternal(long delay, int nRows, String desc) {
		showSlowInternal(delay, Integer.toString(nRows), desc);
	}

	private void showSlowInternal(long delay, String nRows, String desc) {
		StringBuffer buf = new StringBuffer(desc.length() + 60);
		if (nQueriesInProgress > 0)
			buf.append("\n** ").append(nQueriesInProgress).append(
					" in progress **  ");
		buf.append("\n   ");
		buf.append(delay).append("ms    rows ").append(nRows).append("\n   ")
				.append(desc);
		print(buf.toString());
	}

	public int SQLupdate(String SQL) throws SQLException {
		return SQLupdateInternal(null, SQL);
	}

	public int SQLupdate(PreparedStatement SQL, String desc)
			throws SQLException {
		return SQLupdateInternal(SQL, desc);
	}

	private Statement myCreateStatement(String SQL) throws SQLException {
		Statement result = con.createStatement();
		if (slowQueryTime >= 0)
			statements.put(result, SQL);
		return result;
	}

	private int SQLupdateInternal(PreparedStatement SQL, String desc)
			throws SQLException {
		Date start = initSlow(desc);
		int nRows = 0;
		// try {
		if (SQL != null)
			nRows = SQL.executeUpdate();
		else {
			Statement statement = null;
			try {
				statement = con.createStatement();
				nRows = statement.executeUpdate(desc);
			} finally {
				if (statement != null)
					statement.close();
			}
		}
		// } catch (SQLException se) {
		// System.err.println("While executing:\n" + SQL);
		// System.err.println("SQL Exception: " + se.getMessage());
		// se.printStackTrace();
		// }
		showSlow(start, nRows, desc);
		return nRows;
	}

	public int[] SQLupdate(String[] SQL) throws SQLException {
		int nStatements = SQL.length;
		assert nStatements > 0;
		Date start = initSlow(SQL);
		int[] nRows = null;
		// try {
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			for (int i = 0; i < nStatements; i++) {
				// print(SQL[i]);
				stmt.addBatch(SQL[i]);
			}
			nRows = stmt.executeBatch();
		} finally {
			if (stmt != null)
				stmt.close();
		}
		// } catch (SQLException se) {
		// System.err.println("While executing:\n" + SQL);
		// System.err.println("SQL Exception: " + se.getMessage());
		// se.printStackTrace();
		// }
		showSlow(start, nRows, SQL);
		return nRows;
	}

	public ResultSet SQLquery(String SQL) throws SQLException {
		return SQLqueryInternal(null, SQL);
	}

	public ResultSet SQLquery(PreparedStatement SQL, String desc)
			throws SQLException {
		return SQLqueryInternal(SQL, desc);
	}

	private ResultSet SQLqueryInternal(PreparedStatement SQL, String desc)
			throws SQLException {
		Date start = initSlow(desc);
		ResultSet rs = null;
		// try {
		if (SQL != null)
			rs = SQL.executeQuery();
		else {
			// try {
			rs = myCreateStatement(desc).executeQuery(desc);
			// } catch (SQLException ex) {
			// // This doesn't do any good, because all the prepared statements
			// // will break when you try to set parameters.
			// // Solution is to have mySetInt, etc. that catch this and call
			// // prepareStatement again. You'd write
			// // ps = mySetInt(ps, 1, 42)
			// // But prepared statements don't let you get the SQL back, so
			// // this would have to be stored in a hash table
			// // (or in MyPreparedStatement, but it's ugly to have to write
			// // all the required methods). Then you'd write
			// // ps = mySetInt(ps, 1, 42, SQL)
			//
			// try {
			// print("Got exception " + ex + "Try reopening connection...");
			//
			// close();
			// open(info);
			//
			// // Now retry executing the query. If it was a time-out, this
			// // time it should work
			//
			// rs = myCreateStatement(desc).executeQuery(desc);
			// print("Retry worked!");
			//
			// } catch (Exception secondEx) {
			// print("Got another exception while retrying: " + secondEx);
			// throw ex;
			// // this was not a time-out -- do some error handling
			// }
			// }
			if (rs == null)
				print("Null result set for: " + desc);
		}

		// Display the SQL Results
		// while(rs.next( )) {
		// System.out.println(rs.getString("tabl"));
		// }
		// } catch (SQLException se) {
		// System.err.println("While executing:\n" + SQL);
		// System.err.println("SQL Exception: " + se.getMessage());
		// }
		showSlow(start, rs, desc);
		return rs;
	}

	public int SQLqueryInt(String SQL) throws SQLException {
		return SQLqueryIntInternal(SQLquery(SQL), SQL);
	}

	public int SQLqueryInt(PreparedStatement SQL, String desc)
			throws SQLException {
		return SQLqueryIntInternal(SQLquery(SQL, desc), desc);
	}

	private int SQLqueryIntInternal(ResultSet rs, String desc)
			throws SQLException {
		int result = -1;
		try {
			if (rs.next()) {
				result = rs.getInt(1);
				assert !rs.next() : desc + " returned multiple records.";
			}
		} finally {
			close(rs);
		}
		// } catch (SQLException se) {
		// System.err.println("SQL Exception: " + se.getMessage());
		// se.printStackTrace();
		// }
		return result;
	}

	public int[] SQLqueryIntArray(String SQL) throws SQLException {
		return SQLqueryIntArrayInternal(SQLquery(SQL));
	}

	public int[] SQLqueryIntArray(PreparedStatement SQL, String desc)
			throws SQLException {
		return SQLqueryIntArrayInternal(SQLquery(SQL, desc));
	}

	private int[] SQLqueryIntArrayInternal(ResultSet rs) throws SQLException {
		int[] result = null;
		try {
			result = new int[MyResultSet.nRows(rs)];
			for (int i = 0; i < result.length; i++) {
				rs.next();
				result[i] = rs.getInt(1);
			}
		} finally {
			close(rs);
		}
		// } catch (SQLException se) {
		// System.err.println("SQL Exception: " + se.getMessage());
		// se.printStackTrace();
		// }
		return result;
	}

	public String[] SQLqueryStringArray(String SQL) throws SQLException {
		return SQLqueryStringArrayInternal(SQLquery(SQL));
	}

	public String[] SQLqueryStringArray(PreparedStatement SQL, String desc)
			throws SQLException {
		return SQLqueryStringArrayInternal(SQLquery(SQL, desc));
	}

	private String[] SQLqueryStringArrayInternal(ResultSet rs)
			throws SQLException {
		String[] result = null;
		try {
			result = new String[MyResultSet.nRows(rs)];
			for (int i = 0; i < result.length; i++) {
				rs.next();
				result[i] = rs.getString(1);
			}
		} finally {
			close(rs);
		}
		// } catch (SQLException se) {
		// System.err.println("SQL Exception: " + se.getMessage());
		// se.printStackTrace();
		// }
		return result;
	}

	public double SQLqueryDouble(String SQL) throws SQLException {
		return SQLqueryDoubleInternal(SQLquery(SQL), SQL);
	}

	public double SQLqueryDouble(PreparedStatement SQL, String desc)
			throws SQLException {
		return SQLqueryDoubleInternal(SQLquery(SQL, desc), desc);
	}

	private double SQLqueryDoubleInternal(ResultSet rs, String desc)
			throws SQLException {
		double result = -1;
		try {
			if (rs.next()) {
				result = rs.getDouble(1);
				assert !rs.next() : desc + " returned multiple records.";
			}
		} finally {
			close(rs);
		}
		// } catch (SQLException se) {
		// System.err.println("SQL Exception: " + se.getMessage());
		// se.printStackTrace();
		// }
		return result;
	}

	public String SQLqueryString(String SQL) throws SQLException {
		return SQLqueryStringInternal(SQLquery(SQL), SQL);
	}

	public String SQLqueryString(PreparedStatement SQL, String desc)
			throws SQLException {
		return SQLqueryStringInternal(SQLquery(SQL, desc), desc);
	}

	private String SQLqueryStringInternal(ResultSet rs, String desc)
			throws SQLException {
		String result = null;
		try {
			if (rs.next()) {
				result = rs.getString(1);
				assert !rs.next() : desc + " returned multiple records.";
			}
		} finally {
			close(rs);
		}
		// } catch (SQLException se) {
		// System.err.println("SQL Exception: " + se.getMessage());
		// se.printStackTrace();
		// }
		return result;
	}

	public void close(ResultSet rs) throws SQLException {
		// try {
		Statement s = rs.getStatement();
		if (s instanceof PreparedStatement)
			rs.close();
		else {
			if (slowQueryTime >= 0)
				statements.remove(s);
			s.close();
			// } catch (SQLException se) {
			// System.err.println("SQL Exception: " + se.getMessage());
			// se.printStackTrace();
			// }
		}
	}

	public PreparedStatement prepareStatement(String SQL) throws SQLException {
		PreparedStatement result = null;
		// try {
		result = con.prepareStatement(SQL);
		// } catch (SQLException e) {
		// e.printStackTrace();
		// }
		return result;
	}

	public PreparedStatement prepareStatement(String SQL, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		PreparedStatement result = null;
		// try {
		result = con.prepareStatement(SQL, resultSetType, resultSetConcurrency);
		// } catch (SQLException e) {
		// e.printStackTrace();
		// }
		return result;
	}

	private Map preparedStatements = new Hashtable();

	public PreparedStatement lookupPS(String SQL) throws SQLException {
		PreparedStatement ps = (PreparedStatement) preparedStatements.get(SQL);
		if (ps == null) {
			ps = prepareStatement(SQL);
			preparedStatements.put(SQL, ps);
		}
		return ps;
	}

	public String unsignedTypeForMaxValue(int max) {
		assert max >= 0;
		String type = null;
		if (false && max < 256)
			// leave room for adding facets for personal database
			type = "TINYINT";
		else if (max < 256 * 256)
			type = "SMALLINT";
		else if (max < 256 * 256 * 256)
			type = "MEDIUMINT";
		else if (max < 256 * 256 * 256 * 256)
			type = "INT";
		else
			assert false : max + " is too big.";
		// type += " UNSIGNED NOT NULL DEFAULT 0";
		type += " UNSIGNED NOT NULL";
		// print("unsignedTypeForMaxValue " + max + " => " + type);
		return type;
	}

	public void ensureIndex(String table, String name, String columnNames) throws SQLException {
		ensureIndex(table, name, columnNames, "");
	}

	public void ensureIndex(String table, String name, String columnNames,
			String type) throws SQLException {
		assert type == ""
				|| "PRIMARY, UNIQUE, FULLTEXT, SPATIAL, BTREE, HASH, RTREE"
						.indexOf(type) >= 0;
		String oldColumns = SQLqueryString("SELECT GROUP_CONCAT(CONCAT(column_name, "
				+ "IF(sub_part IS NULL,'',CONCAT('(',sub_part,')')))) "
				+ "FROM information_schema.STATISTICS "
				+ "WHERE table_schema = '"
				+ dbName
				+ "' AND table_name = '"
				+ table
				+ "' AND index_name = '"
				+ name
				+ "' ORDER BY seq_in_index");
		if (!columnNames.equals(oldColumns)) {
			Util.print("Redoing index " + table + " " + oldColumns + " => "
					+ columnNames);
			if (oldColumns != null)
				SQLupdate("ALTER TABLE " + table + " DROP INDEX " + name);
			String indexSpec = "INDEX";
			if ("UNIQUE, FULLTEXT, SPATIAL".indexOf(type) >= 0)
				indexSpec = type + " INDEX";
			else if ("PRIMARY".indexOf(type) >= 0) {
				indexSpec = "PRIMARY KEY";
				assert name.equals("");
			} else if ("BTREE, HASH, RTREE".indexOf(type) >= 0)
				name += " USING " + type;
			SQLupdate("ALTER TABLE " + table + " ADD " + indexSpec + " " + name
					+ " (" + columnNames + ")");
		}
	}

	public String toString() {
		return "<JDBCsample " + connectString + ">";
	}

}