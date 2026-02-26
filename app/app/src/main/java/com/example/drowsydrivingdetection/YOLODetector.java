package com.example.drowsydrivingdetection;

import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

//Class runs object detection using the YOLO model. Class handles image preprocessing & model execution
public class YOLODetector {
    private static final String TAG = "YOLODetector";

    private Interpreter interpreter;
    private List<String> classes;
    private int inputSize;
    private float[][][] outputArray;
    //array to store predictions

    public YOLODetector(Interpreter interpreter, List<String> classes, int inputSize){
        this.interpreter = interpreter;
        this.classes = classes;
        this.inputSize = inputSize;

        int[] outputShape = interpreter.getOutputTensor(0).shape();
        //get the shape from the model to assign the output array with the shape values

        Log.d(TAG, "Output shape: [" + outputShape[0] + ", " +
                outputShape[1] + ", " + outputShape[2] + "]");
        //get output shape from model

        if (outputShape.length == 3) {
            outputArray = new float[outputShape[0]][outputShape[1]][outputShape[2]];
        } else {
            Log.d(TAG, "unexpected output shape");
        }
    }

    public static class DetectionResult {
        public float[][][] rawOutput;
        public long inferenceTime;
        public int numDetections; //to count number of detections taking place with confidence of
        //over 0.5

        public DetectionResult(float[][][] rawOutput, long inferenceTime) {
            this.rawOutput = rawOutput;
            this.inferenceTime = inferenceTime; //time taken for inference (used to track model performance)
            this.numDetections = countDetections(rawOutput);

        }

        private int countDetections(float[][][] rawOutput) {
            int count = 0;

            if (rawOutput == null || rawOutput.length == 0) {
                return 0;
            }

            //nested for loop to get the total no. of detection > 0.5
            for (int i = 0; i < rawOutput[0].length; i++) {
                float maxScore = 0;

                for (int j = 4; j < rawOutput[0][i].length; j++) {
                    maxScore = Math.max(maxScore, rawOutput[0][i][j]);
                }

                if (maxScore > 0.5f) {
                    count++;
                }
            }
            return count;
        }

    }

    public DetectionResult detect(Bitmap bitmap) {
        long startTime = System.currentTimeMillis();

        //resizing bitmap to the model input size
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, this.inputSize, this.inputSize, true);
        ByteBuffer inputBuffer = bitmapToByteBuffer(resizedBitmap);

        this.interpreter.run(inputBuffer, this.outputArray);

        long inferenceTime = System.currentTimeMillis() - startTime;

        return new DetectionResult(this.outputArray, inferenceTime);

    }

    private ByteBuffer bitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize);

        int pixel = 0;
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                int val = intValues[pixel++];

                //extracting the RGB values (in order below Red then Green then Blue)
                byteBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f);
                byteBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);
                byteBuffer.putFloat((val & 0xFF) / 255.0f);
            }
        }

        return byteBuffer;
    }


}
