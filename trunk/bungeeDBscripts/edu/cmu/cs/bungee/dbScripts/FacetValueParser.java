package edu.cmu.cs.bungee.dbScripts;

import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * Maps source string into a list of tags to add to the current item,
 * represented as a list of strings {<child of facet type>, <grandcild>, ...}
 * 
 * A ParseOAIHandler is passed in so that the parser can get at resources like
 * lists of named entities.
 * 
 */
interface FacetValueParser {

	/**
	 * The ancestor facets must not include the facet type; this is encoded in
	 * the field.
	 * 
	 * @param value
	 *            raw string from XML file
	 * @return [<ancestor facets alternetive 1>, ...]
	 */
	String[][] parse(String value, Populate handler);
}

class GenericFacetValueParser implements FacetValueParser {

	private static final GenericFacetValueParser self = new GenericFacetValueParser();

	static GenericFacetValueParser getInstance() {
		return self;
	}

	public String[][] parse(String value, Populate handler) {
		if (value == null)
			return new String[0][];
		String[][] result = { { value } };
		return result;
	}

	static String trim(String value) {
		// Get rid of any characters before the first word
		// String result = value.replaceAll("\\A\\W*", "");
		String result = value.replaceAll("\\A.*?\\b", "");

		// Get rid of any characters after the last word (but retain ')')
		// result = result.replaceAll("[\\W&&[^\\)]]*\\z", "");
		result = result.replaceAll("\\b(?:[^\\)]\\B)*\\z", "");
		// if (!result.equals(value))
		// Util.print(value + " => " + result);
		return result;
	}
}

/**
 * parse '5:51:52' or '123' (as seconds) and return ["5:mm:ss", "5:51:ss",
 * "5:51:52"]
 * 
 */
class TimeFacetValueParser implements FacetValueParser {

	private static final TimeFacetValueParser self = new TimeFacetValueParser();

	static TimeFacetValueParser getInstance() {
		return self;
	}

	public String[][] parse(String value, Populate _handler) {
		String[][] result = { { value } };

		// This is the format we generate
		Pattern p = Pattern
				.compile("(\\d+):((?:mm)|(?:\\d{2})):((?:ss)|(?:\\d{2}))");
		Matcher m = p.matcher(value);
		if (m.find()) {
			String hour = m.group(1);
			String minute = m.group(2);
			String second = m.group(3);
			String[][] x = { { hour + ":mm:ss", hour + ":" + minute + ":ss",
					hour + ":" + minute + ":" + second } };
			result = x;
		}

		// Integer interpreted as seconds
		p = Pattern.compile("\\d+");
		m = p.matcher(value);
		if (m.matches()) {
			int second = Integer.parseInt(value);
			int hour = second / 60 / 60;
			second -= hour * 60 * 60;
			int minute = second / 60;
			second -= minute * 60;
			// String hourName = String.valueOf(hour);
			String minuteName = String.valueOf(minute);
			if (minuteName.length() == 1)
				minuteName = "0" + minuteName;
			String secondName = String.valueOf(second);
			if (secondName.length() == 1)
				secondName = "0" + secondName;
			String[][] x = { { hour + ":mm:ss",
					hour + ":" + minuteName + ":ss",
					hour + ":" + minuteName + ":" + secondName } };
			result = x;
		}
		return result;
	}
}

/**
 * parse a date and return ["20th century", "1990s", "1996", "12/1996",
 * "12/31/1996"]
 * 
 */
class DateFacetValueParser implements FacetValueParser {

	private static final DateFacetValueParser self = new DateFacetValueParser();

	static DateFacetValueParser getInstance() {
		return self;
	}

	// String rawValue;
	// ParseOAIhandler handler;

	public String[][] parse(String value, Populate _handler) {
		// rawValue = value;
		// handler = _handler;
		String trimmedValue = trimDate(value);
		String[][] result = { hierDateValues(trimmedValue) };
		// Util.print("parse date " + value + "\n" + trimmedValue);
		return result;
	}

	private final static String[] months = { "jan", "feb", "mar", "apr", "may",
			"jun", "jul", "aug", "sep", "oct", "nov", "dec", "jany", "feby",
			"mar", "apl", "may", "jun", "jul", "augt", "sepr", "octr", "novr",
			"decr", "january", "february", "march", "april", "may", "june",
			"july", "august", "september", "october", "november", "december",
			"sept" };

