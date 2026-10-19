package com.aisc.algoviz.problem.importer.source;

import com.aisc.algoviz.problem.importer.model.ImportDataSet;

/** Boundary giữa workflow import và nơi lưu trữ các record từ source. */
public interface ProblemDataSource {

    /** Đọc một dataset problem/solution hoàn chỉnh cho một lần import. */
    ImportDataSet load();
}
