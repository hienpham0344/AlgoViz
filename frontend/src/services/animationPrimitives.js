/**
 * Animation Primitives & Execution Event Architecture.
 *
 * Defines unified animation operations, fixed semantic color conventions,
 * and data structure representations for algorithm visualizations.
 */

export const ACTION_TYPES = {
  READ: "READ",                         // Đọc giá trị: ô sáng nhẹ, hiện giá trị được đọc
  COMPARE: "COMPARE",                   // So sánh: hai đối tượng được nối/highlight, hiện biểu thức & kết quả
  WRITE: "WRITE",                       // Gán/cập nhật: ô thay đổi, giá trị mới xuất hiện
  POINTER_MOVE: "POINTER_MOVE",         // Di chuyển con trỏ: i, j, left, right, mid trượt đến vị trí mới
  SWAP: "SWAP",                         // Hoán đổi: 2 phần tử di chuyển qua nhau, giữ danh tính
  RANGE_NARROW: "RANGE_NARROW",         // Thu hẹp khoảng: vùng bị loại mờ đi (xám), vùng đang xét giữ sáng
  MAP_LOOKUP: "MAP_LOOKUP",             // Hash map tra cứu: highlight ô đang tra cứu (vàng)
  MAP_PUT: "MAP_PUT",                   // Hash map thêm/cập nhật: ô mới sáng xanh lá
  COUNT_UPDATE: "COUNT_UPDATE",         // Bảng đếm: ký tự đọc nối đến ô tần suất, số đếm tăng/giảm
  RESULT_PRODUCE: "RESULT_PRODUCE",     // Tạo kết quả: đưa giá trị vào vùng kết quả, kèm dòng mã
};

/**
 * Semantic Color Palette (Ý nghĩa màu sắc cố định)
 */
export const SEMANTIC_COLORS = {
  EXAMINING: "#00e5ff",      // Xanh dương / Cyan: Đối tượng / con trỏ đang xét chính (i, left, key đang duyệt)
  SECONDARY: "#d946ef",      // Tím / Purple: Đối tượng thứ hai / con trỏ so sánh (j, right)
  COMPARING: "#f59e0b",      // Vàng / Amber: Thao tác so sánh, tính toán biểu thức (sum, compare, mid)
  SUCCESS: "#01e599",        // Xanh lá / Green: Thay đổi vừa hoàn tất, nghiệm tìm thấy, sorted
  ELIMINATED: "#64748b",     // Xám / Dimmed: Vùng bị loại, phần tử ngoài khoảng xét
  DEFAULT: "#8b5cf6",        // Tím sẫm mặc định: Phần tử bình thường trong cấu trúc
  ERROR: "#ef4444",          // Đỏ: Lỗi hoặc không tìm thấy
};

/**
 * Transform an execution event into a complete, displayable visual frame.
 */
export function createVisualFrame({
  stepIndex,
  actionType,
  activeLine,
  codeStatement,
  annotation,
  items = [],             // Mảng các ô vuông: [{ id, value, pos, state: 'default' | 'cyan' | 'purple' | 'amber' | 'green' | 'dimmed' }]
  pointers = {},          // Con trỏ: { i: 0, j: 1, left: 0, right: 3, mid: 1 }
  dimmedIndices = [],     // Vùng bị loại
  matchedIndices = [],    // Vùng nghiệm / hoàn tất
  scalarVars = [],        // Biến đơn: [{ name: 'target', value: 9 }]
  hashMapData = null,     // Hash map: [{ key: 2, value: 0, state: 'active' | 'added' | 'default' }]
  countsTableData = null, // Bảng đếm tần suất: [{ label: 'a', count: 3, state: 'active' | 'default' }]
  resultData = null,      // Kết quả: { label: 'result', value: '[0, 1]' }
  actionBadge = "",
}) {
  return {
    stepIndex,
    actionType,
    activeLine,
    codeStatement,
    annotation,
    items: items.map((it) => ({ ...it })),
    pointers: { ...pointers },
    dimmedIndices: [...dimmedIndices],
    matchedIndices: [...matchedIndices],
    scalarVars: [...scalarVars],
    hashMapData: hashMapData ? hashMapData.map((d) => ({ ...d })) : null,
    countsTableData: countsTableData ? countsTableData.map((d) => ({ ...d })) : null,
    resultData: resultData ? { ...resultData } : null,
    actionBadge,
  };
}
