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
        bmp = getResizedBitmap(bmp,(int)scaleX(width),(int)scaleY(height));
        bmp = rotateImage(bmp,270);
        //Do whatever you want with this cropped Bitmap
        Log.i("Tag", "Value: " + Float.toString(y-yOffset)+"  " + Float.toString(y+yOffset)+" "+ Float.toString(bmp.getHeight()));
        Log.i("Tag", "Value: " + Float.toString(x-xOffset)+"  " + Float.toString(x+xOffset)+" "+ Float.toString(bmp.getWidth()));
        width = scaleX(width);
        height = scaleY(height);
        float newx = scaleX(face.getPosition().x + face.getWidth() / 2);
        Bitmap faceBitmap = Bitmap.createBitmap(bmp,(int)(newx-xOffset > 0 ? (newx-xOffset) : 0),(int)(y>yOffset ? (y-yOffset) : 0),(int)((newx+xOffset) > width ? width-newx+xOffset : (2*xOffset)),(int)(y+yOffset > height ? height-y+yOffset : (2*yOffset)));
        Log.i("TagF", "Value: " + Float.toString(y-yOffset)+"  " + Float.toString(y+yOffset)+" "+ Float.toString(faceBitmap.getHeight()));
        Log.i("TagF", "Value: " + Float.toString(x-xOffset)+"  " + Float.toString(x+xOffset)+" "+ Float.toString(faceBitmap.getWidth()));
        imageview3.setImageBitmap(faceBitmap);
        int lex = (int)scaleX(face.getPosition().x+3*face.getWidth()/10 - 25);
        int ley = (int)translateY(face.getPosition().y+3*face.getHeight()/8 -25);
        int rex = (int)scaleX(face.getPosition().x+7*face.getWidth()/10 -25);
        int rey = (int)translateY(face.getPosition().y+3*face.getHeight()/8 -25);
        float x50 = scaleX(50);
        float y50 = scaleY(50);
        Bitmap lefteyeBitmap = Bitmap.createBitmap(bmp,(lex > 0 ? lex : 0),(ley>0 ? ley : 0),(int)(lex+x50 > width ? width-lex : x50),(int)(ley + y50 > height ? height-ley : y50));
        Bitmap righteyeBitmap = Bitmap.createBitmap(bmp,(rex > 0 ? rex : 0),(rey>0 ? rey : 0),(int)(rex+x50 > width ? width-rex : x50),(int)(rey + y50 > height ? height-rey : y50));
        imageview4.setImageBitmap(lefteyeBitmap);
        imageview5.setImageBitmap(righteyeBitmap);
        int wfm = imageview6.getWidth();
        int hfm = imageview6.getHeight();
        float srx = (newx-xOffset > 0 ? (newx-xOffset) : 0)/width;
        float brx = ((newx+xOffset) > width ? width : (newx+xOffset))/width;
        float sry = (y-yOffset > 0 ? (y-yOffset) : 0)/height;
        float bry = ((y+yOffset) > height ? height : (y+yOffset))/height;
        Bitmap compare = Bitmap.createBitmap(wfm, hfm, Bitmap.Config.ARGB_8888);
        for(int x1=0;x1<wfm;x1++){
            for(int y1=0;y1<hfm;y1++){
                double r1 = (double)x1*1.0/wfm;
                double r2 = (double)y1*1.0/hfm;
                if(r1 > srx && r1 < brx && r2 > sry && r2< bry){
                    compare.setPixel(x1,y1,Color.BLACK);
                }
                else{
                    compare.setPixel(x1,y1,Color.WHITE);
                }
            }
        }
        imageview6.setImageBitmap(compare);
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
