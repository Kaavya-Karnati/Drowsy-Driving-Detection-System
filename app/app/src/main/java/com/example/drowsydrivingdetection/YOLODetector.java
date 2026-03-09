package com.example.drowsydrivingdetection;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

//Class runs object detection using the YOLO model. Class handles image preprocessing & model execution
public class YOLODetector {
    private static final String TAG = "YOLODetector";

    private Interpreter interpreter;
    private List<String> classes;
    private int inputSize;
    private float[][][] outputArray;
    //array to store predictions

    public YOLODetector(Interpreter interpreter, List<String> classes, int inputSize) {
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

        private int countDetections(float[][][] output) {
            int count = 0;
            if (output == null || output.length == 0) return 0;

            // Check output format
            int dim1 = output[0].length;     // Either 7 or 8400
            int dim2 = output[0][0].length;  // Either 8400 or 7

            boolean isTransposed = (dim1 < 100);  // If dim1 is small (7), it's transposed

            if (isTransposed) {
                // Format: [1, 7, 8400] - transposed
                int numDetections = dim2;  // 8400
                int numClasses = 3;        // close, open, yawn

                for (int i = 0; i < numDetections; i++) {
                    float maxScore = 0;
                    // Check class scores at indices [4][i], [5][i], [6][i]
                    for (int c = 0; c < numClasses; c++) {
                        float score = output[0][4 + c][i];
                        maxScore = Math.max(maxScore, score);
                    }
                    if (maxScore > 0.5f) {
                        count++;
                    }
                }
            } else {
                // Format: [1, 8400, 7] - standard
                int numDetections = dim1;  // 8400

                for (int i = 0; i < numDetections; i++) {
                    float maxScore = 0;
                    // Check class scores at indices [i][4], [i][5], [i][6]
                    for (int j = 4; j < Math.min(output[0][i].length, 7); j++) {
                        maxScore = Math.max(maxScore, output[0][i][j]);
                    }
                    if (maxScore > 0.5f) {
                        count++;
                    }
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

        logDetections(outputArray, 0.6f);

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

    private void logDetections(float[][][] output, float confidenceThreshold) {
        if (output == null || output.length == 0) {
            Log.d(TAG, "No output from model");
            return;
        }

        int dim1 = output[0].length;
        int dim2 = output[0][0].length;
        boolean isTransposed = (dim1 < 100);

        Log.d(TAG, "Model output shape: [1, " + dim1 + ", " + dim2 + "]");
        Log.d(TAG, "Format: " + (isTransposed ? "TRANSPOSED [1, 7, 8400]" : "STANDARD [1, 8400, 7]"));

        int detectionCount = 0;

        if (isTransposed) {
            // Transposed: [1, 7, 8400]
            int numDetections = dim2;

            for (int i = 0; i < numDetections; i++) {
                float cx = output[0][0][i];
                float cy = output[0][1][i];
                float w = output[0][2][i];
                float h = output[0][3][i];

                float maxScore = 0;
                int maxClassIndex = -1;

                for (int j = 0; j < classes.size(); j++) {
                    float score = output[0][4 + j][i];
                    if (score > maxScore) {
                        maxScore = score;
                        maxClassIndex = j;
                    }
                }

                if (maxScore > confidenceThreshold) {
                    detectionCount++;
                    String className = classes.get(maxClassIndex);
                    Log.d(TAG, String.format(
                            "Detection #%d: Class='%s' (%.3f) || BBox=[cx:%.1f, cy:%.1f, w:%.1f, h:%.1f]",
                            detectionCount, className, maxScore, cx, cy, w, h
                    ));
                }
            }
        } else {
            // Standard: [1, 8400, 7]
            int numDetections = dim1;

            for (int i = 0; i < numDetections; i++) {
                float[] prediction = output[0][i];

                float cx = prediction[0];
                float cy = prediction[1];
                float w = prediction[2];
                float h = prediction[3];

                float maxScore = 0;
                int maxClassIndex = -1;

                for (int j = 4; j < Math.min(prediction.length, 4 + classes.size()); j++) {
                    if (prediction[j] > maxScore) {
                        maxScore = prediction[j];
                        maxClassIndex = j - 4;
                    }
                }

                if (maxScore > confidenceThreshold) {
                    detectionCount++;
                    String className = (maxClassIndex >= 0 && maxClassIndex < classes.size())
                            ? classes.get(maxClassIndex)
                            : "unknown";

                    Log.d(TAG, String.format(
                            "Detection #%d: Class='%s' (%.3f) || BBox=[cx:%.1f, cy:%.1f, w:%.1f, h:%.1f]",
                            detectionCount, className, maxScore, cx, cy, w, h
                    ));
                }
            }
        }

        if (detectionCount == 0) {
            Log.d(TAG, "No detections above threshold " + confidenceThreshold);
        } else {
            Log.d(TAG, "Total detections: " + detectionCount);
        }
    }

    public CleanDetectionResult detectAndParse(Bitmap bitmap) {
        //run existing detection
        DetectionResult rawResult = detect(bitmap);

        //instantiate the cleaner result class
        CleanDetectionResult cleanResult = new CleanDetectionResult();

        //parse the raw output into bounding boxes (new model class)
        if (rawResult != null && rawResult.rawOutput != null) {
            List<BoundingBox> boxes = parseDetections(
                    rawResult.rawOutput,
                    bitmap.getWidth(),
                    bitmap.getHeight()
            );
            cleanResult.detections = boxes;
        }

        // Count detections by class
        cleanResult.countDetections();
        Log.e("Clean Result: ", cleanResult.toString());

        return cleanResult;
    }

    private List<BoundingBox> parseDetections(float[][][] rawOutput, int imageWidth, int imageHeight) {
        List<BoundingBox> boxes = new ArrayList<>();
        float confThreshold = 0.65f; //tentative, for testing purposes, will be higher

        if (rawOutput == null || rawOutput.length == 0) {
            Log.d(TAG, "No output from model");
            return boxes;
        }

        int dim1 = rawOutput[0].length;
        int dim2 = rawOutput[0][0].length;
        boolean isTransposed = (dim1 < 100);

        Log.d(TAG, "Parsing detections: Shape: [1, " + dim1 + ", " + dim2 + "]");
        Log.d(TAG, "Format: " + (isTransposed ? "TRANSPOSED [1, 7, 8400]" : "STANDARD [1, 8400, 7]"));

        if (isTransposed) {
            parseTransposedFormat(rawOutput, boxes, confThreshold, imageWidth, imageHeight, dim2);
        } else {
            parseStandardFormat(rawOutput, boxes, confThreshold, imageWidth, imageHeight, dim1);
        }

        Log.d(TAG, "Total parsed detections: " + boxes.size());
        return boxes;
    }

    private void parseTransposedFormat(float[][][] output, List<BoundingBox> boxes,
                                       float confThreshold, int imageWidth, int imageHeight,
                                       int numDetections) {
        for (int i = 0; i < numDetections; i++) {
            // Extract bbox coordinates
            float cx = output[0][0][i];
            float cy = output[0][1][i];
            float w = output[0][2][i];
            float h = output[0][3][i];

            // Find max class score
            float maxScore = 0;
            int maxClassIndex = -1;

            for (int j = 0; j < classes.size(); j++) {
                float score = output[0][4 + j][i];
                if (score > maxScore) {
                    maxScore = score;
                    maxClassIndex = j;
                }
            }

            // Filter by confidence and create BoundingBox
            if (maxScore > confThreshold && maxClassIndex >= 0 && maxClassIndex < classes.size()) {
                RectF bbox = convertToCornerFormat(cx, cy, w, h, imageWidth, imageHeight);
                String label = classes.get(maxClassIndex);
                boxes.add(new BoundingBox(bbox, label, maxScore));

                Log.d(TAG, String.format(
                        "Detection: Class='%s' (%.3f) || BBox=[cx:%.1f, cy:%.1f, w:%.1f, h:%.1f]",
                        label, maxScore, cx, cy, w, h
                ));
            }
        }
    }

    private void parseStandardFormat(float[][][] output, List<BoundingBox> boxes,
                                     float confThreshold, int imageWidth, int imageHeight,
                                     int numDetections) {
        for (int i = 0; i < numDetections; i++) {
            float[] prediction = output[0][i];

            //extract bbox coordinates
            float cx = prediction[0];
            float cy = prediction[1];
            float w = prediction[2];
            float h = prediction[3];

            //find max class score
            float maxScore = 0;
            int maxClassIndex = -1;

            for (int j = 4; j < Math.min(prediction.length, 4 + classes.size()); j++) {
                if (prediction[j] > maxScore) {
                    maxScore = prediction[j];
                    maxClassIndex = j - 4;
                }
            }

            //filter by confidence and create BoundingBox
            if (maxScore > confThreshold && maxClassIndex >= 0 && maxClassIndex < classes.size()) {
                RectF bbox = convertToCornerFormat(cx, cy, w, h, imageWidth, imageHeight);
                String label = classes.get(maxClassIndex);
                boxes.add(new BoundingBox(bbox, label, maxScore));

                Log.d(TAG, String.format(
                        "Detection: Class='%s' (%.3f) || BBox=[cx:%.1f, cy:%.1f, w:%.1f, h:%.1f]",
                        label, maxScore, cx, cy, w, h
                ));
            }
        }
    }

    private RectF convertToCornerFormat(float cx, float cy, float w, float h,
                                        int imageWidth, int imageHeight) {
        //scale from model input size to actual image size
        float scaleX = (float) imageWidth / inputSize;
        float scaleY = (float) imageHeight / inputSize;

        //convert center coordinates to corner coordinates
        float x1 = (cx - w / 2) * scaleX;
        float y1 = (cy - h / 2) * scaleY;
        float x2 = (cx + w / 2) * scaleX;
        float y2 = (cy + h / 2) * scaleY;

        //clamp to image bounds
        x1 = Math.max(0, Math.min(x1, imageWidth));
        y1 = Math.max(0, Math.min(y1, imageHeight));
        x2 = Math.max(0, Math.min(x2, imageWidth));
        y2 = Math.max(0, Math.min(y2, imageHeight));

        return new RectF(x1, y1, x2, y2);
    }

}
