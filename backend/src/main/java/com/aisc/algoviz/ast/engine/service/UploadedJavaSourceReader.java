package com.aisc.algoviz.ast.engine.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Validates and decodes one user-supplied Java source file for AST and debug endpoints. */
@Component
public class UploadedJavaSourceReader {

  private static final long MAX_SOURCE_BYTES = 200_000;

  public String read(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("A non-empty .java file is required.");
    }
    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".java")) {
      throw new IllegalArgumentException("Only .java source files are supported.");
    }
    if (file.getSize() > MAX_SOURCE_BYTES) {
      throw new IllegalArgumentException("The uploaded source exceeds the 200,000 byte limit.");
    }

    try {
      String source = new String(file.getBytes(), StandardCharsets.UTF_8);
      if (source.isBlank()) {
        throw new IllegalArgumentException("The uploaded .java file must not be blank.");
      }
      return source;
    } catch (IOException ex) {
      throw new IllegalArgumentException("Unable to read the uploaded .java file.", ex);
    }
  }
}