	private int month(String monthName) {
		int month = Util.member(months, monthName.toLowerCase());
		if (month == 36)
			month = 9;
		else if (month >= 0)
			month = 1 + month % 12;
		return month;
	}

	private String trimDate(final String value) {
		// Get rid of prefixes in front of the first digit: 'ca', 'c', '(c)',
		// '(C)', 'approximately', etc.
		// possibly followed by '.' and whitespace
		String result = value
				.replaceAll(
						"\\b(?:ca|c|\\([cC]\\)|approximately|between|before|after|photographed|published)\\.?\\s*(\\d)",
						"$1");

		// Get rid of (?) suffixes
		result = result.replaceAll("\\(\\?\\)", "");
		result = result.replaceAll("\\d\\?", "");

		// '[19]75' => '1975', '19]75' => '1975'
		result = result.replaceAll("\\[((?:17)|(?:18)|(?:19)|(?:20))\\]", "$1");
		result = result.replaceAll("\\b((?:17)|(?:18)|(?:19)|(?:20))\\]", "$1");
		result = result.replaceAll("19\\]", "19");

		// 19-- => '20th Century'
		Pattern p = Pattern.compile("\\b(\\d{2})--\\??");
		Matcher m = p.matcher(result);
		while (m.find()) {
			result = century(m.group(1));
			m.reset(result);
		}

		// '1920's' => '1920s'
		result = result.replaceAll("\\b(\\d{4})'s\\z", "$1s");

		// '1942/1945' => '1942-1945'
		result = result.replaceAll("\\b(\\d{4})/(\\d{4})\\z", "$1-$2");

		// '1945-4-15' => '4/15/1945'
		result = result.replaceAll("\\b(\\d{4})-(\\d{1,2})-(\\d{1,2})\\z",
				"$2/$3/$1");

		// '1945-6' => '1945-1946'
		result = result.replaceAll("\\b(\\d{3})(\\d)-(\\d)\\z", "$1$2-$1$3");
		// '1945-46' => '1945-1946'
		result = result.replaceAll("\\b(\\d{2})(\\d{2})-(\\d{2})\\z",
				"$1$2-$1$3");

		// '194[5]' => '1945'
		result = result.replaceAll("\\b(\\d{3})\\[(\\d)\\]", "$1$2");

		// '194-?', '194-' => '1940s'
		result = result.replaceAll("\\b(\\d{3})-\\??", "$10s");

		// '1956 or 1966' => 1956-1966
		p = Pattern.compile("\\b(\\d{4})\\s*(?:\\W|to|and|adn|or)\\s*(\\d{4})");
		m = p.matcher(result);
		if (m.find()) {
			String start = m.group(1);
			String end = m.group(2);
			if (end.compareTo(start) < 0) {
				String s = end;
				end = start;
				start = s;
			}
			result = start + "-" + end;
		}

		p = Pattern.compile("(\\d{4})\\s*([a-zA-Z]{3,})\\.?\\s*(\\d{0,2})");
		m = p.matcher(result);
		while (m.find()) {
			String monthName = m.group(2);
			int month = month(monthName);
			if (month > 0) {
				String day = m.group(3);
				if (day.length() == 0)
					result = month + "/" + m.group(1);
				else
					result = month + "/" + day + "/" + m.group(1);
				m.reset(result);
			}
		}

		p = Pattern
				.compile("\\b(\\d{0,2})\\s*([a-zA-Z]{3,})(?:\\.|,)?\\s*(\\d{4})\\z");
		m = p.matcher(result);
		while (m.find()) {
			String monthName = m.group(2);
			int month = month(monthName);
			if (month > 0) {
				String day = m.group(1);
				String year = m.group(3);
				if (day.length() == 0)
					result = month + "/" + year;
				else
					result = month + "/" + day + "/" + year;
				m.reset(result);
				// Util.print(value + " " + result);
			}
		}

		// 'Nov. 20, 1908' => '11/20/1908'
		p = Pattern
		// .compile(
				// "\\b\\s*([a-zA-Z]{3,})(?:\\.|,)?\\s*(\\d{0,2})[a-z]{0,2},?\\s*(\\d{4})\\z"
				// );
				// .compile(
				// "\\b([a-zA-Z]{3,})(?:\\.|,)?\\s*(\\d{0,2})[a-z]{0,2},?\\s*(\\d{4})\\z"
				// );
				.compile("\\b\\s*([a-zA-Z]{3,})(?:\\.|,)?\\s*(\\d{0,2})[a-z]{0,2},?\\s*(\\d{4})\\D");
		m = p.matcher(result);
		while (m.find()) {
			String monthName = m.group(1);
			int month = month(monthName);
			if (month > 0) {
				String day = m.group(2);
				String year = m.group(3);
				if (day.length() == 0)
					result = month + "/" + year;
				else
					result = month + "/" + day + "/" + year;
				m.reset(result);
				// Util.print(value + " " + result);
			}
		}

		// 'foo, 1945' => '1945'
		result = result.replaceAll("\\b\\D+,(\\s*\\d{4})", "$1");

		// 2005-10-22 => 10/22/2005
		p = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
		m = p.matcher(result);
		if (m.find()) {
			result = trimLeadingZeros(m.group(2)) + "/"
					+ trimLeadingZeros(m.group(3)) + "/" + m.group(1);
		}

		// for Chartres
		p = Pattern.compile("\\b(\\d{4})(\\d{2})(\\d{2})\\z");
		m = p.matcher(result);
		if (m.find()) {
			String year = m.group(1);
			String month = m.group(2);
			String day = m.group(3);
			if (Integer.parseInt(month) <= 12 && Integer.parseInt(day) <= 31)
				result = year;
		}

		// if (!value.equals(result))
		// Util.print(value + " => " + result);
		result = GenericFacetValueParser.trim(result);

		return result;
	}

