package com.example.drowsydrivingdetection.core;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;

public class ImageProcessing {
    // Convert Mat to byteBuffer (https://stackoverflow.com/questions/25652213/java-opencv-mat-object-to-bytebuffer)
    public static void matToByteBuffer(Mat mat, ByteBuffer buffer, byte[] matToByteBufferArray) {
        // Rewinds buffer to beginning before writing
        buffer.rewind();

        mat.get(0, 0, matToByteBufferArray);

        // Converts to float and places in buffer
        for (int i = 0; i < matToByteBufferArray.length; i++) {
            buffer.putFloat((matToByteBufferArray[i] & 0xFF) / 255.f);
        }
    }

    public static void resizeImageConvertColor(Mat rgba, Mat newMat, int imageW, int imageH) {
        // Resizes image but uses INTER_LINEAR because it keeps image quality when scaling
        Imgproc.resize(rgba, newMat, new Size(imageW, imageH), 0, 0, Imgproc.INTER_LINEAR);

        // Since we're using mat -> ByteBuffer now, we need to change RGBA -> RGB since it's expecting 3 channels and OpenCV is giving us 4
        Imgproc.cvtColor(newMat, newMat, Imgproc.COLOR_RGBA2RGB);

        // Rotate 90 degrees for better model performance
        //Core.rotate(resizedRGBA, resizedRGBA, Core.ROTATE_90_CLOCKWISE);
        // This cut FPS from ~15 to 7.9, I don't know if it's a good idea to keep it -Anthony

    }
}
