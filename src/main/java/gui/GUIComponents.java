package gui;

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
import static org.lwjgl.system.MemoryUtil.NULL;

import org.liquidengine.legui.DefaultInitializer;
import org.liquidengine.legui.animation.AnimatorProvider;
import org.liquidengine.legui.component.Component;
import org.liquidengine.legui.component.Frame;
import org.liquidengine.legui.component.Label;
import org.liquidengine.legui.component.RadioButton;
import org.liquidengine.legui.component.RadioButtonGroup;
import org.liquidengine.legui.component.Widget;
import org.liquidengine.legui.event.MouseClickEvent;
import org.liquidengine.legui.listener.MouseClickEventListener;
import org.liquidengine.legui.listener.processor.EventProcessorProvider;
import org.liquidengine.legui.style.color.ColorConstants;
import org.liquidengine.legui.system.context.CallbackKeeper;
import org.liquidengine.legui.system.context.Context;
import org.liquidengine.legui.system.layout.LayoutManager;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWWindowCloseCallbackI;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import runtimevars.Parameters;

public class GUIComponents {

	private long window;
	private Frame leguiframe;
	private DefaultInitializer initializer;
	private volatile boolean running = false;
	private volatile boolean hiding = false;

	public static final int AR_VIEW = 0;
	public static final int PROCESSED_VIEW = 1;
	public static final int MAP_VIEW = 2;
	public static final int ALL_VIEW = 3;
	int view = ALL_VIEW;

	private Widget mainWidget = new Widget(20, 20, 200, 400);
	private Label viewLabel = new Label(10, 10, 100, 10);
	private RadioButtonGroup viewButtonGroup = new RadioButtonGroup();
	private RadioButton arViewButton = new RadioButton(10, 10, 100, 10);
	private RadioButton processedViewButton = new RadioButton(10, 10, 100, 10);
	private RadioButton mapViewButton = new RadioButton(10, 10, 100, 10);
	private RadioButton allViewButton = new RadioButton(10, 10, 100, 10);
	private Label frameNumLabel = new Label(10, 10, 100, 10);
	private Label fpsLabel = new Label(10, 10, 100, 10);

	Component[] order = { viewLabel, arViewButton, processedViewButton, mapViewButton, allViewButton, frameNumLabel,
			fpsLabel };

	public GUIComponents() {

	}

	public void initGUI() {
		if (!GLFW.glfwInit()) {
			throw new RuntimeException("Can't initialize GLFW");
		}
		window = createWindow();
		leguiframe = createFrameWithGUI();
		initializer = new DefaultInitializer(window, leguiframe);

		initializeGuiWithCallbacks();
		running = true;
	}

	private long createWindow() {
		long window = glfwCreateWindow(Parameters.screenWidth, Parameters.screenHeight, "AR System 04", NULL, NULL);
		glfwShowWindow(window);

		glfwMakeContextCurrent(window);
		GL.createCapabilities();
		glfwSwapInterval(0);
		return window;
	}

	// initialize the main widget that contains diagnostic data and options
	private Frame createFrameWithGUI() {
		Frame frame = new Frame(Parameters.screenWidth, Parameters.screenHeight);

		for (int i = 0; i < this.order.length; i++) {
			this.order[i].setPosition(10, 20 * i + 10);
		}

		this.viewLabel.getTextState().setText("View: ");
		this.arViewButton.getTextState().setText("AR View");
		this.processedViewButton.getTextState().setText("Processed View");
		this.mapViewButton.getTextState().setText("Map View");
		this.allViewButton.getTextState().setText("All Views");
		this.frameNumLabel.getTextState().setText("Frame #: --");
		this.fpsLabel.getTextState().setText("Framerate: --");

		this.arViewButton.setRadioButtonGroup(this.viewButtonGroup);
		this.processedViewButton.setRadioButtonGroup(this.viewButtonGroup);
		this.mapViewButton.setRadioButtonGroup(this.viewButtonGroup);
		this.allViewButton.setRadioButtonGroup(this.viewButtonGroup);

		this.arViewButton.setChecked(this.view == AR_VIEW);
		this.processedViewButton.setChecked(this.view == PROCESSED_VIEW);
		this.mapViewButton.setChecked(this.view == MAP_VIEW);
		this.allViewButton.setChecked(this.view == ALL_VIEW);

		// handlers for radio buttons
		this.arViewButton.getListenerMap().addListener(MouseClickEvent.class, (MouseClickEventListener) event -> {
			this.updateView();
		});
		this.processedViewButton.getListenerMap().addListener(MouseClickEvent.class,
				(MouseClickEventListener) event -> {
					this.updateView();
				});
		this.mapViewButton.getListenerMap().addListener(MouseClickEvent.class, (MouseClickEventListener) event -> {
			this.updateView();
		});
		this.allViewButton.getListenerMap().addListener(MouseClickEvent.class, (MouseClickEventListener) event -> {
			this.updateView();
		});

		// Set background color for frame
		frame.getContainer().getStyle().getBackground().setColor(ColorConstants.transparent());
		frame.getContainer().setFocusable(false);

		this.mainWidget.getTitleTextState().setText("Data & Options");
		this.mainWidget.setCloseable(false);

		frame.getContainer().add(this.mainWidget);
		this.mainWidget.getContainer().add(this.viewLabel);
		this.mainWidget.getContainer().add(this.arViewButton);
		this.mainWidget.getContainer().add(this.processedViewButton);
		this.mainWidget.getContainer().add(this.mapViewButton);
		this.mainWidget.getContainer().add(this.allViewButton);
		this.mainWidget.getContainer().add(this.fpsLabel);
		this.mainWidget.getContainer().add(this.frameNumLabel);
		return frame;
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

	public void renderGUI() {

		Context context = initializer.getContext();
		org.liquidengine.legui.system.renderer.Renderer ren = initializer.getRenderer();

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

	public void destroy() {
		initializer.getRenderer().destroy();
		glfwDestroyWindow(window);
	}

	public void updateView() {
		RadioButton selection = this.viewButtonGroup.getSelection();
		if (selection == this.arViewButton) {
			this.view = AR_VIEW;
		} else if (selection == this.processedViewButton) {
			this.view = PROCESSED_VIEW;
		}
		if (selection == this.mapViewButton) {
			this.view = MAP_VIEW;
		}
		if (selection == this.allViewButton) {
			this.view = ALL_VIEW;
		}
	}

	public void updateFrameNumLabel(double frameNum) {
		this.frameNumLabel.getTextState().setText("Frame #:        " + (long) frameNum);
	}

	public void updateFpsLabel(double fps) {
		this.fpsLabel.getTextState().setText("Framerate:        " + fps + " fps");
	}

	public long getWindow() {
		return window;
	}

	public void setWindow(long window) {
		this.window = window;
	}

	public Frame getLeguiframe() {
		return leguiframe;
	}

	public void setLeguiframe(Frame leguiframe) {
		this.leguiframe = leguiframe;
	}

	public DefaultInitializer getInitializer() {
		return initializer;
	}

	public void setInitializer(DefaultInitializer initializer) {
		this.initializer = initializer;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public boolean isHiding() {
		return hiding;
	}

	public void setHiding(boolean hiding) {
		this.hiding = hiding;
	}

	public Widget getMainWidget() {
		return mainWidget;
	}

	public void setMainWidget(Widget mainWidget) {
		this.mainWidget = mainWidget;
	}

	public Label getFpsLabel() {
		return fpsLabel;
	}

	public void setFpsLabel(Label fpsLabel) {
		this.fpsLabel = fpsLabel;
	}

	public int getView() {
		return this.view;
	}

}
