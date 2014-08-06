/** Utilities using the Java3D libraries.
 * 
 */

package imageNav;

import java.awt.*;
import java.awt.image.*;

import javax.swing.*;
import javax.vecmath.*;

import mas.utils.Utils;

public abstract class ImageNavUtils {
	public static final int defaultBrightnessThreshold = 100;

	/** Computes the brightness (defined as the average of the RGB values) of a pixel
	 * 
	 * @param xPos
	 * @param yPos
	 * @param pixels
	 * @param ras
	 * @return
	 */
	public static int checkBrightness(int xPos, int yPos, int[] pixels, Raster ras) {
		ras.getPixel(xPos, yPos, pixels);
		return((pixels[0] + pixels[1] + pixels[2])/3);
	}

	/** Takes an image and washes it out to black or white (high-contrast)
	 * 
	 * @param img
	 * @return
	 */
	public static BufferedImage washoutImage(BufferedImage img, Raster ras, int threshold) {
		int[] pixs = new int[4];
		for (int i=0; i < img.getWidth(); i++) {
			for (int j=0; j < img.getHeight(); j++) {
				// Compare total brightness to a threshold value
				if (checkBrightness(i, j, pixs, ras) > threshold) {
					img.setRGB(i, j, Integer.MAX_VALUE);
				} else {
					img.setRGB(i, j, Integer.MIN_VALUE);
				}
			}
		}
		return(img);
	}

	/** Takes an image and washes it out to black or white (high-contrast)
	 * 
	 * @param img
	 * @return
	 */
	public static BufferedImage washoutImage(BufferedImage img, int threshold) {
		return(washoutImage(img, img.getData(), threshold));
	}

	/** Finds the center of brightness of the given image
	 * 
	 * @param img
	 * @return
	 */
	public static double[] findCentroid(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();
		double centroidX = 0;
		double centroidY = 0;
		int totalBright = 0;
		int[] pixs = new int[4];
		for (int i=0; i < width; i++) {
			for (int j=0; j < height; j++) {
				int bright = checkBrightness(i, j, pixs, img.getData());
				centroidX = centroidX + i*bright;
				centroidY = centroidY + j*bright;
				totalBright = totalBright + bright;
			}
		}
		return(new double[] {centroidX / totalBright, centroidY / totalBright});
	}

	/** Do all these things at once to save processor time.
	 * 
	 * @param img
	 * @param threshold
	 * @param centroid
	 * @param inverseCentroid
	 * @return
	 */
	public static BufferedImage washoutImageAndFindCentroids(BufferedImage img,
			Raster ras, int threshold, double[] centroid, double[] inverseCentroid) {
		int[] pixs = new int[4];
		int width = img.getWidth();
		int height = img.getHeight();
		double centroidX = 0;
		double centroidY = 0;
		int totalBright = 0;
		int bright = 10;		// Default value; it doesn't matter since we're washing
		double invCentroidX = 0;
		double invCentroidY = 0;
		int invTotalBright = 0;
		for (int i=0; i < width; i++) {
			for (int j=0; j < height; j++) {
				// Compare total brightness to a threshold value
				if (checkBrightness(i, j, pixs, ras) > threshold) {
					img.setRGB(i, j, Integer.MAX_VALUE);
					// Since we're washing to the same value, set all to one
					centroidX = centroidX + i*bright;
					centroidY = centroidY + j*bright;
					totalBright = totalBright + bright;
					// InverseCentroid gets nothing
				} else {
					img.setRGB(i, j, Integer.MIN_VALUE);
					// Add to the inverse
					invCentroidX = invCentroidX + i*bright;
					invCentroidY = invCentroidY + j*bright;
					invTotalBright = invTotalBright + bright;
					// centroid value is zero
				}
			}
		}
		// Set centroid
		centroid[0] = centroidX / totalBright;
		centroid[1] = centroidY / totalBright;
		// Set inverse
		inverseCentroid[0] = invCentroidX / invTotalBright;
		inverseCentroid[1] = invCentroidY / invTotalBright;
		return(img);		
	}

