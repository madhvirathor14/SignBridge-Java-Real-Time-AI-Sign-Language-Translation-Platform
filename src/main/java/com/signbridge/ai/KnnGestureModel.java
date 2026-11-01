package com.signbridge.ai;
import org.opencv.core.*;
import org.opencv.ml.KNearest;
import org.opencv.ml.Ml;
import org.opencv.imgcodecs.Imgcodecs;
import java.nio.file.*;
import java.io.*;
import java.util.*;
public class KnnGestureModel {
  private final Path modelPath = Path.of("models/signbridge-knn.yml");
  private final Path labelsPath = Path.of("models/labels.txt");
  private KNearest model;
  private List<String> labels = new ArrayList<>();

  public int train(Path datasetRoot) throws IOException {
    List<float[]> rows = new ArrayList<>(); List<Integer> targets = new ArrayList<>();
    List<String> names;
    try (var ds = Files.list(datasetRoot)) { names = ds.filter(Files::isDirectory).map(p -> p.getFileName().toString()).sorted().toList(); }
    List<String> trainedLabels = new ArrayList<>();
    for (String name : names) {
      Path dir = datasetRoot.resolve(name);
      List<Path> images;
      try (var fs = Files.list(dir)) { images = fs.filter(p -> p.toString().toLowerCase().endsWith(".png")).toList(); }
      if (images.size() < 2) continue;
      int classId = trainedLabels.size(); trainedLabels.add(name);
      for (Path image : images) {
        Mat img = Imgcodecs.imread(image.toString());
        if (img.empty()) { img.release(); continue; }
        MatOfFloat desc = FeatureExtractor.extract(img);
        rows.add(desc.toArray()); targets.add(classId);
        img.release(); desc.release();
      }
    }
    if (trainedLabels.size() < 2 || rows.size() < 4) throw new IOException("Capture at least 2 labels with 2 or more images each.");
    int cols = rows.get(0).length;
    Mat samples = new Mat(rows.size(), cols, CvType.CV_32F);
    Mat responses = new Mat(rows.size(), 1, CvType.CV_32F);
    for (int r=0;r<rows.size();r++) {
      samples.put(r,0,rows.get(r));
      responses.put(r,0,new float[]{targets.get(r)});
    }
    model = KNearest.create(); model.setDefaultK(3); model.setIsClassifier(true);
    boolean trained = model.train(samples, Ml.ROW_SAMPLE, responses);
    samples.release(); responses.release();
    if (!trained) throw new IOException("OpenCV KNN training failed.");
    Files.createDirectories(modelPath.getParent());
    model.save(modelPath.toString());
    Files.write(labelsPath, trainedLabels);
    labels = trainedLabels;
    return rows.size();
  }
  public boolean load() throws IOException {
    if (!Files.exists(modelPath) || !Files.exists(labelsPath)) return false;
    model = KNearest.load(modelPath.toString()); labels = Files.readAllLines(labelsPath);
    return model != null && !labels.isEmpty();
  }
  public Prediction predict(Mat frame) throws IOException {
    if (model == null && !load()) throw new IOException("Train the model first.");
    MatOfFloat d = FeatureExtractor.extract(frame);
    float[] values = d.toArray(); d.release();
    Mat sample = new Mat(1, values.length, CvType.CV_32F); sample.put(0,0,values);
    Mat results = new Mat(), neighbours = new Mat(), distances = new Mat();
    float predicted = model.findNearest(sample, 3, results, neighbours, distances);
    double avgDistance = 0;
    if (!distances.empty()) {
      float[] ds = new float[(int)(distances.total()*distances.channels())];
      distances.get(0,0,ds); for(float v:ds) avgDistance += v;
      if (ds.length > 0) avgDistance /= ds.length;
    }
    int id = Math.round(predicted);
    String label = id >= 0 && id < labels.size() ? labels.get(id) : "UNKNOWN";
    sample.release(); results.release(); neighbours.release(); distances.release();
    // KNN distances are not calibrated probabilities. Display a similarity indicator, not accuracy.
    double similarity = 100.0 / (1.0 + Math.sqrt(Math.max(0,avgDistance))/18.0);
    return new Prediction(label, Math.min(99, similarity), avgDistance);
  }
  public record Prediction(String label, double similarity, double distance) {}
  public boolean isLoaded() { return model != null; }
}
