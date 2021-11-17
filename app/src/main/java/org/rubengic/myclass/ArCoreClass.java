package org.rubengic.myclass;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.PlaybackStatus;
import com.google.ar.core.Point;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.RecordingConfig;
import com.google.ar.core.RecordingStatus;
import com.google.ar.core.Session;
import com.google.ar.core.Track;
import com.google.ar.core.TrackData;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.PlaybackFailedException;
import com.google.ar.core.exceptions.RecordingFailedException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import org.rubengic.myclass.helpers.DisplayRotationHelper;
import org.rubengic.myclass.helpers.FullScreenHelper;
import org.rubengic.myclass.helpers.SnackbarHelper;
import org.rubengic.myclass.helpers.TapHelper;
import org.rubengic.myclass.helpers.TrackingStateHelper;
import org.rubengic.myclass.rendering.BackgroundRenderer;
import org.rubengic.myclass.rendering.ObjectRenderer;
import org.rubengic.myclass.rendering.PlaneRenderer;
import org.rubengic.myclass.rendering.PointCloudRenderer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ArCoreClass extends AppCompatActivity implements GLSurfaceView.Renderer {

    // Application states.
    private enum AppState {
        IDLE,
        RECORDING,
        PLAYBACK
    }
    //esto nos sirve por si nos da un error saber que es de esta clase
    private static final  String TAG = ArCoreClass.class.getSimpleName();

    // Keys to keep track of the active dataset and playback state between restarts.
    //Claves para realizar un seguimiento del conjunto de datos activo
    private static final String DESIRED_APP_STATE_KEY = "desired_app_state_key";
    private static final int PERMISSIONS_REQUEST_CODE = 0;

    // Recording and playback requires android.permission.WRITE_EXTERNAL_STORAGE and
    // android.permission.CAMERA to operate. These permissions must be mirrored in the manifest.
    //para pedir permisos al usuario
    private static final List<String> requiredPermissions =
            Arrays.asList(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE);

    // Randomly generated UUID and custom MIME type to mark the anchor track for this sample.
    private static final UUID ANCHOR_TRACK_ID = UUID.fromString("a65e59fc-2e13-4607-b514-35302121c138");

    // The app state so that it can be preserved when the activity restarts. This is also used to
    // update the UI.
    private final AtomicReference<AppState> currentState = new AtomicReference<>(AppState.IDLE);

    private Session session;
    //esto nos puede servir para saber el estado actual del arcore
    //es un snackbar personalizado
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();

    //Detecta la superficie de la imagen que recibe a traves de la camara
    private DisplayRotationHelper displayRotationHelper;
    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);


    //para detectar las pulsaciones tactiles
    private TapHelper tapHelper;

    private GLSurfaceView surfaceView;

    // The Renderers are created here, and initialized when the GL surface is created.
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final ObjectRenderer virtualObject = new ObjectRenderer();
    private final ObjectRenderer virtualObjectShadow = new ObjectRenderer();
    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    //la matriz de localización en la imagen
    private final float[] anchorMatrix = new float[16];
    private static final float[] DEFAULT_COLOR = new float[] {0f, 0f, 0f, 0f};

    private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";

    // Anchors created from taps used for object placing with a given color.
    private static class ColoredAnchor {
        public final Anchor anchor;
        public final float[] color;

        public ColoredAnchor(Anchor a, float[] color4f) {
            this.anchor = a;
            this.color = color4f;
        }
    }

    //usando la camara
    private final ArrayList<ColoredAnchor> anchors = new ArrayList<>();
    //usando un video
    private final ArrayList<ColoredAnchor> anchorsToBeRecorded = new ArrayList<>();

    private boolean installRequested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadInternalStateFromIntentExtras();

        //cargo el entorno grafico
        setContentView(R.layout.activity_ar_core_class);

        //cargo los textView
        TextView tv_asig = (TextView) findViewById(R.id.tv_asig);
        TextView tv_aula = (TextView) findViewById(R.id.tv_aula);

        //recibo la info de principal activity
        String asig = getIntent().getExtras().getString("asignatura");
        String aula = getIntent().getExtras().getString("aula");

        if(asig != null) {
            
            //muestro el contenido en los titulos
            tv_asig.setText(asig);
            tv_aula.setText("Aula: " + aula);
        }

        //accedo al surfaceView del entorno grafico
        surfaceView = findViewById(R.id.surfaceview);

        displayRotationHelper = new DisplayRotationHelper(this);

        //Configuración del touch listener
        tapHelper = new TapHelper(this);//creo el evento
        surfaceView.setOnTouchListener(tapHelper);//escucha las pulsaciones

        //configuración del render
        //activo las pulsaciones
        surfaceView.setPreserveEGLContextOnPause(true);
        //indico la version a usar del cliente (no lo se porque esta version)
        surfaceView.setEGLContextClientVersion(2);
        //supongo que este es el tamaño por defecto del objeto virtual (no tengo muy claro lo que es)
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        surfaceView.setRenderer(this);
        //Cargo el modelo a usar en la renderización
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        //indico que pueda pintar en la pantalla
        surfaceView.setWillNotDraw(false);

        //indico inicialmente que no tengo instalado nada requerido
        installRequested = false;

        //función que se encarga de la funcionalidad de la realidad aumentada
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(session == null){
            Exception exception = null;
            String message = null;

            try {
                //para saber si instalar o no componentes del ArCoreAPK
                switch (
                        ArCoreApk.getInstance().requestInstall(this, !installRequested)
                ) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }
                /**
                 * If we did not yet obtain runtime permission on Android M and above,
                 * now is a good time to ask the user for it.
                  */
                //para los permisos necesarios
                if(requestPermissions()){
                    return;
                }

                //inicializo la sesión
                session = new Session(this);

                //excepciones
            }catch (UnavailableArcoreNotInstalledException | UnavailableUserDeclinedInstallationException e){
                message = "Por favor installa el Google Play Services para AR (ARCore)";
                exception = e;
            }catch(UnavailableApkTooOldException e){
                message = "Por favor actualiza Google Play Services para AR (ARCore)";
                exception = e;
            }catch(UnavailableSdkTooOldException e){
                message = "Por favor actualiza la aplicación";
                exception = e;
            }catch(UnavailableDeviceNotCompatibleException e){
                message = "Este dispositivo no es compatible con AR (ARCore)";
                exception = e;
            }catch(Exception e){
                message = "Error al crear la sesión AR";
                exception = e;
            }

            //si tiene un mensaje ha ocurrido un error
            if(message != null){
                messageSnackbarHelper.showError(this, message+" ::: "+exception);
                Log.e(TAG, "Excepcion creando la sesion", exception);
                return;
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        //si la camara no esta disponible o activada
        try {
            // Playback will now start if an MP4 dataset has been set.
            session.resume();
        } catch (CameraNotAvailableException e) {
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
            session = null;
            return;
        }

        if (currentState.get() == AppState.PLAYBACK) {
            // Must be called after dataset playback is started by call to session.resume().
            checkPlaybackStatus();
        }
        surfaceView.onResume();
        displayRotationHelper.onResume();
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(session != null){
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();

            session.pause();
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //indico los colores a usar
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        //preparo el objeto de renderización.
        try {
            backgroundRenderer.createOnGlThread(this);
            planeRenderer.createOnGlThread(this, "models/trigrid.png");
            pointCloudRenderer.createOnGlThread(this);

            //virtualObject.createOnGlThread(this, "models/andy.obj", "models/andy.png");
            virtualObject.createOnGlThread(this, "models/pawn.obj", "models/pawn_albedo.png");
            //indico los parametros graficos (difusion, ambiente, specular, ...)
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

            /*virtualObjectShadow.createOnGlThread(
                    this,
                    "models/andy_shadow.obj",
                    "models/andy_shadow.png"
            );*/

            virtualObjectShadow.createOnGlThread(
                    this,
                    "models/pawn.obj",
                    "models/pawn_albedo.png"
            );

            virtualObjectShadow.setBlendMode(ObjectRenderer.BlendMode.Shadow);
            virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

        }catch (IOException e){
            Log.e(TAG, "Error al leer los archivos de assert", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        //clear screen to tell driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        //don't render anything or call session methods until session is created
        if(session == null){
            return;
        }

        /*
        Notify ARCore session that the view size changed so that the projection matrix and the video
        backgroud can be properly adjusted.
         */
        displayRotationHelper.updateSessionIfNeeded(session);

        try{
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            Frame frame = session.update();
            Camera camera = frame.getCamera();

            //Handle one tap per frame
            ColoredAnchor anchor = handleTap(frame, camera);

            if(anchor != null){
                //if we created an anchor, then try to record it.
                anchorsToBeRecorded.add(anchor);
            }

            //if frame is ready, render camera preview image to the GL surface.
            backgroundRenderer.draw(frame);

            //keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

            //If not tracking, don't draw 3D objects, show tracking failure reason instead.
            if(camera.getTrackingState() == TrackingState.PAUSED){
                messageSnackbarHelper.showMessage(
                        this,
                        TrackingStateHelper.getTrackingFailureReasonString(camera)
                );
                return;
            }

            //Get projection matrix.
            //obtiene el punto de proyeccion de la camara
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            //Get camera matrix and draw.
            //obtengo la matriz de la camara, para saber donde se posiciona los objetos virtuales
            float[] viewmtx = new float[16];
            camera.getViewMatrix(viewmtx, 0);

            /**
             * Compute lighting from average intensity of the image.
             * The first three components are color scaling factors.
             * The last one is the average pixel intensity in gamma space.
             */
            final float[] colorCorrectionRgba = new float[4];
            frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

            /**
             * visualize tracked points.
             * Use try-with-resources to automatically release the point cloud.
             */
            try(PointCloud pointCloud = frame.acquirePointCloud()){
                pointCloudRenderer.update(pointCloud);
                pointCloudRenderer.draw(viewmtx, projmtx);
            }

            /**
             * No tracking failure at this point. If we detected any planes, then hide the message
             * UI. If not plane detected, show searching planes message.
             */
            if(hasTrackingPlane()){
                messageSnackbarHelper.hide(this);
            }else{
                messageSnackbarHelper.showMessage(this, SEARCHING_PLANE_MESSAGE);
            }

            //Visualize detected planes.
            planeRenderer.drawPlanes(
                    session.getAllTrackables(Plane.class),
                    camera.getDisplayOrientedPose(),
                    projmtx
            );

            //Visualize anchors created by tapping.
            float scaleFactor = 1.0f;
            for(ColoredAnchor coloredAnchor : anchors){
                if(coloredAnchor.anchor.getTrackingState() != TrackingState.TRACKING){
                    continue;
                }

                /**
                 * Get the current pose of an Anchor in world space. The Anchor pose is updated
                 * during calls to session.update() as ARCore refines its estimate of the world.
                 */
                coloredAnchor.anchor.getPose().toMatrix(anchorMatrix, 0);

                //Update and draw the model and its shadow.
                //actualiza la escala de factores de la escena (ambiente, difusión, ...)
                virtualObject.updateModelMatrix(anchorMatrix, scaleFactor);
                virtualObjectShadow.updateModelMatrix(anchorMatrix, scaleFactor);
                //actualiza el punto de proyección de la camara y la matriz de la camara de lo que captura (la escena)
                virtualObject.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
                virtualObjectShadow.draw(viewmtx, projmtx, colorCorrectionRgba, coloredAnchor.color);
            }
        } catch (Throwable t){
            //Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    /*
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
    */

    /** Checks if we detected at least one plane.
     * Detecta si la camara se mueve o no
     * */
    private boolean hasTrackingPlane() {
        for (Plane plane : session.getAllTrackables(Plane.class)) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                return true;
            }
        }
        return false;
    }

    //calcula la posición donde se coloca el objeto
    /** Try to create an anchor if the user has tapped the screen. */
    private ColoredAnchor handleTap(Frame frame, Camera camera) {
        // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
        MotionEvent tap = tapHelper.poll();
        if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
            for (HitResult hit : frame.hitTest(tap)) {
                // Check if any plane was hit, and if it was hit inside the plane polygon.
                Trackable trackable = hit.getTrackable();
                // Creates an anchor if a plane or an oriented point was hit.
                //creamos el anchor donde contendra la información de los sensores
                if ((trackable instanceof Plane
                        && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                        && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
                        || (trackable instanceof Point
                        && ((Point) trackable).getOrientationMode()
                        == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)) {
                    // Hits are sorted by depth. Consider only closest hit on a plane or oriented point.
                    // Cap the number of objects created. This avoids overloading both the
                    // rendering system and ARCore.
                    if (anchors.size() >= 20) {
                        anchors.get(0).anchor.detach();
                        anchors.remove(0);
                    }

                    // Assign a color to the object for rendering based on the trackable type
                    // this anchor attached to.
                    float[] objColor;
                    if (trackable instanceof Point) {
                        objColor = new float[] {66.0f, 133.0f, 244.0f, 255.0f}; // Blue.
                    } else if (trackable instanceof Plane) {
                        objColor = new float[] {139.0f, 195.0f, 74.0f, 255.0f}; // Green.
                    } else {
                        objColor = DEFAULT_COLOR;
                    }

                    ColoredAnchor anchor = new ColoredAnchor(hit.createAnchor(), objColor);
                    // Adding an Anchor tells ARCore that it should track this position in
                    // space. This anchor is created on the Plane to place the 3D model
                    // in the correct position relative both to the world and to the plane.
                    anchors.add(anchor);
                    return anchor;
                }
            }
        }
        return null;
    }

    //esto es por si no le ha dado permisos o ha negado los permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);

        if(requestCode == PERMISSIONS_REQUEST_CODE){
            for (int i = 0; i<results.length; ++i){
                if(results[i] != PackageManager.PERMISSION_GRANTED) {
                    logAndShowErrorMessage("No puede iniciar la app, necesita permisos: " + permissions[i]);
                    finish();
                }
            }
        }
    }

    //PARA USAR LA PANTALLA COMPLETA DEL MOVIL
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    /** Checks the playback is in progress without issues. */
    private void checkPlaybackStatus() {
        if ((session.getPlaybackStatus() != PlaybackStatus.OK)
                && (session.getPlaybackStatus() != PlaybackStatus.FINISHED)) {
            logAndShowErrorMessage(
                    "Failed to start playback, playback status is: " + session.getPlaybackStatus());
            setStateAndUpdateUI(AppState.IDLE);
        }
    }

    /**
     * Requests any not (yet) granted required permissions needed for recording and playback.
     *
     * <p>Returns false if all permissions are already granted. Otherwise, requests missing
     * permissions and returns true.
     */
    private boolean requestPermissions() {
        List<String> permissionsNotGranted = new ArrayList<>();
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNotGranted.add(permission);
            }
        }
        if (permissionsNotGranted.isEmpty()) {
            return false;
        }
        ActivityCompat.requestPermissions(
                this, permissionsNotGranted.toArray(new String[0]), PERMISSIONS_REQUEST_CODE);
        return true;
    }

    /** Helper function to set state and update UI. */
    //cambia el estado y actualiza
    private void setStateAndUpdateUI(AppState state) {
        currentState.set(state);
        updateUI();
    }

    /** Helper function to log error message and show it on the screen. */
    private void logAndShowErrorMessage(String errorMessage) {
        Log.e(TAG, errorMessage);
        messageSnackbarHelper.showError(this, errorMessage);
    }

    /*
    función UI que actualiza los comportamientos de la interfaz de usuario según el estado actual
    de la aplicación.
     */
    private void updateUI(){}

    /** Loads desired state from intent extras, if available. */
    //carga el estado en el que se encuentra inicialmente
    private void loadInternalStateFromIntentExtras() {
        if (getIntent() == null || getIntent().getExtras() == null) {
            return;
        }
        Bundle bundle = getIntent().getExtras();

        if (bundle.containsKey(DESIRED_APP_STATE_KEY)) {
            String state = getIntent().getStringExtra(DESIRED_APP_STATE_KEY);
            if (state != null) {
                switch (state) {
                    case "IDLE":
                        currentState.set(AppState.IDLE);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}