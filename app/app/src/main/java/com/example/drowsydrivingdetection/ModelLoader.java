package com.example.drowsydrivingdetection;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.gpu.GpuDelegateFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class ModelLoader {
    private Interpreter interpreter;
    private List<String> classes;
    private GpuDelegate gpuDelegate = null;
    private int imgSize = 640;

    public ModelLoader(Context context) {
        try {
            Log.d("Model Loader: ", "loading model");

            ByteBuffer modelBuffer = loadModelFile(context.getAssets(), "best_float32_75.tflite");
            //TF lite requires a ByteBuffer to load models faster & avoid copying the model in RAM

            Interpreter.Options options = new Interpreter.Options();
            // Configure interpreter options (GPU/CPU execution)

            CompatibilityList compatibilityList = new CompatibilityList();

            if (compatibilityList.isDelegateSupportedOnThisDevice()) {
                GpuDelegateFactory.Options delegateOptions = compatibilityList.getBestOptionsForThisDevice();
                this.gpuDelegate = new GpuDelegate(delegateOptions);
                options.addDelegate(this.gpuDelegate);
                Log.d("Model Loader: ", "GPU acceleration enabled");
            } else {
                options.setNumThreads(4); //CPU execution using 4 threads if GPU not present
            }

            // Initialize TFLite interpreter with configured options
            this.interpreter = new Interpreter(modelBuffer, options);
            this.classes = loadClasses(context.getAssets(), "classes_75.txt");

            printModelInfo();

        } catch (Exception err) {
            err.printStackTrace();
            Log.e("Model Loader: ", "Error loading model: " + err.getMessage());
        }

    }

    public boolean isLoaded() {
        return this.interpreter != null;
    }

    public int getInputSize() {
        return this.imgSize;
    }

    public List<String> getLabels() {
        return this.classes;
    }

    public Interpreter getInterpreter() {
        return this.interpreter;
    }

    public void close() {
        if (this.interpreter != null) {
            this.interpreter.close();
        }

        if (this.gpuDelegate != null) {
            this.gpuDelegate.close();
        }

        Log.d("Model Loader: ", "Model resources released");
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        // Load the model from assets into a memory-mapped buffer for fast inference
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadClasses(AssetManager assetManager, String classPath) throws IOException {
        //read the models classes line by line since index matters for YOLO models
        List<String> classes = new ArrayList<>();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(assetManager.open(classPath))
        );

        String line;

        while ((line = reader.readLine()) != null) {
            classes.add(line);
        }

        reader.close();

        return classes;

    }

    private void printModelInfo() {
        if (this.interpreter == null) {
            return;
        }

        int[] inputShape = interpreter.getInputTensor(0).shape();//batch size, channel count
        Log.d("Model Loader: ", "Input shape: [" + inputShape[0] + ", " + inputShape[1] +
                ", " + inputShape[2] + ", " + inputShape[3] + "]");

        // Output tensor info
        int[] outputShape = interpreter.getOutputTensor(0).shape();
        Log.d("Model Loader: ", "Output shape: [" + outputShape[0] + ", " + outputShape[1] +
                ", " + outputShape[2] + "]");

        // Update input size from model
        if (inputShape.length >= 4) {
            imgSize = inputShape[1];
            Log.d("Model Loader: ", "Model input size: " + imgSize);
        }
    }

}
