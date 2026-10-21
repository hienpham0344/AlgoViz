package com.aisc.algoviz.config;

import com.aisc.algoviz.problem.entity.LegacyProblem;
import com.aisc.algoviz.problem.entity.Solution;
import com.aisc.algoviz.problem.repository.SolutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reference programs for the three built-in practice problems. Existing editorials are preserved. */
@Component
@RequiredArgsConstructor
public class ReferenceSolutionSeeder {
    private final SolutionRepository solutions;
    private final com.aisc.algoviz.problem.repository.ProblemRepository problems;

    @Transactional
    public void seed(List<LegacyProblem> practiceProblems) {
        for (LegacyProblem problem : practiceProblems) {
            Reference reference = switch (problem.getTitle()) {
                case "Two Sum" -> new Reference(TWO_SUM, "Try each pair of indices until their values sum to target. Return those two indices.", "O(n²)", "O(1)");
                case "Valid Anagram" -> new Reference(ANAGRAM, "Count each lowercase letter in s and subtract each letter in t. The strings are anagrams when every count is zero.", "O(n)", "O(1)");
                case "Binary Search" -> new Reference(BINARY_SEARCH, "Compare target to the middle element, then keep only the half that can contain it. Return its index or -1.", "O(log n)", "O(1)");
                default -> null;
            };
            if (reference == null || !sampleMatches(problem)) continue;
            if (!solutions.findByProblemId(problem.getId()).isEmpty()) continue;
            solutions.save(Solution.builder()
                    .problem(problems.getReferenceById(problem.getId()))
                    .codeSnippet(reference.code()).explanation(reference.explanation())
                    .timeComplexity(reference.time()).spaceComplexity(reference.space())
                    .language("Java").build());
        }
    }

    private boolean sampleMatches(LegacyProblem problem) {
        String sample = problem.getSampleInput() == null ? "" : problem.getSampleInput().replace("\r", "").trim();
        return switch (problem.getTitle()) {
            case "Two Sum" -> sample.equals("9\n2 7 11 15");
            case "Valid Anagram" -> sample.equals("anagram\nnagaram");
            case "Binary Search" -> sample.equals("9\n-1 0 3 5 9 12");
            default -> false;
        };
    }

    private record Reference(String code, String explanation, String time, String space) {}

    private static final String TWO_SUM = """
            public class TwoSumSolution {
                public static void main(String[] args) {
                    int[] nums = {3, 2, 9, 4, 7};
                    int target = 9;
                    int[] result = twoSum(nums, target);
                    System.out.println(result[0] + " " + result[1]);
                }

                static int[] twoSum(int[] nums, int target) {
                    for (int i = 0; i < nums.length - 1; i++) {
                        for (int j = i + 1; j < nums.length; j++) {
                            int sum = nums[i] + nums[j];
                            if (sum == target) {
                                return new int[] {i, j};
                            }
                        }
                    }
                    return new int[] {-1, -1};
                }
            }
            """;

    private static final String ANAGRAM = """
            public class ValidAnagramSolution {
                public static void main(String[] args) {
                    String s = "anagram";
                    String t = "nagaram";
                    boolean result = isAnagram(s, t);
                    System.out.println(result);
                }

                static boolean isAnagram(String s, String t) {
                    if (s.length() != t.length()) {
                        return false;
                    }
                    int[] counts = new int[26];
                    for (int i = 0; i < s.length(); i++) {
                        int letter = s.charAt(i) - 'a';
                        counts[letter]++;
                    }
                    for (int i = 0; i < t.length(); i++) {
                        int letter = t.charAt(i) - 'a';
                        counts[letter]--;
                    }
                    for (int i = 0; i < counts.length; i++) {
                        if (counts[i] != 0) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            """;

    private static final String BINARY_SEARCH = """
            public class BinarySearchSolution {
                public static void main(String[] args) {
                    int[] nums = {-1, 0, 3, 5, 9, 12};
                    int target = 9;
                    int result = search(nums, target);
                    System.out.println(result);
                }

                static int search(int[] nums, int target) {
                    int left = 0;
                    int right = nums.length - 1;
                    while (left <= right) {
                        int mid = left + (right - left) / 2;
                        if (nums[mid] == target) {
                            return mid;
                        }
                        if (nums[mid] < target) {
                            left = mid + 1;
                        } else {
                            right = mid - 1;
                        }
                    }
                    return -1;
                }
            }
            """;
}
