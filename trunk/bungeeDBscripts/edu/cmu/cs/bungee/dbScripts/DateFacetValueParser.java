package edu.cmu.cs.bungee.dbScripts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * parse a date and return ["20th century", "1990s", "1996", "12/1996",
 * "12/31/1996"]
 * 
 */
public class DateFacetValueParser implements FacetValueParser {

	private static final DateFacetValueParser self = new DateFacetValueParser();

	public static void main(String[] args) {
		Util.print(Util.valueOfDeep(self.parse("1911?", null)));
	}

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
		// Used to be "\\d\\?", but that loses on "1911?"
		result = result.replaceAll("\\?", "");

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
			Util.err("NumberFormatException for century " + prefix);
			// e.printStackTrace();
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
						Util.err("NumberFormatException for date " + value);
						// e.printStackTrace();
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
