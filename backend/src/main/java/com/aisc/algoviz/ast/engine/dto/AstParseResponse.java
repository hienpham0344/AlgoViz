package com.aisc.algoviz.ast.engine.dto;

public class AstParseResponse {

  private boolean success;
  private AstNode data;
  private ApiError error;

  public AstParseResponse() {}

  private AstParseResponse(boolean success, AstNode data, ApiError error) {
    this.success = success;
    this.data = data;
    this.error = error;
  }

  public static AstParseResponse success(AstNode data) {
    return new AstParseResponse(true, data, null);
  }

  public static AstParseResponse failure(String code, String message) {
    return new AstParseResponse(false, null, new ApiError(code, message));
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public AstNode getData() {
    return data;
  }

  public void setData(AstNode data) {
    this.data = data;
  }

  public ApiError getError() {
    return error;
  }

  public void setError(ApiError error) {
    this.error = error;
  }

  public static class ApiError {
    private String code;
    private String message;

    public ApiError() {}

    public ApiError(String code, String message) {
      this.code = code;
      this.message = message;
    }

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public String getMessage() {
      return message;
    }

    public void setMessage(String message) {
      this.message = message;
    }
  }
}