	static String trimLeadingZeros(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) != '0') {
				if (i == 0)
					return s;
				else
					return s.substring(i);
			}
		}
		return "0";
	}

	static String century(String prefix) {
		try {
			String suffix = "th century";
			int n = Integer.parseInt(prefix) + 1;
			if (n < 4 || n > 20) {
				switch (n % 10) {
				case 1:
					suffix = "st century";
					break;
				case 2:
					suffix = "nd century";
					break;
				case 3:
					suffix = "rd century";
					break;
				}
			}
			return n + suffix;
		} catch (NumberFormatException e) {
			Util.err("NumberFormatException for century "+prefix);
//			e.printStackTrace();
			return prefix;
		}
	}

	private static final Pattern yearPattern = Pattern.compile("\\d{4}");

	private static boolean isYear(String s) {
		return yearPattern.matcher(s).matches();
	}

	/**
	 * @param value
	 *            arbitrary string representing a date
	 * @return ["20th century", "1990s", "1996", "12/1996", "12/31/1996"]
	 */
	static String[] hierDateValues(String value) {
		String[] result = { value };
		String parentName = null;
		// Split 5/20/1955-5/20/1956
		String[] date = value.split("-");
		if (date.length == 1) {
			// Split month/day/year
			date = value.split("/");
			if (date.length == 1) {
				if (value.length() == 4) {
					parentName = value.substring(0, 3) + "0s";
				} else if (value.length() == 5 && value.endsWith("0s")) {
					try {
						parentName = century(value.substring(0, 2));
					} catch (java.lang.NumberFormatException e) {
						Util.err("NumberFormatException for date "+value);
//						e.printStackTrace();
					}
				}
			} else {
				String year = date[date.length - 1];
				if (year.length() == 2) {
					year = "19" + year;
				}
				if (year.length() == 4) {
					if (date.length == 2)
						parentName = year;
					else if (date.length == 3)
						parentName = date[0] + "/" + year;
				}
			}
		} else if (date.length == 2 && isYear(date[0]) && isYear(date[1])) {
			if (date[0].substring(0, 3).equals(date[1].substring(0, 3))) {
				parentName = date[0].substring(0, 3) + "0s";
			} else if (date[0].substring(0, 2).equals(date[1].substring(0, 2))) {
				parentName = century(date[0].substring(0, 2));
			} else {
				int century1 = Integer.parseInt(date[0].substring(0, 2));
				int century2 = Integer.parseInt(date[1].substring(0, 2));
				if (century1 + 1 == century2) {
					parentName = century(date[0].substring(0, 2)).substring(0,
							4)
							+ "-" + century(date[1].substring(0, 2));
				}
			}
		}
		if (parentName != null) {
			result = (String[]) Util.append(hierDateValues(parentName), result,
					String.class);
		}
		// Util.print("hierDateValues " + value + " => " +
		// Util.valueOfDeep(result));
		return result;
	}

}

