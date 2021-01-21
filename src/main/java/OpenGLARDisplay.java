
import static org.lwjgl.glfw.GLFW.glfwTerminate;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.joml.Vector3f;
import org.liquidengine.legui.system.context.Context;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.opencv.core.KeyPoint;

import buffers.Buffer;
import entities.Camera;
import entities.Entity;
import gui.GUIComponents;
import models.RawModel;
import models.TexturedModel;
import renderEngine.Loader;
import renderEngine.Renderer;
import runtimevars.Parameters;
import shaders.StaticShader;
import textures.ModelTexture;
import types.Correspondence2D2D;
import types.PipelineOutput;
import types.Point3D;
import types.Pose;

public class OpenGLARDisplay {

	Buffer<PipelineOutput> pipelineBuffer = null;

	Loader loader;
	Renderer renderer;
	Camera camera;
	Camera mapCamera;
	ArrayList<Entity> entities = new ArrayList<Entity>();
	StaticShader cameraShader;
	Entity rawFrameEntity;
	Entity processedFrameEntity;
	StaticShader bgShader;
	StaticShader colorShader;

	Pose pose = new Pose();
	List<Correspondence2D2D> correspondences = new ArrayList<Correspondence2D2D>();
	List<KeyPoint> features = new ArrayList<KeyPoint>();
	List<Point3D> mapPoints = new ArrayList<Point3D>();
	List<Pose> poses = new ArrayList<Pose>();

	// legui
	GUIComponents gui = new GUIComponents();

	public OpenGLARDisplay() {
		this.initOpenGL();
	}

	public OpenGLARDisplay(Buffer<PipelineOutput> pipelineBuffer) {
		this.pipelineBuffer = pipelineBuffer;
		this.initOpenGL();
	}

