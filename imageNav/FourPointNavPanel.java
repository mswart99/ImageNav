package imageNav;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;

import javax.media.j3d.Transform3D;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;

import mas.j3d.utils.Vector3dPanel;
import mas.swing.NumberTextField;
import mas.swing.NumberTextPanel;
import mas.swing.PopPanel;

public class FourPointNavPanel extends PopPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private FourPointNav fpn;
	private Transform3D t3d;
	private Vector3dPanel[] ledPanels;
	private NumberTextPanel fovPanel;
	private NumberTextField[] t3dFields = new NumberTextField[16];
	/** Location of the other three LEDs, relative to the reference. The reference LED must be at (0,0,0),
	 * and the rest must go counter-clockwise
	 */
	private double[][] LEDlocations = {
			{0, 125, 0}, {148, 125, 0}, {140, 0, 0},  
	};	// Millimeters
	private Point3d[] LEDpoints = new Point3d[LEDlocations.length];

	public FourPointNavPanel() {
		super(new BorderLayout());
		init();
	}
	
	private void init() {
		// Initialize the variables
		for (int i=0; i < LEDpoints.length; i++) {
			LEDpoints[i] = new Point3d(LEDlocations[i]);
		}
		fpn = new FourPointNav();
		// First the display Panel
		JPanel displayPanel = new JPanel();
		fovPanel = new NumberTextPanel("FOV", "degrees");
		fovPanel.setValue(45.0);
		displayPanel.add(fovPanel);
		add(displayPanel, BorderLayout.NORTH);
		// Next the navigation LED panels
		JPanel jp = new JPanel(new BorderLayout());
		jp.setBorder(BorderFactory.createLineBorder(Color.black, 1));
		JPanel parentPanel = new JPanel(new GridLayout(1,4));
		ledPanels = new Vector3dPanel[4];
		// The first panel is at (0, 0, 0) by definition
		ledPanels[0] = new Vector3dPanel("LED0", 0, 0, 0);
		ledPanels[0].setEditable(false);
		parentPanel.add(ledPanels[0].getPanel());
		for (int i=1; i < 4; i++) {
			ledPanels[i] = new Vector3dPanel("LED "+ i, 
					LEDlocations[i-1]);
			parentPanel.add(ledPanels[i].getPanel());
		}
		// Add
		jp.add(parentPanel, BorderLayout.CENTER);
		jp.add(new JLabel(
				"LEDs must be arranged counter-clockwise from the reference\nas viewed by the camera"),
				BorderLayout.NORTH);
		add(jp, BorderLayout.SOUTH);
		// Initialize fpn with the LED locations
		fpn.setWorld(ledPanels);
		// And the Transform3D panel
		JPanel t3dPanel = new JPanel(new GridLayout(4, 4));
		t3dPanel.setBorder(BorderFactory.createTitledBorder("Transform"));
		for (int i=0; i < 16; i++) {
			t3dFields[i] = new NumberTextField(0);
			t3dPanel.add(t3dFields[i]);
		}
		add(t3dPanel, BorderLayout.CENTER);
		t3d = new Transform3D();
		setTransform();
	}
	
	
	/** Run the tetraNav function on the given image, with the given parameters, and then push
	 *  the results into the display panels.
	 * @param image
	 * @param navLEDcolor
	 * @param referenceLEDcolor
	 * @param colorMatch
	 * @param show
	 */
	public void tetraNavCCD(BufferedImage image, 
			Color3f navLEDcolor, Color3f referenceLEDcolor, int colorMatch, boolean show) {
		System.out.println("Starting with cot=" + getFOVcotangent() + " from " + fovPanel.getValue());
		double cotFOV = fpn.tetraNavCCD(
				fpn.convertImageToNavPoints(image, navLEDcolor, referenceLEDcolor, colorMatch, show), 
				t3d, getFOVcotangent());
		System.out.println("Ending with cot=" + cotFOV + " becoming " + Math.atan(1/cotFOV)*180.0/Math.PI);
		fovPanel.setValue(Math.atan(1/cotFOV)*180.0/Math.PI);
		// Rewrite the t3d panel
		setTransform();
	}
	
	public BufferedImage buildPseudoImage(Color3f navLEDcolor, Color3f referenceLEDcolor) {
		return(FourPointNav.buildPseudoImage(LEDpoints, t3d, 
				navLEDcolor, referenceLEDcolor, getFOVcotangent()));
	}
	
	public void setTransform() {
		double[] newVals = new double[16];
		t3d.get(newVals);
		for (int i=0; i < 16; i++) {
			t3dFields[i].set(newVals[i]);
		}
	}
	
	public double getFOVcotangent() {
//		return(fovPanel.getValue());
		return(1/Math.tan(fovPanel.getValue()*Math.PI/180.0));
	}

}