class Date045FacetValueParser implements FacetValueParser {

	private static final Date045FacetValueParser self = new Date045FacetValueParser();

	static Date045FacetValueParser getInstance() {
		return self;
	}

	public String[][] parse(String value, Populate handler) {
		String[][] result = { { value } };
		// Util.print("045 code = '" + value + "'");
		boolean OK = false;
		if (value.length() == 4) {
			// see http://www.oclc.org/bibformats/en/0xx/045.shtm
			String lowRange = code045(value.substring(0, 2));
			String hiRange = code045(value.substring(2, 4));
			if (lowRange != null && hiRange != null) {
				OK = true;
				if (!value.substring(0, 2).equals(value.substring(2, 4))) {
					lowRange = lowRange.split("-")[0] + "-"
							+ hiRange.split("-")[1];
				}
				value = lowRange;
			}
		} else {
			// see
			// http://www.loc.gov/marc/bibliographic/ecbdnumb.html#mrcb045
			Pattern p = Pattern
					.compile("\\b([cd])(\\d{4})(\\d{2})?(\\d{2})?(\\d{2})?\\z");
			Matcher m = p.matcher(value);
			if (m.find()) {
				String era = m.group(1);
				String year = m.group(2);
				String month = m.group(3);
				String day = m.group(4);
				String newValue = (day != null) ? month + "/" + day + "/"
						+ year : (month != null) ? month + "/" + year : year;
				if (era.charAt(0) == 'd') {
					value = newValue;
					OK = true;
				} else if (era.charAt(0) == 'c') {
					value = newValue + " B.C.";
					OK = true;
				}
			}
			if (OK)
				result[0] = DateFacetValueParser.hierDateValues(value);
			else {
				Util.err("Bad 045 Date Code " + value);
				result[0] = new String[0];
			}
		}
		return result;
	}

	private String code045(String code) {
		String result = null;
		int code1 = code.charAt(0) - 'a';
		int code2 = code.charAt(1) - '0';
		if (code.length() == 2 && code1 >= 0 && code1 <= 25
				&& (code.charAt(1) == '-' || (code2 >= 0 && code2 <= 9))) {
			if (code1 == 0) {
				assert code2 == 0;
				result = "before 2999 B.C.";
			} else if (code1 > 0 && code1 < 4) {
				int digit1 = 2 - (code1 - 1);
				int digit2 = 9 - code2;
				int end = 100 * (digit1 * 10 + digit2);
				int start = end + 99;
				result = start + "-" + end;
			} else if (code.charAt(1) == '-') {
				int digit1 = code1 - 4;
				int start = digit1 * 100;
				int end = start + 99;
				result = start + "-" + end;
			} else {
				int digit1 = code1 - 4;
				int digit2 = code2;
				int start = 10 * (digit1 * 10 + digit2);
				int end = start + 9;
				result = start + "-" + end;
			}
		}
		// Util.print("code 045 " + code + " => " + result);
		return result;
	}

}

class PlaceFacetValueParser implements FacetValueParser {

	private static final PlaceFacetValueParser self = new PlaceFacetValueParser();

	private static Hashtable<String, String> placeAbbrevs;

	// private ParseOAIhandler handler;

	/**
	 * These are use for place fields only
	 */
	private static final String[] abbrevs = { "Alabama;al:ala", "Alaska;ak",
			"American Samoa;as:a.s", "Arizona;az:ariz", "Arkansas;ar:ark",
			"British Columbia;bc:b.c", "California;ca:cal:calif",
			"Colorado;co:col:colo", "Connecticut;ct:conn", "Delaware;de:del",
			"Federated States Of Micronesia;fm", "Florida;fl:fla",
			"Georgia;ga", "Guam;gu", "Hawaii;hi", "Idaho;id",
			"Illinois;il:ill", "Indiana;in:ind", "Iowa;ia", "Kansas;ks:kan",
			"Kentucky;ky", "Louisiana;la", "Maine;me", "Marshall Islands;mh",
			"Maryland;md", "Massachusetts;ma:mass", "Michigan;mi:mich",
			"Minnesota;mn:minn", "Mississippi;ms:miss", "Missouri;mo",
			"Montana;mt:mont", "Nebraska;ne:neb", "Nevada;nv:nev",
			"New Hampshire;nh", "New Jersey;nj:n.j", "New Mexico;nm:n.m",
			"New York;ny:n.y", "North Carolina;nc:n.c", "North Dakota;nd:n.d",
			"Northern Mariana Islands;mp", "Nova Scotia;ns:n.s", "Ohio;oh",
			"Oklahoma;ok:okla", "Ontario;ont", "Oregon;or:ore", "Palau;pw",
			"Pennsylvania;pa:penn", "Puerto Rico;pr:p.r",
			"Rhode Island;ri:r.i", "South Carolina;sc:s.c",
			"South Dakota;sd:s.d",
			"Tennessee;tn:tenn",
			"Texas;tx:tex",
			// "United States;us",
			"Utah;ut", "Vermont;vt", "Virgin Islands;vi:v.i", "Virginia;va",
			"Washington;wa:wash", "Washington DC;dc:d.c:washington, d.c",
			"West Virginia;wv:w va:wva:w.v", "Wisconsin;wi:wis:wisc",
			"Wyoming;wy:wyo" };

