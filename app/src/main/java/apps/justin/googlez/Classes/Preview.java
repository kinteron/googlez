package apps.justin.googlez.Classes;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.util.Xml;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
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
public class Preview implements SurfaceHolder.Callback{

    private PreviewActivity primary;

    public Preview(Context c){

        //access PreviewActivity methods
        primary = (PreviewActivity) c;


    }

    /*** \redrawing process/
     * The Android framework will only call onDraw() as necessary.
     * Each time that your application is prepared to be drawn, you must
     * request your View be invalidated by calling invalidate().
     * This indicates that you'd like your View to be
     * drawn and Android will then call your onDraw() method
     * (though is not guaranteed that the callback will be instantaneous).
     */


//    protected synchronized void onDraw(Canvas canvas) {  //called by android framework view draw itself
//
//        //all calls to draw through the canvas
//        //update physics?
//        //fisheye ball
//
//        holder.lockCanvas();    //frees canvas for editing | locks for any other purposes
//        //start editing pixels
//
//        //TODO edit canvas and repaint e.g. drawColor()
//
//
//        //done with editing / drawing on canvas - unlocking
//        holder.unlockCanvasAndPost(canvas);
//    }



    @Override
    public void surfaceCreated(SurfaceHolder holder) { //don't necessarily draw in here when rendering is proceeded anywhere else
        Toast.makeText(primary, "surface created", Toast.LENGTH_SHORT).show();
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

                final CameraDevice device = camera;
                primary.setDevice(camera);
                primary.setButton(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        primary.closeCam(device);
                    }
                });
                primary.createCamPreview(device);
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                final CameraDevice device = camera;
                primary.setButton(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        primary.openCam(v);
                    }
                });
                primary.closeCam(camera);
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                primary.closeCam(camera);
            }
        };
    }

    public CameraCaptureSession.StateCallback getSessionCallback(final CameraDevice device){
        return new CameraCaptureSession.StateCallback(){

            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {

                primary.setCaptureSessions(cameraCaptureSession);
//                primary.updatePreview(device);
            }
            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                Toast.makeText(primary, "configuration failed", Toast.LENGTH_SHORT).show();
            }
        };
    }
}