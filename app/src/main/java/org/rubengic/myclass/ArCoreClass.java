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

    // MP4 dataset naming convention: arcore-dataset-video.mp4
    //esto es por si no funciona la camara hacemos un video y lo pintamos
    private static final String MP4_DATASET_FILENAME_TEMPLATE = "arcore-dataset-video.mp4";

    // Keys to keep track of the active dataset and playback state between restarts.
    //Claves para realizar un seguimiento del conjunto de datos activo y el estado de reproducción.
    private static final String DESIRED_DATASET_PATH_KEY = "desired_dataset_path_key";
    private static final String DESIRED_APP_STATE_KEY = "desired_app_state_key";
    private static final int PERMISSIONS_REQUEST_CODE = 0;

    // Recording and playback requires android.permission.WRITE_EXTERNAL_STORAGE and
    // android.permission.CAMERA to operate. These permissions must be mirrored in the manifest.
    //para pedir permisos al usuario
    private static final List<String> requiredPermissions =
            Arrays.asList(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE);

    // Randomly generated UUID and custom MIME type to mark the anchor track for this sample.
    private static final UUID ANCHOR_TRACK_ID = UUID.fromString("a65e59fc-2e13-4607-b514-35302121c138");
    private static final String ANCHOR_TRACK_MIME_TYPE = "application/arcore-class";

    // The app state so that it can be preserved when the activity restarts. This is also used to
    // update the UI.
    private final AtomicReference<AppState> currentState = new AtomicReference<>(AppState.IDLE);

    //¿puede que nos haga falta?
    private String playbackDatasetPath;
    private String lastRecordingDatasetPath;

    private Session session;
    //esto nos puede servir para saber el estado actual del arcore
    //es un snackbar personalizado
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();

    //ni idea si es necesari, si son necesarios habra que crear sus respectivas clases
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
        /*
        TextView t = (TextView) findViewById(R.id.panel_info);
        t.setText(TAG);
         */
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

                //si va a usar un video
                if(currentState.get() == AppState.PLAYBACK){
                    // Dataset playback will start when session.resume() is called.
                    setPlaybackDatasetPath();//accede al archivo mp4 y lo carga
                }
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

            //esto es cuando para de grabar
            if(currentState.get() == AppState.RECORDING){
                stopRecording();
            }

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

            //try to record any anchors that have not been recorded yet.
            recordAnchors(session, frame, camera);

            //if we are playing back, then add any recorded anchors to the session.
            addRecordedAnchors(session, frame, camera);

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
            float[] projmtx = new float[16];
            camera.getProjectionMatrix(projmtx, 0, 0.1f, 100.0f);

            //Get camera matrix and draw.
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
                virtualObject.updateModelMatrix(anchorMatrix, scaleFactor);
                virtualObjectShadow.updateModelMatrix(anchorMatrix, scaleFactor);
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

    /**
     * Restarts current activity to enter or exit playback mode.
     *
     * <p>This method simulates an app with separate activities for recording and playback by
     * restarting the current activity and passing in the desired app state via an intent with extras.
     */
    private void restartActivityWithIntentExtras() {
        Intent intent = this.getIntent();
        Bundle bundle = new Bundle();
        bundle.putString(DESIRED_APP_STATE_KEY, currentState.get().name());
        bundle.putString(DESIRED_DATASET_PATH_KEY, playbackDatasetPath);
        intent.putExtras(bundle);
        this.finish();
        this.startActivity(intent);
    }

    /** Performs action when playback button is clicked. */
    private void startPlayback() {
        if (playbackDatasetPath == null) {
            return;
        }
        currentState.set(AppState.PLAYBACK);
        restartActivityWithIntentExtras();
    }

    /** Performs action when close_playback button is clicked. */
    private void stopPlayback() {
        currentState.set(AppState.IDLE);
        restartActivityWithIntentExtras();
    }

    /** Performs action when start_recording button is clicked. */
    private void startRecording() {
        try {
            lastRecordingDatasetPath = getNewDatasetPath();
            if (lastRecordingDatasetPath == null) {
                logAndShowErrorMessage("Failed to generate a MP4 dataset path for recording.");
                return;
            }

            Track anchorTrack =
                    new Track(session).setId(ANCHOR_TRACK_ID).setMimeType(ANCHOR_TRACK_MIME_TYPE);

            session.startRecording(
                    new RecordingConfig(session)
                            .setMp4DatasetUri(Uri.fromFile(new File(lastRecordingDatasetPath)))
                            .setAutoStopOnPause(false)
                            .addTrack(anchorTrack));
        } catch (RecordingFailedException e) {
            String errorMessage = "Failed to start recording. " + e;
            Log.e(TAG, errorMessage, e);
            messageSnackbarHelper.showError(this, errorMessage);
            return;
        }
        if (session.getRecordingStatus() != RecordingStatus.OK) {
            logAndShowErrorMessage(
                    "Failed to start recording, recording status is " + session.getRecordingStatus());
            return;
        }
        setStateAndUpdateUI(AppState.RECORDING);
    }

    /** Generates a new MP4 dataset filename based on the current system time. */
    private static String getNewMp4DatasetFilename() {
        return String.format(
                Locale.ENGLISH,
                MP4_DATASET_FILENAME_TEMPLATE);
    }

    /** Generates a new MP4 dataset path based on the current system time. */
    private String getNewDatasetPath() {
        File baseDir = this.getExternalFilesDir(null);
        if (baseDir == null) {
            return null;
        }
        return new File(this.getExternalFilesDir(null), getNewMp4DatasetFilename()).getAbsolutePath();
    }

    /** Checks if we detected at least one plane. */
    private boolean hasTrackingPlane() {
        for (Plane plane : session.getAllTrackables(Plane.class)) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                return true;
            }
        }
        return false;
    }

    /** During playback, recreate any anchors that were placed during recording. */
    private void addRecordedAnchors(Session session, Frame frame, Camera camera) {
        for (TrackData data : frame.getUpdatedTrackData(ANCHOR_TRACK_ID)) {
            ByteBuffer payload = data.getData();

            float[] translation = new float[3];
            float[] quaternion = new float[4];
            float[] color = new float[4];

            FloatBuffer floatView = payload.asFloatBuffer();
            floatView.get(translation);
            floatView.get(quaternion);
            floatView.get(color);

            // Transform the recorded anchor pose in the camera coordinate frame back into world
            // coordinates.
            Pose pose = camera.getPose().compose(new Pose(translation, quaternion));
            ColoredAnchor anchor = new ColoredAnchor(session.createAnchor(pose), color);
            anchors.add(anchor);
        }
    }

    /**
     * Try to add anchors to an MP4 data track track if the app is currently recording.
     *
     * <p>Track data recording can sometimes fail due an image not being available for recording in
     * ARCore, so we try to record all anchors that have not been recorded yet.
     */
    private void recordAnchors(Session session, Frame frame, Camera camera) {
        if (!session.getRecordingStatus().equals(RecordingStatus.OK)) {
            // We do not record anchors created before we started recording.
            anchorsToBeRecorded.clear();
            return;
        }

        Iterator<ColoredAnchor> anchorIterator = anchorsToBeRecorded.iterator();
        while (anchorIterator.hasNext()) {
            ColoredAnchor anchor = anchorIterator.next();
            // Transform the anchor pose world coordinates in to camera coordinate frame for easy
            // placement during playback.
            Pose pose = camera.getPose().inverse().compose(anchor.anchor.getPose());
            float[] translation = pose.getTranslation();
            float[] quaternion = pose.getRotationQuaternion();
            ByteBuffer payload =
                    ByteBuffer.allocate(4 * (translation.length + quaternion.length + anchor.color.length));
            FloatBuffer floatView = payload.asFloatBuffer();
            floatView.put(translation);
            floatView.put(quaternion);
            floatView.put(anchor.color);

            try {
                frame.recordTrackData(ANCHOR_TRACK_ID, payload);
                anchorIterator.remove();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Could not record anchor into external data track.", e);
                return;
            }
        }
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

    /** Performs action when stop_recording button is clicked. */
    private void stopRecording() {
        try {
            session.stopRecording();
        } catch (RecordingFailedException e) {
            String errorMessage = "Failed to stop recording. " + e;
            Log.e(TAG, errorMessage, e);
            messageSnackbarHelper.showError(this, errorMessage);
            return;
        }
        if (session.getRecordingStatus() == RecordingStatus.OK) {
            logAndShowErrorMessage(
                    "Failed to stop recording, recording status is " + session.getRecordingStatus());
            return;
        }
        if (new File(lastRecordingDatasetPath).exists()) {
            playbackDatasetPath = lastRecordingDatasetPath;
            Log.d(TAG, "MP4 dataset has been saved at: " + playbackDatasetPath);
        } else {
            logAndShowErrorMessage(
                    "Recording failed. File " + lastRecordingDatasetPath + " wasn't created.");
        }
        setStateAndUpdateUI(AppState.IDLE);
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

    /** Sets the path of the MP4 dataset to playback. */
    private void setPlaybackDatasetPath() {
        if (session.getPlaybackStatus() == PlaybackStatus.OK) {
            //muestro el mensaje y cambio el estado
            logAndShowErrorMessage("Session is already playing back.");
            setStateAndUpdateUI(AppState.PLAYBACK);
            return;
        }
        if (playbackDatasetPath != null) {
            try {
                session.setPlaybackDatasetUri(Uri.fromFile(new File(playbackDatasetPath)));
            } catch (PlaybackFailedException e) {
                String errorMsg = "Failed to set playback MP4 dataset. " + e;
                Log.e(TAG, errorMsg, e);
                messageSnackbarHelper.showError(this, errorMsg);
                Log.d(TAG, "Setting app state to IDLE, as the playback is not in progress.");
                setStateAndUpdateUI(AppState.IDLE);
                return;
            }
            setStateAndUpdateUI(AppState.PLAYBACK);
        }
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
        if (bundle.containsKey(DESIRED_DATASET_PATH_KEY)) {
            playbackDatasetPath = getIntent().getStringExtra(DESIRED_DATASET_PATH_KEY);
        }
        if (bundle.containsKey(DESIRED_APP_STATE_KEY)) {
            String state = getIntent().getStringExtra(DESIRED_APP_STATE_KEY);
            if (state != null) {
                switch (state) {
                    case "PLAYBACK":
                        currentState.set(AppState.PLAYBACK);
                        break;
                    case "IDLE":
                        currentState.set(AppState.IDLE);
                        break;
                    case "RECORDING":
                        currentState.set(AppState.RECORDING);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}