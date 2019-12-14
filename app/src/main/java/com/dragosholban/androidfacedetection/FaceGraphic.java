package com.dragosholban.androidfacedetection;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

import java.io.ByteArrayOutputStream;

import static com.dragosholban.androidfacedetection.FaceDetectionActivity.rotateImage;


class FaceGraphic extends GraphicOverlay.Graphic{
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;
    public ImageView imageview3;
    public ImageView imageview4;
    public ImageView imageview5;
    public ImageView imageview6;
    public MyFaceDetector myFaceDetector;
    private static final int COLOR_CHOICES[] = {
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.MAGENTA,
            Color.RED,
            Color.WHITE,
            Color.YELLOW
    };
    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;

    private volatile Face mFace;
    private int mFaceId;

    FaceGraphic(GraphicOverlay overlay) {
        super(overlay);
        this.imageview3 = overlay.imageview3;
        this.imageview4 = overlay.imageview4;
        this.imageview5 = overlay.imageview5;
        this.imageview6 = overlay.imageview6;
        this.myFaceDetector = overlay.myFaceDetector;
        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(selectedColor);

        mIdPaint = new Paint();
        mIdPaint.setColor(selectedColor);
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    void setId(int id) {
        mFaceId = id;
    }


    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
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
    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }
        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);
        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);
        canvas.drawText("id: " + mFaceId, x + ID_X_OFFSET, y + ID_Y_OFFSET, mIdPaint);
        canvas.drawCircle(translateX(face.getPosition().x+3*face.getWidth()/10), translateY(face.getPosition().y+3*face.getHeight()/8), FACE_POSITION_RADIUS, mFacePositionPaint);
        canvas.drawCircle(translateX(face.getPosition().x+7*face.getWidth()/10), translateY(face.getPosition().y+3*face.getHeight()/8), FACE_POSITION_RADIUS, mFacePositionPaint);
        // Draws a bounding box around the face.
        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;
        canvas.drawRect(left, top, right, bottom, mBoxPaint);
        //My modification
        Frame frame = myFaceDetector.getmFrame();
        if(frame == null){
            return;
        }
        float width = frame.getMetadata().getWidth();
        float height = frame.getMetadata().getHeight();
        YuvImage yuvImage = new YuvImage(frame.getGrayscaleImageData().array(), ImageFormat.NV21, (int)width, (int)height, null);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, (int)width, (int)height), 100, byteArrayOutputStream);
        byte[] jpegArray = byteArrayOutputStream.toByteArray();
        Bitmap bmp = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.length);
        bmp = getResizedBitmap(bmp,(int)scaleX(height),(int)scaleY(width));
        //Do whatever you want with this cropped Bitmap
        Log.i("Tag", "Value: " + Float.toString(y-yOffset)+"  " + Float.toString(y+yOffset)+" "+ Float.toString(bmp.getHeight()));
        Log.i("Tag", "Value: " + Float.toString(x-xOffset)+"  " + Float.toString(x+xOffset)+" "+ Float.toString(bmp.getWidth()));
        width = scaleX(width);
        height = scaleY(height);
        Bitmap faceBitmap = Bitmap.createBitmap(bmp,(int)(x-xOffset > 0 ? (x-xOffset) : 0),(int)(y>yOffset ? (y-yOffset) : 0),(int)((x+xOffset) > height ? height-x+xOffset : (2*xOffset)),(int)(y+yOffset > width ? width-y+yOffset : (2*yOffset)));
        Log.i("TagF", "Value: " + Float.toString(y-yOffset)+"  " + Float.toString(y+yOffset)+" "+ Float.toString(faceBitmap.getHeight()));
        Log.i("TagF", "Value: " + Float.toString(x-xOffset)+"  " + Float.toString(x+xOffset)+" "+ Float.toString(faceBitmap.getWidth()));
        Bitmap rotatedbitmap = rotateImage(faceBitmap,270);
        imageview3.setImageBitmap(rotatedbitmap);
        Bitmap lefteyeBitmap = Bitmap.createBitmap(bmp,(int)translateY(face.getPosition().y+3*face.getHeight()/8)-25,(int)translateX(face.getPosition().x+3*face.getWidth()/10)-25,50,50);
        Bitmap righteyeBitmap = Bitmap.createBitmap(bmp,(int)translateY(face.getPosition().y+5*face.getHeight()/8)-25,(int)translateX(face.getPosition().x+3*face.getWidth()/10)-25,50,50);

        Bitmap rleb = rotateImage(lefteyeBitmap,270);
        Bitmap rreb = rotateImage(righteyeBitmap,270);
        imageview4.setImageBitmap(rleb);
        imageview5.setImageBitmap(rreb);
        // Uptil here
        // Draws a circle for each face feature detected
        for (Landmark landmark : face.getLandmarks()) {
            Log.i("Found Landmark","landmark.getType()");
            if(landmark.getType()==Landmark.RIGHT_EYE) {
                // the preview display of front-facing cameras is flipped horizontally
                float cx = canvas.getWidth() - scaleX(landmark.getPosition().x);
                float cy = scaleY(landmark.getPosition().y);
                canvas.drawCircle(cx, cy, 10, mIdPaint);
            }
            if(landmark.getType()==Landmark.LEFT_EYE) {
                // the preview display of front-facing cameras is flipped horizontally
                float cx = canvas.getWidth() - scaleX(landmark.getPosition().x);
                float cy = scaleY(landmark.getPosition().y);
                canvas.drawCircle(cx, cy, 10, mIdPaint);
            }
        }
    }

}
