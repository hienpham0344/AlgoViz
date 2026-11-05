package com.aisc.algoviz.ast.engine.dto;

import java.util.ArrayList;
import java.util.List;

public class AstNode {

  private String astNodeId;
  private String type;
  private String name;
  private Integer line;
  private Integer column;
  private Integer startLine;
  private Integer endLine;
  private List<AstNode> children = new ArrayList<>();

  public AstNode() {}

  public AstNode(String type, String name) {
    this.type = type;
    this.name = name;
  }

  public AstNode(String type, String name, Integer line, Integer column) {
    this.type = type;
    this.name = name;
    this.line = line;
    this.column = column;
  }

  public String getAstNodeId() {
    return astNodeId;
  }

  public void setAstNodeId(String astNodeId) {
    this.astNodeId = astNodeId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getLine() {
    return line;
  }

  public void setLine(Integer line) {
    this.line = line;
  }

  public Integer getColumn() {
    return column;
  }

  public void setColumn(Integer column) {
    this.column = column;
  }

  public Integer getStartLine() {
    return startLine;
  }

  public void setStartLine(Integer startLine) {
    this.startLine = startLine;
  }

  public Integer getEndLine() {
    return endLine;
  }

  public void setEndLine(Integer endLine) {
    this.endLine = endLine;
  }

  public List<AstNode> getChildren() {
    return children;
  }

  public void setChildren(List<AstNode> children) {
    this.children = children;
  }

  public void addChild(AstNode child) {
    this.children.add(child);
  }
}
