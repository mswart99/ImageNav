package imageNav;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Color3f;

import mas.j3d.LedGroup;
import mas.j3d.utils.Color3fPanel;
import mas.j3d.utils.FourPointNav;
import mas.j3d.utils.FourPointNavPanel;
import mas.j3d.utils.J3dUtils;
import mas.swing.ImageFilterPanel;
import mas.swing.ImagePanel;
import mas.swing.SlideBox;
import mas.swing.SliderPanel;
import mas.utils.ImageFolderFilter;
import mas.utils.Utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;


public class BasicCameraTest extends JFrame implements Runnable, ActionListener {
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
	protected FourPointNavPanel fpn;
	protected ImageFilterPanel[] processedImages = new ImageFilterPanel[2];
	//	protected SlideBox sp;
	protected WebcamPanel webcamPanel;
	protected JButton nextButton;
	public static final Color3f navColor = new Color3f(102.0f/255, 230.0f/255, 97.0f/255);
	public static final Color3f refColor = new Color3f(207.0f/255, 24.0f/255/255, 43.0f/255/255);
	public static final int navSphere = 71;
	public static final int refSphere = 74;
	

	public BasicCameraTest() {
		super("Test webcam panel");

		Container cp = getContentPane();
		cp.setLayout(new BorderLayout());
		JPanel controlPanel = new JPanel();
		cp.add(controlPanel, BorderLayout.NORTH);

		JRadioButton runButton = new JRadioButton("Run");
		runButton.addActionListener(this);
		controlPanel.add(runButton);
		JRadioButton stopButton = new JRadioButton("Stop");
		stopButton.addActionListener(this);
		controlPanel.add(stopButton);
		// Link the two
		ButtonGroup bg = new ButtonGroup();
		bg.add(runButton);
		bg.add(stopButton);

		JRadioButton camButton = new JRadioButton("Webcam");
		camButton.addActionListener(this);
		controlPanel.add(camButton);
		JRadioButton fileButton = new JRadioButton("Local Files");
		fileButton.addActionListener(this);
		controlPanel.add(fileButton);
		ButtonGroup bg2 = new ButtonGroup();
		bg2.add(camButton);
		bg2.add(fileButton);

		nextButton = new JButton("Next Image");
		nextButton.addActionListener(this);
		nextButton.setEnabled(false);
		controlPanel.add(nextButton);

		JButton loadButton = new JButton("Load Image");
		loadButton.addActionListener(this);
		controlPanel.add(loadButton);

		JButton doNavButton = new JButton("Re-Do Nav");
		doNavButton.addActionListener(this);
		controlPanel.add(doNavButton);

		JPanel gridPanel = new JPanel(new GridLayout(3,2,5,5));
		cp.add(gridPanel, BorderLayout.CENTER);

		java.util.List<Webcam> allcams = Webcam.getWebcams();
		webcam = allcams.get(0);
		Dimension[] viewSizes = webcam.getViewSizes();
		webcam.setViewSize(viewSizes[0]);
		// Load it but don't start
		webcamPanel = new WebcamPanel(webcam, false);
		gridPanel.add(webcamPanel);

		ip = new ImagePanel("Webcam analytics");
		gridPanel.add(ip);
		processedImages[0] = new ImageFilterPanel("Navigation LED finder", navColor, navSphere);
		processedImages[1] = new ImageFilterPanel("Reference LED finder", refColor, refSphere);
		for (int i=0; i < 2; i++) {
			gridPanel.add(processedImages[i]);
		}
		fpn = new FourPointNavPanel();
		gridPanel.add(fpn);

		pack();
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	private boolean useWebcam = true;	// Default is to use the webcam

	public void actionPerformed(ActionEvent aEvent) {
		String actionC = aEvent.getActionCommand();
		if (actionC.equals("Run")) {
			th = new Thread(this);
			th.start();

		} else if (actionC.equals("Stop")) {
			th = null;
		} else if (actionC.equals("Webcam")) {
			webcamPanel.start();
			useWebcam = true;
			nextButton.setEnabled(false);
		} else if (actionC.equals("Local Files")) {
			useWebcam = false;
			if (webcamPanel != null) {
				webcamPanel.pause();
			}
			localImages = Utils.loadLocalImages(this);
			imageCounter = 0;
			nextButton.setEnabled(true);
		} else if (actionC.equals("Load Image")) {
			loadAndProcessImage();
		} else if (actionC.equals("Next Image")) {
			processImage(imageCounter++);
		} else if (actionC.equals("Re-Do Nav")) {
			runImage(activeImage);
		}

	}

	public void processImage(int imageNum) {
		// Reset if we've run over the list
		if (imageNum >= localImages.length) {
			imageNum = 0;
		}
		runImage(localImages[imageNum]);
	}

	/** Default is to have localImages hold only one image. This prevents null errors.
	 * 
	 */
	protected BufferedImage[] localImages;
	protected BufferedImage activeImage;
	protected int imageCounter;

	public void loadAndProcessImage() {
		File imgFile = Utils.openFile(this, "Choose Image File", ImageFolderFilter.imageExtensions, 
				"Image Files");		
		try {
			runImage(ImageIO.read(imgFile));
		} catch (IOException ioe) {
			System.err.println("Trouble with i/o on " + imgFile.getAbsolutePath()
					+ ": " + ioe.getMessage());
		}
	}

	public void run() {
		while (th == Thread.currentThread()) {
			Utils.sleep(2000);
			if (useWebcam) {
				runImage(webcam.getImage());
			}
		}
	}

	public void runImage(BufferedImage bi) {
		if (bi == null) {
			System.err.println("Tried to process a null image");
			return;
		}
		// Store this image as the active image (so we can re-run)
		activeImage = bi;
		// Put it on the main ImagePanel, unaltered
		ip.processImage(bi);
		//		int colorMatchThreshold = (int) sp.getValue();
		// Process it on both of our processing panels
		processedImages[0].processImage(bi);
		processedImages[1].processImage(bi);
		// Attempt a nav solution
		fpn.tetraNavCCD(bi, processedImages[0].getFilterColor(), 
				processedImages[1].getFilterColor(), 
				processedImages[0].getMatchThreshold(), true);
	}

}
