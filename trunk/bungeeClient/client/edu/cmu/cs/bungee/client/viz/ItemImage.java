package edu.cmu.cs.bungee.client.viz;

import java.awt.Image;
import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import edu.cmu.cs.bungee.client.query.Perspective;
import edu.cmu.cs.bungee.client.query.Query.Item;

final class ItemImage {

	Bungee art;

	Item item;

	/**
	 * This is the size of the image in the database
	 */
	private int rawW;

	private int rawH;

	// quality of the cached image, from 0 to 100
	int quality;

	// boolean rawLoaded;

	/**
	 * This is the image as read from the database, which may have downconverted
	 * the size or quality.
	 */
	Image rawImage;

	Set<Perspective> facets = new HashSet<Perspective>();

	private String description;

	// static BufferedImage compatibleImage = Util.createCompatibleImage(1, 1);
	//
	// static ColorModel compatibleColorModel = compatibleImage.getColorModel();
	//
	// static SampleModel compatibleSampleModel =
	// compatibleImage.getSampleModel();

	// public ItemImage(Art _art, int _item) {
	// init(_art, _item, 0, 0, null);
	// }

	ItemImage(Bungee art1, Item item1, int rawW1, int rawH1, int quality1,
			Image bi, String description) {
		init(art1, item1, rawW1, rawH1, quality1, bi, description);
	}

	// GridImage(Art _art, int _item, int w, int h, boolean imageLoaded,
	// BufferedImage im) {
	// rawLoaded = imageLoaded;
	// init(_art, _item, w, h, im);
	// }
	//
	// GridImage(Art _art, int _item, BufferedImage im) {
	// rawLoaded = true;
	// init(_art, _item, 0, 0, im);
	// }

	private void init(Bungee _art, Item _item, int w, int h, int _quality,
			Image bi, String desc) {
		// Util.print("init itemImage " + _item + " " + w + "x" + h);
		// if (im != null)
		// Util.print(im.getWidth() + "x" + im.getHeight());
		art = _art;
		item = _item;
		rawW = w;
		rawH = h;
		description = desc;
		if (bi != null)
			setRawImage(bi, _quality);
		assert art.lookupItemImage(item) == null;

		Map<Item, ItemImage> table = art.getItemImagesTable();
		if (table == null) {
			table = new Hashtable<Item, ItemImage>();
			// Util.print("new lookupItemImage");
			art.itemImages = new SoftReference<Map<Item, ItemImage>>(table);
		}
		// Util.print("lookupii table size " + table.size());
		table.put(item, this);
	}

	boolean bigEnough(Image bi, int _quality) {
		return bigEnough(bi.getWidth(null), bi.getHeight(null), _quality);
	}

	/**
	 * If either rawImage width >= w OR rawImage height >= h, the rawImage is at
	 * least as big as the scaled thumbnail from an infinitely big original
	 * would be. If it is as big as the original, we use it as is, too.
	 * 
	 * @param w
	 * @param h
	 * @param _quality
	 * @return whether to use the cached rawImage
	 */
	boolean bigEnough(int w, int h, int _quality) {
		if (rawImage == null)
			// Use the "missing" image
			return true;
		int actualW = rawImage.getWidth(null);
		int actualH = rawImage.getHeight(null);

		// could allow for lower quality if the image is to be reduced. Would
		// need to compute the real amount of reduction, which isn't just
		// (actualH * actualW) / (w * h).
		boolean result = (actualW >= w || rawW == actualW || actualH >= h || rawH == actualH)
				&& quality >= _quality;
		// Util.print("bigEnough " + item + " " + w + " " + rawW + " " + actualW
		// + " " + h + " " + rawH + " " + actualH + " " + result);
		return result;
	}

	// public int rawW() {
	// if (rawW == 0)
	// rawW = getRawImage().getWidth(null);
	// return rawW;
	// }
	//
	// public int rawH() {
	// if (rawH == 0)
	// rawH = getRawImage().getHeight(null);
	// return rawH;
	// }

	int currentW() {
		return getRawImage().getWidth(null);
	}

	int currentH() {
		return getRawImage().getHeight(null);
	}

	void setRawImage(Image bi, int _quality) {
		// System.out.println("setRawImage " + bi.getWidth(null) + "x" +
		// bi.getHeight(null));
		rawImage = bi;
		quality = _quality;
	}

	Image getRawImage() {
		// if (rawImage == null) {
		// int[] thumbs = { item };
		// ResultSet rs = art.query.getThumbs(thumbs, -1, -1);
		// try {
		// if (rs.next()) {
		// InputStream blobStream = ((MyResultSet) rs)
		// .getInputStream(2);
		// BufferedImage image = ImageIO.read(blobStream);
		// setRawImage(image);
		// }
		// } catch (SQLException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		if (rawImage == null)
			return art.getMissingImage();
		else
			return rawImage;
	}

	String getName() {
		if (description != null)
			return description;
		return item.toString();
	}

	@Override
	public String toString() {
		return "<ItemImage for " + item
				+ (description != null ? " " + description : "") + ">";
	}

}