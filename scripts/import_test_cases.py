import ssl
import re
import html
import pg8000.native

def main():
    print("Connecting to Supabase PostgreSQL...")
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE

    con = pg8000.native.Connection(
        user="postgres.hdkwlhukwmrifmagvxhi",
        password="Truykich2.0",
        host="aws-0-ap-northeast-1.pooler.supabase.com",
        port=6543,
        database="postgres",
        ssl_context=ctx
    )

    # 1. Existing problems in test_cases
    existing_pids = set(r[0] for r in con.run("SELECT DISTINCT problem_id FROM test_cases;"))
    print(f"Existing problems with test cases: {len(existing_pids)}")

    # 2. Fetch problems
    problems = con.run("SELECT id, leetcode_id, title, description, sample_input, sample_output FROM problems WHERE description IS NOT NULL;")
    print(f"Fetched {len(problems)} problems.")

    pat = re.compile(
        r"<strong>\s*Input:?\s*</strong>\s*(.*?)\s*<strong>\s*Output:?\s*</strong>\s*(.*?)(?:<strong>\s*Explanation|&nbsp;|</pre>|\n<p>|<p>)",
        re.DOTALL | re.IGNORECASE
    )

    test_case_records = []
    sample_updates = []

    for row in problems:
        p_id, lc_id, title, desc, s_inp, s_out = row
        matches = pat.findall(desc)
        if not matches:
            continue

        first_c_inp = None
        first_c_out = None

        for idx, (inp, out) in enumerate(matches):
            c_inp = html.unescape(re.sub(r"<[^>]+>", "", inp)).strip().replace("\r\n", "\n")
            c_out = html.unescape(re.sub(r"<[^>]+>", "", out)).strip().replace("\r\n", "\n")
            if not c_inp or not c_out:
                continue

            if idx == 0:
                first_c_inp = c_inp
                first_c_out = c_out

            # Only add to test_cases if this problem doesn't already have test cases
            if p_id not in existing_pids:
                is_sample = (idx < 3)
                test_case_records.append((p_id, c_inp, c_out, True, is_sample))

        if (s_inp is None or s_out is None) and first_c_inp and first_c_out:
            sample_updates.append((first_c_inp, first_c_out, p_id))

    print(f"Found {len(test_case_records)} new test cases to insert across problems without test cases.")
    print(f"Found {len(sample_updates)} problems to update with sample_input / sample_output.")

    def sql_quote(s):
        if s is None:
            return "NULL"
        if isinstance(s, bool):
            return "TRUE" if s else "FALSE"
        if isinstance(s, (int, float)):
            return str(s)
        escaped = str(s).replace("'", "''")
        return f"'{escaped}'"

    # 3. Batch insert test_cases
    BATCH_SIZE = 500
    total_inserted = 0
    for i in range(0, len(test_case_records), BATCH_SIZE):
        chunk = test_case_records[i : i + BATCH_SIZE]
        row_tuples = [
            f"({p_id}, {sql_quote(c_inp)}, {sql_quote(c_out)}, {sql_quote(is_pub)}, {sql_quote(is_smp)})"
            for p_id, c_inp, c_out, is_pub, is_smp in chunk
        ]
        sql = f"INSERT INTO test_cases (problem_id, input, expected_output, is_public, is_sample) VALUES {', '.join(row_tuples)};"
        con.run(sql)
        total_inserted += len(chunk)
        print(f"Inserted {total_inserted}/{len(test_case_records)} test cases...")

    # Update sequence
    con.run("SELECT setval('test_cases_id_seq', (SELECT COALESCE(MAX(id), 1) FROM test_cases));")
    print("Updated test_cases_id_seq.")

    # 4. Batch update sample_input/sample_output in chunks
    UPDATE_BATCH = 200
    for i in range(0, len(sample_updates), UPDATE_BATCH):
        chunk = sample_updates[i : i + UPDATE_BATCH]
        stmts = [
            f"UPDATE problems SET sample_input = {sql_quote(s_inp)}, sample_output = {sql_quote(s_out)} WHERE id = {p_id};"
            for s_inp, s_out, p_id in chunk
        ]
        con.run("BEGIN; " + " ".join(stmts) + " COMMIT;")
        print(f"Updated samples {min(i + UPDATE_BATCH, len(sample_updates))}/{len(sample_updates)}...")
    print(f"Updated sample_input/sample_output for {len(sample_updates)} problems.")

    # 5. Core Frontend Problems Starter Code Seeding
    core_starters = [
        (
            912, # Sort an Array
            "Sorting",
            "Space-separated array of integers or nums = [5,2,3,1]",
            "Sorted array of integers.",
            "1 <= nums.length <= 5 * 10^4\n-5 * 10^4 <= nums[i] <= 5 * 10^4",
            """class Solution:
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
""",
            """function sortArray(nums) {
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
""",
            """class Solution {
    public int[] sortArray(int[] nums) {
        java.util.Arrays.sort(nums);
        return nums;
    }
}
""",
            """#include <vector>
#include <algorithm>
using namespace std;

class Solution {
public:
    vector<int> sortArray(vector<int>& nums) {
        sort(nums.begin(), nums.end());
        return nums;
    }
};
"""
        ),
        (
            3, # Longest Substring Without Repeating Characters
            "Sliding Window",
            "A single string s (e.g. s = \"abcabcbb\").",
            "Integer length of the longest unique substring.",
            "0 <= s.length <= 5 * 10^4\ns consists of English letters, digits, symbols and spaces.",
            """class Solution:
    def lengthOfLongestSubstring(self, s: str) -> int:
        char_map = {}
        max_len = left = 0
        for right, char in enumerate(s):
            if char in char_map and char_map[char] >= left:
                left = char_map[char] + 1
            char_map[char] = right
            max_len = max(max_len, right - left + 1)
        return max_len
""",
            """function lengthOfLongestSubstring(s) {
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
""",
            """class Solution {
    public int lengthOfLongestSubstring(String s) {
        int[] map = new int[128];
        java.util.Arrays.fill(map, -1);
        int maxLen = 0, left = 0;
        for (int right = 0; right < s.length(); right++) {
            char c = s.charAt(right);
            if (map[c] >= left) {
                left = map[c] + 1;
            }
            map[c] = right;
            maxLen = Math.max(maxLen, right - left + 1);
        }
        return maxLen;
    }
}
""",
            """#include <string>
#include <vector>
#include <algorithm>
using namespace std;

class Solution {
public:
    int lengthOfLongestSubstring(string s) {
        vector<int> charIdx(128, -1);
        int maxLen = 0, left = 0;
        for (int right = 0; right < (int)s.length(); right++) {
            if (charIdx[s[right]] >= left) left = charIdx[s[right]] + 1;
            charIdx[s[right]] = right;
            maxLen = max(maxLen, right - left + 1);
        }
        return maxLen;
    }
};
"""
        ),
        (
            42, # Trapping Rain Water
            "Two Pointers",
            "An array of non-negative integers height (e.g. height = [0,1,0,2,1,0,1,3,2,1,2,1]).",
            "Total water trapped in units.",
            "n == height.length\n1 <= n <= 2 * 10^4\n0 <= height[i] <= 10^5",
            """class Solution:
    def trap(self, height: list[int]) -> int:
        if not height: return 0
        l, r = 0, len(height) - 1
        left_max, right_max = height[l], height[r]
        water = 0
        while l < r:
            if left_max < right_max:
                l += 1
                left_max = max(left_max, height[l])
                water += left_max - height[l]
            else:
                r -= 1
                right_max = max(right_max, height[r])
                water += right_max - height[r]
        return water
""",
            """function trap(height) {
    let l = 0, r = height.length - 1;
    let leftMax = 0, rightMax = 0, water = 0;
    while (l < r) {
        if (height[l] < height[r]) {
            height[l] >= leftMax ? (leftMax = height[l]) : (water += leftMax - height[l]);
            l++;
        } else {
            height[r] >= rightMax ? (rightMax = height[r]) : (water += rightMax - height[r]);
            r--;
        }
    }
    return water;
}
""",
            """class Solution {
    public int trap(int[] height) {
        int l = 0, r = height.length - 1;
        int leftMax = 0, rightMax = 0, water = 0;
        while (l < r) {
            if (height[l] < height[r]) {
                if (height[l] >= leftMax) leftMax = height[l];
                else water += leftMax - height[l];
                l++;
            } else {
                if (height[r] >= rightMax) rightMax = height[r];
                else water += rightMax - height[r];
                r--;
            }
        }
        return water;
    }
}
""",
            """#include <vector>
#include <algorithm>
using namespace std;

class Solution {
public:
    int trap(vector<int>& height) {
        int l = 0, r = height.size() - 1;
        int leftMax = 0, rightMax = 0, water = 0;
        while (l < r) {
            if (height[l] < height[r]) {
                if (height[l] >= leftMax) leftMax = height[l];
                else water += leftMax - height[l];
                l++;
            } else {
                if (height[r] >= rightMax) rightMax = height[r];
                else water += rightMax - height[r];
                r--;
            }
        }
        return water;
    }
};
"""
        ),
        (
            20, # Valid Parentheses
            "Stack",
            "A string s containing '(', ')', '{', '}', '[' and ']'.",
            "Boolean true or false.",
            "1 <= s.length <= 10^4\ns consists of parentheses only '()[]{}'.",
            """class Solution:
    def isValid(self, s: str) -> bool:
        stack = []
        mapping = {')': '(', '}': '{', ']': '['}
        for char in s:
            if char in mapping:
                top = stack.pop() if stack else '#'
                if mapping[char] != top:
                    return False
            else:
                stack.append(char)
        return not stack
""",
            """function isValid(s) {
    const stack = [];
    const map = { ')': '(', '}': '{', ']': '[' };
    for (let char of s) {
        if (map[char]) {
            if (stack.pop() !== map[char]) return false;
        } else {
            stack.push(char);
        }
    }
    return stack.length === 0;
}
""",
            """class Solution {
    public boolean isValid(String s) {
        java.util.Stack<Character> stack = new java.util.Stack<>();
        for (char c : s.toCharArray()) {
            if (c == '(') stack.push(')');
            else if (c == '{') stack.push('}');
            else if (c == '[') stack.push(']');
            else if (stack.isEmpty() || stack.pop() != c) return false;
        }
        return stack.isEmpty();
    }
}
""",
            """#include <string>
#include <stack>
using namespace std;

class Solution {
public:
    bool isValid(string s) {
        stack<char> st;
        for (char c : s) {
            if (c == '(') st.push(')');
            else if (c == '{') st.push('}');
            else if (c == '[') st.push(']');
            else {
                if (st.empty() || st.top() != c) return false;
                st.pop();
            }
        }
        return st.empty();
    }
};
"""
        ),
        (
            53, # Maximum Subarray
            "Dynamic Programming",
            "An integer array nums.",
            "The largest sum of any contiguous subarray.",
            "1 <= nums.length <= 10^5\n-10^4 <= nums[i] <= 10^4",
            """class Solution:
    def maxSubArray(self, nums: list[int]) -> int:
        cur_sum = max_sum = nums[0]
        for x in nums[1:]:
            cur_sum = max(x, cur_sum + x)
            max_sum = max(max_sum, cur_sum)
        return max_sum
""",
            """function maxSubArray(nums) {
    let cur = nums[0], max = nums[0];
    for (let i = 1; i < nums.length; i++) {
        cur = Math.max(nums[i], cur + nums[i]);
        max = Math.max(max, cur);
    }
    return max;
}
""",
            """class Solution {
    public int maxSubArray(int[] nums) {
        int cur = nums[0], max = nums[0];
        for (int i = 1; i < nums.length; i++) {
            cur = Math.max(nums[i], cur + nums[i]);
            max = Math.max(max, cur);
        }
        return max;
    }
}
""",
            """#include <vector>
#include <algorithm>
using namespace std;

class Solution {
public:
    int maxSubArray(vector<int>& nums) {
        int cur = nums[0], maxVal = nums[0];
        for (size_t i = 1; i < nums.size(); i++) {
            cur = max(nums[i], cur + nums[i]);
            maxVal = max(maxVal, cur);
        }
        return maxVal;
    }
};
"""
        ),
        (
            70, # Climbing Stairs
            "Dynamic Programming",
            "An integer n representing number of steps.",
            "Number of distinct ways to climb to the top.",
            "1 <= n <= 45",
            """class Solution:
    def climbStairs(self, n: int) -> int:
        if n <= 2: return n
        a, b = 1, 2
        for _ in range(3, n + 1):
            a, b = b, a + b
        return b
""",
            """function climbStairs(n) {
    if (n <= 2) return n;
    let a = 1, b = 2;
    for (let i = 3; i <= n; i++) {
        const next = a + b;
        a = b;
        b = next;
    }
    return b;
}
""",
            """class Solution {
    public int climbStairs(int n) {
        if (n <= 2) return n;
        int a = 1, b = 2;
        for (int i = 3; i <= n; i++) {
            int next = a + b;
            a = b;
            b = next;
        }
        return b;
    }
}
""",
            """class Solution {
public:
    int climbStairs(int n) {
        if (n <= 2) return n;
        int a = 1, b = 2;
        for (int i = 3; i <= n; i++) {
            int next = a + b;
            a = b;
            b = next;
        }
        return b;
    }
};
"""
        )
    ]

    for lc_id, cat, in_fmt, out_fmt, constr, py_code, js_code, java_code, cpp_code in core_starters:
        sql = f"""
            UPDATE problems
            SET category = {sql_quote(cat)},
                input_format = {sql_quote(in_fmt)},
                output_format = {sql_quote(out_fmt)},
                constraints = {sql_quote(constr)},
                starter_code_python = {sql_quote(py_code)},
                starter_code_js = {sql_quote(js_code)},
                starter_code_java = {sql_quote(java_code)},
                starter_code_cpp = {sql_quote(cpp_code)}
            WHERE leetcode_id = {lc_id};
        """
        con.run(sql)

    print(f"Seeded starter codes and metadata for {len(core_starters)} core problems.")

    # 6. Final verification
    total_tc = con.run("SELECT count(*) FROM test_cases;")[0][0]
    total_probs_with_tc = con.run("SELECT count(DISTINCT problem_id) FROM test_cases;")[0][0]
    print(f"\nSUCCESS! Total test cases in Supabase: {total_tc}")
    print(f"Total problems with test cases: {total_probs_with_tc} / 4028")

    con.close()

if __name__ == "__main__":
    main()
