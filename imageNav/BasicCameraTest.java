package imageNav;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Color3f;

import mas.j3d.LedGroup;
import mas.j3d.utils.Color3fPanel;
import mas.j3d.utils.FourPointNav;
import mas.j3d.utils.J3dUtils;
import mas.swing.ImageFilterPanel;
import mas.swing.ImagePanel;
import mas.swing.SlideBox;
import mas.swing.SliderPanel;
import mas.utils.Utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;


public class BasicCameraTest extends JFrame implements Runnable {
	//final int INTERVAL=1000;///you may use interval
	//	IplImage image;
	//	CanvasFrame canvas = new CanvasFrame("Web Cam");

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {
		BasicCameraTest gs = new BasicCameraTest();
	}

	protected ImagePanel ip;
	protected Webcam webcam;
	protected Thread th;
	protected FourPointNav fpn = new FourPointNav();
	protected ImageFilterPanel[] processedImages = new ImageFilterPanel[2];
//	protected SlideBox sp;

	public BasicCameraTest() {
		super("Test webcam panel");
		th = new Thread(this);

		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		JPanel gridPanel = new JPanel(new GridLayout(2,2,5,5));
		cp.add(gridPanel, BorderLayout.CENTER);

		java.util.List<Webcam> allcams = Webcam.getWebcams();
		webcam = allcams.get(0);
		Dimension[] viewSizes = webcam.getViewSizes();
		webcam.setViewSize(viewSizes[0]);
		WebcamPanel webcamPanel = new WebcamPanel(webcam);
		gridPanel.add(webcamPanel);

		ip = new ImagePanel("Webcam analytics");
		gridPanel.add(ip);
		processedImages[0] = new ImageFilterPanel("Navigation LED finder", LedGroup.green);
		processedImages[1] = new ImageFilterPanel("Reference LED finder", LedGroup.red);
		for (int i=0; i < 2; i++) {
			gridPanel.add(processedImages[i]);
		}

		// Now the controls
//		JPanel controlPanel = new JPanel(new BorderLayout());
//		cp.add(controlPanel, BorderLayout.NORTH);
//		controlPanel.add(navLEDcolor.getPanel(), BorderLayout.WEST);
//		controlPanel.add(refLEDcolor.getPanel(), BorderLayout.EAST);
//		sp = new SlideBox(SlideBox.HORIZONTAL, "Color Match", 0, 
//				255, 1, "", Color.LIGHT_GRAY);
//		sp.setEnabled(true);
//		controlPanel.add(sp, BorderLayout.NORTH);

		pack();
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		th.start();
	}

	// TO DO: create Color3fPanel that allows us to set RGB and see it as a color on the panel
	// TO DO: create brightnessSlider to set brightness threshold (0-255)

//	private Color3fPanel navLEDcolor = new Color3fPanel(LedGroup.green, "Navigation Color");
//	private Color3fPanel refLEDcolor = new Color3fPanel(LedGroup.red, "Reference Color");

	public void run() {
		while (th == Thread.currentThread()) {
			Utils.sleep(2000);
			BufferedImage bi = webcam.getImage();
			ip.processImage(bi);
//			int colorMatchThreshold = (int) sp.getValue();
			processedImages[0].processImage(bi);
			processedImages[1].processImage(bi);
			fpn.convertImageToNavPoints(bi, processedImages[0].getFilterColor(), 
					processedImages[1].getFilterColor(), 
					processedImages[0].getMatchThreshold(), false);
		}
	}
}
