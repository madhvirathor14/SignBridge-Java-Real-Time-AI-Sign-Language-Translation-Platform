package com.signbridge.service;
import java.io.IOException;
public class SpeechService {
  public void speak(String text) throws IOException {
    if(text == null || text.isBlank()) return;
    String os=System.getProperty("os.name").toLowerCase(); ProcessBuilder pb;
    if(os.contains("win")) {
      String safe=text.replace("'","''");
      pb=new ProcessBuilder("powershell","-NoProfile","-Command","Add-Type -AssemblyName System.Speech; $s=New-Object System.Speech.Synthesis.SpeechSynthesizer; $s.Speak('"+safe+"');");
    } else if(os.contains("mac")) pb=new ProcessBuilder("say",text);
    else pb=new ProcessBuilder("espeak",text);
    pb.redirectErrorStream(true); pb.start();
  }
}
