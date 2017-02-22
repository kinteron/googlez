package apps.justin.googlez.Activites;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;

import apps.justin.gogglez.R;
import apps.justin.googlez.Classes.Preview;


public class PreviewActivity extends AppCompatActivity {

    private static final String TAG = "AndroidCameraApi";

    private CameraCharacteristics characteristics;
    private Preview drawableView;
    private Handler cameraEventHandler;
    private Button btnStartCam;

    /*SparseIntArray
    more memory efficient way to map integers to integers
    this container keeps its mappings in an array data structure, using a binary search to find keys
     */

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        //if phone's getting rotated
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    //camera related references
    private String cameraId;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;

    //image relevant
    private Size imageDimension;
    private ImageReader imageReader;
    private File file;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private boolean mFlashSupported;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;    //used for background processes


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        cameraEventHandler = new Handler();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // used for camera operations
        Button btnCapture = (Button) findViewById(R.id.btnCapture);
        btnStartCam = (Button) findViewById(R.id.btnStartCam);
        //frames get displayed on
        SurfaceView surface = (SurfaceView) findViewById(R.id.surface);

        surface.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //distortImage();
                Toast.makeText(PreviewActivity.this, "touched screen", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        drawableView = new Preview(this);   //created SurfaceView

        Thread drawingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //makes changes onto underlying surface
            }
        });
        drawingThread.start();

        //after changes are applied - view's ready to be drawn request draw()

    }

    private void errorDialog() {
        Dialog dialog = new Dialog(this);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        dialog.setTitle(R.string.camera_access_failure_message);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                finish();
            }
        });
        dialog.create();
    }


    private String findBackFacingCamera(CameraManager manager) {

        String[] camList = null;    //hopefully list[0] is back- and list[1] is front-cam
        try {
            camList = manager.getCameraIdList();

            // Search for the front facing camera
            int numberOfCameras = camList.length;
            for (int i = 0; i < numberOfCameras; i++) {

                if(manager.getCameraCharacteristics(camList[i]).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT){
                    cameraId = camList[i];
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return cameraId;
    }

    public void openCam(View v) {
        v.setEnabled(false);
        openCamera();

    }

    //    @TargetApi(Build.VERSION_CODES.M)
    private CameraManager openCamera() {

        String camID = "";
        //get camera instance
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String[] camList = null;

        try {
            camID = findBackFacingCamera(manager);
            //hardware device information available settings and output parameters
            characteristics = manager.getCameraCharacteristics(camID);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            //control size of surface later on set the size of the surface with holder
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

        } catch (CameraAccessException e) {
            e.printStackTrace();
            errorDialog();
        }

            //assignment is done in activity_preview.XML

//        drawableView.invalidate();
//            manager.openCamera(camList[0], drawableView.getCallback(), drawableView.getHandler());
        //in order to capture or stream images from camera

        //authoritative list for all output formats, that are supported by a camera device

        try {
            if (new ContextCompat().checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return null;
            }
            manager.openCamera(camID, drawableView.getCallback(), cameraEventHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return manager;
    }

    public void captureImage(View v) {
        //takePicture();
    }


    public void createCamPreview(final CameraDevice device) {
        try {
            SurfaceHolder holder = drawableView.getHolder();
            Surface texture = holder.getSurface();
            assert texture != null;

            //set in cameraOpen
//            holder.setFixedSize(imageDimension.getWidth(), imageDimension.getHeight());

            captureRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(texture);

            startSession(texture, device);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startSession(Surface texture, final CameraDevice device) throws CameraAccessException{
        //set of output
        device.createCaptureSession(Arrays.asList(texture), new CameraCaptureSession.StateCallback(){
            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                //The camera is already closed
                if (null == device) {
                    return;
                }
                // When the session is ready, start displaying the preview.
                cameraCaptureSessions = cameraCaptureSession;
                updatePreview(device);
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                Toast.makeText(PreviewActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
            }
        }, null);

//        List<SurfaceView> outputs = new ArrayList<SurfaceView>();
//                outputs.add(new Preview(this,
//                        manager,
//                        manager.getCameraCharacteristics(manager.getCameraIdList()[0]).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)));
//        CameraDevice.StateCallback callback = drawableView.getCallback();

    }

    private void updatePreview(CameraDevice device) {
        if(null == device) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    public void closeCam(CameraDevice device) {
        if (null != device) {
            device.close();
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    public void setButton(View.OnClickListener listener){
        btnStartCam.setOnClickListener(listener);
    }

    // manager.getCameraCharacteristics(camList[0]).get(CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY);
    /** A safe way to get an instance of the Camera object. */
    //public static Camera getCameraInstance(){...} deprecated

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(PreviewActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        Log.e(TAG, "onResume");
//        startBackgroundThread();
//        if (textureView.isAvailable()) {
//            openCamera();
//        } else {
//            textureView.setSurfaceTextureListener(textureListener);
//        }
//    }
//    @Override
//    protected void onPause() {
//        Log.e(TAG, "onPause");
//        //closeCamera();
//        stopBackgroundThread();
//        super.onPause();
//    }

}
