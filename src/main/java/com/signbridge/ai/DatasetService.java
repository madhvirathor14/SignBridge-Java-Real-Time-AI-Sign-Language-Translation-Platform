package com.signbridge.ai;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
public class DatasetService {
  private final Path root = Path.of("dataset");
  public DatasetService() { try { Files.createDirectories(root); } catch(IOException e) { throw new RuntimeException(e); } }
  public int saveBurstFrame(String label, Mat frame) throws IOException {
    Path dir = root.resolve(label); Files.createDirectories(dir);
    long count;
    try (var s = Files.list(dir)) { count = s.filter(p -> p.toString().endsWith(".png")).count(); }
    Mat crop = FeatureExtractor.centerCrop(frame);
    Path out = dir.resolve(String.format(Locale.ROOT, "%05d.png", count+1));
    boolean ok = Imgcodecs.imwrite(out.toString(), crop); crop.release();
    if (!ok) throw new IOException("Could not write dataset image: " + out);
    return (int)count+1;
  }
  public Map<String,Integer> counts() throws IOException {
    Map<String,Integer> map = new TreeMap<>();
    if (!Files.exists(root)) return map;
    try (var dirs = Files.list(root)) {
      for (Path d : dirs.filter(Files::isDirectory).toList()) {
        try (var imgs = Files.list(d)) { map.put(d.getFileName().toString(),
          (int)imgs.filter(p -> p.toString().toLowerCase().endsWith(".png")).count()); }
      }
    }
    return map;
  }
  public Path root() { return root; }
}
