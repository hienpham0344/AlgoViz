/**
 * AlgoViz Course & Lesson Notebook Data
 * Kaggle-style rich educational content with theory, executable code sandboxes, and interactive quizzes.
 */

export const LESSON_CONTENT_REGISTRY = {
  // Course: sorting-masterclass
  "sorting-masterclass": {
    1: {
      id: 1,
      title: "Comparison Invariants & Adjacent Swaps",
      courseId: "sorting-masterclass",
      courseTitle: "Sorting Algorithms Masterclass",
      category: "Sorting & Searching",
      difficulty: "Beginner",
      duration: "25 min",
      type: "concept",
      overview:
        "Understand how pairwise comparisons establish sorted partitions, how inversions measure disorder, and why adjacent swaps form the foundation of elementary sorting.",
      markdownSections: [
        {
          id: "intro",
          title: "1. The Fundamental Sorting Invariant",
          content: `In computer science, an array $A[0 \\dots n-1]$ is sorted in non-decreasing order if and only if every adjacent pair satisfies:

$$A[i] \\le A[i+1] \\quad \\text{for all } 0 \\le i < n - 1$$

If there exists any index $i$ where $A[i] > A[i+1]$, the pair $(A[i], A[i+1])$ is called an **inversion**. 
Every sorting algorithm that operates by comparing adjacent elements is fundamentally **an engine that discovers and eliminates inversions** one swap at a time.`,
        },
        {
          id: "inversions",
          title: "2. Inversions and the Lower Bound of Adjacent Swaps",
          content: `Consider the permutation $[5, 1, 4, 2, 8]$.
Let us enumerate all inversion pairs $(i, j)$ such that $i < j$ and $A[i] > A[j]$:
- $(5, 1)$, $(5, 4)$, $(5, 2)$
- $(4, 2)$

Total inversions: **4**.

> **Theorem (Adjacent Swap Invariant):**
> A single adjacent swap of $A[i]$ and $A[i+1]$ can decrease the total inversion count by at most **1**.
> Therefore, any algorithm that exclusively swaps adjacent elements (such as unoptimized Bubble Sort or Insertion Sort) requires at least $I$ swaps, where $I$ is the number of inversions in the input array. In the worst case (reverse-sorted array), $I = \\frac{n(n-1)}{2} = O(n^2)$.`,
        },
        {
          id: "stability",
          title: "3. The Stability Guarantee of Adjacent Swaps",
          content: `An algorithm is **stable** if elements with identical keys retain their relative original order after sorting.

When we only swap adjacent items under the strict condition:

\`\`\`python
if arr[j] > arr[j + 1]: # STRICT inequality preserves stability!
    arr[j], arr[j + 1] = arr[j + 1], arr[j]
\`\`\`

Two equal elements $arr[j] == arr[j+1]$ will **never** be swapped. Thus, their relative order is guaranteed to be preserved. If we carelessly wrote \`if arr[j] >= arr[j + 1]:\`, we would swap equal elements, immediately destroying stability!`,
        },
      ],
      codeSandbox: {
        language: "python",
        title: "Adjacent Inversion Counter & Swap Simulator",
        description: "Examine how adjacent swaps eliminate inversions step-by-step on an arbitrary array:",
        starterCode: `# Interactive Adjacent Swap Inversion Engine
def count_inversions(arr):
    inversions = []
    n = len(arr)
    for i in range(n):
        for j in range(i + 1, n):
            if arr[i] > arr[j]:
                inversions.append((arr[i], arr[j]))
    return inversions

def resolve_adjacent_swaps(arr):
    a = list(arr)
    steps = []
    swaps = 0
    passes = 0
    n = len(a)
    
    print(f"Initial Array: {a}")
    initial_invs = count_inversions(a)
    print(f"Total Inversions to eliminate: {len(initial_invs)} -> {initial_invs}\\n")
    
    for i in range(n):
        swapped = False
        passes += 1
        for j in range(0, n - i - 1):
            if a[j] > a[j + 1]:
                # Inversion found at adjacent pair
                print(f"Pass {passes}: Swap ({a[j]}, {a[j+1]}) at indices [{j}, {j+1}]")
                a[j], a[j + 1] = a[j + 1], a[j]
                swaps += 1
                swapped = True
        if not swapped:
            print(f"Early stop triggered at pass {passes} (zero inversions remaining)")
            break
            
    print(f"\\nFinal Sorted Array: {a}")
    print(f"Total Adjacent Swaps Executed: {swaps}")
    return a

# Try changing this array to test different permutations!
test_array = [5, 1, 4, 2, 8]
result = resolve_adjacent_swaps(test_array)
`,
        sampleOutput: `Initial Array: [5, 1, 4, 2, 8]
Total Inversions to eliminate: 4 -> [(5, 1), (5, 4), (5, 2), (4, 2)]

Pass 1: Swap (5, 1) at indices [0, 1]
Pass 1: Swap (5, 4) at indices [1, 2]
Pass 1: Swap (5, 2) at indices [2, 3]
Pass 2: Swap (4, 2) at indices [1, 2]
Early stop triggered at pass 3 (zero inversions remaining)

Final Sorted Array: [1, 2, 4, 5, 8]
Total Adjacent Swaps Executed: 4`,
      },
      quizzes: [
        {
          id: "q1",
          question: "If an array has 6 inversions, what is the absolute minimum number of adjacent swaps required to sort it?",
          options: [
            "3 swaps",
            "6 swaps",
            "12 swaps",
            "Depends on whether QuickSort or MergeSort is used",
          ],
          correctIndex: 1,
          explanation:
            "Correct! Because each adjacent swap can eliminate at most one inversion, an array with 6 inversions strictly requires at least 6 adjacent swaps.",
        },
        {
          id: "q2",
          question: "In an adjacent comparison sort, why does changing `if A[j] > A[j+1]` to `if A[j] >= A[j+1]` cause the algorithm to become unstable?",
          options: [
            "It increases the time complexity from O(n^2) to O(n^3)",
            "It swaps duplicate keys with equal values, corrupting their original relative order",
            "It causes an infinite loop whenever duplicate keys are present",
            "It produces an incorrectly sorted array",
          ],
          correctIndex: 1,
          explanation:
            "Correct! When two elements have equal keys, swapping them changes their relative ordering from the input, which violates the formal definition of stability.",
        },
      ],
      keyTakeaways: [
        "An array is sorted if and only if adjacent inversions equal zero.",
        "Each adjacent swap reduces total inversions by at most 1.",
        "Worst-case inverted arrays have n(n-1)/2 inversions, dictating the O(n^2) lower bound for adjacent sorts.",
        "Strict inequality (>) during swaps guarantees algorithm stability.",
      ],
      targetProblemId: 912,
    },

    2: {
      id: 2,
      title: "Bubble Sort: Visual Step Trace & Optimization",
      courseId: "sorting-masterclass",
      courseTitle: "Sorting Algorithms Masterclass",
      category: "Sorting & Searching",
      difficulty: "Beginner",
      duration: "35 min",
      type: "visualizer",
      overview:
        "Master the Bubble Sort algorithm, its loop invariants, the 'swapped' early-termination flag, and why its worst-case is O(n^2) while its best-case is O(n).",
      markdownSections: [
        {
          id: "mechanics",
          title: "1. The Bubble Mechanism & Partition Invariant",
          content: `Bubble Sort repeatedly steps through the list, compares adjacent elements, and swaps them if they are in the wrong order.

After the **$k$-th pass**:
- The largest $k$ elements are guaranteed to have 'bubbled up' to their final sorted positions at indices $[n-k \\dots n-1]$.
- Therefore, the inner loop only needs to inspect up to index $n - k - 1$, shaving off redundant comparisons each pass.`,
        },
        {
          id: "optimization",
          title: "2. The Early Termination Optimization",
          content: `In its naive form, Bubble Sort always executes $\\frac{n(n-1)}{2}$ comparisons even if the input is already sorted $[1, 2, 3, 4, 5]$.

By introducing a boolean flag \`swapped = False\`:
1. If an entire pass completes with **zero swaps**, the array is already sorted!
2. We immediately break out of the loop.
3. This drops the best-case time complexity to **$O(n)$** with $O(1)$ auxiliary space.`,
        },
      ],
      codeSandbox: {
        language: "python",
        title: "Optimized Bubble Sort Implementation",
        description: "Run Bubble Sort with comparison counting and early termination flag:",
        starterCode: `def bubble_sort(arr):
    nums = list(arr)
    n = len(nums)
    total_comparisons = 0
    total_swaps = 0
    
    print(f"Input: {nums}")
    for i in range(n):
        swapped = False
        print(f"--- Pass {i + 1} ---")
        for j in range(0, n - i - 1):
            total_comparisons += 1
            if nums[j] > nums[j + 1]:
                nums[j], nums[j + 1] = nums[j + 1], nums[j]
                total_swaps += 1
                swapped = True
                print(f"  Swap [{j}, {j+1}]: {nums}")
        if not swapped:
            print("  Array fully sorted! Early termination triggered.")
            break
            
    print(f"\\nResult: {nums}")
    print(f"Total Comparisons: {total_comparisons} (out of {n*(n-1)//2} max)")
    print(f"Total Swaps: {total_swaps}")
    return nums

# Test with nearly sorted array
bubble_sort([10, 20, 30, 50, 40])
`,
        sampleOutput: `Input: [10, 20, 30, 50, 40]
--- Pass 1 ---
  Swap [3, 4]: [10, 20, 30, 40, 50]
--- Pass 2 ---
  Array fully sorted! Early termination triggered.

Result: [10, 20, 30, 40, 50]
Total Comparisons: 7 (out of 10 max)
Total Swaps: 1`,
      },
      quizzes: [
        {
          id: "q1",
          question: "What is the best-case time complexity of Bubble Sort with the early-exit swapped flag?",
          options: ["O(1)", "O(log n)", "O(n)", "O(n log n)"],
          correctIndex: 2,
          explanation:
            "Correct! If the array is already sorted, pass 1 performs (n-1) comparisons, finds 0 swaps, and immediately exits, yielding O(n) runtime.",
        },
        {
          id: "q2",
          question: "Why does the inner loop bound shrink from `n-1` to `n-i-1` on pass `i`?",
          options: [
            "To prevent out-of-bounds array access",
            "Because the largest `i` elements have already settled into their final positions at the end",
            "Because elements at the front are already sorted",
            "It is an arbitrary heuristic that only works on small arrays",
          ],
          correctIndex: 1,
          explanation:
            "Correct! In each pass, the maximum unsorted element bubbles to the rightmost unsorted index, so indices from n-i to n-1 are already in final position.",
        },
      ],
      keyTakeaways: [
        "Bubble Sort guarantees the k-th largest element is placed after pass k.",
        "Inner loop limit is n - i - 1 to eliminate redundant checks.",
        "The swapped flag enables O(n) best-case on presorted inputs.",
        "Space complexity is strictly O(1) in-place.",
      ],
      targetProblemId: 912,
    },

    3: {
      id: 3,
      title: "Merge Sort: Divide & Conquer Subarray Trees",
      courseId: "sorting-masterclass",
      courseTitle: "Sorting Algorithms Masterclass",
      category: "Sorting & Searching",
      difficulty: "Beginner",
      duration: "40 min",
      type: "visualizer",
      overview:
        "Understand the logarithmic recursion tree, the two-pointer merge routine, and why Merge Sort achieves guaranteed O(n log n) worst-case time with O(n) auxiliary space.",
      markdownSections: [
        {
          id: "divide-conquer",
          title: "1. The Divide & Conquer Paradigm",
          content: `Merge Sort splits the sorting problem into three clean recursive phases:
1. **Divide**: Find the midpoint $m = \\lfloor (l + r) / 2 \\rfloor$ and split array into two halves.
2. **Conquer**: Recursively sort the left half and right half until reaching single-element base cases ($l \\ge r$).
3. **Combine**: Merge the two sorted subarrays in linear $O(k)$ time using two pointers.

The recurrence relation is:
$$T(n) = 2T(n/2) + O(n)$$
By the Master Theorem (Case 2), this yields $T(n) = \\Theta(n \\log n)$ in **all cases** (best, average, and worst).`,
        },
        {
          id: "merging",
          title: "2. The Two-Pointer Linear Merge",
          content: `Given two sorted slices $L$ and $R$:
We place pointer $i$ at $L[0]$ and pointer $j$ at $R[0]$.
At each step, we compare $L[i]$ and $R[j]$:
- If $L[i] \\le R[j]$, we append $L[i]$ to the merged buffer and increment $i$.
- Else, we append $R[j]$ and increment $j$.

Using $\\le$ ensures that when $L[i] == R[j]$, the left item (which came earlier in the original array) is chosen first, **preserving stability**!`,
        },
      ],
      codeSandbox: {
        language: "python",
        title: "Merge Sort with Tree Trace",
        description: "Inspect the recursive division and merging steps:",
        starterCode: `def merge_sort(arr, depth=0):
    indent = "  " * depth
    if len(arr) <= 1:
        print(f"{indent}Base case reached: {arr}")
        return arr

    mid = len(arr) // 2
    print(f"{indent}Divide: {arr[:mid]} | {arr[mid:]}")
    
    left = merge_sort(arr[:mid], depth + 1)
    right = merge_sort(arr[mid:], depth + 1)
    
    # Merge step
    merged = []
    i = j = 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            merged.append(left[i])
            i += 1
        else:
            merged.append(right[j])
            j += 1
    merged.extend(left[i:])
    merged.extend(right[j:])
    
    print(f"{indent}Merge -> {merged}")
    return merged

sample = [38, 27, 43, 3, 9, 82, 10]
print("Input:", sample)
sorted_res = merge_sort(sample)
print("Sorted:", sorted_res)
`,
        sampleOutput: `Input: [38, 27, 43, 3, 9, 82, 10]
Divide: [38, 27, 43] | [3, 9, 82, 10]
  Divide: [38] | [27, 43]
    Base case reached: [38]
    Divide: [27] | [43]
      Base case reached: [27]
      Base case reached: [43]
    Merge -> [27, 43]
  Merge -> [27, 38, 43]
  Divide: [3, 9] | [82, 10]
    Divide: [3] | [9]
      Base case reached: [3]
      Base case reached: [9]
    Merge -> [3, 9]
    Divide: [82] | [10]
      Base case reached: [82]
      Base case reached: [10]
    Merge -> [10, 82]
  Merge -> [3, 9, 10, 82]
Merge -> [3, 9, 10, 27, 38, 43, 82]
Sorted: [3, 9, 10, 27, 38, 43, 82]`,
      },
      quizzes: [
        {
          id: "q1",
          question: "What is the auxiliary space complexity of standard standard array-based Merge Sort?",
          options: ["O(1)", "O(log n)", "O(n)", "O(n log n)"],
          correctIndex: 2,
          explanation:
            "Correct! Merging two arrays requires an auxiliary buffer of size up to O(n) to store intermediate sorted slices.",
        },
        {
          id: "q2",
          question: "What is the height of the recursion tree when sorting an array of size n with Merge Sort?",
          options: ["O(1)", "O(log n)", "O(n)", "O(n^2)"],
          correctIndex: 1,
          explanation:
            "Correct! Halving the array at each level produces a tree of height ceil(log2 n).",
        },
      ],
      keyTakeaways: [
        "Merge Sort delivers guaranteed O(n log n) worst, average, and best-case performance.",
        "Stable by default when using <= in the two-pointer merge comparator.",
        "Requires O(n) additional memory buffer for subarray reconstruction.",
        "Highly parallelizable and optimal for external sorting on large disks.",
      ],
      targetProblemId: 912,
    },

    4: {
      id: 4,
      title: "Quick Sort: Partitioning Invariants & Worst Cases",
      courseId: "sorting-masterclass",
      courseTitle: "Sorting Algorithms Masterclass",
      category: "Sorting & Searching",
      difficulty: "Intermediate",
      duration: "45 min",
      type: "visualizer",
      overview:
        "Master pivot selection strategies, Lomuto vs Hoare partitioning schemes, and recursion stack bounds to prevent O(n^2) degradation on adversarial inputs.",
      markdownSections: [
        {
          id: "partitioning",
          title: "1. The Partition Invariant",
          content: `Quick Sort selects an element called the **pivot**, reorders the array around it, and recurses:

After \`partition(arr, low, high)\`:
1. The pivot element is at its **exact final sorted index $p$**.
2. All elements in $arr[low \\dots p-1]$ are $\\le pivot$.
3. All elements in $arr[p+1 \\dots high]$ are $\\ge pivot$.`,
        },
        {
          id: "pitfalls",
          title: "2. The O(n^2) Worst Case and Pivot Selection",
          content: `If we always choose the first or last element as pivot:
- On an already sorted array $[1, 2, 3, 4, 5]$, the partition splits the problem into sizes $0$ and $n-1$.
- Recursion depth degenerates to $O(n)$, causing $T(n) = T(n-1) + O(n) = O(n^2)$!
- **Mitigation**: Randomized pivot selection or **Median-of-Three** (comparing first, middle, and last elements) prevents adversarial triggers.`,
        },
      ],
      codeSandbox: {
        language: "python",
        title: "Lomuto Partition Simulator",
        description: "Observe the pivot settlement and boundary partitioning:",
        starterCode: `def lomuto_partition(arr, low, high):
    pivot = arr[high]
    i = low - 1  # Boundary of elements <= pivot
    print(f"Partitioning range [{low}..{high}], Pivot = {pivot}")
    
    for j in range(low, high):
        if arr[j] <= pivot:
            i += 1
            arr[i], arr[j] = arr[j], arr[i]
            print(f"  Item {arr[i]} <= {pivot}: swapped with index {j} -> {arr}")
            
    # Place pivot in its rightful place
    arr[i + 1], arr[high] = arr[high], arr[i + 1]
    print(f"Pivot {pivot} placed at final index {i + 1} -> {arr}\\n")
    return i + 1

test = [10, 80, 30, 90, 40, 50, 70]
print("Before partition:", test)
p_idx = lomuto_partition(test, 0, len(test) - 1)
print(f"Pivot settled at index {p_idx}: {test[p_idx]}")
print("Left partition:", test[:p_idx])
print("Right partition:", test[p_idx + 1:])
`,
        sampleOutput: `Before partition: [10, 80, 30, 90, 40, 50, 70]
Partitioning range [0..6], Pivot = 70
  Item 10 <= 70: swapped with index 0 -> [10, 80, 30, 90, 40, 50, 70]
  Item 30 <= 70: swapped with index 2 -> [10, 30, 80, 90, 40, 50, 70]
  Item 40 <= 70: swapped with index 4 -> [10, 30, 40, 90, 80, 50, 70]
  Item 50 <= 70: swapped with index 5 -> [10, 30, 40, 50, 80, 90, 70]
Pivot 70 placed at final index 4 -> [10, 30, 40, 50, 70, 90, 80]

Pivot settled at index 4: 70
Left partition: [10, 30, 40, 50]
Right partition: [90, 80]`,
      },
      quizzes: [
        {
          id: "q1",
          question: "When naive QuickSort always picks the first element as pivot, what is the runtime on an already sorted array?",
          options: ["O(n)", "O(n log n)", "O(n^2)", "O(log n)"],
          correctIndex: 2,
          explanation:
            "Correct! Always selecting the minimum element creates completely unbalanced partitions (0 and n-1), resulting in O(n^2) quadratic comparisons.",
        },
      ],
      keyTakeaways: [
        "In-place partition requires O(1) auxiliary space beyond recursion stack.",
        "Average case is O(n log n) with high cache locality due to sequential scans.",
        "Median-of-three or randomized pivot prevents O(n^2) worst-case degradation.",
        "Standard QuickSort is unstable due to non-adjacent partitioning swaps.",
      ],
      targetProblemId: 912,
    },

    5: {
      id: 5,
      title: "Stability, In-Place Space & Memory Trade-offs",
      courseId: "sorting-masterclass",
      courseTitle: "Sorting Algorithms Masterclass",
      category: "Sorting & Searching",
      difficulty: "Intermediate",
      duration: "30 min",
      type: "theory",
      overview:
        "Deep dive into algorithm stability, multi-key ordering in real production databases, memory hierarchy impacts, and non-comparison sorting alternatives.",
      markdownSections: [
        {
          id: "stability-prod",
          title: "1. Why Stability Matters in Production",
          content: `In application development, sorting often operates on complex objects with multiple fields.

For example, consider student records:

\`\`\`python
students = [
    {"name": "Alice", "score": 90},
    {"name": "Bob", "score": 85},
    {"name": "Charlie", "score": 90},
]
\`\`\`

If the list was already sorted alphabetically by \`name\`, and we now sort by \`score\` in descending order:
- A **stable sort** guarantees \`Alice\` appears before \`Charlie\` because \`Alice\` was ahead originally.
- An **unstable sort** may invert them, putting \`Charlie\` ahead of \`Alice\` unexpectedly.

This property enables multi-pass sorting: to sort by primary key $A$ and secondary key $B$, you simply perform **stable sort on $B$ first, then stable sort on $A$**!`,
        },
        {
          id: "comparison-matrix",
          title: "2. The Master Sorting Trade-off Matrix",
          content: `| Algorithm | Best Time | Average Time | Worst Time | Space | Stable? | In-Place? |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Bubble Sort** | $O(n)$ | $O(n^2)$ | $O(n^2)$ | $O(1)$ | **Yes** | Yes |
| **Insertion Sort**| $O(n)$ | $O(n^2)$ | $O(n^2)$ | $O(1)$ | **Yes** | Yes |
| **Selection Sort**| $O(n^2)$ | $O(n^2)$ | $O(n^2)$ | $O(1)$ | No | Yes |
| **Merge Sort** | $O(n \\log n)$ | $O(n \\log n)$ | $O(n \\log n)$ | $O(n)$ | **Yes** | No |
| **Quick Sort** | $O(n \\log n)$ | $O(n \\log n)$ | $O(n^2)$ | $O(\\log n)$ | No | Yes |
| **Heap Sort** | $O(n \\log n)$ | $O(n \\log n)$ | $O(n \\log n)$ | $O(1)$ | No | Yes |
| **Timsort (Python)**| $O(n)$ | $O(n \\log n)$ | $O(n \\log n)$ | $O(n)$ | **Yes** | No |`,
        },
      ],
      codeSandbox: {
        language: "python",
        title: "Multi-Key Stability Demonstration",
        description: "Verify how Python's built-in Timsort preserves stability across multiple keys:",
        starterCode: `# Multi-key stable sort demonstration
data = [
    ("Charlie", 90),
    ("Alice", 90),
    ("Bob", 85),
    ("David", 90),
    ("Eve", 85)
]

print("Original list:")
for name, score in data:
    print(f"  {name:8}: {score}")

# 1. First sort alphabetically by Name
by_name = sorted(data, key=lambda item: item[0])
print("\\n1. After sorting by name:")
for name, score in by_name:
    print(f"  {name:8}: {score}")

# 2. Then stable sort descending by Score
by_score = sorted(by_name, key=lambda item: item[1], reverse=True)
print("\\n2. After stable sort by score (notice tie-breaking preserves name order!):")
for name, score in by_score:
    print(f"  {name:8}: {score}")
`,
        sampleOutput: `Original list:
  Charlie : 90
  Alice   : 90
  Bob     : 85
  David   : 90
  Eve     : 85

1. After sorting by name:
  Alice   : 90
  Bob     : 85
  Charlie : 90
  David   : 90
  Eve     : 85

2. After stable sort by score (notice tie-breaking preserves name order!):
  Alice   : 90
  Charlie : 90
  David   : 90
  Bob     : 85
  Eve     : 85`,
      },
      quizzes: [
        {
          id: "q1",
          question: "Which of the following algorithms is in-place ($O(1)$ auxiliary space) AND guarantees $O(n \\log n)$ worst-case time?",
          options: ["Merge Sort", "Heap Sort", "Quick Sort", "Bubble Sort"],
          correctIndex: 1,
          explanation:
            "Correct! Heap Sort operates strictly in-place with O(1) auxiliary space and guarantees O(n log n) worst-case time, though it is unstable.",
        },
        {
          id: "q2",
          question: "Why does Python (and Java Objects) use Timsort instead of standard QuickSort?",
          options: [
            "Timsort requires less CPU instructions than QuickSort",
            "Timsort is stable and exploits pre-existing runs in real-world data for O(n) best-case",
            "Timsort uses zero memory allocations",
            "QuickSort cannot sort string keys",
          ],
          correctIndex: 1,
          explanation:
            "Correct! Timsort is an adaptive hybrid of Merge Sort and Insertion Sort designed by Tim Peters. It is stable and runs in O(n) on partially sorted data.",
        },
      ],
      keyTakeaways: [
        "Stability is essential when sorting records with multiple priority keys.",
        "Merge Sort is stable but costs O(n) auxiliary space.",
        "Heap Sort achieves O(n log n) with O(1) space, but is unstable.",
        "Timsort combines the stability of Merge Sort with the cache locality of Insertion Sort.",
      ],
      targetProblemId: 912,
    },

    6: {
      id: 6,
      title: "Assessment Quiz & Sorting Lab",
      courseId: "sorting-masterclass",
      courseTitle: "Sorting Algorithms Masterclass",
      category: "Sorting & Searching",
      difficulty: "Beginner",
      duration: "20 min",
      type: "quiz",
      overview:
        "Comprehensive final evaluation: test your mastery of worst-case complexity, stability proofs, asymptotic lower bounds, and algorithm selection rules.",
      markdownSections: [
        {
          id: "exam-overview",
          title: "Course Assessment & Decision Framework",
          content: `Congratulations on reaching the final milestone of the **Sorting Algorithms Masterclass**!

Before starting the assessment quiz, keep these three golden rules of interview algorithm selection in mind:
1. **Tiny array ($n \\le 32$)**: Insertion sort dominates due to negligible overhead and $O(n)$ cache-locality.
2. **General in-memory sorting**: QuickSort or Timsort.
3. **Guaranteed worst-case $O(n \\log n)$ with stability required**: Merge Sort or Timsort.`,
        },
      ],
      codeSandbox: {
        language: "python",
        title: "Sorting Benchmark Lab",
        description: "Compare comparisons count between Bubble Sort and Selection Sort on reversed data:",
        starterCode: `# Sorting Lab: Comparison Analysis
def compare_algorithms(n=6):
    rev = list(range(n, 0, -1))
    print(f"Reversed Input (n={n}): {rev}")
    
    # Bubble Sort comparisons
    b_arr = list(rev)
    b_comps = 0
    for i in range(n):
        for j in range(0, n - i - 1):
            b_comps += 1
            if b_arr[j] > b_arr[j+1]:
                b_arr[j], b_arr[j+1] = b_arr[j+1], b_arr[j]
                
    # Selection Sort comparisons
    s_arr = list(rev)
    s_comps = 0
    for i in range(n):
        min_idx = i
        for j in range(i + 1, n):
            s_comps += 1
            if s_arr[j] < s_arr[min_idx]:
                min_idx = j
        s_arr[i], s_arr[min_idx] = s_arr[min_idx], s_arr[i]
        
    print(f"Bubble Sort Comparisons:    {b_comps}")
    print(f"Selection Sort Comparisons: {s_comps}")
    print(f"Theoretical n(n-1)/2:       {n * (n - 1) // 2}")

compare_algorithms(6)
`,
        sampleOutput: `Reversed Input (n=6): [6, 5, 4, 3, 2, 1]
Bubble Sort Comparisons:    15
Selection Sort Comparisons: 15
Theoretical n(n-1)/2:       15`,
      },
      quizzes: [
        {
          id: "q1",
          question: "What is the comparison-based sorting information-theoretic lower bound for sorting n items?",
          options: ["Ω(n)", "Ω(n log n)", "Ω(n^2)", "Ω(log n)"],
          correctIndex: 1,
          explanation:
            "Correct! Any comparison decision tree requires at least log2(n!) leaves, which by Stirling's approximation is Ω(n log n).",
        },
        {
          id: "q2",
          question: "An interviewer asks you to sort 10 million 64-bit integers on a machine with limited RAM. Which strategy is most appropriate?",
          options: [
            "In-memory QuickSort",
            "External 2-Way or K-Way Merge Sort using temporary disk runs",
            "Bubble Sort with swapped optimization",
            "Counting Sort with an array of size 2^64",
          ],
          correctIndex: 1,
          explanation:
            "Correct! When data exceeds RAM, External K-Way Merge Sort sorts manageable chunks in memory, writes them to disk, and merges them using a min-heap buffer.",
        },
        {
          id: "q3",
          question: "Which sort performs the fewest write/swap operations ($O(n)$ swaps in all cases)?",
          options: ["Selection Sort", "Bubble Sort", "Insertion Sort", "Merge Sort"],
          correctIndex: 0,
          explanation:
            "Correct! Selection Sort makes at most 1 swap per outer loop pass, executing exactly n - 1 swaps in total.",
        },
      ],
      keyTakeaways: [
        "Comparison sorting has an absolute lower bound of Ω(n log n).",
        "Non-comparison sorts (Counting, Radix, Bucket) achieve O(n) by exploiting bounded key distributions.",
        "Selection Sort minimizes memory writes (at most n swaps).",
        "External sorting relies on chunked Merge Sort with disk buffering.",
      ],
      targetProblemId: 912,
    },
  },
};

