export function javaFilename(source) {
  const name = source?.match(/\bpublic\s+(?:final\s+)?class\s+(\w+)/)?.[1]
    || source?.match(/\bclass\s+(\w+)/)?.[1] || "Main";
  return `${name}.java`;
}

export function formatRuntimeValue(variable) {
  if (variable.visualValue !== undefined && variable.visualValue !== null) {
    return typeof variable.visualValue === "object"
      ? JSON.stringify(variable.visualValue, null, 2) : String(variable.visualValue);
  }
  return variable.runtimeValue ?? "unavailable";
}

export function sequenceValues(variable) {
  if (Array.isArray(variable.visualValue)) return variable.visualValue;
  if (variable.visualType === "string" && typeof variable.visualValue === "string") {
    return [...variable.visualValue];
  }
  return null;
}

// Only label indices that actually belong to this array/string in the source or AST visual plan.
export function pointersForVariable(variable, step, source) {
  const name = variable.variableName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const references = new RegExp(`\\b${name}(?:\\[\\s*([a-zA-Z_]\\w*)\\s*\\]|\\.charAt\\(\\s*([a-zA-Z_]\\w*)\\s*\\))`, "g");
  const labels = new Set([...source.matchAll(references)].map((match) => match[1] || match[2]));
  for (const state of step?.visualStates || []) {
    if (state.array?.variableName !== variable.variableName) continue;
    for (const pointer of Object.values(state.pointers || {})) {
      if (pointer.variableName) labels.add(pointer.variableName);
    }
  }
  return (step?.variables || []).filter((item) =>
    labels.has(item.variableName) && Number.isInteger(item.visualValue),
  ).map((item) => ({ name: item.variableName, index: item.visualValue }));
}

/**
 * Generate high-fidelity visual trace for Two Sum algorithm.
 * Each step precisely mirrors the execution of Java code line-by-line,
 * illuminating the corresponding square cells as the algorithm progresses.
 */
/**
 * Generate high-fidelity visual trace for Two Sum algorithm.
 * Uses a challenging input [3, 2, 9, 4, 7] with target 9 to showcase multi-step iterations,
 * pointer movement, operator badge animation (+), and variable initialization scoping.
 */
