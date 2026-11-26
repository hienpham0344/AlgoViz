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
                """
                class Solution:
                    def twoSum(self, nums: list[int], target: int) -> list[int]:
                        seen = {}
                        for i, num in enumerate(nums):
                            diff = target - num
                            if diff in seen:
                                return [seen[diff], i]
                            seen[num] = i
                        return []
                """,
                """
                function twoSum(nums, target) {
                  const map = new Map();
                  for (let i = 0; i < nums.length; i++) {
                    const complement = target - nums[i];
                    if (map.has(complement)) {
                      return [map.get(complement), i];
                    }
                    map.set(nums[i], i);
                  }
                  return [];
                }
                """,
                """
                class Solution {
                    public int[] twoSum(int[] nums, int target) {
                        for (int i = 0; i < nums.length; i++) {
                            for (int j = i + 1; j < nums.length; j++) {
                                if (nums[i] + nums[j] == target) {
                                    return new int[] { i, j };
                                }
                            }
                        }
                        return new int[0];
                    }
                }
                """,
                """
                #include <iostream>
                #include <vector>
                #include <unordered_map>
                using namespace std;

                int main() {
                    int target;
                    if (!(cin >> target)) return 0;
                    vector<int> nums;
                    int x;
                    while (cin >> x) nums.push_back(x);
                    unordered_map<int, int> seen;
                    for (int i = 0; i < (int)nums.size(); i++) {
                        int diff = target - nums[i];
                        if (seen.count(diff)) {
                            cout << seen[diff] << " " << i << endl;
                            return 0;
                        }
                        seen[nums[i]] = i;
                    }
                    return 0;
                }
                """,
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
                """
                class Solution:
                    def isAnagram(self, s: str, t: str) -> bool:
                        return sorted(s.strip()) == sorted(t.strip())
                """,
                """
                function isAnagram(s, t) {
                  return String(s).trim().split('').sort().join('') === String(t).trim().split('').sort().join('');
                }
                """,
                """
                class Solution {
                    public boolean isAnagram(String s, String t) {
                        if (s.length() != t.length()) return false;
                        int[] counts = new int[26];
                        for (int i = 0; i < s.length(); i++) {
                            counts[s.charAt(i) - 'a']++;
                            counts[t.charAt(i) - 'a']--;
                        }
                        for (int count : counts) {
                            if (count != 0) return false;
                        }
                        return true;
                    }
                }
                """,
                """
                #include <iostream>
                #include <string>
                #include <algorithm>
                using namespace std;

                int main() {
                    string s, t;
                    if (!(cin >> s >> t)) return 0;
                    sort(s.begin(), s.end());
                    sort(t.begin(), t.end());
                    cout << (s == t ? "true" : "false") << endl;
                    return 0;
                }
                """,
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
                """
                class Solution:
                    def search(self, nums: list[int], target: int) -> int:
                        left, right = 0, len(nums) - 1
                        while left <= right:
                            mid = (left + right) // 2
                            if nums[mid] == target:
                                return mid
                            elif nums[mid] < target:
                                left = mid + 1
                            else:
                                right = mid - 1
                        return -1
                """,
                """
                function binarySearch(nums, target) {
                  let left = 0, right = nums.length - 1;
                  while (left <= right) {
                    const mid = Math.floor((left + right) / 2);
                    if (nums[mid] === target) return mid;
                    if (nums[mid] < target) left = mid + 1;
                    else right = mid - 1;
                  }
                  return -1;
                }
                """,
                """
                class Solution {
                    public int search(int[] nums, int target) {
                        int left = 0, right = nums.length - 1;
                        while (left <= right) {
                            int mid = left + (right - left) / 2;
                            if (nums[mid] == target) return mid;
                            if (nums[mid] < target) left = mid + 1;
                            else right = mid - 1;
                        }
                        return -1;
                    }
                }
                """,
                """
                #include <iostream>
                #include <vector>
                using namespace std;

                int main() {
                    int target;
                    if (!(cin >> target)) return 0;
                    vector<int> nums;
                    int x;
                    while (cin >> x) nums.push_back(x);
                    int left = 0, right = (int)nums.size() - 1;
                    while (left <= right) {
                        int mid = left + (right - left) / 2;
                        if (nums[mid] == target) { cout << mid << endl; return 0; }
                        if (nums[mid] < target) left = mid + 1;
                        else right = mid - 1;
                    }
                    cout << -1 << endl;
                    return 0;
                }
                """,
                List.of(
                        new CaseDef("9\n-1 0 3 5 9 12", "4", true)
                )
        );

        ensurePracticeProblem(
                "Sort an Array", 912, "Sorting", "Medium",
                "Given an array of integers `nums`, sort the array in ascending order and return it.",
                "Space-separated integers or JSON array for nums.",
                "Sorted array in ascending order.",
                "1 <= nums.length <= 5 * 10^4\n-5 * 10^4 <= nums[i] <= 5 * 10^4",
                "5 2 3 1", "[1, 2, 3, 5]",
                """
                class Solution:
                    def sortArray(self, nums: list[int]) -> list[int]:
                        n = len(nums)
                        for i in range(n - 1):
                            for j in range(n - 1 - i):
                                if nums[j] > nums[j + 1]:
                                    nums[j], nums[j + 1] = nums[j + 1], nums[j]
                        return nums
                """,
                """
                function sortArray(nums) {
                  const arr = [...nums];
                  for (let i = 0; i < arr.length - 1; i++) {
                    for (let j = 0; j < arr.length - 1 - i; j++) {
                      if (arr[j] > arr[j + 1]) {
                        const temp = arr[j];
                        arr[j] = arr[j + 1];
                        arr[j + 1] = temp;
                      }
                    }
                  }
                  return arr;
                }
                """,
                """
                class Solution {
                    public int[] sortArray(int[] nums) {
                        for (int i = 0; i < nums.length - 1; i++) {
                            for (int j = 0; j < nums.length - 1 - i; j++) {
                                if (nums[j] > nums[j + 1]) {
                                    int temp = nums[j];
                                    nums[j] = nums[j + 1];
                                    nums[j + 1] = temp;
                                }
                            }
                        }
                        return nums;
                    }
                }
                """,
                """
                #include <iostream>
                #include <vector>
                #include <algorithm>
                using namespace std;

                int main() {
                    vector<int> nums;
                    int x;
                    while (cin >> x) nums.push_back(x);
                    sort(nums.begin(), nums.end());
                    for (size_t i = 0; i < nums.size(); i++) {
                        if (i) cout << " ";
                        cout << nums[i];
                    }
                    cout << endl;
                    return 0;
                }
                """,
                List.of(
                        new CaseDef("5 2 3 1", "[1, 2, 3, 5]", true),
                        new CaseDef("5 1 1 2 0 0", "[0, 0, 1, 1, 2, 5]", true)
                )
        );

        ensurePracticeProblem(
                "Longest Substring Without Repeating Characters", 3, "Sliding Window", "Medium",
                "Given a string `s`, find the length of the longest substring without duplicate characters.",
                "Single string s.",
                "Integer length of the longest substring without repeating characters.",
                "0 <= s.length <= 5 * 10^4",
                "abcabcbb", "3",
                """
                class Solution:
                    def lengthOfLongestSubstring(self, s: str) -> int:
                        seen = {}
                        left = 0
                        best = 0
                        for right, ch in enumerate(s.strip()):
                            if ch in seen and seen[ch] >= left:
                                left = seen[ch] + 1
                            seen[ch] = right
                            best = max(best, right - left + 1)
                        return best
                """,
                """
                function lengthOfLongestSubstring(s) {
                  const map = new Map();
                  let left = 0, best = 0;
                  const str = String(s).trim();
                  for (let right = 0; right < str.length; right++) {
                    const ch = str[right];
                    if (map.has(ch) && map.get(ch) >= left) {
                      left = map.get(ch) + 1;
                    }
                    map.set(ch, right);
                    best = Math.max(best, right - left + 1);
                  }
                  return best;
                }
                """,
                """
                class Solution {
                    public int lengthOfLongestSubstring(String s) {
                        int[] last = new int[128];
                        java.util.Arrays.fill(last, -1);
                        int left = 0, best = 0;
                        for (int right = 0; right < s.length(); right++) {
                            char c = s.charAt(right);
                            if (c < 128 && last[c] >= left) {
                                left = last[c] + 1;
                            }
                            if (c < 128) last[c] = right;
                            best = Math.max(best, right - left + 1);
                        }
                        return best;
                    }
                }
                """,
                """
                #include <iostream>
                #include <string>
                #include <vector>
                #include <algorithm>
                using namespace std;

                int main() {
                    string s;
                    if (!(cin >> s)) { cout << 0 << endl; return 0; }
                    vector<int> last(256, -1);
                    int left = 0, best = 0;
                    for (int right = 0; right < (int)s.size(); right++) {
                        unsigned char c = s[right];
                        if (last[c] >= left) left = last[c] + 1;
                        last[c] = right;
                        best = max(best, right - left + 1);
                    }
                    cout << best << endl;
                    return 0;
                }
                """,
                List.of(
                        new CaseDef("abcabcbb", "3", true),
                        new CaseDef("bbbbb", "1", true),
                        new CaseDef("pwwkew", "3", true)
                )
        );

        List<LegacyProblem> seedProblems = new ArrayList<>();
        problemRepository.findByTitle("Two Sum").ifPresent(seedProblems::add);
        problemRepository.findByTitle("Valid Anagram").ifPresent(seedProblems::add);
        problemRepository.findByTitle("Binary Search").ifPresent(seedProblems::add);

        referenceSolutionSeeder.seed(seedProblems);
    }

    private boolean isPlaceholderStarter(String code) {
        if (code == null || code.isBlank()) return true;
        return code.contains("Write code here")
                || code.contains("Write your solution here")
                || code.contains("int main() {\n    return 0;\n}");
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
            if (isPlaceholderStarter(p.getStarterCodePython())) { p.setStarterCodePython(pyCode); updated = true; }
            if (isPlaceholderStarter(p.getStarterCodeJs())) { p.setStarterCodeJs(jsCode); updated = true; }
            if (isPlaceholderStarter(p.getStarterCodeJava())) { p.setStarterCodeJava(javaCode); updated = true; }
            if (isPlaceholderStarter(p.getStarterCodeCpp())) { p.setStarterCodeCpp(cppCode); updated = true; }
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
