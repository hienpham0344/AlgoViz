package com.aisc.algoviz.submission.sandbox;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JavaDriverHelperTest {

    @Test
    void extractsPublicClassName() {
        String code = "public class TwoSumSolution { public static void main(String[] args) {} }";
        assertEquals("TwoSumSolution", JavaDriverHelper.extractClassName(code));
    }

    @Test
    void extractsPackagePrivateClassName() {
        String code = "class Solution { public int[] twoSum() { return new int[]{}; } }";
        assertEquals("Solution", JavaDriverHelper.extractClassName(code));
    }

    @Test
    void returnsMainWhenNoClassFoundOrNull() {
        assertEquals("Main", JavaDriverHelper.extractClassName(null));
        assertEquals("Main", JavaDriverHelper.extractClassName("   "));
        assertEquals("Main", JavaDriverHelper.extractClassName("// only comments"));
    }

    @Test
    void leavesCodeWithMainUntouched() {
        String code = "class Solution { public static void main(String[] args) { System.out.println(\"hi\"); } }";
        assertEquals(code, JavaDriverHelper.prepareJavaCode(code));
    }

    @Test
    void synthesizesDriverForTwoSum() {
        String code = "class Solution { public int[] twoSum(int[] nums, int target) { return new int[]{0, 1}; } }";
        String prepared = JavaDriverHelper.prepareJavaCode(code);
        assertTrue(prepared.contains("public static void main(String[] args)"));
        assertTrue(prepared.contains("twoSum"));
        assertTrue(prepared.contains("parseArray"));
    }

    @Test
    void synthesizesDriverForIsAnagram() {
        String code = "class Solution { public boolean isAnagram(String s, String t) { return true; } }";
        String prepared = JavaDriverHelper.prepareJavaCode(code);
        assertTrue(prepared.contains("public static void main(String[] args)"));
        assertTrue(prepared.contains("isAnagram"));
    }

    @Test
    void synthesizesDriverForBinarySearch() {
        String code = "class Solution { public int binarySearch(int[] nums, int target) { return 0; } }";
        String prepared = JavaDriverHelper.prepareJavaCode(code);
        assertTrue(prepared.contains("public static void main(String[] args)"));
        assertTrue(prepared.contains("binarySearch"));
    }

    @Test
    void replacesPublicWithPackagePrivateWhenSynthesizingDriver() {
        String code = "public class Solution { public int[] twoSum(int[] nums, int target) { return new int[]{0, 1}; } }";
        String prepared = JavaDriverHelper.prepareJavaCode(code);
        assertFalse(prepared.contains("public class Solution"));
        assertTrue(prepared.contains("class Solution"));
        assertTrue(prepared.contains("class Main"));
    }
}