export function generateTwoSumTrace(initialNums = [3, 2, 9, 4, 7], target = 9) {
  // If input is still the old trivial [2, 7, 11, 15], upgrade to challenging [3, 2, 9, 4, 7]
  const isOldTrivial = Array.isArray(initialNums) &&
    initialNums.length === 4 &&
    initialNums[0] === 2 && initialNums[1] === 7 && initialNums[2] === 11 && initialNums[3] === 15;
  const nums = isOldTrivial || !Array.isArray(initialNums) || initialNums.length === 0
    ? [3, 2, 9, 4, 7]
    : initialNums;

  const items = nums.map((val, idx) => ({ id: idx, value: val, pos: idx }));
  const cloneItems = () => items.map((it) => ({ ...it }));
  const steps = [];
  let opCount = 0;

  // Step 1: Line 55: int[] nums = {3, 2, 9, 4, 7};
  // CHƯA khởi tạo target hay các biến khác -> scalarVars rỗng!
  steps.push({
    stepIndex: 0,
    items: cloneItems(),
    nums: [...nums],
    type: "IDLE",
    status: "idle",
    leftIndex: -1,
    rightIndex: -1,
    activeIndices: [],
    comparingIndices: [],
    matchedIndices: [],
    pointers: {},
    activeLine: 55,
    comparison: "Khởi tạo mảng dữ liệu",
    annotation: `Line 55: Khởi tạo mảng nums gồm ${nums.length} phần tử: [${nums.join(", ")}].`,
    context: `Khởi tạo mảng nums = [${nums.join(", ")}]`,
    operationCount: 0,
    scalarVars: [],
    resultData: { label: "result", value: null, status: "pending" },
    operatorInfo: null,
  });

  // Step 2: Line 56: int target = 9;
  // Biến target BẮT ĐẦU XUẤT HIỆN tại bước này!
  steps.push({
    stepIndex: steps.length,
    items: cloneItems(),
    nums: [...nums],
    type: "IDLE",
    status: "idle",
    leftIndex: -1,
    rightIndex: -1,
    activeIndices: [],
    comparingIndices: [],
    matchedIndices: [],
    pointers: {},
    activeLine: 56,
    comparison: `Khởi tạo mục tiêu: target = ${target}`,
    annotation: `Line 56: Thiết lập giá trị mục tiêu target = ${target}. Thuật toán cần tìm 2 phần tử có tổng đúng bằng ${target}.`,
    context: `Thiết lập giá trị mục tiêu target = ${target}`,
    operationCount: 0,
    scalarVars: [{ name: "target", value: target, newlyCreated: true }],
    resultData: { label: "result", value: null, status: "pending" },
    operatorInfo: null,
  });

  // Step 3: Line 57: int[] result = twoSum(nums, target);
  steps.push({
    stepIndex: steps.length,
    items: cloneItems(),
    nums: [...nums],
    type: "IDLE",
    status: "idle",
    leftIndex: -1,
    rightIndex: -1,
    activeIndices: [],
    comparingIndices: [],
    matchedIndices: [],
    pointers: {},
    activeLine: 57,
    comparison: "Bắt đầu gọi hàm twoSum()",
    annotation: `Line 57: Bắt đầu gọi phương thức twoSum(nums, target) để tìm kiếm cặp nghiệm thỏa mãn.`,
    context: `Gọi hàm twoSum(nums, target)`,
    operationCount: 0,
    scalarVars: [{ name: "target", value: target }],
    resultData: { label: "result", value: null, status: "pending" },
    operatorInfo: null,
  });

  for (let i = 0; i < nums.length - 1; i++) {
    const valI = nums[i];

    // Step: Line 62: for (int i = ... ) -> Vòng lặp ngoài duyệt i
    // Biến i XUẤT HIỆN / CẬP NHẬT tại đây!
    steps.push({
      stepIndex: steps.length,
      items: cloneItems(),
      nums: [...nums],
      type: "POINTER_MOVE",
      status: "compare",
      leftIndex: i,
      rightIndex: -1,
      activeIndices: [i],
      comparingIndices: [i],
      matchedIndices: [],
      pointers: { i },
      activeLine: 62,
      comparison: `Xét phần tử thứ nhất: nums[${i}] = ${valI}`,
      annotation: `Line 62: Vòng lặp ngoài: Chọn phần tử thứ nhất tại chỉ số i = ${i} có giá trị ${valI} (sáng Xanh Cyan).`,
      context: `Duyệt con trỏ i = ${i} (nums[${i}] = ${valI})`,
      operationCount: opCount,
      scalarVars: [
        { name: "target", value: target },
        { name: "i", value: i, newlyCreated: true },
      ],
      resultData: { label: "result", value: null, status: "pending" },
      operatorInfo: null,
    });

    for (let j = i + 1; j < nums.length; j++) {
      opCount++;
      const valJ = nums[j];
      const sum = valI + valJ;
      const isMatch = sum === target;

      const pairInfo = {
        leftItemId: i,
        rightItemId: j,
        valLeft: valI,
        valRight: valJ,
        idxLeft: i,
        idxRight: j,
      };

      // Step: Line 63: for (int j = ... ) -> Vòng lặp trong duyệt j
      // Biến j XUẤT HIỆN / CẬP NHẬT tại đây!
      steps.push({
        stepIndex: steps.length,
        items: cloneItems(),
        nums: [...nums],
        type: "POINTER_MOVE",
        status: "compare",
        leftIndex: i,
        rightIndex: j,
        activeIndices: [i, j],
        comparingIndices: [i, j],
        matchedIndices: [],
        activePair: pairInfo,
        pointers: { i, j },
        activeLine: 63,
        comparison: `Xét phần tử thứ hai: nums[${j}] = ${valJ}`,
        annotation: `Line 63: Vòng lặp trong: Chọn phần tử thứ hai tại chỉ số j = ${j} có giá trị ${valJ} (sáng Tím Purple).`,
        context: `Duyệt con trỏ j = ${j} (nums[${j}] = ${valJ})`,
        operationCount: opCount,
        scalarVars: [
          { name: "target", value: target },
          { name: "i", value: i },
          { name: "j", value: j, newlyCreated: true },
        ],
        resultData: { label: "result", value: null, status: "pending" },
        operatorInfo: null,
      });

      // Step: Line 64: int sum = nums[i] + nums[j];
      // ANIMATION PHÉP CỘNG (+): Biến sum XUẤT HIỆN, badge dấu (+) hiện giữa 2 ô i và j!
      steps.push({
        stepIndex: steps.length,
        items: cloneItems(),
        nums: [...nums],
        type: "COMPARE",
        status: "compare",
        leftIndex: i,
        rightIndex: j,
        activeIndices: [i, j],
        comparingIndices: [i, j],
        matchedIndices: [],
        activePair: pairInfo,
        pointers: { i, j },
        activeLine: 64,
        comparison: `Phép cộng: nums[${i}] (${valI}) + nums[${j}] (${valJ}) = ${sum}`,
        annotation: `Line 64: Thực hiện phép cộng hai phần tử đang chọn: nums[${i}] (${valI}) + nums[${j}] (${valJ}) = ${sum}.`,
        context: `Tính tổng: ${valI} + ${valJ} = ${sum}`,
        operationCount: opCount,
        scalarVars: [
          { name: "target", value: target },
          { name: "i", value: i },
          { name: "j", value: j },
          { name: "sum", value: sum, newlyCreated: true },
        ],
        resultData: { label: "result", value: null, status: "pending" },
        operatorInfo: {
          type: "ADD",
          symbol: "+",
          leftIdx: i,
          rightIdx: j,
          leftVal: valI,
          rightVal: valJ,
          resultVal: sum,
          expr: `${valI} + ${valJ} = ${sum}`,
        },
      });

      // Step: Line 65: if (sum == target)
      // ANIMATION SO SÁNH (== / ≠)
      steps.push({
        stepIndex: steps.length,
        items: cloneItems(),
        nums: [...nums],
        type: isMatch ? "MATCHED" : "COMPARE",
        status: isMatch ? "matched" : "compare",
        leftIndex: i,
        rightIndex: j,
        activeIndices: [i, j],
        comparingIndices: [i, j],
        matchedIndices: isMatch ? [i, j] : [],
        activePair: pairInfo,
        pointers: { i, j },
        activeLine: 65,
        comparison: isMatch
          ? `Tổng ${sum} == target ${target} (Khớp hoàn toàn!)`
          : `Tổng ${sum} ≠ target ${target} (Chưa khớp)`,
        annotation: isMatch
          ? `Line 65: So sánh tổng ${sum} == target ${target} → Bằng nhau! Tìm thấy cặp nghiệm tại chỉ số [${i}, ${j}].`
          : `Line 65: So sánh tổng ${sum} ≠ target ${target} → Chưa bằng. Tiếp tục dịch con trỏ j sang phần tử kế tiếp.`,
        context: isMatch ? `Tổng ${sum} bằng target ${target}` : `Tổng ${sum} khác target ${target}`,
        operationCount: opCount,
        scalarVars: [
          { name: "target", value: target },
          { name: "i", value: i },
          { name: "j", value: j },
          { name: "sum", value: sum },
        ],
        resultData: isMatch
          ? { label: "result", value: `[${i}, ${j}]`, status: "found", leftIdx: i, rightIdx: j }
          : { label: "result", value: null, status: "pending" },
        operatorInfo: {
          type: "COMPARE",
          symbol: isMatch ? "==" : "≠",
          leftIdx: i,
          rightIdx: j,
          leftVal: sum,
          rightVal: target,
          expr: `${sum} ${isMatch ? "==" : "≠"} ${target}`,
          isMatch,
        },
      });

      if (isMatch) {
        // Step: Line 66: return new int[] {i, j};
        // Biến result XUẤT HIỆN, vùng kết quả bừng sáng XANH LÁ NEON!
        steps.push({
          stepIndex: steps.length,
          items: cloneItems(),
          nums: [...nums],
          type: "MATCHED",
          status: "matched",
          leftIndex: i,
          rightIndex: j,
          activeIndices: [i, j],
          comparingIndices: [],
          matchedIndices: [i, j],
          activePair: pairInfo,
          pointers: { i, j },
          activeLine: 66,
          comparison: `Tìm thấy cặp nghiệm tại vị trí [${i}, ${j}]!`,
          annotation: `Line 66: Hai phần tử nums[${i}] (${valI}) và nums[${j}] (${valJ}) phát sáng Xanh Lá Neon! Trả về mảng nghiệm new int[] {${i}, ${j}}.`,
          context: `Trả về kết quả cặp chỉ số [${i}, ${j}]`,
          operationCount: opCount,
          scalarVars: [
            { name: "target", value: target },
            { name: "i", value: i },
            { name: "j", value: j },
            { name: "sum", value: sum },
            { name: "result", value: `[${i}, ${j}]`, newlyCreated: true },
          ],
          resultData: {
            label: "result",
            value: `[${i}, ${j}]`,
            status: "found",
            leftIdx: i,
            rightIdx: j,
            formula: `nums[${i}] (${valI}) + nums[${j}] (${valJ}) = ${sum}`,
          },
          operatorInfo: null,
        });

        // Step: Line 57: int[] result = twoSum(nums, target);
        steps.push({
          stepIndex: steps.length,
          items: cloneItems(),
          nums: [...nums],
          type: "COMPLETE",
          status: "complete",
          leftIndex: -1,
          rightIndex: -1,
          activeIndices: [i, j],
          comparingIndices: [],
          matchedIndices: [i, j],
          pointers: {},
          activeLine: 57,
          comparison: `Gán kết quả: result = [${i}, ${j}]`,
          annotation: `Line 57: Hàm twoSum() hoàn tất và trả về cặp nghiệm [${i}, ${j}], gán vào biến result.`,
          context: `Gán kết quả result = [${i}, ${j}]`,
          operationCount: opCount,
          scalarVars: [
            { name: "target", value: target },
            { name: "result", value: `[${i}, ${j}]` },
          ],
          resultData: {
            label: "result",
            value: `[${i}, ${j}]`,
            status: "found",
            leftIdx: i,
            rightIdx: j,
            formula: `nums[${i}] (${valI}) + nums[${j}] (${valJ}) = ${sum}`,
          },
          operatorInfo: null,
        });

        // Step: Line 58: System.out.println(result[0] + " " + result[1]);
        steps.push({
          stepIndex: steps.length,
          items: cloneItems(),
          nums: [...nums],
          type: "COMPLETE",
          status: "complete",
          leftIndex: -1,
          rightIndex: -1,
          activeIndices: [i, j],
          comparingIndices: [],
          matchedIndices: [i, j],
          pointers: {},
          activeLine: 58,
          comparison: `In kết quả ra màn hình: "${i} ${j}"`,
          annotation: `Line 58: In ra màn hình cặp chỉ số nghiệm: "${i} ${j}". Hai ô nums[${i}] và nums[${j}] sáng xanh lá hoàn tất!`,
          context: `In kết quả nghiệm: ${i} ${j}`,
          operationCount: opCount,
          scalarVars: [
            { name: "target", value: target },
            { name: "result", value: `[${i}, ${j}]` },
          ],
          resultData: {
            label: "result",
            value: `[${i}, ${j}]`,
            status: "found",
            printed: `${i} ${j}`,
            formula: `nums[${i}] (${valI}) + nums[${j}] (${valJ}) = ${sum}`,
          },
          operatorInfo: null,
        });

        return steps;
      }
    }
  }

  // A successful match returns above; reaching here means all pairs were exhausted.
  steps.push({
      stepIndex: steps.length,
      items: cloneItems(),
      nums: [...nums],
      type: "COMPLETE",
      status: "complete",
      leftIndex: -1,
      rightIndex: -1,
      activeIndices: [],
      comparingIndices: [],
      matchedIndices: [],
      pointers: {},
      activeLine: 70,
      comparison: "Không tìm thấy cặp nghiệm",
      annotation: `Line 70: Đã duyệt hết tất cả các cặp phần tử mà không tìm thấy tổng bằng ${target}. Trả về [-1, -1].`,
      context: `Không tìm thấy cặp thỏa mãn, trả về [-1, -1]`,
      operationCount: opCount,
      scalarVars: [
        { name: "target", value: target },
        { name: "result", value: "[-1, -1]" },
      ],
      resultData: { label: "result", value: "[-1, -1]", status: "not_found" },
      operatorInfo: null,
    });

  return steps;
}


