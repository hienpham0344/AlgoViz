# Chrome Web Store Listing — AlgoViz Side Panel & Code Bridge

> Last Updated: 2026-11-26

## Store Listing

**Extension Name** [REQUIRED]
AlgoViz Side Panel & Code Bridge

**Short Description** [REQUIRED]
Step through live Java runtime traces, visualize arrays and pointers, and run multi-language code alongside Wecode UIT and LeetCode.

**Detailed Description** [REQUIRED]
Step through every line of your algorithm and visualize live array states, pointers, and local variables directly in a browser side panel while practicing on Wecode UIT, LeetCode, and coding tutorials.

Key Features:
Live Line-by-Line Java Trace: Watch arrays sort, pointers move, and local variables update step by step without leaving your problem tab.
One-Click Editor Grab: Pull your current solution directly from Wecode UIT, LeetCode, or GeeksforGeeks code editors into the side panel.
Right-Click Code Selection Trace: Highlight any algorithm snippet on a webpage, right-click, and open an interactive execution trace immediately.
Multi-Language Code Runner: Test Python, C++, JavaScript, and Java code with custom input and inspect execution time and memory metrics.
Instant Time & Space Complexity Insights: Get clear Big-O time and space complexity estimates along with optimization suggestions.
Seamless Studio Deep-Linking: Jump straight to the full AlgoViz problem workspace or visualizer studio whenever you want a full-screen view.

How to Use:
1. Click the AlgoViz toolbar icon to open the Side Panel alongside Wecode UIT or LeetCode.
2. Click "Grab Editor" (or highlight code on any page and right-click "Trace Selected Code in AlgoViz Side Panel").
3. Click "Trace Java (JDI)" to scrub through every execution step with live visual bars and pointer badges, or click "Run Code" for Python, C++, and JavaScript.

Privacy & Trust:
Your code is only processed when you explicitly click Grab, Trace, Run, or Analyze. Settings and active snippets are stored locally in your browser and sent only to your configured AlgoViz server.

Support & Feedback:
Report issues or request features at https://github.com/hienpham0344/AlgoViz/issues

**Category** [REQUIRED]
Developer Tools

**Single Purpose** [REQUIRED]
Extracts algorithm code from coding practice pages and visualizes step-by-step runtime execution traces and complexity in a side panel.

**Primary Language** [REQUIRED]
English

## Graphics & Assets

| Asset | Dimensions | Status | Filename |
|-------|-----------|--------|----------|
| Store Icon [REQUIRED] | 128×128 PNG | ✅ Ready | `extension/icons/icon-128.png` |
| Toolbar Icon (48px) | 48×48 PNG | ✅ Ready | `extension/icons/icon-48.png` |
| Favicon / Menu Icon (16px) | 16×16 PNG | ✅ Ready | `extension/icons/icon-16.png` |
| Screenshot 1 [REQUIRED] | 1280×800 | 🟡 Needs capture at booth | `docs/screenshots/sidepanel-wecode-trace.png` |
| Screenshot 2 [RECOMMENDED] | 1280×800 | 🟡 Needs capture at booth | `docs/screenshots/sidepanel-leetcode-pointers.png` |
| Small Promo Tile [RECOMMENDED] | 440×280 | ⬜ Not created | |

### Screenshot Notes
- **Screenshot 1**: Wecode UIT / LeetCode problem open on the left (70% width) with the AlgoViz Side Panel open on the right (30% width) showing live Binary Search / Bubble Sort array bars, active pointer tags (`low`, `mid`, `high`), and local variable values.
- **Screenshot 2**: Right-click context menu *"Trace Selected Code in AlgoViz Side Panel"* triggering the on-page confirmation toast and displaying AST-recognized algorithm patterns and Big-O complexity pills.

## Permissions Justification

