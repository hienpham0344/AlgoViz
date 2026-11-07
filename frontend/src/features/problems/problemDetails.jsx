import { useEffect, useMemo, useRef, useState } from "react";
import AlgoMasterVisualizer from "../visualizer/AlgoMasterVisualizer.jsx";
import UserCodeVisualizer from "./UserCodeVisualizer.jsx";
import SubmissionCelebrationModal from "./SubmissionCelebrationModal.jsx";
import VisualExampleCard from "./VisualExampleCard.jsx";
import ResizableColumns from "../../components/ResizableColumns.jsx";
import { CustomSelect, ProgressView } from "../../components/index.js";
import { api } from "../../services/api.js";
import { problemImages, problemText, readableMath } from "../../services/problemText.js";
import { buildGenericProblem } from "../../services/problemDetailFallback.js";
import { hasReferenceTrace } from "../../services/problemCapabilities.js";
import { clearProblemDraft, readProblemDraft, saveProblemDraft } from "../../services/problemDraft.js";
import { isAccepted, languageKey, mapSubmission, runResult } from "../../services/submissions.js";
import { fetchExecutionTrace } from "../../services/astApi.js";
import ApiState from "../../components/ApiState.jsx";
import "./problemDetails.css";

const RUNNER_LANGUAGES = ["Python 3", "JavaScript", "C++", "Java"];

