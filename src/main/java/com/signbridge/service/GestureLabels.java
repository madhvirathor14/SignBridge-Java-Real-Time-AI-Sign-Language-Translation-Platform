package com.signbridge.service;
import java.util.List;
public final class GestureLabels {
  private GestureLabels() {}
  public static final List<String> LABELS = List.of("HELLO","THANK_YOU","YES","NO","HELP","PLEASE","STOP","WATER","OTHER");
  public static String display(String s) { return s.replace('_',' '); }
}
