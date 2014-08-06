package imageNav;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Quat4d;
import javax.imageio.ImageIO;
import javax.media.j3d.Transform3D;

import mas.j3d.utils.J3dUtils;
import mas.j3d.utils.LEDbin;
import mas.swing.ImagePanel;
import mas.utils.Utils;

/** Given the relative position of four planar points (pAll_i, a 3 x 4 matrix
 * of column position vectors) and the representation of those points in an 
 * image frame (x,y, focal length ), we compute the position and rotation of the camera
 * relative to those same four points.
 *
 * @author Michael Swartwout   13 November 2006
 * Based on "Pose Estimation for Camera Calibration and Landmark Tracking",
 * by Abidi & Chandra, Proceedings of the  1990 IEEE International 
 * Conference on Robotics and Automation, 1990, p 420-426.  Note that the
 * example used in section 4 is wrong -- or at least we could not get it to
 * work given their equations.  Similarly, something is wrong with their
 * P1c, P2c, etc. equations in the middle of p. 423, so we stick with the
 * simpler representation.
 */
public class FourPointNav {
	private Vector3d tempV = new Vector3d();
	private int maxLEDs = 100;
	private LEDbin[] refLEDs = new LEDbin[maxLEDs];
	private LEDbin[] navLEDs = new LEDbin[maxLEDs];
	private Point3d referenceSpot = new Point3d();
	private Vector3d[] imagePoints = new Vector3d[5];
	private Vector3d[] imagePointBuffer = new Vector3d[imagePoints.length+1];
	private double[] distanceSpots = new double[imagePoints.length+1];
	private Vector3d[] dirs = {new Vector3d(), new Vector3d(), new Vector3d()};
	private Vector3d crossTemp = new Vector3d();
	private BufferedImage navI;