/**
 * Generate high-fidelity visual trace for Binary Search algorithm.
 */
export function generateBinarySearchTrace(initialNums = [-1, 0, 3, 5, 9, 12], target = 9) {
  const nums = Array.isArray(initialNums) && initialNums.length ? initialNums : [-1, 0, 3, 5, 9, 12];
  const items = nums.map((val, idx) => ({ id: idx, value: val, pos: idx }));
  const cloneItems = () => items.map((it) => ({ ...it }));
  const steps = [];
  let left = 0;
  let right = nums.length - 1;
  let opCount = 0;

  steps.push({
    stepIndex: 0,
    items: cloneItems(),
    nums: [...nums],
    type: "IDLE",
    status: "idle",
    leftIndex: left,
    rightIndex: right,
    midIndex: -1,
    comparingIndices: [],
    matchedIndices: [],
    dimmedIndices: [],
    pointers: { left, right },
    activeLine: 55,
    comparison: "Khởi tạo",
    annotation: `Mảng đã sắp xếp: [${nums.join(", ")}], tìm target = ${target}. Khoảng tìm kiếm: [0..${right}].`,
    context: `Line 55: int target = ${target}; binarySearch(nums, target);`,
    operationCount: 0,
    scalarVars: [
      { name: "target", value: target },
      { name: "left", value: left },
      { name: "right", value: right },
    ],
  });

  while (left <= right) {
    opCount++;
    const mid = Math.floor((left + right) / 2);
    const valMid = nums[mid];
    const dimmed = [];
    for (let k = 0; k < nums.length; k++) {
      if (k < left || k > right) dimmed.push(k);
    }

    const pairInfo = {
      leftItemId: left,
      rightItemId: right,
      midItemId: mid,
      valLeft: nums[left],
      valRight: nums[right],
      valMid,
      idxLeft: left,
      idxRight: right,
      idxMid: mid,
    };

    steps.push({
      stepIndex: steps.length,
      items: cloneItems(),
      nums: [...nums],
      type: "COMPARE",
      status: "compare",
      leftIndex: left,
      rightIndex: right,
      midIndex: mid,
      comparingIndices: [mid],
      matchedIndices: [],
      dimmedIndices: dimmed,
      activePair: pairInfo,
      pointers: { left, right, mid },
      activeLine: 64,
      comparison: `mid = ${mid} (nums[${mid}] = ${valMid}) vs Target ${target}`,
      annotation: `Tính điểm giữa: mid = (${left} + ${right}) / 2 = ${mid}. Giá trị nums[${mid}] = ${valMid} (Cam). So sánh với target ${target}.`,
      context: `Line 64: int mid = left + (right - left) / 2; // mid = ${mid}, val = ${valMid}`,
      operationCount: opCount,
      scalarVars: [
        { name: "target", value: target },
        { name: "left", value: left },
        { name: "right", value: right },
        { name: "mid", value: mid },
      ],
    });

    if (valMid === target) {
      steps.push({
        stepIndex: steps.length,
        items: cloneItems(),
        nums: [...nums],
        type: "MATCHED",
        status: "matched",
        leftIndex: left,
        rightIndex: right,
        midIndex: mid,
        comparingIndices: [],
        matchedIndices: [mid],
        dimmedIndices: dimmed,
        activePair: pairInfo,
        pointers: { mid },
        activeLine: 66,
        comparison: `🎯 TÌM THẤY TẠI VỊ TRÍ [${mid}]`,
        annotation: `🎯 nums[${mid}] = ${valMid} bằng đúng target = ${target}! Trả về chỉ số vị trí tìm thấy là ${mid}.`,
        context: `Line 66: return mid; // Tìm thấy target tại index ${mid}`,
        operationCount: opCount,
        scalarVars: [
          { name: "target", value: target },
          { name: "result", value: mid },
        ],
      });

      steps.push({
        stepIndex: steps.length,
        items: cloneItems(),
        nums: [...nums],
        type: "COMPLETE",
        status: "complete",
        leftIndex: -1,
        rightIndex: -1,
        midIndex: mid,
        comparingIndices: [],
        matchedIndices: [mid],
        dimmedIndices: dimmed,
        pointers: { mid },
        activeLine: 67,
        comparison: `Hoàn tất: index = ${mid}`,
        annotation: `Tìm kiếm nhị phân thành công sau ${opCount} bước chia đôi. Kết quả vị trí: ${mid}.`,
        context: `Line 67: Kết thúc tìm kiếm nhị phân.`,
        operationCount: opCount,
        scalarVars: [
          { name: "target", value: target },
          { name: "result", value: mid },
        ],
      });

      return steps;
    }

    if (valMid < target) {
      steps.push({
        stepIndex: steps.length,
        items: cloneItems(),
        nums: [...nums],
        type: "UPDATE",
        status: "update",
        leftIndex: left,
        rightIndex: right,
        midIndex: mid,
        comparingIndices: [mid],
        matchedIndices: [],
        dimmedIndices: dimmed,
        activePair: pairInfo,
        pointers: { left, right, mid },
        activeLine: 69,
        comparison: `${valMid} < ${target} → Thu hẹp nửa trái (left = ${mid + 1})`,
        annotation: `Vì nums[${mid}] = ${valMid} < ${target}, loại bỏ nửa trái. Dịch con trỏ trái left = mid + 1 = ${mid + 1}.`,
        context: `Line 69: left = mid + 1; // left cập nhật thành ${mid + 1}`,
        operationCount: opCount,
        scalarVars: [
          { name: "target", value: target },
          { name: "left", value: mid + 1 },
          { name: "right", value: right },
          { name: "mid", value: mid },
        ],
      });
      left = mid + 1;
    } else {
      steps.push({
        stepIndex: steps.length,
        items: cloneItems(),
        nums: [...nums],
        type: "UPDATE",
        status: "update",
        leftIndex: left,
        rightIndex: right,
        midIndex: mid,
        comparingIndices: [mid],
        matchedIndices: [],
        dimmedIndices: dimmed,
        activePair: pairInfo,
        pointers: { left, right, mid },
        activeLine: 72,
        comparison: `${valMid} > ${target} → Thu hẹp nửa phải (right = ${mid - 1})`,
        annotation: `Vì nums[${mid}] = ${valMid} > ${target}, loại bỏ nửa phải. Dịch con trỏ phải right = mid - 1 = ${mid - 1}.`,
        context: `Line 72: right = mid - 1; // right cập nhật thành ${mid - 1}`,
        operationCount: opCount,
        scalarVars: [
          { name: "target", value: target },
          { name: "left", value: left },
          { name: "right", value: mid - 1 },
          { name: "mid", value: mid },
        ],
      });
      right = mid - 1;
    }
  }

  steps.push({
    stepIndex: steps.length,
    items: cloneItems(),
    nums: [...nums],
    type: "COMPLETE",
    status: "complete",
    leftIndex: -1,
    rightIndex: -1,
    comparingIndices: [],
    matchedIndices: [],
    dimmedIndices: nums.map((_, i) => i),
    pointers: { left, right },
    activeLine: 75,
    comparison: "Không tìm thấy (-1)",
    annotation: `Không tìm thấy giá trị ${target} trong mảng sau ${opCount} bước chia đôi. Trả về -1.`,
    context: `Line 75: return -1;`,
    operationCount: opCount,
    scalarVars: [
      { name: "target", value: target },
      { name: "result", value: -1 },
    ],
  });

  return steps;
}

