/*
 * Copyright 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.gc.crc.rnad.indoorwifi;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.GuardedBy;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Anchor.CloudAnchorState;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Config.CloudAnchorMode;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Array;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ca.gc.crc.rnad.common.helpers.CameraPermissionHelper;
import ca.gc.crc.rnad.common.helpers.DisplayRotationHelper;
import ca.gc.crc.rnad.common.helpers.FullScreenHelper;
import ca.gc.crc.rnad.common.helpers.SnackbarHelper;
import ca.gc.crc.rnad.common.rendering.BackgroundRenderer;
import ca.gc.crc.rnad.common.rendering.ObjectRenderer;
import ca.gc.crc.rnad.common.rendering.ObjectRenderer.BlendMode;
import ca.gc.crc.rnad.common.rendering.PlaneRenderer;
import ca.gc.crc.rnad.common.rendering.PointCloudRenderer;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.annotation.SuppressLint;
import android.widget.TextView;

import static android.app.Notification.VISIBILITY_PUBLIC;
import static java.lang.Thread.sleep;

/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3d model of the Android robot.
 */
public class MainActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    public String t = new Timestamp(System.currentTimeMillis()).toString();
    private static final String TAG = MainActivity.class.getSimpleName();
    private MqttAndroidClient client0;
    private PahoMqttClient pahoMqttClient;
    public EditText msg2;
    public String  subscribeTopic1, subscribeTopic2,subscribeTopic3;
    private Button publishMessage, subscribe, unSubscribe;
    String IPaddress;
    AnchorDB anchorDB;
    EditText hello;
    Frame frame;
    // Rendering. The Renderers are created here, and initialized when the GL surface is created.
    private GLSurfaceView surfaceView;
    private final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();
    private final ObjectRenderer virtualObject = new ObjectRenderer();
    private final ObjectRenderer virtualObjectShadow = new ObjectRenderer();
    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();
    private final float[] anchorMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] colorCorrectionRgba = new float[4];
    public String  info;
    TextView details;
    private final Object singleTapAnchorLock = new Object();
    @GuardedBy("singleTapAnchorLock")
    private MotionEvent queuedSingleTap;

    private final SnackbarHelper snackbarHelper = new SnackbarHelper();
    private GestureDetector gestureDetector;
    private DisplayRotationHelper displayRotationHelper;
    private Session session;
    private boolean installRequested;
    @Nullable
    @GuardedBy("singleTapAnchorLock")
    private Anchor anchor;
    private List <Anchor> anchors = new ArrayList<Anchor>();
    private StorageManager storageManager;
    private enum AppAnchorState {
        NONE,
        HOSTING,
        HOSTED,
        RESOLVING,
        RESOLVED
    }
    public int shortcodee = 10;
    @GuardedBy("singleTapAnchorLock")
    private AppAnchorState appAnchorState = AppAnchorState.NONE;


    /** Handles a single tap during a {@link #onDrawFrame(GL10)} call. */
    private void handleTapOnDraw(TrackingState currentTrackingState, Frame currentFrame) {
        synchronized (singleTapAnchorLock) {
//      if (anchor == null && queuedSingleTap != null && currentTrackingState == TrackingState.TRACKING)
            if ( queuedSingleTap != null && currentTrackingState == TrackingState.TRACKING){
//          && appAnchorState == AppAnchorState.NONE) {
                for (HitResult hit : currentFrame.hitTest(queuedSingleTap)) {
                    if (shouldCreateAnchorWithHit(hit)) {
                        Anchor newAnchor = session.hostCloudAnchor(hit.createAnchor());
                        setNewAnchor(newAnchor);
                        appAnchorState = AppAnchorState.HOSTING;
                        snackbarHelper.showMessage(this, "Now hosting anchor...");
                        break;
                    }
                }
            }
            queuedSingleTap = null;
        }
    }
    public void displayDetails(View v){
        Log.d("hh", v.getId() + "=="+R.id.networks +"" );
            Intent i = new Intent(MainActivity.this,WiFiScannerActivity.class);
            startActivity(i);
            }



    /**
     * Returns {@code true} if and only if {@code hit} can be used to create an anchor.
     *
     * <p>Checks if a plane was hit and if the hit was inside the plane polygon, or if an oriented
     * point was hit. We only want to create an anchor if the hit satisfies these conditions.
     */
    private static boolean shouldCreateAnchorWithHit(HitResult hit) {
        Trackable trackable = hit.getTrackable();
        if (trackable instanceof Plane) {
            // Check if any plane was hit, and if it was hit inside the plane polygon
            return ((Plane) trackable).isPoseInPolygon(hit.getHitPose());
        } else if (trackable instanceof Point) {
            // Check if an oriented point was hit.
            return ((Point) trackable).getOrientationMode() == OrientationMode.ESTIMATED_SURFACE_NORMAL;
        }
        return false;
    }

    /** Checks the anchor after an update. */
    private void checkUpdatedAnchor() {
        synchronized (singleTapAnchorLock) {
            if (appAnchorState != AppAnchorState.HOSTING && appAnchorState != AppAnchorState.RESOLVING) {
                // Do nothing if the app is not waiting for a hosting or resolving action to complete.
                return;
            }
            CloudAnchorState cloudState = anchor.getCloudAnchorState();
            //   anchorIdForPrinting = anchor.getCloudAnchorId();
            if (appAnchorState == AppAnchorState.HOSTING) {

                // If the app is waiting for a hosting action to complete.
                if (cloudState.isError()) {
                    snackbarHelper.showMessageWithDismiss(this, "Error hosting anchor: " + cloudState);
                    appAnchorState = AppAnchorState.NONE;

                } else if (cloudState == CloudAnchorState.SUCCESS) {
                    storageManager.nextShortCode(
                            (shortCode) -> {
                                if (shortCode == null) {
                                    snackbarHelper.showMessageWithDismiss(this, "Could not obtain a short code.");
                                    return;
                                }
                                synchronized (singleTapAnchorLock) {
                                    storageManager.storeUsingShortCode(shortCode, anchor.getCloudAnchorId());
                                    snackbarHelper.showMessageWithDismiss(
                                            this, "Anchor hosted successfully! Cloud Short Code: " + shortCode);
                                    try{
                                        msg2.setText(shortCode+ "");
                                        setShortCode(shortCode);
                                        try {
                                            String msg = msg2.getText().toString().trim();


                                            if (!msg.isEmpty()) {
                                                try {
                                                    pahoMqttClient.publishMessage(client0, msg, 1, Constants.PUBLISH_TOPIC1);
                                                } catch (MqttException e) {
                                                    e.printStackTrace();
                                                } catch (UnsupportedEncodingException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        } catch (NullPointerException e){
                                            //
                                        }

                                    }catch (Exception e){}}
                            });
                    appAnchorState = AppAnchorState.HOSTED;

                    // Add by Yudong, only successful to show the anchor
                    anchors.add(anchor);

                }
            } else if (appAnchorState == AppAnchorState.RESOLVING) {
                // If the app is waiting for a resolving action to complete.
                if (cloudState.isError()) {
                    snackbarHelper.showMessageWithDismiss(this, "Error resolving anchor: " + cloudState);
                    appAnchorState = AppAnchorState.NONE;
                } else if (cloudState == CloudAnchorState.SUCCESS) {
                    snackbarHelper.showMessageWithDismiss(this, "Anchor resolved successfully!");
                    appAnchorState = AppAnchorState.RESOLVED;

                }
            }
        }

    }

    /**
     * Callback function that is invoked when the OK button in the resolve dialog is pressed.
     *
     * @param dialogValue The value entered in the resolve dialog.
     */
    private void onResolveOkPressed(String dialogValue) {
        int shortCode = Integer.parseInt(dialogValue);
        storageManager.getCloudAnchorID(
                shortCode,
                (cloudAnchorId) -> {
                    if (cloudAnchorId == null) {
                        return;
                    }
                    synchronized (singleTapAnchorLock) {
                        Anchor resolvedAnchor = session.resolveCloudAnchor(cloudAnchorId);
                        setNewAnchor(resolvedAnchor);
                        snackbarHelper.showMessage(this, "Now resolving anchor...");
                        appAnchorState = AppAnchorState.RESOLVING;
                        anchors.add (resolvedAnchor);

                    }

                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        anchorDB = new AnchorDB(getApplicationContext());
        anchorDB.openDB();
        hello = (EditText) findViewById(R.id.hello);
        startThread();
        msg2 = findViewById(R.id.publishMessage2);
        surfaceView = findViewById(R.id.surfaceview);
        displayRotationHelper = new DisplayRotationHelper(this);
        pahoMqttClient = new PahoMqttClient();
        details = findViewById(R.id.textView);
        details.setText("");
        Button button = findViewById(R.id.button);
        publishMessage = findViewById(R.id.publishMessage);
        pahoMqttClient = new PahoMqttClient();
        subscribeTopic1 = "CONNECTION OKAY";
        publishMessage = (Button) findViewById(R.id.publishMessage);
        String subscribeTopic= ("anchorId");
        client0 = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID0 );
        publishMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String topic = subscribeTopic.trim();

                try {
                    pahoMqttClient.subscribe(client0, topic, 1);
                } catch (MqttException e) {
                    e.printStackTrace();}
                String msg = subscribeTopic1.trim();
                if (!msg.isEmpty()) {
                    try {
                        pahoMqttClient.publishMessage(client0, msg, 1, Constants.PUBLISH_TOPIC1);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }

            }
        });


        Intent intent = new Intent(MainActivity.this, MqttMessageService.class);
        startService(intent);

        // Set up tap listener.
        gestureDetector =
                new GestureDetector(
                        this,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onSingleTapUp(MotionEvent e) {
                                synchronized (singleTapAnchorLock) {
                                    queuedSingleTap = e;
                                }
                                return true;
                            }

                            @Override
                            public boolean onDown(MotionEvent e) {
                                return true;
                            }
                        });
        surfaceView.setOnTouchListener((unusedView, event) -> gestureDetector.onTouchEvent(event));

        // Set up renderer.
        surfaceView.setPreserveEGLContextOnPause(true);
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        installRequested = false;

        // Initialize the "Clear" button. Clicking it will clear the current anchor, if it exists.
        Button clearButton = findViewById(R.id.clear_button);
        clearButton.setOnClickListener(
                (unusedView) -> {
                    synchronized (singleTapAnchorLock) {
                        onClearButton();
                    }
                });

        Button resolveButton = findViewById(R.id.resolve_button);
        resolveButton.setOnClickListener(
                (unusedView) -> {
                    ResolveDialogFragment dialog = new ResolveDialogFragment();
                    dialog.setOkListener(this::onResolveOkPressed);
                    dialog.show(getSupportFragmentManager(), "Resolve");
                });

        storageManager = new StorageManager(this);


    }

    @Override
    protected void onResume() {
        super.onResume();

        if (session == null) {
            Exception exception = null;
            int messageId = -1;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }
                session = new Session(this);
            } catch (UnavailableArcoreNotInstalledException e) {
                messageId = R.string.snackbar_arcore_unavailable;
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                messageId = R.string.snackbar_arcore_too_old;
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                messageId = R.string.snackbar_arcore_sdk_too_old;
                exception = e;
            } catch (Exception e) {
                messageId = R.string.snackbar_arcore_exception;
                exception = e;
            }

            if (exception != null) {
                snackbarHelper.showError(this, getString(messageId));
                Log.e(TAG, "Exception creating session", exception);
                return;
            }

            // Create default config and check if supported.
            Config config = new Config(session);
            config.setCloudAnchorMode(CloudAnchorMode.ENABLED);
            session.configure(config);
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            // In some cases (such as another camera app launching) the camera may be given to
            // a different app instead. Handle this properly by showing a message and recreate the
            // session at the next iteration.
            snackbarHelper.showError(this, getString(R.string.snackbar_camera_unavailable));
            session = null;
            return;
        }
        surfaceView.onResume();
        displayRotationHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper.onPause();
            surfaceView.onPause();
            session.pause();
        }
    }
    public void onButtonClick(View v){
        if(v.getId() == R.id.graph){
            Intent i = new Intent(MainActivity.this,graph.class);
            startActivity(i);

        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().
            backgroundRenderer.createOnGlThread(this);
            planeRenderer.createOnGlThread(this, "models/trigrid.png");
            pointCloudRenderer.createOnGlThread(this);

            virtualObject.createOnGlThread(this, "models/andy.obj", "models/andy.png");
            virtualObject.setMaterialProperties(0.0f, 2.0f, 0.5f, 6.0f);

            virtualObjectShadow.createOnGlThread(
                    this, "models/andy_shadow.obj", "models/andy_shadow.png");
            virtualObjectShadow.setBlendMode(BlendMode.Shadow);
            virtualObjectShadow.setMaterialProperties(1.0f, 0.0f, 0.0f, 1.0f);

        } catch (IOException ex) {
            Log.e(TAG, "Failed to read an asset file", ex);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        displayRotationHelper.onSurfaceChanged(width, height);
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (session == null) {
            return;
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper.updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(backgroundRenderer.getTextureId());

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
             frame = session.update();
            Camera camera = frame.getCamera();
            TrackingState cameraTrackingState = camera.getTrackingState();

            // Check anchor after update.
            checkUpdatedAnchor();

            // Handle taps.
            handleTapOnDraw(cameraTrackingState, frame);

            // Draw background.
            backgroundRenderer.draw(frame);

            // If not tracking, don't draw 3d objects.
            if (cameraTrackingState == TrackingState.PAUSED) {
                return;
            }

            // Get projection and camera matrices.
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);
            camera.getViewMatrix(viewMatrix, 0);

            // Visualize tracked points.
            PointCloud pointCloud = frame.acquirePointCloud();
            pointCloudRenderer.update(pointCloud);
            pointCloudRenderer.draw(viewMatrix, projectionMatrix);

            // Application is responsible for releasing the point cloud resources after
            // using it.
            pointCloud.release();

            // Visualize planes.
            planeRenderer.drawPlanes(
                    session.getAllTrackables(Plane.class), camera.getDisplayOrientedPose(), projectionMatrix);

            // Visualize anchor.

            for (Anchor ak: anchors) {
                boolean shouldDrawAnchor = false;
                synchronized (singleTapAnchorLock) {
                    if (ak != null && ak.getTrackingState() == TrackingState.TRACKING) {
                        frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

                        // Get the current pose of an Anchor in world space. The Anchor pose is updated
                        // during calls to session.update() as ARCore refines its estimate of the world.
                        ak.getPose().toMatrix(anchorMatrix, 0);
                        shouldDrawAnchor = true;
                    }
                }
                if (shouldDrawAnchor) {
                    draw_Anchor(frame);
                }
            }
        } catch (Throwable t) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t);
        }
    }

    private void draw_Anchor(Frame frame) {
        float scaleFactor = 1.0f;
        frame.getLightEstimate().getColorCorrection(colorCorrectionRgba, 0);

        // Update and draw the model and its shadow.
        virtualObject.updateModelMatrix(anchorMatrix, scaleFactor);
        virtualObjectShadow.updateModelMatrix(anchorMatrix, scaleFactor);
        virtualObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba);
        virtualObjectShadow.draw(viewMatrix, projectionMatrix, colorCorrectionRgba);
    }

    /** Sets the new anchor in the scene. */
    @GuardedBy("singleTapAnchorLock")
    private void setNewAnchor(@Nullable Anchor newAnchor) {
//    if (anchor != null) {
//      anchor.detach();
//    }
        anchor = newAnchor;
        appAnchorState = AppAnchorState.NONE;
        snackbarHelper.hide(this);
    }


    /** Sets the new anchor in the scene. */
    @GuardedBy("singleTapAnchorLock")
    private void onClearButton() {
        for (Anchor ak: anchors) {
            if (ak != null) {
                ak.detach();
            }
        }
        anchor = null;
        anchors.clear();

        appAnchorState = AppAnchorState.NONE;
        snackbarHelper.showMessage(this, "All anchors have been cleared.");
    }


    public void setShortCode(int e){
        shortcodee = e;
    }
    public String getShortCode(){
        return (" "+shortcodee);
    }

    public JSONObject JsonWifiInfo() throws CameraNotAvailableException, JSONException, UnavailableArcoreNotInstalledException, UnavailableSdkTooOldException, UnavailableApkTooOldException {
        @SuppressLint("WifiManagerLeak") WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        JSONObject jsonObject = new JSONObject();
        if (!(msg2.getText().toString().equalsIgnoreCase("10"))) {
            for (Anchor anchor : anchors) {
                Camera camera = frame.getCamera();

                Pose a = camera.getPose();
                Pose b = anchor.getPose();
                float distance = getDistance(a, b);
                Log.d("distance", distance+"");
                 if (distance < 1) {
                    try {
                        jsonObject.put("ShortCode", storageManager.getShortCode(anchor.getCloudAnchorId()));
                        Log.d("sss",storageManager.getShortCode(anchor.getCloudAnchorId())+"");
                        jsonObject.put("Frequency", (float) wifiInfo.getFrequency() / 1000 + "GHz");
                        jsonObject.put("Strength", wifiInfo.getRssi() + "dBm");
                        jsonObject.put("speed", wifiInfo.getLinkSpeed() + "Mbps");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

               }
            }
        }   else {jsonObject.put("No Data", "Create Anchor or get closer to any" );}
        return jsonObject;
    }
                //else{return jsonObject.put("Anchor","No Wifi Info");}


                public JSONObject JsonAnchor () throws JSONException {
                    JSONObject jsonObject = new JSONObject();
                    @SuppressLint("WifiManagerLeak") WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    IPaddress = Formatter.formatIpAddress(wifiInfo.getIpAddress()) + "";
                    if(!(msg2.getText().toString().equalsIgnoreCase("10")) ){
                    try {
                        jsonObject.put("Anchor ID ", msg2.getText().toString());
                        jsonObject.put("IPadress", IPaddress);

                    } catch (JSONException e) {
                        e.printStackTrace();}

                    }else {jsonObject.put("Anchor","No Anchor yet");
                          jsonObject.put("IPaddress", IPaddress);
                    }
                    return jsonObject;
                }
                public void startThread () {
                    ExampleThread runnable = new ExampleThread();
                    new Thread(runnable).start();
                }

                class ExampleThread implements Runnable {
                    @SuppressLint("WifiManagerLeak")
                    WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                    @Override
                    public void run() {
                        int i = 0;
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                        while (1 == 1) {
                            {
                                getMessageNotification();
                                try {

                                    anchorDB.insertRecord(getShortCode(), (float) wifiInfo.getFrequency() / 1000 + "GHz", wifiInfo.getLinkSpeed() + "Mbps", wifiInfo.getRssi() + "dbm");
                                    Log.d("sub34", "DB SUC");
                                } catch (Exception e) {
                                    Log.d("sub34", "NO DB");
                                }
                                try {
                                    Thread.sleep(20000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }

                            try {
                                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                                String msg = "I am  alive " + timestamp + "  IPaddress" + IPaddress;


                                if (!msg.isEmpty()) {
                                    try {
                                        pahoMqttClient.publishMessage(client0, msg, 1, Constants.PUBLISH_TOPIC3);
                                        try {
                                            pahoMqttClient.publishMessage(client0, JsonWifiInfo().toString(), 1, Constants.PUBLISH_TOPIC2);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        } catch (UnavailableArcoreNotInstalledException e) {
                                            e.printStackTrace();
                                        } catch (UnavailableSdkTooOldException e) {
                                            e.printStackTrace();
                                        } catch (UnavailableApkTooOldException e) {
                                            e.printStackTrace();
                                        }
                                        pahoMqttClient.publishMessage(client0, JsonAnchor().toString(), 1, Constants.PUBLISH_TOPIC4);

                                    } catch (MqttException e) {
                                        e.printStackTrace();
                                    } catch (UnsupportedEncodingException e) {
                                        e.printStackTrace();
                                    } catch (CameraNotAvailableException e) {
                                        e.printStackTrace();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            } catch (NullPointerException e) {
                                try {
                                    Thread.sleep(10000);
                                } catch (InterruptedException er) {
                                    er.printStackTrace();
                                }
                            }
                            i++;
                        }
                    }
                }

                public void getMessageNotification () {
                    Log.d("hello", "why");
                    NotificationManager mNotificationManager =
                            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    List<StatusBarNotification> a = new ArrayList<>();
                    for (StatusBarNotification statusBarNotification : a = Arrays.asList(mNotificationManager.getActiveNotifications())) {
                        if (statusBarNotification.getId() == 100) {
                            String shortCode3 = statusBarNotification.getNotification().tickerText.toString();
                            Log.d("hello", shortCode3);

                            if (!shortCode3.equalsIgnoreCase("CONNECTION OKAY")) {
                                 //.setText( shortCode3);
                                Log.d("shortCode", msg2.getText().toString()+ "==" + shortCode3 + "now");
                                try {
                                    onResolveOkPressed(shortCode3);
                                    Log.d("OnResolve", "good");

                                } catch (NullPointerException e) {
                                }

                            }
                        }


                    }
                }



  public float getDistance(Pose startPose, Pose endPose){
        float dx = startPose.tx() - endPose.tx();
        float dy = startPose.ty() - endPose.ty();
        float dz = startPose.tz() - endPose.tz();

// Compute the straight-line distance.
        float distanceMeters = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
        return distanceMeters;
    }

}

