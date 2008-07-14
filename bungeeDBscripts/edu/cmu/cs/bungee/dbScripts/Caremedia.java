package edu.cmu.cs.bungee.dbScripts;

import java.sql.ResultSet;
import java.sql.SQLException;

import edu.cmu.cs.bungee.javaExtensions.JDBCSample;
import edu.cmu.cs.bungee.javaExtensions.Util;

/**
 * Run it like this: -db cm -user root -pass tartan01 -verbose -reset
 * 
 * Don't renumber - IDs match caremedia event_id
 * 
 */
public class Caremedia extends Options {

	public static void main(String[] args) {
		try {
			Caremedia caremedia = new Caremedia();
			caremedia.init(false, args);
			caremedia.loadImages();
		} catch (Exception ex) {
			ex.printStackTrace(System.out);
		}
	}

	@Override
	void readData() {
		ResultSet rs;
		try {
			rs = handler.db
					.getJdbc()
					.SQLquery(
							"SELECT "
									+ "event_id, "
									+ "description, "
									+ "camera_name, "
									+ "start_date, "
									+ "TIMESTAMPDIFF(SECOND, start_date, end_date) duration, "
									+ "annotator_id, "
									+ "subject_id, "
									+ "IFNULL(prim.name, CONCAT(event.primary_event_id, ' <Undefined>')) primary_name, "
									+ "IFNULL(secondary.name, CONCAT(event.primary_event_id, '_',"
									+ " event.secondary_event_id, ' <Undefined>')) secondary_name, "
									+ "event_date, "
									+ "location, "
									+ "assist "
									+ "FROM event "
									+ "LEFT JOIN event_types prim ON event.primary_event_id=prim.primary_event_id"
									+ " AND prim.secondary_event_id IS NULL "
									+ "LEFT JOIN event_types secondary"
									+ " ON event.primary_event_id=secondary.primary_event_id"
									+ " AND event.secondary_event_id=secondary.secondary_event_id"
					// + " limit 1000"
					);
			Attribute description = Attribute.getAttribute("description");
			Facet camera = Facet.getGenericFacet("Camera");
			Facet date = Facet.getDateFacet("Start Date");
			Facet time = Facet.getTimeFacet("Start Time");
			Facet duration = Facet.getTimeFacet("Duration");
			Facet annotator = Facet.getNumericFacet("Annotator");
			Facet subject = Facet.getNumericFacet("Subject");
			Facet eventDate = Facet.getDateFacet("Event Date");
			Facet location = Facet.getGenericFacet("Location");
			Facet assist = Facet.getGenericFacet("Assist");
			Facet event = Facet.getParsingFacet("Event",
					PreparsedFacetValueParser.getInstance());
			// Extract '5' from '1_5Conversation', or '1' from '1 Conversation'.
			// (?s) means . matches newlines.
			event.sortPattern = "(?s)(?:\\d+_)?(\\d+).*";
			while (rs.next()) {
				handler.newItem(rs.getInt("event_id"));
				description.insert(rs.getString("description"), handler);
				camera.insert(rs.getString("camera_name"), handler);
				date.insert(rs.getString("start_date"), handler);
				time.insert(rs.getString("start_date"), handler);
				duration.insert(rs.getInt("duration") + "", handler);
				annotator.insert(rs.getString("annotator_id"), handler);
				subject.insert(rs.getString("subject_id"), handler);
				eventDate.insert(rs.getString("event_date"), handler);
				location.insert(rs.getString("location"), handler);
				assist.insert(rs.getString("assist"), handler);
				event.insert(rs.getString("primary_name").split("\n")[0]
						+ " -- "
						+ rs.getString("secondary_name").split("\n")[0],
						handler);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	void loadImages() throws SQLException {
		Util.print("Loading images...");
		JDBCSample jdbc = handler.db.getJdbc();

		jdbc.SQLupdate("TRUNCATE TABLE images");
		jdbc.SQLupdate("DROP TABLE IF EXISTS all_times");
		// List movies separately in case there's no matching shotbreak.
		jdbc
				.SQLupdate("CREATE  TABLE all_times AS "
						+ "(SELECT NULL event_id, shotbreak_id, movie.movie_id, "
						+ "TIMESTAMPADD(SECOND,shot.startoffset/1000, copyright_date) time_z, "
						+ "TIMESTAMPADD(SECOND,shot.endoffset/1000, copyright_date) shot_end, "
						+ "TIMESTAMPADD(SECOND,media_length/1000,copyright_date) movie_end, "
						+ "SUBSTRING(movie.movie_name, 1, 4) camera_name "
						+ "FROM movie INNER JOIN shotbreak shot USING (movie_id) "
						// + "LIMIT 1000"
						+ ") "

						+ "UNION "
						+ "(SELECT event_id, NULL, NULL, "
						+ "TIMESTAMPADD(SECOND,TIMESTAMPDIFF(SECOND, start_date, end_date)/2,start_date),"
						+ " CAST('9999-12-31 23:59:59' AS DATETIME), NULL, camera_name "
						+ "FROM event "
						// + "LIMIT 1000"
						+ ") "

						+ "UNION "
						+ "(SELECT NULL, NULL, movie_id, copyright_date, CAST('1000-01-01 00:00:00' AS DATETIME),"
						+ " TIMESTAMPADD(SECOND,media_length/1000,copyright_date),"
						+ " SUBSTRING(movie_name, 1, 4) " + "FROM movie "
						// + "LIMIT 1000"
						+ ") "

				);
		jdbc
		.SQLupdate("UPDATE item SET shotbreak_id = NULL, movie_id = NULL");
		// jdbc
		// .SQLupdate("ALTER TABLE all_times ADD INDEX time_index(camera_name,
		// time_z);");
		jdbc.SQLupdate("DROP PROCEDURE IF EXISTS compute_shotbreaks");
		jdbc
				.SQLupdate("CREATE PROCEDURE compute_shotbreaks()"
						+ "BEGIN"
						+ " DECLARE done INT DEFAULT 0; "
						+ " DECLARE current_shot, current_movie, event, shot, movie BIGINT DEFAULT -1;"
						+ " DECLARE camera, current_camera CHAR(10) DEFAULT '';"
						+ " DECLARE current_shot_end, current_movie_end, time_x, shot_end_x, movie_end_x DATETIME DEFAULT NULL;"
						+ ""
						+ " DECLARE cur CURSOR FOR"
						+ " SELECT event_id, camera_name, time_z, shotbreak_id, shot_end, movie_id, movie_end FROM all_times "
						// + "force index (time_index) "
						+ " ORDER BY camera_name, time_z, shot_end;"
						+ " DECLARE CONTINUE HANDLER FOR SQLSTATE '02000' SET done = 1;"
						+ ""
						+ " OPEN cur;"
						+ " REPEAT"
						+ "  FETCH cur INTO event, camera, time_x, shot, shot_end_x, movie, movie_end_x; "
						+ "  IF NOT done THEN"
						+ "   IF event IS NOT NULL AND camera = current_camera"
						+
						// time_x ought to always be less than current_shot_end
						// here, as shots are not
						// supposed to have gaps. However there was a bug
						// processing the last shot of a movie and
						// Bob says to use the last shot anyway. The -15 minutes
						// is just a sanity check on the way Bob described the
						// bug.
						" AND TIMESTAMPADD(MINUTE, -15, time_x) <= current_shot_end"
						+ " THEN"
						+ "     UPDATE item SET shotbreak_id = current_shot WHERE record_num = event;"
						+ "   END IF;"
						+ "   IF event IS NOT NULL AND camera = current_camera AND time_x <= current_movie_end THEN"
						+ "     UPDATE item SET movie_id = current_movie WHERE record_num = event;"
						+ "   END IF;"
						+ "   IF camera != current_camera THEN"
						+ "     SET current_shot = NULL;"
						+ "     SET current_movie = NULL;"
						+ "     SET current_camera = camera;"
						+ "   END IF;"
						+ "   IF shot IS NOT NULL THEN"
						+ "     SET current_shot = shot;"
						+ "     SET current_shot_end = shot_end_x;"
						// + " SET current_camera = camera;"
						+ "   END IF;" + "   IF movie IS NOT NULL THEN"
						+ "     SET current_movie = movie;"
						+ "     SET current_movie_end = movie_end_x;"
						// + " SET current_camera = camera;"
						+ "   END IF;" + "  END IF;"
						+ " UNTIL done END REPEAT;" + " CLOSE cur; END");
		jdbc.SQLupdate("CALL compute_shotbreaks()");
		jdbc.SQLupdate("DROP PROCEDURE compute_shotbreaks");

		jdbc.SQLupdate("DROP TABLE IF EXISTS images");
		jdbc.SQLupdate("CREATE TABLE images LIKE wpa.images");
		jdbc.SQLupdate("INSERT INTO images "
				+ "(SELECT record_num, image, image_width, image_height"
				+ " FROM item INNER JOIN shotbreak USING (shotbreak_id)"
				+ " INNER JOIN movie ON movie.movie_id = shotbreak.movie_id"
				+ " WHERE image IS NOT NULL)");
		Util.print("...done");
	}
}
