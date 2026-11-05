# AST-tool tích hợp trong AlgoViz

AlgoViz đóng gói engine AST-tool cùng backend Spring Boot, phục vụ cùng origin với frontend. Không cần chạy một backend ở cổng 8081. Engine phân tích Java bằng JavaParser và lấy snapshot từ JVM thực qua JDI. Source engine lấy từ working tree `/home/hien/Project/AST-tool`, tại revision `887dd96b19ab580b8fa76504ed97eded470f214c`; checksum nguồn và các test được lưu trong [ast-engine-source.json](ast-engine-source.json). Package được chuyển thành `com.aisc.algoviz.ast.engine`; không mang theo application, controller, CORS hay exception handler của ứng dụng độc lập.

## Sử dụng

Build và chạy bằng các script hiện có: `npm run build`, `npm run start:offline`. Cần **full JDK 17+**, bao gồm `javac` và module `jdk.jdi`.

Trong Algorithm Visualizer Studio, chọn Java, sửa source/testcase rồi Run. Trong workspace bài tập, Run Java và mở tab Visualize. Playback, timeline, dòng source, mảng và biến cục bộ dùng cùng trace. Sửa source hoặc testcase sẽ vô hiệu hóa trace cũ. Java trong phần lời giải tham khảo cũng dùng trace runtime; các minh họa cho ngôn ngữ khác được ghi rõ là reference illustration. Python/JavaScript/C++ vẫn chạy qua execution API, nhưng chưa có runtime trace từ AST-tool.

Source có `public static void main(String[] args)` nhận testcase qua stdin. Khi thiếu `main`, engine có thể sinh driver cho một phương thức không mơ hồ với tham số `int[]`, `int` hoặc một/hai `String`. Mảng và target chấp nhận `target\narray`, `array\ntarget` hoặc `[array], target`; hai String dùng hai dòng. Các kiểu đối số khác hoặc nhiều entry methods cần `main` tường minh. Driver không xuất hiện trong các bước được hiển thị; dòng statement và biến được đổi về dòng source trong editor.

Snapshot là trạng thái **trước khi** statement tại breakpoint thực thi (`BEFORE_LOCATION`, `LINE_BREAKPOINT`). Không coi mọi scalar là con trỏ vào mảng. Các đối tượng/collection dùng những dạng chiếu có giới hạn của engine; không gọi `toString()` tùy ý trên đối tượng của chương trình. Nhận diện thuật toán là phân tích tĩnh, không phải chứng minh tính đúng đắn hay verdict chấm bài.

## API

- `GET /api/ast/health`: trạng thái `UP`/`DISABLED`, engine và ngôn ngữ hỗ trợ.
- `POST /api/ast/trace`: JSON `{code, language: "java", customInput}`; trả `source`, `customInput`, `steps`, `algorithmHints`, `warnings`, `status`, `output`, `errorMessage`, `partial`, `executionTimeMs`.
- `POST /api/ast/parse`: multipart `file`, trả structured AST, không thực thi.
- `POST /api/debug/steps`: multipart `file`, `customInput`, tương thích client AST trước đây.
- `POST /api/debug`: multipart, trả danh sách observations phẳng.
- `GET /api/algorithms/catalog`, `POST /api/algorithms/recognize`: catalog và nhận diện tĩnh bằng multipart Java `file`.

Frontend: `fetchAlgorithmCatalog()` (có cache) và `recognizeAlgorithms()` trong `astApi.js`. Visualizer Java hiển thị `algorithmHints` của trace (loại thuật toán, nhóm từ catalog, dòng, độ tin cậy) bằng component `AlgorithmHints`.

Source không hợp lệ hoặc language không hỗ trợ trả 422; request/upload không hợp lệ trả 400. Hai phiên trace có thể chạy đồng thời; request vượt năng lực hiện tại trả 429, engine tắt trả 503. Cảnh báo breakpoint trùng dòng là thông tin về độ hạt quan sát, không biến một chương trình chạy xong thành thất bại. Một trace chạm giới hạn trả `partial: true`, cảnh báo và trạng thái `Trace Limit Reached`; UI vẫn có thể xem phần snapshot đã thu được, không thay bằng dữ liệu mô phỏng.

## Giới hạn thực thi

Mặc định: tối đa 8 giây quan sát, 2.000 bước, 100 phần tử trong đối số mảng do driver sinh, heap target 64 MiB, 1 MiB output trong lúc chạy; response giữ tối đa 65.536 byte mỗi stream và báo nếu bị cắt. Cấu hình dùng `algoviz.ast.enabled`, `algoviz.ast.timeout-seconds` và `algoviz.ast.max-steps`.

JDI kết nối loopback bằng [ListeningConnector](https://docs.oracle.com/en/java/javase/17/docs/api/jdk.jdi/com/sun/jdi/connect/ListeningConnector.html); target JVM có working directory tạm, classpath chỉ chứa chương trình đã compile, không kế thừa biến môi trường server. Compiler tắt annotation processing. Target/process con được dừng khi kết thúc hoặc chạm giới hạn và workspace được dọn.

Đây là chế độ native cho môi trường phát triển cục bộ, cùng phạm vi với native execution hiện tại; thư mục tạm và giới hạn tài nguyên không tạo sandbox cách ly filesystem/network. Khi Docker execution được bật, JDI native bị tắt để không vượt qua chính sách đó. Môi trường chia sẻ cần tắt `algoviz.ast.enabled` hoặc triển khai worker JDI được cách ly phù hợp; xem [hướng dẫn sandbox](SANDBOX_CODE_EDITOR.md).

## Kiểm tra

`bash scripts/test-backend.sh -o` chạy regression tests engine cùng các API test thực thi Java, stdin, source mapping, sort, partial trace và môi trường target. JDI test cần quyền mở socket loopback. `npm --prefix frontend test` kiểm tra client, cancellation, adapter và trạng thái component; `npm run lint` và `npm run build` dùng quy trình hiện có. `VITE_AST_API_BASE_URL` vẫn cho phép chỉ định origin của engine riêng; mặc định dùng backend AlgoViz.
