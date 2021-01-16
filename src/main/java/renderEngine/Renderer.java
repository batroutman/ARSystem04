package renderEngine;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import entities.Camera;
import entities.Entity;
import models.RawModel;
import models.TexturedModel;
import runtimevars.CameraIntrinsics;
import runtimevars.Parameters;
import shaders.StaticShader;
import toolbox.Maths;
import types.Correspondence2D2D;
import types.Feature;

public class Renderer {

	private static final float FOV = 70;
	private static final float NEAR_PLANE = 0.01f;
	private static final float FAR_PLANE = 50000;

	private Matrix4f projectionMatrix;

	public Renderer(StaticShader shader) {
		createProjectionMatrix();
		shader.start();
		shader.loadProjectionMatrix(projectionMatrix);
		shader.stop();
	}

	public void prepare() {

		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glClearColor(0, 0.0f, 0.0f, 1);

	}

	public void render(Camera camera, ArrayList<Entity> entities, StaticShader cameraShader, Entity background,
			StaticShader bgShader) {

		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_NEVER);
		bgShader.start();
		TexturedModel bgModel = background.getModel();
		RawModel bgRawModel = bgModel.getRawModel();
		GL30.glBindVertexArray(bgRawModel.getVaoID());
		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, bgModel.getTexture().getID());
		GL11.glDrawElements(GL11.GL_TRIANGLES, bgRawModel.getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL30.glBindVertexArray(0);
		bgShader.stop();

		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		cameraShader.start();
		cameraShader.loadViewMatrix(camera);
		for (Entity entity : entities) {
			TexturedModel model = entity.getModel();
			RawModel rawModel = model.getRawModel();
			GL30.glBindVertexArray(rawModel.getVaoID());
			GL20.glEnableVertexAttribArray(0);
			GL20.glEnableVertexAttribArray(1);
			Matrix4f transformationMatrix = Maths.createTransformationMatrix(entity.getPosition(), entity.getRotX(),
					entity.getRotY(), entity.getRotZ(), entity.getScale());
			cameraShader.loadTransformationMatrix(transformationMatrix);
			GL13.glActiveTexture(GL13.GL_TEXTURE0);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, model.getTexture().getID());
			GL11.glDrawElements(GL11.GL_TRIANGLES, rawModel.getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
			GL20.glDisableVertexAttribArray(0);
			GL20.glDisableVertexAttribArray(1);
			GL30.glBindVertexArray(0);
		}
		cameraShader.stop();
	}

	public void renderProcessedView(Entity background, StaticShader bgShader, List<Correspondence2D2D> correspondences,
			List<Feature> features) {

		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_NEVER);
		bgShader.start();
		TexturedModel bgModel = background.getModel();
		RawModel bgRawModel = bgModel.getRawModel();
		GL30.glBindVertexArray(bgRawModel.getVaoID());
		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, bgModel.getTexture().getID());
		GL11.glDrawElements(GL11.GL_TRIANGLES, bgRawModel.getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL30.glBindVertexArray(0);
		bgShader.stop();
		this.renderFeatures(features);
		this.renderCorrespondences(correspondences);

	}

	public void renderCorrespondences(List<Correspondence2D2D> correspondences) {
		GL11.glColor3f(0, 1, 0);
		GL11.glLineWidth(2.0f);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0, Parameters.width, Parameters.height, 0, 0, 100);
		for (Correspondence2D2D corr : correspondences) {
			GL11.glBegin(GL11.GL_LINES);
			GL11.glVertex2d(corr.getX0(), corr.getY0());
			GL11.glVertex2d(corr.getX1(), corr.getY1());
			GL11.glEnd();
		}
	}

	public void renderFeatures(List<Feature> features) {
		GL11.glColor3f(0, 0, 1);
		GL11.glLineWidth(2.0f);
		GL11.glMatrixMode(GL11.GL_PROJECTION);
		GL11.glLoadIdentity();
		GL11.glOrtho(0, Parameters.width, Parameters.height, 0, 0, 100);
		for (Feature feature : features) {
			double topLeftX = feature.getX() - (feature.getPatchSize() / 2) - 1;
			double topLeftY = feature.getY() - (feature.getPatchSize() / 2) - 1;
			double patchSize = feature.getPatchSize() + 2;
			GL11.glBegin(GL11.GL_LINES);
			GL11.glVertex2d(topLeftX, topLeftY);
			GL11.glVertex2d(topLeftX + patchSize, topLeftY);
			GL11.glVertex2d(topLeftX + patchSize, topLeftY);
			GL11.glVertex2d(topLeftX + patchSize, topLeftY + patchSize);
			GL11.glVertex2d(topLeftX + patchSize, topLeftY + patchSize);
			GL11.glVertex2d(topLeftX, topLeftY + patchSize);
			GL11.glVertex2d(topLeftX, topLeftY + patchSize);
			GL11.glVertex2d(topLeftX, topLeftY);
			GL11.glEnd();
		}
	}

	private void createProjectionMatrix() {
		// float aspectRatio = (float) Display.getWidth() / (float) Display.getHeight();
		// float y_scale = (float) ((1f / Math.tan(Math.toRadians(FOV / 2f))) *
		// aspectRatio);
		// float y_scale = (float) ((1f / (Display.getWidth() / (2 *
		// CameraIntrinsics.fx))) * aspectRatio);
		// float x_scale = y_scale / aspectRatio;
		float frustum_length = FAR_PLANE - NEAR_PLANE;

		projectionMatrix = new Matrix4f();
		float width = CameraIntrinsics.cx * 2;
		float height = CameraIntrinsics.cy * 2;
		float x0 = 0;
		float y0 = 0;
		projectionMatrix.m00(2 * CameraIntrinsics.fx / width);
		projectionMatrix.m10(-2 * CameraIntrinsics.s / width);
		projectionMatrix.m20((width - 2 * CameraIntrinsics.cx + 2 * x0) / width);
		projectionMatrix.m11(2 * CameraIntrinsics.fy / height);
		projectionMatrix.m21((-height + 2 * CameraIntrinsics.cy + 2 * y0) / height);
		projectionMatrix.m22((-FAR_PLANE - NEAR_PLANE) / (FAR_PLANE - NEAR_PLANE));
		projectionMatrix.m32(-2 * FAR_PLANE * NEAR_PLANE / frustum_length);
		projectionMatrix.m23(-1);
		projectionMatrix.m33(0);

	}

}
