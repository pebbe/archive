package nl.xs4all.pebbe.vrfracland;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;

import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import javax.microedition.khronos.egl.EGLConfig;

public class MainActivity extends GvrActivity implements GvrView.StereoRenderer {

    private FracLand fracland;
    private Wereld wereld;
    private Info info;
    private Arrows arrows;
    private int[] texturenames;

    private static final float afstand = 3.0f;

    protected float[] modelFracLand;
    protected float[] modelWorld;
    private float[] camera;
    private float[] view;
    private float[] modelViewProjection;
    private float[] modelView;
    private float[] forward;
    private boolean started;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeGvrView();

        // Initialize other objects here.
        modelFracLand = new float[16];
        modelWorld = new float[16];
        camera = new float[16];
        view = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];
        forward = new float[3];
    }

    @Override
    public void onSurfaceCreated(EGLConfig config) {
        Matrix.setLookAtM(camera, 0,
                0.0f, 0.0f, 0.01f,  // 0.01f
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f);

        // Generate Textures, if more needed, alter these numbers.
        texturenames = new int[2];
        GLES20.glGenTextures(2, texturenames, 0);

        started = false;
        fracland = new FracLand();
        info = new Info(this, texturenames[0]);
        arrows = new Arrows();
        wereld = new Wereld(this, texturenames[1]);
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        Matrix.setIdentityM(modelFracLand, 0);
        Matrix.setIdentityM(modelWorld, 0);

        headTransform.getForwardVector(forward, 0);
        Matrix.translateM(modelFracLand, 0, afstand * forward[0], afstand * forward[1], afstand * forward[2]);

    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);

        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
        float[] perspective = eye.getPerspective(0.1f, 100.0f);

        Matrix.multiplyMM(modelView, 0, view, 0, modelWorld, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);

        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        wereld.draw(modelViewProjection);

        if (started) {
            // enable face culling feature
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            GLES20.glCullFace(GLES20.GL_BACK);

            // nodig als objecten niet convex zijn
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glDepthFunc(GLES20.GL_LEQUAL);

            Matrix.multiplyMM(modelView, 0, view, 0, modelFracLand, 0);
            Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
            fracland.draw(modelViewProjection);
        } else {
            info.draw(modelViewProjection);
            arrows.draw(modelViewProjection);
        }

    }

    @Override
    public void onRendererShutdown() {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    public void initializeGvrView() {
        setContentView(R.layout.common_ui);

        GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

        gvrView.setRenderer(this);
        //gvrView.setTransitionViewEnabled(true);

        // Enable Cardboard-trigger feedback with Daydream headsets. This is a simple way of supporting
        // Daydream controller input for basic interactions using the existing Cardboard trigger API.
        gvrView.enableCardboardTriggerEmulation();

        /*
        if (gvrView.setAsyncReprojectionEnabled(true)) {
            // Async reprojection decouples the app framerate from the display framerate,
            // allowing immersive interaction even at the throttled clockrates set by
            // sustained performance mode.
            AndroidCompat.setSustainedPerformanceMode(this, true);
        }
        */

        setGvrView(gvrView);
    }

    @Override
    public void onCardboardTrigger() {
        fracland.init(System.currentTimeMillis());
        started = true;
    }

    private void log(String s, float[] m) {
        /*
        Log.i("MYTAG", s + " [ [ " + m[0] + " " + m[4] + " " + m[8] + " " + m[12] + " ]");
        Log.i("MYTAG", s + "   [ " + m[1] + " " + m[5] + " " + m[9] + " " + m[13] + " ]");
        Log.i("MYTAG", s + "   [ " + m[2] + " " + m[6] + " " + m[10] + " " + m[14] + " ]");
        Log.i("MYTAG", s + "   [ " + m[3] + " " + m[7] + " " + m[11] + " " + m[15] + " ] ]");
        */
    }

}
