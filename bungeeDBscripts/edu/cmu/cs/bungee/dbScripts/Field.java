package edu.cmu.cs.bungee.dbScripts;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * A field maps from a source string to a database insert call.
 * 
 */
class Field {
	String name;

	static final Field IGNORABLE_FIELD = new Field("Ignorable Field");

	Field(String _name) {
		name = _name;
	}

	@SuppressWarnings("unused")
	boolean insert(String value, Populate handler) throws SQLException {
		assert false : value;
		return false;
	}

	/**
	 * After readData completes, this is called for each inserted field.
	 * 
	 * @throws SQLException
	 */
	@SuppressWarnings("unused")
	void cleanUp(Database db) throws SQLException {
		// override this
	}
}

class Attribute extends Field {

	// static Hashtable<String, Attribute> fieldCache = new Hashtable<String,
	// Attribute>();

	static Attribute getAttribute(String name1) {
		// Attribute result = fieldCache.get(name1);
		// if (result == null) {
		// result = new Attribute(name1);
		// }
		return new Attribute(name1);
	}

	Attribute(String _column) {
		super(_column);
	}

	@Override
	boolean insert(String value, Populate handler) throws SQLException {
		boolean result = value != null;
		if (result) {
			String oldValue = handler.getAttribute(name);
			if (oldValue != null) {
				value = oldValue + "\n" + value;
			}
			handler.insertAttribute(this, value);
		}
		return true;
	}

	@Override
	public String toString() {
		return "<Attribute " + name + ">";
	}
}

class FunctionalAttribute extends Attribute {
	boolean noWarn;

	static FunctionalAttribute getAttribute(String name, boolean _noWarn) {
		// FunctionalAttribute result = (FunctionalAttribute)
		// fieldCache.get(name);
		// if (result == null) {
		// result = new FunctionalAttribute(name, _noWarn);
		// }
		return new FunctionalAttribute(name, _noWarn);
	}

	FunctionalAttribute(String _column, boolean _noWarn) {
		super(_column);
		noWarn = _noWarn;
	}

	@Override
	boolean insert(String value, Populate handler) throws SQLException {
		assert value != null;
		String oldValue = handler.getAttribute(name);
		if (oldValue != null) {
			if (!noWarn)
				Util.err("Multiple values for " + name + ": " + oldValue + " "
						+ value);

			// If there's a clash, go with the first
			// value. This is the right thing for
			// multiple MARC 856 fields, at least,
			// according to Caroline Arms.
			return false;
		}
		handler.insertAttribute(this, value);
		return true;
	}
}

class Facet extends Field {

	FacetValueParser parser;

	/**
	 * cleanUp will set raw_facet.sort to the concatenation of all groups
	 * matched by this pattern.
	 */
	String sortPattern = null;

	// private static Hashtable<String, Facet> fieldCache = new
	// Hashtable<String, Facet>();

	private Facet(String name1, FacetValueParser _parser) {
		super(name1);
		parser = _parser;
	}

	static Facet getParsingFacet(String name, FacetValueParser parser) {
		// Facet result = fieldCache.get(name);
		// if (result == null) {
		// result = new Facet(name, parser);
		// } else {
		// assert result.parser == parser;
		// }
		return new Facet(name, parser);
	}

	static Facet getTimeFacet(String name) {
		Facet facet = getParsingFacet(name, TimeFacetValueParser.getInstance());
		// facet.sortPattern =
		// "(?:\\d+:\\d{2}:(\\d{2}))|(?:\\d+:(\\d{2}):00-\\d+:\\d{2}:59)|(?:(\\d+):00-\\d+:59)"
		// ;
		facet.sortPattern = "(?:\\d+:\\d{2}:(\\d{2}))|(?:\\d+:(\\d{2}):ss)|(?:(\\d+):mm:ss)";
		return facet;
	}

	static final String dateSortPattern = "(?:\\d{1,2}/(\\d{1,2})/\\d{4})|(?:(\\d{1,2})/\\d{4})|(?:(\\d+)(?:s|(?:.*century))?)";

	static Facet getDateFacet(String name) {
		Facet facet = getParsingFacet(name, DateFacetValueParser.getInstance());
		facet.sortPattern = dateSortPattern;
		return facet;
	}

	static Facet getGenericFacet(String name) {
		return getParsingFacet(name, GenericFacetValueParser.getInstance());
	}

	static Facet getPreparsedFacet(String name) {
		return getParsingFacet(name, PreparsedFacetValueParser.getInstance());
	}	

	static Facet getDate045Facet(String name) {
		Facet facet = getParsingFacet(name, Date045FacetValueParser
				.getInstance());
		facet.sortPattern = dateSortPattern;
		return facet;
	}

	static Facet getNumericFacet(String name) {
		Facet facet = getParsingFacet(name, GenericFacetValueParser
				.getInstance());
		facet.sortPattern = "(\\d+)";
		return facet;
	}

	static Facet getPlaceFacet(String name) {
		return getParsingFacet(name, PlaceFacetValueParser.getInstance());
	}

	static Facet getPlace043Facet(String name) {
		return getParsingFacet(name, Place043FacetValueParser.getInstance());
	}

	static Facet getSubjectFacet(String name) {
		return getParsingFacet(name, SubjectFacetValueParser.getInstance());
	}

	@Override
	boolean insert(String value, Populate handler) throws SQLException {
		String[][] facets = parser.parse(value, handler);
//		if (facets.length > 0 && facets[0].length > 0
//				&& "07".equals(facets[0][0])) {
//			Util.print(this + " " + value + " " + Util.valueOfDeep(facets));
//		}
		for (int i = 0; i < facets.length; i++) {
			List<String> path = new ArrayList<String>(facets[i].length + 1);
			path.add(name);
			for (int j = 0; j < facets[i].length; j++) {
				path.add(j + 1, facets[i][j]);
			}
			handler.insertFacet(this, path);
		}
		return facets.length > 0;
	}

	@Override
	void cleanUp(Database db) throws SQLException {
		if (sortPattern != null) {
			Pattern pattern = Pattern.compile(sortPattern);
			setSort(db.getFacetType(name), pattern, db);
		}
	}

	void setSort(int facet, Pattern pattern, Database db) throws SQLException {
		if (facet > 0) {
			String facetName = db.getName(facet);
			Matcher matcher = pattern.matcher(facetName);
			if (matcher.matches()) {
				StringBuffer sort = new StringBuffer();
				for (int i = 0; i < matcher.groupCount(); i++) {
					String group = matcher.group(i + 1);
					// Util.print(i + " " + group);
					if (group != null)
						sort.append(group);
				}
				// Util.print(facetName + " " + sort);
				PreparedStatement ps = db
						.lookupPS("UPDATE raw_facet SET sort = ? WHERE name = ?");
				ps.setString(1, sort.toString());
				ps.setString(2, facetName);
				db.db(ps, "Set sort");
			}
		}
		int[] grandChildren = db.getChildIDs(facet);
		for (int i = 0; i < grandChildren.length; i++) {
			setSort(grandChildren[i], pattern, db);
		}
	}

	@Override
	public String toString() {
		return "<Facet " + name + ">";
	}
}
