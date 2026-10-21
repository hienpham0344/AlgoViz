-- ==========================================================
-- AlgoViz - Supabase PostgreSQL Database Schema & Seed Data
-- ==========================================================

-- 1. Bảng USERS (Quản lý tài khoản & Điểm số)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    avatar_url VARCHAR(255),
    role VARCHAR(50) DEFAULT 'ROLE_USER',
    points INT DEFAULT 0,
    solved_problems_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng REFRESH_TOKENS (Quản lý JWT Refresh Token rotation)
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(512) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_refresh_token_token ON refresh_tokens(token);

-- 3. Bảng PROBLEMS (Danh sách bài toán & Starter Code)
CREATE TABLE IF NOT EXISTS problems (
    id BIGSERIAL PRIMARY KEY,
    leetcode_id INT,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255),
    category VARCHAR(100),
    difficulty VARCHAR(20) NOT NULL,
    description TEXT,
    input_format TEXT,
    output_format TEXT,
    constraints TEXT,
    sample_input TEXT,
    sample_output TEXT,
    starter_code_python TEXT,
    starter_code_cpp TEXT,
    starter_code_java TEXT,
    starter_code_js TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Bảng PROBLEM_TAGS (Tags thuật toán: Array, Hash Table, Dynamic Programming, ...)
CREATE TABLE IF NOT EXISTS problem_tags (
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    tag VARCHAR(100) NOT NULL,
    PRIMARY KEY (problem_id, tag)
);

-- 5. Bảng SOLUTIONS (Lời giải mẫu chuẩn / Editorial)
CREATE TABLE IF NOT EXISTS solutions (
    id BIGSERIAL PRIMARY KEY,
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    pattern_id BIGINT,
    code_snippet TEXT NOT NULL,
    explanation TEXT,
    time_complexity VARCHAR(50),
    space_complexity VARCHAR(50),
    language VARCHAR(20) DEFAULT 'Java'
);

-- 6. Bảng TEST_CASES (Các bộ test chạy thử & test ẩn để chấm điểm)
CREATE TABLE IF NOT EXISTS test_cases (
    id BIGSERIAL PRIMARY KEY,
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    input TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    is_sample BOOLEAN DEFAULT FALSE
);

-- 7. Bảng SUBMISSIONS (Lịch sử nộp bài & kết quả chấm)
CREATE TABLE IF NOT EXISTS submissions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    problem_id BIGINT NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    language VARCHAR(50) NOT NULL,
    code TEXT NOT NULL,
    status VARCHAR(50),
    execution_time_ms DOUBLE PRECISION,
    memory_usage_mb DOUBLE PRECISION,
    passed_test_cases INT,
    total_test_cases INT,
    error_message TEXT,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================================
-- DỮ LIỆU MỒI (SEED DATA)
-- ==========================================================

-- Demo User (mật khẩu mặc định: password123 băm BCrypt)
INSERT INTO users (username, email, password, full_name, points, solved_problems_count)
VALUES (
    'demo',
    'demo@algoviz.com',
    '$2a$10$7ZepwW9vPjFzE1o9w8Wjke7tq.8fFkI9h99H8yS0lE9h5X5lD2Aue',
    'Demo User',
    150,
    5
) ON CONFLICT (username) DO NOTHING;

-- Bài toán 1: Two Sum
INSERT INTO problems (
    id, leetcode_id, title, slug, category, difficulty, description,
    input_format, output_format, constraints, sample_input, sample_output,
    starter_code_python, starter_code_js, starter_code_java, starter_code_cpp
) VALUES (
    1, 1, 'Two Sum', 'two-sum', 'Arrays & Hashing', 'Easy',
    'Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.',
    'First line contains target. Second line contains space-separated integers for nums.',
    'Print two indices separated by a space.',
    '2 <= nums.length <= 10^4\n-10^9 <= nums[i] <= 10^9\n-10^9 <= target <= 10^9',
    '9\n2 7 11 15', '0 1',
    'def two_sum(target, nums):\n    # Write your solution here\n    pass\n',
    'function twoSum(target, nums) {\n  // Write your solution here\n}\n',
    'public class Main {\n    public static void main(String[] args) {\n        // Write code here\n    }\n}\n',
    '#include <iostream>\nusing namespace std;\n\nint main() {\n    return 0;\n}\n'
) ON CONFLICT (id) DO NOTHING;

