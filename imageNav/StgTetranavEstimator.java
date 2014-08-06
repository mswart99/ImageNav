package imageNav;

import java.awt.image.BufferedImage;

import javax.vecmath.*;

import mas.spacecraft.SpacecraftTransformGroup;
import mas.spacecraft.StatePacket;
import mas.spacecraft.StgEstimator;

public abstract class StgTetranavEstimator extends StgEstimator {

	/** Create an estimator based on the given spacecraft.
	 * 
	 * @param stgParent
	 */
	public StgTetranavEstimator(SpacecraftTransformGroup stgParent) {
		// Call the constructor that deals strictly with the TransformGroup
		super(stgParent);
	}

	private Vector3d rTemp = new Vector3d();
	private Vector3d rTemp2 = new Vector3d();
	private Quat4d qerr = new Quat4d();
	private Quat4d q2 = new Quat4d();
	private Vector3d qv = new Vector3d();
	protected FourPointNav imageNav;
//	protected BufferedImage img;

	public void initImageNav(Vector3d[] refPoints) {
		imageNav = new FourPointNav();
		imageNav.setWorld(refPoints);
	}

	/** Run imageNav with the given color filters.
	 * 
	 * @param image
	 * @param fov
	 * @param navColor
	 * @param refColor
	 */
	public void runImageNav(BufferedImage image, double fov, Color3f navColor, 
			Color3f refColor, int brightThreshold) {
		imageNav.tetraNav(image, navColor, refColor, transform_C_from_N, 
				fov, 
				brightThreshold, false);
		// Get actual position
		parent.getState().getLocalPositionStandardUnits(rTemp2);
		parent.getState().getQuaternion(q);
		// Transformation from camera to camera base coordinates
		//		transform_C_to_N.invert();
		//		System.out.println("BE192: ImageNav transform:  " + transform_C_from_N);
		// Position vector in camera base coordinates
		transform_C_from_N.get(rTemp);
		/* If ImageNav has no solution, it will spit out NaN in the position
		 * column.  (The direction cosine matrix will be the identity).
		 */
		if (!Double.isNaN(rTemp.length())) {
			//			System.out.println("\tCamera location in C-frame " + rTemp);				
			//			pTemp.set(rTemp);
			//			System.out.println("BE96: Dock3D " + transform_A_to_C);
			// Position vector in Akoya body coordinates
			//			transform_A_from_C.transform(pTemp);
			//			System.out.println("\tCamera location in A-frame " + pTemp);
			// Now in orbital coordinates
			//			transform_O_from_A.transform(pTemp);
			//			System.out.println("\tCamera location in O-frame: " + pTemp);
			// Just to be sure, take it again from the top
			//			pTemp.set(rTemp);
			//			transform_O_from_C.transform(pTemp);
			//			System.out.println("\tCamera location in O-frame (other calc): " + pTemp);

			// One more time, with feeling
			// First build transform_O_from_N
			transform_O_from_N.mul(transform_O_from_C, transform_C_from_N);
			//			transform_O_from_N.get(rTemp);
			//			System.out.println("\tCamera location in O-frame (end-to-end calc): " + rTemp);
			// Now transform to B
			transform_O_from_B.mul(transform_O_from_N, transform_N_from_B);
			//			transform_B_to_O.mul(transform_B_to_N, transform_N_to_O);
			//			System.out.println("Bandit's actual transform: " + parent.getTransform());
			//			System.out.println("Bandit's computed transform: " + transform_O_from_B);
			//			Transform3D inv = new Transform3D(parent.getTransform());
			//			inv.invert();
			//			System.out.println("Bandit's inverse transform: " + inv);
			transform_O_from_B.get(q2, rTemp);
			System.out.println("\tBandit's true center in O-frame: " + rTemp2);
			System.out.println("\tLocation of Bandit center in O-frame: " + rTemp);
			rTemp2.sub(rTemp);
			System.out.println("\tError: " + rTemp2 + "\n(" + 100*rTemp2.length() + " cm)");
			System.out.println("\tBandit's true quat from statepacket: " + q);
			System.out.println("\tBandit's computed quat: " + q2);
			qerr.set(q);
			qerr.inverse();
			qerr.mul(q2);
			qv.set(q.x, q.y, q.z);
			qv.normalize();
			System.out.println("\tQuaternion error " + qerr + "\n\t" + qv + "\n(" 
					+ 2*Math.acos(qerr.w)*180/Math.PI + " deg)");
			sp.setQuat(q2);
			// Smooth by adding 10% of the difference between rLast and rTemp to rLast
			System.out.println("STGE285:  Smoothing the imagenav");
			sp.getLocalPositionStandardUnits(rLast);
			smoother.sub(rTemp, rLast);
			smoother.scale(0.1);
			rLast.add(smoother);
			sp.setLocalPosition(rLast, StatePacket.simUnits[StatePacket.POSITION[0]]);
			//			sp.setLocalPosition(rTemp, StatePacket.simUnits[StatePacket.POSITION[0]]);
		}
	}

	private Vector3d smoother = new Vector3d();
	private Vector3d rLast = new Vector3d();

	public BufferedImage getImageFromImageNav() {
		return(imageNav.getProcessedNavImage());
	}
}