	static Hashtable<String, String> getPlaceAbbrevs() {
		if (placeAbbrevs == null) {
			placeAbbrevs = new Hashtable<String, String>();
			for (int i = 0; i < abbrevs.length; i++) {
				String[] x = abbrevs[i].split(";");
				placeAbbrevs.put(x[0].toLowerCase(), x[0]);
				String[] abbrs = x[1].split(":");
				for (int j = 0; j < abbrs.length; j++) {
					placeAbbrevs.put(abbrs[j].toLowerCase(), x[0]);
				}
			}
		}
		return placeAbbrevs;
	}

	static PlaceFacetValueParser getInstance() {
		return self;
	}

	public String[][] parse(String value, Populate _handler) {
		// handler = _handler;
		String[][] result = trimPlaces(value, _handler);
		if (result[0].length == 1)
			result = hierPlaces(result[0][0], _handler);
		return result;
	}

	/**
	 * This just looks up atomic locations, not 'Ogden, UT'
	 * 
	 * @param value
	 * @return e.g. [["North America", "United States", "Utah"]]
	 */
	private String[][] hierPlaces(String value, Populate handler) {
		String[][] result = { { value } };
		if (handler.moves != null) {
			String ancestorss = handler.moves.get("Places -- " + value);
//			Util.print("hierPlaces "+value+" "+ancestorss);
			if (ancestorss != null) {
				String[] ancestors = Util.splitSemicolon(ancestorss);
				result = new String[ancestors.length][];
				for (int i = 0; i < ancestors.length; i++) {
					String[] ancestorFacets = ancestors[i].split(" -- ");
					assert ancestorFacets[0].equals("Places") : value + " "
							+ ancestorss;
					result[i] = (String[]) Util.subArray(ancestorFacets, 1,
							String.class);
				}
			}
		}
		// String broad;
		// PreparedStatement ps = jdbc.lookupPS("SELECT broader FROM
		// loc.places_hierarchy WHERE term = ?");
		// ps.setString(1, "Places -- " + result[0]);
		// try {
		// while ((broad = jdbc.SQLqueryString(ps, "lookup broader")) != null) {
		// result = (String[]) Util.push(result, broad, String.class);
		// ps.setString(1, result[0]);
		// }
		// } catch (AssertionError e) {
		// // Skip if there are multiple parents for term
		// // in
		// // the
		// // places hierarchy
		// }
		// if (result.length>1)
		// Util.print("hierPlaces " + Util.join(result));
		return result;
	}

