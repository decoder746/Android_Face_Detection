package com.dragosholban.androidfacedetection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.SparseArray;
import android.widget.ImageView;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;

import java.io.ByteArrayOutputStream;

import static com.dragosholban.androidfacedetection.FaceDetectionActivity.rotateImage;

class MyFaceDetector extends Detector<Face> {
    private Detector<Face> mDelegate;
    public ImageView mimv1;
    public ImageView mimv2;
    public ImageView mimv3;
    public Frame mFrame;
    public int width,height;
    MyFaceDetector(Detector<Face> delegate) {
        mDelegate = delegate;
    }

    public SparseArray<Face> detect(Frame frame) {
        // *** add your custom frame processing code here
        width = frame.getMetadata().getWidth();
        height = frame.getMetadata().getHeight();
        mFrame = frame;
        YuvImage yuvImage = new YuvImage(frame.getGrayscaleImageData().array(), ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, byteArrayOutputStream);
        byte[] jpegArray = byteArrayOutputStream.toByteArray();
        Bitmap bmp = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.length);
        SparseArray<Face> detectedFaces = mDelegate.detect(frame);
        for(int i=0;i<detectedFaces.size();i++){          //can't use for-each loops for SparseArrays
            Face face = detectedFaces.valueAt(i);
            //get it's coordinates
            Bitmap faceBitmap = Bitmap.createBitmap(bmp, (int)(scaleX(bmp,frame,face.getPosition().x-face.getWidth()/2)),(int)scaleY(bmp,frame,(face.getPosition().y-face.getHeight()/2)),(int)scaleX(bmp,frame,face.getWidth()),(int)scaleY(bmp,frame,face.getHeight()));
            //Do whatever you want with this cropped Bitmap
            Bitmap rotatedbitmap = rotateImage(faceBitmap,270);
            mimv1.setImageBitmap(rotatedbitmap);
            Bitmap lefteyeBitmap = Bitmap.createBitmap(bmp, (int)(scaleX(bmp,frame,face.getPosition().x+3*face.getWidth()/10)),(int)scaleY(bmp,frame,face.getPosition().y+3*face.getHeight()/8),40,40);
            Bitmap righteyeBitmap = Bitmap.createBitmap(bmp,(int)(scaleX(bmp,frame,face.getPosition().x+7*face.getWidth()/10)),(int)scaleY(bmp,frame,face.getPosition().y+3*face.getHeight()/8),40,40);
            Bitmap rleb = rotateImage(lefteyeBitmap,270);
            Bitmap rreb = rotateImage(righteyeBitmap,270);
            mimv2.setImageBitmap(rleb);
            mimv3.setImageBitmap(rreb);
        }

        return mDelegate.detect(frame);
    }

    public Frame getmFrame() {
        return mFrame;
    }

    public boolean isOperational() {
        return mDelegate.isOperational();
    }
    public void setimageview(ImageView imv1,ImageView imv2,ImageView imv3){
        mimv1 = imv1;mimv2 = imv2; mimv3 = imv3;
    }
    public boolean setFocus(int id) {
        return mDelegate.setFocus(id);
    }
    public float scaleX(Bitmap bmp,Frame frame,float x){
        return ((float)bmp.getWidth()/(float)width)*x;
    }
    public float scaleY(Bitmap bmp,Frame frame,float x){
        return ((float)bmp.getHeight()/(float)height)*x;
    }
}