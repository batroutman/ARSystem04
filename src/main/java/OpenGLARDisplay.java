
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_H;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.joml.Vector3f;
import org.liquidengine.legui.DefaultInitializer;
import org.liquidengine.legui.animation.AnimatorProvider;
import org.liquidengine.legui.component.Button;
import org.liquidengine.legui.component.Component;
import org.liquidengine.legui.component.Frame;
import org.liquidengine.legui.component.Label;
import org.liquidengine.legui.component.RadioButton;
import org.liquidengine.legui.component.RadioButtonGroup;
import org.liquidengine.legui.event.CursorEnterEvent;
import org.liquidengine.legui.event.MouseClickEvent;
import org.liquidengine.legui.listener.CursorEnterEventListener;
import org.liquidengine.legui.listener.MouseClickEventListener;
import org.liquidengine.legui.listener.processor.EventProcessorProvider;
import org.liquidengine.legui.style.border.SimpleLineBorder;
import org.liquidengine.legui.style.color.ColorConstants;
import org.liquidengine.legui.system.context.CallbackKeeper;
import org.liquidengine.legui.system.context.Context;
import org.liquidengine.legui.system.layout.LayoutManager;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWWindowCloseCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import buffers.Buffer;
import entities.Camera;
import entities.Entity;
import models.RawModel;
import models.TexturedModel;
import renderEngine.Loader;
import renderEngine.Renderer;
import runtimevars.Parameters;
import shaders.StaticShader;
import textures.ModelTexture;
import toolbox.Utils;
import types.Correspondence2D2D;
import types.PipelineOutput;

public class OpenGLARDisplay {

	Buffer<PipelineOutput> pipelineBuffer = null;

	int AR_VIEW = 0;
	int PROCESSED_VIEW = 1;
	int MAP_VIEW = 2;
	int ALL_VIEW = 3;
	int view = ALL_VIEW;

	Loader loader;
	Renderer renderer;
	Camera camera;
	ArrayList<Entity> entities = new ArrayList<Entity>();
	StaticShader cameraShader;
	Entity rawFrameEntity;
	Entity processedFrameEntity;
	StaticShader bgShader;

	ArrayList<Correspondence2D2D> correspondences = new ArrayList<Correspondence2D2D>();

	// legui
	private long window;
	private Frame leguiframe;
	private DefaultInitializer initializer;
	private volatile boolean running = false;
	private volatile boolean hiding = false;

	public OpenGLARDisplay() {
		this.initOpenGL();
	}

	public OpenGLARDisplay(Buffer<PipelineOutput> pipelineBuffer) {
		this.pipelineBuffer = pipelineBuffer;
		this.initOpenGL();
	}

	public void initOpenGL() {

		// initialize
//		DisplayManager.createDisplay(Parameters.width, Parameters.height);
		if (!GLFW.glfwInit()) {
			throw new RuntimeException("Can't initialize GLFW");
		}
		window = createWindow();
		leguiframe = createFrameWithGUI();
		initializer = new DefaultInitializer(window, leguiframe);

		initializeGuiWithCallbacks();
		running = true;

		this.loader = new Loader();
		this.cameraShader = new StaticShader(false);
		this.renderer = new Renderer(this.cameraShader);

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

		// create background data
		float[] bgVertices = { -1, 1, 0, -1, -1, 0, 1, -1, 0, 1, 1, 0 };

		int[] bgIndices = { 0, 1, 3, 3, 1, 2 };

		float[] bgTextureCoords = { 0, 0, 0, 1, 1, 1, 1, 0 };

		// set up background models
		this.bgShader = new StaticShader(true);
		RawModel rawBgModel = this.loader.loadToVAO(bgVertices, bgTextureCoords, bgIndices);
		TexturedModel rawBgStaticModel = new TexturedModel(rawBgModel,
				new ModelTexture(this.loader.loadTexture("sample_texture")));
		this.rawFrameEntity = new Entity(rawBgStaticModel, new Vector3f(0, 0, -10), 0, 0, 0, 2000);

		RawModel processedBgModel = this.loader.loadToVAO(bgVertices, bgTextureCoords, bgIndices);
		TexturedModel processedBgStaticModel = new TexturedModel(processedBgModel,
				new ModelTexture(this.loader.loadTexture("sample_texture")));
		this.processedFrameEntity = new Entity(processedBgStaticModel, new Vector3f(0, 0, -10), 0, 0, 0, 2000);

	}

	private long createWindow() {
		long window = glfwCreateWindow(Parameters.screenWidth, Parameters.screenHeight, "AR System 04", NULL, NULL);
		glfwShowWindow(window);

		glfwMakeContextCurrent(window);
		GL.createCapabilities();
		glfwSwapInterval(0);
		return window;
	}

	private Frame createFrameWithGUI() {
		Frame frame = new Frame(Parameters.width, Parameters.height);

		// Set background color for frame
		frame.getContainer().getStyle().getBackground().setColor(ColorConstants.transparent());
		frame.getContainer().setFocusable(false);

		Button button = new Button("Add components", 20, 20, 160, 30);
		SimpleLineBorder border = new SimpleLineBorder(ColorConstants.black(), 1);
		button.getStyle().setBorder(border);

		boolean[] added = { false };
		button.getListenerMap().addListener(MouseClickEvent.class, (MouseClickEventListener) event -> {
			if (!added[0]) {
				added[0] = true;
				for (Component c : generateOnFly()) {
					frame.getContainer().add(c);
				}
			}
		});

		button.getListenerMap().addListener(CursorEnterEvent.class, (CursorEnterEventListener) System.out::println);

		frame.getContainer().add(button);
		return frame;
	}

