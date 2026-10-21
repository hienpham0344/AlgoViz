import ssl
import re
import html
import json
import urllib.request
from datetime import datetime, timedelta
import pg8000.native

def sql_quote(s):
    if s is None:
        return "NULL"
    if isinstance(s, bool):
        return "TRUE" if s else "FALSE"
    if isinstance(s, (int, float)):
        return str(s)
    escaped = str(s).replace("'", "''")
    return f"'{escaped}'"

def clean_constraints_text(desc):
    if not desc:
        return None
    m = re.search(r'<strong>\s*Constraints:?\s*</strong>\s*(?:<ul>)?(.*?)(?:</ul>|</pre>|\n<p>|<p>|$)', desc, re.DOTALL | re.IGNORECASE)
    if not m:
        return None
    raw = m.group(1)
    # Replace superscript/subscript tags
    raw = re.sub(r'<sup>(.*?)</sup>', r'^\1', raw)
    raw = re.sub(r'<sub>(.*?)</sub>', r'_\1', raw)
    # Replace li tags with bullet points
    raw = re.sub(r'</?li>', '\n• ', raw)
    # Strip HTML tags
    clean = re.sub(r'<[^>]+>', '', raw)
    clean = html.unescape(clean)
    # Replace non-breaking spaces
    clean = clean.replace('\u00a0', ' ').replace('&nbsp;', ' ')
    # Normalize lines
    lines = [l.strip() for l in clean.split('\n') if l.strip() and l.strip() != '•']
    out_lines = []
    for l in lines:
        if not l.startswith('•') and not l.startswith('-'):
            l = '• ' + l
        out_lines.append(l)
    return '\n'.join(out_lines) if out_lines else None