	/**
	 * @param value
	 *            e.g. "downtown (ogden, utah)" or "ogden (utah)" or "ogden,
	 *            utah" 'Victoria Falls (Zambia and Zimbabwe)' 'Keystone, W. Va'
	 * @return [["North America", "United States", "Utah", "Ogden", "downtown"]]
	 *         [[Africa, Zambia, Victoria Falls ], [Africa, Zimbabwe, Victoria
	 *         Falls ]] [[North America, United States, West Virginia,
	 *         Keystone]]
	 */
	private String[][] trimPlaces(String value, Populate _handler) {
		boolean debug = false&&value.matches(".*, Italy");
		String[][] result = { { value } };
		// group(1) is everything up to the first comma or parenthesis
		// group(2) is everything after the first comma or paren up to the next
		// close parenthesis, colon, or the end
		Pattern p = Pattern.compile("\\b([^(]+?)(?:,|\\()\\s*(.*?)\\s*(\\z|:|\\))");
		Matcher m = p.matcher(value);
		if(debug)Util.print("trimPlaces "+value);
		if (m.find()) {
			String narrow = m.group(1); // e.g. "downtown"
			String broadAbbrev = m.group(2);
			if(debug)Util.print(" narrow/broad "+narrow+" "+broadAbbrev);
			if (broadAbbrev != null) {
				// broadAbbrev = broadAbbrev.toLowerCase();
				broadAbbrev = broadAbbrev.replaceAll("\\.", "");
				String[] multipleParents = broadAbbrev.split(" [aA][nN][dD] ");
				if (multipleParents.length > 1) {
					String[][] x = new String[multipleParents.length][];
					x[0] = result[0];
					result = x;
				}
				for (int j = 0; j < multipleParents.length; j++) {
					String[] broads = multipleParents[j].split(","); // e.g.
					// ["Ogden",
					// "UT"]
					// or
					// ["UT"]
					String lastBroad = broads[broads.length - 1].trim()
							.toLowerCase(); // e.g. UT
					String[] broad = lookupLocation(lastBroad, _handler); // e.g.
					// "Location --
					// North America
					// -- United
					// States --
					// Utah"
					if(debug)Util.print(" multiple parents "+lastBroad+" "+broad);
					if (broad == null)
						broad = lookupLocation(m.group(2), _handler);
					// If broad is still null, it might be "Russia
					// (Federation)", which isn't a place so we should ignore it
					if (broad != null) {
						result[j] = broad;
						for (int i = broads.length - 2; i >= 0; i--) {
							// use loop in case broads is ["Ogden", "UT", "US"]
							result[j] = (String[]) Util.endPush(result[j],
									broads[i].trim(), String.class);
						}
						result[j] = (String[]) Util.endPush(result[j], narrow,
								String.class); // e.g. ["Location", "North
						// America", "United States",
						// "Utah", "Ogden", "downtown"]
					}
					// if (result[j] != null) {
					// PreparedStatement ps = jdbc.lookupPS("SELECT broader FROM
					// loc.places_hierarchy WHERE term = ?");
					// ps.setString(1, result[j][0]);
					// try {
					// while ((broad = jdbc.SQLqueryString(ps,
					// "lookup broader")) != null) {
					// assert false : Util.valueOfDeep(result[j])
					// + " " + broad;
					// result[j] = (String[]) Util.push(result[j],
					// broad, String.class);
					// ps.setString(1, result[j][0]);
					// }
					// } catch (AssertionError e) {
					// // Skip if there are multiple parents for term
					// // in
					// // the
					// // places hierarchy
					// }
					// }
				}
			}
		}
		result = (String[][]) Util.delete(result, null, String[].class);
		// if (result[0].length > 1)
		// Util.err("trimPlaces '" + value + "' => "
		// + Util.valueOfDeep(result) + " '" + m.group(1) + "' '"
		// + m.group(2) + "'");
		return result;
	}

	/**
	 * This just looks up atomic locations, not 'Ogden, UT'
	 * 
	 * @param possibleLocation
	 * @return e.g. [["North America", "United States", "Utah"]]
	 */
	private String[] lookupLocation(String possibleLocation, Populate _handler) {
		String unabbreviated = getPlaceAbbrevs().get(
				possibleLocation.toLowerCase());
		String[] result = null;
		if (unabbreviated != null) {
			result = new String[1];
			result[0] = unabbreviated;
		}
		if (result == null) {
			String[][] hierPlaces = hierPlaces(possibleLocation, _handler);
			if (hierPlaces.length == 1 && hierPlaces[0].length > 1)
				result = hierPlaces[0];
		}
		return result;
	}

	/**
	 * E.g. "Downtown (Pittsburgh, Pa.)" where Pa is in abbrevs. Only adds
	 * Downtown if it is in cities.
	 */
	static Pattern cityState = Pattern
			.compile("\\s*(.*?)\\,?\\s*\\((\\w*?)\\.?,?\\s*(\\w+)\\.?\\)\\z");

