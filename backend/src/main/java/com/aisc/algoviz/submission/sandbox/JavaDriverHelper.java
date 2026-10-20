package com.aisc.algoviz.submission.sandbox;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaDriverHelper {

    private static final Pattern PUBLIC_CLASS_PATTERN = Pattern.compile("public\\s+class\\s+([A-Za-z0-9_]+)");
    private static final Pattern ANY_CLASS_PATTERN = Pattern.compile("class\\s+([A-Za-z0-9_]+)");

    public static String extractClassName(String code) {
        if (code == null || code.isBlank()) {
            return "Main";
        }
        Matcher pubMatch = PUBLIC_CLASS_PATTERN.matcher(code);
        if (pubMatch.find()) {
            return pubMatch.group(1);
        }
        Matcher anyMatch = ANY_CLASS_PATTERN.matcher(code);
        if (anyMatch.find()) {
            return anyMatch.group(1);
        }
        return "Main";
    }

    public static String prepareJavaCode(String code) {
        if (code == null || code.isBlank()) {
            return code;
        }

        if (code.contains("static void main")) {
            return code;
        }

        // If code has no main method, synthesize runner for known practice problems
        String normalizedCode = code.replaceAll("public\\s+class\\s+", "class ");

        if (code.contains("twoSum")) {
            return TWO_SUM_DRIVER + "\n\n" + normalizedCode;
        } else if (code.contains("isAnagram")) {
            return IS_ANAGRAM_DRIVER + "\n\n" + normalizedCode;
        } else if (code.contains("binarySearch") || code.contains("search")) {
            return BINARY_SEARCH_DRIVER + "\n\n" + normalizedCode;
        }

        return code;
    }

    private static final String TWO_SUM_DRIVER = """
            class Main {
                public static void main(String[] args) {
                    java.util.Scanner sc = new java.util.Scanner(System.in);
                    if (!sc.hasNext()) return;
                    String raw = sc.useDelimiter("\\\\A").next().trim();
                    if (raw.isEmpty()) return;
                    String[] lines = java.util.Arrays.stream(raw.split("\\\\r?\\\\n"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toArray(String[]::new);
                    int target = 0;
                    int[] nums = new int[0];
                    if (lines.length >= 2) {
                        if (lines[0].contains(" ") || lines[0].contains(",")) {
                            nums = parseArray(lines[0]);
                            target = Integer.parseInt(lines[1]);
                        } else {
                            target = Integer.parseInt(lines[0]);
                            nums = parseArray(lines[1]);
                        }
                    } else {
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\\\[(.*?)\\\\]").matcher(raw);
                        if (m.find()) {
                            nums = parseArray(m.group(1));
                            String rest = raw.replace(m.group(0), "").replaceAll("[,\\\\s]", "");
                            target = Integer.parseInt(rest);
                        } else {
                            String[] parts = raw.split("[,\\\\s]+");
                            target = Integer.parseInt(parts[0]);
                            nums = new int[parts.length - 1];
                            for (int i = 1; i < parts.length; i++) nums[i - 1] = Integer.parseInt(parts[i]);
                        }
                    }
                    try {
                        Class<?> clazz = Class.forName("Solution");
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        java.lang.reflect.Method method;
                        int[] res;
                        try {
                            method = clazz.getMethod("twoSum", int[].class, int.class);
                            res = (int[]) method.invoke(instance, nums, target);
                        } catch (NoSuchMethodException e) {
                            method = clazz.getMethod("twoSum", int.class, int[].class);
                            res = (int[]) method.invoke(instance, target, nums);
                        }
                        if (res != null) {
                            System.out.println(java.util.Arrays.toString(res));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                private static int[] parseArray(String s) {
                    String clean = s.replaceAll("[\\\\[\\\\]]", "").trim();
                    if (clean.isEmpty()) return new int[0];
                    String[] tokens = clean.split("[,\\\\s]+");
                    int[] arr = new int[tokens.length];
                    for (int i = 0; i < tokens.length; i++) arr[i] = Integer.parseInt(tokens[i]);
                    return arr;
                }
            }
            """;

    private static final String IS_ANAGRAM_DRIVER = """
            class Main {
                public static void main(String[] args) {
                    java.util.Scanner sc = new java.util.Scanner(System.in);
                    if (!sc.hasNext()) return;
                    String raw = sc.useDelimiter("\\\\A").next().trim();
                    if (raw.isEmpty()) return;
                    String[] lines = java.util.Arrays.stream(raw.split("\\\\r?\\\\n"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toArray(String[]::new);
                    String s = lines.length > 0 ? lines[0] : "";
                    String t = lines.length > 1 ? lines[1] : "";
                    if (t.isEmpty() && s.contains(" ")) {
                        String[] parts = s.split("\\\\s+");
                        s = parts[0];
                        t = parts[1];
                    }
                    try {
                        Class<?> clazz = Class.forName("Solution");
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        java.lang.reflect.Method method = clazz.getMethod("isAnagram", String.class, String.class);
                        Object res = method.invoke(instance, s, t);
                        System.out.println(res);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            """;

    private static final String BINARY_SEARCH_DRIVER = """
            class Main {
                public static void main(String[] args) {
                    java.util.Scanner sc = new java.util.Scanner(System.in);
                    if (!sc.hasNext()) return;
                    String raw = sc.useDelimiter("\\\\A").next().trim();
                    if (raw.isEmpty()) return;
                    String[] lines = java.util.Arrays.stream(raw.split("\\\\r?\\\\n"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toArray(String[]::new);
                    int target = lines.length > 0 ? Integer.parseInt(lines[0]) : 0;
                    int[] nums = lines.length > 1 ? parseArray(lines[1]) : new int[0];
                    try {
                        Class<?> clazz = Class.forName("Solution");
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        java.lang.reflect.Method method;
                        try {
                            method = clazz.getMethod("binarySearch", int[].class, int.class);
                        } catch (NoSuchMethodException e) {
                            try {
                                method = clazz.getMethod("binarySearch", int.class, int[].class);
                            } catch (NoSuchMethodException e2) {
                                method = clazz.getMethod("search", int[].class, int.class);
                            }
                        }
                        Object res;
                        if (method.getParameterTypes()[0] == int[].class) {
                            res = method.invoke(instance, nums, target);
                        } else {
                            res = method.invoke(instance, target, nums);
                        }
                        System.out.println(res);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                private static int[] parseArray(String s) {
                    String clean = s.replaceAll("[\\\\[\\\\]]", "").trim();
                    if (clean.isEmpty()) return new int[0];
                    String[] tokens = clean.split("[,\\\\s]+");
                    int[] arr = new int[tokens.length];
                    for (int i = 0; i < tokens.length; i++) arr[i] = Integer.parseInt(tokens[i]);
                    return arr;
                }
            }
            """;
}
