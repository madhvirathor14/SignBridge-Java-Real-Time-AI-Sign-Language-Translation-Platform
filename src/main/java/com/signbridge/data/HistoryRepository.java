package com.signbridge.data;
import com.signbridge.model.Translation;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
public final class HistoryRepository {
  private static final String URL="jdbc:sqlite:data/signbridge.db";
  private HistoryRepository(){}
  public static void init() throws Exception {
    Files.createDirectories(Path.of("data"));
    try(Connection c=DriverManager.getConnection(URL); Statement s=c.createStatement()) {
      s.executeUpdate("CREATE TABLE IF NOT EXISTS translations(id INTEGER PRIMARY KEY AUTOINCREMENT,text TEXT NOT NULL,created_at TEXT NOT NULL)");
    }
  }
  public static void save(String text) throws SQLException {
    try(Connection c=DriverManager.getConnection(URL); PreparedStatement p=c.prepareStatement("INSERT INTO translations(text,created_at) VALUES(?,?)")) {
      p.setString(1,text); p.setString(2,LocalDateTime.now().toString()); p.executeUpdate();
    }
  }
  public static List<Translation> latest(int limit) throws SQLException {
    List<Translation> out=new ArrayList<>();
    try(Connection c=DriverManager.getConnection(URL); PreparedStatement p=c.prepareStatement("SELECT id,text,created_at FROM translations ORDER BY id DESC LIMIT ?")) {
      p.setInt(1,limit); try(ResultSet r=p.executeQuery()) { while(r.next()) out.add(new Translation(r.getInt(1),r.getString(2),LocalDateTime.parse(r.getString(3)))); }
    } return out;
  }
}