	public void initOpenGL() {

		// initialize
		this.gui.initGUI();

		this.loader = new Loader();
		this.cameraShader = new StaticShader(StaticShader.VERTEX_FILE, StaticShader.FRAGMENT_FILE);
		this.colorShader = new StaticShader(StaticShader.VERTEX_FILE, StaticShader.COLOR_FRAGMENT_FILE);
		StaticShader[] shaders = { this.cameraShader, this.colorShader };
		this.renderer = new Renderer(shaders);

		// temporarily set up cube data (eventually load from blender)
		float[] vertices = { -0.5f, 0.5f, 0, -0.5f, -0.5f, 0, 0.5f, -0.5f, 0, 0.5f, 0.5f, 0,

				-0.5f, 0.5f, 1, -0.5f, -0.5f, 1, 0.5f, -0.5f, 1, 0.5f, 0.5f, 1,

				0.5f, 0.5f, 0, 0.5f, -0.5f, 0, 0.5f, -0.5f, 1, 0.5f, 0.5f, 1,

				-0.5f, 0.5f, 0, -0.5f, -0.5f, 0, -0.5f, -0.5f, 1, -0.5f, 0.5f, 1,

				-0.5f, 0.5f, 1, -0.5f, 0.5f, 0, 0.5f, 0.5f, 0, 0.5f, 0.5f, 1,

				-0.5f, -0.5f, 1, -0.5f, -0.5f, 0, 0.5f, -0.5f, 0, 0.5f, -0.5f, 1

		};

//		float[] textureCoords = {
//
//				0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0,
//				1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0
//
//		};

//		float[] textureCoords = {
//
//				1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1,
//				1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0
//
//		};

		float[] textureCoords = { 0.25f, 0f, 0.25f, 0.25f, 0.5f, 0.25f, 0.5f, 0, 0.25f, 0.25f, 0.25f, 0.5f, 0.5f, 0.5f,
				0.5f, 0.25f, 0.25f, 0.5f, 0.25f, 0.75f, 0.5f, 0.75f, 0.5f, 0.5f, 0.25f, 0.75f, 0.25f, 1f, 0.5f, 1f,
				0.5f, 0.75f, 0f, 0.25f, 0f, 0.5f, 0.25f, 0.5f, 0.25f, 0.25f, 0.5f, 0.25f, 0.5f, 0.5f, 0.75f, 0.5f,
				0.75f, 0.25f };

		int[] indices = { 0, 1, 3, 3, 1, 2, 4, 5, 7, 7, 5, 6, 8, 9, 11, 11, 9, 10, 12, 13, 15, 15, 13, 14, 16, 17, 19,
				19, 17, 18, 20, 21, 23, 23, 21, 22

		};

		RawModel tModel = this.loader.loadToVAO(vertices, textureCoords, indices);
		TexturedModel tStaticModel = new TexturedModel(tModel,
				new ModelTexture(this.loader.loadTexture("solid_colors_64")));
		Entity tEntity = new Entity(tStaticModel, new Vector3f(0.109906f, -0.122303f, 1.1223031f), 0, 0, 0, 0.05f);
//		Entity tEntity = new Entity(tStaticModel, new Vector3f(0, 0, 1.5f), 0, 0, 0, 0.5f);
		this.entities.add(tEntity);

		Random rand = new Random(100);

//		// right line of boxes
//		for (int i = 0; i < 100; i++) {
//			this.entities.add(new Entity(tStaticModel, new Vector3f(50f, 0f, (i - 50) * 10), 0, 0, 0, 1f));
//		}
//
//		// left line of boxes
//		for (int i = 0; i < 100; i++) {
//			this.entities.add(new Entity(tStaticModel, new Vector3f(-50f, 0f, (i - 50) * 10), 0, 0, 0, 1f));
//		}
//
//		// bottom plane of boxes
//		for (int i = 0; i < 100; i++) {
//			this.entities.add(new Entity(tStaticModel,
//					new Vector3f(rand.nextFloat() * 250 - 125, 0f, rand.nextFloat() * 250 - 125), 0, 0, 0, 0.5f));
//		}

		// create camera
		this.camera = new Camera();
		this.mapCamera = new Camera();

		// create background data
		float[] bgVertices = { -1, 1, 0, -1, -1, 0, 1, -1, 0, 1, 1, 0 };

		int[] bgIndices = { 0, 1, 3, 3, 1, 2 };

		float[] bgTextureCoords = { 0, 0, 0, 1, 1, 1, 1, 0 };

		// set up background models
		this.bgShader = new StaticShader(StaticShader.BG_VERTEX_FILE, StaticShader.FRAGMENT_FILE);
		RawModel rawBgModel = this.loader.loadToVAO(bgVertices, bgTextureCoords, bgIndices);
		TexturedModel rawBgStaticModel = new TexturedModel(rawBgModel,
				new ModelTexture(this.loader.loadTexture("sample_texture")));
		this.rawFrameEntity = new Entity(rawBgStaticModel, new Vector3f(0, 0, -10), 0, 0, 0, 2000);

		RawModel processedBgModel = this.loader.loadToVAO(bgVertices, bgTextureCoords, bgIndices);
		TexturedModel processedBgStaticModel = new TexturedModel(processedBgModel,
				new ModelTexture(this.loader.loadTexture("sample_texture")));
		this.processedFrameEntity = new Entity(processedBgStaticModel, new Vector3f(0, 0, -10), 0, 0, 0, 2000);

	}

