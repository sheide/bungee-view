package log;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;

class Chart extends LazyPNode {
	
	Log log;
	
	Set<Session> sessions = new HashSet<Session>();
	Set<String> dbNames = new HashSet<String>();
	int maxOps = 0;
	Date minDate = new Timestamp(Long.MAX_VALUE);
	Date maxDate = new Timestamp(0);
	
	Xaxis xAxis;
	Yaxis yAxis;
	
	Chart(Log _log) {
		log = _log;
	}
	
	void addSession(Session session) {
		sessions.add(session);
		dbNames.add(session.db);
		maxOps = Math.max(maxOps, session.nOps);
		if (session.start.compareTo(minDate) < 0)
			minDate = session.start;
		if (session.end.compareTo(maxDate) > 0)
			maxDate = session.end;
	}

	void draw() {
//		Util.print("draw " + getBounds());
//		setPaint(Color.yellow);
		xAxis = new Xaxis(minDate, maxDate);
		yAxis = new Yaxis(dbNames);
		redraw();
	}
	
	void redraw() {
		removeAllChildren();
		addChild(xAxis);
		xAxis.setOffset(0, getHeight());
		xAxis.draw(getWidth());
		yAxis.setOffset(0, 0);
		addChild(yAxis);
		yAxis.draw(100, getHeight());
		for (Iterator<Session> it = sessions.iterator(); it.hasNext();) {
			Session session = it.next();
			session.setOffset(xAxis.encode(session.start), yAxis.encode(session.db));
			session.setSize(maxOps);
			addChild(session);
//			Util.print(session + " " + session.getOffset());
		}		
	}

	void showDocument(String URLstring) {
		log.showDocument(URLstring);
	}

	public void highlight(String IPaddress) {
		for (Iterator<Session> it = sessions.iterator(); it.hasNext();) {
			Session session = it.next();
			session.highlight(IPaddress);
		}
	}
}
