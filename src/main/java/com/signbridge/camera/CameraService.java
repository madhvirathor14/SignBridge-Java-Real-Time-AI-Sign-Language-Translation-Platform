package com.signbridge.camera;
import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.core.MatOfByte;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
public class CameraService {
  static { OpenCV.loadLocally(); }
  private VideoCapture capture;
  public synchronized boolean start(int index) {
    stop(); capture = new VideoCapture(index);
    if (capture.isOpened()) {
      capture.set(Videoio.CAP_PROP_FRAME_WIDTH, 640);
      capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);
      return true;
    }
    stop(); return false;
  }
  public synchronized Mat readMat() {
    if (capture == null || !capture.isOpened()) return null;
    Mat frame = new Mat();
    if (!capture.read(frame) || frame.empty()) { frame.release(); return null; }
    return frame;
  }
  public BufferedImage toBufferedImage(Mat frame) {
    if (frame == null || frame.empty()) return null;
    MatOfByte bytes = new MatOfByte(); Imgcodecs.imencode(".jpg", frame, bytes);
    try { return ImageIO.read(new ByteArrayInputStream(bytes.toArray())); }
    catch (Exception e) { return null; }
    finally { bytes.release(); }
  }
  public synchronized boolean isRunning() { return capture != null && capture.isOpened(); }
  public synchronized void stop() { if (capture != null) { capture.release(); capture = null; } }
}