/**
 * Universal Fallback Generator
 * Generates structured Kaggle-style notebook content for any course/lesson ID if not explicitly registered.
 */
export function getLessonContent(courseId, lessonId, courseMeta = null) {
  const courseRegistry = LESSON_CONTENT_REGISTRY[courseId];
  if (courseRegistry && courseRegistry[lessonId]) {
    return courseRegistry[lessonId];
  }

  // Derive from course metadata
  const title = courseMeta?.title || "Algorithmic Foundations";
  const lessons = courseMeta?.lessons || [];
  const lessonObj = lessons.find((l) => l.id === Number(lessonId)) || {
    id: lessonId,
    title: `Lesson ${lessonId}: Core Concepts & Analysis`,
    duration: "25 min",
    type: "concept",
    summary: "Master the algorithmic invariants, runtime complexity bounds, and code patterns for this topic.",
  };

  return {
    id: Number(lessonId),
    title: lessonObj.title,
    courseId: courseId,
    courseTitle: title,
    category: courseMeta?.category || "Computer Science",
    difficulty: courseMeta?.difficulty || "Intermediate",
    duration: lessonObj.duration || "25 min",
    type: lessonObj.type || "concept",
    overview:
      lessonObj.summary ||
      `Learn the core structural patterns, time complexity bounds, and real-world trade-offs for ${lessonObj.title}.`,
    markdownSections: [
      {
        id: "concept-overview",
        title: `1. Understanding ${lessonObj.title}`,
        content: `In this notebook lesson, we explore the essential principles of **${lessonObj.title}** within the **${title}** track.

Key objectives:
- Break down the core recurrence and invariant guarantees.
- Analyze best-case, average-case, and adversarial worst-case bounds.
- Implement an idiomatic Python solution with clean edge-case coverage.`,
      },
      {
        id: "complexity-invariants",
        title: "2. Invariants & Algorithmic Bounds",
        content: `When analyzing solutions for this topic:
- **Correctness Invariant**: Ensure that after each iteration or recursive call, the processed subproblem maintains validity.
- **Space-Time Trade-off**: Evaluate whether auxiliary caching (hash maps, recursion stacks) yields acceptable overhead versus in-place pointer techniques.`,
      },
    ],
    codeSandbox: {
      language: "python",
      title: `${lessonObj.title} - Interactive Lab`,
      description: "Experiment with the algorithm implementation in this runnable code cell:",
      starterCode: `# Interactive Playground: ${lessonObj.title}
def solution(data):
    print(f"Processing input: {data}")
    # Process items step-by-step
    results = []
    for idx, val in enumerate(data):
        results.append(val * 2 if isinstance(val, (int, float)) else val)
    print(f"Transformed output: {results}")
    return results

# Run with sample input
sample_input = [1, 2, 4, 8, 16]
output = solution(sample_input)
print("Execution completed successfully.")
`,
      sampleOutput: `Processing input: [1, 2, 4, 8, 16]
Transformed output: [2, 4, 8, 16, 32]
Execution completed successfully.`,
    },
    quizzes: [
      {
        id: "q1",
        question: `When implementing ${lessonObj.title}, what is the primary consideration for handling edge cases?`,
        options: [
          "Always handle empty inputs, single-element collections, and duplicates explicitly",
          "Assume inputs are always non-empty and well-formed",
          "Convert all inputs to strings before processing",
          "Allocate O(n^2) auxiliary memory to avoid boundary checks",
        ],
        correctIndex: 0,
        explanation:
          "Correct! Robust algorithmic implementations must gracefully handle boundary states such as empty collections, single elements, and duplicate values.",
      },
    ],
    keyTakeaways: [
      `Master the core invariant underlying ${lessonObj.title}.`,
      "Evaluate boundary conditions and asymptotic guarantees before writing code.",
      "Check space complexity trade-offs between recursion and iteration.",
    ],
    targetProblemId: courseMeta?.targetProblemId || 1,
  };
}
