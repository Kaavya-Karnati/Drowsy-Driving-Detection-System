package com.example.drowsydrivingdetection;

import android.content.Context;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.gpu.GpuDelegateFactory;

import java.nio.ByteBuffer;
import java.util.List;

public class ModelLoader {
    private static final String TAG = "ModelLoader";
    private Interpreter interpreter;
    private List<String> classes;
    private GpuDelegate gpuDelegate = null;
    private int imgSize = 640;

    public ModelLoader(Context context){
        try {
            Log.d(TAG, "loading model");

            ByteBuffer modelBuffer = loadModelFile(context.getAssets(), "best_float32.tflite");
            //TF lite requires a ByteBuffer to load models faster & avoid copying the model in RAM

            Interpreter.Options options = new Interpreter.Options(); //this object controls
            //threading delegates GPU and controls execution behavior

            CompatibilityList compatibilityList = new CompatibilityList();

            if (compatibilityList.isDelegateSupportedOnThisDevice()) {
                GpuDelegateFactory.Options delegateOptions = compatibilityList.getBestOptionsForThisDevice();
                this.gpuDelegate = new GpuDelegate(delegateOptions);
                options.addDelegate(this.gpuDelegate);
                Log.d(TAG, "GPU acceleration enabled");
            } else {
                options.setNumThreads(4); //CPU execution using 4 threads if GPU not present
            }

            // Create interpreter
            this.interpreter = new Interpreter(modelBuffer, options);

            // Load classes
            this.classes = loadClasses(context.getAssets(), "classes.txt");

            // Print model info
            printModelInfo();
        } catch (Exception err) {
            err.printStackTrace();
        }

    }
    
}
