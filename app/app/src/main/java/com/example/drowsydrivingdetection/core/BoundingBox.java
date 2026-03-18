package com.example.drowsydrivingdetection.core;

import android.annotation.SuppressLint;
import android.graphics.RectF;

import androidx.annotation.NonNull;

//Stores the bounding box coordinates, predicted label (open/closed/yawn), confidence score, and timestamp of when the detection occurred

public class BoundingBox {
    public RectF boundingBox;
    public String label;
    public float confidence;
    public long timestamp;

    public BoundingBox(RectF boundingBox, String label, float confidence) {
        this.boundingBox = boundingBox;
        this.label = label;
        this.confidence = confidence;
        this.timestamp = System.currentTimeMillis();
    }

    // Convert the detection to a simple string
    @NonNull
    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("%s (%.2f)", this.label, this.confidence);
    }
}