-- Bài toán 2: Valid Anagram
INSERT INTO problems (
    id, leetcode_id, title, slug, category, difficulty, description,
    input_format, output_format, constraints, sample_input, sample_output,
    starter_code_python, starter_code_js, starter_code_java, starter_code_cpp
) VALUES (
    2, 242, 'Valid Anagram', 'valid-anagram', 'Strings', 'Easy',
    'Given two strings s and t, return true if t is an anagram of s, and false otherwise.',
    'First line string s, second line string t.',
    'true or false',
    '1 <= s.length, t.length <= 5 * 10^4',
    'anagram\nnagaram', 'true',
    'def is_anagram(s, t):\n    return sorted(s) == sorted(t)\n',
    'function isAnagram(s, t) {\n  return s.split('''').sort().join('''') === t.split('''').sort().join('''');\n}\n',
    'public class Main {\n    public static void main(String[] args) {\n        // Write code here\n    }\n}\n',
    '#include <iostream>\nusing namespace std;\n\nint main() {\n    return 0;\n}\n'
) ON CONFLICT (id) DO NOTHING;

-- Bài toán 3: Binary Search
INSERT INTO problems (
    id, leetcode_id, title, slug, category, difficulty, description,
    input_format, output_format, constraints, sample_input, sample_output,
    starter_code_python, starter_code_js, starter_code_java, starter_code_cpp
) VALUES (
    3, 704, 'Binary Search', 'binary-search', 'Searching', 'Easy',
    'Given an array of integers nums which is sorted in ascending order, and an integer target, write a function to search target in nums. If target exists, then return its index. Otherwise, return -1.',
    'First line target. Second line sorted array.',
    'Index of target or -1.',
    '1 <= nums.length <= 10^4\n-10^4 < nums[i], target < 10^4',
    '9\n-1 0 3 5 9 12', '4',
    'def search(nums, target):\n    # Write your solution here\n    pass\n',
    'function search(nums, target) {\n  // Write your solution here\n}\n',
    'public class Main {\n    public static void main(String[] args) {\n        // Write code here\n    }\n}\n',
    '#include <iostream>\nusing namespace std;\n\nint main() {\n    return 0;\n}\n'
) ON CONFLICT (id) DO NOTHING;

-- Tags cho bài toán
INSERT INTO problem_tags (problem_id, tag) VALUES
(1, 'Array'), (1, 'Hash Table'),
(2, 'String'), (2, 'Hash Table'), (2, 'Sorting'),
(3, 'Binary Search'), (3, 'Array')
ON CONFLICT DO NOTHING;

-- Test cases
INSERT INTO test_cases (problem_id, input, expected_output, is_sample) VALUES
(1, '9\n2 7 11 15', '0 1', true),
(1, '6\n3 2 4', '1 2', true),
(1, '6\n3 3', '0 1', true),
(2, 'anagram\nnagaram', 'true', true),
(2, 'rat\ncar', 'false', false),
(3, '9\n-1 0 3 5 9 12', '4', true),
(3, '2\n-1 0 3 5 9 12', '-1', true)
ON CONFLICT DO NOTHING;

-- Lời giải mẫu (Solutions)
INSERT INTO solutions (problem_id, code_snippet, explanation, time_complexity, space_complexity, language) VALUES
(1, 'public int[] twoSum(int[] nums, int target) {\n    Map<Integer, Integer> map = new HashMap<>();\n    for (int i = 0; i < nums.length; i++) {\n        int complement = target - nums[i];\n        if (map.containsKey(complement)) return new int[] { map.get(complement), i };\n        map.put(nums[i], i);\n    }\n    return new int[]{};\n}', 'Use a hash map for single pass lookup.', 'O(n)', 'O(n)', 'Java'),
(2, 'public boolean isAnagram(String s, String t) {\n    if (s.length() != t.length()) return false;\n    int[] count = new int[26];\n    for (char c : s.toCharArray()) count[c - ''a'']++;\n    for (char c : t.toCharArray()) if (--count[c - ''a''] < 0) return false;\n    return true;\n}', 'Count character frequencies with an array.', 'O(n)', 'O(1)', 'Java'),
(3, 'public int search(int[] nums, int target) {\n    int left = 0, right = nums.length - 1;\n    while (left <= right) {\n        int mid = left + (right - left) / 2;\n        if (nums[mid] == target) return mid;\n        else if (nums[mid] < target) left = mid + 1;\n        else right = mid - 1;\n    }\n    return -1;\n}', 'Iterative binary search with midpoint bisect.', 'O(log n)', 'O(1)', 'Java')
ON CONFLICT DO NOTHING;

-- Cập nhật sequence id cho Postgres
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));
SELECT setval('problems_id_seq', (SELECT COALESCE(MAX(id), 1) FROM problems));
SELECT setval('solutions_id_seq', (SELECT COALESCE(MAX(id), 1) FROM solutions));
SELECT setval('test_cases_id_seq', (SELECT COALESCE(MAX(id), 1) FROM test_cases));
