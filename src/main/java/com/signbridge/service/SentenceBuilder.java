package com.signbridge.service;
import java.util.*;
public class SentenceBuilder {
  private final List<String> words = new ArrayList<>();
  public void add(String word) { if(word != null && !word.isBlank() && !word.equals("OTHER") && !word.equals("UNKNOWN")) words.add(GestureLabels.display(word)); }
  public void undo() { if(!words.isEmpty()) words.remove(words.size()-1); }
  public void clear() { words.clear(); }
  public String build() { return String.join(" ", words); }
}
