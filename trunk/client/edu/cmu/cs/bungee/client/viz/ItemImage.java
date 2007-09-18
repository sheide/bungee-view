package edu.cmu.cs.bungee.client.viz;

import java.awt.Image;
import java.lang.ref.SoftReference;
import java.util.Hashtable;
import java.util.Map;

import edu.cmu.cs.bungee.client.query.Query.Item;



//final class ItemImageOuter {
//	private final Art art;
//	private final Map items = new Hashtable();
//	ItemImageOuter(Art _art) {
//		art = _art;
//	}
//
//	 ItemImage lookupItem(Item item) {
//		return (ItemImage) items.get(item);
//	}
//
//	 ItemImage ensureItem(Item _item, int _rawW, int _rawH, int _quality,
//			Image bi) {
//		ItemImage result = (ItemImage) items.get(_item);
//		if (result == null) {
//			result = new ItemImage(_item, _rawW, _rawH, _quality,
//					bi);
//		}
//		return result;
//	}
//	final class ItemImage {
//	  final Item item;
//
//		/**
//		 * This is the size of the image in the database
//		 */
//		 final int rawW;
//
//		 final int rawH;
//
//		// quality of the cached image, from 0 to 100
//		 private int quality;
//
//		/**
//		 * This is the image as read from the database, which may have downconverted 
//		 * the size or quality.
//		 */
//		private Image rawImage;
//
//	private ItemImage(Item _item, int _rawW, int _rawH, int _quality,
//			Image bi) {
//		// Util.print("init itemImage " + _item + " " + w + "x" + h);
//		// if (im != null)
//		// Util.print(im.getWidth() + "x" + im.getHeight());
//		assert lookupItem(_item) == null;
//		item = _item;
//		rawW = _rawW;
//		rawH = _rawH;
//		quality = _quality;
//		if (bi != null)
//			setRawImage(bi, _quality);
//		items.put(_item, this);
//	}
//
//	boolean bigEnough(Image bi, int _quality) {
//		return bigEnough(bi.getWidth(null), bi.getHeight(null), _quality);
//	}
//
//	 boolean bigEnough(int w, int h, int _quality) {
//		int actualW = rawImage.getWidth(null);
//		int actualH = rawImage.getHeight(null);
//		boolean result = (actualW >= w || rawW == actualW || actualH >= h || rawH == actualH)
//				&& quality * actualW * actualH >= _quality * w * h;
////		Util
////				.print("bigEnough "
////						+ item
////						+ " "
////						+ w
////						+ " "
////						+ rawW
////						+ " "
////						+ actualW
////						+ " "
////						+ h
////						+ " "
////						+ rawH
////						+ " "
////						+ actualH
////						+ " "
////						+ result);
//		return result;
//	}
//
//	 int currentW() {
//		return getRawImage().getWidth(null);
//	}
//
//	 int currentH() {
//		return getRawImage().getHeight(null);
//	}
//
//	 void setRawImage(Image bi, int _quality) {
//		// System.out.println("setRawImage " + bi.getWidth(null) + "x" +
//		// bi.getHeight(null));
//		rawImage = bi;
//		quality = _quality;
//	}
//
//		private transient BufferedImage missingImage;
//
//		BufferedImage getMissingImage() {
//			if (missingImage == null) {
//				String where = art.codeBase() + "missing.gif";
//				// Util.print(where);
//				try {
//					missingImage = ImageIO.read(new URL(where));
//				} catch (MalformedURLException e) {
//					e.printStackTrace();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//			return missingImage;
//		}
//
//		 Image getRawImage() {
//			Image result = rawImage;
//				if (result == null) {
//				result = getMissingImage();
//			}
//			return result;
//		}
//
//		transient SoftReference itemImages;
//
//		public synchronized ItemImage ensureItemImage(int item, int rawW, int rawH,
//				int quality, InputStream blobStream) {
//			ItemImage result = lookupItem(item);
//			if (blobStream == null) {
//				if (result == null) {
//					BufferedImage missing = getMissingImage();
//					rawW = missing.getWidth();
//					rawH = missing.getHeight();
//					result = new ItemImage(this, item, rawW, rawH, 100, missing);
//				}
//			} else {
//				Image bi = Util.readCompatibleImage(blobStream);
//				if (result == null)
//					result =  ;
//				else if (!result.bigEnough(bi, quality)) {
//					assert quality >= result.quality
//							|| bi.getWidth(null) > result.rawImage.getWidth(null)
//							|| bi.getHeight(null) > result.rawImage.getHeight(null);
//					result.setRawImage(bi, quality);
//				}
//			}
//			return result;
//		}
//
//}

public class ItemImage {

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

	// static BufferedImage compatibleImage = Util.createCompatibleImage(1, 1);
	//
	// static ColorModel compatibleColorModel = compatibleImage.getColorModel();
	//
	// static SampleModel compatibleSampleModel =
	// compatibleImage.getSampleModel();

	// public ItemImage(Art _art, int _item) {
	// init(_art, _item, 0, 0, null);
	// }

	public ItemImage(Bungee art1, Item item1, int rawW1, int rawH1, int quality1,
			Image bi) {
		init(art1, item1, rawW1, rawH1, quality1, bi);
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

	private void init(Bungee _art, Item _item, int w, int h, int _quality, Image bi) {
		// Util.print("init itemImage " + _item + " " + w + "x" + h);
		// if (im != null)
		// Util.print(im.getWidth() + "x" + im.getHeight());
		art = _art;
		item = _item;
		rawW = w;
		rawH = h;
		if (bi != null)
			setRawImage(bi, _quality);
		assert art.lookupItemImage(item) == null;

		Map table = art.getItemImagesTable();
		if (table == null) {
			table = new Hashtable();
			art.itemImages = new SoftReference(table);
		}
		table.put(item, this);
	}

	public boolean bigEnough(Image bi, int _quality) {
		return bigEnough(bi.getWidth(null), bi.getHeight(null), _quality);
	}

	public boolean bigEnough(int w, int h, int _quality) {
		int actualW = rawImage.getWidth(null);
		int actualH = rawImage.getHeight(null);
		boolean result = (actualW >= w || rawW == actualW || actualH >= h || rawH == actualH)
				&& quality * actualW * actualH >= _quality * w * h;
//		Util
//				.print("bigEnough "
//						+ item
//						+ " "
//						+ w
//						+ " "
//						+ rawW
//						+ " "
//						+ actualW
//						+ " "
//						+ h
//						+ " "
//						+ rawH
//						+ " "
//						+ actualH
//						+ " "
//						+ result);
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

	public int currentW() {
		return getRawImage().getWidth(null);
	}

	public int currentH() {
		return getRawImage().getHeight(null);
	}

	public void setRawImage(Image bi, int _quality) {
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
		if (rawImage == null) {
			rawImage = art.getMissingImage();
			quality = 100;
		}
		return rawImage;
	}

}