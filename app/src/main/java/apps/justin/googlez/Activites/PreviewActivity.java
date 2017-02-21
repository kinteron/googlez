package apps.justin.googlez.Activites;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.util.Xml;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import apps.justin.gogglez.R;
import apps.justin.googlez.Classes.Preview;



public class PreviewActivity extends AppCompatActivity {

    private CameraCharacteristics characteristics;
    private Preview drawableView;

    /*more memory efficient way to map integers to integers
    this container keeps its mappings in an array data structure, using a binary search to find keys
     */

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
    //camera related references
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest captureRequest;
    protected CaptureRequest.Builder captureRequestBuilder;

    //image relevant
    private android.hardware.Camera.Size imageDimension;
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

        XmlPullParser parser = getResources().getXml(R.layout.activity_preview));
        AttributeSet attributes = Xml.asAttributeSet(parser);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //setup Listeners for capture

        Button btnCapture = (Button) findViewById(R.id.btnCapture);



        startSession(openCamera());

        Thread drawingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //makes changes onto underlying surface
            }
        });
        drawingThread.start();

        //after changes are applied - view's ready to be drawn request draw()
        drawableView.invalidate();
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

    private CameraManager openCamera() {
        String camID = "";
        //get camera instance
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        String[] camList = null;

        try {
            camList = manager.getCameraIdList();    //hopefully list[0] is back- and list[1] is front-cam
            camID = camList[0];
            //hardware device information available settings and output parameters
            characteristics = manager.getCameraCharacteristics(camID);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            errorDialog();
        }

        CameraCharacteristics characteristics = null;

        try {

            characteristics = manager.getCameraCharacteristics(camID);
            StreamConfigurationMap configs = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            drawableView = new Preview(this, manager, configs);   //created SurfaceView
            drawableView.setLay
            //assign surface to preview object (


//            manager.openCamera(camList[0], drawableView.getCallback(), drawableView.getHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        //in order to capture or stream images from camera

        //authoritative list for all output formats, that are supported by a camera device
        StreamConfigurationMap configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        return manager;
    }

    private void startSession(CameraManager manager) {
        //set of output
        List<SurfaceView> outputs = new ArrayList<SurfaceView>();
        for(int i = 0; i < 10; i++){
            try {
                outputs.add(new Preview(this, manager, manager.getCameraCharacteristics(manager.getCameraIdList()[0]).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)));
            } catch (CameraAccessException e) {
                e.printStackTrace();
            };
        }
        CameraDevice.StateCallback callback = drawableView.getCallback();
        drawableView.getHandler();

    }


    // manager.getCameraCharacteristics(camList[0]).get(CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY);
    /** A safe way to get an instance of the Camera object. */
    //public static Camera getCameraInstance(){...} deprecated

}
