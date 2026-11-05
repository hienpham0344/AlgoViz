package com.aisc.algoviz.ast;

import com.aisc.algoviz.ast.engine.ast.AstAnalyzer;
import com.aisc.algoviz.ast.engine.debug.DebugException;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Pattern;

/** Adds a driver only when the requested Java method has a supported, unambiguous input contract. */
@Component
public class TraceSourcePreparer {
    private final AstAnalyzer analyzer;
    private final ObjectMapper json;
    public TraceSourcePreparer(AstAnalyzer analyzer, ObjectMapper json) { this.analyzer = analyzer; this.json = json; }
    public record Prepared(String executable, String original, int lineOffset, boolean syntheticMain) {}

    public Prepared prepare(String code, String input) {
        var unit = analyzer.parseCompilationUnit(code);
        if (unit.findAll(MethodDeclaration.class).stream().anyMatch(m -> m.isPublic() && m.isStatic()
                && m.getNameAsString().equals("main") && m.getType().isVoidType() && m.getParameters().size() == 1
                && m.getParameter(0).getTypeAsString().equals("String[]"))) return new Prepared(code, code, 0, false);
        if (unit.getPackageDeclaration().isPresent()) throw new DebugException("Add a main method to trace a packaged class.");
        var classes = unit.getTypes().stream().filter(ClassOrInterfaceDeclaration.class::isInstance)
                .map(ClassOrInterfaceDeclaration.class::cast).filter(c -> !c.isInterface()).toList();
        if (classes.size() != 1) throw new DebugException("Provide one class or an explicit main method.");
        var type = classes.get(0);
        var methods = type.getMethods().stream().filter(m -> !m.isPrivate() && !m.getNameAsString().equals("main")
                && m.getBody().isPresent() && !m.getParameters().isEmpty()).toList();
        if (methods.size() != 1) throw new DebugException("Provide a main method when the entry method is ambiguous.");
        MethodDeclaration method = methods.get(0);
        String[] arguments = new String[method.getParameters().size()];
        long arrays = method.getParameters().stream().filter(p -> p.getTypeAsString().equals("int[]")).count();
        long strings = method.getParameters().stream().filter(p -> p.getTypeAsString().equals("String")).count();
        long ints = method.getParameters().stream().filter(p -> p.getTypeAsString().equals("int")).count();
        if (arrays > 1 || ints > 1 || (strings > 0 && strings != arguments.length)
                || arrays + strings + ints != arguments.length || arguments.length > 2)
            throw new DebugException("This method needs an explicit main method to construct its inputs.");
        String array = "new int[0]";
        String scalar = "0";
        String raw = input == null ? "" : input.trim();
        if (arrays > 0) {
            var match = Pattern.compile("\\[([^\\]]*)\\]").matcher(raw);
            String values;
            if (match.find()) {
                values = match.group(1);
                if (ints > 0) scalar = integer(raw.substring(match.end()).replaceFirst("^[,\\s]+", "").replaceFirst("^target\\s*=\\s*", ""));
            } else if (ints > 0) {
                String[] lines = raw.split("\\R");
                if (lines.length != 2) throw new DebugException("Expected target and array on separate lines, or [array], target.");
                boolean arrayFirst = lines[0].trim().matches(".*[\\s,].*");
                values = lines[arrayFirst ? 0 : 1];
                scalar = integer(lines[arrayFirst ? 1 : 0]);
            } else values = raw;
            List<String> numbers = values.isBlank() ? List.of() : Arrays.stream(values.trim().split("[,\\s]+"))
                    .map(TraceSourcePreparer::integer).toList();
            if (numbers.size() > 100) throw new DebugException("Trace inputs are limited to 100 array elements.");
            array = "new int[]{" + String.join(",", numbers) + "}";
        } else if (ints > 0) scalar = integer(raw);
        String[] text = strings > 0 ? (strings == 1 ? new String[]{input == null ? "" : input} : (input == null ? "" : input).split("\\R", -1)) : new String[0];
        if (strings > 0 && text.length != strings) throw new DebugException("Expected one input line per String argument.");
        int strIndex = 0;
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = switch (method.getParameter(i).getTypeAsString()) {
                case "int[]" -> "__algovizInput";
                case "int" -> scalar;
                case "String" -> quoted(text[strIndex++]);
                default -> throw new DebugException("Unsupported entry method parameter.");
            };
        }
        String receiver = method.isStatic() ? type.getNameAsString() : "new " + type.getNameAsString() + "()";
        String call = receiver + "." + method.getNameAsString() + "(" + String.join(",", arguments) + ")";
        String driver = "\npublic static void main(String[] __algovizArgs) {\nint[] __algovizInput = " + array + ";\n";
        if (method.getType().isVoidType()) driver += call + ";\nSystem.out.println(java.util.Arrays.toString(__algovizInput));\n";
        else {
            driver += "var __algovizResult = " + call + ";\n";
            driver += "System.out.println(" + (method.getTypeAsString().equals("int[]") ? "java.util.Arrays.toString(__algovizResult)" : "__algovizResult") + ");\n";
        }
        driver += "}\n";
        var end = type.getEnd().orElseThrow();
        int index = 0;
        for (int line = 1; line < end.line; line++) index = code.indexOf('\n', index) + 1;
        index += end.column - 1;
        String executable = "import java.util.*;\n" + code.substring(0, index) + driver + code.substring(index);
        return new Prepared(executable, code, 1, true);
    }

    private String quoted(String value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new DebugException("Invalid String input.", ex); }
    }
    private static String integer(String value) {
        try { return Integer.toString(Integer.parseInt(value.trim())); }
        catch (NumberFormatException ex) { throw new DebugException("Expected a 32-bit integer in the testcase."); }
    }
}