	// int[] getFacets(Field field, String value, int parent) throws
	// SQLException {
	// int[] result = null;
	// int facetType = field.getFacetType();
	// // Util.print("getFacets " + field + " " + value + " " + parent + " " +
	// // facetType);
	// if (parent < 0) {
	// result = lookupFacets(value, facetType);
	// parent = facetType;
	// } else {
	// int theResult = lookupFacet(value, parent);
	// // if (theResult < 0) {
	// // theResult = lookupFacet(value, facetType);
	// // if (theResult > 0)
	// // setParent(theResult, parent);
	// // }
	// if (theResult > 0)
	// result = Util.push(result, theResult);
	// }
	// if (result == null) {
	// // if (parent != facetType) {
	// // if (Util.join(foo, " -- ").indexOf("Pearce, James
	// // Alfred") >= 0)
	// // Util.print("getFacets adding " + value + " " + parent + "["
	// // + facetType + "] " + Util.join(foo, " -- "));
	// int theResult = newFacet(value, parent);
	// result = Util.push(result, theResult);
	// // }
	// // else
	// // Util.err("getFacets not adding " + value + " " + parent + "["
	// // + facetType + "] " + Util.join(foo, " -- "));
	// }
	// // if (result != null)
	// // Util.print("..getFacets(" + value + ") => "
	// // + getFacetName(result[0]));
	// // else
	// // Util.print("..getFacets(" + value + ") => null");
	// return result;
	// }

}

class SubFacetValueParser extends PlaceFacetValueParser {
	String getKey() {
		assert false;
		return null;
	}

	@Override
	public String[][] parse(String value, Populate handler) {
		/**
		 * Holds successive subfield values for concatenating hierarchical
		 * lists. E.g. in
		 * 
		 * <marc:subfield code="z">Virgina</marc:subfield>
		 * 
		 * <marc:subfield code="z">Richmond</marc:subfield>
		 */
		return super.parse(getSubFacets(value, getKey(), handler), handler);
	}

	/**
	 * @param value
	 *            facet name
	 * @param key
	 *            either "z" for Places or "corp" for Specific Organizations
	 * @return ancestor facets
	 */
	@SuppressWarnings("unchecked")
	static String getSubFacets(String value, String key, Populate handler) {
		Map<String, String[]> subFacetTable = (Hashtable<String, String[]>) handler.parserState;
		String[] prev = subFacetTable.get(key);
		if (prev == null || !prev[prev.length - 1].equals(value)) {
			String[] updated = (String[]) Util.endPush(prev, value,
					String.class);
			subFacetTable.put(key, updated);
		}
		if (prev != null)
			value += ", " + Util.join(prev);
		return value;
	}
}

class PlaceSubFacetValueParser extends SubFacetValueParser {

	private static final PlaceSubFacetValueParser self = new PlaceSubFacetValueParser();

	static PlaceSubFacetValueParser getInstance() {
		return self;
	}

	@Override
	String getKey() {
		return "z";
	}
}

class NamedEntitySubFacetValueParser extends SubFacetValueParser {

	private static final NamedEntitySubFacetValueParser self = new NamedEntitySubFacetValueParser();

	static NamedEntitySubFacetValueParser getInstance() {
		return self;
	}

	@Override
	String getKey() {
		return "corp";
	}
}

class Place043FacetValueParser implements FacetValueParser {

	private static final Place043FacetValueParser self = new Place043FacetValueParser();

	static Place043FacetValueParser getInstance() {
		return self;
	}

	public String[][] parse(String value, Populate handler) {
		String[][] result = { handler.db.lookupGAC(GenericFacetValueParser
				.trim(value)) };
		return result;
	}
}

class PreparsedFacetValueParser implements FacetValueParser {

	private static final PreparsedFacetValueParser self = new PreparsedFacetValueParser();

	static PreparsedFacetValueParser getInstance() {
		return self;
	}

	public String[][] parse(String value, Populate handler) {
		String[][] result = { value.split(" -- ") };
		return result;
	}

}

class SubjectFacetValueParser implements FacetValueParser {

	private static final SubjectFacetValueParser self = new SubjectFacetValueParser();

	static SubjectFacetValueParser getInstance() {
		return self;
	}

