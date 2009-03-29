package edu.cmu.cs.bungee.javaExtensions;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MyResultSet implements ResultSet {
	private static final long serialVersionUID = -959016825795947094L;

	private int currentRow;

	int nRows;

	private Column[] columns;

	int getColumnCount() {
		return columns.length;
	}

	static List makeColumnTypeList(Object col1, Object col2, Object col3,
			Object col4, Object col5, Object col6, Object col7, Object col8,
			Object col9, Object col10) {
		List temp = new ArrayList();
		addCol(col1, temp);
		addCol(col2, temp);
		addCol(col3, temp);
		addCol(col4, temp);
		addCol(col5, temp);
		addCol(col6, temp);
		addCol(col7, temp);
		addCol(col8, temp);
		addCol(col9, temp);
		addCol(col10, temp);
		return Collections.unmodifiableList(temp);
	}

	static void addCol(Object type, List temp) {
		if (type != null)
			temp.add(type);
	}

	public static final List INT = makeColumnTypeList(Column.IntegerType, null,
			null, null, null, null, null, null, null, null);

	public static final List INT_INT = makeColumnTypeList(Column.IntegerType,
			Column.IntegerType, null, null, null, null, null, null, null, null);

	public static final List INT_INT_INT = makeColumnTypeList(
			Column.IntegerType, Column.IntegerType, Column.IntegerType, null,
			null, null, null, null, null, null);

	public static final List INT_INT_INT_INT = makeColumnTypeList(
			Column.IntegerType, Column.IntegerType, Column.IntegerType,
			Column.IntegerType, null, null, null, null, null, null);

	public static final List INT_INT_STRING = makeColumnTypeList(
			Column.IntegerType, Column.IntegerType, Column.StringType, null,
			null, null, null, null, null, null);

	public static final List INT_INT_INT_STRING = makeColumnTypeList(
			Column.IntegerType, Column.IntegerType, Column.IntegerType,
			Column.StringType, null, null, null, null, null, null);

	public static final List INT_PINT_STRING_INT_INT_INT_INT_DOUBLE_PINT_PINT = makeColumnTypeList(
			Column.IntegerType, Column.PositiveIntegerType, Column.StringType,
			Column.IntegerType, Column.IntegerType, Column.IntegerType,
			Column.IntegerType, Column.DoubleType, Column.PositiveIntegerType,
			Column.PositiveIntegerType);

	public static final List INT_SINT = makeColumnTypeList(Column.IntegerType,
			Column.SortedIntegerType, null, null, null, null, null, null, null,
			null);

	public static final List INT_STRING = makeColumnTypeList(
			Column.IntegerType, Column.StringType, null, null, null, null,
			null, null, null, null);

	public static final List PINT_SINT_STRING_INT_INT_INT = makeColumnTypeList(
			Column.PositiveIntegerType, Column.SortedIntegerType,
			Column.StringType, Column.IntegerType, Column.IntegerType,
			Column.IntegerType, null, null, null, null);

	public static final List PINT_SINT_STRING_INT_INT_INT_INT = makeColumnTypeList(
			Column.PositiveIntegerType, Column.SortedIntegerType,
			Column.StringType, Column.IntegerType, Column.IntegerType,
			Column.IntegerType, Column.IntegerType, null, null, null);

	public static final List SINT = makeColumnTypeList(
			Column.SortedIntegerType, null, null, null, null, null, null, null,
			null, null);

	public static final List SINT_IMAGE_INT_INT = makeColumnTypeList(
			Column.SortedIntegerType, Column.ImageType, Column.IntegerType,
			Column.IntegerType, null, null, null, null, null, null);

	public static final List SINT_PINT = makeColumnTypeList(
			Column.SortedIntegerType, Column.PositiveIntegerType, null, null,
			null, null, null, null, null, null);

	public static final List SINT_INT = makeColumnTypeList(
			Column.SortedIntegerType, Column.IntegerType, null, null, null,
			null, null, null, null, null);

	public static final List SINT_INT_INT_INT_INT = makeColumnTypeList(
			Column.PositiveIntegerType, Column.IntegerType, Column.IntegerType,
			Column.IntegerType, Column.IntegerType, null, null, null, null,
			null);

	public static final List SNMINT_INT_INT = makeColumnTypeList(
			Column.SortedNMIntegerType, Column.IntegerType, Column.IntegerType,
			null, null, null, null, null, null, null);

	public static final List SNMINT_PINT = makeColumnTypeList(
			Column.SortedNMIntegerType, Column.PositiveIntegerType, null, null,
			null, null, null, null, null, null);

	public static final List STRING = makeColumnTypeList(Column.StringType,
			null, null, null, null, null, null, null, null, null);

	public static final List STRING_IMAGE_INT_INT = makeColumnTypeList(
			Column.StringType, Column.ImageType, Column.IntegerType,
			Column.IntegerType, null, null, null, null, null, null);

	public static final List STRING_INT = makeColumnTypeList(Column.StringType,
			Column.IntegerType, null, null, null, null, null, null, null, null);

	public static final List STRING_INT_INT = makeColumnTypeList(
			Column.StringType, Column.IntegerType, Column.IntegerType, null,
			null, null, null, null, null, null);

	public static final List STRING_INT_INT_INT = makeColumnTypeList(
			Column.StringType, Column.IntegerType, Column.IntegerType,
			Column.IntegerType, null, null, null, null, null, null);

	public static final List STRING_SINT = makeColumnTypeList(
			Column.StringType, Column.SortedIntegerType, null, null, null,
			null, null, null, null, null);

	public static final List STRING_STRING_STRING_INT_INT_INT_INT = makeColumnTypeList(
			Column.StringType, Column.StringType, Column.StringType,
			Column.IntegerType, Column.IntegerType, Column.IntegerType,
			Column.IntegerType, null, null, null);

	public static final List STRING_STRING_STRING_STRING = makeColumnTypeList(
			Column.StringType, Column.StringType, Column.StringType,
			Column.StringType, null, null, null, null, null, null);

	public MyResultSet(DataInputStream s, List types) {
		nRows = readInt(s) - 1;
		// System.out.println(nRows + " rows");
		if (nRows >= 0) {
			int nCols = readInt(s);
			assert nCols == types.size() : nCols + " " + types.size() + " "
					+ nRows;
			columns = new Column[nCols];
			for (int i = 0; i < nCols; i++) {
				boolean sorted = false;
				boolean positive = false;
				Object type = types.get(i);
				try {
					if (type == Column.SortedIntegerType) {
						sorted = true;
						positive = true;
						columns[i] = new IntColumn(s, nRows, sorted, positive);
					} else if (type == Column.PositiveIntegerType) {
						positive = true;
						columns[i] = new IntColumn(s, nRows, sorted, positive);
					} else if (type == Column.IntegerType) {
						columns[i] = new IntColumn(s, nRows, sorted, positive);
					} else if (type == Column.SortedNMIntegerType) {
						sorted = true;
						columns[i] = new IntColumn(s, nRows, sorted, positive);
					} else if (type == Column.StringType) {
						columns[i] = new StringColumn(s, nRows);
					} else if (type == Column.DoubleType) {
						columns[i] = new DoubleColumn(s, nRows);
					} else if (type == Column.ImageType) {
						columns[i] = new BlobColumn(s, nRows);
					} else {
						assert false : "Unknown column type: " + type;
					}
				} catch (IOException e) {
					Util.err("While reading " + type + " column " + i + " of "
							+ types);
					for (int j = 0; j < i; j++) {
						Util.err(Util.valueOfDeep(columns[j].getValues()));
					}
					e.printStackTrace();
				}
			}
		}
	}

	public boolean absolute(int row) {
		// System.out.println("absolute " + row);
		if (row >= 0)
			currentRow = row;
		else
			currentRow = nRows + row + 1;
		return currentRow >= 1 && currentRow <= nRows;
	}

	public int getRow() {
		return currentRow;
	}

	public void afterLast() {
		currentRow = nRows + 1;
	}

	public void beforeFirst() {
		currentRow = 0;
	}

	public boolean next() {
		return ++currentRow <= nRows;
	}

	public void close() {
		// Only the garbage collector needs to know
	}

	public Object[] getValues(int columnIndex) {
		return columns[columnIndex - 1].getValues();
	}

	private void checkCurrentRowCol(int columnIndex) throws SQLException {
		if (currentRow < 1 || currentRow > nRows)
			throw new SQLException("Not on valid [1, " + nRows + "] row: "
					+ currentRow);
		if (columnIndex < 1 || columnIndex > columns.length)
			throw new SQLException("Invalid [1, " + columns.length
					+ "] column: " + columnIndex);
	}

	public String getString(int columnIndex) throws SQLException {
		checkCurrentRowCol(columnIndex);
		return columns[columnIndex - 1].getString(currentRow);
	}

	public int getInt(int columnIndex) throws SQLException {
		checkCurrentRowCol(columnIndex);
		return columns[columnIndex - 1].getInt(currentRow);
	}

	public double getDouble(int columnIndex) throws SQLException {
		checkCurrentRowCol(columnIndex);
		return columns[columnIndex - 1].getDouble(currentRow);
	}

	public InputStream getInputStream(int columnIndex) throws SQLException {
		checkCurrentRowCol(columnIndex);
		return columns[columnIndex - 1].getBlob(currentRow);
	}

	public String getString(String columnName) {
		return columns[findColumn(columnName) - 1].getString(currentRow);
	}

	public int getInt(String columnName) {
		return columns[findColumn(columnName) - 1].getInt(currentRow);
	}

	public int findColumn(String columnName) {
		// Util.print("findColumn " + columnName);
		// for (int i = 0; i < columns.length; i++) {
		// Util.print(i + " " + columns[i].name);
		// if (columns[i].name.equals(columnName))
		// assert false; return i;
		// }
		assert Util.ignore(columnName);
		assert false;
		return -1;
	}

	public Column getColumn(int columnIndex) {
		return columns[columnIndex];
	}

	public boolean first() {
		currentRow = 1;
		return nRows > 0;
	}

	// 00 xxxxxx
	// 01 xxx xxx
	// 10 xxxxxx,xxxxxxxx
	// 110 xxxxx,xxxxxxxx,xxxxxxxx
	// 111 xxxxx,xxxxxxxx,xxxxxxxx,xxxxxxxx
	static int readIntOrTwo(DataInputStream in, int[] second)
			throws IOException {
		int result = -1;
		second[0] = -1;
		int n = in.readUnsignedByte();
		// Util.print("riot " + n);
		if (n >= 64) {
			if (n >= 128) {
				if (n >= 192) {
					if (n >= 224) {
						// starts 111; read 4 bytes
						result = ((n & 31) << 24)
								+ (in.readUnsignedByte() << 16)
								+ (in.readUnsignedByte() << 8)
								+ in.readUnsignedByte();
					} else {
						// starts 110; read 3 bytes
						result = ((n & 31) << 16)
								+ (in.readUnsignedByte() << 8)
								+ in.readUnsignedByte();
					}
				} else {
					// starts 10; read 2 bytes
					result = ((n & 63) << 8) + in.readUnsignedByte();
				}
			} else {
				// starts 01; read two half-bytes
				result = (n & 56) >> 3;
				second[0] = n & 7;
			}
		} else {
			// starts 0; read 1 bytes
			result = n;
		}
		// System.out.println("readIntOrTwo " + result + " " + second[0]);
		return result;
	}

	// 0 xxxxxxx
	// 10 xxxxxx,xxxxxxxx
	// 110 xxxxx,xxxxxxxx,xxxxxxxx
	// 111 xxxxx,xxxxxxxx,xxxxxxxx,xxxxxxxx
	public static int readInt(DataInputStream in) {
		int result = -1;
		try {
			int n = in.readUnsignedByte();
			if (n >= 128) {
				if (n >= 192) {
					if (n >= 224) {
						// starts 111; read 4 bytes
						result = ((n & 31) << 24)
								+ (in.readUnsignedByte() << 16)
								+ (in.readUnsignedByte() << 8)
								+ in.readUnsignedByte();
					} else {
						// starts 110; read 3 bytes
						result = ((n & 31) << 16)
								+ (in.readUnsignedByte() << 8)
								+ in.readUnsignedByte();
					}
				} else {
					// starts 10; read 2 bytes
					result = ((n & 63) << 8) + in.readUnsignedByte();
				}
			} else {
				// starts 0; read 1 bytes
				result = n;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * @param in
	 * @return value read from in, or false if there's an error.
	 */
	public static boolean readBoolean(DataInputStream in) {
		boolean result = false;
		try {
			result = in.readBoolean();
			// Util.print("readDouble => " + result);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// String result = null;
		// int nChars = readInt(in) - 1;
		// if (nChars >= 0) {
		// try {
		// StringBuffer buf = new StringBuffer(nChars);
		// for (int i = 0; i < nChars; i++) {
		// buf.append(in.readChar());
		// }
		// result = buf.toString();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		return result;
	}

	public static double readDouble(DataInputStream in) {
		double result = Double.NaN;
		try {
			result = in.readDouble();
			// Util.print("readDouble => " + result);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// String result = null;
		// int nChars = readInt(in) - 1;
		// if (nChars >= 0) {
		// try {
		// StringBuffer buf = new StringBuffer(nChars);
		// for (int i = 0; i < nChars; i++) {
		// buf.append(in.readChar());
		// }
		// result = buf.toString();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		return result;
	}

	public static String readString(DataInputStream in) {
		String result = null;
		try {
			result = in.readUTF();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// String result = null;
		// int nChars = readInt(in) - 1;
		// if (nChars >= 0) {
		// try {
		// StringBuffer buf = new StringBuffer(nChars);
		// for (int i = 0; i < nChars; i++) {
		// buf.append(in.readChar());
		// }
		// result = buf.toString();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		return result;
	}

	static InputStream readBlob(DataInputStream s) {
		InputStream result = null;
		int nBytes = readInt(s) - 1;
		// Util.print("Blob bytes = " + nBytes);
		if (nBytes >= 0) {
			try {
				byte[] blob = new byte[nBytes];
				s.readFully(blob, 0, nBytes);
				// Util.print("\nreadBlob " + nBytes);
				result = new ByteArrayInputStream(blob);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return result;
	}

	public static abstract class Column {

		/**
		 * This column type contains integers from 0 to about 1,073,741,824
		 */
		public final static Object IntegerType = "INTEGER";

		public final static Object StringType = "STRING";

		public final static Object ImageType = "IMAGE";

		/**
		 * This column type contains integers from 1 to about 1,073,741,824 Each
		 * value must be at least 1 greater than previous value. Encode as n -
		 * prev - 1.
		 */
		public final static Object SortedIntegerType = "SORTED_INTEGER";

		/**
		 * This column type contains integers from 1 to about 1,073,741,824
		 */
		public final static Object PositiveIntegerType = "POSITIVE_INTEGER";

		/**
		 * This column type contains integers from 0 to about 1,073,741,824 each
		 * value must be at least as large as previous value. Encode as n -
		 * prev.
		 */
		public final static Object SortedNMIntegerType = "SORTED_NM_INTEGER";

		public final static Object DoubleType = "DOUBLE";

		// String name;

		// int type;

		protected abstract String getString(int row);

		protected abstract InputStream getBlob(int currentRow);

		protected abstract int getInt(int row);

		protected abstract double getDouble(int row);

		public Object[] getValues() {
			return null;
		}

	}

	public class StringColumn extends Column {
		private static final long serialVersionUID = 3352428345234393804L;

		// final static int type = StringType;

		private String[] values;

		public StringColumn(DataInputStream s, int nRows1) {
			values = new String[nRows1];
			for (int i = 0; i < nRows1; i++) {
				values[i] = readString(s);
			}
		}

		protected String getString(int row) {
			return values[row - 1];
		}

		protected int getInt(int row) {
			assert false;
			return Integer.parseInt(values[row - 1]);
		}

		protected InputStream getBlob(int row) {
			assert Util.ignore(row);
			assert false;
			return null;
		}

		protected double getDouble(int row) {
			assert false;
			return Double.parseDouble(values[row - 1]);
		}

		public Object[] getValues() {
			return (String[]) values.clone();
		}
	}

	class DoubleColumn extends Column {

		private static final long serialVersionUID = -512893701109549243L;

		// final static int type = DoubleType;

		private double[] values;

		public DoubleColumn(DataInputStream s, int nRows1) {
			values = new double[nRows1];
			for (int i = 0; i < nRows1; i++) {
				values[i] = readDouble(s);
			}
		}

		protected String getString(int row) {
			assert false;
			return Double.toString(values[row - 1]);
		}

		protected int getInt(int row) {
			assert false;
			return (int) values[row - 1];
		}

		protected InputStream getBlob(int row) {
			assert Util.ignore(row);
			assert false;
			return null;
		}

		protected double getDouble(int row) {
			return values[row - 1];
		}
	}

	class IntColumn extends Column {
		private static final long serialVersionUID = -241283930914273397L;

		// final static int type = IntegerType;

		private int[] values;

		public IntColumn(DataInputStream s, int nRows1, boolean sorted,
				boolean positive) throws IOException {
			// Util.print("IntColumn " + sorted + " " + positive + " " + nRows);
			values = new int[nRows1];
			int[] vals = new int[1];
			int prev = 0;
			for (int i = 0; i < nRows1; i++) {
				try {
					if (sorted) {
						prev += readIntOrTwo(s, vals);
						if (positive)
							prev++;
						values[i] = prev;
						if (vals[0] >= 0) {
							prev += vals[0];
							if (positive)
								prev++;
							values[++i] = prev;
						}
					} else if (positive) {
						values[i] = readIntOrTwo(s, vals) + 1;
						if (vals[0] >= 0) {
							values[++i] = vals[0] + 1;
						}
					} else {
						values[i] = readIntOrTwo(s, vals);
						if (vals[0] >= 0) {
							values[++i] = vals[0];
						}
					}
				} catch (IOException e) {
					Util.err("While reading integer from row " + i + " of "
							+ nRows);
					for (int j = 0; j <= i; j++) {
						Util.err("row " + j + " = " + values[j]);
					}
					throw (e);
				}
			}
		}

		protected String getString(int row) {
			assert false;
			return Integer.toString(values[row - 1]);
		}

		protected int getInt(int row) {
			return values[row - 1];
		}

		protected InputStream getBlob(int row) {
			assert Util.ignore(row);
			assert false;
			return null;
		}

		protected double getDouble(int row) {
			assert false;
			return values[row - 1];
		}

		public Object[] getValues() {
			Integer[] result = new Integer[values.length];
			for (int i = 0; i < values.length; i++) {
				result[i] = new Integer(values[i]);
			}
			return result;
		}
	}

	class BlobColumn extends Column {
		private static final long serialVersionUID = -4365032224127394378L;

		// final static int type = ImageType;

		private InputStream[] values;

		public BlobColumn(DataInputStream s, int nRows1) {
			values = new InputStream[nRows1];
			for (int i = 0; i < nRows1; i++) {
				values[i] = readBlob(s);
			}
		}

		protected String getString(int row) {
			assert Util.ignore(row);
			assert false;
			return null;
		}

		protected int getInt(int row) {
			assert Util.ignore(row);
			assert false;
			return -1;
		}

		protected InputStream getBlob(int row) {
			assert Util.ignore(row);
			return values[row - 1];
		}

		protected double getDouble(int row) {
			assert Util.ignore(row);
			assert false;
			return Double.NaN;
		}
	}

	public boolean wasNull() {
		// TODO Auto-generated method stub
		assert false;
		return false;
	}

	public boolean getBoolean(int arg0) {
		assert Util.ignore(arg0);
		// TODO Auto-generated method stub
		assert false;
		return false;
	}

	public byte getByte(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return 0;
	}

	public short getShort(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return 0;
	}

	public long getLong(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return 0;
	}

	public float getFloat(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return 0;
	}

	public BigDecimal getBigDecimal(int arg0, int arg1) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public byte[] getBytes(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Date getDate(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Time getTime(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Timestamp getTimestamp(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public InputStream getAsciiStream(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public InputStream getUnicodeStream(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public InputStream getBinaryStream(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public boolean getBoolean(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return false;
	}

	public byte getByte(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return 0;
	}

	public short getShort(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return 0;
	}

	public long getLong(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return 0;
	}

	public float getFloat(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return 0;
	}

	public double getDouble(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return 0;
	}

	public BigDecimal getBigDecimal(String arg0, int arg1) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public byte[] getBytes(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Date getDate(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Time getTime(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Timestamp getTimestamp(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public InputStream getAsciiStream(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public InputStream getUnicodeStream(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public InputStream getBinaryStream(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public SQLWarning getWarnings() {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public void clearWarnings() {
		// TODO Auto-generated method stub

	}

	public String getCursorName() {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public ResultSetMetaData getMetaData() {
		return new MyMetaData();
	}

	public Object getObject(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Object getObject(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Reader getCharacterStream(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Reader getCharacterStream(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public BigDecimal getBigDecimal(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public BigDecimal getBigDecimal(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public boolean isBeforeFirst() {
		// TODO Auto-generated method stub
		assert false;
		return false;
	}

	public boolean isAfterLast() {
		// TODO Auto-generated method stub
		assert false;
		return false;
	}

	public boolean isFirst() {
		// TODO Auto-generated method stub
		assert false;
		return false;
	}

	public boolean isLast() {
		// TODO Auto-generated method stub
		assert false;
		return false;
	}

	public boolean last() {
		currentRow = nRows;
		return nRows > 0;
	}

	public boolean relative(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return false;
	}

	public boolean previous() {
		// TODO Auto-generated method stub
		assert false;
		return false;
	}

	public void setFetchDirection(int arg0) {
		// TODO Auto-generated method stub

	}

	public int getFetchDirection() {
		// TODO Auto-generated method stub
		assert false;
		return 0;
	}

	public void setFetchSize(int arg0) {
		// TODO Auto-generated method stub

	}

	public int getFetchSize() {
		// TODO Auto-generated method stub
		assert false;
		return 0;
	}

	public int getType() {
		// TODO Auto-generated method stub
		assert false;
		return 0;
	}

	public int getConcurrency() {
		// TODO Auto-generated method stub
		assert false;
		return 0;
	}

	public boolean rowUpdated() {
		// TODO Auto-generated method stub
		assert false;
		return false;
	}

	public boolean rowInserted() {
		// TODO Auto-generated method stub
		assert false;
		return false;
	}

	public boolean rowDeleted() {
		// TODO Auto-generated method stub
		assert false;
		return false;
	}

	public void updateNull(int arg0) {
		// TODO Auto-generated method stub

	}

	public void updateBoolean(int arg0, boolean arg1) {
		// TODO Auto-generated method stub

	}

	public void updateByte(int arg0, byte arg1) {
		// TODO Auto-generated method stub

	}

	public void updateShort(int arg0, short arg1) {
		// TODO Auto-generated method stub

	}

	public void updateInt(int arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	public void updateLong(int arg0, long arg1) {
		// TODO Auto-generated method stub

	}

	public void updateFloat(int arg0, float arg1) {
		// TODO Auto-generated method stub

	}

	public void updateDouble(int arg0, double arg1) {
		// TODO Auto-generated method stub

	}

	public void updateBigDecimal(int arg0, BigDecimal arg1) {
		// TODO Auto-generated method stub

	}

	public void updateString(int arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	public void updateBytes(int arg0, byte[] arg1) {
		// TODO Auto-generated method stub

	}

	public void updateDate(int arg0, Date arg1) {
		// TODO Auto-generated method stub

	}

	public void updateTime(int arg0, Time arg1) {
		// TODO Auto-generated method stub

	}

	public void updateTimestamp(int arg0, Timestamp arg1) {
		// TODO Auto-generated method stub

	}

	public void updateAsciiStream(int arg0, InputStream arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	public void updateBinaryStream(int arg0, InputStream arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	public void updateCharacterStream(int arg0, Reader arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	public void updateObject(int arg0, Object arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	public void updateObject(int arg0, Object arg1) {
		// TODO Auto-generated method stub

	}

	public void updateNull(String arg0) {
		// TODO Auto-generated method stub

	}

	public void updateBoolean(String arg0, boolean arg1) {
		// TODO Auto-generated method stub

	}

	public void updateByte(String arg0, byte arg1) {
		// TODO Auto-generated method stub

	}

	public void updateShort(String arg0, short arg1) {
		// TODO Auto-generated method stub

	}

	public void updateInt(String arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	public void updateLong(String arg0, long arg1) {
		// TODO Auto-generated method stub

	}

	public void updateFloat(String arg0, float arg1) {
		// TODO Auto-generated method stub

	}

	public void updateDouble(String arg0, double arg1) {
		// TODO Auto-generated method stub

	}

	public void updateBigDecimal(String arg0, BigDecimal arg1) {
		// TODO Auto-generated method stub

	}

	public void updateString(String arg0, String arg1) {
		// TODO Auto-generated method stub

	}

	public void updateBytes(String arg0, byte[] arg1) {
		// TODO Auto-generated method stub

	}

	public void updateDate(String arg0, Date arg1) {
		// TODO Auto-generated method stub

	}

	public void updateTime(String arg0, Time arg1) {
		// TODO Auto-generated method stub

	}

	public void updateTimestamp(String arg0, Timestamp arg1) {
		// TODO Auto-generated method stub

	}

	public void updateAsciiStream(String arg0, InputStream arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	public void updateBinaryStream(String arg0, InputStream arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	public void updateCharacterStream(String arg0, Reader arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	public void updateObject(String arg0, Object arg1, int arg2) {
		// TODO Auto-generated method stub

	}

	public void updateObject(String arg0, Object arg1) {
		// TODO Auto-generated method stub

	}

	public void insertRow() {
		// TODO Auto-generated method stub

	}

	public void updateRow() {
		// TODO Auto-generated method stub

	}

	public void deleteRow() {
		// TODO Auto-generated method stub

	}

	public void refreshRow() {
		// TODO Auto-generated method stub

	}

	public void cancelRowUpdates() {
		// TODO Auto-generated method stub

	}

	public void moveToInsertRow() {
		// TODO Auto-generated method stub

	}

	public void moveToCurrentRow() {
		// TODO Auto-generated method stub

	}

	public Statement getStatement() {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Object getObject(int arg0, Map arg1) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Ref getRef(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Blob getBlob(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Clob getClob(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Array getArray(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Object getObject(String arg0, Map arg1) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Ref getRef(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Blob getBlob(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Clob getClob(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Array getArray(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Date getDate(int arg0, Calendar arg1) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Date getDate(String arg0, Calendar arg1) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Time getTime(int arg0, Calendar arg1) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Time getTime(String arg0, Calendar arg1) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Timestamp getTimestamp(int arg0, Calendar arg1) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public Timestamp getTimestamp(String arg0, Calendar arg1) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public URL getURL(int arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public URL getURL(String arg0) {
		// TODO Auto-generated method stub
		assert false;
		return null;
	}

	public void updateRef(int arg0, Ref arg1) {
		// TODO Auto-generated method stub

	}

	public void updateRef(String arg0, Ref arg1) {
		// TODO Auto-generated method stub

	}

	public void updateBlob(int arg0, Blob arg1) {
		// TODO Auto-generated method stub

	}

	public void updateBlob(String arg0, Blob arg1) {
		// TODO Auto-generated method stub

	}

	public void updateClob(int arg0, Clob arg1) {
		// TODO Auto-generated method stub

	}

	public void updateClob(String arg0, Clob arg1) {
		// TODO Auto-generated method stub

	}

	public void updateArray(int arg0, Array arg1) {
		// TODO Auto-generated method stub

	}

	public void updateArray(String arg0, Array arg1) {
		// TODO Auto-generated method stub

	}

	public static int nRows(ResultSet rs) throws SQLException {
		int row = rs.getRow();
		rs.last();
		int result = rs.getRow();
		resetRow(rs, row);
		return result;
	}

	static void resetRow(ResultSet rs, int row) throws SQLException {
		if (row == 0)
			// absolute(0) may barf
			rs.beforeFirst();
		else
			rs.absolute(row);
	}

	public String valueOfDeep(int maxRows) {
		List types = new LinkedList();
		for (int i = 0; i < columns.length; i++) {
			Column column = columns[i];
			Object type = null;
			if (column instanceof IntColumn)
				type = Column.IntegerType;
			else if (column instanceof BlobColumn)
				type = Column.ImageType;
			else if (column instanceof StringColumn)
				type = Column.ImageType;
			else if (column instanceof StringColumn)
				type = Column.StringType;
			types.add(type);
		}
		return valueOfDeep(this, types, maxRows);
	}

	public static String valueOfDeep(ResultSet result, List types, int maxRows) {
		StringBuffer buf = new StringBuffer();
		try {
			int nRows = MyResultSet.nRows(result);
			int nCols = types.size();
			// buf.append(nRows).append(" rows, ").append(nCols).append(
			// " cols in result set");
			int row = result.getRow();
			result.beforeFirst();
			for (int i = 0; i < nRows && (maxRows < 0 || i < maxRows); i++) {
				result.next();
				buf.append("\n");
				for (int j = 0; j < nCols; j++) {
					Object type = types.get(j);
					if (type == MyResultSet.Column.IntegerType
							|| type == MyResultSet.Column.SortedIntegerType
							|| type == MyResultSet.Column.SortedNMIntegerType
							|| type == MyResultSet.Column.PositiveIntegerType)
						buf.append(result.getInt(j + 1)).append("\t");
					else if (type == MyResultSet.Column.StringType)
						buf.append(result.getString(j + 1)).append("\t");
					else if (type == MyResultSet.Column.DoubleType)
						buf.append(result.getDouble(j + 1)).append("\t");
					else if (type == MyResultSet.Column.ImageType)
						buf.append("<image>\t");
					else
						assert false : "Unknown ColumnType: " + type;
				}
			}
			if (nRows > maxRows) {
				buf.append("\n... ").append(nRows - maxRows).append(
						" more records");
			}
			resetRow(result, row);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return buf.toString();
	}

	class MyMetaData implements ResultSetMetaData {

		public String getCatalogName(int column) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public String getColumnClassName(int column) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public int getColumnCount() throws SQLException {
			return MyResultSet.this.getColumnCount();
		}

		public int getColumnDisplaySize(int column) throws SQLException {
			// TODO Auto-generated method stub
			return 0;
		}

		public String getColumnLabel(int column) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public String getColumnName(int column) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public int getColumnType(int column) throws SQLException {
			// TODO Auto-generated method stub
			return 0;
		}

		public String getColumnTypeName(int column) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public int getPrecision(int column) throws SQLException {
			// TODO Auto-generated method stub
			return 0;
		}

		public int getScale(int column) throws SQLException {
			// TODO Auto-generated method stub
			return 0;
		}

		public String getSchemaName(int column) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public String getTableName(int column) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

		public boolean isAutoIncrement(int column) throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isCaseSensitive(int column) throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isCurrency(int column) throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isDefinitelyWritable(int column) throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}

		public int isNullable(int column) throws SQLException {
			// TODO Auto-generated method stub
			return 0;
		}

		public boolean isReadOnly(int column) throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isSearchable(int column) throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isSigned(int column) throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isWritable(int column) throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean isWrapperFor(Class arg0) throws SQLException {
			// TODO Auto-generated method stub
			return false;
		}

		public Object unwrap(Class arg0) throws SQLException {
			// TODO Auto-generated method stub
			return null;
		}

	}

	public int getHoldability() throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	public Reader getNCharacterStream(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public Reader getNCharacterStream(String arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public NClob getNClob(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public NClob getNClob(String arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getNString(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getNString(String arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public RowId getRowId(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public RowId getRowId(String arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public SQLXML getSQLXML(int arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public SQLXML getSQLXML(String arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isClosed() throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public void updateAsciiStream(int arg0, InputStream arg1)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateAsciiStream(String arg0, InputStream arg1)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateAsciiStream(int arg0, InputStream arg1, long arg2)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateAsciiStream(String arg0, InputStream arg1, long arg2)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateBinaryStream(int arg0, InputStream arg1)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateBinaryStream(String arg0, InputStream arg1)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateBinaryStream(int arg0, InputStream arg1, long arg2)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateBinaryStream(String arg0, InputStream arg1, long arg2)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateBlob(int arg0, InputStream arg1) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateBlob(String arg0, InputStream arg1) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateBlob(int arg0, InputStream arg1, long arg2)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateBlob(String arg0, InputStream arg1, long arg2)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateCharacterStream(int arg0, Reader arg1)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateCharacterStream(String arg0, Reader arg1)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateCharacterStream(int arg0, Reader arg1, long arg2)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateCharacterStream(String arg0, Reader arg1, long arg2)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateClob(int arg0, Reader arg1) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateClob(String arg0, Reader arg1) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateClob(int arg0, Reader arg1, long arg2)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateClob(String arg0, Reader arg1, long arg2)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateNCharacterStream(int arg0, Reader arg1)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateNCharacterStream(String arg0, Reader arg1)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateNCharacterStream(int arg0, Reader arg1, long arg2)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateNCharacterStream(String arg0, Reader arg1, long arg2)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateNClob(int arg0, NClob arg1) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateNClob(String arg0, NClob arg1) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateNClob(int arg0, Reader arg1) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateNClob(String arg0, Reader arg1) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateNClob(int arg0, Reader arg1, long arg2)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateNClob(String arg0, Reader arg1, long arg2)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateNString(int arg0, String arg1) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateNString(String arg0, String arg1) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateRowId(int arg0, RowId arg1) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateRowId(String arg0, RowId arg1) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateSQLXML(int arg0, SQLXML arg1) throws SQLException {
		// TODO Auto-generated method stub

	}

	public void updateSQLXML(String arg0, SQLXML arg1) throws SQLException {
		// TODO Auto-generated method stub

	}

	public boolean isWrapperFor(Class arg0) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	public Object unwrap(Class arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

}