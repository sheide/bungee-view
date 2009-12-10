package edu.cmu.cs.bungee.javaExtensions;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager; //import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData; //import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning; //import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.swing.SwingUtilities;

public class JDBCSample {

	static Map<Statement, String> statements = new HashMap<Statement, String>();

	/**
	 * Print queries that take longer than this. Print all queries and extra
	 * info if slowQueryTime = 0. Never print if slowQueryTime < 0.
	 */
	private final static int slowQueryTime = -1;

	/**
	 * Only used by showSlow.
	 */
	private int nQueriesInProgress = 0;

	private static Object driver; // We don't need the driver;

	Connection con;

	private String connectString;

	private GenericServlet servlet; // Used to write to log file. Call print

	public String dbName;

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
		// +"&useUsageAdvisor=true";
		if (driver == null)
			driver = Class.forName("com.mysql.jdbc.Driver").newInstance();
		print("Connect string " + connectString);
		con = DriverManager.getConnection(connectString);
	}

	public void close() throws SQLException {
		if (slowQueryTime >= 0) {
			print(statements.size() + " SQL statements were not closed:");
			// if (statements.size() > 0) {
			for (String SQL : statements.values()) {
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

	private Date initSlow(Object desc) {
		Date start = null;
		if (slowQueryTime >= 0) {
			if (SwingUtilities.isEventDispatchThread())
				print("Calling DB in EventDispatchThread: " + desc);
			if (slowQueryTime == 0) {
				StringBuffer buf = new StringBuffer(2000);
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

	private void showSlow(Date start, int nRows, Object desc) {
		if (slowQueryTime >= 0) {
			nQueriesInProgress--;
			long delay = new Date().getTime() - start.getTime();
			if (delay >= slowQueryTime)
				showSlowInternal(delay, nRows, desc);
		}
	}

	private void showSlow(Date start, ResultSet rs, Object desc)
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

	private void showSlowInternal(long delay, int nRows, Object desc) {
		showSlowInternal(delay, Integer.toString(nRows), desc);
	}

	private void showSlowInternal(long delay, String nRows, Object desc) {
		StringBuffer buf = new StringBuffer(2000);
		if (nQueriesInProgress > 0)
			buf.append("\n** ").append(nQueriesInProgress).append(
					" in progress **  ");
		buf.append("\n   ");
		buf.append(delay).append("ms    rows ").append(nRows).append("\n   ")
				.append(desc);
		print(buf.toString());
	}

	public int SQLupdate(String SQL) throws SQLException {
		return SQLupdateInternal(SQL);
	}

	public int SQLupdate(PreparedStatement SQL) throws SQLException {
		return SQLupdateInternal(SQL);
	}

	private Statement myCreateStatement(String SQL) throws SQLException {
		Statement result = con.createStatement();
		if (slowQueryTime >= 0)
			statements.put(result, SQL);
		return result;
	}

	private int SQLupdateInternal(PreparedStatement SQL) throws SQLException {
		Date start = initSlow(SQL);
		int nRows = 0;
		try {
			nRows = SQL.executeUpdate();
		} catch (SQLException se) {
			print("While executing:\n" + SQL);
			// System.err.println("SQL Exception: " + se.getMessage());
			// se.printStackTrace();
			throw (se);
		}
		showSlow(start, nRows, SQL);
		return nRows;
	}

	private int SQLupdateInternal(String SQL) throws SQLException {
		Date start = initSlow(SQL);
		int nRows = 0;
		try {
			Statement statement = null;
			try {
				statement = con.createStatement();
				nRows = statement.executeUpdate(SQL);
			} finally {
				if (statement != null)
					statement.close();
			}
		} catch (SQLException se) {
			print("While executing:\n" + SQL);
			// System.err.println("SQL Exception: " + se.getMessage());
			// se.printStackTrace();
			throw (se);
		}
		showSlow(start, nRows, SQL);
		return nRows;
	}

	public int[] SQLupdate(String[] SQL) throws SQLException {
		int nStatements = SQL.length;
		assert nStatements > 0;
		Date start = initSlow(SQL);
		int[] nRows = null;
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			for (int i = 0; i < nStatements; i++) {
				// print(SQL[i]);
				stmt.addBatch(SQL[i]);
			}
			nRows = stmt.executeBatch();
		} catch (SQLException se) {
			print("While executing:\n" + SQL);
			// System.err.println("SQL Exception: " + se.getMessage());
			// se.printStackTrace();
			throw (se);
		} finally {
			if (stmt != null)
				stmt.close();
		}
		showSlow(start, nRows, SQL);
		return nRows;
	}

	public ResultSet SQLquery(String SQL) throws SQLException {
		return SQLqueryInternal(SQL);
	}

	public ResultSet SQLquery(PreparedStatement SQL) throws SQLException {
		return SQLqueryInternal(SQL);
	}

	private ResultSet SQLqueryInternal(PreparedStatement SQL)
			throws SQLException {
		Date start = initSlow(SQL);
		ResultSet rs = null;
		try {
			rs = SQL.executeQuery();
		} catch (SQLException se) {
			print("While executing:\n" + SQL);
			throw (se);
		}
		showSlow(start, rs, SQL);
		return rs;
	}

	private ResultSet SQLqueryInternal(String SQL) throws SQLException {
		Date start = initSlow(SQL);
		ResultSet rs = null;
		try {
			rs = myCreateStatement(SQL).executeQuery(SQL);
			// } catch (SQLException ex) {
			// // This doesn't do any good, because all the prepared
			// statements
			// // will break when you try to set parameters.
			// // Solution is to have mySetInt, etc. that catch this and
			// call
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
				print("Null result set for: " + SQL);
		} catch (SQLException se) {
			print("While executing:\n" + SQL);
			throw (se);
		}
		showSlow(start, rs, SQL);
		return rs;
	}

	public int SQLqueryInt(String SQL) throws SQLException {
		return SQLqueryIntInternal(SQLquery(SQL), SQL);
	}

	public int SQLqueryInt(PreparedStatement SQL) throws SQLException {
		return SQLqueryIntInternal(SQLquery(SQL), SQL.toString());
	}

	private int SQLqueryIntInternal(ResultSet rs, String SQL)
			throws SQLException {
		int result = -1;
		try {
			if (rs.next()) {
				result = rs.getInt(1);
				assert !rs.next() : SQL + " returned multiple records.";
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

	public int[] SQLqueryIntArray(PreparedStatement SQL) throws SQLException {
		return SQLqueryIntArrayInternal(SQLquery(SQL));
	}

	private int[] SQLqueryIntArrayInternal(ResultSet rs) throws SQLException {
		int[] result = null;
		try {
			result = new int[MyResultSet.nRows(rs)];
			for (int i = 0; i < result.length; i++) {
				rs.next();
				result[i] = rs.getInt(1);
			}
		} catch (SQLException se) {
			print("SQL Exception: " + se.getMessage());
			// se.printStackTrace();
			throw (se);
		} finally {
			close(rs);
		}
		return result;
	}

	public String[] SQLqueryStringArray(String SQL) throws SQLException {
		return SQLqueryStringArrayInternal(SQLquery(SQL));
	}

	public String[] SQLqueryStringArray(PreparedStatement SQL)
			throws SQLException {
		return SQLqueryStringArrayInternal(SQLquery(SQL));
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

	public double SQLqueryDouble(PreparedStatement SQL) throws SQLException {
		return SQLqueryDoubleInternal(SQLquery(SQL), SQL);
	}

	private double SQLqueryDoubleInternal(ResultSet rs, Object desc)
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

	public String SQLqueryString(PreparedStatement SQL) throws SQLException {
		return SQLqueryStringInternal(SQLquery(SQL), SQL);
	}

	private String SQLqueryStringInternal(ResultSet rs, Object desc)
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

	public PreparedStatement prepareStatement(String SQL, int resultSetType,
			int resultSetConcurrency) throws SQLException {
		PreparedStatement result = null;
		try {
			result = con.prepareStatement(SQL, resultSetType,
					resultSetConcurrency);
		} catch (SQLException e) {
			print("While preparing: " + SQL);
			// e.printStackTrace();
			throw (e);
		}
		return result;
	}

	private Map<String, MyPreparedStatement> myPreparedStatements = new Hashtable<String, MyPreparedStatement>();

	public PreparedStatement lookupPS(String SQL) throws SQLException {
		MyPreparedStatement ps = myPreparedStatements
				.get(SQL);
		if (ps == null) {
			ps = new MyPreparedStatement(SQL);
			myPreparedStatements.put(SQL, ps);
		}
		return ps;
	}

	/**
	 * A PreparedStatement that remembers its source SQL. In the future,
	 * possibly remember parameter values, too, as an array of Objects or
	 * Strings.
	 * 
	 */
	public class MyPreparedStatement implements PreparedStatement {

		private PreparedStatement ps;
		private String SQL;
		private String[] paramValues = new String[10];

		MyPreparedStatement(String _SQL) throws SQLException {
			try {
				SQL = _SQL;
				ps = con.prepareStatement(SQL);
			} catch (SQLException e) {
				print("While preparing: " + SQL);
				// e.printStackTrace();
				throw (e);
			}
		}

		@Override
		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append("<MyPreparedStatement ").append(SQL).append("[");
			try {
				ParameterMetaData parameterMetaData = ps.getParameterMetaData();
				int parameterCount = parameterMetaData.getParameterCount();
				for (int i = 1; i <= parameterCount; i++) {
					if (i > 1)
						buf.append(", ");
					buf.append(paramValues[i]);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			buf.append("]>");
			return buf.toString();
		}

		public void addBatch() throws SQLException {
			ps.addBatch();
		}

		public void clearParameters() throws SQLException {
			ps.clearParameters();
		}

		public boolean execute() throws SQLException {
			return ps.execute();
		}

		public ResultSet executeQuery() throws SQLException {
			return ps.executeQuery();
		}

		public int executeUpdate() throws SQLException {
			return ps.executeUpdate();
		}

		public ResultSetMetaData getMetaData() throws SQLException {
			return ps.getMetaData();
		}

		public ParameterMetaData getParameterMetaData() throws SQLException {
			return ps.getParameterMetaData();
		}

		public void setArray(int i, Array x) throws SQLException {
			paramValues[i] = x.toString();
			ps.setArray(i, x);
		}

		public void setAsciiStream(int parameterIndex, InputStream x, int length)
				throws SQLException {
			paramValues[parameterIndex] = x.toString();
			ps.setAsciiStream(parameterIndex, x, length);
		}

		public void setBigDecimal(int parameterIndex, BigDecimal x)
				throws SQLException {
			paramValues[parameterIndex] = x.toString();
			ps.setBigDecimal(parameterIndex, x);
		}

		public void setBinaryStream(int parameterIndex, InputStream x,
				int length) throws SQLException {
			paramValues[parameterIndex] = x.toString();
			ps.setBinaryStream(parameterIndex, x, length);
		}

		public void setBlob(int i, Blob x) throws SQLException {
			paramValues[i] = x.toString();
			ps.setBlob(i, x);
		}

		public void setBoolean(int parameterIndex, boolean x)
				throws SQLException {
			paramValues[parameterIndex] = Boolean.toString(x);
			ps.setBoolean(parameterIndex, x);
		}

		public void setByte(int parameterIndex, byte x) throws SQLException {
			paramValues[parameterIndex] = Byte.toString(x);
			ps.setByte(parameterIndex, x);
		}

		public void setBytes(int parameterIndex, byte[] x) throws SQLException {
			paramValues[parameterIndex] = x.toString();
			ps.setBytes(parameterIndex, x);
		}

		public void setCharacterStream(int parameterIndex, Reader reader,
				int length) throws SQLException {
			paramValues[parameterIndex] = reader.toString();
			ps.setCharacterStream(parameterIndex, reader, length);
		}

		public void setClob(int i, Clob x) throws SQLException {
			paramValues[i] = x.toString();
			ps.setClob(i, x);
		}

		public void setDate(int parameterIndex, java.sql.Date x)
				throws SQLException {
			paramValues[parameterIndex] = x.toString();
			ps.setDate(parameterIndex, x);
		}

		public void setDate(int parameterIndex, java.sql.Date x, Calendar cal)
				throws SQLException {
			paramValues[parameterIndex] = x.toString();
			ps.setDate(parameterIndex, x, cal);
		}

		public void setDouble(int parameterIndex, double x) throws SQLException {
			paramValues[parameterIndex] = Double.toString(x);
			ps.setDouble(parameterIndex, x);
		}

		public void setFloat(int parameterIndex, float x) throws SQLException {
			paramValues[parameterIndex] = Float.toString(x);
			ps.setFloat(parameterIndex, x);
		}

		public void setInt(int parameterIndex, int x) throws SQLException {
			paramValues[parameterIndex] = Integer.toString(x);
			ps.setInt(parameterIndex, x);
		}

		public void setLong(int parameterIndex, long x) throws SQLException {
			paramValues[parameterIndex] = Long.toString(x);
			ps.setLong(parameterIndex, x);
		}

		public void setNull(int parameterIndex, int sqlType)
				throws SQLException {
			ps.setNull(parameterIndex, sqlType);
		}

		public void setNull(int paramIndex, int sqlType, String typeName)
				throws SQLException {
			ps.setNull(paramIndex, sqlType, typeName);
		}

		public void setObject(int parameterIndex, Object x) throws SQLException {
			paramValues[parameterIndex] = x.toString();
			ps.setObject(parameterIndex, x);
		}

		public void setObject(int parameterIndex, Object x, int targetSqlType)
				throws SQLException {
			paramValues[parameterIndex] = x.toString();
			ps.setObject(parameterIndex, x, targetSqlType);
		}

		public void setObject(int parameterIndex, Object x, int targetSqlType,
				int scale) throws SQLException {
			paramValues[parameterIndex] = x.toString();
			ps.setObject(parameterIndex, x, targetSqlType, scale);
		}

		public void setRef(int i, Ref x) throws SQLException {
			paramValues[i] = x.toString();
			ps.setRef(i, x);
		}

		public void setShort(int parameterIndex, short x) throws SQLException {
			paramValues[parameterIndex] = Short.toString(x);
			ps.setShort(parameterIndex, x);
		}

		public void setString(int parameterIndex, String x) throws SQLException {
			paramValues[parameterIndex] = x;
			ps.setString(parameterIndex, x);
		}

		public void setTime(int parameterIndex, Time x) throws SQLException {
			paramValues[parameterIndex] = x.toString();
			ps.setTime(parameterIndex, x);
		}

		public void setTime(int parameterIndex, Time x, Calendar cal)
				throws SQLException {
			paramValues[parameterIndex] = x.toString();
			ps.setTime(parameterIndex, x, cal);
		}

		public void setTimestamp(int parameterIndex, Timestamp x)
				throws SQLException {
			paramValues[parameterIndex] = x.toString();
			ps.setTimestamp(parameterIndex, x);
		}

		public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
				throws SQLException {
			paramValues[parameterIndex] = x.toString();
			ps.setTimestamp(parameterIndex, x, cal);
		}

		public void setURL(int parameterIndex, URL x) throws SQLException {
			paramValues[parameterIndex] = x.toString();
			ps.setURL(parameterIndex, x);
		}

		public void setUnicodeStream(int parameterIndex, InputStream x,
				int length) throws SQLException {
			paramValues[parameterIndex] = x.toString();
			// ps.setUnicodeStream(parameterIndex, x,
			// length);
		}

		public void addBatch(String arg0) throws SQLException {
			ps.addBatch(arg0);
		}

		public void cancel() throws SQLException {
			ps.cancel();
		}

		public void clearBatch() throws SQLException {
			ps.clearBatch();
		}

		public void clearWarnings() throws SQLException {
			ps.clearWarnings();
		}

		public void close() throws SQLException {
			ps.close();
		}

		public boolean execute(String arg0) throws SQLException {
			return ps.execute(arg0);
		}

		public boolean execute(String arg0, int arg1) throws SQLException {
			return ps.execute(arg0, arg1);
		}

		public boolean execute(String arg0, int[] arg1) throws SQLException {
			return ps.execute(arg0, arg1);
		}

		public boolean execute(String arg0, String[] arg1) throws SQLException {
			return ps.execute(arg0, arg1);
		}

		public int[] executeBatch() throws SQLException {
			return null;
		}

		public ResultSet executeQuery(String arg0) throws SQLException {
			return ps.executeQuery(arg0);
		}

		public int executeUpdate(String arg0) throws SQLException {
			return ps.executeUpdate(arg0);
		}

		public int executeUpdate(String arg0, int arg1) throws SQLException {
			return ps.executeUpdate(arg0, arg1);
		}

		public int executeUpdate(String arg0, int[] arg1) throws SQLException {
			return ps.executeUpdate(arg0, arg1);
		}

		public int executeUpdate(String arg0, String[] arg1)
				throws SQLException {
			return ps.executeUpdate(arg0, arg1);
		}

		public Connection getConnection() throws SQLException {
			return ps.getConnection();
		}

		public int getFetchDirection() throws SQLException {
			return ps.getFetchDirection();
		}

		public int getFetchSize() throws SQLException {
			return ps.getFetchSize();
		}

		public ResultSet getGeneratedKeys() throws SQLException {
			return ps.getGeneratedKeys();
		}

		public int getMaxFieldSize() throws SQLException {
			return ps.getMaxFieldSize();
		}

		public int getMaxRows() throws SQLException {
			return ps.getMaxRows();
		}

		public boolean getMoreResults() throws SQLException {
			return ps.getMoreResults();
		}

		public boolean getMoreResults(int arg0) throws SQLException {
			return ps.getMoreResults(arg0);
		}

		public int getQueryTimeout() throws SQLException {
			return ps.getQueryTimeout();
		}

		public ResultSet getResultSet() throws SQLException {
			return ps.getResultSet();
		}

		public int getResultSetConcurrency() throws SQLException {
			return ps.getResultSetConcurrency();
		}

		public int getResultSetHoldability() throws SQLException {
			return ps.getResultSetHoldability();
		}

		public int getResultSetType() throws SQLException {
			return ps.getResultSetType();
		}

		public int getUpdateCount() throws SQLException {
			return ps.getUpdateCount();
		}

		public SQLWarning getWarnings() throws SQLException {
			return ps.getWarnings();
		}

		public void setCursorName(String arg0) throws SQLException {
			ps.setCursorName(arg0);
		}

		public void setEscapeProcessing(boolean arg0) throws SQLException {
			ps.setEscapeProcessing(arg0);
		}

		public void setFetchDirection(int arg0) throws SQLException {
			ps.setFetchDirection(arg0);
		}

		public void setFetchSize(int arg0) throws SQLException {
			ps.setFetchSize(arg0);
		}

		public void setMaxFieldSize(int arg0) throws SQLException {
			ps.setMaxFieldSize(arg0);
		}

		public void setMaxRows(int arg0) throws SQLException {
			ps.setMaxRows(arg0);
		}

		public void setQueryTimeout(int arg0) throws SQLException {
			ps.setQueryTimeout(arg0);
		}

		@SuppressWarnings("unused")
		public void setAsciiStream(int arg0, InputStream arg1)
				throws SQLException {
			// TODO Auto-generated method stub
		}

		@SuppressWarnings("unused")
		public void setAsciiStream(int arg0, InputStream arg1, long arg2)
				throws SQLException {
			// TODO Auto-generated method stub
		}

		@SuppressWarnings("unused")
		public void setBinaryStream(int arg0, InputStream arg1)
				throws SQLException {
			// TODO Auto-generated method stub
		}

		@SuppressWarnings("unused")
		public void setBinaryStream(int arg0, InputStream arg1, long arg2)
				throws SQLException {
			// TODO Auto-generated method stub
		}

		@SuppressWarnings("unused")
		public void setBlob(int arg0, InputStream arg1) throws SQLException {
			// TODO Auto-generated method stub
		}

		@SuppressWarnings("unused")
		public void setBlob(int arg0, InputStream arg1, long arg2)
				throws SQLException {
			// TODO Auto-generated method stub
		}

		@SuppressWarnings("unused")
		public void setCharacterStream(int arg0, Reader arg1)
				throws SQLException {
			// TODO Auto-generated method stub
		}

		@SuppressWarnings("unused")
		public void setCharacterStream(int arg0, Reader arg1, long arg2)
				throws SQLException {
			// TODO Auto-generated method stub
		}

		@SuppressWarnings("unused")
		public void setClob(int arg0, Reader arg1) throws SQLException {
			// TODO Auto-generated method stub
		}

		@SuppressWarnings("unused")
		public void setClob(int arg0, Reader arg1, long arg2)
				throws SQLException {
			// TODO Auto-generated method stub
		}

		@SuppressWarnings("unused")
		public void setNCharacterStream(int arg0, Reader arg1)
				throws SQLException {
			// TODO Auto-generated method stub
		}

		@SuppressWarnings("unused")
		public void setNCharacterStream(int arg0, Reader arg1, long arg2)
				throws SQLException {
			// TODO Auto-generated method stub
		}

		// public void setNClob(int arg0, NClob arg1) throws SQLException {
		// // TODO Auto-generated method stub
		// }
		//
		// public void setNClob(int arg0, Reader arg1) throws SQLException {
		// // TODO Auto-generated method stub
		// }
		//
		// public void setNClob(int arg0, Reader arg1, long arg2)
		// throws SQLException {
		// // TODO Auto-generated method stub
		// }

		@SuppressWarnings("unused")
		public void setNString(int arg0, String arg1) throws SQLException {
			// TODO Auto-generated method stub
		}

		// public void setRowId(int arg0, RowId arg1) throws SQLException {
		// // TODO Auto-generated method stub
		// }
		//
		// public void setSQLXML(int arg0, SQLXML arg1) throws SQLException {
		// // TODO Auto-generated method stub
		// }

		@SuppressWarnings("unused")
		public boolean isClosed() throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}

		@SuppressWarnings("unused")
		public boolean isPoolable() throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}

		@SuppressWarnings("unused")
		public void setPoolable(boolean arg0) throws SQLException {
			// TODO Auto-generated method stub

		}

		@SuppressWarnings("unused")
		public boolean isWrapperFor(Class<?> arg0) throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}

		@SuppressWarnings("unused")
		public Object unwrap(Class<?> arg0) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

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

	public void ensureIndex(String table, String name, String columnNames)
			throws SQLException {
		ensureIndex(dbName, table, name, columnNames, "");
	}

	public void ensureIndex(String table, String name, String columnNames,
			String type) throws SQLException {
		ensureIndex(dbName, table, name, columnNames, type);
	}

	public void ensureIndex(String db, String table, String name,
			String columnNames, String type) throws SQLException {
		assert type == ""
				|| "PRIMARY, UNIQUE, FULLTEXT, SPATIAL, BTREE, HASH, RTREE"
						.indexOf(type) >= 0;
		String oldColumns = SQLqueryString("SELECT GROUP_CONCAT(CONCAT(column_name, "
				+ "IF(sub_part IS NULL,'',CONCAT('(',sub_part,')')))) "
				+ "FROM information_schema.STATISTICS "
				+ "WHERE table_schema = '"
				+ db
				+ "' AND table_name = '"
				+ table
				+ "' AND index_name = '"
				+ name
				+ "' ORDER BY seq_in_index");
		if (!columnNames.equalsIgnoreCase(oldColumns)) {
			Util.print("Redoing index " + table + "." + name + " " + oldColumns
					+ " => " + columnNames);
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

	public boolean databaseExists(String dbName1) throws SQLException {
		return SQLqueryInt("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = '"
				+ dbName1 + "'") > 0;
	}

	public boolean tableExists(String table) throws SQLException {
		return SQLqueryInt("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = '"
				+ dbName + "' AND TABLE_NAME = '" + table + "'") > 0;
	}

	public boolean columnExists(String schema, String table, String column)
			throws SQLException {
		return SQLqueryInt("SELECT COUNT(*) FROM information_schema.columns "
				+ "WHERE table_schema = '" + schema + "' AND table_name = '"
				+ table + "' AND column_name = '" + column + "'") > 0;
	}

	@Override
	public String toString() {
		return "<JDBCsample " + connectString + ">";
	}

}