	public FourPointNav() {
		super();
		for (int i=0; i < pAll_c.length; i++) {
			pAll_c[i] = new Vector3d();
			pAll_zero[i] = new Vector3d();
		}
		for (int i=0; i < imagePointBuffer.length; i++) {
			imagePointBuffer[i] = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, 
					Double.MAX_VALUE);
		}
		for (int i=0; i < imagePoints.length; i++) {
			imagePoints[i] = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, 
					Double.MAX_VALUE);
		}
		for (int i=0; i < maxLEDs; i++) {
			refLEDs[i] = new LEDbin();
			navLEDs[i] = new LEDbin();
		}
	}

	public static void main(String[] args) {
		FourPointNav fpn = new FourPointNav();
		double d = 1.5*0.0254;
		Vector3d[] pAll_i = {
				new Vector3d(0, 0, 0),	
				new Vector3d(d, 0, 0),	
				new Vector3d(d, d, 0),	
				new Vector3d(0, d, 0),	
		};
		Point3d[] pAll_p = new Point3d[pAll_i.length];
		for (int i=0; i < pAll_i.length; i++) {
			pAll_p[i] = new Point3d(pAll_i[i]);
		}
		fpn.setWorld(pAll_i);
		String imgBase = ".";
		//		String imgBase = "C:\\Documents and Settings\\swartwoutm\\My Documents\\Projects\\ImageNav";
		//		String imgBase = "Z:\\Nanosat-5-Specific\\LED Pictures";
		String[] imgSrc = {//"bwImageCh1_08Jan2009at113615.bmp",
				//				"bwImageCh1_08Jan2009at113620.bmp",
				//				"bwImageCh1_08Jan2009at113625.bmp",
				//				"bwImageCh1_08Jan2009at113630.bmp",
				//				"bwImageCh1_08Jan2009at113635.bmp",
				//				"bwImageCh1_08Jan2009at113640.bmp",
				//				"bwImageCh1_08Jan2009at113645.bmp",
				//				"bwImageCh1_08Jan2009at113655.bmp",
				//	"bwImageCh1_08Jan2009at113700.bmp",
				//				"test1.bmp"
				//				};
				//		String[] imgSrc = {
				"test1.bmp",
//				"test2.bmp",
//				"test3.bmp",
//				"test4.bmp",
//				"test5.bmp",
		};
		//		String imgBase = ".";
		//		String[] imgSrc = {"img_test1.png","img_test2.png"};
		//		String[] imgSrc = {"bwImageCh3_07Aug2008at165132.bmp",
		//				"bwImageCh3_07Aug2008at165137.bmp", 
		//				"bwImageCh3_07Aug2008at165142.bmp"};
		//				"DSCF0190.JPG", "DSCF0191.JPG", "DSCF0201.JPG", "DSCF0221.JPG",
		//		};
		//		float[] refColor = new float[3];
		//		float[] navColor = new float[3];
		//		for (int j=0; j<3; j++) {
		//			refColor[j] = 1.0f;
		//			navColor[j] = ((float) Utils.BASE_FILTERS[Utils.GREEN_FILTER][j])/255;
		////			refColor[j] = ((float) Utils.BASE_FILTERS[Utils.RED_FILTER][j])/255;
		//		}
		//		Color3f refColor3f = new Color3f(refColor);
		//		Color3f navColor3f = new Color3f(navColor);
		//		Color3f refColor3f = AkoyaGroup.LED_COLORS[AkoyaGroup.DOCKREF1_LED];
		//		Color3f navColor3f = AkoyaGroup.LED_COLORS[AkoyaGroup.DOCKNAV_LED];
		Color3f bwColor3f = new Color3f(1.0f, 1.0f, 1.0f);
		Transform3D transform_C_to_N = new Transform3D();
		double cotFOVguess = 1/Math.tan(45*Math.PI/360);

		for (int i=0; i < imgSrc.length; i++) {
			try {
				File f = new File(imgBase + File.separator + imgSrc[i]);
				BufferedImage bi = ImageIO.read(f);
				Vector3d[] imagePoints = fpn.convertImageToNavPoints(bi, 
						//						navColor3f, refColor3f, 
						bwColor3f, null, 50,
						true);
				//				System.out.println(Utils.arrayToStringBuffer(imagePoints));
				double cotFOVreal = fpn.tetraNavCCD(imagePoints, transform_C_to_N, cotFOVguess);
				System.out.println("Take 1: " + transform_C_to_N 
						+ "\nWith FOV = " + Math.atan(1/cotFOVreal)*180/Math.PI +
						" vs " + Math.atan(1/cotFOVguess)*180/Math.PI);
				Vector3d r = new Vector3d();
				transform_C_to_N.get(r);
				System.out.println("Are we at " + r);
				for (int j=-5; j < 5; j++) {
					double fov = (1 + (double) j/10)*Math.atan(1/cotFOVreal)*180/Math.PI;
				JFrame jf = new JFrame();
				ImagePanel ip = new ImagePanel("Pseudo");
//				transform_C_to_N.transpose();
//				Transform3D t3dd = new Transform3D();
//				t3dd.rotZ(-180*Math.PI/180);
//				t3dd.mul(transform_C_to_N);
//				transform_C_to_N.mul(t3dd);
				ip.processImage(FourPointNav.buildPseudoImage(pAll_p, transform_C_to_N, 
						new Color3f(Color.green), new Color3f(Color.red), 
						fov));
				jf.getContentPane().add(ip);
				jf.setVisible(true);
				jf.pack();
				}
			} catch (IOException ioe) {
				// TODO Auto-generated catch block
				System.err.println("Can't find " + ioe.getMessage());
			}
		}
	}

	/**
	 * Computes the camera pose relative to a reference frame, which is as follows:
	 *
	 * @param bi -- original image with four navigation, one reference LED showing (at least)
	 * @param ledColor -- color of the navigation LEDs
	 * @param refColor -- color of the reference LED (used to identify LED 0)
	 * @param t3d -- transformation from reference plane to image plane (passed as a parameter,
	 * 			  	 but reset inside this function
	 * @param cotFOVguess -- an initial guess of the cotangent of the FOV of the lens
	 * @param colorMatch -- the allowed offset between the LED RGB color and any color that will count
	 *                      as an LED
	 * @return -- the computed cotangent of the FOV
	 */
	public double tetraNav(BufferedImage bi, Color3f ledColor, Color3f refColor, 
			Transform3D t3d, double cotFOVguess, int colorMatch) {
		return(tetraNavCCD(
				convertImageToNavPoints(bi, ledColor, refColor, 
						colorMatch, false), 
						t3d, cotFOVguess));
	}

	/**
	 * Computes the camera pose relative to a reference frame, which is as follows:
	 *
	 * @param bi -- original image with four navigation, one reference LED showing (at least)
	 * @param ledColor -- color of the navigation LEDs
	 * @param refColor -- color of the reference LED (used to identify LED 0)
	 * @param t3d -- transformation from reference plane to image plane (passed as a parameter,
	 * 			  	 but reset inside this function
	 * @param cotFOVguess -- an initial guess of the cotangent of the FOV of the lens
	 * @param colorMatch -- the allowed offset between the LED RGB color and any color that will count
	 *                      as an LED
	 * @param showImage -- if true, displays the raw and processed images side-by-side in a pop-up window.
	 * @return -- the computed cotangent of the FOV
	 */
	public double tetraNav(BufferedImage bi, Color3f ledColor, Color3f refColor, 
			Transform3D t3d, double cotFOVguess, int colorMatch, 
			boolean showImage) {
		return(tetraNavCCD(
				convertImageToNavPoints(bi, ledColor, refColor, 
						colorMatch, showImage), 
						t3d, cotFOVguess));
	}


	/**
	 * Finds the location (in camera coordinates) of the navigation markers.
	 * For this to be useful in the rest of the algorithm, the camera points
	 * must be organized as:
	 * -- nav LEDs in counter-clockwise order, with the reference *defined* as (0,0,0)
	 * -- reference LED marked by proximity to the nav LED
	 * -- The other three are at (x,y,0) 
	 * -- Unlike some image references, this is a 'classic' right-hand system 
	 *    where x is to the right and y is towards the top of the image.

	 * @param image
	 * @param show -- debug flag to display intermediate images during the process
	 * @return
	 */
	public Vector3d[] convertImageToNavPoints(BufferedImage image, Color3f navLEDcolor,
			Color3f referenceLEDcolor, int colorMatch, boolean show) {
		navI = ImageNavUtils.colorFilter(image, navLEDcolor, 
				colorMatch, show);
		// Find all navigation LEDs in image
		int numNav = findLEDsFromImage(navI, navLEDs);
		System.out.print("Found " + numNav + " nav LEDs: ");
		for (int i=0; i < numNav; i++) {
			System.out.print(navLEDs[i].getCentroid() + " ");
		}
		System.out.println();
		// Initialize the distance and point arrays
		Arrays.fill(distanceSpots, Double.MAX_VALUE);
		for (int i=0; i < imagePoints.length; i++) {
			imagePoints[i].set(Double.MAX_VALUE, Double.MAX_VALUE, 
					Double.MAX_VALUE);
		}
		// Find all reference LEDS in image
		referenceSpot.x = Double.NaN;
		if (referenceLEDcolor != null) {
			/* If the reference LED is a different color, we find that and then 
			 * reference to it.
			 */
			BufferedImage refI = ImageNavUtils.colorFilter(image, referenceLEDcolor, 
					colorMatch, show);
			findLEDsFromImage(refI, refLEDs);
			System.out.println("Ref at " + refLEDs[0].getCentroid());
			// Find the nav LEDs closest to our reference
			referenceSpot.set(refLEDs[0].getCentroid());
			// Find the 4 lights closest to the reference LED
			findClosestNto(referenceSpot, navLEDs, 4, numNav,
					distanceSpots, imagePoints);
			// imagePoints is now sorted
			for (int i=0; i < 4; i++) {
				System.out.println("Ipoints: " + imagePoints[i]);
				drawCross(navI, imagePoints[i], 5, 0xffff00);
			}
		} 
		if (Double.compare(referenceSpot.x, Double.NaN) == 0) {
			/* All our LEDs are the same color (probably white/gray), and we have to 
			 * make do as best we can.
			 */
			for (int i=0; i < imagePointBuffer.length; i++) {
				imagePointBuffer[i].set(Double.MAX_VALUE, Double.MAX_VALUE, 
						Double.MAX_VALUE);
			}
			findClosestN(navLEDs, 5, numNav, distanceSpots, imagePoints, imagePointBuffer);
			referenceSpot.set(imagePoints[0]);
			for (int i=0; i < imagePoints.length; i++) {
				System.out.println("FPN251: Point " + i + " is in the final set: " + imagePoints[i]);
			}
			try {
				drawCross(navI, imagePoints[0], 5, 0xff0000);
			} catch (ArrayIndexOutOfBoundsException aioobe) { }
			// Slide the rest up one
			for (int i=1; i < imagePoints.length; i++) {
				try {
					drawCross(navI, imagePoints[i], 5, 0xffff00);
				} catch (ArrayIndexOutOfBoundsException aioobe) { }
				imagePoints[i-1].set(imagePoints[i]);
			}
		}

		/* We have them sorted, closest to ref on up.
		 * Next, an important transformation.  The image and LED finder worked in
		 * 'image' coordinates, where (0,0) is the upper-left corner of the image 
		 * (and therefore +x is to the right and +y is down the image)
		 * .
		 * In our algorithm, (0,0) is the center of the image, which makes
		 * +x to the right (still), but +y is towards the top.  So we need to flip
		 * everything and recenter.
		 * 
		 * And we need to rescale such that the x-edges of the image are at +/-1.  
		 * Such a mess.
		 * 
		 * So, first we put the image center at 0
		 */

		double w = image.getWidth();
		double h = image.getHeight();
		//		System.out.println("w=" + w + ", h="+h);
		//		System.out.println("Scale increment is " + 2/(w-1) + " and " + 2*h/w/(h-1));
		/* **************************************************************
		 * **************** EXTREMELY IMPORTANT! ************************
		 * The four-point imagenav algorithm specified by Abidi, et al
		 * uses the pinhole camera model -- so the image is mirrored
		 * on the CCD.  Our images are delivered in 'corrected' form
		 * (the mirror image has been mirrored back).  We have not 
		 * altered Abidi's algorithm, which means that we have to mirror
		 * the imagePoints calculations, below.
		 * **************************************************************
		 */ 
		for (int i=0; i < 4; i++) {
			//			imagePoints[i].y -= 1;
			System.out.println("FPN366:  In image space, the points are " + imagePoints[i]);
			imagePoints[i].x =  (2*imagePoints[i].x/(w-1) - 1);
			imagePoints[i].y = -(2*imagePoints[i].y/(h-1) - 1) * (h/w);
		}
		for (int i=0; i < 4; i++) {
			System.out.println("FPN 370 rescale:  Point " + i + ": " + imagePoints[i]);
		}
		// Finally, reorder them to be counter-clockwise
		organizeLEDs(imagePoints);
		for (int i=0; i < 4; i++) {
			System.out.println("FPN 300 CCN Correction:  Point " + i + " is now at " + imagePoints[i]);
		}
		return(imagePoints);
	}

	/** Draws a small cross at the given point (used as a marker)
	 * 
	 * @param image
	 * @param pt -- center of the cross
	 * @param sidelength -- length of each arm of the cross
	 * @param color -- color of the lines to draw
	 */
	public static void drawCross(BufferedImage image, Tuple3d pt, 
			int sidelength, int color) {
		for (int i=0; i < sidelength; i++) {
			try {
				image.setRGB((int) pt.x, (int) pt.y+i, color);			
				image.setRGB((int) pt.x+i, (int) pt.y, color);			
				image.setRGB((int) pt.x-i, (int) pt.y, color);			
				image.setRGB((int) pt.x, (int) pt.y-i, color);	
			} catch (ArrayIndexOutOfBoundsException aioobe) { // Ignore it 
			}
		}
	}


	/** Pick the N LEDs that are closest to one another, and place them in the
	 *  first N spots in the array.
	 *  
	 * @param leds
	 * @param n
	 */
	public static void findClosestN(LEDbin[] leds, int n, int numLEDsToCheck,
			double[] distanceSpots, Vector3d[] imagePoints, 
			Vector3d[] imagePointBuffer) {
		if (numLEDsToCheck < n) {
			// We don't have enough LEDs!
			System.out.println("FPN343: Not enough LEDs to check");
			return;
		}
		double refDistance = Double.MAX_VALUE;
		double thisDistance;
		for (int i=0; i < numLEDsToCheck; i++) {
			if (leds[i].isLEDlike()) {
				/* This algorithm will always put our reference LED in the first
				 * position (distance = 0), but the reference LED is supposed to
				 * be among the N points, so it's okay.
				 */
				thisDistance = findClosestNto(leds[i].getCentroid(), leds, n, 
						numLEDsToCheck,
						distanceSpots, imagePointBuffer);
				if (thisDistance < refDistance) {
					refDistance = thisDistance;
					for (int j=0; j < n; j++) {
						// Copy the points into our current best set
						imagePoints[j].set(imagePointBuffer[j]);
					}
				}
			}
		}
	}

	/** Find the N LEDs that are closest (in distance) to a given reference spot.
	 * The algorithm assumes that each LEDbin has had the checkForLEDness() 
	 * method called; we will cull the list for un-LED-like elements.
	 * 
	 * @param reference
	 * @param leds
	 * @param n
	 * @param numLEDStoCheck
	 * @param distanceSpots
	 * @param imagePoints
	 * @return
	 */
	public static double findClosestNto(Point3d reference, LEDbin[] leds, int n,
			int numLEDStoCheck, double[] distanceSpots, Vector3d[] imagePoints) {
		// Keeps track of how close a given LED is to our reference
		double distanceToRef;
		// DistanceSpots will hold the N closest distances
		Arrays.fill(distanceSpots, Double.MAX_VALUE);
		for (int i=0; i < numLEDStoCheck; i++) {
			// Only check on the LEDs that are sufficiently LED-like
			if (leds[i].isLEDlike()) {
				distanceToRef = reference.distance(leds[i].getCentroid());
				System.out.println(leds[i].getCentroid() + " is " + distanceToRef + " from " + reference);
				/* Compare this distance to those in our shortlist, and
				 * replace as needed.
				 */
				for (int j=0; j < n; j++) {
					if (distanceToRef < distanceSpots[j]) {
						slideUp(imagePoints, j);
						slideUp(distanceSpots, j);
						distanceSpots[j] = distanceToRef;
						imagePoints[j].set(leds[i].getCentroid());
						break;
					}
				}
			} 
		}
		// Return the longest distance that made our list
		return(distanceSpots[n-1]);
	}


	/** Arrange the LEDs in proper counter-clockwise order -- which we
	 *  get by knowing that the vector cross (0-i) x (0-k) has 
	 *  z-component > 0 for i < k 
	 *  
	 *  Of course, this means we assume that point 0 is our reference.
	 *  
	 *  IMPORTANT:  You need to have reversed the image coordinates to be 
	 *  our coordinates (i.e. change the sign on y to match a right-hand
	 *  system) 
	 *  
	 * @param imagePoints, with the points already defined as above 
	 */
	private void organizeLEDs(Vector3d[] imagePoints) {
		for (int i=1; i < 4; i++) {
			dirs[i-1].set(imagePoints[i]);
			dirs[i-1].sub(imagePoints[0]);
		}
		swapTest(dirs[0], dirs[1], imagePoints[1], imagePoints[2]);
		swapTest(dirs[1], dirs[2], imagePoints[2], imagePoints[3]);
		swapTest(dirs[0], dirs[1], imagePoints[1], imagePoints[2]);
		//		System.out.println("Organized LEDs" + Utils.arrayToStringBuffer(imagePoints));
	}

	/** If the cross product is in the -z plane, swap the both sets of vectors	 
	 * 
	 * @param t1
	 * @param t2
	 */
	private void swapTest(Vector3d t1, Vector3d t2, Vector3d v1, Vector3d v2) {
		crossTemp.cross(t1, t2);
		if (crossTemp.z < 0) {
			// Swap
			crossTemp.set(t1);
			t1.set(t2);
			t2.set(crossTemp);
			crossTemp.set(v1);
			v1.set(v2);
			v2.set(crossTemp);
		}
	}

	/** Starting with element j, move everything one spot back in the arrays,
	 *  with the last element dropping off.
	 * @param v
	 * @param j
	 */
	public static Vector3d[] slideUp(Vector3d[] v, int j) {
		for (int i=v.length-1; i > j; i--) {
			v[i].set(v[i-1]);
		}
		return(v);
	}

	/** Starting with element j, move everything one spot back in the arrays,
	 *  with the last element dropping off.
	 * @param v
	 * @param j
	 */
	public static double[] slideUp(double[] v, int j) {
		for (int i=v.length-1; i > j; i--) {
			v[i] = v[i-1];
		}
		return(v);
	}


	/** Find bright lights in a given grayscale image.  We simply look at the 
	 *  red saturation level number & assume everything else matches.
	 *  
	 *  
	 * @param image
	 * @param ledBins
	 * @return
	 */
	public int findLEDsFromImage(BufferedImage image, LEDbin[] ledBins) {
		int width = image.getWidth();
		int height = image.getHeight();
		// Array of brightness levels of the entire image
		int[][] brightnessMap = new int[width][height];
		// Populate it
		for (int i=0; i < width; i++) {
			for (int j=0; j < height; j++) {
				brightnessMap[i][j] = (image.getRGB(i, j) >> 16) & 0xff;
			}
		}
		for (int i=0; i < ledBins.length; i++) {
			ledBins[i].setBrightnessMap(brightnessMap);
		}
		// Start with a single rectangle encompassing the entire image
		binCounter = 0;
		ledBins[binCounter].setRect(0, 0, width-1, height-1);
		findLEDrecursiveCycle(ledBins, binCounter);
		int LEDboxColor = 0xff0000;
		int notLEDboxColor = 0x0000ff;
		int boxColor;
		for (int j=0; j <= binCounter; j++) {
			ledBins[j].computeCentroid();
			if (ledBins[j].checkForLEDness()) {
				boxColor = LEDboxColor;
			} else {
				boxColor = notLEDboxColor;
			}
			drawBox(image, ledBins[j], boxColor);
		}
		return(++binCounter);
	}

	/** Draw a box around the given LED
	 * 
	 * @param image
	 * @param led
	 * @param boxColor
	 */
	public static void drawBox(BufferedImage image, LEDbin led, int boxColor) {
		int w=led.width;
		int h=led.height;
		int x=led.x;
		int y=led.y;
		// For diagnostic purposes, draw a colored border around the LEDs
		for (int i=0; i < w; i++) {
			image.setRGB(i+x, y, boxColor);
			image.setRGB(x+i, y+h, boxColor);
		}
		for (int i=0; i < h; i++) {
			image.setRGB(x, y+i, boxColor);
			image.setRGB(x+w, y+i, boxColor);
		}
	}

	private int binCounter;
	private int pixelDensityThreshold = 50;	// Threshold for allowing a meridian split
	private int pixelBrightnessThreshold = 150;	// Threshold for calling a pixel in box


	/** 
	 * Recursive method to subdivide an image into rectangles with each rectangle 
	 * containing a point of light.
	 * 
	 * The algorithms consists of:
	 * 1) Shrinking the rectangle until each edge 'touches' the light region
	 * 2) Figuring out whether our rectangle contains one light, or multiple lights.
	 *    (If there's only one light, we exit.)
	 * 3) Splitting the box into two rectangles, each with (at least) one light.
	 * 4) Recursively calling the method for both new rectangles.
	 * 
	 * A 'light' is defined as a region of the image whose average pixel density
	 * is sufficiently high.  A rectangle is first shrunk until each edge of the
	 * rectangle contains at least one pixel with brightness above a given threshold.
	 * Then, the density is computed and the rectangle split across its longest axis
	 * along the 'darkest' meridian we can find.
	 * 
	 * @param allBins -- the array of all possible LEDbins
	 * @param thisBin -- the specific rectangle we are investigating.
	 * @param errorCheck -- apply rules to eliminate non-LEDs
	 */
	private void findLEDrecursiveCycle(LEDbin[] allBins, int thisBin) {
		// Shrink rectangle to fit relevant pixels
		allBins[thisBin].shrinkToFit(pixelBrightnessThreshold);
		//		System.out.println(thisBin + " shrunk to " + allBins[thisBin]);
		// Try to split it
		if (binCounter == maxLEDs-1) {
			// Punt!
			return;
		}
		// Try to split along a meridian line based on whichever side is longer
		if (allBins[thisBin].splitAlongMeridian(
				allBins[binCounter+1], 
				(allBins[thisBin].width > allBins[thisBin].height),
				pixelDensityThreshold)) {
			/* Recursively run this cycle on the 'old' box and the new
			 * Note that you have to run the new box first, or else the pointers
			 * get all goofed up by later recursive calls
			 */
			binCounter++;
			//			System.out.println("Splitting " + thisBin + " into " + thisBin + " and " + binCounter);			
			//			System.out.println("\tSplitting " + thisBin + " " + allBins[thisBin]);
			//			System.out.println("\tSplit to " + binCounter + " " + allBins[binCounter]);
			findLEDrecursiveCycle(allBins, binCounter);
			findLEDrecursiveCycle(allBins, thisBin);
		} else {
			// Done.  This is a minimally-sized bin
			allBins[thisBin].computeCentroid();
			//			System.out.println("**********\n\tLED " + thisBin + " at " + allBins[thisBin].getCentroid() + "\n**********");
		}
	}



	private Vector3d[] pAll_i;
	private double[] A;
	private double[][] s;

	/** Defines the inertial-frame location of the four reference points.
	 *	These first steps can be done once, offline.  
	 *
	 * VERY IMPORTANT:  The first reference point is DEFINED to be at [0, 0, 0],
	 * and all the other points are assumed to be in the same plane (i.e, 
	 * [x(i), y(i), 0]).  Anything else will lead to confusion.
	 * 
	 * @param pAll_i
	 */
	public void setWorld(Vector3d[] pAll_i) {
		this.pAll_i = pAll_i;
		int N = pAll_i.length;
		A = new double[N];
		s = new double[N][N];
		/* ********* Note that these first steps can be done once, offline *****/
		for (int i=0; i < N; i++) {
			// step 0:  build s-matrix
			for (int j=0; j < i; j++) {
				tempV.set(pAll_i[j]);
				tempV.sub(pAll_i[i]);
				s[i][j] = tempV.length();
				s[j][i] = s[i][j];
			}
			//			System.out.println("s" + i + ": " + Utils.arrayToStringBuffer(s[i]));
		}

		// step 1: find A (needs to be done only once - does not depend on x or y)
		A[0] = Math.sqrt(
				Math.pow(s[0][1]*s[0][1] + s[0][2]*s[0][2] + s[1][2]*s[1][2], 2) 
				- 2*(Math.pow(s[0][1],4) + Math.pow(s[0][2],4) + Math.pow(s[1][2],4))
				)/4;
		A[1] = Math.sqrt(
				Math.pow(s[0][1]*s[0][1] + s[0][3]*s[0][3] + s[1][3]*s[1][3], 2) 
				- 2*(Math.pow(s[0][1],4) + Math.pow(s[0][3],4) + Math.pow(s[1][3],4))
				)/4;
		A[2] = Math.sqrt(
				Math.pow(s[0][2]*s[0][2] + s[0][3]*s[0][3] + s[2][3]*s[2][3], 2) 
				- 2*(Math.pow(s[0][2],4) + Math.pow(s[0][3],4) + Math.pow(s[2][3],4))
				)/4;
		A[3] = Math.sqrt(
				Math.pow(s[1][2]*s[1][2] + s[1][3]*s[1][3] + s[2][3]*s[2][3], 2) 
				- 2*(Math.pow(s[1][2],4) + Math.pow(s[1][3],4) + Math.pow(s[2][3],4))
				)/4;
		//		System.out.println("A: " + Utils.arrayToStringBuffer(A));
	}

	/** 
	 * Compute the four-point-in-plane image-based navigation solution
	 * @param pAll_i -- position of the reference points in reference coordinates
	 *                  pAll_i is, by definition, (0,0,0) and the other three points
	 *                  have their z-component == 0.
	 * @param imagePoints -- position of the four reference points in the camera space
	 * 					These points must be in order (i.e., imagePoints[j] refers to
	 * 					pAll_i[j]) and, again, the z-component is zero.
	 * @param t3d		The complete transformation from reference point 0 to the
	 * 					camera
	 * @return
	 */
	public double tetraNavPinhole(Vector3d[] pAll_i, 
			Vector3d[] imagePoints, Transform3D t3d, double fGuess) {
		setWorld(pAll_i);
		return(tetraNavPinhole(imagePoints, t3d, fGuess));
	}

	private double[] B = new double[4];
	private double[][] C = new double[4][4];
	private double[][] Hsq = new double[4][4];
	//	private double[][][] Esquare = new double[4][4][4];
	//	private double[][][] fAll = new double[4][4][4];
	private double[] F = new double[4];
	private double[] d = new double[4];
	private Vector3d[] pAll_c = new Vector3d[4];
	private Vector3d[] pAll_zero = new Vector3d[4];
	private Matrix3d cic_matrix = new Matrix3d();
	private Quat4d qTemp = new Quat4d();


	/**
	 * Compute the four-point-in-plane image-based navigation solution
	 * @param imagePoints -- position of the four reference points in the camera space
	 * 					These points must be in order (i.e., imagePoints[j] refers to
	 * 					pAll_i[j]) and, again, the z-component is zero.
	 * @param t3d		The complete transformation from reference point 0 to the
	 * 					camera
	 * @return
	 */
	private double tetraNavCore(Vector3d[] imagePoints, double fGuess) {
		System.out.println("FPN679: fguess=" + fGuess);
		// step 2: find B (part of the triple product)
		B[0] = imagePoints[0].x*(imagePoints[2].y - imagePoints[1].y) 
				+ imagePoints[0].y*(imagePoints[1].x - imagePoints[2].x) 
				+ imagePoints[1].y*imagePoints[2].x - imagePoints[1].x*imagePoints[2].y;
		B[1] = imagePoints[0].x*(imagePoints[3].y 
				- imagePoints[1].y) + imagePoints[0].y*(imagePoints[1].x - imagePoints[3].x) 
				+ imagePoints[1].y*imagePoints[3].x - imagePoints[1].x*imagePoints[3].y;
		B[2] = imagePoints[0].x*(imagePoints[3].y - imagePoints[2].y) 
				+ imagePoints[0].y*(imagePoints[2].x - imagePoints[3].x) 
				+ imagePoints[2].y*imagePoints[3].x - imagePoints[2].x*imagePoints[3].y;
		B[3] = imagePoints[1].x*(imagePoints[3].y - imagePoints[2].y) 
				+ imagePoints[1].y*(imagePoints[2].x - imagePoints[3].x) 
				+ imagePoints[2].y*imagePoints[3].x - imagePoints[2].x*imagePoints[3].y;
		//		System.out.println("B: " + Utils.arrayToStringBuffer(B));

		// step 3: find C
		Utils.zeroArray(C);
		C[0][1] = (B[2]/A[2]) * (A[3]/B[3]);
		C[0][2] = (B[1]/A[1]) * (A[3]/B[3]);
		C[0][3] = (B[0]/A[0]) * (A[3]/B[3]);
		C[1][2] = (B[1]/A[1]) * (A[2]/B[2]);
		C[1][3] = (B[0]/A[0]) * (A[2]/B[2]);
		C[2][3] = (B[0]/A[0]) * (A[1]/B[1]);
		for (int i=0; i < 4; i++) {
			for (int j=i+1; j < 4; j++) {
				C[j][i] = 1/C[i][j];
			}
			//			System.out.println("C" + i + ": " + Utils.arrayToStringBuffer(C[i]));
		}

		// step 4: find H.  Note that we can only run this computation after we know
		// all the C[i][j]
		for (int i=0; i < 4; i++) {
			for (int j=0; j < 4; j++) {
				Hsq[i][j] = Math.pow(imagePoints[i].x-C[i][j]*imagePoints[j].x, 2) 
						+ Math.pow(imagePoints[i].y-C[i][j]*imagePoints[j].y, 2);
			}
			//			System.out.println("H" + i + ": " + Utils.arrayToStringBuffer(Hsq[i]));
		}

		// step 5 find f
		//		Utils.zeroArray(Esquare);

		int fCount = 0;
		double fSum = 0;
		//		System.out.println("WARNING: FPN has changed the sign on temp");
		/* According to Abidi, we vary i & j from 0-3, and then pick the value of k that
		 * is different. In other words, (i,j,k) =
		 * (0, 1, 2) (0, 1, 3) (0, 2, 1) (0, 2, 3) (0, 3, 1) (0, 3, 2)
		 * (... I give up)
		 */
		for (int i=0; i < 4; i++) {
			for (int j=0; j < 4; j++) {
				for (int k=0; k < 4; k++) {
					double Esquare = s[i][j]*s[i][j]*(1-C[i][k])*(1-C[i][k]) 
							- s[i][k]*s[i][k]*(1-C[i][j])*(1-C[i][j]);
					//					Esquare[i][j][k] = s[i][j]*s[i][j]*(1-C[i][k])*(1-C[i][k]) 
					//					- s[i][k]*s[i][k]*(1-C[i][j])*(1-C[i][j]);
					// Okay, so my three dimensional notation is complete overkill,
					// since we only care about f(i,j,k) when i~=j~=k
					if ((i != j) && (k != i) && (k != j)) {
						double temp = (s[i][k]*s[i][k] * Hsq[i][j] 
								- s[i][j]* s[i][j] * Hsq[i][k]) 
								/ Esquare;
						//								/ Esquare[i][j][k]);
						//						fAll[i][j][k] = Math.sqrt(temp);
						//						fSum = fSum + fAll[i][j][k];
						System.out.println(i+","+j+","+k+" yields f=:" + temp);
						if (temp > 0) {
							fSum = fSum + Math.sqrt(temp);
							fCount = fCount + 1;
						}
					}
				}
			}
		}
		System.out.println("fsum="+fSum+", fCount="+fCount);
		if ((fSum == 0) | (fSum > 1000*fCount)) {
			System.out.println("FPN 713:  Substituted f:" + fGuess 
					+ " instead of averaged f:" + fSum/fCount);
			fSum = fGuess;
			fCount = 1;
		}
		/* Look at what we're doing! */
		System.out.println("FPN 785:  Calculated f = " + (fSum/fCount) + 
				", instead of using f = " + fGuess);
		double f = fSum/fCount;
		//		double f = fGuess;

		// Compute the length of the tetrahedral sides in the F-frame
		for (int i=0; i < 4; i++) {
			F[i] = Math.sqrt(imagePoints[i].lengthSquared() + f*f);
		}
		//		System.out.println("F: " + Utils.arrayToStringBuffer(F));

		// Compute the average length of one side in the C-frame
		int dCount = 0;
		double dSum = 0;
		for (int j=1; j < 4; j++) {
			dSum = dSum + s[0][j]*F[0]/Math.sqrt(Hsq[0][j] + f*f*(1-C[0][j])*(1-C[0][j]));
			dCount = dCount + 1;
		}
		d[0] = dSum/dCount;
		// Compute the other tetrahedral sides, which are proportional to d1
		for (int i=1; i < 4; i++) {
			d[i] = C[0][i]*F[i]/F[0]*d[0];
		}
		//		System.out.println("d: " + Utils.arrayToStringBuffer(d));
		return(f);
	}

	/** Compute the four-point image navigation transformation using the pinhole
	 *  camera assumptions (baseline Abidi paper).
	 *  
	 * @param imagePoints -- position of the four reference points in the camera space
	 * 					These points must be in order (i.e., imagePoints[j] refers to
	 * 					pAll_i[j]) and, again, the z-component is zero.
	 * @param t3d		The complete transformation from reference point 0 to the
	 * 					camera
	 * @return
	 */
	public double tetraNavPinhole(Vector3d[] imagePoints, Transform3D t3d, 
			double fGuess) {
		/* Use the same core function to create f (and the variables F and d, which 
		 * are global).
		 */
		double f = tetraNavCore(imagePoints, fGuess);
		/* Compute the position of the points in the camera-centered frame.  Note
		 * that I used to think Abidi's paper had a sign error (the last f-term),
		 * but I was wrong.  *My* error was in how I arranged the points, above.. 
		 */
		for (int i=0; i < 4; i++) {
			pAll_c[i].set(imagePoints[i]);
			pAll_c[i].z = -f;
			pAll_c[i].scale(-d[i]/F[i]);
			pAll_c[i].z = pAll_c[i].z + f;
		}
		tetraNavBuildT3D(t3d);
		return(f);
	}

	/** Compute the four-point image navigation solution using the 'CCD' assumption 
	 *  (i.e., the imagePoint coordinates are pulled straight off a digital image, not
	 *  from the pinhole camera values)
	 *  
	 * @param imagePoints -- position of the four reference points in the camera space
	 * 					These points must be in order (i.e., imagePoints[j] refers to
	 * 					pAll_i[j]) and, again, the z-component is zero.
	 * @param t3d		The complete transformation from reference point 0 to the
	 * 					camera
	 * @param cotFOVguess -- what we think the field of view is (will be updated)
	 * @return
	 */
	public double tetraNavCCD(Vector3d[] imagePoints, Transform3D t3d, 
			double cotFOVguess) {
		/* Use the same core function to create f (and the variables F and d, which 
		 * are global).
		 */
		double cotFOV = tetraNavCore(imagePoints, cotFOVguess);
		for (int i=0; i < 4; i++) {
			pAll_c[i].set(imagePoints[i]);
			pAll_c[i].z = cotFOV;
			pAll_c[i].scale(d[i]/F[i]);
		}
		//		System.out.println("TN797:  Converted image points: " + Utils.arrayToStringBuffer(pAll_c));
		tetraNavBuildT3D(t3d);
		return(cotFOV);
	}

	private Transform3D tetraNavBuildT3D(Transform3D t3d) {
		/* Compute the position of the points in the camera-centered frame.  Be
		 * wary of the signs; I have irreconciled differences between the signs
		 * in Abidi's paper and my own (trial-and-errorish) solution.
		 */
		double xx = 0;
		double xy = 0;
		double yy = 0;
		/* Build conversion matrix.  This works because we know that
		 * all the points in pAll_i are in a plane (z-element is zero).  So, first
		 * we write the relative to point 0 (the C-frame).
		 */
		for (int i=0; i < 4; i++) {
			pAll_zero[i].sub(pAll_c[i], pAll_c[0]);
			// Gather up administrative stuff needed in the next calculations
			xx = xx + pAll_i[i].x* pAll_i[i].x;
			yy = yy + pAll_i[i].y* pAll_i[i].y;
			xy = xy + pAll_i[i].x* pAll_i[i].y;
		}
		/* The rotation matrix is computing the pseudoinverse of the the points in
		 * the inertial frame (known).  In happy matrix algebra, it looks like this:
		 * 
		 * Cic = -pAll_zero*pAll_i(1:2,:)'*inv(pAll_i(1:2,:)*pAll_i(1:2,:)');
		 * 
		 * We rewrite this as:
		 * 
		 * Cic = -tempD2 * tempD1 * inv([xx xy; xy yy])
		 */
		double xyDenom = xx*yy - xy*xy;
		double[][] invMatrix = {
				{yy/xyDenom , -xy/xyDenom},
				{-xy/xyDenom , xx/xyDenom}
		};
		// It's all there but the sign
		double[][] tempD1 = new double[pAll_i.length][2];
		for (int i=0; i < pAll_i.length; i++) {
			tempD1[i][0] = pAll_i[i].x;
			tempD1[i][1] = pAll_i[i].y;
		}
		double[][] tempD2 = new double[3][pAll_zero.length];
		for (int i=0; i < pAll_zero.length; i++) {
			tempD2[0][i] = -pAll_zero[i].x;
			tempD2[1][i] = -pAll_zero[i].y;
			tempD2[2][i] = -pAll_zero[i].z;
		}
		double[][] Cic = Utils.matrixMultiply(tempD2, 
				Utils.matrixMultiply(tempD1, invMatrix));
		/* HERE'S WHERE THE WHEELS COME OFF.  There's a disconnect between
		 * Java3D matrix definitions and Abidi's definitions.  So, at this
		 * last stage, we flip signs and transpose the matrix.
		 * There are two issues:  Abidi's transformation is the inverse of
		 * the 'standard' Transform3D in Java3D, and also we're carrying a
		 * sign-flip problem thanks to the pinhole camera model.  (Our produced
		 * images are already mirrored; while Abidi's use the mirror image.)
		 */
		cic_matrix.setRow(0, 
				-Cic[0][0], 
				-Cic[0][1], 
				-(Cic[1][0]*Cic[2][1] - Cic[2][0]*Cic[1][1]) );
		cic_matrix.setRow(1, 
				-Cic[1][0], 
				-Cic[1][1],
				-(Cic[2][0]*Cic[0][1] - Cic[0][0]*Cic[2][1]) );
		cic_matrix.setRow(2, 
				Cic[2][0], 
				Cic[2][1], 
				Cic[0][0]*Cic[1][1] - Cic[1][0]*Cic[0][1] );
		J3dUtils.makeUnitByColumn(cic_matrix);
		tempV.set(pAll_c[0]);
		tempV.x = -tempV.x;
		tempV.y = -tempV.y;
		//		tempV.z = -tempV.z;
		//		System.out.println("FPN941: " + pAll_c[0] + "; " + tempV);
		//		cic_matrix.transpose();
		/* We convert to quaternions because cic_matrix may not
		 * be exactly square, and my dc2q is not confused by this
		 * problem.  Java3D will give you very bad answers.
		 */
		J3dUtils.dc2q(cic_matrix, qTemp);
		cic_matrix.set(qTemp);
		cic_matrix.transform(tempV);
		t3d.set(cic_matrix, tempV, 1);
		return(t3d);
	}

	//	public Vector3d[] buildPointsInS(Vector3d[] pAll_i) {
	//	/* ************************************************************
	//	 * Convert world points to s-frame (standard position).  It involves three
	//	 * rotations:  theta about the 3-axis, beta about the 2-axis, and alpha
	//	 * about the 1-axis
	//	 */
	//	double t1 = (pAll_i[1].y-pAll_i[0].y);
	//	double t2 = (pAll_i[1].x - pAll_i[0].x);
	//	double theta = Math.atan(t1 / t2);
	//	
	//	// Flip theta if p1*1I < p0*1I
	//	if (pAll_i[1].x < pAll_i[0].x) {
	//		theta = theta + Math.PI;
	//	}
	//	double beta = Math.atan(-(pAll_i[1].z - pAll_i[0].z) / 
	//			Math.sqrt(t1*t1 + t2*t2));
	//	double alpha = Math.atan2(  ( (pAll_i[2].x-pAll_i[0].x)*Math.cos(theta) + 
	//			(pAll_i[2].y-pAll_i[0].y)*Math.sin(theta))*Math.sin(beta) + 
	//			(pAll_i[2].z-pAll_i[0].z)*Math.cos(beta)  , 
	//			-(pAll_i[2].x-pAll_i[0].x)*Math.sin(theta) + 
	//			(pAll_i[2].y-pAll_i[0].y)*Math.cos(theta) );
	//	/* Establish the transformation matrix for the origin, which we take to be
	//	 * point 0.
	//	 */
	//	Matrix4d T_origin_translate = new Matrix4d();
	//	tempV.set(pAll_i[0]);
	//	tempV.negate();
	//	T_origin_translate.set(tempV);
	//	
	//	// This is the transformation from world frame to standard frame
	//	T_i_to_s = new Matrix4d();
	//	T_i_to_s.mul(J3dUtils.eulerrot4d(alpha, 1), J3dUtils.eulerrot4d(beta, 2));
	//	T_i_to_s.mul(T_i_to_s, J3dUtils.eulerrot4d(theta, 3));
	//	T_i_to_s.mul(T_i_to_s, T_origin_translate);
	//	System.out.println("T_i_to_s: " + T_i_to_s);
	//	// These values are used in the numerical solution, below
	//	pAll_s = new Vector3d[pAll_i.length];
	//	// Transform each of the points from the i-frame to the s-frame
	//	for (int i=0; i < pAll_s.length; i++) {
	//		tempV.set(pAll_i[i]);
	//		T_i_to_s.transform(tempV);
	//		pAll_s[i].set(tempV);
	//		System.out.println("pAll_s " + i + ": " + pAll_s[i]);
	//	}
	//	return(pAll_s);
	//}
	//
	///** Compute the pose from points on a given image.  This function assumes you've
	// * already calibrated your world points using buildPointsInS()
	// * 
	// * @param pImage_c -- location of the target points in the image (x, y only used)
	// * @param pAll_c
	// * @return
	// */
	//public Matrix4d computePose(Point3d[] pImage_c, double[][] pAll_c) {
	//	
	//	/* ***********************************************************************
	//	 * Now convert the camera image from the camera frame to the ideal frame 
	//	 * (3D looking at origin, 1D parallel to 1S).  The three rotations are phi
	//	 * about the 3-axis, omega about the 2-axis, and rho about the 1-axis.
	//	 * HOWEVER, THERE'S ANOTHER COORDINATE ROTATION/TRANSFORMATION THAT I DON'T
	//	 * QUITE UNDERSTAND -- SEE Tfix, BELOW
	//	 */
	//	double phi = Math.atan(-pImage_c[0].x/f);
	//	double omega = Math.atan(pImage_c[0].y*Math.cos(phi)/f);
	//	double rho = Math.atan(
	//			(pImage_c[1].y*Math.cos(omega) + pImage_c[1].x*Math.sin(phi)*Math.sin(omega) - 
	//					f*Math.cos(phi)*Math.sin(omega)) /
	//					(pImage_c[1].x*Math.cos(phi) + f*Math.sin(phi)) );
	//	Matrix4d Tfocal = new Matrix4d();
	//	Tfocal.setIdentity();
	//	Tfocal.setElement(2, 3, -f);
	//	
	//	Matrix4d T_c_to_d = new Matrix4d();
	//	T_c_to_d.mul(J3dUtils.eulerrot4d(rho, 1), J3dUtils.eulerrot4d(omega, 2));
	//	T_c_to_d.mul(T_c_to_d, J3dUtils.eulerrot4d(phi, 3));
	//	T_c_to_d.mul(T_c_to_d, Tfocal);
	//	// I DON'T KNOW WHY THIS TRANSFORMATION IS NECESSARY!!!!!!
	//	Matrix3d Tfix = new Matrix3d(0, 0, 1, 1, 0, 0, 0, 1, 0);
	//	Matrix3d T3 = new Matrix3d();
	//	T3.transpose(Tfix);
	//	T3.mul(T3, J3dUtils.eulerrot(rho, 1));
	//	T3.mul(T3, J3dUtils.eulerrot(omega, 2));
	//	T3.mul(T3, J3dUtils.eulerrot(phi, 3)); 
	//	T3.mul(T3, Tfix);
	//	T_c_to_d.set(T3);
	//	T_c_to_d.mul(T_c_to_d, Tfocal);
	//	Matrix4d T_d_to_c = new Matrix4d();
	//	T_d_to_c.invert(T_c_to_d);                            // CALCULATED
	//	
	//	/* Convert image to ideal coordinates.  Since we don't have the 3D vector
	//	 * knowledge of the camera, we cannot compute this directly.  Instead, we
	//	 * rely on our knowledge of how our pinhole camera operates
	//	 */
	//	Vector3d[] pImage_d = new Vector3d[4];
	//	// We only work with points 1 and 2, since 0 is our origin
	//	for (int i=1; i < 3; i++) {
	//		pImage_d[i] = new Vector3d();
	//		pImage_d[i].x = -f*(T_c_to_d.m00 * pImage_c[i].x + 
	//				T_c_to_d.m01*pImage_c[i].y - T_c_to_d.m02*f) / 
	//				(T_c_to_d.m20*pImage_c[i].x + T_c_to_d.m21*pImage_c[i].y - 
	//						T_c_to_d.m22*f);
	//		pImage_d[i].y = -f*(T_c_to_d.m10*pImage_c[i].x + 
	//				T_c_to_d.m11*pImage_c[i].y - T_c_to_d.m12*f) / 
	//				(T_c_to_d.m20*pImage_c[i].x + T_c_to_d.m21*pImage_c[i].y - 
	//						T_c_to_d.m22*f);                               // CALCULATED
	//	}
	//	
	//	/* ******************************************************************
	//	 **** THIS IS AN ERROR CHECK AND NOT PART OF THE COMPUTATION *******
	//	 **** It uses knowledge not available to the imaging system  *******
	//	 pAll_d = T_c_to_d*pAll_c;
	//	 **** End error check                                        *******
	//	 ******************************************************************/
	//	
	//	/* *****************************************************************
	//	 * This solution cannot be resolve analytically.  Instead, we resort
	//	 * to a numerical approach.  Note that there are FOUR possible solutions
	//	 */
	//	double X1a = pAll_s[1].x;
	//	double X2a = pAll_s[2].x;
	//	double Y2a = pAll_s[2].y;
	//	double x1p = pImage_d[1].x;
	//	double x2p = pImage_d[2].x;
	//	double y2p = pImage_d[2].y;
	//	
	////	* We start with C11, which we know to lie between -1 and 1.  First,
	////	* we scan that range, looking for crossovers.  (Why?  Because Matlab's
	////	* fsolve has trouble finding all four points on its own.) 
	////	*/
	////	guessCount = 0;
	////	numPts = 100;
	////	ctest = -1:(2/numPts):1;
	////	for (int i=0; i < numPts + 1; i++) {
	////	// Looking in cSolver, you'll see that there are two possible
	////	// solutions (C31 is +/- sqrt(1-C11^2) ).  We look for both
	////	cerr(i) = cSolver(ctest(i),f, x1p, x2p, y2p, X1a, X2a, Y2a, 1);
	////	cerr2(i) = cSolver(ctest(i),f, x1p, x2p, y2p, X1a, X2a, Y2a, -1);
	////	if (i > 1) {
	////	if (sign(cerr(i)) != sign(cerr(i-1))) {
	////	guessCount = guessCount + 1;
	////	guess(guessCount) = ctest(i-1);
	////	guessSign(guessCount) = 1;
	////	}
	////	if (sign(cerr2(i)) != sign(cerr2(i-1))) {
	////	guessCount = guessCount + 1;
	////	guess(guessCount) = ctest(i-1);
	////	guessSign(guessCount) = -1;
	////	}
	////	}
	////	}
	////	//				figure(1),clf,plot(ctest, cerr, ctest, cerr2), grid
	//	
	//	// Step through all four data points to find the possible solutions.
	//	int[] guess = {-1, -1, 0, 0, 1, 1};
	//	int[] guessSign = {1, -1, 1, -1, 1, -1};
	//	int guessCount = guess.length;
	//	Matrix4d[] Tcalc = new Matrix4d[guessCount];
	//	for (int i=0; i < guessCount; i++) {
	//		// Use fzero to find the value of C11 that zeros the error.  Use our
	//		// initial guesses, computed above
	////		C11 = fzero(@(u) cSolver(u,f, x1p, x2p, y2p, X1a, X2a, Y2a, ...
	////		guessSign(i)), guess(i));
	////		*     C11 = fminsearch(@(u) cSolver(u,f, x1p, x2p, y2p, X1a, X2a, Y2a, ...
	////		*         guessSign(i)), guess(i));
	//		// Compute the C matrix
	//		Matrix4d C = new Matrix4d();
	////		[ctest, C] = cSolver(C11,f, x1p, x2p, y2p, X1a, X2a, Y2a, ...
	////		guessSign(i));
	//		//*     fprintf('Testing for root *f: *f\n', C11, ctest);
	//		// Compute the transformation from world frame to camera frame
	//		Tcalc[i] = new Matrix4d();
	//		Tcalc[i].mul(T_d_to_c , C);
	//		Tcalc[i].mul(Tcalc[i], T_i_to_s);
	//		// The camera position is the last column of the inverse of this
	//		// transformation
	////		camera_i(:[i] = inv(Tcalc)*[0 0 0 1]';
	//	}
	////	// We know that the last row is 1, so we drop it
	////	camera_i = camera_i(1:3,:);
	//	Tcalc[0].invert();
	//	return(Tcalc[0]);
	//}

	//	private double[][] pImage_diagnostic = new double[2][4];

	/**
	 * Creates an equivalent image of the LEDs based on the inputs.
	 * 
	 * IMPORTANT!!!  Abidi's algorithm assumes a pinhole camera.  We need
	 * to mirror the points (i.e. reverse the signs).
	 * 
	 * @param points_i -- base-frame location of the points
	 * @param eulerAngles -- orientation of the camera
	 * @param rCamera_i -- position of the camera
	 * @param navC -- color of the navigation LEDs
	 * @param refC -- color of the reference LED (to find the origin)
	 * @param fov -- camera field of view
	 * @return
	 */
	public static BufferedImage buildPseudoImage(Point3d[] points_i, 
			Vector3d eulerAngles, Vector3d rCamera_i, Color3f navC,
			Color3f refC, double fov) {
		/* The transform is built from the euler angles and camera position
		 *   T_i_to_c = T_alpha * T_beta * T_gamma * T_offset
		 *   eulerAngles = [alpha beta gamma] for [1 2 3] rotation
		 */
		Transform3D[] ts = new Transform3D[4];
		ts[0] = new Transform3D();
		ts[0].rotX(-eulerAngles.x);
		ts[1] = new Transform3D();
		ts[1].rotY(-eulerAngles.y);
		ts[2] = new Transform3D();
		ts[2].rotZ(-eulerAngles.z);
		// Don't forget the sign!
		ts[3] = new Transform3D();
		rCamera_i.negate();
		ts[3].set(rCamera_i);
		Transform3D t3d = new Transform3D();
		t3d.mul(ts[0], ts[1]);
		t3d.mul(ts[2]);
		t3d.mul(ts[3]);
		//		t3d.invert();
		return(buildPseudoImage(points_i, t3d, navC, refC, fov));
	}

	/**
	 * Creates an equivalent image of the LEDs based on the inputs.
	 * 
	 * IMPORTANT!!!  Abidi's algorithm assumes a pinhole camera.  We need
	 * to mirror the points (i.e. reverse the signs).
	 * 
	 * @param points_i -- base-frame location of the points
	 * @param t3d -- transformation matrix of the camera
	 * @param navC -- color of the navigation LEDs
	 * @param refC -- color of the reference LED (to find the origin)
	 * @param fov -- camera field of view
	 * @return
	 */
	public static BufferedImage buildPseudoImage(Point3d[] points_i, 
			Transform3D t3d, Color3f navC, Color3f refC, double fov) {

		int N = points_i.length;
		Point3d[] points_c = new Point3d[N];
		double[][] pImage_c = new double[2][N];
		/* Build each of the camera-world points by transforming the base-frame
		 * into the camera's frame, and then into the image plane.
		 */
//		double f = 1 / Math.tan(fov);
		for (int i=0; i < N; i++) {
			points_c[i] = new Point3d();
			t3d.transform(points_i[i], points_c[i]);
			System.out.println("FPN1210: points_i to points_c: " + points_i[i] + " to " + points_c[i]);
			// Sign flip -- image is mirrored.
			pImage_c[0][i] = points_c[i].x / points_c[i].z / Math.tan(fov);
			pImage_c[1][i] = points_c[i].y / points_c[i].z / Math.tan(fov);			
//						pImage_c[0][i] = f* points_c[i].x / (f - points_c[i].z);
//						pImage_c[1][i] = f* points_c[i].y / (f - points_c[i].z);	
		}
				System.out.println("FPN855: pImage = " + Utils.arrayToStringBuffer(pImage_c));
		int w = 300;
		int h = (int) 1.1*w;
		// Write the image-plane points into the image
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		WritableRaster rast = bi.getRaster();
		int[] dRef = {
				(int) (refC.x*255),
				(int) (refC.y*255),
				(int) (refC.z*255),
		};
		int[] dNav = {
				(int) (navC.x*255),
				(int) (navC.y*255),
				(int) (navC.z*255),
		};
		int[] dWhite = {255, 255, 255};
		int[] dColor;
		double[] baseX = new double[N+1];
		double[] baseY = new double[N+1];
		int LEDradius = 2;
		/* Convert from image space (the range -1 to 1) to pixel space (the range 0
		 * to [dimension-1]).  Note that the final results are extremely sensitive
		 * to roundoff errors.  To try to manage that, we set the position of the
		 * base LED in pixel space by rounding, and then draw every other
		 * point relative to the base.
		 */
		// Mapping [-1 1] to [0 (w-1)]
		double led0x = (w-1) * (1 + pImage_c[0][0] ) / 2;
		baseX[0] = Math.round(led0x);
		// Mapping [-h/w h/w] to [(h-1) 0]  (note the direction-flip)
		double led0y = ( (h-1) * (1 - pImage_c[1][0] * w/h)  / 2);
		baseY[0] = Math.round(led0y);
		//		System.out.println("LED at X: " + baseX[0] + ", Y: " + baseY[0]);
		for (int i=1; i < N; i++) {
			double xRelative = (w-1) * (1 + pImage_c[0][i] ) / 2 - led0x;
			baseX[i] = baseX[0] + xRelative;
			// Mapping [-h/w h/w] to [(h-1) 0]  (note the direction-flip)
			double yRelative = ( (h-1) * (1 - pImage_c[1][i] * w/h)  / 2) - led0y;
			baseY[i] = baseY[0] + yRelative;
			//			System.out.println("LED at X: " + baseX[i] + ", Y: " + baseY[i]);
		}
		// Finally the marker to find LED0
		// The reference point is along the line from point 0 to point 3
		baseX[N] = (baseX[0] + 0.1*(baseX[3] - baseX[0]));
		baseY[N] = (baseY[0] + 0.1*(baseY[3] - baseY[0]));	
		// Put in the axes
		int divs = 10;
		for (int i=0; i < w; i++) {
			for (int j=1; j < divs; j++) {
				rast.setPixel(i, h*j/divs, dWhite);
			}
		}
		divs = 4;
		for (int i=0; i < h; i++) {
			for (int j=1; j < divs; j++) {
				rast.setPixel(w*j/divs, i, dWhite);				
			}
		}
		// Write the reference point, then the nav (so that nav overlaps reference)
		for (int i = N; i >= 0; i--) {
			if (i == N) {
				// The green point is along the line from point 0 to point 3
				dColor = dRef;
			} else {
				dColor = dNav;
			}
			for (int j=0; j <= LEDradius; j++) {
				for (int k=0; k <= LEDradius; k++) {
					if (j*j + k*k <= LEDradius*LEDradius) {
						tryToSetPixel(rast, 
								(int) baseX[i] + j, (int) baseY[i] + k, dColor);						
						tryToSetPixel(rast, 
								(int) baseX[i] - j, (int) baseY[i] + k, dColor);						
						tryToSetPixel(rast, 
								(int) baseX[i] - j, (int) baseY[i] - k, dColor);						
						tryToSetPixel(rast, 
								(int) baseX[i] + j, (int) baseY[i] - k, dColor);						
					}
				}
			}
		}
		return(bi);
	}

	/** This method ensures we don't try to write outside of the image boundaries.
	 * 
	 * @param rast
	 * @param x
	 * @param y
	 * @param data
	 */
	private static void tryToSetPixel(WritableRaster rast, int x, int y, int[] data) {
		if ((x >= 0) & (x < rast.getWidth())) {
			if ((y >= 0) & (y < rast.getHeight())) {
				rast.setPixel(x, y, data);
			}
		}
	}

	public BufferedImage getProcessedNavImage() {
		return navI;
	}
}