| Permission | Type | Justification |
|------------|------|---------------|
| `sidePanel` | permissions | Displays the persistent AlgoViz runtime trace player and code runner alongside coding problems without covering the webpage. |
| `storage` | permissions | Saves the user's preferred AlgoViz server URL, dark/light theme choice, and the most recently selected code snippet across browser restarts. |
| `contextMenus` | permissions | Adds the "Trace Selected Code in AlgoViz Side Panel" right-click menu item when a student highlights code on any webpage. |
| `scripting` | permissions | Extracts selected or editor code from the active tab when the user clicks "Grab Editor" inside the side panel on pages where the content script has not initialized yet. |
| `tabs` | permissions | Reads the active tab's URL and title to detect the corresponding problem ID for deep-linking into the full AlgoViz workspace. |
| `http://localhost:8080/*` | host_permissions | Connects the side panel to the local AlgoViz Docker / Spring Boot trace and code execution server during local study and classroom labs. |
| `http://localhost:5173/*` | host_permissions | Connects to the local AlgoViz Vite development environment during development and testing. |
| `https://*.azurecontainerapps.io/*` | host_permissions | Connects the side panel to the cloud-hosted AlgoViz backend deployed on Azure Container Apps. |
| `https://leetcode.com/*` | host_permissions | Allows the content script to read code from the LeetCode editor and display confirmation toasts when the user grabs or traces a solution. |
| `https://*.uit.edu.vn/*` | host_permissions | Allows UIT students practicing on Wecode UIT (`wecode.uit.edu.vn`, `khmt.uit.edu.vn`) to extract editor code directly into the AlgoViz Side Panel. |
| `https://*.geeksforgeeks.org/*` | host_permissions | Allows students reading algorithm tutorials on GeeksforGeeks to grab sample code blocks into the trace player with one click. |

## Privacy & Data Use

### Data Collection

**Does the extension collect user data?** No

| Data Type | Collected? | Transmitted Off-Device? | Purpose | Shared with Third Parties? |
|-----------|-----------|------------------------|---------|---------------------------|
| Personally identifiable info | No | No | N/A | No |
| Health info | No | No | N/A | No |
| Financial info | No | No | N/A | No |
| Authentication info | No | No | N/A | No |
| Personal communications | No | No | N/A | No |
| Location | No | No | N/A | No |
| Web history | No | No | N/A | No |
| User activity | No | No | N/A | No |
| Website content | No (processed on-demand only) | Sent only to user-configured AlgoViz server on explicit click | Executes runtime trace and complexity analysis for user-selected code snippets | No |

### Data Use Certification
- [x] Data is NOT sold to third parties
- [x] Data is NOT used for purposes unrelated to the extension's core functionality
- [x] Data is NOT used for creditworthiness or lending purposes

## Privacy Policy

**Privacy Policy URL** [REQUIRED]
https://github.com/hienpham0344/AlgoViz/blob/main/README.md#security

## Distribution

**Visibility**: Public
**Regions**: All regions (Primary audience: Vietnam / UIT SEAPP)

## Developer Info

**Publisher Name** [REQUIRED]
AlgoViz Team (UIT — Khoa Công nghệ Phần mềm)

**Contact Email** [REQUIRED]
contact@algoviz.dev

**Support URL / Email** [RECOMMENDED]
https://github.com/hienpham0344/AlgoViz/issues

**Homepage URL** [RECOMMENDED]
https://github.com/hienpham0344/AlgoViz

## Version History

| Version | Date | Changes | Status |
|---------|------|---------|--------|
| 1.0.0 | 2026-11-26 | Initial Manifest V3 release for SEAPP UIT: Side Panel Java AST/JDI trace visualizer, Wecode UIT & LeetCode editor code bridge, multi-language code runner, and AST complexity analyzer. | Draft |

## Review Notes

### Known Issues / Limitations
- Live line-by-line runtime trace (`POST /api/ast/trace`) requires a reachable AlgoViz backend instance (`http://localhost:8080` via Docker or Azure Container Apps URL).