	public String[][] parse(String value, Populate handler) {
		String[][] result = { { value } };
		String[] subjects = value.split("\\.?; ?");
		result = new String[subjects.length][];
		for (int i = 0; i < result.length; i++) {
			if (i < subjects.length)
				result[i] = subjects[i].split("-{2,}");
			for (int j = 0; j < result[i].length; j++) {
				result[i][j] = result[i][j].trim();
				String s = result[i][j];
				s = handler.rename(s);
				if (s.length() == 0) {
					Util.print("Ignoring zero length facet name in '"
							+ result[i][j] + "' in '"
							+ Util.join(result[i], " -- ") + "' in '" + value
							+ "'");
					result[i] = null;
					break;
				} else if (s.charAt(0) == '-'
						|| (s.charAt(s.length() - 1) == '-' && result[i].length > j + 1)
						|| s.indexOf("-pitts") >= 0 || s.indexOf("-penns") >= 0
						|| (s.indexOf("enns") != s.indexOf("ennsylvania"))
						|| (s.indexOf("itts") != s.indexOf("ittsburgh"))) {
					Util.print("WARNING ignoring suspicious facet name '" + s
							+ "' in '" + result[i][j] + "' in '"
							+ Util.join(result[i], " -- ") + "' in '" + value
							+ "'");
					result[i] = null;
					break;
				}
				int k = Util.member(result[i], s, j + 1);
				if (k > 0) {
					if (s.equalsIgnoreCase("Pennsylvania")
							&& result[i][k - 1].equalsIgnoreCase("Pittsburgh")) {
						result[i] = (String[]) Util.deleteIndex(result[i], k,
								String.class);
					} else {
						Util.print("WARNING repeated facet name '"
								+ result[i][j] + "' in '"
								+ Util.join(result[i], " -- ") + "' in '"
								+ value + "'");
						result[i] = (String[]) Util.subArray(result[i], 0, j,
								String.class);
						Util.print("...treating as: ["
								+ Util.join(result[i], ",") + "] ");
					}
				}
				if (!result[i][0].equals("Places")
						&& PlaceFacetValueParser.getPlaceAbbrevs().get(
								s.toLowerCase()) != null) {
					String[] place = (String[]) Util
							.push(Util.subArray(result[i], j, String.class),
									"Places", String.class);
					if (j > 0) {
						result = (String[][]) Util.endPush(result, place,
								String[].class);
						result[i] = (String[]) Util.subArray(result[i], 0,
								j - 1, String.class);
					} else
						result[i] = place;
				}

				Matcher m = PlaceFacetValueParser.cityState.matcher(s);
				if (m.find()) {
					String state = PlaceFacetValueParser.getPlaceAbbrevs().get(
							m.group(3).toLowerCase());
					if (state != null) {
						state = "Places--" + state;
						String city = m.group(2);
						if (city != null && city.length() > 0)
							state += "--" + city;
						if (handler.cities != null
								&& handler.cities.contains(m.group(1)
										.toLowerCase())) {
							state += "--" + m.group(1);
							result[i] = state.split("--");
						} else {
							result[i][j] = m.group(1);
							result = (String[][]) Util.endPush(result, state
									.split("--"), String[].class);
						}
					} else {
						// Util.print("Not a state: " + s);
						Pattern p2 = Pattern.compile("(.+)\\s*\\((.+)\\)\\z");
						Matcher m2 = p2.matcher(s);
						if(m2.find()) {
						String[] augmented = null;
						result[i][j] = m2.group(1);
						if (j > 0)
							augmented = (String[]) Util.subArray(result[i], 0,
									j - 1, String.class);
						augmented = (String[]) Util.endPush(augmented, m2
								.group(2), String.class);
						augmented = (String[]) Util.append(augmented, Util
								.subArray(result[i], j, String.class),
								String.class);
						result[i] = augmented;
					}else {
						Util.err("SubjectFacetValueParser: No match for " + s);
					}}
				}
			}
		}
		result=(String[][]) Util.delete(result, null, String[].class);
		for (int i = 0; i < result.length; i++) {
			if (/* result[i] != null && */ !result[i][0].equals("Places")
					&& Util.member(result[i], "Pennsylvania", 1) > 0)
				Util.print("Should this be a place? ["
						+ Util.join(result[i], ",") + "] " + value);
		}
		return result;
	}

}

class ANDedValuesParser implements FacetValueParser {

	private static final ANDedValuesParser self = new ANDedValuesParser();

	static ANDedValuesParser getInstance() {
		return self;
	}

	public String[][] parse(String value, Populate handler) {
		String[] terms = value.split(" and ");
		String[][] result = new String[terms.length][];
		for (int i = 0; i < result.length; i++) {
			result[i] = new String[1];
			result[i][0] = terms[i];
		}
		return result;
	}

}
