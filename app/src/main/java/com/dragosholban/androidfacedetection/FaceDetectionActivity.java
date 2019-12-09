package com.dragosholban.androidfacedetection;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.IOException;

public class FaceDetectionActivity extends AppCompatActivity {

    private static final String TAG = "FaceDetection";
    private final int eyedimen = 20;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detection);

        final FaceDetector detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();

        Intent intent = getIntent();
        final ImageView imageView = findViewById(R.id.imageView);
        final ImageView imageView2 = findViewById(R.id.imageView2);
        final String mCurrentPhotoPath = intent.getStringExtra("mCurrentPhotoPath");

        // run image related code after the view was laid out
        // to have all dimensions calculated
        imageView.post(new Runnable() {
            @Override
            public void run() {
                if (mCurrentPhotoPath != null) {
                    Bitmap bitmap = getBitmapFromPathForImageView(mCurrentPhotoPath, imageView);
                    imageView.setImageBitmap(bitmap);

                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    SparseArray<Face> faces = detector.detect(frame);

                    Log.d(TAG, "Faces detected: " + String.valueOf(faces.size()));

                    Paint paint = new Paint();
                    paint.setColor(Color.GREEN);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5);

                    Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(mutableBitmap);
                    Bitmap facebitmap;
                    for (int i = 0; i < faces.size(); ++i) {
                        Face face = faces.valueAt(i);
                        int left_eyex,right_eyex,left_eyey,right_eyey;
                        for (Landmark landmark : face.getLandmarks()) {
                            if(landmark.getType() == Landmark.LEFT_EYE) {
                                int cx = (int) (landmark.getPosition().x);
                                int cy = (int) (landmark.getPosition().y);
                                canvas.drawCircle(cx, cy, 10, paint);
                                left_eyex = cx;
                                left_eyey = cy;
                            }
                            if(landmark.getType() == Landmark.RIGHT_EYE) {
                                int cx = (int) (landmark.getPosition().x);
                                int cy = (int) (landmark.getPosition().y);
                                canvas.drawCircle(cx, cy, 10, paint);
                                right_eyex = cx;
                                right_eyey = cy;
                            }
                        }

                        Path path = new Path();
                        path.moveTo(face.getPosition().x, face.getPosition().y);
                        path.lineTo(face.getPosition().x + face.getWidth(), face.getPosition().y);
                        path.lineTo(face.getPosition().x + face.getWidth(), face.getPosition().y + face.getHeight());
                        path.lineTo(face.getPosition().x, face.getPosition().y + face.getHeight());
                        path.close();
                        Paint redPaint = new Paint();
                        redPaint.setColor(0XFFFF0000);
                        redPaint.setStyle(Paint.Style.STROKE);
                        redPaint.setStrokeWidth(8.0f);
                        canvas.drawPath(path, redPaint);
                        facebitmap = Bitmap.createBitmap((int)face.getWidth(),(int)face.getHeight(), Bitmap.Config.ARGB_8888);
                        Canvas canvas3 = new Canvas(facebitmap);
                        Rect desRect = new Rect(0, 0, facebitmap.getWidth(), facebitmap.getHeight());
                        Rect srcRect = new Rect((int)face.getPosition().x,(int)face.getPosition().y,(int)(face.getPosition().x +face.getWidth()),(int)(face.getPosition().y+face.getHeight()));
                        canvas.drawBitmap(mutableBitmap, srcRect, desRect, null);
                        imageView2.setImageBitmap(facebitmap);
                    }

                    imageView.setImageBitmap(mutableBitmap);
                }
            }
        });
    }

    private Bitmap getBitmapFromPathForImageView(String mCurrentPhotoPath, ImageView imageView) {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        Bitmap rotatedBitmap = bitmap;

        // rotate bitmap if needed
        try {
            ExifInterface ei = new ExifInterface(mCurrentPhotoPath);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(bitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(bitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(bitmap, 270);
                    break;
            }
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return rotatedBitmap;
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}