const DEFAULT_PROBLEMS = {
  1: {
    id: 1,
    title: "Two Sum",
    difficulty: "Easy",
    category: "Array & Hash Table",
    topics: ["Array", "Hash Table"],
    companies: ["Amazon", "Google", "Apple", "Microsoft", "Meta"],
    hints: [
      "A really brute force way would be to search for all possible pairs of numbers but that would be too slow. Can you think of using a Hash Map to look up complement in O(1)?",
      "So, if we check for the number x and its complement target - x, then for each number we just need to see if the complement already exists in our dictionary.",
    ],
    description:
      "Given an array of integers `nums` and an integer `target`, return indices of the two numbers such that they add up to `target`.\n\nYou may assume that each input would have exactly one solution, and you may not use the same element twice.\n\nYou can return the answer in any order.",
    inputFormat: "First line: target integer. Second line: space-separated integers for nums array.",
    outputFormat: "Indices [index1, index2] formatted as a JSON array.",
    constraints: [
      "2 <= nums.length <= 10^4",
      "-10^9 <= nums[i] <= 10^9",
      "-10^9 <= target <= 10^9",
      "Only one valid answer exists.",
    ],
    starterCodePython: `class Solution:
    def twoSum(self, nums: list[int], target: int) -> list[int]:
        seen = {}
        for i, num in enumerate(nums):
            diff = target - num
            if diff in seen:
                return [seen[diff], i]
            seen[num] = i
        return []
`,
    starterCodeJs: `function twoSum(nums, target) {
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
`,
    starterCodeCpp: `class Solution {
public:
    vector<int> twoSum(vector<int>& nums, int target) {
        unordered_map<int, int> map;
        for (int i = 0; i < nums.size(); i++) {
            int complement = target - nums[i];
            if (map.find(complement) != map.end()) {
                return {map[complement], i};
            }
            map[nums[i]] = i;
        }
        return {};
    }
};
`,
    starterCodeJava: `class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (map.containsKey(complement)) {
                return new int[] { map.get(complement), i };
            }
            map.put(nums[i], i);
        }
        return new int[] {};
    }
}
`,
    sampleTestCases: [
      { input: "9\n2 7 11 15", expectedOutput: "[0, 1]" },
      { input: "6\n3 2 4", expectedOutput: "[1, 2]" },
      { input: "6\n3 3", expectedOutput: "[0, 1]" },
    ],
    solutions: [
      {
        id: "py",
        name: "One-Pass Hash Table (Optimal)",
        language: "Python 3",
        timeComplexity: "O(n)",
        spaceComplexity: "O(n)",
        explanation:
          "Traverse the array once. For each element `num`, calculate `diff = target - num`. If `diff` exists in our dictionary `seen`, we return `[seen[diff], i]`. Otherwise, record `seen[num] = i`.",
        codeSnippet: `class Solution:
    def twoSum(self, nums: list[int], target: int) -> list[int]:
        seen = {}
        for i, num in enumerate(nums):
            diff = target - num
            if diff in seen:
                return [seen[diff], i]
            seen[num] = i
        return []`,
      },
      {
        id: "js",
        name: "One-Pass Hash Map",
        language: "JavaScript",
        timeComplexity: "O(n)",
        spaceComplexity: "O(n)",
        explanation:
          "Utilize ES6 `Map` to store visited numbers and their indices. Perform single-iteration lookup with O(1) average time complexity.",
        codeSnippet: `function twoSum(nums, target) {
    const map = new Map();
    for (let i = 0; i < nums.length; i++) {
        const complement = target - nums[i];
        if (map.has(complement)) {
            return [map.get(complement), i];
        }
        map.set(nums[i], i);
    }
    return [];
}`,
      },
      {
        id: "java",
        name: "One-Pass HashMap Approach",
        language: "Java",
        timeComplexity: "O(n)",
        spaceComplexity: "O(n)",
        explanation:
          "Single-pass traversal storing complement key to index inside `java.util.HashMap`. Guarantees optimal linear runtime.",
        codeSnippet: `class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (map.containsKey(complement)) {
                return new int[] { map.get(complement), i };
            }
            map.put(nums[i], i);
        }
        return new int[] {};
    }
}`,
      },
      {
        id: "cpp",
        name: "Hash Table using unordered_map",
        language: "C++",
        timeComplexity: "O(n)",
        spaceComplexity: "O(n)",
        explanation:
          "Use `std::unordered_map` for O(1) average time hash lookups while inspecting each number in the array.",
        codeSnippet: `class Solution {
public:
    vector<int> twoSum(vector<int>& nums, int target) {
        unordered_map<int, int> map;
        for (int i = 0; i < nums.size(); i++) {
            int complement = target - nums[i];
            if (map.find(complement) != map.end()) {
                return {map[complement], i};
            }
            map[nums[i]] = i;
        }
        return {};
    }
};`,
      },
    ],
  },
  704: {
    id: 704,
    title: "Binary Search",
    difficulty: "Easy",
    category: "Binary Search",
    topics: ["Array", "Binary Search"],
    companies: ["Google", "Microsoft", "Amazon", "Apple"],
    hints: [
      "Keep track of two boundary pointers, left and right.",
      "Calculate mid = left + (right - left) // 2 to avoid integer overflow.",
    ],
    description:
      "Given an array of integers `nums` which is sorted in ascending order, and an integer `target`, write a function to search `target` in `nums`. If `target` exists, then return its index. Otherwise, return `-1`.\n\nYou must write an algorithm with `O(log n)` runtime complexity.",
    inputFormat: "First line: target integer. Second line: sorted space-separated array.",
    outputFormat: "Index of target or -1.",
    constraints: [
      "1 <= nums.length <= 10^4",
      "-10^4 < nums[i], target < 10^4",
      "All the integers in nums are unique.",
      "nums is sorted in ascending order.",
    ],
    starterCodePython: `class Solution:
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
`,
    starterCodeJs: `function search(nums, target) {
    let left = 0, right = nums.length - 1;
    while (left <= right) {
        const mid = Math.floor((left + right) / 2);
        if (nums[mid] === target) return mid;
        if (nums[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return -1;
}
`,
    starterCodeCpp: `class Solution {
public:
    int search(vector<int>& nums, int target) {
        int l = 0, r = nums.size() - 1;
        while (l <= r) {
            int mid = l + (r - l) / 2;
            if (nums[mid] == target) return mid;
            if (nums[mid] < target) l = mid + 1;
            else r = mid - 1;
        }
        return -1;
    }
};
`,
    starterCodeJava: `class Solution {
    public int search(int[] nums, int target) {
        int l = 0, r = nums.length - 1;
        while (l <= r) {
            int mid = l + (r - l) / 2;
            if (nums[mid] == target) return mid;
            if (nums[mid] < target) l = mid + 1;
            else r = mid - 1;
        }
        return -1;
    }
}
`,
    sampleTestCases: [
      { input: "9\n-1 0 3 5 9 12", expectedOutput: "4" },
      { input: "2\n-1 0 3 5 9 12", expectedOutput: "-1" },
    ],
    solutions: [
      {
        id: "bs-py",
        name: "Standard Binary Search Iteration",
        language: "Python 3",
        timeComplexity: "O(log n)",
        spaceComplexity: "O(1)",
        explanation:
          "Halves the search space at each iteration by checking if target is to the left or right of mid.",
        codeSnippet: `class Solution:
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
        return -1`,
      },
    ],
  },
  912: {
    id: 912,
    title: "Sort an Array (Merge Sort)",
    difficulty: "Medium",
    category: "Sorting",
    topics: ["Sorting", "Divide & Conquer", "Merge Sort"],
    companies: ["Google", "Amazon", "Microsoft", "Meta"],
    hints: [
      "Can you solve it in O(n log n) time complexity and with O(n) auxiliary memory?",
      "Divide the array into two halves, recursively sort both halves, and then merge them.",
    ],
    description:
      "Given an array of integers `nums`, sort the array in ascending order and return it.\n\nYou must solve the problem without using any built-in functions in `O(n log n)` time complexity and with the smallest space complexity possible.",
    inputFormat: "Space-separated array of integers.",
    outputFormat: "Sorted array of integers.",
    constraints: [
      "1 <= nums.length <= 5 * 10^4",
      "-5 * 10^4 <= nums[i] <= 5 * 10^4",
    ],
    starterCodePython: `class Solution:
    def sortArray(self, nums: list[int]) -> list[int]:
        if len(nums) <= 1:
            return nums
        mid = len(nums) // 2
        left = self.sortArray(nums[:mid])
        right = self.sortArray(nums[mid:])
        return self.merge(left, right)
        
    def merge(self, left: list[int], right: list[int]) -> list[int]:
        res = []
        i = j = 0
        while i < len(left) and j < len(right):
            if left[i] <= right[j]:
                res.append(left[i])
                i += 1
            else:
                res.append(right[j])
                j += 1
        res.extend(left[i:])
        res.extend(right[j:])
        return res
`,
    starterCodeJs: `function sortArray(nums) {
    if (nums.length <= 1) return nums;
    const mid = Math.floor(nums.length / 2);
    const left = sortArray(nums.slice(0, mid));
    const right = sortArray(nums.slice(mid));
    
    const result = [];
    let i = 0, j = 0;
    while (i < left.length && j < right.length) {
        if (left[i] <= right[j]) result.push(left[i++]);
        else result.push(right[j++]);
    }
    return result.concat(left.slice(i)).concat(right.slice(j));
}
`,
    starterCodeCpp: `class Solution {
public:
    vector<int> sortArray(vector<int>& nums) {
        sort(nums.begin(), nums.end());
        return nums;
    }
};
`,
    starterCodeJava: `class Solution {
    public int[] sortArray(int[] nums) {
        mergeSort(nums, 0, nums.length - 1);
        return nums;
    }
    private void mergeSort(int[] nums, int l, int r) {
        if (l >= r) return;
        int m = l + (r - l) / 2;
        mergeSort(nums, l, m);
        mergeSort(nums, m + 1, r);
        merge(nums, l, m, r);
    }
    private void merge(int[] nums, int l, int m, int r) {
        int[] temp = new int[r - l + 1];
        int i = l, j = m + 1, k = 0;
        while (i <= m && j <= r) {
            if (nums[i] <= nums[j]) temp[k++] = nums[i++];
            else temp[k++] = nums[j++];
        }
        while (i <= m) temp[k++] = nums[i++];
        while (j <= r) temp[k++] = nums[j++];
        System.arraycopy(temp, 0, nums, l, temp.length);
    }
}
`,
    sampleTestCases: [
      { input: "5 2 3 1", expectedOutput: "[1, 2, 3, 5]" },
      { input: "5 1 1 2 0 0", expectedOutput: "[0, 0, 1, 1, 2, 5]" },
    ],
    solutions: [
      {
        id: "merge-sort",
        name: "Divide & Conquer (Merge Sort)",
        language: "Python 3",
        timeComplexity: "O(n log n)",
        spaceComplexity: "O(n)",
        explanation:
          "Merge Sort recursively splits the array into two halves until length <= 1, then merges sorted subarrays in linear time.",
        codeSnippet: `class Solution:
    def sortArray(self, nums: list[int]) -> list[int]:
        if len(nums) <= 1:
            return nums
        mid = len(nums) // 2
        left = self.sortArray(nums[:mid])
        right = self.sortArray(nums[mid:])
        return self.merge(left, right)`,
      },
    ],
  },
  242: {
    id: 242,
    title: "Valid Anagram",
    difficulty: "Easy",
    category: "Strings",
    topics: ["Hash Table", "String", "Sorting"],
    companies: ["Amazon", "Bloomberg", "Google", "Facebook"],
    hints: [
      "Can you count the frequency of each character in both strings?",
      "If the lengths differ, they cannot be anagrams.",
    ],
    description:
      "Given two strings `s` and `t`, return `true` if `t` is an anagram of `s`, and `false` otherwise.\n\nAn **Anagram** is a word or phrase formed by rearranging the letters of a different word or phrase, typically using all the original letters exactly once.",
    inputFormat: "First line: string s. Second line: string t.",
    outputFormat: "true or false.",
    constraints: [
      "1 <= s.length, t.length <= 5 * 10^4",
      "s and t consist of lowercase English letters.",
    ],
    starterCodePython: `class Solution:
    def isAnagram(self, s: str, t: str) -> bool:
        if len(s) != len(t):
            return False
        count = {}
        for char in s:
            count[char] = count.get(char, 0) + 1
        for char in t:
            if char not in count or count[char] == 0:
                return False
            count[char] -= 1
        return True
`,
    starterCodeJs: `function isAnagram(s, t) {
    if (s.length !== t.length) return false;
    const count = {};
    for (const c of s) count[c] = (count[c] || 0) + 1;
    for (const c of t) {
        if (!count[c]) return false;
        count[c]--;
    }
    return true;
}
`,
    starterCodeCpp: `class Solution {
public:
    bool isAnagram(string s, string t) {
        if (s.length() != t.length()) return false;
        vector<int> count(26, 0);
        for (char c : s) count[c - 'a']++;
        for (char c : t) {
            if (--count[c - 'a'] < 0) return false;
        }
        return true;
    }
};
`,
    starterCodeJava: `class Solution {
    public boolean isAnagram(String s, String t) {
        if (s.length() != t.length()) return false;
        int[] count = new int[26];
        for (char c : s.toCharArray()) count[c - 'a']++;
        for (char c : t.toCharArray()) {
            if (--count[c - 'a'] < 0) return false;
        }
        return true;
    }
}
`,
    sampleTestCases: [
      { input: "anagram\nnagaram", expectedOutput: "true" },
      { input: "rat\ncar", expectedOutput: "false" },
    ],
    solutions: [
      {
        id: "anagram-hash",
        name: "Frequency Counter Array",
        language: "Python 3",
        timeComplexity: "O(n)",
        spaceComplexity: "O(1)",
        explanation:
          "Uses fixed 26-slot frequency array to compare character counts in O(n) time and O(1) auxiliary memory.",
        codeSnippet: `class Solution:
    def isAnagram(self, s: str, t: str) -> bool:
        if len(s) != len(t):
            return False
        return collections.Counter(s) == collections.Counter(t)`,
      },
    ],
  },
  3: {
    id: 3,
    title: "Longest Substring Without Repeating Characters",
    difficulty: "Medium",
    category: "Sliding Window",
    topics: ["Hash Table", "String", "Sliding Window"],
    companies: ["Amazon", "Microsoft", "Apple", "Google", "Bloomberg"],
    hints: [
      "Use two pointers to create a sliding window.",
      "Store the last seen index of each character to jump the left pointer forward in O(1).",
    ],
    description:
      "Given a string `s`, find the length of the **longest substring** without duplicate characters.",
    inputFormat: "A single string s.",
    outputFormat: "Integer length of the longest unique substring.",
    constraints: [
      "0 <= s.length <= 5 * 10^4",
      "s consists of English letters, digits, symbols and spaces.",
    ],
    starterCodePython: `class Solution:
    def lengthOfLongestSubstring(self, s: str) -> int:
        char_map = {}
        max_len = 0
        left = 0
        for right, char in enumerate(s):
            if char in char_map and char_map[char] >= left:
                left = char_map[char] + 1
            char_map[char] = right
            max_len = max(max_len, right - left + 1)
        return max_len
`,
    starterCodeJs: `function lengthOfLongestSubstring(s) {
    const map = new Map();
    let maxLen = 0, left = 0;
    for (let right = 0; right < s.length; right++) {
        const char = s[right];
        if (map.has(char) && map.get(char) >= left) {
            left = map.get(char) + 1;
        }
        map.set(char, right);
        maxLen = Math.max(maxLen, right - left + 1);
    }
    return maxLen;
}
`,
    starterCodeCpp: `class Solution {
public:
    int lengthOfLongestSubstring(string s) {
        vector<int> charIdx(128, -1);
        int maxLen = 0, left = 0;
        for (int right = 0; right < s.length(); right++) {
            if (charIdx[s[right]] >= left) {
                left = charIdx[s[right]] + 1;
            }
            charIdx[s[right]] = right;
            maxLen = max(maxLen, right - left + 1);
        }
        return maxLen;
    }
};
`,
    starterCodeJava: `class Solution {
    public int lengthOfLongestSubstring(String s) {
        int[] charMap = new int[128];
        Arrays.fill(charMap, -1);
        int maxLen = 0, left = 0;
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            if (charMap[c] >= left) {
                left = charMap[c] + 1;
            }
            charMap[c] = right;
            maxLen = Math.max(maxLen, right - left + 1);
        }
        return maxLen;
    }
}
`,
    sampleTestCases: [
      { input: "abcabcbb", expectedOutput: "3" },
      { input: "bbbbb", expectedOutput: "1" },
      { input: "pwwkew", expectedOutput: "3" },
    ],
    solutions: [
      {
        id: "sliding-window-opt",
        name: "Sliding Window with Direct Index Map",
        language: "Python 3",
        timeComplexity: "O(n)",
        spaceComplexity: "O(min(m, n))",
        explanation:
          "Maintain a sliding window [left, right]. When a repeated character is encountered, advance left pointer to the position immediately following the duplicate.",
        codeSnippet: `class Solution:
    def lengthOfLongestSubstring(self, s: str) -> int:
        char_map = {}
        max_len = left = 0
        for right, char in enumerate(s):
            if char in char_map and char_map[char] >= left:
                left = char_map[char] + 1
            char_map[char] = right
            max_len = max(max_len, right - left + 1)
        return max_len`,
      },
    ],
  },
  206: {
    id: 206,
    title: "Reverse Linked List",
    difficulty: "Easy",
    category: "Linked List",
    topics: ["Linked List", "Recursion"],
    companies: ["Amazon", "Apple", "Google", "Microsoft", "Meta"],
    hints: [
      "Think about maintaining three pointers: prev, curr, and next.",
      "At each step, point curr.next to prev, then advance both prev and curr.",
    ],
    description:
      "Given the `head` of a singly linked list, reverse the list, and return *the reversed list*.",
    inputFormat: "Space-separated values representing the linked list nodes.",
    outputFormat: "Reversed linked list represented as space-separated values or array.",
    constraints: [
      "The number of nodes in the list is the range [0, 5000].",
      "-5000 <= Node.val <= 5000",
    ],
    starterCodePython: `class Solution:
    def reverseList(self, head):
        prev = None
        curr = head
        while curr:
            next_temp = curr.next
            curr.next = prev
            prev = curr
            curr = next_temp
        return prev
`,
    starterCodeJs: `function reverseList(head) {
    let prev = null;
    let curr = head;
    while (curr !== null) {
        let nextTemp = curr.next;
        curr.next = prev;
        prev = curr;
        curr = nextTemp;
    }
    return prev;
}
`,
    starterCodeCpp: `class Solution {
public:
    ListNode* reverseList(ListNode* head) {
        ListNode* prev = nullptr;
        ListNode* curr = head;
        while (curr != nullptr) {
            ListNode* nextTemp = curr->next;
            curr->next = prev;
            prev = curr;
            curr = nextTemp;
        }
        return prev;
    }
};
`,
    starterCodeJava: `class Solution {
    public ListNode reverseList(ListNode head) {
        ListNode prev = null;
        ListNode curr = head;
        while (curr != null) {
            ListNode nextTemp = curr.next;
            curr.next = prev;
            prev = curr;
            curr = nextTemp;
        }
        return prev;
    }
}
`,
    sampleTestCases: [
      { input: "1 2 3 4 5", expectedOutput: "[5, 4, 3, 2, 1]" },
      { input: "1 2", expectedOutput: "[2, 1]" },
    ],
    solutions: [
      {
        id: "reverse-iterative",
        name: "Iterative Pointer Reversal",
        language: "Python 3",
        timeComplexity: "O(n)",
        spaceComplexity: "O(1)",
        explanation:
          "Traverse the linked list once, flipping each node's `next` pointer to point to the previously visited node.",
        codeSnippet: `class Solution:
    def reverseList(self, head):
        prev = None
        curr = head
        while curr:
            next_node = curr.next
            curr.next = prev
            prev = curr
            curr = next_node
        return prev`,
      },
    ],
  },
};

