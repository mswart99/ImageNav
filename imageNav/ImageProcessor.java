package imageNav;

import java.awt.image.*;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import mas.utils.Utils;

/** A container for methods associated with image processing applications.  The purpose
 *  of the container is to hold all the standard objects to cut down on memory usage.
 *  
 * @author swartwoutm
 *
 */
public class ImageProcessor {
	public static final int WASHOUT_THRESHOLD = 50;
	Vector3d limbVector = new Vector3d();
	Point3d limbCenter = new Point3d();
	Vector3d centroidVector = new Vector3d();
	Vector3d crossTemp = new Vector3d();

	public ImageProcessor() {
		super();
	}
	
	/** Given an image of what is presumed to be the horizon, this method first
	 *  identifies the horizon, and then computes the desired angular velocity 
	 *  (in camera coordinates) to align the computed horizon with the provided
	 *  target.
	 * @param img
	 * @param targetCenter -- this method only uses the y-coordinate
	 * @param targetVector -- aligned such that, when the image is arranged so the
	 * Earth is "down", the vector points left.  The z-coordinate is not used.
	 * @return
	 */
	public Vector3d levelHorizonController(BufferedImage img,
			Point3d targetCenter, Vector3d targetVector, Vector3d omega_desired,
			boolean debug) {
		double[] lightCentroid = new double[2];
		double[] darkCentroid = new double[2];
		if (debug) {
			ImageNavUtils.imageInFrame(img, "Before");
		}
		ImageNavUtils.washoutImageAndFindCentroids(img, img.getData(), WASHOUT_THRESHOLD,
				lightCentroid, darkCentroid);
		// Compute the centroid of this image -- used later
		double[] leftPts = new double[] {-1, -1};
		double[] rightPts = new double[] {0, 0};
		findHorizon(img, leftPts, rightPts);
		/* If we cannot find the horizon, we do not attempt control.  We signal this
		 * by passing a null vector, indicating we have no solution.
		 */
		if (leftPts[0] + leftPts[1] < 0) {
			omega_desired.set(Double.MAX_VALUE, 0, 0);
			return(omega_desired);
		}
		/* Next, we define two lines:  the limb vector (from one side of the screen
		 * to the other) and the centroid vector (from dark to light).  Our goal is
		 * to define the limb vector so that it goes "left" from the perspective of
		 * the light side of the screen.
		 */
		limbVector.set(leftPts[0]-rightPts[0], 
				leftPts[1]-rightPts[1], 0);
		limbVector.normalize();
		limbCenter.set((leftPts[0]+rightPts[0])/2,
				(leftPts[1]+rightPts[1])/2, 0);
		centroidVector.set(darkCentroid[0]-lightCentroid[0], 
				darkCentroid[1]-lightCentroid[1], 0);
		/* The proper orientation of limbVector is to be in the same direction as
		 * the cross product of the 3-axis with the centroid vector.  (Draw the 
	     * picture yourself and see.)
	     */
		if (limbVector.x*centroidVector.y-centroidVector.x*limbVector.y < 0) {
			limbVector.scale(-1);
		}
		/* Our desired angular velocity about the camera boresight is a function
		 * of the dot product of limbVector and targetVector -- a big omega_boresight
		 * when they are 180 degrees apart, almost nothing when they are almost aligned.
		 * So we get the magnitude from their dot product, and the sign from their cross
		 * product.  This is unscaled -- max value is 1
		 */
		crossTemp.cross(limbVector, targetVector);
		double omega_boresight = Math.signum(crossTemp.z)*(1-limbVector.dot(targetVector))/2;
		/* The desired angular velocity about the pitch axis is proportional 
		 * to the vertical offset between the target horizon and our current edge.  This
		 * is scaled such that the max value (if target center is at top of screen) is 1.
		 */
		double omega_pitch = (targetCenter.y - limbCenter.y)/(img.getHeight()/2)/10;
		// In camera-frame, the coords are (pitch-down, yaw-left, roll-CW)
		omega_desired.set(omega_pitch, 0, omega_boresight);
		if (debug) {
			int[] targetColors = {0, 255, 0};
			int[] limbColors = {255, 0, 0};
			Vector3d lv = new Vector3d(limbVector);
			lv.normalize();
			WritableRaster rast = img.getRaster();
			for (int i=-5; i < 5; i++) {
				try {
					rast.setPixel((int) targetCenter.x + i, 
							(int) targetCenter.y, 
							targetColors);
				} catch (ArrayIndexOutOfBoundsException aioobe) { }
				try {
					rast.setPixel((int) targetCenter.x, 
							(int) targetCenter.y + i, 
							targetColors);
				} catch (ArrayIndexOutOfBoundsException aioobe) { }
				try {
					rast.setPixel((int) limbCenter.x + i, 
							(int) limbCenter.y, 
							limbColors);
				} catch (ArrayIndexOutOfBoundsException aioobe) { }
				try {
					rast.setPixel((int) limbCenter.x, 
							(int) limbCenter.y + i, 
							limbColors);
				} catch (ArrayIndexOutOfBoundsException aioobe) { }
			}
			for (int i=0; i < 100; i++) {
				try {
					rast.setPixel((int) (limbCenter.x + i * lv.x), 
							(int) (limbCenter.y + i * lv.y), 
							limbColors);				
				} catch (ArrayIndexOutOfBoundsException aioobe) { }
			}
			ImageNavUtils.imageInFrame(img, "Diagnostic");
		}
		return(omega_desired); 
	}
	
	protected BufferedImage lineOnly;
	
	public BufferedImage findHorizon(BufferedImage img, double[] leftPts,
			double[] rightPts) {
		Raster ras = img.getData();
		int width = img.getWidth();
		int height = img.getHeight();
		// Now find the edge line between light and dark -- should be horizon
		int[] pixs = new int[3*height];
		double[] edgeX = new double[2*(height+width)];
		double[] edgeY = new double[edgeX.length];
		int edgeCount = 0;
		lineOnly = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int i=0; i < width; i++) {
			// Grab an entire column
			ras.getPixels(i, 0, 1, height, pixs);
			int lastBright = pixs[0];
			for (int j=1; j < height; j++) {
				int bright = pixs[3*j];
				if (Math.abs(bright-lastBright) > 200) {
					edgeX[edgeCount] = i;
					edgeY[edgeCount] = j;
					edgeCount++;
					lineOnly.setRGB(i, j, Integer.MAX_VALUE);
				} 
				lastBright = bright;
			}
		}
		// Chop our edge arrays to only those containing edge points
		edgeX = Utils.chopArray(edgeX, edgeCount);
		edgeY = Utils.chopArray(edgeY, edgeCount);
		// Now, approximate as a straight line
		double[] fits = Utils.leastSquaresFit(edgeX, edgeY);
		boolean foundLeft = false;
		boolean foundRight = false;
		for (int i=0; i < width; i++) {
			double jFit = fits[0]+((double) i)*fits[1];
			// Step through until we find the first point on the screen
			if (!foundLeft) {
				if ((jFit >=0) && ((int) jFit < height-1)) {
					foundLeft = true;
					leftPts[0] = i;
					leftPts[1] = jFit;
				}
			} else if (!foundRight) {
				if (((int) jFit >= height-1) || (i == width-1)) {
					foundRight = true;
					rightPts[0] = i;
					rightPts[1] = jFit;
				}
			}
			jFit = Utils.constrain(jFit, 0, height-1);
			lineOnly.setRGB(i, (int) jFit, 0xffffff00);
		}
		return(lineOnly);
	}
}