	private List<Component> generateOnFly() {
		List<Component> list = new ArrayList<>();

		Label label = new Label(20, 60, 200, 20);
		label.getTextState().setText("Generated on fly label");
		label.getStyle().setTextColor(ColorConstants.red());

		RadioButtonGroup group = new RadioButtonGroup();
		RadioButton radioButtonFirst = new RadioButton("First", 20, 90, 200, 20);
		RadioButton radioButtonSecond = new RadioButton("Second", 20, 110, 200, 20);

		radioButtonFirst.setRadioButtonGroup(group);
		radioButtonSecond.setRadioButtonGroup(group);

		list.add(label);
		list.add(radioButtonFirst);
		list.add(radioButtonSecond);

		return list;
	}

	private void initializeGuiWithCallbacks() {
		GLFWKeyCallbackI escapeCallback = (w1, key, code, action,
				mods) -> running = !(key == GLFW_KEY_ESCAPE && action != GLFW_RELEASE);

		// used to skip gui rendering
		GLFWKeyCallbackI hideCallback = (w1, key, code, action, mods) -> {
			if (key == GLFW_KEY_H && action == GLFW_RELEASE)
				hiding = !hiding;
		};
		GLFWWindowCloseCallbackI windowCloseCallback = w -> running = false;

		CallbackKeeper keeper = initializer.getCallbackKeeper();
		keeper.getChainKeyCallback().add(escapeCallback);
		keeper.getChainKeyCallback().add(hideCallback);
		keeper.getChainWindowCloseCallback().add(windowCloseCallback);

		org.liquidengine.legui.system.renderer.Renderer renderer = initializer.getRenderer();
		renderer.initialize();
	}

	public void updateDisplay(Context context, org.liquidengine.legui.system.renderer.Renderer ren) {

		context.updateGlfwWindow();
		this.renderer.prepare();
		if (this.view == AR_VIEW) {
			GL11.glViewport(0, 0, Parameters.screenWidth, Parameters.screenHeight);
			this.renderer.render(this.camera, this.entities, this.cameraShader, this.rawFrameEntity, this.bgShader);
		} else if (this.view == PROCESSED_VIEW) {
			GL11.glViewport(0, 0, Parameters.screenWidth, Parameters.screenHeight);
			this.renderer.renderProcessedView(this.processedFrameEntity, this.bgShader, this.correspondences);
		} else if (this.view == ALL_VIEW) {
			GL11.glViewport(0, 0, Parameters.screenWidth / 2, Parameters.screenHeight / 2);
			this.renderer.render(this.camera, this.entities, this.cameraShader, this.rawFrameEntity, this.bgShader);
			GL11.glViewport(Parameters.screenWidth / 2, 0, Parameters.screenWidth / 2, Parameters.screenHeight / 2);
			this.renderer.renderProcessedView(this.processedFrameEntity, this.bgShader, this.correspondences);
		}

		// render gui frame
		if (!hiding) {
			GL11.glViewport(0, 0, Parameters.screenWidth, Parameters.screenHeight);
			ren.render(leguiframe, context);
		}
		// poll events to callbacks
		glfwPollEvents();
		glfwSwapBuffers(window);
		// Now we need to process events. Firstly we need to process system
		// events.
		initializer.getSystemEventProcessor().processEvents(leguiframe, context);

		// When system events are translated to GUI events we need to
		// process them.
		// This event processor calls listeners added to ui components
		EventProcessorProvider.getInstance().processEvents();

		// When everything done we need to relayout components.
		LayoutManager.getInstance().layout(leguiframe);

		// Run animations. Should be also called cause some components use
		// animations for updating state.
		AnimatorProvider.getAnimator().runAnimations();

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
	}

	public void detectChanges() {

		PipelineOutput output;
		synchronized (this.pipelineBuffer) {
			output = this.pipelineBuffer.getNext();
		}

		if (output == null) {
			return;
		}
		if (!output.finalFrame) {
			this.setCameraPose(output.r00, output.r01, output.r02, output.r10, output.r11, output.r12, output.r20,
					output.r21, output.r22, output.tx, output.ty, output.tz);
		}

		if (output.rawFrameBuffer != null) {
			this.setFrameToTexture(output.rawFrameBuffer, this.rawFrameEntity, true);
		}

		if (output.processedFrameBuffer != null) {
			this.setFrameToTexture(output.processedFrameBuffer, this.processedFrameEntity, false);
		}
		this.correspondences = output.correspondences;

	}

	public void displayLoop() {

		Context context = initializer.getContext();
		org.liquidengine.legui.system.renderer.Renderer ren = initializer.getRenderer();

		while (running) {
			long start = System.currentTimeMillis();
			this.detectChanges();
			this.updateDisplay(context, ren);
			long end = System.currentTimeMillis();
			Utils.pl("framerate: " + (1000 / (end - start)));
		}
		initializer.getRenderer().destroy();
		glfwDestroyWindow(window);
		glfwTerminate();
		this.cameraShader.cleanUp();
		this.loader.cleanUp();

	}

}
