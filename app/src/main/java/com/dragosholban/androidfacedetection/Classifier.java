package com.dragosholban.androidfacedetection;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class Classifier {

    private static final String LOG_TAG = Classifier.class.getSimpleName();

    // Name of the model file (under assets folder)
    private static final String MODEL_PATH = "detect.tflite";

    // TensorFlow Lite interpreter for running inference with the tflite model
    private final Interpreter interpreter;

    /* Input */
    // A ByteBuffer to hold image data for input to model
    private final ByteBuffer inputImage1;
    private final ByteBuffer inputImage2;
    private final ByteBuffer inputImage3;
    private final ByteBuffer inputImage4;

    private final int[] imagePixels1 = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
    private final int[] imagePixels2 = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
    private final int[] imagePixels3 = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];
    private final int[] imagePixels4 = new int[DIM_FLAT_SIZE*DIM_FLAT_SIZE];

    // Input size
    private static final int DIM_BATCH_SIZE = 1;
    public static final int DIM_IMG_SIZE_X = 224;
    public static final int DIM_IMG_SIZE_Y = 224;
    private static final int DIM_PIXEL_SIZE = 3;
    private static final int DIM_FLAT_MASK = 625;
    public static final int DIM_FLAT_SIZE = 25;
    /* Output*/
    private static final int DIMEN = 2;

    private float[][] outputArray = new float[DIM_BATCH_SIZE][DIMEN];

    public Classifier(Activity activity) throws IOException {
        interpreter = new Interpreter(loadModelFile(activity));
        inputImage1 =
                ByteBuffer.allocateDirect(4
                        * DIM_BATCH_SIZE
                        * DIM_IMG_SIZE_X
                        * DIM_IMG_SIZE_Y
                        * DIM_PIXEL_SIZE);
        inputImage2 =
                ByteBuffer.allocateDirect(4
                        * DIM_BATCH_SIZE
                        * DIM_IMG_SIZE_X
                        * DIM_IMG_SIZE_Y
                        * DIM_PIXEL_SIZE);
        inputImage3 =
                ByteBuffer.allocateDirect(4
                        * DIM_BATCH_SIZE
                        * DIM_IMG_SIZE_X
                        * DIM_IMG_SIZE_Y
                        * DIM_PIXEL_SIZE);
        inputImage4 = ByteBuffer.allocateDirect(4*DIM_BATCH_SIZE*DIM_FLAT_MASK);
        inputImage1.order(ByteOrder.nativeOrder());
        inputImage2.order(ByteOrder.nativeOrder());
        inputImage3.order(ByteOrder.nativeOrder());
        inputImage4.order(ByteOrder.nativeOrder());
    }

    // Memory-map the model file in Assets
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * To classify an image, follow these steps:
     * 1. pre-process the input image
     * 2. run inference with the model
     * 3. post-process the output result for display in UI
     *
     * @param bitmap1
     * @return the digit with the highest probability
     */
    public float[][] classify(Bitmap bitmap1,Bitmap bitmap2,Bitmap bitmap3,Bitmap bitmap4) {
        preprocess(bitmap1,bitmap2,bitmap3,bitmap4);
        Log.i(LOG_TAG,"Classifier preprocessing has been done");
        runInference();
        return postprocess();
    }

    /**
     * Preprocess the bitmap by converting it to ByteBuffer & grayscale
     *
     * @param bitmap1
     */
    private void preprocess(Bitmap bitmap1,Bitmap bitmap2,Bitmap bitmap3,Bitmap bitmap4) {
        convertBitmapToByteBuffer(bitmap1,bitmap2,bitmap3,bitmap4);
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap1,Bitmap bitmap2,Bitmap bitmap3,Bitmap bitmap4) {
        if (inputImage1 == null) {
            return;
        }
        inputImage1.rewind();

        bitmap1.getPixels(imagePixels1, 0, bitmap1.getWidth(), 0, 0,
                bitmap1.getWidth(), bitmap1.getHeight());
        bitmap2.getPixels(imagePixels2, 0, bitmap2.getWidth(), 0, 0,
                bitmap2.getWidth(), bitmap2.getHeight());
        bitmap3.getPixels(imagePixels3, 0, bitmap3.getWidth(), 0, 0,
                bitmap3.getWidth(), bitmap3.getHeight());
        bitmap4.getPixels(imagePixels4, 0, bitmap4.getWidth(), 0, 0,
                bitmap4.getWidth(), bitmap4.getHeight());

        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = imagePixels1[pixel++];
                inputImage1.putFloat(convertToColorScale(val)[0]);
                inputImage1.putFloat(convertToColorScale(val)[1]);
                inputImage1.putFloat(convertToColorScale(val)[2]);
            }
        }
        pixel=0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = imagePixels2[pixel++];
                inputImage2.putFloat(convertToColorScale(val)[0]);
                inputImage2.putFloat(convertToColorScale(val)[1]);
                inputImage2.putFloat(convertToColorScale(val)[2]);
            }
        }
        pixel=0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = imagePixels3[pixel++];
                inputImage3.putFloat(convertToColorScale(val)[0]);
                inputImage3.putFloat(convertToColorScale(val)[1]);
                inputImage3.putFloat(convertToColorScale(val)[2]);
            }
        }
        pixel=0;
        for (int i = 0; i < DIM_FLAT_SIZE; ++i) {
            for (int j = 0; j < DIM_FLAT_SIZE; ++j) {
                final int val = imagePixels4[pixel++];
                inputImage4.putFloat(convertToGrayScale(val));
            }
        }
    }

    private float[] convertToColorScale(int color) {
        float r = ((color >> 16) & 0xFF);
        float g = ((color >> 8) & 0xFF);
        float b = ((color) & 0xFF);
        float returnarray[] = new float[]{r/255,g/255,b/255};
        return returnarray;
    }
    private float convertToGrayScale(int color) {
        float r = ((color >> 16) & 0xFF);
        float g = ((color >> 8) & 0xFF);
        float b = ((color) & 0xFF);
        int grayscaleValue = (int) (0.299f * r + 0.587f * g + 0.114f * b);
        float preprocessedValue = grayscaleValue / 255.0f; // normalize the value by dividing by 255.0f
        return preprocessedValue;
    }

    /**
     * Run inference with the classifier model
     * Input is image
     * Output is an array of probabilities
     */
    private void runInference() {
        Object[] inputs = {inputImage1,inputImage2,inputImage3,inputImage4};
        Map<Integer, Object> map_of_indices_to_outputs = new HashMap<>();
        map_of_indices_to_outputs.put(0,outputArray);
        long startTime = System.currentTimeMillis();
        interpreter.runForMultipleInputsOutputs(inputs, map_of_indices_to_outputs);
        long endTime = System.currentTimeMillis();
        Log.i("ADebugTag", "Value: " + Long.toString(endTime-startTime));
    }

    /**
     * Figure out the prediction of digit by finding the index with the highest probability
     *
     * @return
     */
    private float[][] postprocess() {
        // Index with highest probability
        inputImage1.clear();
        inputImage2.clear();
        inputImage3.clear();
        inputImage4.clear();
        Log.i(LOG_TAG,"Classifier postprocessing done");
        return outputArray;
    }

}