package com.aisc.algoviz.config;

import com.aisc.algoviz.problem.entity.LegacyProblem;
import com.aisc.algoviz.problem.repository.LegacyProblemRepository;
import com.aisc.algoviz.submission.entity.TestCase;
import com.aisc.algoviz.user.entity.User;
import com.aisc.algoviz.user.repository.UserRepository;
import com.aisc.algoviz.submission.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final LegacyProblemRepository problemRepository;
    private final UserRepository userRepository;
    private final TestCaseRepository testCaseRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReferenceSolutionSeeder referenceSolutionSeeder;

    public record CaseDef(String input, String expectedOutput, boolean isSample) {}

    @Override
    @Transactional
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

        ensurePracticeProblem(
                "Two Sum", 1, "Arrays & Hashing", "Easy",
                "Given an array of integers `nums` and an integer `target`, return indices of the two numbers such that they add up to `target`.",
                "First line contains target. Second line contains space-separated integers for nums.",
                "Print two indices separated by a space.",
                "2 <= nums.length <= 10^4\n-10^9 <= nums[i] <= 10^9\n-10^9 <= target <= 10^9",
                "9\n2 7 11 15", "0 1",
                "def two_sum(target, nums):\n    # Write your solution here\n    pass\n",
                "function twoSum(target, nums) {\n  // Write your solution here\n}\n",
                "public class Main {\n    public static void main(String[] args) {\n        // Write code here\n    }\n}\n",
                "#include <iostream>\nusing namespace std;\n\nint main() {\n    return 0;\n}\n",
                List.of(
                        new CaseDef("9\n2 7 11 15", "0 1", true),
                        new CaseDef("6\n3 2 4", "1 2", true),
                        new CaseDef("6\n3 3", "0 1", true)
                )
        );

        ensurePracticeProblem(
                "Valid Anagram", 242, "Strings", "Easy",
                "Given two strings `s` and `t`, return `true` if `t` is an anagram of `s`, and `false` otherwise.",
                "First line string s, second line string t.",
                "true or false",
                "1 <= s.length, t.length <= 5 * 10^4",
                "anagram\nnagaram", "true",
                "def is_anagram(s, t):\n    return sorted(s) == sorted(t)\n",
                "function isAnagram(s, t) {\n  return s.split('').sort().join('') === t.split('').sort().join('');\n}\n",
                "public class Main {\n    public static void main(String[] args) {\n        // Write code here\n    }\n}\n",
                "#include <iostream>\nusing namespace std;\n\nint main() {\n    return 0;\n}\n",
                List.of(
                        new CaseDef("anagram\nnagaram", "true", true),
                        new CaseDef("rat\ncar", "false", false)
                )
        );

        ensurePracticeProblem(
                "Binary Search", 704, "Searching", "Easy",
                "Given an array of integers `nums` which is sorted in ascending order, and an integer `target`, write a function to search `target` in `nums`. If `target` exists, then return its index. Otherwise, return `-1`.",
                "First line target. Second line sorted array.",
                "Index of target or -1.",
                "1 <= nums.length <= 10^4",
                "9\n-1 0 3 5 9 12", "4",
                "def binary_search(target, nums):\n    left, right = 0, len(nums) - 1\n    while left <= right:\n        mid = (left + right) // 2\n        if nums[mid] == target: return mid\n        elif nums[mid] < target: left = mid + 1\n        else: right = mid - 1\n    return -1\n",
                "function binarySearch(target, nums) {\n  // Write your solution here\n}\n",
                "public class Main {\n    public static void main(String[] args) {\n        // Write code here\n    }\n}\n",
                "#include <iostream>\nusing namespace std;\n\nint main() {\n    return 0;\n}\n",
                List.of(
                        new CaseDef("9\n-1 0 3 5 9 12", "4", true)
                )
        );

        List<LegacyProblem> seedProblems = new ArrayList<>();
        problemRepository.findByTitle("Two Sum").ifPresent(seedProblems::add);
        problemRepository.findByTitle("Valid Anagram").ifPresent(seedProblems::add);
        problemRepository.findByTitle("Binary Search").ifPresent(seedProblems::add);

        referenceSolutionSeeder.seed(seedProblems);
    }

    private void ensurePracticeProblem(
            String title, int leetcodeId, String category, String difficulty,
            String description, String inputFormat, String outputFormat, String constraints,
            String sampleInput, String sampleOutput,
            String pyCode, String jsCode, String javaCode, String cppCode,
            List<CaseDef> testCaseDefs
    ) {
        LegacyProblem p = problemRepository.findByTitle(title).orElse(null);
        if (p == null) {
            p = LegacyProblem.legacyBuilder()
                    .title(title)
                    .leetcodeId(leetcodeId)
                    .category(category)
                    .difficulty(difficulty)
                    .description(description)
                    .inputFormat(inputFormat)
                    .outputFormat(outputFormat)
                    .constraints(constraints)
                    .sampleInput(sampleInput)
                    .sampleOutput(sampleOutput)
                    .starterCodePython(pyCode)
                    .starterCodeJs(jsCode)
                    .starterCodeJava(javaCode)
                    .starterCodeCpp(cppCode)
                    .build();
            problemRepository.save(p);
        } else {
            boolean updated = false;
            if (p.getLeetcodeId() == null) { p.setLeetcodeId(leetcodeId); updated = true; }
            if (p.getCategory() == null) { p.setCategory(category); updated = true; }
            if (p.getStarterCodePython() == null) { p.setStarterCodePython(pyCode); updated = true; }
            if (p.getStarterCodeJs() == null) { p.setStarterCodeJs(jsCode); updated = true; }
            if (p.getStarterCodeJava() == null) { p.setStarterCodeJava(javaCode); updated = true; }
            if (p.getStarterCodeCpp() == null) { p.setStarterCodeCpp(cppCode); updated = true; }
            if (p.getInputFormat() == null) { p.setInputFormat(inputFormat); updated = true; }
            if (p.getOutputFormat() == null) { p.setOutputFormat(outputFormat); updated = true; }
            if (p.getSampleInput() == null) { p.setSampleInput(sampleInput); updated = true; }
            if (p.getSampleOutput() == null) { p.setSampleOutput(sampleOutput); updated = true; }
            if (updated) {
                problemRepository.save(p);
            }
        }

        if (testCaseRepository.findByProblemId(p.getId()).isEmpty()) {
            final LegacyProblem targetProblem = p;
            List<TestCase> tcs = new ArrayList<>();
            for (CaseDef def : testCaseDefs) {
                tcs.add(TestCase.builder()
                        .problem(targetProblem)
                        .input(def.input())
                        .expectedOutput(def.expectedOutput())
                        .isSample(def.isSample())
                        .build());
            }
            testCaseRepository.saveAll(tcs);
        }
    }
}