	public void updateDisplay(Context context) {

		context.updateGlfwWindow();
		this.renderer.prepare();
		if (this.gui.getView() == GUIComponents.VIEW.AR) {

			GL11.glViewport(0, 0, Parameters.screenWidth, Parameters.screenHeight);
			this.renderer.render(this.camera, this.entities, this.cameraShader, this.rawFrameEntity, this.bgShader);

		} else if (this.gui.getView() == GUIComponents.VIEW.PROCESSED) {

			GL11.glViewport(0, 0, Parameters.screenWidth, Parameters.screenHeight);
			this.renderer.renderProcessedView(this.processedFrameEntity, this.bgShader, this.correspondences,
					this.features, this.gui.getFeatureDisplayType());

		} else if (this.gui.getView() == GUIComponents.VIEW.MAP) {

			GL11.glViewport(0, 0, Parameters.screenWidth, Parameters.screenHeight);
			this.renderer.renderMapView(this.mapCamera, this.colorShader, this.mapPoints, this.poses, this.pose, 3);

		} else if (this.gui.getView() == GUIComponents.VIEW.ALL) {

			GL11.glViewport(0, 0, Parameters.screenWidth / 2, Parameters.screenHeight / 2);
			this.renderer.render(this.camera, this.entities, this.cameraShader, this.rawFrameEntity, this.bgShader);
			GL11.glViewport(Parameters.screenWidth / 2, 0, Parameters.screenWidth / 2, Parameters.screenHeight / 2);
			this.renderer.renderProcessedView(this.processedFrameEntity, this.bgShader, this.correspondences,
					this.features, this.gui.getFeatureDisplayType());
			GL11.glViewport(Parameters.screenWidth / 2, Parameters.screenHeight / 2, Parameters.screenWidth / 2,
					Parameters.screenHeight / 2);
			this.renderer.renderMapView(this.mapCamera, this.colorShader, this.mapPoints, this.poses, this.pose, 2);

		}

		// render gui frame
		this.gui.renderGUI();

	}

	public void setFrameToTexture(byte[] bytes, Entity bgEntity, boolean bgr) {

		ByteBuffer pixels = ByteBuffer.allocateDirect(bytes.length);
		pixels.put(bytes);
		pixels.flip();

		// delete old texture and create new texture
		GL11.glDeleteTextures(bgEntity.getModel().getTexture().getID());
		int textureID = GL11.glGenTextures();
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		if (bgr) {
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, Parameters.width, Parameters.height, 0, GL11.GL_RGB,
					GL11.GL_UNSIGNED_BYTE, pixels);
		} else {
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_LUMINANCE, Parameters.width, Parameters.height, 0,
					GL11.GL_LUMINANCE, GL11.GL_UNSIGNED_BYTE, pixels);
		}

		GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

		// set bgEntity texture to new texture
		bgEntity.getModel().getTexture().setID(textureID);
	}

	public void setCameraPose(double r00, double r01, double r02, double r10, double r11, double r12, double r20,
			double r21, double r22, double tx, double ty, double tz) {
		this.camera.setMatrix(r00, r01, r02, r10, r11, r12, r20, r21, r22, tx, ty, tz);
		this.mapCamera.setMatrix(r00, r01, r02, r10, r11, r12, r20, r21, r22, tx, ty + 0.5, tz + 2);
	}

	public void detectChanges() {

		PipelineOutput output;
		synchronized (this.pipelineBuffer) {
			output = this.pipelineBuffer.getNext();
		}

		if (output == null) {
			return;
		}

		this.setCameraPose(output.pose.getR00(), output.pose.getR01(), output.pose.getR02(), output.pose.getR10(),
				output.pose.getR11(), output.pose.getR12(), output.pose.getR20(), output.pose.getR21(),
				output.pose.getR22(), output.pose.getTx(), output.pose.getTy(), output.pose.getTz());

		if (output.rawFrameBuffer != null) {
			this.setFrameToTexture(output.rawFrameBuffer, this.rawFrameEntity, true);
		}

		if (output.processedFrameBuffer != null) {
			this.setFrameToTexture(output.processedFrameBuffer, this.processedFrameEntity, false);
		}

		this.pose = output.pose;
		this.correspondences = output.correspondences;
		this.features = output.features;
		this.mapPoints = output.points;
		this.poses = output.cameras;

		this.gui.updateFpsLabel(output.fps);
		this.gui.updateFrameNumLabel(output.frameNum);
		this.gui.updateNumFeaturesLabel(output.numFeatures);

	}

	public void displayLoop() {

		Context context = this.gui.getInitializer().getContext();

		while (this.gui.isRunning()) {
			this.detectChanges();
			this.updateDisplay(context);
		}

		this.gui.destroy();
		glfwTerminate();
		this.cameraShader.cleanUp();
		this.loader.cleanUp();

	}

}
