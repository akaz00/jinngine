package jinngine.physics;
import java.util.*;

import jinngine.physics.solver.*;
import jinngine.physics.solver.Solver.constraint;
import jinngine.geometry.contact.*;
import jinngine.collision.*;
import jinngine.geometry.*;
import jinngine.math.*;
import jinngine.physics.force.*;
import jinngine.physics.constraint.*;
import jinngine.util.*;
import jinngine.util.ComponentGraph.Component;

/**
 * A fixed time stepping rigid body simulator. It uses a contact graph to organise constraints, and generates
 * contact constraints upon intersecting/touching geometries. The engine is limited to having one constraint per. 
 * body pair. This means that if a joint constraint is present, there can be no contact constraints simultaneously. Such behaviour 
 * should be modelled using joint limits. 
 * 
 * @author mo
 */
public final class Engine implements Model {
	// Bodies in model
	public final List<Body> bodies = new ArrayList<Body>();
	
	// List of geometry classifiers
	private final List<ContactGeneratorClassifier> geometryClassifiers = new ArrayList<ContactGeneratorClassifier>();

	// Constraints, joints and forces
	private final List<constraint> constraintList = new LinkedList<constraint>();  
	private final List<Force> forces = new LinkedList<Force>(); 
	
	// Create a contact graph classifier, used by the contact graph for determining
	// fixed bodies, i.e. bodies considered to have infinite mass. 
	private final HashMapComponentGraph.NodeClassifier<Body> classifier = 
		new HashMapComponentGraph.NodeClassifier<Body>() {
			public boolean isDelimitor(Body node) {
				return node.isFixed();
			}
	};

	// Create the contact graph using the classifier above
	private final HashMapComponentGraph<Body,Constraint> contactGraph = new HashMapComponentGraph<Body,Constraint>(classifier);

	// Set of maintained contact constraints and generators
	private final Map<Pair<Body>,ContactConstraint> contactConstraints = new HashMap<Pair<Body>,ContactConstraint>();
	private final Map<Pair<Geometry>,ContactGenerator> contactGenerators = new HashMap<Pair<Geometry>,ContactGenerator>();
	
	// Setup a broad-phase handler. The handler ensures that ContactConstraints are properly inserted and 
	// removed from the contact graph, whenever the broad-phase collision detection detects overlaps and separations
	private final BroadfaseCollisionDetection.Handler handler = 
		new BroadfaseCollisionDetection.Handler() {
			@Override
			public final void overlap(Pair<Geometry> inputpair) {
				//retrieve the bodies associated with overlapping geometries
				Body a = inputpair.getFirst().getBody();
				Body b = inputpair.getSecond().getBody();
				
				//ignore overlaps stemming from the same body				
				if ( a == b) return;
				//ignore overlaps for non-body geometries
				if ( a == null || b == null) return;
				//ignore overlaps of fixed bodies
				if ( a.isFixed() && b.isFixed() ) return;

				
				
				//always order bodies and geometries the same way, so that normals 
				//will be pointing the right direction
				Pair<Body> bpair;
				Pair<Geometry> gpair;
				if ( a.hashCode() > b.hashCode()) {
					bpair = new Pair<Body>(a,b);
					gpair = new Pair<Geometry>(inputpair.getFirst(), inputpair.getSecond());
				} else {
					bpair = new Pair<Body>(b,a);
					gpair = new Pair<Geometry>(inputpair.getSecond(), inputpair.getFirst());
				}
				
				
				ContactConstraint contactConstraint;

				//a contact constraint already exists 
				if (contactConstraints.containsKey(bpair)) {
					contactConstraint = contactConstraints.get(bpair);
															
					//add a new contact generator to this contact constraint
					ContactGenerator generator = getContactGenerator(gpair);
					contactGenerators.put(gpair, generator);
					contactConstraint.addGenerator(generator);

				//no contact constraint is present
				} else {					
					//do not act if some other constraint(joint) is already present
					//in the contact graph
					if (contactGraph.getEdge(bpair) == null)  {
						//create a new contact generator
						ContactGenerator generator = getContactGenerator(gpair);
						
						//create a new contact constraint
						contactConstraint = new FrictionalContactConstraint(bpair.getFirst(),bpair.getSecond(),generator);
//						constraint = new CorrectionContact(a,b,generator);
								
						//insert into data structures
						contactConstraints.put(bpair, contactConstraint);
						contactGenerators.put(gpair, generator);
						contactGraph.addEdge( bpair, contactConstraint);
					}
				}
			}
			
			@Override
			public final void separation(Pair<Geometry> pair) {
				//retrieve the bodies associated with overlapping geometries
				Body a = pair.getFirst().getBody();
				Body b = pair.getSecond().getBody();
				Pair<Body> bpair = new Pair<Body>(a,b);

				//ignore overlaps stemming from the same body				
				if ( a == b) return;
				//ignore overlaps for non-body geometries
				if ( a == null || b == null) return;
				//ignore overlaps of fixed bodies
				if ( a.isFixed() && b.isFixed() ) return;
				
				//if this geometry pair has an acting contact constraint,
				//we must remove the contact generator
				if (contactConstraints.containsKey(bpair)) {
					//check that we have the generator (if not, something is very wrong)
					if (contactGenerators.containsKey(pair)) {
				
						//remove the generator from the contact constraint
						ContactConstraint constraint = contactConstraints.get(bpair);					
						constraint.removeGenerator(contactGenerators.get(pair));
						
						//remove the generator from our list
						contactGenerators.remove(pair);

						//if the contact constraint has no more generators, also
						//remove the contact constraint
						if (constraint.getNumberOfGenerators() < 1 ) {
							contactConstraints.remove(bpair);
							contactGraph.removeEdge(bpair);	
						}
					} else {
						//this is not good, report an error
						System.out.println("missing contact generator");
						System.exit(0);
					}
				} else {
					//TODO enable this and do tests
					//this is not good, report an error
					//System.out.println("no constraint pressent");
					//System.exit(0);
				}
			}
	};
	
