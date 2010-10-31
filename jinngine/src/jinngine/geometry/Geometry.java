/**
 * Copyright (c) 2008-2010  Morten Silcowitz.
 *
 * This file is part of the Jinngine physics library
 *
 * Jinngine is published under the GPL license, available 
 * at http://www.gnu.org/copyleft/gpl.html. 
 */
package jinngine.geometry;
import jinngine.math.*;
import jinngine.physics.*;

/**
 * A general encapsulation for geometries used in jinngine. All implementation of geometry classes 
 * should implement this interface.
 */
public interface Geometry extends BoundingBox {
	/**
	 * Return the body that is associated with this Geometry instance
	 * @return 
	 */
	public Body getBody();
		
	/**
	 * Specify a body to be associated with this Geometry instance. Normally, it should not be necessary for the user 
	 * to call this method directly. A geometry will have a body assigned automatically, when the geometry is added to
	 * the body
	 */
	public void setBody(Body b);
	
	/**
	 * Set the local rotation for this geometry and displacement. The local transform is applied before the transform 
	 * of the associated body is applied. This is used to rotate and translate geometries around in the
	 * object space. This transform must not include any scaling or other transformation types. That sort of 
	 * transformations are to be handled internally by the geometry instance. 
	 * 
	 * @param R An orthonormal rotation matrix
	 * @param b Displacement vector
	 */
	public void setLocalTransform( Matrix3 R, Vector3 b);
	
	/**
	 * Get the local transform for this geometry. The rotation and displacement is assigned to 
	 * the given matrix and vector. 
	 * @param R Matrix to contain the rotation matrix of the local transform
	 * @param b A vector to contain the local transform displacement
	 */
	public void getLocalTransform( Matrix3 R, Vector3 b);
	
	/**
	 * Return the local translation of this geometry
	 * @param t 
	 */
	public void getLocalTranslation( Vector3 t ) ;
		
	/**
	 * Set the local scale. Not all geometries will support scaling and/or non-uniform scaling
	 * @param s
	 */
	public void setLocalScale( Vector3 s);
	
	
	/** 
	 * Get the amount of mass for this geometry
	 */
	public double getMass();
	
	/**
	 * Return the inertia matrix for this geometry, given in the centre of mass frame and scaled to mass = 1
	 */
	public InertiaMatrix getInertiaMatrix();
	
	/**
	 * Get the envelope size for this geometry. The envelope is related to narrow-phase contact determination
	 * and describes a positive distance to a geometry surface. If another geometry is placed within this
	 * distance, the two geometries are considered to be in contact.
	 * @return
	 */
	public double getEnvelope();
	
	/**
	 * Set the collision envelope size. The size of the collision envelope determines when contact points are generated by
	 * narrow-phase contact generation, and when broad-phase collision detection declares geometries to be in vicinity of 
	 * each other. Too small envelopes results in often occurring penetrations, which are computationally more expensive to 
	 * handle, and hurts the quality of animation. Too large envelopes will cause objects be appearing to hover above each other,
	 * and results in excessive amounts of contact points, which hurts both performance and accuracy of force computations.
	 */
	public void setEnvelope(double envelope);

	/**
	 * Get the final transform for this geometry, going from object space to world space.
	 * @return 4x4 affine transformation matrix
	 */
	public Matrix4 getWorldTransform();
	
	/**
	 * Get the auxiliary reference. This reference is a way for the user to 
	 * link geometry objects to some user space object 
	 * @return
	 */
	public Object getUserReference();
		
	/**
	 * Set the auxiliary reference. This reference is a way for the user to 
	 * link geometry objects to some user space reference
	 */
	public void setUserReference(Object user);
	
	/**
	 * Get the centre of mass position in local space
	 * @param cm Vector3 reference to contain the centre of mass position
	 * @return the reference to the cm object
	 */
	public Vector3 getLocalCentreOfMass(Vector3 cm);
	
}

