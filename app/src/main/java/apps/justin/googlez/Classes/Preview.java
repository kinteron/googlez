package apps.justin.googlez.Classes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.util.Xml;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;

import apps.justin.googlez.Activites.PreviewActivity;

/**
 * Created by Justin on 20.11.2016.
 *
 * hold information of changes to the surface
 * surface is available between surfaceCreated and surfaceDestroyed
 */
public class Preview extends SurfaceView implements SurfaceHolder.Callback{

    private SurfaceHolder holder;
    private PreviewActivity primary;

    public Preview(Context c){
        super(c);
        //access PreviewActivity methods
        primary = (PreviewActivity) c;
        holder = getHolder();
        holder.addCallback(this);
        this.surfaceCreated(holder);
    }

    public Preview(Context context, StreamConfigurationMap configs) {
        this(context);

        Size[] outputSizes = configs.getOutputSizes(SurfaceView.class);
//                holder.setFixedSize(outputSizes[0].getWidth(), outputSizes[0].getHeight());
        holder.setFixedSize(300, 300);
        // deprecated setting, but required on Android versions prior to 3.0
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    public Preview(Context context, CameraManager manager, StreamConfigurationMap configs) {
        this(context, configs);


    }

    /*** \redrawing process/
     * The Android framework will only call onDraw() as necessary.
     * Each time that your application is prepared to be drawn, you must
     * request your View be invalidated by calling invalidate().
     * This indicates that you'd like your View to be
     * drawn and Android will then call your onDraw() method
     * (though is not guaranteed that the callback will be instantaneous).
     */

    @Override
    protected synchronized void onDraw(Canvas canvas) {  //called by android framework view draw itself
        super.onDraw(canvas);

        //all calls to draw through the canvas
        //update physics?
        //fisheye ball

        holder.lockCanvas();    //frees canvas for editing | locks for any other purposes
        //start editing pixels

        //TODO edit canvas and repaint e.g. drawColor()


        //done with editing / drawing on canvas - unlocking
        holder.unlockCanvasAndPost(canvas);
    }



    @Override
    public void surfaceCreated(SurfaceHolder holder) { //don't necessarily draw in here when rendering is proceeded anywhere else
        Log.d(VIEW_LOG_TAG, "surface Created ");
        Toast.makeText(primary, VIEW_LOG_TAG + "surface Created", Toast.LENGTH_SHORT).show();
        primary.openCam();
//        try {
//            // create the surface and start camera preview
////            if (mCamera == null) {
////                mCamera.setPreviewDisplay(holder);
////                mCamera.startPreview();
////            }
//        } catch (IOException e) {
//            Log.d(VIEW_LOG_TAG, "Error setting camera preview: " + e.getMessage());
//        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public CameraDevice.StateCallback getCallback(){
        return new CameraDevice.StateCallback(){
            @Override
            public void onOpened(CameraDevice camera) {
                Log.d(VIEW_LOG_TAG, "camera opened" + camera);
                primary.createCamPreview(camera);
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                camera.close();
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                camera.close();
            }
        };
    }
}