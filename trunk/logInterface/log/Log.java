package log;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;

import edu.cmu.cs.bungee.javaExtensions.JDBCSample;
import edu.cmu.cs.bungee.javaExtensions.URLQuery;
import edu.cmu.cs.bungee.javaExtensions.Util;
import edu.umd.cs.piccolo.PLayer;
import edu.umd.cs.piccolox.PFrame;

public final class Log extends PFrame {

	private JDBCSample jdbc;

	BasicService basicJNLPservice = maybeGetBasicService();

	Chart chart;

	public static void main(String[] args) {
		// Util.print("Starting Art");
		new Log(args);
	}

	private Log(String[] args) {
		// gross hack to pass arguments to initialize
		super(args.length > 0 ? args[0] : null, false, null);
	}

	@Override
	public void initialize() {
		getCanvas().setPanEventHandler(null);
		getCanvas().setZoomEventHandler(null);
		String argString = getTitle();
		// Util.print(argString);
		URLQuery argURLQuery = new URLQuery(argString);
		setTitle("Bungee View Log");
		String dbs = argURLQuery.getArgument("dbs");
		assert dbs != null && dbs.length() > 0 : "Empty db name list";
		String[] dbNames = Util.splitComma(dbs);
		String host = argURLQuery.getArgument("host");
		String user = argURLQuery.getArgument("user");
		String pass = argURLQuery.getArgument("pass");
		try {
			jdbc = new JDBCSample(host, dbNames[0], user, pass);
			read(dbNames);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Rectangle getDefaultFrameBounds() {
		return new Rectangle(0, 0, 1000, 740);
	}

	private void setSize() {
		double w = getWidth();
		double h = getHeight();
		PLayer layer = getCanvas().getLayer();
		chart.setOffset(140, 140);
		chart.setBounds(0, 0, w - 280, h - 280);
		// chart.setBounds(0, 0, 200, 200);
		layer.addChild(chart);
		chart.draw();
	}

	void read(String[] dbNames) throws SQLException {
		chart = new Chart(this);
		for (int i = 0; i < dbNames.length; i++) {
			String table = dbNames[i] + ".user_actions";
			ResultSet rs = jdbc
					.SQLquery("SELECT session, COUNT(*), MIN(timestamp), MAX(timestamp), client FROM "
							+ table + " GROUP BY session");
			while (rs.next()) {
				int sessionID = rs.getInt(1);
				int nOps = rs.getInt(2);
				Date minDate = rs.getTimestamp(3);
				Date maxDate = rs.getTimestamp(4);
				String IP = rs.getString(5);
				Session session = new Session(sessionID, dbNames[i], nOps,
						minDate, maxDate, IP);
				chart.addSession(session);
			}
			rs.close();
		}
		setSize();
	}

	private static BasicService maybeGetBasicService() {
		BasicService s = null;
		try {
			s = (BasicService) ServiceManager.lookup("javax.jnlp.BasicService");
		} catch (UnavailableServiceException e) {
			Util.err("jnlp.BasicService is not available");
		}
		return s;
	}

	void showDocument(String URLstring) {
		Util.print("showDocument " + URLstring);
		if (basicJNLPservice != null) {
			try {
				basicJNLPservice.showDocument(new URL(URLstring));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		} else {
			try {
				Runtime.getRuntime().exec(
						"C:\\Program Files\\Mozilla Firefox\\firefox.exe \""
								+ URLstring + "\"");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
