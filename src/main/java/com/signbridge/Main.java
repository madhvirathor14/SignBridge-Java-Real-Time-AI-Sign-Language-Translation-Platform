package com.signbridge;
import com.signbridge.data.HistoryRepository;
import com.signbridge.ui.SignBridgeFrame;
import javax.swing.*;
public class Main {
  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      try { HistoryRepository.init(); new SignBridgeFrame().setVisible(true); }
      catch (Exception e) { e.printStackTrace(); JOptionPane.showMessageDialog(null, "Startup failed: " + e.getMessage()); }
    });
  }
}