	// Broad phase collision detection implementation 
	private BroadfaseCollisionDetection broadfase = new SweepAndPrune(handler);
//	private BroadfaseCollisionDetection broadfase = new AllPairsTest(handler);

	//Create a linear complementarity problem solver
	private Solver solver = new ProjectedGaussSeidel();
	//time-step size
	private double dt = 0.01; 

	public Engine() {
		//create some initial ContactGeneratorClassifiers
		//The sphere-sphere classifier
		geometryClassifiers.add(new ContactGeneratorClassifier() {
			@Override
			public ContactGenerator createGeneratorFromGeometries(Geometry a,
					Geometry b) {
				if ( a instanceof jinngine.geometry.Sphere && b instanceof jinngine.geometry.Sphere) {
					return new SphereContactGenerator((jinngine.geometry.Sphere)a, (jinngine.geometry.Sphere)b);
					//return new SamplingSupportMapContacts(a.getBody(),b.getBody(), (SupportMap3)a, (SupportMap3)b);
				}
				//not recognised
				return null;	
			}
		});
		
		//General convex support maps
		geometryClassifiers.add(new ContactGeneratorClassifier() {
			@Override
			public ContactGenerator createGeneratorFromGeometries(Geometry a,
					Geometry b) {
				if ( a instanceof SupportMap3 && b instanceof SupportMap3) {
					//return new SupportMapContactGenerator2((SupportMap3)a, a,  (SupportMap3)b, b);
					//return new SupportMapContactGenerator2((SupportMap3)a, a,  (SupportMap3)b, b);
					return new FeatureSupportMapContactGenerator((SupportMap3)a, a,  (SupportMap3)b, b);
				}
				//not recognised
				return null;	
			}
		});
	}