def main():
    print("=== AlgoViz: Bắt đầu tối ưu và hoàn thiện dữ liệu mẫu Supabase ===")
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

    # -------------------------------------------------------------
    # 1. ĐỒNG BỘ CATEGORY CHO TOÀN BỘ 4.028 BÀI TẬP TỪ PATTERN_TAGS
    # -------------------------------------------------------------
    print("\n1. Đang đồng bộ category từ pattern_tags LeetCode...")
    con.run("""
        UPDATE problems 
        SET category = pattern_tags->>0 
        WHERE (category IS NULL OR category = '') 
          AND pattern_tags IS NOT NULL 
          AND jsonb_array_length(pattern_tags) > 0;
    """)
    con.run("""
        UPDATE problems 
        SET category = 'Algorithm' 
        WHERE category IS NULL OR category = '';
    """)
    cat_filled = con.run("SELECT count(*) FROM problems WHERE category IS NOT NULL;")[0][0]
    print(f"-> Hoàn tất! Số bài có category chuẩn: {cat_filled:,} / 4,028 bài.")

    # -------------------------------------------------------------
    # 2. TRÍCH XUẤT RÀNG BUỘC (CONSTRAINTS) SẠCH TỪ DESCRIPTION
    # -------------------------------------------------------------
    print("\n2. Đang trích xuất và làm sạch constraints từ đề bài...")
    rows = con.run("SELECT id, description FROM problems WHERE constraints IS NULL AND description IS NOT NULL;")
    constraints_updates = []
    for r in rows:
        p_id, desc = r
        clean_c = clean_constraints_text(desc)
        if clean_c:
            constraints_updates.append((clean_c, p_id))

    print(f"-> Tìm thấy {len(constraints_updates)} bài có thể trích xuất constraints.")
    BATCH = 300
    for i in range(0, len(constraints_updates), BATCH):
        chunk = constraints_updates[i : i + BATCH]
        stmts = [
            f"UPDATE problems SET constraints = {sql_quote(c)} WHERE id = {pid};"
            for c, pid in chunk
        ]
        con.run("BEGIN; " + " ".join(stmts) + " COMMIT;")
    
    total_constr = con.run("SELECT count(*) FROM problems WHERE constraints IS NOT NULL;")[0][0]
    print(f"-> Hoàn tất! Số bài có constraints chuẩn UTF-8: {total_constr:,} bài.")

    # -------------------------------------------------------------
    # 3. NẠP STARTER CODE CHUẨN CHO 210 BÀI TỪ TESTEVAL
    # -------------------------------------------------------------
    print("\n3. Đang nạp Starter Code LeetCode chuẩn từ TestEval...")
    req = urllib.request.Request(
        "https://raw.githubusercontent.com/LLM4SoftwareTesting/TestEval/main/data/leetcode-py-all.jsonl",
        headers={"User-Agent": "Mozilla/5.0"}
    )
    with urllib.request.urlopen(req) as res:
        testeval_data = [json.loads(line) for line in res]

    starter_updates = []
    for d in testeval_data:
        t_num = d.get("task_num")
        func_name = d.get("func_name")
        code = d.get("python_solution", "")
        # Extract def func_name(...) signature
        m = re.search(r'(\s*def\s+' + re.escape(func_name) + r'\s*\(.*?\)(?:\s*->\s*[^:]+)?\s*:)', code, re.DOTALL)
        if m:
            sig = m.group(1).strip()
            # Clean spaces
            starter_code = f"class Solution:\n    {sig}\n        # Write your code here\n        pass\n"
            starter_updates.append((starter_code, t_num))

    for sc, t_num in starter_updates:
        con.run(f"UPDATE problems SET starter_code_python = {sql_quote(sc)} WHERE leetcode_id = {t_num} AND (starter_code_python IS NULL OR starter_code_python = '');")

    total_starters = con.run("SELECT count(*) FROM problems WHERE starter_code_python IS NOT NULL;")[0][0]
    print(f"-> Hoàn tất! Tổng số bài đã có Starter Code Python: {total_starters} bài.")

    # -------------------------------------------------------------
    # 4. NẠP 10 SUBMISSIONS MẪU THỰC TẾ CHO USER 'DEMO'
    # -------------------------------------------------------------
    print("\n4. Đang nạp 10 submissions mẫu thực tế cho tài khoản demo...")
    # Get demo user ID
    user_row = con.run("SELECT id FROM users WHERE username = 'demo' LIMIT 1;")
    if not user_row:
        print("Tạo tài khoản demo mặc định...")
        con.run("""
            INSERT INTO users (username, email, password, role, points, solved_problems_count, full_name, created_at, updated_at)
            VALUES ('demo', 'demo@algoviz.com', '$2a$10$f3O3nnScjvdochk0mf6.we0xbttoereN50COcWedsh4wvHyCNhRmy', 'ROLE_USER', 150, 5, 'Demo User', NOW(), NOW());
        """)
        demo_id = con.run("SELECT id FROM users WHERE username = 'demo' LIMIT 1;")[0][0]
    else:
        demo_id = user_row[0][0]

    # Map leetcode_id to db_id
    p_map = {r[0]: r[1] for r in con.run("SELECT leetcode_id, id FROM problems WHERE leetcode_id IN (1, 704, 242, 912, 3, 42, 20, 53, 70, 207);")}

    now = datetime.now()
    sample_subs = [
        (
            p_map.get(1), demo_id, "Python",
            """class Solution:
    def twoSum(self, nums: list[int], target: int) -> list[int]:
        seen = {}
        for i, num in enumerate(nums):
            diff = target - num
            if diff in seen:
                return [seen[diff], i]
            seen[num] = i
        return []""",
            "Accepted", 1.2, 14.1, 3, 3, None, now - timedelta(hours=2)
        ),
        (
            p_map.get(704), demo_id, "Java",
            """class Solution {
    public int search(int[] nums, int target) {
        int l = 0, r = nums.length - 1;
        while (l <= r) {
            int mid = l + (r - l) / 2;
            if (nums[mid] == target) return mid;
            else if (nums[mid] < target) l = mid + 1;
            else r = mid - 1;
        }
        return -1;
    }
}""",
            "Accepted", 0.8, 18.5, 1, 1, None, now - timedelta(days=1, hours=3)
        ),
        (
            p_map.get(912), demo_id, "Python",
            """class Solution:
    def sortArray(self, nums: list[int]) -> list[int]:
        return sorted(nums)""",
            "Accepted", 18.4, 22.1, 2, 2, None, now - timedelta(days=2, hours=4)
        ),
        (
            p_map.get(3), demo_id, "JavaScript",
            """function lengthOfLongestSubstring(s) {
    const map = new Map();
    let maxLen = 0, left = 0;
    for (let right = 0; right < s.length; right++) {
        const char = s[right];
        if (map.has(char) && map.get(char) >= left) left = map.get(char) + 1;
        map.set(char, right);
        maxLen = Math.max(maxLen, right - left + 1);
    }
    return maxLen;
}""",
            "Accepted", 3.5, 15.6, 3, 3, None, now - timedelta(days=3, hours=1)
        ),
        (
            p_map.get(20), demo_id, "Python",
            """class Solution:
    def isValid(self, s: str) -> bool:
        stack = []
        mapping = {')': '(', '}': '{', ']': '['}
        for char in s:
            if char in mapping:
                top = stack.pop() if stack else '#'
                if mapping[char] != top: return False
            else:
                stack.append(char)
        return not stack""",
            "Accepted", 1.1, 13.8, 5, 5, None, now - timedelta(days=4, hours=5)
        ),
        (
            p_map.get(42), demo_id, "Python",
            """class Solution:
    def trap(self, height: list[int]) -> int:
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
        return water""",
            "Accepted", 4.2, 16.0, 2, 2, None, now - timedelta(days=5, hours=2)
        ),
        (
            p_map.get(53), demo_id, "C++",
            """class Solution {
public:
    int maxSubArray(vector<int>& nums) {
        int cur = nums[0], maxVal = nums[0];
        for (size_t i = 1; i < nums.size(); i++) {
            cur = max(nums[i], cur + nums[i]);
            maxVal = max(maxVal, cur);
        }
        return maxVal;
    }
};""",
            "Accepted", 0.5, 9.2, 3, 3, None, now - timedelta(days=6, hours=3)
        ),
        (
            p_map.get(70), demo_id, "Python",
            """class Solution:
    def climbStairs(self, n: int) -> int:
        if n <= 2: return n
        a, b = 1, 2
        for _ in range(3, n + 1):
            a, b = b, a + b
        return b""",
            "Accepted", 0.9, 13.7, 2, 2, None, now - timedelta(days=7, hours=1)
        ),
        (
            p_map.get(207), demo_id, "Python",
            """class Solution:
    def canFinish(self, numCourses: int, prerequisites: list[list[int]]) -> bool:
        # Incomplete implementation test
        return len(prerequisites) < numCourses""",
            "Wrong Answer", 12.0, 19.1, 1, 2, "Test case #2 failed: expected false, got true", now - timedelta(days=7, hours=4)
        ),
        (
            p_map.get(242), demo_id, "Python",
            """def is_anagram(s, t):
    return sorted(s) == sorted(t)""",
            "Accepted", 2.1, 13.9, 2, 2, None, now - timedelta(days=8, hours=2)
        )
    ]

    # Clear existing demo submissions to avoid duplicate spam on re-run
    con.run(f"DELETE FROM submissions WHERE user_id = {demo_id};")

    for s in sample_subs:
        pid, uid, lang, code, status, time_ms, mem_mb, passed, total, err, sub_time = s
        if pid is not None:
            time_str = sub_time.strftime('%Y-%m-%d %H:%M:%S')
            sql = f"""
                INSERT INTO submissions (problem_id, user_id, language, code, status, execution_time_ms, memory_usage_mb, passed_test_cases, total_test_cases, error_message, submitted_at)
                VALUES ({pid}, {uid}, {sql_quote(lang)}, {sql_quote(code)}, {sql_quote(status)}, {time_ms}, {mem_mb}, {passed}, {total}, {sql_quote(err)}, '{time_str}');
            """
            con.run(sql)

    # Update sequence
    con.run("SELECT setval('submissions_id_seq', (SELECT COALESCE(MAX(id), 1) FROM submissions));")
    subs_count = con.run("SELECT count(*) FROM submissions;")[0][0]
    print(f"-> Hoàn tất! Đã nạp thành công {subs_count} submissions mẫu.")

    # -------------------------------------------------------------
    # 5. KIỂM TRA FONT & ENCODING TRÊN CÁC BẢNG
    # -------------------------------------------------------------
    print("\n5. Kiểm tra font, ký tự đặc biệt & encoding UTF-8...")
    test_chars = con.run("""
        SELECT title, category, constraints 
        FROM problems 
        WHERE leetcode_id IN (1, 3, 20, 42, 912) 
        ORDER BY leetcode_id;
    """)
    for tc in test_chars:
        print(f"  • {tc[0]} | Category: {tc[1]} | Constraints preview: {str(tc[2])[:40].replace('\n', ' ')}...")

    con.close()
    print("\n=== TOÀN BỘ CÔNG VIỆC HOÀN THÀNH XUẤT SẮC! ===")

if __name__ == "__main__":
    main()
