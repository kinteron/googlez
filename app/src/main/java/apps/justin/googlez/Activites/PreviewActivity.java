package apps.justin.googlez.Activites;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.graphics.ImageFormat;
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
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import apps.justin.gogglez.R;
import apps.justin.googlez.Classes.Preview;


public class PreviewActivity extends AppCompatActivity {

    private static final String TAG = "AndroidCameraApi";

    private CameraCharacteristics characteristics;
    private SurfaceView drawableView;
    private Handler cameraEventHandler;
    private CameraDevice device;
    private Button btnStartCam;
    private Preview preview;

    /*SparseIntArray
    more memory efficient way to map integers to integers
    this container keeps its mappings in an array data structure, using a binary search to find keys
     */

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

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
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;    //used for background processes
    private CameraCaptureSession captureSessions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

//        cameraEventHandler = new Handler();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // used for camera operations
        btnStartCam = (Button) findViewById(R.id.btnStartCam);

        //frames get displayed on
        SurfaceView surface = (SurfaceView) findViewById(R.id.surface);
        surface.getHolder().addCallback(preview = new Preview(this));

        surface.getHolder().setFixedSize(300, 300);
//        preview.surfaceCreated(holder);
        // deprecated setting, but required on Android versions prior to 3.0
        surface.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        drawableView = surface;

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

                if (manager.getCameraCharacteristics(camList[i]).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
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
            manager.openCamera(camID, preview.getCallback(), cameraEventHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return manager;
    }

    public void captureImage(View v) {
        if (cameraId == null) {
            Toast.makeText(PreviewActivity.this, "activate Camera first", Toast.LENGTH_SHORT).show();
            return;
        }
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            Size[] sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);

            int width = 640, height = 480;

            width = sizes[0].getWidth();
            height = sizes[0].getHeight();

            //maxImages = 1
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            ArrayList<Surface> outputs = new ArrayList<>();
            outputs.add(reader.getSurface());
            outputs.add(drawableView.getHolder().getSurface());

            //prioritizing image quality over frame rate
            final CaptureRequest.Builder captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            // Orientation
//            int rotation = getWindowManager().getDefaultDisplay().getRotation();
//            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            final File file = new File(Environment.getExternalStorageDirectory() + "/img.jpg");
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);
                        try {
                            save(bytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } finally { //certain call / close image always
                        if (image != null) {
                            image.close();
                        }
                    }
                }
            };

            //delegate handling to backgroundThread
            reader.setOnImageAvailableListener(imgAvailableListener(),backgroundHandler);

            device.createCaptureSession(outputs, preview.getSessionCallback(device), backgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback captureCallback(){
        return new CameraCaptureSession.CaptureCallback(){
            @Override
            public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                super.onCaptureCompleted(session, request, result);
                Toast.makeText(PreviewActivity.this, "saved File", Toast.LENGTH_SHORT).show();
                createCamPreview(device);
            }
        };
    }

    private ImageReader.OnImageAvailableListener imgAvailableListener() {
        return new ImageReader.OnImageAvailableListener(){

            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = null;
                try {
                    image = reader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);
                    save(bytes);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (image != null) {
                        image.close();
                    }
                }
            }
        };
    }

    private void save(byte[] bytes) throws IOException{
        OutputStream output = null;
        try {
            output = new FileOutputStream(file);
            output.write(bytes);
        } finally {
            if (null != output) {
                output.close();
            }
        }
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

    private void startSession(Surface texture, final CameraDevice device) throws CameraAccessException {
        //set of output
        device.createCaptureSession(Arrays.asList(texture), new CameraCaptureSession.StateCallback() {
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

    public void updatePreview(CameraDevice device) {
        if (null == device) {
            Log.e(TAG, "error");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            //continuously capturing frames
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
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

    public void setButton(View.OnClickListener listener) {
        btnStartCam.setOnClickListener(listener);
    }

    // manager.getCameraCharacteristics(camList[0]).get(CameraMetadata.COLOR_CORRECTION_ABERRATION_MODE_HIGH_QUALITY);

    /**
     * A safe way to get an instance of the Camera object.
     */
    //public static Camera getCameraInstance(){...} deprecated
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(PreviewActivity.this, "Allow permission first", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG, "onResume");
        createBackgroundThread();
    }

    private void createBackgroundThread() {
        this.backgroundThread = new HandlerThread("background action");
        backgroundThread.start();

        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        //closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    private void stopBackgroundThread() {
        //quit looper
        backgroundThread.quitSafely();
    }

    public void setCaptureSessions(CameraCaptureSession captureSessions) {
        this.captureSessions = captureSessions;
    }

    public void setDevice(CameraDevice device) {
        this.device = device;
    }
}
