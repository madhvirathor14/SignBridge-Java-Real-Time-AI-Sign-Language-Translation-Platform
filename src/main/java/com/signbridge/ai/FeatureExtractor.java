
package com.signbridge.ai;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

public final class FeatureExtractor {

    private static final Size WINDOW = new Size(64, 64);

    private static final HOGDescriptor HOG =
            new HOGDescriptor(
                    WINDOW,
                    new Size(16, 16),
                    new Size(8, 8),
                    new Size(8, 8),
                    9
            );

    private FeatureExtractor() {}

    public static MatOfFloat extract(Mat source) {

        Mat crop = centerCrop(source);
        Mat gray = new Mat();
        Mat resized = new Mat();

        Imgproc.cvtColor(crop, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.resize(gray, resized, WINDOW);
        Imgproc.equalizeHist(resized, resized);

        MatOfFloat descriptor = new MatOfFloat();
        HOG.compute(resized, descriptor);

        crop.release();
        gray.release();
        resized.release();

        return descriptor;
    }

    public static Mat centerCrop(Mat src) {

        int side = Math.min(src.cols(), src.rows());

        int x = (src.cols() - side) / 2;
        int y = (src.rows() - side) / 2;

        Mat roi = new Mat(src, new Rect(x, y, side, side));

        Mat copy = roi.clone();

        roi.release();

        return copy;
    }
}
