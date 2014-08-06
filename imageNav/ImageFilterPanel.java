package imageNav;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.vecmath.Color3f;

import mas.swing.ImagePanel;
import mas.swing.SlideBox;

public class ImageFilterPanel extends ImagePanel implements ChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected FourPointNav fpn = new FourPointNav();
	protected SlideBox sp;
	private Color3fPanel ledColorChooser; 
	private BufferedImage baseImage;	// The un-edited image. Stored so we can re-run

	public ImageFilterPanel(String title, Color3f baseColor) {
		super(title);
		init(baseColor, 128);
	}
	
	private void init(Color3f baseColor, int baseSphere) {
		ledColorChooser = new Color3fPanel(baseColor, "Filter Color");
		ledColorChooser.addChangeListener(this);

		// Now the controls
		JPanel controlPanel = new JPanel(new BorderLayout());
		add(controlPanel, BorderLayout.EAST);
		controlPanel.add(ledColorChooser.getPanel(), BorderLayout.CENTER);
		sp = new SlideBox(SlideBox.HORIZONTAL, "Color Match", 0, 
				255, 1, "", Color.LIGHT_GRAY);
		sp.setEnabled(true);
		sp.addChangeListener(this);
		sp.setValue(baseSphere);
		controlPanel.add(sp, BorderLayout.NORTH);
	}

	public ImageFilterPanel(String title, Color3f baseColor, int baseSphere) {
		super(title);
		init(baseColor, baseSphere);
	}

	public void processImage(BufferedImage newImage) {
		baseImage = newImage;
		if (newImage != null) {
			super.processImage(ImageNavUtils.colorFilter(newImage, ledColorChooser, 
					getMatchThreshold(), false));
		}
	}

	public Color3f getFilterColor() {
		return ledColorChooser;
	}

	public int getMatchThreshold() {
		return (int) sp.getValue();
	}

	public void stateChanged(ChangeEvent ce) {
		processImage(baseImage);	
	}

}