/**
 * Generate high-fidelity visual trace for Two Sum using Hash Map:
 * Map complement -> index. Shows both Array square cells and Hash Map table.
 */
export function generateTwoSumHashMapTrace(initialNums = [2, 7, 11, 15], target = 9) {
  const nums = Array.isArray(initialNums) && initialNums.length ? initialNums : [2, 7, 11, 15];
  const items = nums.map((val, idx) => ({ id: idx, value: val, pos: idx }));
  const cloneItems = () => items.map((it) => ({ ...it }));
  const steps = [];
  const mapEntries = []; // [{ key, value, state }]

  // Step 1: Khởi tạo mảng và HashMap
  steps.push({
    stepIndex: 0,
    items: cloneItems(),
    nums: [...nums],
    type: "IDLE",
    status: "idle",
    activeIndices: [],
    matchedIndices: [],
    pointers: {},
    activeLine: 55,
    comparison: "Khởi tạo mảng và Map",
    annotation: `Khởi tạo mảng nums gồm ${nums.length} phần tử và bảng băm Map<Integer, Integer> map = new HashMap<>().`,
    context: `Map<Integer, Integer> map = new HashMap<>();`,
    operationCount: 0,
    scalarVars: [{ name: "target", value: target }],
    hashMapData: [],
  });

  for (let i = 0; i < nums.length; i++) {
    const val = nums[i];
    const complement = target - val;

    // Step: Đang xét nums[i]
    steps.push({
      stepIndex: steps.length,
      items: cloneItems(),
      nums: [...nums],
      type: "POINTER_MOVE",
      status: "compare",
      activeIndices: [i],
      matchedIndices: [],
      pointers: { i },
      activeLine: 62,
      comparison: `i = ${i} (nums[${i}] = ${val})`,
      annotation: `Line 62: Xét phần tử thứ i = ${i} (nums[${i}] = ${val}). Ô [${i}] sáng lên (Xanh Cyan).`,
      context: `for (int i = 0; i < nums.length; i++) // i = ${i}, val = ${val}`,
      operationCount: i + 1,
      scalarVars: [
        { name: "target", value: target },
        { name: "i", value: i },
        { name: "val", value: val },
      ],
      hashMapData: mapEntries.map((e) => ({ ...e, state: "default" })),
    });

    // Step: Tính complement
    steps.push({
      stepIndex: steps.length,
      items: cloneItems(),
      nums: [...nums],
      type: "COMPARE",
      status: "compare",
      activeIndices: [i],
      matchedIndices: [],
      pointers: { i },
      activeLine: 63,
      comparison: `complement = ${target} - ${val} = ${complement}`,
      annotation: `Line 63: Tính số bù complement = target (${target}) - nums[${i}] (${val}) = ${complement}.`,
      context: `int complement = target - nums[i]; // complement = ${complement}`,
      operationCount: i + 1,
      scalarVars: [
        { name: "target", value: target },
        { name: "i", value: i },
        { name: "complement", value: complement },
      ],
      hashMapData: mapEntries.map((e) => ({ ...e, state: "default" })),
    });

    // Step: Tra cứu map.containsKey(complement)
    const foundEntry = mapEntries.find((e) => e.key === complement);
    if (foundEntry) {
      // Map lookup MATCHED!
      steps.push({
        stepIndex: steps.length,
        items: cloneItems(),
        nums: [...nums],
        type: "MAP_LOOKUP",
        status: "compare",
        activeIndices: [i],
        matchedIndices: [],
        pointers: { i },
        activeLine: 64,
        comparison: `map.containsKey(${complement}) == true`,
        annotation: `Line 64: Tra cứu Hash Map thấy khóa ${complement} (vàng) đã tồn tại với chỉ số index = ${foundEntry.value}!`,
        context: `if (map.containsKey(complement)) // Tìm thấy key ${complement}!`,
        operationCount: i + 1,
        scalarVars: [
          { name: "target", value: target },
          { name: "complement", value: complement },
        ],
        hashMapData: mapEntries.map((e) => ({
          ...e,
          state: e.key === complement ? "active" : "default",
        })),
      });

      // Step: Trả về kết quả
      const resIndices = [foundEntry.value, i];
      steps.push({
        stepIndex: steps.length,
        items: cloneItems(),
        nums: [...nums],
        type: "RESULT_PRODUCE",
        status: "matched",
        activeIndices: resIndices,
        matchedIndices: resIndices,
        pointers: {},
        activeLine: 65,
        comparison: `✓ return new int[] {${foundEntry.value}, ${i}}`,
        annotation: `Line 65: Hai ô nghiệm [${foundEntry.value}] và [${i}] sáng Xanh Lá Neon! Tìm thấy nghiệm [${foundEntry.value}, ${i}].`,
        context: `return new int[] { map.get(complement), i }; // [${foundEntry.value}, ${i}]`,
        operationCount: i + 1,
        scalarVars: [
          { name: "target", value: target },
          { name: "result", value: `[${foundEntry.value}, ${i}]` },
        ],
        hashMapData: mapEntries.map((e) => ({
          ...e,
          state: e.key === complement ? "matched" : "default",
        })),
        resultData: { label: "result", value: `[${foundEntry.value}, ${i}]` },
      });

      return steps;
    } else {
      // Chưa có trong map
      steps.push({
        stepIndex: steps.length,
        items: cloneItems(),
        nums: [...nums],
        type: "MAP_LOOKUP",
        status: "compare",
        activeIndices: [i],
        matchedIndices: [],
        pointers: { i },
        activeLine: 64,
        comparison: `map.containsKey(${complement}) == false`,
        annotation: `Line 64: Khóa ${complement} chưa có trong Hash Map. Chuẩn bị đưa nums[${i}] = ${val} vào Map.`,
        context: `if (map.containsKey(complement)) // Chưa có key ${complement}`,
        operationCount: i + 1,
        scalarVars: [
          { name: "target", value: target },
          { name: "complement", value: complement },
        ],
        hashMapData: mapEntries.map((e) => ({ ...e, state: "default" })),
      });

      // Step: map.put(nums[i], i)
      mapEntries.push({ key: val, value: i, state: "added" });
      steps.push({
        stepIndex: steps.length,
        items: cloneItems(),
        nums: [...nums],
        type: "MAP_PUT",
        status: "update",
        activeIndices: [i],
        matchedIndices: [],
        pointers: { i },
        activeLine: 67,
        comparison: `map.put(${val}, ${i})`,
        annotation: `Line 67: Thêm cặp { Khóa: ${val} → Giá trị: ${i} } vào Hash Map (sáng Xanh Lá).`,
        context: `map.put(nums[i], i); // map[${val}] = ${i}`,
        operationCount: i + 1,
        scalarVars: [
          { name: "target", value: target },
          { name: "i", value: i },
        ],
        hashMapData: mapEntries.map((e) => ({
          ...e,
          state: e.key === val ? "added" : "default",
        })),
      });
    }
  }

  return steps;
}