	/**
	 *  Core method in jinngine is the Model.timeStep() method. The method, 
	 *  in order, does the following:
	 *  
	 *  1. clear out all auxiliary parameters
	 *  a. Apply forces to bodies
	 *  b. Integrate forward on velocities
	 *  2. run broad-phase collision detection
	 *  3. if overlaps, run narrow-phase collision detection, and possibly 
	 *     generating contact constraints using specified parameters (e.g. with 
	 *     or without friction)
	 *  4. Apply constraints generated by modeled joints (e.g a fixed joint, generating 6 constraints)
	 *  5. Solve the velocity based LCP problem using the solver
	 *  6. Apply impulses (delta velocities) to bodies
	 *  7. Integrate forward on positions
	 *  
	 */
	private final void timeStep() {
		// Clear acting forces and auxillary variables
		for (Body c:bodies) {
			c.clearForces();
			c.deltaVCm.assign(Vector3.zero);
			c.deltaOmegaCm.assign(Vector3.zero);
		}

        // Apply all forces	
		for (Force f: forces) {
			f.apply(dt);
		}

		// Run the broad-phase collision detection (this automatically updates the contactGraph,
		// through the BroadfaseCollisionDetection.Handler type)
		broadfase.run();
		
		// Create a special iterator to be used with joints. All joints use this iterator to 
		// insert constraint entries into the constraintList
		constraintList.clear();
		ListIterator<constraint> constraintIterator = constraintList.listIterator();
		
		// Iterate through groups/components in the contact graph
		Iterator<ComponentGraph.Component> components = contactGraph.getComponents();		
		while (components.hasNext()) {
			//The component 
			ComponentGraph.Component g = components.next();
			
			Iterator<Constraint> constraints = contactGraph.getEdgesInComponent(g);
			while (constraints.hasNext()) {
				constraints.next().applyConstraints(constraintIterator, dt);
			}
		} //while groups
		
		//run the solver
		solver.solve( constraintList, bodies, 0.0 );
		
		// Apply delta velocities and integrate positions forward
		for (Body c : bodies ) {
			//don't process inactive bodies
			if (c.ignore)
				continue;
						
			// Apply computed forces to bodies
			if ( !c.isFixed() ) {
				c.state.vCm.assign( c.state.vCm.add( c.deltaVCm));
				c.state.omegaCm.assign( c.state.omegaCm.add( c.deltaOmegaCm));	
				Matrix3.multiply(c.state.I, c.state.omegaCm, c.state.L);
				c.state.P.assign(c.state.vCm.multiply(c.state.M));
			}

			//integrate forward on positions
			c.advancePositions(dt);
		}
	} //time-step


	@Override
	public void setDt(double dt) {
		this.dt = dt;
	}

	public void addForce( Force f ) {
		forces.add(f);
	}

	public void removeForce(Force f) {
		forces.remove(f);
	}


	public void addBody( Body c) {
		bodies.add(c);
		c.updateTransformations();
		
		//install geometries into the broad-phase collision detection
		Iterator<Geometry> i = c.getGeometries();
		while (i.hasNext()) {
			Geometry g = i.next();
			broadfase.add(g);
		}
	}
	
	public void addConstraint(Pair<Body> pair, Constraint joint) {
		contactGraph.addEdge(pair, joint);
	}
	
	
	public List<Constraint> getConstraints() {
		List<Constraint> list = new ArrayList<Constraint>();
		Iterator<Component> ci = contactGraph.getComponents();
		while(ci.hasNext()) {
			Iterator<Constraint> ei = contactGraph.getEdgesInComponent(ci.next());
			while(ei.hasNext())
				list.add(ei.next());
		}
			
		return list;
	}
	
	public final void removeConstraint(Pair<Body> pair) {
		contactGraph.removeEdge(pair);
	}

	@Override
	public final BroadfaseCollisionDetection getBroadfase() {
		return broadfase;
	}  
	
	private ContactGenerator getContactGenerator(Pair<Geometry> pair) {
		for ( ContactGeneratorClassifier gc: geometryClassifiers) {
			ContactGenerator g = gc.createGeneratorFromGeometries(pair.getFirst(), pair.getSecond());
			
			if (g!=null) {
				return g;
			}
		}
		return null;
	}

	@Override
	public final void addContactGeneratorClasifier(
			ContactGeneratorClassifier classifier) {
		geometryClassifiers.add(classifier);
		
	}

	@Override
	public final void removeBody(Body body) {
		//remove associated geometries from collision detection
		Iterator<Geometry> i = body.getGeometries();
		while( i.hasNext()) {
			broadfase.remove(i.next());			
		}
		
		//finally remove from body list
		bodies.remove(body);
		
	}

	@Override
	public final void tick() {
		timeStep();
	}

	@Override
	public final double getDt() {
		return dt;
	}

	@Override
	public final Solver getSolver() {
		return solver;
	}

	@Override
	public final void setSolver(Solver s) {
		solver = s;
	}

	@Override
	public Iterator<Body> getBodies() {
		return bodies.iterator();
	}
}
