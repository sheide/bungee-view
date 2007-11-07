package edu.cmu.cs.bungee.client.viz;
//package viz;
//
//import java.awt.Image;
//import java.awt.image.BufferedImage;
//import java.awt.image.RenderedImage;
//import java.awt.image.renderable.ParameterBlock;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.Iterator;
//import java.util.List;
//import java.util.ListIterator;
//import java.util.SortedSet;
//import java.util.TreeSet;
//import java.util.Vector;
//
//import javax.media.jai.Interpolation;
//import javax.media.jai.InterpolationNearest;
//import javax.media.jai.JAI;
//import javax.media.jai.PerspectiveTransform;
//import javax.media.jai.RenderedOp;
//import javax.media.jai.WarpPerspective;
//import javax.media.jai.operator.TranslateDescriptor;
//
//import query.Perspective;
//
//import madUtilPackage.printDeep;
//import madUtilPackage.Util;
//import madUtilPackage.ValueComparator;
//
//class MyTile extends GridImage {
//
//	// BufferedImage scaledImage;
//
//	// float scale = 1;
//
//	// BufferedImage transformedImage;
//
//	// double[] params;
//
////	GridImage image;
//
//	int[] offset = { Integer.MAX_VALUE, Integer.MAX_VALUE };
//
//	ArrayList ceilingIntervals;
//
//	int maxCeilingInterval;
//
//	static HeightComparator heightComparator = new HeightComparator();
//
//	static WidthComparator widthComparator = new WidthComparator();
//
//	static AreaComparator areaComparator = new AreaComparator();
//
//	static PerimeterComparator perimeterComparator = new PerimeterComparator();
//
//	static TopComparator topComparator = new TopComparator();
//
//	public MyTile(GridImage im) {
//		super(im.itemImage);
//	}
//
//	MyTile(int x, int y, int w, int h) {
//		super(null, -1, null);
//		setOffset(x, y);
//	}
//	
//	MyTile(Art _art, int _item, BufferedImage image) {
//		super(_art, _item, image);
//	}
//
//	boolean isPlaced() {
//		return rawX() < Integer.MAX_VALUE;
//	}
//
//	void unplace() {
//		setOffset(Integer.MAX_VALUE, Integer.MAX_VALUE);
//	}
//
//	int rawH() {
//		return itemImage.rawH();
//	}
//
//	int rawW() {
//		return itemImage.rawW();
//	}
//
////	void setRawW(int w) {
////		itemImage.rawW = w;
////	}
//
//	int rawX() {
//		return offset[0];
//	}
//
//	int rawY() {
//		return offset[1];
//	}
//
//	int rawRight() {
//		return rawX() + rawW();
//	}
//
//	int rawTop() {
//		return rawY() + rawH();
//	}
//
//	Image getRawImage() {
//		return itemImage.getRawImage();
//	}
//
//	int item() {
//		return itemImage.item;
//	}
//
//	void setOffset(int x, int y) {
//		offset[0] = x;
//		offset[1] = y;
//	}
//
//	public boolean rawIntersects(int x, int y, int w, int h) {
//		return x + w > rawX() && x < rawRight() && y + h > rawY()
//				&& y < rawTop();
//	}
//
//	public int area() {
//		return rawW() * rawH();
//	}
//
//	RenderedOp translate() {
//		// Util.printDeep(offset);
//		Image im = getRawImage();
//		assert im != null;
//		assert isPlaced();
//		return TranslateDescriptor.create((RenderedImage) getRawImage(), new Float(offset[0]),
//				new Float(offset[1]), Interpolation
//						.getInstance(Interpolation.INTERP_NEAREST), null);
//	}
//
//	void initCeiling(int w) {
//		ceilingIntervals = new ArrayList();
//		ceilingIntervals.add(new Integer(0));
//		ceilingIntervals.add(new Integer(w));
//		maxCeilingInterval = w;
//	}
//
//	int updateLegalIntervals(MyTile tile) {
//		Image3D.intersectInterval(ceilingIntervals, tile.rawX(), tile
//				.rawRight());
//		maxCeilingInterval = 0;
//		for (ListIterator i = ceilingIntervals.listIterator(); i.hasNext();) {
//			Integer intLeft = (Integer) i.next();
//			int iLeft = intLeft.intValue();
//			Integer intRight = (Integer) i.next();
//			int iRight = intRight.intValue();
//			if (iLeft >= rawRight() || iRight == iLeft) {
//				i.remove();
//				i.previous();
//				i.remove();
//			} else
//				maxCeilingInterval = Math.max(maxCeilingInterval, iRight
//						- iLeft);
//		}
//		return maxCeilingInterval;
//	}
//
//	// public BufferedImage getScaledImage(float _scale) {
//	// BufferedImage result = getRawImage();
//	// if (_scale != 1) {
//	// if (_scale != scale) {
//	// scale = _scale;
//	// Image im = result.getScaledInstance((int) (rawW() / scale), -1,
//	// Image.SCALE_FAST);
//	// assert im != null;
//	// Image3D.ensureLoaded(im);
//	// scaledImage = PImage.toBufferedImage(im, false);
//	// }
//	// result = scaledImage;
//	// }
//	// return result;
//	// }
//
//	// public int X() {
//	// return (int) (rawX() / scale);
//	// }
//	//
//	// public int Y() {
//	// return (int) (rawY() / scale);
//	// }
//	//
//	// public int W() {
//	// return (int) (rawW() / scale);
//	// }
//	//
//	// public int H() {
//	// return (int) (rawH() / scale);
//	// }
//}
//
//class HeightComparator extends ValueComparator {
//
//	public int value(Object data) {
//		return ((MyTile) data).rawH();
//	}
//}
//
//class WidthComparator extends ValueComparator {
//
//	public int value(Object data) {
//		return ((MyTile) data).rawW();
//	}
//}
//
//class AreaComparator extends ValueComparator {
//
//	public int value(Object data) {
//		MyTile tile = (MyTile) data;
//		return tile.rawW() * tile.rawH();
//	}
//}
//
//class PerimeterComparator extends ValueComparator {
//
//	public int value(Object data) {
//		MyTile tile = (MyTile) data;
//		return tile.rawW() + tile.rawH();
//	}
//}
//
//class TopComparator implements Comparator {
//
//	public int compare(Object data1, Object data2) {
//		MyTile tile1 = (MyTile) data1;
//		MyTile tile2 = (MyTile) data2;
//		int result = tile1.rawTop() - tile2.rawTop();
//		if (result == 0 && !data1.equals(data2))
//			result = tile1.item() - tile2.item();
//		return result;
//	}
//
//}
//
//public class Image3D {
//
//	final static int minSize = 50;
//
//	final static int facetBorder = 3;
//
//	final static int onOffBorder = 2;
//
////	final static int imageBorder = 0;
//
//	final static boolean loadThumbsNow = true;
//
//	Art art;
//
//	Perspective facet;
//
//	MyTile[] allTiles;
//
//	MyTile[] onTiles;
//
//	MyTile[] offTiles;
//
//	MyTile[] placedTiles;
//
//	int w;
//
//	int h;
//
//	float scale = 1;
//
//	private int maxOnHeight;
//
//	private int maxOffHeight;
//
//	static int maxImageW = 500;
//
//	Image3D(Art _art, Perspective _facet) {
//		art = _art;
//		facet = _facet;
//	}
//
//	MyTile[] collage(int x, int _w, int onH, int offH) {
//		// int h = onH;
//		// boolean isOn = true;
//		// if (offH > onH) {
//		// h = offH;
//		// isOn = false;
//		// }
//		_w -= facetBorder;
//		x += facetBorder / 2;
//		int border = Math.min(onOffBorder, Math.min(onH, offH));
//		onH -= border / 2;
//		offH -= border / 2;
//		allocateTiles(_w, onH, offH);
//		w = (int) Math.ceil(_w * scale);
//		h = (int) Math.ceil((onH + offH + border) * scale);
//		// getTiles(w - facetBorder, h - onOffBorder, onH, offH, isOn);
//
//		// GraphicsConfiguration graphicsConfiguration = GraphicsEnvironment
//		// .getLocalGraphicsEnvironment().getDefaultScreenDevice()
//		// .getDefaultConfiguration();
//		// BufferedImage result = graphicsConfiguration.createCompatibleImage(
//		// scaleIt(w), scaleIt(onH + offH), Transparency.TRANSLUCENT);
//		// WritableRaster raster = result.getRaster();
//
//		// Util.print("raster size " + raster.getWidth() + " "
//		// + raster.getHeight());
//
//		placedTiles = (MyTile[]) Util.append(getTiles(offTiles, x, _w, offH, 0,
//				maxOffHeight), getTiles(onTiles, x, _w, onH, offH + border,
//				maxOnHeight), MyTile.class);
//		return placedTiles;
//	}
//
//	MyTile[] getTiles(MyTile[] tiles, int xOffset, int w, int h, int yOffset,
//			int maxH) {
//		if (h > Bar.minImageSize) {
//			w *= scale;
//			h *= scale;
//			xOffset *= scale;
//			yOffset *= scale;
//			Util.print("BLD " + facet + " " + w + "x" + h + " y=" + yOffset
//					+ " n available tiles=" + tiles.length);
//			return BLD(tiles, xOffset, w, h, yOffset, maxH);
//		} else
//			return null;
//	}
//
//	void allocateTiles(int w, int onH, int offH) {
//		int[][] histograms = new int[2][maxImageW];
//		int[][] areas = new int[2][maxImageW];
//		int[] totalAreas = new int[2];
//		int[] maxHeights = new int[2];
//		int[] indexes = new int[2];
//		int[] heights = { onH, offH };
//		scale = 1;
//		MyTile[] allTiles = null; //getAllTiles();
//		int nTiles = allTiles == null ? 0 : allTiles.length;
//		MyTile[][] tiles = new MyTile[2][nTiles];
//		int minW = 999999;
//		int maxW = -1;
//		for (int i = 0; i < nTiles; i++) {
//			MyTile tile = allTiles[i];
//			tile.unplace();
//			int rawW = tile.rawW();
//			if (rawW >= maxImageW) {
//				maxImageW *= 2;
//				allocateTiles(w, onH, offH);
//				return;
//			}
//			int rawH = tile.rawH();
//			int rawArea = rawW * rawH;
//			int index = art.isOn(tile.item()) ? 0 : 1;
//			tiles[index][indexes[index]++] = tile;
//			totalAreas[index] += rawArea;
//			histograms[index][rawW]++;
//			areas[index][rawW] += rawArea;
//			if (rawH > maxHeights[index])
//				maxHeights[index] = rawH;
//			if (rawW > maxW)
//				maxW = rawW;
//			if (rawW < minW)
//				minW = rawW;
//		}
//		int[] cumCounts = new int[2];
//		assert minW > 0;
//		for (int index = 0; index < 2; index++) {
//			tiles[index] = (MyTile[]) Util.subArray(tiles[index], 0,
//					indexes[index] - 1, MyTile.class);
//			cumCounts[index] = indexes[index] / 2;
//		}
//		onTiles = tiles[0];
//		offTiles = tiles[1];
//		maxOnHeight = maxHeights[0];
//		maxOffHeight = maxHeights[1];
//		int[] cumAreas = { 0, 0 };
//		int desiredNcols = Math.max(1, Math.round(w / (float) minSize));
//		float bestShortfall = 9999999;
//		float bestScale = 1;
//		int cumIndex = minW;
//		boolean found = false;
//		for (int medianW = minW; medianW <= maxW && !found; medianW++) {
//			if (histograms[0][medianW] > 0 || histograms[1][medianW] > 0) {
//				scale = (medianW * desiredNcols) / (float) w;
//				float x = w * scale * scale;
//				float shortfall = 0;
//				for (; cumIndex <= medianW * desiredNcols && cumIndex <= maxW; cumIndex++) {
//					for (int index = 0; index < 2; index++)
//						cumAreas[index] += areas[index][cumIndex];
//				}
//				for (int index = 0; index < 2; index++) {
//					cumCounts[index] -= histograms[index][medianW];
//					if (heights[index] > 0)
//						shortfall += heights[index]
//								* Math.max(0, 1 - cumAreas[index]
//										/ (heights[index] * x));
//				}
//				if (shortfall < bestShortfall) {
//					bestShortfall = shortfall;
//					bestScale = scale;
//				}
//				// Util.print(n[0] + " " + areas[0] + " " + (onH * x));
//				if (cumCounts[0] <= 0 && cumCounts[1] <= 0 && shortfall == 0) {
//					// Util.print("allocateTiles " + w + "x" + (onH + offH) + "
//					// "
//					// + medianW + " " + desiredNcols + " " + scale);
//					found = true;
//					;
//				}
//				// Util.print(medianW + ". " + desiredNcols + " " + scale + " "
//				// + (cumAreas[0] + cumAreas[1]) + " " + shortfall);
//			}
//		}
//		if (!found) {
//			// Util.print(maxW + " " + desiredNcols + " " + scale + " "
//			// + (cumAreas[0] + cumAreas[1]));
//			scale = bestScale;
//		}
//		Util.print("allocateTiles: desiredNcols=" + desiredNcols + "; scale="
//				+ scale + "; width range: " + minW + "-" + maxW
//				+ "\nestimated weighted shortfall: "
//				+ (bestShortfall / (onH + offH)) + " at scale=" + bestScale);
//		scale = Math.max(1, scale);
//		Util.print("on coverage if all images fit: " + cumAreas[0]
//				/ (heights[0] * w * scale * scale) + " (n="
//				+ (indexes[0] / 2 - cumCounts[0]) + "; area=" + cumAreas[0]
//				+ ")");
//		Util.print("off coverage if all images fit: " + cumAreas[1]
//				/ (heights[1] * w * scale * scale) + " (n="
//				+ (indexes[1] / 2 - cumCounts[1]) + "; area=" + cumAreas[1]
//				+ ")");
//	}
//
//	static MyTile[] BLD(MyTile[] tiles, int x, int w, int h, int y, int maxH) {
//		assert h > Bar.minImageSize ;
//		assert w > Bar.minImageSize;
//		int area = -1;
//		ValueComparator order = null;
//		ValueComparator[] orders = { MyTile.heightComparator // ,
//		// MyTile.widthComparator,
//		// MyTile.areaComparator,
//		// MyTile.perimeterComparator
//		};
//		for (int i = 0; i < orders.length; i++) {
//			Arrays.sort(tiles, orders[i]);
//			int a1 = BLDinternal(tiles, w, h, maxH);
//			if (a1 >= area) {
//				area = a1;
//				order = orders[i];
//			}
//		}
//		if (order != orders[orders.length - 1]) {
//			Arrays.sort(tiles, order);
//			BLDinternal(tiles, w, h, maxH);
//		}
//		tiles = invert(tiles, h, x, y);
//		Util.print("BLD return " + tiles.length + " images placed. Coverage="
//				+ (area / (float) (w * h)));
//		return tiles;
//	}
//
//	static int BLDinternal(MyTile[] tiles, int w, int h, int maxH) {
//		int area = 0;
//		int n = 0;
//		TreeSet placed = new TreeSet(MyTile.topComparator);
//		MyTile fakeBottom = new MyTile(0, 0, w, 0);
//		fakeBottom.initCeiling(w);
//		// MyTile fakeLeft= new MyTile();
//		// fakeLeft.setOffset(0, 1);
//		placed.add(fakeBottom);
//		// placed.add(fakeLeft);
//		MyTile fakeTop = new MyTile(0, h, 0, 0);
//		SortedSet possibleBottoms = placed.headSet(fakeTop);
//		MyTile fakeLeftTop = new MyTile(0, 0, 0, 0);
//		for (int i = 0; i < tiles.length; i++) {
//			MyTile tile = tiles[i];
//			// tile.unplace();
//			int tileW = tile.rawW();
//			if (tileW <= w) {
//				int tileH = tile.rawH();
//				// Util.print(i + " " + tileW + "x" + tileH + " "
//				// + possibleBottoms.size() + " " + placed.size());
//				for (Iterator bottomIterator = possibleBottoms.iterator(); bottomIterator
//						.hasNext()
//						&& !tile.isPlaced();) {
//					MyTile bottomTile = (MyTile) bottomIterator.next();
//					int bottom = bottomTile.rawTop();
//					assert bottom <= tile.rawY() && bottom < h : bottom + " "
//							+ h;
//					int minLeft = Math.max(0, bottomTile.rawX() - tileW);
//					int maxRight = Math.min(w - tileW, bottomTile.rawRight());
//					if (minLeft <= maxRight
//							&& bottomTile.maxCeilingInterval >= tileW
//							&& bottom + tileH <= h) {
//						// Util
//						// .print(" "
//						// + bottom
//						// + " "
//						// + bottomTile.maxCeilingInterval
//						// + " "
//						// + Util
//						// .valueOfDeep(bottomTile.ceilingIntervals));
//						TreeSet possibleLefts = new TreeSet();
//						ArrayList legalIntervals = new ArrayList();
//						legalIntervals.add(new Integer(minLeft));
//						legalIntervals.add(new Integer(maxRight));
//						if (minLeft <= 0)
//							possibleLefts.add(new Integer(0));
//						fakeLeftTop.setOffset(0, bottom + tileH + maxH);
//						// Util.print(placed.subSet(bottomTile, fakeLeftTop)
//						// .size());
//						for (Iterator leftIterator = placed.subSet(bottomTile,
//								fakeLeftTop).iterator(); leftIterator.hasNext()
//								&& legalIntervals.size() > 0;) {
//							MyTile leftTile = (MyTile) leftIterator.next();
//							assert leftTile.rawTop() >= bottom;
//							if (leftTile.rawTop() > bottom
//									&& leftTile.rawY() < bottom + tileH) {
//								if (leftTile.rawY() <= bottom)
//									bottomTile.updateLegalIntervals(leftTile);
//								// Util.print(" " + left);
//								if (intersectInterval(legalIntervals, leftTile
//										.rawX()
//										- tileW, leftTile.rawRight()))
//									possibleLefts.add(new Integer(leftTile
//											.rawRight()));
//							}
//						}
//						for (Iterator it = possibleLefts.iterator(); it
//								.hasNext()
//								&& !tile.isPlaced();) {
//							int left = ((Integer) it.next()).intValue();
//							if (inRange(legalIntervals, left)) {
//								tile.setOffset(left, bottom);
//								placed.add(tile);
//								tile.initCeiling(w);
//								n++;
//								area += tile.rawW()
//										* (Math.min(tile.rawTop(), h) - tile
//												.rawY());
//								// Util.print("placing " + i + ". " + tileW +
//								// "x"
//								// + tileH + " " + tile.rawX() + ", "
//								// + tile.rawY());
//							}
//						}
//					}
//				}
//			}
//		}
//		return area;
//	}
//
//	// layout tiles bottom-to-top
//	static MyTile[] invert(MyTile[] tiles, int h, int x, int y) {
//		List result = new ArrayList(Arrays.asList(tiles));
//		int maxTop = 0, minBottom = 999999;
//		for (ListIterator it = result.listIterator(); it.hasNext();) {
//			MyTile tile = (MyTile) it.next();
//			if (tile.isPlaced()) {
//				tile.setOffset(x + tile.rawX(), y + h - tile.rawTop());
//				maxTop = Math.max(maxTop, tile.rawTop());
//				minBottom = Math.min(minBottom, tile.rawY());
//				assert tile.rawY() >= y;
//				assert tile.rawTop() <= y + h;
//			} else
//				it.remove();
//		}
////		Util.print("maxTop=" + maxTop + " " + minBottom);
//		return (MyTile[]) result.toArray(new MyTile[0]);
//	}
//
//	static boolean intersectInterval(List interval, int left, int right) {
//		// Util.print("intersectInterval " + left + " " + right + " "
//		// + Util.valueOfDeep(interval));
//		boolean result = false;
//		for (ListIterator i = interval.listIterator(); i.hasNext();) {
//			Integer intLeft = (Integer) i.next();
//			Integer intRight = (Integer) i.next();
//			int iLeft = intLeft.intValue();
//			int iRight = intRight.intValue();
//			if (iLeft <= right && left < iRight) {
//				if (right <= iRight) {
//					result = true;
//					i.previous();
//					i.previous();
//					i.set(new Integer(right));
//					i.next();
//					i.next();
//					if (iLeft <= left) {
//						i.add(intLeft);
//						i.add(new Integer(left));
//					}
//				} else if (left < iLeft) {
//					i.remove();
//					i.previous();
//					i.remove();
//				} else
//					i.set(new Integer(left));
//			}
//		}
//		// Util.print("intersectInterval return "
//		// + Util.valueOfDeep(interval));
//		return result;
//	}
//
//	static boolean inRange(List interval, int x) {
//		boolean result = false;
//		for (Iterator i = interval.iterator(); i.hasNext() && !result;) {
//			Integer intLeft = (Integer) i.next();
//			Integer intRight = (Integer) i.next();
//			int iLeft = intLeft.intValue();
//			int iRight = intRight.intValue();
//			result = iLeft <= x && iRight >= x;
//		}
//		return result;
//	}
//
////	MyTile[] getAllTiles() {
////		// Util.print("getAllTiles " + facet + " " + facet.totalCount);
////		if (allTiles == null && facet.totalCount >= 0) {
////			GridImage[] images = art.getAllThumbs(facet);
////			int index = 0;
////			allTiles = new MyTile[images.length];
////			for (int i = 0; i < images.length; i++) {
////				GridImage im = images[i];
////				if (im != null && im.rawW > 0)
////					allTiles[index++] = new MyTile(im);
////			}
////			allTiles = (MyTile[]) Util.subArray(allTiles, 0, index - 1,
////					MyTile.class);
////		}
//////		Util.print("getAllTiles return " + allTiles.length);
////		return allTiles;
////	}
//
////	MyTile[] collageInternal(int w, int h, int onH, int offH, boolean isOn,
////			BufferedImage result, int y, WritableRaster raster) {
////		MyTile[] images = null;
////		w -= facetBorder;
////		if (h > Bar.minImageSize) {
////			// int nColors = result.getColorModel().getNumColorComponents();
////			// long loadTime = 0;
////			// long scaleTime = 0;
////			// long grayTime = 0;
////			// long writeTime = 0;
////			// int nWritten = 0;
////			// int x = facetBorder / 2;
////			// loadTime -= new Date().getTime();
////			// images = getTiles(w, h, y, onH, offH, isOn);
////			// loadTime += new Date().getTime();
////			// y = scaleIt(y);
////			// // Util.print("\nenter collageInternal scale=" + scale + " " + w
////			// +
////			// // "x"
////			// // + h + " <" + x + ", " + y + ">");
////			// for (int i = 0; i < images.length; i++) {
////			// MyTile tile = images[i];
////			// assert tile.isPlaced();
////			// nWritten++;
////			// scaleTime -= new Date().getTime();
////			// BufferedImage i1 = tile.getScaledImage(1); // scale);
////			// // Util.print("... <" + (x + tile.X()) + ", " + (y +
////			// // tile.Y())
////			// // + "> " + tile.W() + "x" + tile.H());
////			// scaleTime += new Date().getTime();
////			// ensureLoaded(i1);
////			// grayTime -= new Date().getTime();
////			// RenderedImage i2 = PImage.toBufferedImage(i1, !i1
////			// .getColorModel().isCompatibleRaster(raster));
////			// grayTime += new Date().getTime();
////			// writeTime -= new Date().getTime();
////			// int bottom = tile.Y();
////			// int left = scaleIt(x) + tile.X();
////			// int top = bottom + i2.getHeight();
////			// int width = i2.getWidth();
////			// int height = top - Math.max(y, bottom);
////			// // bottom = scaleIt(2 * y + h) - height - bottom; // draw
////			// // bottom-to-top
////			// Raster sub = (bottom < y) ? i2.getData(new Rectangle(width,
////			// height)) : i2.getData();
////			// bottom = Math.max(y, bottom);
////			// // Util.print("(" + left + ", " + bottom + ") "
////			// // + width + "x" + height + " " + (bottom+height));
////			// raster.setDataElements(left, bottom, sub);
////			// int[] samples = new int[width * height];
////			// int[] alpha = new int[samples.length];
////			// Arrays.fill(alpha, 255);
////			// raster.setSamples(left, bottom, width, height, nColors,
////			// alpha);
////			// writeTime += new Date().getTime();
////			// }
////			// Util.print("exit collageInternal " + y + " " + raster.getHeight()
////			// + " " + nWritten + ": " + loadTime + " " + scaleTime + " " // +
////			// // grayTime
////			// // + "
////			// // "
////			// + writeTime);
////			// Util.print("collageInternal " + images.length);
////		}
////		return images;
////	}
////
////	int scaleIt(int coord) {
////		return (int) (scale * coord);
////	}
////
////	static void ensureLoaded(Image im) {
////		Component obs = new Label();
////		while (im.getHeight(obs) < 0) {
////			try {
////				Thread.sleep(100);
////			} catch (InterruptedException e) {
////				// TODO Auto-generated catch block
////				e.printStackTrace();
////			}
////		}
////	}
//
//	static RenderedOp project(RenderedImage srcImage, double x0, double y0,
//			double x1, double y1, double x2, double y2, double x3, double y3,
//			double x0p, double y0p, double x1p, double y1p, double x2p,
//			double y2p, double x3p, double y3p) {
//		ParameterBlock pb = projectParams(x0, y0, x1, y1, x2, y2, x3, y3, x0p,
//				y0p, x1p, y1p, x2p, y2p, x3p, y3p);
//		pb.addSource(srcImage);
//		RenderedOp dstImage = JAI.create("warp", pb);
//		return dstImage;
//	}
//
//	static ParameterBlock projectParams(double x0, double y0, double x1,
//			double y1, double x2, double y2, double x3, double y3, double x0p,
//			double y0p, double x1p, double y1p, double x2p, double y2p,
//			double x3p, double y3p) {
//		// Util.print(x0 + ", " + y0 + ", " + x1 + ", " + y1 + ", " + x2 + ", "
//		// + y2 + ", " + x3 + ", " + y3 + ", " + x0p + ", " + y0p + ", "
//		// + x1p + ", " + y1p + ", " + x2p + ", " + y2p + ", " + x3p
//		// + ", " + y3p);
//		PerspectiveTransform transform = PerspectiveTransform.getQuadToQuad(x0,
//				y0, x1, y1, x2, y2, x3, y3, x0p, y0p, x1p, y1p, x2p, y2p, x3p,
//				y3p);
//		// transform = new PerspectiveTransform();
//		assert transform.getDeterminant() != 0 : x0 + ", " + y0 + ", " + x1
//				+ ", " + y1 + ", " + x2 + ", " + y2 + ", " + x3 + ", " + y3
//				+ ", " + x0p + ", " + y0p + ", " + x1p + ", " + y1p + ", "
//				+ x2p + ", " + y2p + ", " + x3p + ", " + y3p;
//		WarpPerspective warp = new WarpPerspective(transform);
//		ParameterBlock pb = new ParameterBlock();
//		// pb.addSource(srcImage);
//		pb.add(warp);
//		pb.add(new InterpolationNearest());
//		// Util.print(describeParameters(pb));
//		return pb;
//	}
//
//	// This uses JAI because it doesn't wash out colors when resizing grayscale
//	// images
////	static RenderedOp scaleTranslate(RenderedImage srcImage, double x,
////			double y, double w, double h) {
////		double srcW = srcImage.getWidth();
////		double srcH = srcImage.getHeight();
////		return project(srcImage, x, y, x + w, y, x + w, y + h, x, y + h, 0, 0,
////				srcW, 0, srcW, srcH, 0, srcH);
////	}
//
//	static String describeParameters(ParameterBlock pb) {
//		Vector parameters = pb.getParameters();
//		int n = parameters.size();
//		for (int i = 0; i < n; i++) {
//			if (parameters.get(i) instanceof WarpPerspective)
//				Util.printDeep(((WarpPerspective) parameters.get(i))
//						.getTransform().getMatrix(new double[3][3]));
//		}
//		return Util.valueOfDeep(parameters);
//	}
//
////	static RenderedOp toGrayscale(BufferedImage srcImage, ColorModel srcCM,
////			ColorSpace dstCS) {
////		// Util.print("");
////		// Util.printDeep(srcImage.getData().getPixel(3, 3, (int[])
////		// null));
////		// RenderedImage result = new ColorConvertOp(srcCM.getColorSpace(),
////		// dstCS,
////		// null).filter(srcImage, null);
////
////		int dstNColorBands = dstCS.getNumComponents();
////		int nColorBands = srcCM.getNumColorComponents();
////		int nBands = srcCM.getNumComponents() + 1;
////		double[][] matrix1 = new double[dstNColorBands][nBands];
////		for (int b = 0; b < dstNColorBands; b++)
////			for (int i = 0; i < nColorBands; i++)
////				matrix1[b][i] = 1. / nColorBands;
////		// Util.printDeep(matrix1);
////		ParameterBlock pb1 = new ParameterBlock();
////		pb1.addSource(srcImage);
////		pb1.add(matrix1);
////		RenderedOp result2 = JAI.create("bandcombine", pb1, null);
////		// Util.printDeep(srcImage.getData().getPixel(3, 3, (int[])
////		// null));
////		// Util.printDeep(result.getData().getPixel(3, 3, (int[]) null));
////		// Util.printDeep(result2.getData().getPixel(3, 3, (int[])
////		// null));
////		// for (int i = 0; i < result.getWidth(); i++)
////		// for (int j = 0; j < result.getHeight(); j++) {
////		// int src = result2.getData().getPixel(i, j, (int[]) null)[0];
////		// int dst = result.getData().getPixel(i, j, (int[]) null)[0];
////		// mm[src][0] = src;
////		// mm[src][1] = dst;
////		// }
////		return result2;
////
////		// ColorModel oldModel = srcImage.getColorModel();
////		// int pixelSize = oldModel.getPixelSize();
////		// int transferType = oldModel.getTransferType();
////		// // Create a grayscale color model.
////		// ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
////		// int bits[] = new int[] { pixelSize, pixelSize };
////		// ColorModel cm = new ComponentColorModel(cs, bits, true, true,
////		// Transparency.TRANSLUCENT, transferType);
////		// return JAI.create("ColorConvert", srcImage, cm);
////	}
//}