/**
 * Generate high-fidelity visual trace for Valid Anagram using Frequency Table (Bảng đếm tần suất).
 */
export function generateAnagramTrace(s = "anagram", t = "nagaram") {
  const sArr = [...s];
  const tArr = [...t];
  const items = sArr.map((ch, idx) => ({ id: idx, value: ch, pos: idx }));
  const cloneItems = () => items.map((it) => ({ ...it }));
  const steps = [];

  // Tạo bảng đếm tần suất các ký tự xuất hiện trong s và t
  const uniqueChars = Array.from(new Set([...sArr, ...tArr])).sort();
  const countsMap = {};
  uniqueChars.forEach((ch) => { countsMap[ch] = 0; });

  const getCountsTableData = (activeCh = null) => {
    return uniqueChars.map((ch) => ({
      char: ch,
      count: countsMap[ch],
      state: ch === activeCh ? "active" : "default",
    }));
  };

  steps.push({
    stepIndex: 0,
    items: cloneItems(),
    nums: sArr,
    type: "IDLE",
    status: "idle",
    activeIndices: [],
    matchedIndices: [],
    pointers: {},
    activeLine: 88,
    comparison: "Khởi tạo bảng đếm tần suất",
    annotation: `Khởi tạo chuỗi s="${s}", t="${t}" và bảng đếm tần suất counts cho các ký tự [${uniqueChars.join(", ")}].`,
    context: `int[] counts = new int[26];`,
    operationCount: 0,
    scalarVars: [
      { name: "s", value: `"${s}"` },
      { name: "t", value: `"${t}"` },
    ],
    countsTableData: getCountsTableData(),
  });

  for (let i = 0; i < s.length; i++) {
    const chS = s[i];
    const chT = t[i];

    // Đọc ký tự s[i] và tăng tần suất
    countsMap[chS]++;
    steps.push({
      stepIndex: steps.length,
      items: cloneItems(),
      nums: sArr,
      type: "COUNT_UPDATE",
      status: "update",
      activeIndices: [i],
      matchedIndices: [],
      pointers: { i },
      activeLine: 91,
      comparison: `s[${i}] = '${chS}' → counts['${chS}'] tăng lên ${countsMap[chS]}`,
      annotation: `Line 91: Đọc s[${i}]='${chS}' (Xanh Cyan) nối tới ô '${chS}' trong bảng đếm → tăng count thành ${countsMap[chS]}.`,
      context: `counts[s.charAt(i) - 'a']++; // counts['${chS}'] = ${countsMap[chS]}`,
      operationCount: (i * 2) + 1,
      scalarVars: [
        { name: "i", value: i },
        { name: "charS", value: `'${chS}'` },
      ],
      countsTableData: getCountsTableData(chS),
    });

    // Đọc ký tự t[i] và giảm tần suất
    countsMap[chT]--;
    steps.push({
      stepIndex: steps.length,
      items: cloneItems(),
      nums: sArr,
      type: "COUNT_UPDATE",
      status: "update",
      activeIndices: [i],
      matchedIndices: [],
      pointers: { i },
      activeLine: 92,
      comparison: `t[${i}] = '${chT}' → counts['${chT}'] giảm xuống ${countsMap[chT]}`,
      annotation: `Line 92: Đọc t[${i}]='${chT}' (Tím Purple) nối tới ô '${chT}' trong bảng đếm → giảm count thành ${countsMap[chT]}.`,
      context: `counts[t.charAt(i) - 'a']--; // counts['${chT}'] = ${countsMap[chT]}`,
      operationCount: (i * 2) + 2,
      scalarVars: [
        { name: "i", value: i },
        { name: "charT", value: `'${chT}'` },
      ],
      countsTableData: getCountsTableData(chT),
    });
  }

  const isAnagram = Object.values(countsMap).every((c) => c === 0);

  steps.push({
    stepIndex: steps.length,
    items: cloneItems(),
    nums: sArr,
    type: "RESULT_PRODUCE",
    status: "complete",
    activeIndices: items.map((_, idx) => idx),
    matchedIndices: items.map((_, idx) => idx),
    pointers: {},
    activeLine: 102,
    comparison: isAnagram ? "✓ Valid Anagram: true" : "✗ Not Anagram: false",
    annotation: isAnagram
      ? `Line 102: Mọi ký tự trong bảng đếm tần suất đều triệt tiêu về 0 → "${s}" và "${t}" là đảo chữ hợp lệ (true)!`
      : `Line 102: Tần suất ký tự không triệt tiêu → Không phải đảo chữ (false)!`,
    context: `return true; // Tất cả tần suất đều bằng 0`,
    operationCount: s.length * 2,
    scalarVars: [{ name: "isAnagram", value: String(isAnagram) }],
    countsTableData: getCountsTableData(),
    resultData: { label: "isAnagram", value: String(isAnagram) },
  });

  return steps;
}

