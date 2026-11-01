package com.signbridge.model;
import java.time.LocalDateTime;
public record Translation(int id, String text, LocalDateTime createdAt) {}
