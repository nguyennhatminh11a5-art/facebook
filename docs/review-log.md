# Review log

Ghi các phát hiện 🟡 được hoãn (CLAUDE.md mục 11) kèm lý do và kế hoạch xử lý.

| Ngày | Sprint | Mức | Phát hiện | Lý do hoãn / quyết định |
|---|---|---|---|---|
| 2026-09-25 | 0–5 | ⚠️ | Toàn bộ code do Claude viết và tự review — **không phải review độc lập**. | Diff đụng `auth/`, `security/`, `media/`, `PostAccessPolicy` phải được người đọc trước khi merge (mục 11, lớp 3). Chạy thêm `/code-review high` và `/security-review`. |
| 2026-09-25 | 2 | 🟡 | `MaxUploadSizeExceededException` → 413 chưa có test tự động: MockMvc bỏ qua giới hạn multipart của Tomcat. | Do `ResponseEntityExceptionHandler` xử lý sẵn; service cũng chặn ảnh > 5 MB (400). Thêm test với `@SpringBootTest(webEnvironment = RANDOM_PORT)` nếu cần. |
| 2026-09-25 | 2 | 🟡 | Feed dạng `user_id IN (...)` phải sort gộp, chi phí tăng theo tổng số bài của bạn bè. | Giới hạn đã biết, chấp nhận ở quy mô đồ án (mục 5.1). |
| 2026-09-25 | 1 | 🟡 | Rate limit trong bộ nhớ: mất khi khởi động lại, sai khi chạy nhiều instance. | Quyết định kiến trúc (mục 12). Chuyển sang bảng DB/Redis khi scale. |
| 2026-09-25 | 5 | 🟢 | Bundle frontend > 500 kB (Vite cảnh báo). | Có thể tách route bằng `lazy()` khi cần; chưa ảnh hưởng đồ án. |
| 2026-09-25 | 0 | 🟢 | Máy dev chưa có JDK 21; build/test chạy bằng JDK 25 với `--release 21`. CI dùng Temurin 21. | Cài Temurin 21 để khớp CI. |