/**
 * Helper to generate human-readable Vietnamese explanations for arbitrary AST steps,
 * preventing raw Java code from being dumped onto the UI.
 */
export function generateAstExplanation(step, rawCode = "", variables = [], pointersFound = []) {
  const code = (rawCode || step?.code || "").trim();
  const line = step?.lineNumber || 0;

  // 1. Console print statement
  if (code.includes("System.out.println")) {
    const resVar = variables.find((v) => v.variableName === "result" || v.name === "result");
    const resVal = resVar ? formatRuntimeValue(resVar) : "";
    return {
      title: "In kết quả ra màn hình",
      explanation: resVal
        ? `In kết quả nghiệm ${resVal} ra màn hình console. Thuật toán kết thúc!`
        : `In thông tin kết quả thực thi ra màn hình console.`,
      type: "COMPLETE",
      operatorInfo: null,
    };
  }

  // 2. Array initialization
  if (code.includes("int[] nums") || (code.includes("nums") && code.includes("{"))) {
    return {
      title: "Khởi tạo mảng dữ liệu",
      explanation: `Khởi tạo mảng số nguyên đầu vào nums để chuẩn bị thực hiện giải thuật.`,
      type: "IDLE",
      operatorInfo: null,
    };
  }

  // 3. Target setup
  if (code.includes("int target") || code.includes("target =")) {
    const targetVar = variables.find((v) => v.variableName === "target" || v.name === "target");
    const tVal = targetVar ? (targetVar.visualValue ?? targetVar.runtimeValue) : "target";
    return {
      title: `Khởi tạo mục tiêu target = ${tVal}`,
      explanation: `Thiết lập giá trị mục tiêu target = ${tVal}. Thuật toán cần tìm nghiệm thỏa mãn giá trị này.`,
      type: "IDLE",
      operatorInfo: null,
    };
  }

  // 4. Return statement (Matched)
  if (code.includes("return")) {
    if (code.includes("-1")) {
      return {
        title: "Không tìm thấy nghiệm",
        explanation: `Đã duyệt hết các trường hợp mà không tìm thấy kết quả thỏa mãn. Trả về [-1, -1].`,
        type: "COMPLETE",
        operatorInfo: null,
      };
    }
    return {
      title: "Trả về kết quả tìm được",
      explanation: `Đã tìm thấy nghiệm thỏa mãn điều kiện bài toán! Hàm trả về kết quả.`,
      type: "MATCHED",
      operatorInfo: null,
    };
  }

  // 5. Addition (sum calculation)
  if (code.includes("+") && (code.includes("nums[") || code.includes("sum"))) {
    const leftPtr = pointersFound[0];
    const rightPtr = pointersFound[1];
    return {
      title: "Tính tổng hai phần tử",
      explanation: `Thực hiện phép cộng hai phần tử đang được con trỏ trỏ tới.`,
      type: "COMPARE",
      operatorInfo: leftPtr && rightPtr ? {
        type: "ADD",
        symbol: "+",
        leftIdx: leftPtr.index,
        rightIdx: rightPtr.index,
        expr: "nums[i] + nums[j]",
      } : null,
    };
  }

  // 6. Subtraction (complement calculation)
  if (code.includes("-") && (code.includes("target") || code.includes("complement"))) {
    return {
      title: "Tính số bù complement",
      explanation: `Lấy giá trị target trừ đi phần tử hiện tại để tìm số bù cần tìm trong Hash Map.`,
      type: "COMPARE",
      operatorInfo: {
        type: "SUBTRACT",
        symbol: "-",
        expr: "target - nums[i]",
      },
    };
  }

  // 7. Comparison (if condition)
  if (code.includes("==") || code.includes("if")) {
    return {
      title: "So sánh điều kiện nghiệm",
      explanation: `Kiểm tra xem giá trị tính toán hiện tại có khớp với điều kiện mục tiêu hay không.`,
      type: "COMPARE",
      operatorInfo: {
        type: "COMPARE",
        symbol: "==",
        expr: "sum == target",
      },
    };
  }

  // 8. Pointer loop
  if (/for\s*\(\s*int\s+([a-zA-Z_]\w*)/.test(code)) {
    const varName = code.match(/for\s*\(\s*int\s+([a-zA-Z_]\w*)/)[1];
    const curVar = variables.find((v) => v.variableName === varName || v.name === varName);
    const val = curVar ? (curVar.visualValue ?? curVar.runtimeValue) : "?";
    return {
      title: `Duyệt con trỏ ${varName} = ${val}`,
      explanation: `Vòng lặp di chuyển con trỏ ${varName} đến vị trí ${val} để xét phần tử.`,
      type: "POINTER_MOVE",
      operatorInfo: null,
    };
  }

  // Fallback explanation
  return {
    title: `Thực thi bước dòng ${line}`,
    explanation: `Thực hiện câu lệnh tại dòng ${line} để cập nhật trạng thái dữ liệu.`,
    type: "COMPARE",
    operatorInfo: null,
  };
}

/**
 * Route problem title & source to the appropriate solution-specific trace generator.
 */
export function buildSimulatedTraceForSolution(problemTitle = "", source = "", customNums = null) {
  const normTitle = (problemTitle || "").toLowerCase();
  const normSource = (source || "").toLowerCase();

  // 1. Binary Search
  if (normTitle.includes("binary search") || normSource.includes("binarysearch")) {
    return generateBinarySearchTrace(customNums || [-1, 0, 3, 5, 9, 12], 9);
  }

  // 2. Valid Anagram with Frequency Table
  if (normTitle.includes("anagram") || normSource.includes("isanagram")) {
    return generateAnagramTrace("anagram", "nagaram");
  }

  // 3. Sorting (Bubble Sort)
  if (
    normTitle.includes("sort") ||
    normSource.includes("bubblesort") ||
    normSource.includes("nums[j] > nums[j + 1]")
  ) {
    return null; // Will fallback to generateBubbleSortTrace
  }

  // 4. Two Sum using Hash Map
  if (normSource.includes("hashmap") || normSource.includes("containskey") || normSource.includes("map.put")) {
    return generateTwoSumHashMapTrace(customNums || [3, 2, 9, 4, 7], 9);
  }

  // 5. Two Sum using Two Pointers / Brute Force
  return generateTwoSumTrace(customNums || [3, 2, 9, 4, 7], 9);
}

/**
 * Adapt arbitrary AST debug execution steps (from AST-tool JDI runner) into rich visual steps.
 */
export function adaptAstStepsToRichVisualizer(astSteps = [], source = "") {
  if (!Array.isArray(astSteps) || astSteps.length === 0) return [];

  let opCount = 0;
  let lastArrayStr = "";

  return astSteps.map((step, index) => {
    opCount++;
    const variables = step.variables || [];
    // Find primary array or sequence variable
    let arrayVar = variables.find((v) => Array.isArray(v.visualValue) && v.visualValue.length > 0);
    if (!arrayVar) {
      arrayVar = variables.find((v) => sequenceValues(v) !== null);
    }

    const rawVals = arrayVar ? sequenceValues(arrayVar) : [];
    const nums = (rawVals || []).map((v) => (typeof v === "number" ? v : String(v)));
    const items = nums.map((v, idx) => ({ id: idx, value: v, pos: idx }));

    const pointersFound = arrayVar ? pointersForVariable(arrayVar, step, source) : [];
    const pointersMap = {};
    pointersFound.forEach((p) => {
      pointersMap[p.name] = p.index;
    });

    const leftIdx = pointersFound[0]?.index ?? -1;
    const rightIdx = pointersFound[1]?.index ?? -1;

    // Detect mutation
    const curArrayStr = JSON.stringify(nums);
    let type = "COMPARE";
    let status = "compare";
    if (lastArrayStr && curArrayStr !== lastArrayStr) {
      type = "UPDATE";
      status = "update";
      lastArrayStr = curArrayStr;
    } else {
      lastArrayStr = curArrayStr;
    }

    const scalarVars = variables
      .filter((v) => !Array.isArray(v.visualValue) && v.variableName !== arrayVar?.variableName)
      .map((v) => ({ name: v.variableName, value: formatRuntimeValue(v) }));

    const exp = generateAstExplanation(step, step.code, variables, pointersFound);
    const finalType = exp.type || type;
    const isMatched = finalType === "MATCHED";
    const isComplete = finalType === "COMPLETE";

    return {
      stepIndex: index,
      items,
      nums,
      type: finalType,
      status: isMatched ? "matched" : isComplete ? "complete" : status,
      leftIndex: leftIdx,
      rightIndex: rightIdx,
      comparingIndices: pointersFound.map((p) => p.index).filter((idx) => idx >= 0),
      matchedIndices: isMatched || isComplete ? pointersFound.map((p) => p.index) : [],
      dimmedIndices: [],
      pointers: pointersMap,
      activeLine: step.lineNumber || 20,
      comparison: exp.title,
      annotation: `Line ${step.lineNumber} · ${exp.title}: ${exp.explanation}`,
      context: `${step.context?.methodName || "main"}() · ${exp.title}`,
      operationCount: opCount,
      scalarVars,
      operatorInfo: exp.operatorInfo || null,
      resultData: isMatched || isComplete ? {
        label: "result",
        value: variables.find((v) => v.variableName === "result")?.visualValue || "Hoàn tất",
        status: "found",
      } : { label: "result", value: null, status: "pending" },
      rawStep: step,
    };
  });
}
