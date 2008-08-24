package log;

import java.awt.Color;
import java.text.DateFormat;
import java.util.Date;

import edu.cmu.cs.bungee.piccoloUtils.gui.APText;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPNode;
import edu.cmu.cs.bungee.piccoloUtils.gui.LazyPPath;
import edu.cmu.cs.bungee.piccoloUtils.gui.MyInputEventHandler;
import edu.umd.cs.piccolo.PNode;

class Session extends LazyPPath {

	int ID;
	int nOps;
	Date start;
	Date end;
	String db;
	String IPaddress;
	static final int maxEdge = 30;
	static final Color normalColor = Color.cyan;
	static final Color highlightColor = Color.black;

	Session(int sessionID, String dbName, int opCount, Date minDate,
			Date maxDate, String IP) {
		ID = sessionID;
		nOps = opCount;
		start = minDate;
		end = maxDate;
		db = dbName;
		IPaddress = IP;

		setStroke(LazyPPath.getStrokeInstance(1));
		setStrokePaint(normalColor);
	}

	void setSize(int maxSize) {
		double edgeRatio = Math.sqrt(maxSize) / maxEdge;
		float edge = (float) (Math.sqrt(nOps) / edgeRatio);
		setBounds(-edge / 2, -edge / 2, edge, edge);

		float[] Xs = { -edge / 2, edge / 2, edge / 2, -edge / 2, -edge / 2 };
		float[] Ys = { -edge / 2, -edge / 2, edge / 2, edge / 2, -edge / 2 };
		setPathToPolyline(Xs, Ys);

		LazyPNode child = new LazyPNode();
		child.setPaint(Color.white);
		child.setTransparency(0.001f);
		child.setBounds(getBounds());
		addChild(child);
		child.addInputEventListener(new SessionEventHandler());
	}

	String elapsedTime() {
		long elapsedS = (end.getTime() - start.getTime()) / 1000;
		long elapsedM = elapsedS / 60;
		long elapsedS1 = elapsedS % 60;
		return elapsedM + ":" + elapsedS1;
	}

	@Override
	public String toString() {
		return "<Session " + start + " elapsed time=" + elapsedTime()
				+ " nOps=" + nOps + " " + db + ">";
	}

	private class SessionEventHandler extends MyInputEventHandler {

		private APText popup = new APText();

		SessionEventHandler() {
			super(Session.class);
			popup.setOffset(0, 30);
		}

		@Override
		protected boolean enter(PNode node) {
			popup.setText(DateFormat.getDateTimeInstance().format(start)
					+ "\nelapsed time: " + elapsedTime() + "\nnOps: " + nOps
					+ "\nIP address: " + IPaddress);
			addChild(popup);
			((Chart) getParent()).highlight(IPaddress);
			return true;
		}

		@Override
		protected boolean exit(PNode node) {
			removeChild(popup);
			((Chart) getParent()).highlight(null);
			return true;
		}

		@Override
		protected boolean click(PNode node) {
			String URLstring = "http://localhost/bungee/bungee.jsp?db=" + db
					+ "&session=" + ID;
			((Chart) getParent()).showDocument(URLstring);
			return true;
		}
	}

	 void highlight(String address) {
		Color color = IPaddress.equals(address) ? highlightColor : normalColor;
		setStrokePaint(color);
	}

}