	/** Finds the center of darkness of a given image.
	 * 
	 * @param img
	 * @return
	 */
	public static double[] findInverseCentroid(BufferedImage img) {
		Raster ras = img.getData();
		double centroidX = 0;
		double centroidY = 0;
		int totalBright = 0;
		int[] pixs = new int[4];
		for (int i=0; i < img.getWidth(); i++) {
			for (int j=0; j < img.getHeight(); j++) {
				int bright = 255 - checkBrightness(i, j, pixs, ras);
				centroidX = centroidX + i*bright;
				centroidY = centroidY + j*bright;
				totalBright = totalBright + bright;
			}
		}
		return(new double[] {centroidX / totalBright, centroidY / totalBright});
	}

	/** Sticks the image into a frame and pops it open.
	 * 
	 * @param img
	 * @param title
	 * @return
	 */
	public static JFrame imageInFrame(BufferedImage img, String title) {
		JFrame jf = new JFrame(title);
		jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		jf.getContentPane().add(new JLabel(new ImageIcon(img)));
		jf.pack();
		jf.validate();
		jf.setVisible(true);
		return(jf);
	}


	/** Apply a simple filter to an image to only show the desired color.
	 * 
	 * @param source -- source image
	 * @param filter -- the color to screen for
	 * @param colorMatchDistance -- the allowed rgb variance from the desired color
	 * @param showImages -- flag to display the before/after side by side
	 * @return -- the recolored image, with the desired color as white and all else
	 * 	black.
	 */
	public static BufferedImage colorFilter(BufferedImage source, Color3f filter, 
			int colorMatchDistance, boolean showImages) {
		BufferedImage filteredImage = new BufferedImage(source.getWidth(),
				source.getHeight(), BufferedImage.TYPE_INT_RGB);
		double[] recolor = new double[3];
		for (int i=0; i < source.getWidth(); i++) {
			for (int j=0; j < source.getHeight(); j++) {
				int rgb = source.getRGB(i, j);
				int r = (rgb >> 16) & 0xff;
				int g = (rgb >> 8) & 0xff;
				int b = rgb & 0xff;
				/* Look for colors that are "close enough" to our desired color,
				 * as indicated by the rgb 'distance' between the colors.
				 */

				// Compute the difference in each of the r, g, b values
				recolor[0] = (r - 256*filter.x);
				recolor[1] = (g - 256*filter.y);
				recolor[2] = (b - 256*filter.z);
				// Now compute the std dev from the filter
//				double sigma = Math.sqrt(Utils.variance(recolor));
				// Compute the "distance" between the desired color and this color
				double sigma = Utils.norm2(recolor);
				// Wipe out any pixel that doesn't fit the color scheme
				if (sigma > colorMatchDistance) {
					rgb = 0;
				} else {
					// Set it to white
					rgb = -1;
				}
				filteredImage.setRGB(i, j, rgb);
			}
		}
		if (showImages) {
			JFrame jf = new JFrame("Side by side with filter " + filter);
			jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			jf.getContentPane().setLayout(new GridLayout(1,2));
			jf.getContentPane().add(new JLabel(new ImageIcon(source)));
			jf.getContentPane().add(new JLabel(new ImageIcon(filteredImage)));
			jf.pack();
			jf.setVisible(true);
		}
		return(filteredImage);
	}

	/** Apply a simple filter to an image to only show the desired color.
	 * 
	 * @param source -- source image
	 * @param filterType -- which color to allow (RED_FILTER, GREEN_FILTER, BLUE_FILTER)
	 * @return -- the recolored image, with the desired color as white and all else
	 * 	black.
	 */
	public static BufferedImage colorFilter(BufferedImage source, int brightnessThreshold,
			Color3f filter) {
		return(colorFilter(source, filter, brightnessThreshold, false));
	}
}
