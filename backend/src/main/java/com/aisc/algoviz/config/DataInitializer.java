package com.aisc.algoviz.config;

import com.aisc.algoviz.problem.entity.LegacyProblem;
import com.aisc.algoviz.submission.entity.TestCase;
import com.aisc.algoviz.user.entity.User;
import com.aisc.algoviz.problem.repository.LegacyProblemRepository;
import com.aisc.algoviz.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final LegacyProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReferenceSolutionSeeder referenceSolutionSeeder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            User demoUser = User.builder()
                    .username("demo")
                    .email("demo@algoviz.com")
                    .password(passwordEncoder.encode("password123"))
                    .fullName("Demo User")
                    .points(150)
                    .solvedProblemsCount(5)
                    .build();
            userRepository.save(demoUser);
        }

        if (problemRepository.count() == 0) {
            List<LegacyProblem> initialProblems = createSampleProblems();
            problemRepository.saveAll(initialProblems);
        }
        referenceSolutionSeeder.seed(problemRepository.findAll());
    }

    private List<LegacyProblem> createSampleProblems() {
        List<LegacyProblem> list = new ArrayList<>();

        // Problem 1: Two Sum
        LegacyProblem p1 = LegacyProblem.legacyBuilder()
                .title("Two Sum")
                .category("Arrays & Hashing")
                .difficulty("Easy")
                .description("Given an array of integers `nums` and an integer `target`, return indices of the two numbers such that they add up to `target`.")
                .inputFormat("First line contains target. Second line contains space-separated integers for nums.")
                .outputFormat("Print two indices separated by a space.")
                .constraints("2 <= nums.length <= 10^4\n-10^9 <= nums[i] <= 10^9\n-10^9 <= target <= 10^9")
                .sampleInput("9\n2 7 11 15")
                .sampleOutput("0 1")
                .starterCodePython("def two_sum(target, nums):\n    # Write your solution here\n    pass\n")
                .starterCodeJs("function twoSum(target, nums) {\n  // Write your solution here\n}\n")
                .starterCodeJava("public class Main {\n    public static void main(String[] args) {\n        // Write code here\n    }\n}\n")
                .starterCodeCpp("#include <iostream>\nusing namespace std;\n\nint main() {\n    return 0;\n}\n")
                .build();

        TestCase tc1_1 = TestCase.builder().problem(p1).input("9\n2 7 11 15").expectedOutput("0 1").isSample(true).build();
        TestCase tc1_2 = TestCase.builder().problem(p1).input("6\n3 2 4").expectedOutput("1 2").isSample(true).build();
        TestCase tc1_3 = TestCase.builder().problem(p1).input("6\n3 3").expectedOutput("0 1").isSample(true).build();
        p1.setTestCases(List.of(tc1_1, tc1_2, tc1_3));
        list.add(p1);

        // Problem 2: Valid Anagram
        LegacyProblem p2 = LegacyProblem.legacyBuilder()
                .title("Valid Anagram")
                .category("Strings")
                .difficulty("Easy")
                .description("Given two strings `s` and `t`, return `true` if `t` is an anagram of `s`, and `false` otherwise.")
                .inputFormat("First line string s, second line string t.")
                .outputFormat("true or false")
                .constraints("1 <= s.length, t.length <= 5 * 10^4")
                .sampleInput("anagram\nnagaram")
                .sampleOutput("true")
                .starterCodePython("def is_anagram(s, t):\n    return sorted(s) == sorted(t)\n")
                .starterCodeJs("function isAnagram(s, t) {\n  return s.split('').sort().join('') === t.split('').sort().join('');\n}\n")
                .build();

        TestCase tc2_1 = TestCase.builder().problem(p2).input("anagram\nnagaram").expectedOutput("true").isSample(true).build();
        TestCase tc2_2 = TestCase.builder().problem(p2).input("rat\ncar").expectedOutput("false").isSample(false).build();
        p2.setTestCases(List.of(tc2_1, tc2_2));
        list.add(p2);

        // Problem 3: Binary Search
        LegacyProblem p3 = LegacyProblem.legacyBuilder()
                .title("Binary Search")
                .category("Searching")
                .difficulty("Easy")
                .description("Given an array of integers `nums` which is sorted in ascending order, and an integer `target`, write a function to search `target` in `nums`. If `target` exists, then return its index. Otherwise, return `-1`.")
                .inputFormat("First line target. Second line sorted array.")
                .outputFormat("Index of target or -1.")
                .constraints("1 <= nums.length <= 10^4")
                .sampleInput("9\n-1 0 3 5 9 12")
                .sampleOutput("4")
                .starterCodePython("def binary_search(target, nums):\n    left, right = 0, len(nums) - 1\n    while left <= right:\n        mid = (left + right) // 2\n        if nums[mid] == target: return mid\n        elif nums[mid] < target: left = mid + 1\n        else: right = mid - 1\n    return -1\n")
                .build();

        TestCase tc3_1 = TestCase.builder().problem(p3).input("9\n-1 0 3 5 9 12").expectedOutput("4").isSample(true).build();
        p3.setTestCases(List.of(tc3_1));
        list.add(p3);

        return list;
    }
}
