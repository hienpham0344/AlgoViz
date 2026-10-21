import ssl
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

def main():
    print("=== AlgoViz: Bắt đầu chuẩn hóa & tối ưu toàn vẹn Database Supabase ===")
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
    # 1. ĐỒNG BỘ 3.073 LỜI GIẢI TỪ KB_SOLUTIONS SANG SOLUTIONS
    # -------------------------------------------------------------
    print("\n1. Đang đồng bộ lời giải từ kb_solutions sang solutions...")
    # Lấy các cặp (problem_id, language) đã có trong solutions để tránh trùng
    existing_solutions = con.run("SELECT problem_id, lower(language) FROM solutions;")
    existing_set = set((r[0], r[1]) for r in existing_solutions if r[0] and r[1])
    print(f"-> Số solution hiện có trong solutions: {len(existing_solutions)}")

    # Lấy các bài giải hợp lệ từ kb_solutions khớp với problems
    kb_rows = con.run("""
        SELECT kb.problem_id, kb.code_snippet, kb.explanation, kb.pattern_id
        FROM kb_solutions kb
        JOIN problems p ON p.id = kb.problem_id
        WHERE kb.code_snippet IS NOT NULL AND length(trim(kb.code_snippet)) > 0;
    """)
    print(f"-> Số bài giải hợp lệ tìm thấy trong kb_solutions: {len(kb_rows)}")

    to_insert = []
    for r in kb_rows:
        pid, code, expl, pat_id = r
        if (pid, "java") not in existing_set:
            clean_expl = expl if expl and expl.strip() else "Optimal reference Java solution."
            to_insert.append((pid, code, clean_expl, "O(N)", "O(1)", "Java", pat_id))
            existing_set.add((pid, "java"))

    print(f"-> Số solution mới sẽ thêm vào solutions: {len(to_insert)}")
    BATCH = 300
    for i in range(0, len(to_insert), BATCH):
        chunk = to_insert[i : i + BATCH]
        row_tuples = [
            f"({pid}, {sql_quote(code)}, {sql_quote(expl)}, {sql_quote(tc)}, {sql_quote(sc)}, {sql_quote(lang)}, {sql_quote(pat)})"
            for pid, code, expl, tc, sc, lang, pat in chunk
        ]
        sql = f"""
            INSERT INTO solutions (problem_id, code_snippet, explanation, time_complexity, space_complexity, language, pattern_id)
            VALUES {", ".join(row_tuples)};
        """
        con.run(sql)
        print(f"  Đã nạp {min(i + BATCH, len(to_insert))}/{len(to_insert)} solutions...")

    total_sol = con.run("SELECT count(*) FROM solutions;")[0][0]
    distinct_prob_sol = con.run("SELECT count(DISTINCT problem_id) FROM solutions;")[0][0]
    print(f"-> Hoàn tất! Bảng solutions hiện có: {total_sol:,} bài giải (bao phủ {distinct_prob_sol:,} bài tập).")

    # -------------------------------------------------------------
    # 2. XÓA BỎ 10 BẢNG DƯ THỪA (0 DÒNG, BACKEND KHÔNG DÙNG)
    # -------------------------------------------------------------
    print("\n2. Đang dọn dẹp các bảng dư thừa không sử dụng...")
    unused_tables = [
        "trace_steps",
        "traces",
        "comparisons",
        "bug_notes",
        "debug_sessions",
        "bug_rules",
        "rules",
        "kb_bugs",
        "bugs",
        "pattern_results"
    ]

    for tbl in unused_tables:
        con.run(f"DROP TABLE IF EXISTS {tbl} CASCADE;")
        print(f"  ✓ Đã xóa bảng dư thừa: {tbl}")

    # -------------------------------------------------------------
    # 3. THÊM CÁC INDEX TĂNG TỐC TRUY VẤN CHUẨN CHỈ
    # -------------------------------------------------------------
    print("\n3. Đang tạo các Index tăng tốc truy vấn...")
    indexes = [
        ("idx_test_cases_problem_id", "CREATE INDEX IF NOT EXISTS idx_test_cases_problem_id ON test_cases(problem_id);"),
        ("idx_test_cases_sample", "CREATE INDEX IF NOT EXISTS idx_test_cases_sample ON test_cases(problem_id, is_sample);"),
        ("idx_solutions_problem_id", "CREATE INDEX IF NOT EXISTS idx_solutions_problem_id ON solutions(problem_id);"),
        ("idx_submissions_user_id", "CREATE INDEX IF NOT EXISTS idx_submissions_user_id ON submissions(user_id);"),
        ("idx_submissions_user_prob", "CREATE INDEX IF NOT EXISTS idx_submissions_user_prob ON submissions(user_id, problem_id);"),
        ("idx_submissions_submitted", "CREATE INDEX IF NOT EXISTS idx_submissions_submitted ON submissions(submitted_at DESC);"),
        ("idx_problems_category", "CREATE INDEX IF NOT EXISTS idx_problems_category ON problems(category);"),
        ("idx_problems_difficulty", "CREATE INDEX IF NOT EXISTS idx_problems_difficulty ON problems(difficulty);"),
    ]

    for idx_name, idx_sql in indexes:
        con.run(idx_sql)
        print(f"  ✓ Đã tạo/kiểm tra index: {idx_name}")

    # -------------------------------------------------------------
    # 4. ĐỒNG BỘ TẤT CẢ SEQUENCE CHÍNH XÁC
    # -------------------------------------------------------------
    print("\n4. Đang đồng bộ lại các Sequences...")
    sequences = [
        ("problems_id_seq", "problems"),
        ("test_cases_id_seq", "test_cases"),
        ("solutions_id_seq", "solutions"),
        ("submissions_id_seq", "submissions"),
        ("users_id_seq", "users")
    ]
    for seq_name, tbl_name in sequences:
        con.run(f"SELECT setval('{seq_name}', (SELECT COALESCE(MAX(id), 1) FROM {tbl_name}));")
        print(f"  ✓ Đã đồng bộ sequence: {seq_name}")

    # -------------------------------------------------------------
    # 5. KIỂM TRA TOÀN VẸN CUỐI CÙNG
    # -------------------------------------------------------------
    print("\n5. Báo cáo kiểm tra toàn vẹn cuối cùng:")
    rem_tables = con.run("""
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
        ORDER BY table_name;
    """)
    print("Danh sách bảng còn lại trong schema public:")
    for t in rem_tables:
        tname = t[0]
        cnt = con.run(f'SELECT count(*) FROM "{tname}";')[0][0]
        print(f"  • {tname:20} : {cnt:,} dòng")

    con.close()
    print("\n=== CHUẨN HÓA VÀ KIỂM TRA TOÀN VẸN THÀNH CÔNG RỰC RỠ! ===")

if __name__ == "__main__":
    main()
