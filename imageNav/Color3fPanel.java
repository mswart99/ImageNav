package imageNav;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.vecmath.Color3f;

import mas.swing.NumberTextField;
import mas.swing.SlideBox;

public class Color3fPanel extends Color3f implements ActionListener, ChangeListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected JPanel colorPanel;
	protected JPanel colorBox;

	public Color3fPanel(float x, float y, float z, String name) {
		super(x, y, z);
		init(name);
	}

	public Color3fPanel(Color3f color, String name) {
		super(color);
		init(name);
	}

	private void init(String name) {
		colorPanel = new JPanel(new GridLayout(4,1));
		colorBox = new JPanel();
		colorBox.setBorder(BorderFactory.createLineBorder(Color.black));
		colorBox.setBackground(get());
		colorPanel.add(colorBox);
		colorPanel.add(buildSlider(x, "R", this, Color.red));
		colorPanel.add(buildSlider(y, "G", this, Color.green));
		colorPanel.add(buildSlider(z, "B", this, Color.blue));
		colorPanel.setBorder(BorderFactory.createTitledBorder(name));
	}
	
	private static SlideBox buildSlider(float value, String name, ChangeListener cl, 
			Color sliderColor) {
		SlideBox sp = new SlideBox(SlideBox.HORIZONTAL, name, 0, 
				255, 1, "", sliderColor);
		sp.setValue(255*value);
		sp.setSlideName(name);
		sp.setEnabled(true);
		sp.addChangeListener(cl);
		return(sp);
	}

	/** Respond to changes to the RGB boxes by updating the color.
	 * 
	 */
	public void actionPerformed(ActionEvent aEvent) {
		// This 
		NumberTextField ntf = (NumberTextField) aEvent.getSource();
		double newVal = ntf.getValueFromText();
		// Error check
		newVal = Math.max(newVal, 0);
		newVal = Math.min(newVal, 255);
		ntf.set(newVal);
		String actionC = aEvent.getActionCommand();
		if (actionC.equals("R")) {
			x = (float) (newVal/255);
		} else if (actionC.equals("G")) {
			y = (float) (newVal/255);
		} else if (actionC.equals("B")) {
			z = (float) (newVal/255);
		}
		// Update the Box
		colorBox.setBackground(get());
		fireStateChanged();
	}
	
	public void stateChanged(ChangeEvent cEvent) {
		// This 
		JSlider sb = (JSlider) cEvent.getSource();
		double newVal = sb.getValue();
		// Error check
		newVal = Math.max(newVal, 0);
		newVal = Math.min(newVal, 255);
		String name = sb.getName();
		if (name.equals("R")) {
			x = (float) (newVal/255);
		} else if (name.equals("G")) {
			y = (float) (newVal/255);
		} else if (name.equals("B")) {
			z = (float) (newVal/255);
		}
		// Update the Box
		colorBox.setBackground(get());
		// Let our external listeners know
		fireStateChanged();
	}
	
	public JPanel getPanel() {
		return(colorPanel);
	}

	/** ChangeListener are listening for changes to the colorPanel, indicating
	 * that the values have been changed
	 * 
	 * @param al
	 */
	public void addChangeListener(ChangeListener listener) {
	    listenerList.add(ChangeListener.class, listener);
	}
	
	protected EventListenerList listenerList = new EventListenerList();

	protected void fireStateChanged() {
	    ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
	    if (listeners != null && listeners.length > 0) {
	        ChangeEvent evt = new ChangeEvent(colorPanel);
	        for (ChangeListener listener : listeners) {
	            listener.stateChanged(evt);
	        }
	    }
	}
}