function renderInlineMarkdown(text) {
  if (!text) return null;
  const parts = text.split(/(`[^`]+`)/);
  return parts.map((part, i) => {
    if (part.startsWith("`") && part.endsWith("`")) {
      return <code key={i} className="inline-code">{part.slice(1, -1)}</code>;
    }
    return part;
  });
}

function ProblemDetailsPage({ problemId = 1, isAuthenticated = false }) {
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [sampleNotice, setSampleNotice] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [submissionsError, setSubmissionsError] = useState("");
  const [currentProblem, setCurrentProblem] = useState(() => {
    return DEFAULT_PROBLEMS[problemId] || buildGenericProblem(problemId);
  });

  // Left Pane Tabs: Description, Solution (merges editorial & visualizer), Submissions
  const [leftTab, setLeftTab] = useState(() => {
    const hash = window.location.hash || "";
    if (hash.includes("tab=solution") || hash.includes("tab=editorial") || hash.includes("tab=solutions") || hash.includes("tab=visualizer")) return "solution";
    if (hash.includes("tab=submissions")) return "submissions";
    return "description";
  });

  const [solutionLanguage, setSolutionLanguage] = useState("Python 3");
  const RUNNER_STORAGE_KEY = "algoviz-runner-height-percent";
  const [runnerPercent, setRunnerPercent] = useState(() => {
    try {
      const saved = localStorage.getItem(RUNNER_STORAGE_KEY);
      if (saved) {
        const val = Number(saved);
        if (!isNaN(val) && val >= 8 && val <= 88) return val;
      }
    } catch {}
    return 38; // balanced default height (38%)
  });
  const [isDraggingRunner, setIsDraggingRunner] = useState(false);
  const editorWrapperRef = useRef(null);
  const dragStartRef = useRef(null);

  const isRunnerExpanded = runnerPercent >= 55;

  const updateRunnerPercent = (val) => {
    const clamped = Math.min(88, Math.max(8, Math.round(val * 10) / 10));
    setRunnerPercent(clamped);
    try {
      localStorage.setItem(RUNNER_STORAGE_KEY, String(clamped));
    } catch {}
  };

  const handleRunnerResizeStart = (e) => {
    if (e.button !== 0) return;
    const wrapper = editorWrapperRef.current;
    if (!wrapper) return;
    const rect = wrapper.getBoundingClientRect();
    dragStartRef.current = {
      wrapperTop: rect.top,
      wrapperHeight: rect.height,
      initialPercent: runnerPercent,
    };
    setIsDraggingRunner(true);
    e.currentTarget.setPointerCapture(e.pointerId);
    document.body.classList.add("is-resizing-runner-dock");
  };

  const handleRunnerResizeMove = (e) => {
    if (!dragStartRef.current) return;
    const { wrapperTop, wrapperHeight } = dragStartRef.current;
    if (wrapperHeight <= 0) return;
    const wrapperBottom = wrapperTop + wrapperHeight;
    const runnerPx = wrapperBottom - e.clientY;
    let nextPercent = (runnerPx / wrapperHeight) * 100;
    updateRunnerPercent(nextPercent);
  };

  const handleRunnerResizeEnd = (e) => {
    if (!dragStartRef.current) return;
    dragStartRef.current = null;
    setIsDraggingRunner(false);
    document.body.classList.remove("is-resizing-runner-dock");
    try {
      e.currentTarget.releasePointerCapture(e.pointerId);
    } catch {}
  };

  const handleRunnerDoubleClick = () => {
    updateRunnerPercent(runnerPercent > 55 ? 38 : 72);
  };

  const handleRunnerKeyDown = (e) => {
    if (e.key === "ArrowUp") {
      e.preventDefault();
      updateRunnerPercent(runnerPercent + 5);
    } else if (e.key === "ArrowDown") {
      e.preventDefault();
      updateRunnerPercent(runnerPercent - 5);
    } else if (e.key === "Home") {
      e.preventDefault();
      updateRunnerPercent(8);
    } else if (e.key === "End") {
      e.preventDefault();
      updateRunnerPercent(85);
    }
  };

  const toggleExpandRunner = () => {
    if (runnerPercent >= 55) {
      updateRunnerPercent(38);
    } else {
      updateRunnerPercent(72);
    }
  };

  const handleTabChangeWithComfort = (tabKey) => {
    setRunnerTab(tabKey);
    // If user selects visualize, give generous comfortable height if currently cramped
    if (tabKey === "visualize" && runnerPercent < 55) {
      updateRunnerPercent(68);
    }
  };

  useEffect(() => {
    const handleHash = () => {
      const hash = window.location.hash || "";
      if (hash.includes("tab=solution") || hash.includes("tab=editorial") || hash.includes("tab=solutions") || hash.includes("tab=visualizer")) {
        setLeftTab("solution");
      } else if (hash.includes("tab=submissions")) {
        setLeftTab("submissions");
      } else if (hash.includes("tab=description")) {
        setLeftTab("description");
      }
    };
    window.addEventListener("hashchange", handleHash);
    return () => window.removeEventListener("hashchange", handleHash);
  }, []);

  const [guestDraft] = useState(() => readProblemDraft(problemId));
  const guestDraftRef = useRef(guestDraft);
  const [language, setLanguage] = useState(() => guestDraft?.language || "Python 3");
  const [codeByLanguage, setCodeByLanguage] = useState({
    "Python 3": "",
    JavaScript: "",
    "C++": "",
    Java: "",
    ...guestDraft?.codeByLanguage,
  });

  const [runnerTab, setRunnerTab] = useState("cases");
  const [activeCaseIndex, setActiveCaseIndex] = useState(0);
  const [executionState, setExecutionState] = useState("ready");
  const [executionResult, setExecutionResult] = useState(null);
  const [pendingAction, setPendingAction] = useState(null);
  const [announcement, setAnnouncement] = useState("");

  // Badges & hints expansion
  const [showTopics, setShowTopics] = useState(false);
  const [showCompanies, setShowCompanies] = useState(false);
  const [hintOpen, setHintOpen] = useState(false);
  const [toastMessage, setToastMessage] = useState("");
  const [celebrationData, setCelebrationData] = useState(null);
  const [isCelebrationOpen, setIsCelebrationOpen] = useState(false);
  const metaGroupRef = useRef(null);

  useEffect(() => {
    const handleOutsideClick = (e) => {
      if (metaGroupRef.current && !metaGroupRef.current.contains(e.target)) {
        setShowTopics(false);
        setShowCompanies(false);
      }
    };
    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, []);

  // Submission history stays pending until the server responds.
  const [submissionsList, setSubmissionsList] = useState([]);
  const [submissionsLoading, setSubmissionsLoading] = useState(true);

  useEffect(() => {
    if (!isAuthenticated) {
      setSubmissionsList([]);
      setSubmissionsLoading(false);
      setSubmissionsError("");
      return;
    }
    let active = true;
    setSubmissionsLoading(true);
    setSubmissionsError("");
    api.getProblemSubmissions(problemId).then((data) => {
      if (!Array.isArray(data)) throw new Error("Invalid submissions response.");
      if (active) { setSubmissionsList(data.map(mapSubmission)); setSubmissionsError(""); }
    }).catch((error) => { if (active) setSubmissionsError(error.message); })
      .finally(() => { if (active) setSubmissionsLoading(false); });
    return () => { active = false; };
  }, [problemId, reloadKey, isAuthenticated]);

  useEffect(() => {
    let cancelled = false;
    async function loadProblem() {
      setIsLoading(true);
      setLoadError("");
      setSampleNotice("");
      const fallback = DEFAULT_PROBLEMS[problemId] || buildGenericProblem(problemId);
      try {
        const data = await api.getProblemById(problemId);
        if (!data?.id) throw new Error("Invalid problem response.");
        if (!cancelled) {
          let normalizedConstraints = fallback.constraints;
          if (Array.isArray(data.constraints)) {
            normalizedConstraints = data.constraints;
          } else if (typeof data.constraints === "string" && data.constraints.trim()) {
            normalizedConstraints = data.constraints
              .split(/\r?\n/)
              .map((s) => s.replace(/^[•\-\*]\s*/, "").trim())
              .filter(Boolean);
          }

          const merged = { ...fallback, ...data, constraints: normalizedConstraints,
            difficulty: data.difficulty || fallback.difficulty,
            sampleTestCases: Array.isArray(data.sampleTestCases) ? data.sampleTestCases : fallback.sampleTestCases,
            solutions: Array.isArray(data.solutions) ? data.solutions : fallback.solutions,
          };
          setCurrentProblem(merged);
          setCodeByLanguage({
            "Python 3": merged.starterCodePython || fallback.starterCodePython || "",
            "C++": merged.starterCodeCpp || fallback.starterCodeCpp || "",
            JavaScript: merged.starterCodeJs || fallback.starterCodeJs || "",
            Java: merged.starterCodeJava || fallback.starterCodeJava || "",
            ...guestDraftRef.current?.codeByLanguage,
          });
          if (guestDraftRef.current) {
            clearProblemDraft(problemId);
            guestDraftRef.current = null;
          }
        }
      } catch (error) {
        if (!cancelled) {
          if ((!error.status || error.status >= 500) && DEFAULT_PROBLEMS[problemId]) {
            setCurrentProblem(fallback);
            setSampleNotice("Live problem data is unavailable. This is a sample problem; code execution and submissions require the server.");
          } else {
            setLoadError(error.message);
          }
        }
      } finally { if (!cancelled) setIsLoading(false); }
    }
    loadProblem();
    return () => {
      cancelled = true;
    };
  }, [problemId, reloadKey]);

  const testCasesList = useMemo(() => {
    if (currentProblem?.sampleTestCases && currentProblem.sampleTestCases.length > 0) {
      return currentProblem.sampleTestCases.map((tc) => ({
        input: tc.input || "",
        expected: tc.expectedOutput || "",
      }));
    }
    if (currentProblem?.sampleInput || currentProblem?.sampleOutput) {
      return [{ input: currentProblem.sampleInput || "", expected: currentProblem.sampleOutput || "" }];
    }
    return [];
  }, [currentProblem]);
  const illustrations = useMemo(() => problemImages(currentProblem.description), [currentProblem.description]);

  const fallback = DEFAULT_PROBLEMS[problemId] || buildGenericProblem(problemId);
  const code = codeByLanguage[language] || {
    "Python 3": currentProblem?.starterCodePython || fallback.starterCodePython || "",
    JavaScript: currentProblem?.starterCodeJs || fallback.starterCodeJs || "",
    "C++": currentProblem?.starterCodeCpp || fallback.starterCodeCpp || "",
    Java: currentProblem?.starterCodeJava || fallback.starterCodeJava || "",
  }[language] || "";

  const handleCodeChange = (nextCode) => {
    setCodeByLanguage((c) => ({ ...c, [language]: nextCode }));
    setExecutionState("ready");
  };

  const handleLanguageChange = (nextLang) => {
    setLanguage(nextLang);
    setExecutionState("ready");
    setExecutionResult(null);
  };

  const handleResetCode = () => {
    const starter = {
      "Python 3": currentProblem?.starterCodePython || fallback.starterCodePython || "",
      JavaScript: currentProblem?.starterCodeJs || fallback.starterCodeJs || "",
      "C++": currentProblem?.starterCodeCpp || fallback.starterCodeCpp || "",
      Java: currentProblem?.starterCodeJava || fallback.starterCodeJava || "",
    }[language] || "";

    setCodeByLanguage((c) => ({ ...c, [language]: starter }));
    setExecutionState("ready");
    setExecutionResult(null);
    showToast(starter ? "Reset code to the provided template." : "Editor cleared; no starter code is available for this language.");
  };

  const handleCopyCode = async () => {
    try {
      await navigator.clipboard.writeText(code);
      showToast("Code copied to clipboard!");
    } catch {
      // fallback
    }
  };

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(""), 2800);
  };

  const handleLoadSolutionToEditor = (sol) => {
    const targetLang = RUNNER_LANGUAGES.find((item) => languageKey(item) === languageKey(sol.language));
    if (!targetLang || !sol.codeSnippet) return;
    setLanguage(targetLang);
    setCodeByLanguage((c) => ({ ...c, [targetLang]: sol.codeSnippet }));
    showToast(`Loaded ${targetLang} solution into editor!`);
  };

  const handleRunCode = async () => {
    if (pendingAction) return;
    setPendingAction("run");
    setExecutionState("running");
    setExecutionResult(null);
    if (runnerTab !== "visualize") setRunnerTab("console");
    const currentCase = testCasesList[activeCaseIndex];
    try {
      const trace = languageKey(language) === "java"
        ? await fetchExecutionTrace(code, { customInput: currentCase?.input || "" }) : null;
      const result = trace ? { ...trace, errorMessage: trace.errorMessage || undefined }
        : await api.runCode(languageKey(language), code, currentCase?.input || "");
      const evaluation = { ...runResult(result, currentCase?.expected || ""), trace };
      setExecutionState(evaluation.passed ? "passed" : "failed");
      setExecutionResult(evaluation);
    } catch (error) {
      setExecutionState("failed");
      setExecutionResult({ status: "Execution unavailable", actual: error.message, expected: currentCase?.expected || "", traceError: error.message });
      setAnnouncement(error.message);
    } finally { setPendingAction(null); }
  };

  const handleSubmit = async () => {
    if (pendingAction) return;
    if (!isAuthenticated) {
      if (!saveProblemDraft(problemId, language, codeByLanguage)) {
        showToast("Could not preserve your code. Copy it before signing in.");
        return;
      }
      window.location.hash = `#login?next=${encodeURIComponent(`problem-details?id=${problemId}`)}`;
      return;
    }
    setPendingAction("submit");
    setExecutionState("running");
    setExecutionResult(null);
    setRunnerTab("console");
    try {
      const result = await api.submitCode(problemId, languageKey(language), code);
      if (!result?.id || !result?.status) throw new Error("The server returned no submission result.");
      const accepted = isAccepted(result.status);
      setExecutionState(accepted ? "passed" : "failed");
      setExecutionResult({
        status: result.status,
        actual: result.errorMessage || `${result.passedTestCases ?? 0} / ${result.totalTestCases ?? 0} test cases passed.`,
        expected: testCasesList[activeCaseIndex]?.expected || "",
        runtimeMs: result.executionTimeMs, memoryMb: result.memoryUsageMb,
      });
      setSubmissionsList((previous) => [mapSubmission(result), ...previous.filter((item) => item.id !== result.id)]);
      setSubmissionsError("");
      showToast(accepted ? "Submission Accepted! 🎉" : result.status);
      if (accepted) {
        setCelebrationData({ runtimeMs: result.executionTimeMs, memoryMb: result.memoryUsageMb,
          language, problemTitle: currentProblem.title });
        setIsCelebrationOpen(true);
      }
    } catch (error) {
      setExecutionState("failed");
      setExecutionResult({ status: "Submission unavailable", actual: error.message, expected: "" });
      setAnnouncement(error.message);
    } finally { setPendingAction(null); }
  };

  const lineCount = Math.max(1, code.split(/\r?\n/).length);
  if (isLoading || loadError) return (
    <main className="ide-problem-details">
      <a href="#problems">← Back to problems</a>
      <ApiState loading={isLoading} error={loadError} onRetry={() => setReloadKey((key) => key + 1)} label="Loading problem…" page />
    </main>
  );

  return (
    <main className="ide-problem-details" id="top">
      {sampleNotice && <p className="problem-sample-notice" role="status">{sampleNotice} <button type="button" onClick={() => setReloadKey((key) => key + 1)}>Retry live data</button></p>}
      {!DEFAULT_PROBLEMS[problemId] && <p className="problem-sample-notice" role="note">Imported problems include reference snippets and example inputs. To run or submit code, provide a complete program that reads stdin and prints its answer.</p>}
      {/* 1. TOP STUDIO BAR (LeetCode Inspired) */}
      <header className="ide-topbar" aria-label="Problem workspace navigation">
        <div className="ide-topbar__left">
          <a href="#problems" className="ide-back-btn" title="Back to problemset">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="15 18 9 12 15 6" />
            </svg>
            <span>Problemset</span>
          </a>
          <span className="ide-topbar__sep">/</span>
          <div className="ide-prob-badge">
            <span className="ide-prob-id">#{currentProblem.id}</span>
            <span className="ide-prob-title">{currentProblem.title}</span>
            <span className={`ide-diff-pill ide-diff-pill--${currentProblem.difficulty.toLowerCase()}`}>
              {currentProblem.difficulty}
            </span>
          </div>
        </div>

        <div className="ide-topbar__right">
          <button
            type="button"
            className="ide-action-btn ide-btn-run"
            disabled={Boolean(pendingAction) || !code.trim()}
            onClick={handleRunCode}
          >
            {pendingAction === "run" ? <ProgressView size="sm" label="Running…" /> : <>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <polygon points="5 3 19 12 5 21 5 3" />
              </svg><span>Run</span>
            </>}
          </button>

          <button
            type="button"
            className="ide-action-btn ide-btn-submit"
            disabled={Boolean(pendingAction) || !code.trim()}
            onClick={handleSubmit}
          >
            {pendingAction === "submit" ? <ProgressView size="sm" label="Judging…" /> : <>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <polyline points="20 6 9 17 4 12" />
              </svg><span>Submit</span>
            </>}
          </button>
        </div>
      </header>

      {/* 2. MAIN 2-COLUMN RESIZABLE WORKSPACE */}
      <div className="ide-workspace-body">
        <ResizableColumns
          className="ide-workspace-grid"
          minPrimaryWidth={360}
          minSecondaryWidth={380}
          resizeLabel="Drag to resize problem tabs and code visualizer"
        >
          {/* LEFT COLUMN: LEETCODE STRUCTURED WORKSPACE (Description, Editorial, Solutions, Submissions - NO discussion) */}
          <div className="ide-left-pane">
            {/* LeetCode Tab Strip */}
            <div className="lc-tab-strip" role="tablist" aria-label="LeetCode content tabs">
              <button
                type="button"
                role="tab"
                aria-selected={leftTab === "description"}
                className={`lc-tab ${leftTab === "description" ? "is-active" : ""}`}
                onClick={() => setLeftTab("description")}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                  <polyline points="14 2 14 8 20 8" />
                  <line x1="16" y1="13" x2="8" y2="13" />
                  <line x1="16" y1="17" x2="8" y2="17" />
                </svg>
                <span>Description</span>
              </button>

              <button
                type="button"
                role="tab"
                aria-selected={leftTab === "solution"}
                className={`lc-tab ${leftTab === "solution" ? "is-active" : ""}`}
                onClick={() => setLeftTab("solution")}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
                  <path d="M22 3h-6a4 4 0 0 1 3-3h7z" />
                </svg>
                <span>Solution</span>
              </button>

              <button
                type="button"
                role="tab"
                aria-selected={leftTab === "submissions"}
                className={`lc-tab ${leftTab === "submissions" ? "is-active" : ""}`}
                onClick={() => setLeftTab("submissions")}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <polyline points="12 6 12 12 16 14" />
                </svg>
                <span>Submissions</span>
                {submissionsList.length > 0 && (
                  <span className="lc-tab-badge">{submissionsList.length}</span>
                )}
              </button>
            </div>

            {/* TAB 1: DESCRIPTION */}
            {leftTab === "description" && (
              <div className="lc-panel-content lc-description-content">
                <div className="lc-problem-header">
                  <h1 className="lc-problem-title">
                    {currentProblem.id}. {currentProblem.title}
                  </h1>

                  <div className="lc-meta-chips" ref={metaGroupRef}>
                    <span className={`lc-diff-badge lc-diff-badge--${currentProblem.difficulty.toLowerCase()}`}>
                      {currentProblem.difficulty}
                    </span>

                    {/* Topics Dropdown Pill */}
                    {currentProblem.topics?.length > 0 && (
                    <div className="lc-pill-group">
                      <button
                        type="button"
                        className={`lc-meta-pill ${showTopics ? "is-open" : ""}`}
                        onClick={() => {
                          setShowTopics(!showTopics);
                          setShowCompanies(false);
                        }}
                        aria-expanded={showTopics}
                        aria-label="Toggle problem topics"
                      >
                        <span>Topics</span>
                        <svg className="lc-meta-chevron" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" aria-hidden="true">
                          <polyline points="6 9 12 15 18 9" />
                        </svg>
                      </button>
                      {showTopics && (
                        <div className="lc-meta-tags-popup" role="menu">
                          <div className="lc-meta-popup-header">
                            <span className="lc-meta-popup-title">Algorithm Topics</span>
                          </div>
                          <div className="lc-meta-popup-chips">
                            {currentProblem.topics.map((t, idx) => (
                              <span key={idx} className="lc-tag-chip">{t}</span>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                    )}

                    {/* Companies Pill with Locked State */}
                    {currentProblem.companies?.length > 0 && (
                    <div className="lc-pill-group">
                      <button
                        type="button"
                        className={`lc-meta-pill lc-meta-pill--companies ${showCompanies ? "is-open" : ""}`}
                        onClick={() => {
                          setShowCompanies(!showCompanies);
                          setShowTopics(false);
                        }}
                        aria-expanded={showCompanies}
                        aria-label="Toggle target companies"
                      >
                        <svg className="lc-lock-icon" width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                          <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                          <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                        </svg>
                        <span>Companies</span>
                        <svg className="lc-meta-chevron" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" aria-hidden="true">
                          <polyline points="6 9 12 15 18 9" />
                        </svg>
                      </button>
                      {showCompanies && (
                        <div className="lc-meta-tags-popup lc-meta-tags-popup--companies" role="menu">
                          <div className="lc-meta-popup-header">
                            <div className="lc-meta-popup-title">
                              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                              </svg>
                              <span>Target Companies</span>
                            </div>
                            <span className="lc-locked-badge">Locked</span>
                          </div>

                          <div className="lc-meta-popup-chips">
                            {currentProblem.companies.map((c, idx) => (
                              <span key={idx} className="lc-tag-chip lc-tag-chip--locked" title={`${c} (Locked)`}>
                                <svg width="9" height="9" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                                  <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/>
                                </svg>
                                {c}
                              </span>
                            ))}
                          </div>

                          <p className="lc-meta-popup-note">
                            Frequently asked in technical interviews by top tech firms.
                          </p>
                        </div>
                      )}
                    </div>
                    )}

                    {/* Hint Accordion */}
                    {currentProblem.hints?.length > 0 && (
                    <button
                      type="button"
                      className={`lc-hint-btn ${hintOpen ? "is-open" : ""}`}
                      onClick={() => setHintOpen(!hintOpen)}
                    >
                      <span>💡 Hint 1</span>
                    </button>
                    )}
                  </div>

                  {hintOpen && currentProblem.hints?.length > 0 && (
                    <div className="lc-hint-card">
                      <p>{currentProblem.hints[0]}</p>
                    </div>
                  )}
                </div>

                {/* Problem Statement Body */}
                <div className="lc-problem-body">
                  <div className="lc-statement-text">
                    {(problemText(currentProblem.description) || "No description is available for this problem yet.").split("\n\n").map((para, i) => (
                      <p key={i}>{renderInlineMarkdown(para)}</p>
                    ))}
                  </div>
                  {illustrations.length > 0 && <div className="problem-illustrations">
                    {illustrations.map((item) => <img key={item.src} src={item.src} alt={item.alt} loading="lazy" />)}
                  </div>}

                  {/* Interactive Visual Examples (Blocks / Memory Cells) */}
                  <div className="lc-examples-list">
                    {testCasesList.map((tc, idx) => (
                      <VisualExampleCard
                        key={idx}
                        tc={tc}
                        index={idx}
                        problemId={currentProblem.id}
                      />
                    ))}
                  </div>
                  {!testCasesList.length && <p role="status">No sample test cases are available for this problem.</p>}

                  {/* Constraints */}
                  {Array.isArray(currentProblem.constraints) && currentProblem.constraints.length > 0 && (
                    <div className="lc-constraints-block">
                      <div className="lc-constraints-title">Constraints:</div>
                      <ul className="lc-constraints-list">
                        {currentProblem.constraints.map((c, idx) => (
                          <li key={idx}>{renderInlineMarkdown(c)}</li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {/* AlgoMaster Visualizer Banner Callout */}
                  {hasReferenceTrace(currentProblem.id) && <div className="lc-visualizer-callout">
                    <div className="callout-left">
                      <span className="callout-icon">⚡</span>
                      <div>
                        <strong>AlgoViz Interactive Visualizer</strong>
                        <p>Watch this algorithm execute step-by-step with array animations and synchronized code!</p>
                      </div>
                    </div>
                    <button
                      type="button"
                      className="callout-btn"
                      onClick={() => setLeftTab("solution")}
                    >
                      Open Solution &amp; Visualizer →
                    </button>
                  </div>}
                </div>
              </div>
            )}

            {/* TAB 2: SOLUTION (Unified Intuition, Algorithm, Embedded Visualizer & Reference Code) */}
            {leftTab === "solution" && (
              <div className="lc-panel-content lc-editorial-content">
                <div className="editorial-header">
                  <div className="editorial-eyebrow">Reference Solutions</div>
                  <h2 className="editorial-title">{currentProblem.id}. {currentProblem.title} — {hasReferenceTrace(currentProblem.id) ? "Solutions & Visualizer" : "Solutions"}</h2>
                  <p className="editorial-lead">
                    {currentProblem.id === 1 ? "Explore the Two Sum walkthrough and reference implementations." : "Review the available reference implementations for this problem."}
                  </p>
                </div>

                {currentProblem.id === 1 && <>
                <article className="editorial-approach-card">
                  <div className="approach-header-row">
                    <span className="approach-tag approach-tag--optimal">Approach 1</span>
                    <h3>One-Pass Hash Table (Optimal)</h3>
                  </div>

                  <div className="approach-complexities">
                    <span className="comp-badge comp-time">Time: <strong>O(n)</strong></span>
                    <span className="comp-badge comp-space">Space: <strong>O(n)</strong></span>
                  </div>

                  <div className="approach-section">
                    <h4>Intuition</h4>
                    <p>
                      To improve our runtime from brute force's <code className="inline-code">O(n²)</code>, we need a faster way to check if the complement exists in the array. If the complement exists, we need to get its index. What is the best way to maintain a mapping of each element in the array to its index? A hash map!
                    </p>
                  </div>

                  <div className="approach-section">
                    <h4>Algorithm</h4>
                    <p>
                      While we iterate and insert elements into the hash map, we also look back to check if the current element's complement already exists in the table. If it exists, we have found a solution and return immediately!
                    </p>
                  </div>

                  <div className="approach-diagram-box">
                    <div className="diagram-title">Step-by-step Walkthrough (target = 9):</div>
                    <div className="diagram-steps">
                      <div className="diag-step">
                        <span className="step-num">Step 1:</span>
                        <span>Inspect <code className="inline-code">nums[0] = 2</code>. Complement = 9 - 2 = 7. Seen map is empty. Add <code className="inline-code">seen[2] = 0</code>.</span>
                      </div>
                      <div className="diag-step">
                        <span className="step-num">Step 2:</span>
                        <span>Inspect <code className="inline-code">nums[1] = 7</code>. Complement = 9 - 7 = 2. Look up 2 in seen map: found index 0! Return <code className="inline-code">[0, 1]</code>.</span>
                      </div>
                    </div>
                  </div>

                  {/* EMBEDDED VISUALIZER BOX (AlgoMaster Visualizer inside Solution flow) */}
                  <div className="solution-embedded-visualizer">
                    <div className="embedded-vis-header">
                      <div className="embedded-vis-header-left">
                        <span className="embedded-vis-icon">⚡</span>
                        <div>
                          <h4 className="embedded-vis-title">Interactive Algorithm Visualizer</h4>
                          <span className="embedded-vis-subtitle">Pre-setup optimal solution trace with synchronized code walkthrough</span>
                        </div>
                      </div>
                    </div>
                    <div className="embedded-vis-body">
                      <AlgoMasterVisualizer
                        problem={currentProblem}
                        language={solutionLanguage}
                        onLanguageChange={setSolutionLanguage}
                        onAnnounce={setAnnouncement}
                      />
                    </div>
                  </div>

                  {/* High-Contrast Complexity Analysis Card */}
                  <div className="complexity-analysis-card">
                    <div className="complexity-analysis-header">
                      <div className="complexity-title-group">
                        <span className="complexity-header-icon" aria-hidden="true">
                          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                            <line x1="18" y1="20" x2="18" y2="10" />
                            <line x1="12" y1="20" x2="12" y2="4" />
                            <line x1="6" y1="20" x2="6" y2="14" />
                          </svg>
                        </span>
                        <h4>Complexity Analysis (Độ phức tạp thuật toán)</h4>
                      </div>
                      <span className="complexity-badge-optimal">Optimal O(n)</span>
                    </div>

                    <div className="complexity-items-grid">
                      {/* 1. Time Complexity Card */}
                      <div className="complexity-item-card complexity-item-card--time">
                        <div className="complexity-item-top">
                          <div className="complexity-item-meta">
                            <span className="complexity-type-icon complexity-type-icon--time" aria-hidden="true">
                              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
                                <circle cx="12" cy="12" r="10" />
                                <polyline points="12 6 12 12 16 14" />
                              </svg>
                            </span>
                            <span className="complexity-type-label">Time Complexity (Thời gian)</span>
                          </div>
                          <span className="complexity-big-o complexity-big-o--time">O(n)</span>
                        </div>
                        <p className="complexity-item-desc">
                          We traverse the list containing <code className="complexity-var">n</code> elements only once. Each lookup in the hash table costs only <code className="complexity-var">O(1)</code> time on average.
                        </p>
                      </div>

                      {/* 2. Space Complexity Card */}
                      <div className="complexity-item-card complexity-item-card--space">
                        <div className="complexity-item-top">
                          <div className="complexity-item-meta">
                            <span className="complexity-type-icon complexity-type-icon--space" aria-hidden="true">
                              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
                                <line x1="12" y1="4" x2="12" y2="20" />
                                <line x1="4" y1="12" x2="20" y2="12" />
                              </svg>
                            </span>
                            <span className="complexity-type-label">Space Complexity (Bộ nhớ)</span>
                          </div>
                          <span className="complexity-big-o complexity-big-o--space">O(n)</span>
                        </div>
                        <p className="complexity-item-desc">
                          The extra space required depends on the number of items stored in the hash table, which stores at most <code className="complexity-var">n</code> elements in the worst case.
                        </p>
                      </div>
                    </div>
                  </div>
                </article>

                <article className="editorial-approach-card">
                  <div className="approach-header-row">
                    <span className="approach-tag">Approach 2</span>
                    <h3>Two-Pass Hash Table</h3>
                  </div>
                  <div className="approach-complexities">
                    <span className="comp-badge comp-time">Time: <strong>O(n)</strong></span>
                    <span className="comp-badge comp-space">Space: <strong>O(n)</strong></span>
                  </div>
                  <p>
                    A two-pass approach first builds the entire hash map from the array, then performs a second pass checking for <code className="inline-code">target - nums[i]</code>. It achieves linear complexity but requires two passes over the data.
                  </p>
                </article>

                <article className="editorial-approach-card">
                  <div className="approach-header-row">
                    <span className="approach-tag approach-tag--brute">Approach 3</span>
                    <h3>Brute Force (Nested Loops)</h3>
                  </div>
                  <div className="approach-complexities">
                    <span className="comp-badge comp-time">Time: <strong>O(n²)</strong></span>
                    <span className="comp-badge comp-space">Space: <strong>O(1)</strong></span>
                  </div>
                  <p>
                    Loop through each element <code className="inline-code">x</code> and search for whether there is another value that equals to <code className="inline-code">target - x</code>. While using <code className="inline-code">O(1)</code> space, its quadratic runtime is suboptimal for large inputs.
                  </p>
                </article>
                </>}

                {currentProblem.id === 704 && (
                  <div className="solution-embedded-visualizer">
                    <div className="embedded-vis-header"><h3 className="embedded-vis-title">Binary Search Visualizer</h3></div>
                    <div className="embedded-vis-body">
                      <AlgoMasterVisualizer problem={currentProblem} language={solutionLanguage}
                        onLanguageChange={setSolutionLanguage} onAnnounce={setAnnouncement} />
                    </div>
                  </div>
                )}

                {/* Reference Solutions Library Section */}
                <div className="solutions-header" style={{ marginTop: "32px" }}>
                  <div className="editorial-eyebrow">Available references</div>
                  <h3 className="solutions-title">Reference Implementations Library</h3>
                  <p className="solutions-lead">
                    Code and explanations provided with this problem.
                  </p>
                </div>

                <div className="solutions-list">
                  {!currentProblem.solutions?.length && <p role="status">No reference solutions are available for this problem yet.</p>}
                  {(currentProblem.solutions || []).map((sol) => (
                    <div className="solution-library-card" key={sol.id}>
                      <div className="sol-card-header">
                        <div className="sol-header-left">
                          <span className="sol-lang-badge">{sol.language}</span>
                          <span className="sol-name">{sol.name || "Reference solution"}</span>
                        </div>
                        <div className="sol-header-right">
                          {sol.timeComplexity && <span className="sol-chip sol-chip--time">{sol.timeComplexity}</span>}
                          {sol.spaceComplexity && <span className="sol-chip sol-chip--space">{sol.spaceComplexity}</span>}
                        </div>
                      </div>

                      {sol.explanation && <p className="sol-explanation">{readableMath(sol.explanation)}</p>}

                      <div className="sol-code-block">
                        <pre><code>{sol.codeSnippet}</code></pre>
                      </div>

                      <div className="sol-card-footer">
                        <button
                          type="button"
                          className="sol-load-btn"
                          disabled={!sol.codeSnippet || !RUNNER_LANGUAGES.some((item) => languageKey(item) === languageKey(sol.language))}
                          onClick={() => handleLoadSolutionToEditor(sol)}
                        >
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                            <polyline points="16 18 22 12 16 6" />
                            <polyline points="8 6 2 12 8 18" />
                          </svg>
                          <span>Load into Editor</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* TAB 3: SUBMISSIONS (LeetCode Submission History) */}
            {leftTab === "submissions" && (
              <div className="lc-panel-content lc-submissions-content" aria-busy={submissionsLoading}>
                <div className="submissions-header">
                  <div className="editorial-eyebrow">Submission Records</div>
                  <h2 className="submissions-title">Submission History</h2>
                  <p className="submissions-lead">
                    Detailed record of your verified code submissions, runtime performance, and memory consumption.
                  </p>
                </div>

                {!isAuthenticated ? <p role="status"><a href={`#login?next=${encodeURIComponent(`problem-details?id=${problemId}&tab=submissions`)}`}>Sign in</a> to view your submissions.</p> : <ApiState loading={submissionsLoading} error={submissionsError} onRetry={() => setReloadKey((key) => key + 1)} label="Loading submissions…" />}
                {isAuthenticated && !submissionsLoading && !submissionsList.length && !submissionsError && <p role="status">No submissions yet. Submit your solution to get started.</p>}
                {!submissionsLoading && !submissionsError && submissionsList.length > 0 && <div className="submissions-table-card">
                  <table className="submissions-table">
                    <thead>
                      <tr>
                        <th>Status</th>
                        <th>Language</th>
                        <th>Runtime</th>
                        <th>Memory</th>
                        <th>Submitted</th>
                      </tr>
                    </thead>
                    <tbody>
                      {submissionsList.map((sub) => (
                        <tr key={sub.id} className="submission-row">
                          <td>
                            <span className={`sub-status-pill ${sub.completed ? "sub-status-pill--accepted" : "sub-status-pill--failed"}`}>
                              <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3">
                                <polyline points="20 6 9 17 4 12" />
                              </svg>
                              <span>{sub.status}</span>
                            </span>
                          </td>
                          <td>
                            <span className="sub-lang">{sub.language}</span>
                          </td>
                          <td>
                            <span className="sub-metric">
                              <strong>{sub.runtimeMs ?? "—"} ms</strong>
                            </span>
                          </td>
                          <td>
                            <span className="sub-metric">
                              <strong>{sub.memoryMb ?? "—"} MB</strong>
                            </span>
                          </td>
                          <td>
                            <span className="sub-date">{sub.date}</span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>}
              </div>
            )}
          </div>

          {/* RIGHT COLUMN: Code Editor & Execution Studio */}
          <div className="ide-right-pane">
            {/* Right Pane Subbar: Clean title & Editor Tools */}
            <div className="right-subbar">
              <div className="editor-subbar-title-group">
                <span className="editor-subbar-glyph">&lt;/&gt;</span>
                <span className="editor-subbar-title">Code Editor</span>
              </div>

              {/* Tools for Editor Mode */}
              <div className="right-tools">
                <div className="ide-lang-selector">
                  <CustomSelect
                    value={language}
                    onChange={handleLanguageChange}
                    options={RUNNER_LANGUAGES}
                    ariaLabel="Select Language"
                  />
                </div>
                <button
                  type="button"
                  className="tool-btn"
                  onClick={handleResetCode}
                  title="Reset code to the provided template"
                >
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
                    <polyline points="1 4 1 10 7 10" />
                    <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10" />
                  </svg>
                  <span>Reset</span>
                </button>
                <button
                  type="button"
                  className="tool-btn"
                  onClick={handleCopyCode}
                  title="Copy code to clipboard"
                >
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
                    <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                    <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
                  </svg>
                  <span>Copy</span>
                </button>
              </div>
            </div>

            {/* Code Editor Studio with Testcase & Visualization Console */}
            <div className="right-editor-wrapper" ref={editorWrapperRef}>
              {/* Upper: Code Editor */}
              <div
                className={`ide-editor-section ${isRunnerExpanded ? "is-minimized" : ""}`}
                style={{
                  flex: `1 1 ${100 - runnerPercent}%`,
                  height: `${100 - runnerPercent}%`,
                  minHeight: "80px",
                  transition: isDraggingRunner ? "none" : "flex 0.22s cubic-bezier(0.16, 1, 0.3, 1), height 0.22s cubic-bezier(0.16, 1, 0.3, 1)",
                }}
              >
                <div className="editor-canvas">
                  <div className="editor-gutter" aria-hidden="true">
                    {Array.from({ length: lineCount }, (_, i) => (
                      <span key={i} className="gutter-num">
                        {i + 1}
                      </span>
                    ))}
                  </div>
                  <textarea
                    className="editor-textarea"
                    value={code}
                    onChange={(e) => handleCodeChange(e.target.value)}
                    spellCheck="false"
                    rows={lineCount}
                    aria-label={`${language} code editor`}
                  />
                </div>
              </div>

              {/* Draggable Horizontal Splitter Divider (Matching ResizableColumns Handle Style) */}
              <div
                className={`runner-resizer-divider ${isDraggingRunner ? "is-dragging" : ""}`}
                role="separator"
                aria-orientation="horizontal"
                aria-valuenow={Math.round(runnerPercent)}
                aria-valuemin={8}
                aria-valuemax={88}
                aria-label="Kéo lên/xuống để chỉnh độ cao Console và Visualizer"
                tabIndex={0}
                onPointerDown={handleRunnerResizeStart}
                onPointerMove={handleRunnerResizeMove}
                onPointerUp={handleRunnerResizeEnd}
                onPointerCancel={handleRunnerResizeEnd}
                onDoubleClick={handleRunnerDoubleClick}
                onKeyDown={handleRunnerKeyDown}
                title="Kéo lên/xuống (↕) · Nhấp đúp để đặt lại"
              >
                <div className="runner-resizer-line" />
                <div
                  className="runner-resizer-handle-btn"
                  aria-hidden="true"
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M12 4v16M8 8l4-4 4 4m-8 8 4 4 4-4" />
                  </svg>
                </div>
                {isDraggingRunner && (
                  <div className="runner-resizer-badge" aria-hidden="true">
                    Độ cao: {Math.round(runnerPercent)}%
                  </div>
                )}
              </div>

              {/* Lower: Test Runner Console & User Code Visualizer */}
              <div
                className={`ide-runner-section ${isRunnerExpanded ? "is-expanded" : ""}`}
                style={{
                  flex: `0 0 ${runnerPercent}%`,
                  height: `${runnerPercent}%`,
                  minHeight: "36px",
                  transition: isDraggingRunner ? "none" : "flex 0.22s cubic-bezier(0.16, 1, 0.3, 1), height 0.22s cubic-bezier(0.16, 1, 0.3, 1)",
                }}
              >
                <div className="runner-subbar">
                  <div className="runner-tabs">
                    <button
                      type="button"
                      className={`runner-tab ${runnerTab === "cases" ? "is-active" : ""}`}
                      onClick={() => handleTabChangeWithComfort("cases")}
                    >
                      Test Cases
                    </button>
                    <button
                      type="button"
                      className={`runner-tab ${runnerTab === "console" ? "is-active" : ""}`}
                      onClick={() => handleTabChangeWithComfort("console")}
                    >
                      Console Output
                      {executionResult && (
                        <span className={`runner-tab-dot ${executionState === "passed" ? "is-passed" : "is-failed"}`} />
                      )}
                    </button>
                    <button
                      type="button"
                      className={`runner-tab runner-tab--visualize ${runnerTab === "visualize" ? "is-active" : ""}`}
                      onClick={() => handleTabChangeWithComfort("visualize")}
                    >
                      <span className="runner-vis-tab-icon">✨</span>
                      <span>Visualize</span>
                      {executionResult && (
                        <span className="runner-vis-ready-tag">Ready</span>
                      )}
                    </button>
                  </div>

                  <div className="runner-subbar-right">
                    {/* Quick Height Presets (Lên / Xuống) */}
                    <div className="runner-height-presets" role="group" aria-label="Chế độ độ cao console">
                      <button
                        type="button"
                        className={`runner-preset-btn ${runnerPercent <= 15 ? "is-active" : ""}`}
                        onClick={() => updateRunnerPercent(8)}
                        title="Thu nhỏ tối thiểu (36px)"
                        aria-label="Thu nhỏ tối thiểu"
                      >
                        <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4">
                          <line x1="5" y1="18" x2="19" y2="18" />
                        </svg>
                      </button>
                      <button
                        type="button"
                        className={`runner-preset-btn ${runnerPercent > 15 && runnerPercent <= 48 ? "is-active" : ""}`}
                        onClick={() => updateRunnerPercent(38)}
                        title="Độ cao chuẩn (38%)"
                        aria-label="Độ cao chuẩn"
                      >
                        <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4">
                          <line x1="4" y1="12" x2="20" y2="12" />
                          <rect x="4" y="5" width="16" height="14" rx="2" />
                        </svg>
                      </button>
                      <button
                        type="button"
                        className={`runner-preset-btn ${runnerPercent > 48 && runnerPercent <= 78 ? "is-active" : ""}`}
                        onClick={() => updateRunnerPercent(68)}
                        title="Mở rộng Visualize (68%)"
                        aria-label="Mở rộng Visualize"
                      >
                        <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4">
                          <polyline points="18 15 12 9 6 15" />
                        </svg>
                      </button>
                      <button
                        type="button"
                        className={`runner-preset-btn ${runnerPercent > 78 ? "is-active" : ""}`}
                        onClick={() => updateRunnerPercent(88)}
                        title="Toàn màn hình Runner (88%)"
                        aria-label="Toàn màn hình Runner"
                      >
                        <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4">
                          <polyline points="15 3 21 3 21 9" />
                          <polyline points="9 21 3 21 3 15" />
                          <line x1="21" y1="3" x2="14" y2="10" />
                          <line x1="3" y1="21" x2="10" y2="14" />
                        </svg>
                      </button>
                    </div>

                    <button
                      type="button"
                      className="runner-expand-btn"
                      onClick={toggleExpandRunner}
                      title={isRunnerExpanded ? "Thu gọn Runner (Collapse)" : "Mở rộng Runner (Expand)"}
                      aria-label={isRunnerExpanded ? "Thu gọn Runner" : "Mở rộng Runner"}
                    >
                      {isRunnerExpanded ? (
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4">
                          <polyline points="4 14 12 6 20 14" />
                        </svg>
                      ) : (
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4">
                          <polyline points="4 10 12 18 20 10" />
                        </svg>
                      )}
                      <span>{isRunnerExpanded ? "Thu gọn" : "Mở rộng"}</span>
                    </button>
                  </div>
                </div>

                <div className="runner-content">
                  {runnerTab === "cases" && (
                    <div className="runner-cases-view">
                      {!testCasesList.length && <p role="status">No sample test cases are available. Run uses empty stdin until examples are provided.</p>}
                      <div className="case-chips">
                        {testCasesList.map((_, idx) => (
                          <button
                            type="button"
                            key={idx}
                            className={`case-chip ${activeCaseIndex === idx ? "is-active" : ""}`}
                            onClick={() => setActiveCaseIndex(idx)}
                          >
                            Case {idx + 1}
                          </button>
                        ))}
                      </div>

                      {testCasesList.length > 0 && <div className="case-io-details">
                        <div className="case-field">
                          <label>Input:</label>
                          <pre>{testCasesList[activeCaseIndex]?.input}</pre>
                        </div>
                        <div className="case-field">
                          <label>Expected Output:</label>
                          <pre>{testCasesList[activeCaseIndex]?.expected}</pre>
                        </div>
                      </div>}
                    </div>
                  )}

                  {runnerTab === "console" && (
                    <div className="runner-console-view" aria-busy={Boolean(pendingAction)}>
                      {pendingAction ? <ApiState loading label={pendingAction === "submit" ? "Judging submission…" : "Running code…"} /> : executionResult ? (
                        <div className="console-result">
                          <div className={`console-banner is-${executionState}`}>
                            <div className="banner-status-line">
                              <span className="status-badge">{executionResult.status}</span>
                              <span className="status-meta">
                                Runtime: <strong>{executionResult.runtimeMs ?? "—"} ms</strong> • Memory: <strong>{executionResult.memoryMb ?? "—"} MB</strong>
                              </span>
                            </div>
                          </div>

                          <div className="console-io-comparison">
                            <div className="io-col">
                              <span className="io-head">Actual Output</span>
                              <pre className="io-pre actual">{executionResult.actual}</pre>
                            </div>
                            <div className="io-col">
                              <span className="io-head">Expected Output</span>
                              <pre className="io-pre expected">{executionResult.expected}</pre>
                            </div>
                          </div>
                        </div>
                      ) : (
                        <div className="console-empty">
                          <p>Click &quot;Run&quot; or &quot;Submit&quot; to test your code against sample test cases.</p>
                        </div>
                      )}
                    </div>
                  )}

                  {runnerTab === "visualize" && (
                    <UserCodeVisualizer
                      code={code}
                      language={language}
                      problem={currentProblem}
                      testCase={testCasesList[activeCaseIndex]}
                      executionResult={executionResult}
                      onRunCode={handleRunCode}
                      loading={Boolean(pendingAction)}
                    />
                  )}
                </div>
              </div>
            </div>
          </div>
        </ResizableColumns>
      </div>

      {/* Submission Success Celebration Modal & Confetti Animation */}
      <SubmissionCelebrationModal
        isOpen={isCelebrationOpen}
        onClose={() => setIsCelebrationOpen(false)}
        data={celebrationData}
        onViewSubmissions={() => {
          setLeftTab("submissions");
          setIsCelebrationOpen(false);
        }}
        onViewSolution={() => {
          setLeftTab("solution");
          setIsCelebrationOpen(false);
        }}
      />

      {/* Floating Toast Notification */}
      {toastMessage && (
        <div className="lc-toast-notification">
          <span>{toastMessage}</span>
        </div>
      )}

      <p className="sr-only" aria-live="polite">
        {announcement}
      </p>
    </main>
  );
}

export default ProblemDetailsPage;
