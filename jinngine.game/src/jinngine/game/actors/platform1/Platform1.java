package jinngine.game.actors.platform1;

import java.nio.FloatBuffer;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.bounding.BoundingVolume;
import com.ardor3d.extension.model.collada.jdom.ColladaImporter;
import com.ardor3d.extension.model.collada.jdom.data.ColladaStorage;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Image.Format;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.light.DirectionalLight;
import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Matrix3;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyMatrix3;
import com.ardor3d.renderer.state.BlendState;
import com.ardor3d.renderer.state.LightState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.BlendState.DestinationFunction;
import com.ardor3d.renderer.state.BlendState.SourceFunction;
import com.ardor3d.renderer.state.FogState.DensityFunction;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.Line;
import com.ardor3d.scenegraph.Node;
import com.ardor3d.scenegraph.Spatial;
import com.ardor3d.scenegraph.controller.SpatialController;
import com.ardor3d.scenegraph.hint.LightCombineMode;
import com.ardor3d.scenegraph.shape.Box;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.geom.BufferUtils;

import jinngine.game.Game;
import jinngine.game.actors.PhysicalActor;
import jinngine.physics.Body;
import jinngine.physics.PhysicsScene;
import jinngine.physics.force.GravityForce;

public class Platform1 implements PhysicalActor {

	public Body platformbox1body;
	private Node platformbox1;
	private static jinngine.math.Vector3 pos = new jinngine.math.Vector3();
	private boolean enableLight = true;
	private double scale = 0.5;
	private float shade ;
	
	public Platform1(jinngine.math.Vector3 pos, boolean lighting, double scale, double shade) {
		this.enableLight = lighting;
		this.pos.assign(pos);
		this.scale = scale;
		this.shade = (float)shade;
	}
	
	@Override
	public void act( Game game ) {
		// TODO Auto-generated method stub

	} 

	@Override
	public void start( Game game ) {
		PhysicsScene physics = game.getPhysics();
		Node rootnode = game.getRendering().getRootNode();

		
//		final ColladaImporter colladaImporter = new ColladaImporter();
//        final ColladaStorage storage = colladaImporter.readColladaFile("platformbox1.dae");
//        platformbox1 = storage.getScene();
//        platformbox1.setTranslation(new Vector3(0,-25,0));
//        platformbox1.setScale(scale);
//        rootnode.attachChild(platformbox1);

		// Make a box...
		platformbox1 = new Node();
		rootnode.attachChild(platformbox1);
        
		// make the outline
		ColorRGBA[] colors = new ColorRGBA[24];
		for ( int i=0; i<colors.length; i++)
			colors[i] = new ColorRGBA(0f,0f,0f,1.0f);
		
		// define outline lines for the box
		Vector3[] outline = new Vector3[]  { 
				new Vector3( scale, scale, scale), new Vector3(-scale, scale, scale),
				new Vector3( scale, scale, scale), new Vector3( scale,-scale, scale),
				new Vector3( scale, scale, scale), new Vector3( scale, scale,-scale),
				
				new Vector3(-scale, scale, scale), new Vector3(-scale,-scale, scale),
				new Vector3(-scale, scale, scale), new Vector3(-scale, scale,-scale),
				
				new Vector3( scale,-scale, scale), new Vector3(-scale,-scale, scale),
				new Vector3( scale,-scale, scale), new Vector3( scale,-scale,-scale),
				
				new Vector3(-scale,-scale, scale), new Vector3(-scale,-scale,-scale),

				new Vector3( scale, scale,-scale), new Vector3(-scale, scale,-scale),
				new Vector3( scale, scale,-scale), new Vector3( scale,-scale,-scale),

				new Vector3(-scale, scale,-scale), new Vector3(-scale,-scale,-scale),
				
				new Vector3( scale,-scale,-scale), new Vector3(-scale,-scale,-scale)
				
		};

		Line line = new Line("vector", outline, null, colors, null);
		line.setAntialiased(false);
        line.setModelBound(new BoundingBox());
        line.setLineWidth(4f);
        

        line.getSceneHints().setLightCombineMode(LightCombineMode.Off);
    
//        LightState _lightState = new LightState();
//        _lightState.setEnabled(false);
//        line.setRenderState(_lightState);


        
        platformbox1.attachChild(line);

		Box box = new Box("Box", new Vector3(), scale, scale, scale);
        // Setup a bounding box for it.
        box.setModelBound(new BoundingBox());
  
//        FloatBuffer buffer = BufferUtils.createColorBuffer(4*6);
//        for (int i=0; i<4*6; i=i+1) {
//        	buffer.put(0.5f);
//        	buffer.put(0.5f);
//        	buffer.put(0.5f);
//        	buffer.put(1);
//        }
//        box.getMeshData().setColorBuffer(buffer);
        
        box.setSolidColor(new ColorRGBA(shade,shade,shade,1));
        
		platformbox1.attachChild(box);

        
        
//        Texture tex = TextureManager.load("platformbox1texlarge.tga", 
//        		Texture.MinificationFilter.Trilinear,
//                Format.Guess, true);
//        tex.setMagnificationFilter(MagnificationFilter.Bilinear);
//        tex.setAnisotropicFilterPercent(0);
//        TextureState headts = new TextureState();
//        headts.setEnabled(true);
//        headts.setTexture( tex, 0 );
//        platformbox1.setRenderState(headts);
		
        // define some light
        final DirectionalLight light = new DirectionalLight();
        light.setDirection(0.5, 1, 0);
        light.setDiffuse(new ColorRGBA(0.2f, 0.2f, 0.2f, 1.0f));
        light.setAmbient(new ColorRGBA(1.9f, 1.9f, 1.9f, 1.0f));
        light.setSpecular(new ColorRGBA(0.0f, 0.0f, 0.0f, 0.0f));
        light.setAttenuate(false);
        light.setEnabled(true);
        
        final LightState ls = new LightState();
        ls.attach(light);
        ls.setEnabled(true);
        //platformbox1.setRenderState(ls);

        // connect the node with this actor
        platformbox1.setUserData(this);
        
        //setup shadowing
        game.getRendering().getPssmPass().add(platformbox1);
        game.getRendering().getPssmPass().addOccluder(box);
        
        platformbox1body = new Body(new jinngine.geometry.Box(scale*2,scale*2,scale*2));
        physics.addBody(platformbox1body);
        physics.addForce(new GravityForce(platformbox1body));
        platformbox1body.setPosition(pos);
        platformbox1body.setAngularVelocity(new jinngine.math.Vector3(0,0,0));
               
        platformbox1.addController(new SpatialController<Spatial>() {
            public void update(final double time, final Spatial caller) {
            	Body body = platformbox1body;
            	caller.setTranslation(body.state.position.x, body.state.position.y, body.state.position.z);
            	ReadOnlyMatrix3 mat = new Matrix3(body.state.rotation.a11, body.state.rotation.a12, body.state.rotation.a13,
            			body.state.rotation.a21, body.state.rotation.a22, body.state.rotation.a23, 
            			body.state.rotation.a31, body.state.rotation.a32, body.state.rotation.a33);
            	
            	caller.setRotation(mat);
            }
        });
	}

	@Override
	public void stop( Game game ) {
		// TODO Auto-generated method stub

	}

	@Override
	public Body getBodyFromNode(Node node) {
		if (node == platformbox1)
			return platformbox1body;
		else
			return null;
	}

}