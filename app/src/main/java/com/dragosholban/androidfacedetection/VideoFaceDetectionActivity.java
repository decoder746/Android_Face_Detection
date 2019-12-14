package com.dragosholban.androidfacedetection;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;


import java.io.IOException;


public class VideoFaceDetectionActivity extends AppCompatActivity{

    private CameraPreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private CameraSource mCameraSource = null;
    public Frame curFrame;


    private ImageView imageView3; // Face (Top right)
    private ImageView imageView4; // Left eye (Top left)
    private ImageView imageView5; // Right eye (Bottom left)
    private ImageView imageView6; // Face Mask (Bottom right)
    private TextView textView; // X coordinate
    private TextView textView2; // Y coordinate

    private Classifier classifier;

    private static final String TAG = "VideoFaceDetection";
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int RC_HANDLE_GMS = 2;
    public MyFaceDetector myFaceDetector;
    public MyFaceDetector getMyFaceDetector(){
        return myFaceDetector;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_face_detection);

        imageView3 = findViewById(R.id.imageView3);
        imageView4 = findViewById(R.id.imageView4);
        imageView5 = findViewById(R.id.imageView5);
        imageView6 = findViewById(R.id.imageView6);
        textView = findViewById(R.id.textView);
        textView2 = findViewById(R.id.textView2);
        myButtonListenerMethod();

        try {
            classifier = new Classifier(this);
            Log.i(TAG,"Classifier initialized");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mPreview = findViewById(R.id.preview);
        mGraphicOverlay = findViewById(R.id.faceOverlay);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // permission not granted, initiate request
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            createCameraSource();
        }
    }

    public void myButtonListenerMethod() {
        Button button = (Button) findViewById(R.id.button_classify);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap scaledBitmap1 = getResizedBitmap((((BitmapDrawable)imageView3.getDrawable()).getBitmap()),classifier.DIM_IMG_SIZE_X, classifier.DIM_IMG_SIZE_Y);
                Bitmap scaledBitmap2 = getResizedBitmap((((BitmapDrawable)imageView4.getDrawable()).getBitmap()),classifier.DIM_IMG_SIZE_X, classifier.DIM_IMG_SIZE_Y);
                Bitmap scaledBitmap3 = getResizedBitmap((((BitmapDrawable)imageView5.getDrawable()).getBitmap()),classifier.DIM_IMG_SIZE_X, classifier.DIM_IMG_SIZE_Y);
                Bitmap scaledBitmap4 = getResizedBitmap((((BitmapDrawable)imageView6.getDrawable()).getBitmap()),classifier.DIM_FLAT_SIZE, classifier.DIM_FLAT_SIZE);
//                Bitmap scaledBitmap1 = ((BitmapDrawable)imageView3.getDrawable()).getBitmap();
//                Bitmap scaledBitmap2 = ((BitmapDrawable)imageView4.getDrawable()).getBitmap();
//                Bitmap scaledBitmap3 = ((BitmapDrawable)imageView5.getDrawable()).getBitmap();
//                Bitmap scaledBitmap4 = ((BitmapDrawable)imageView6.getDrawable()).getBitmap();
                Log.i(TAG,"Reached before classifier");
                float[][] digit = classifier.classify(scaledBitmap1,scaledBitmap2,scaledBitmap3,scaledBitmap4);
                Log.i(TAG,"Reached after classifier");
                textView.setText(String.valueOf(digit[0][0]));
                textView2.setText(String.valueOf(digit[0][1]));
            }
        });
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CAMERA_PERMISSION && resultCode == RESULT_OK) {
            createCameraSource();
        }
    }

    private void createCameraSource() {
        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.FAST_MODE)
                .build();
        myFaceDetector = new MyFaceDetector(detector);
        myFaceDetector.setimageview(imageView3,imageView4,imageView5);
        myFaceDetector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        mCameraSource = new CameraSource.Builder(context, myFaceDetector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(100.0f)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    private void startCameraSource() {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
                mPreview.setImageView(imageView3,imageView4,imageView5,imageView6,myFaceDetector);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }
}
