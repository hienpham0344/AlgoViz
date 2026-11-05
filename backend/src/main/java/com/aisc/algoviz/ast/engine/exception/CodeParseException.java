package com.aisc.algoviz.ast.engine.exception;

import java.util.List;

/**
 * Nem ra khi code Java nguoi dung nhap vao KHONG the parse duoc (loi cu phap). Giu lai danh sach
 * thong bao loi chi tiet (thuong co dong/cot) de tra ve cho frontend hien thi dung cho, thay vi mot
 * thong bao chung chung "Internal Server Error".
 */
public class CodeParseException extends RuntimeException {

  private final String code;
  private final List<String> problems;

  public CodeParseException(String message, List<String> problems) {
    this(message, "INVALID_JAVA_CODE", problems);
  }

  public CodeParseException(String message, String code, List<String> problems) {
    super(message);
    this.code = code;
    this.problems = problems;
  }

  public String getCode() {
    return code;
  }

  public List<String> getProblems() {
    return problems;
  }
}
