# mini-facebook — Mạng xã hội thu nhỏ (React + Spring Boot + MySQL)

File này là **quy tắc bắt buộc** khi làm việc trong repo. Khi quy tắc ở đây mâu thuẫn với code,
dừng lại và hỏi — không tự chọn một bên.

## 1. Phạm vi v1.0

| # | Nhóm | Tính năng |
|---|---|---|
| 1 | Tài khoản | Đăng ký, đăng nhập, đăng xuất, làm mới phiên |
| 2 | Hồ sơ | Xem hồ sơ, sửa tên hiển thị/bio, đổi ảnh đại diện, bài của một người, tìm người theo tên |
| 3 | Bài viết | Đăng (text và/hoặc tối đa 4 ảnh), sửa nội dung, xoá bài của mình; bảng tin |
| 4 | Tương tác | Reaction (LIKE/LOVE/HAHA/WOW/SAD/ANGRY, 1 reaction/người/bài), bình luận, xoá bình luận |
| 5 | Bạn bè | Gửi/huỷ lời mời, chấp nhận/từ chối, huỷ kết bạn, danh sách bạn bè, gợi ý (bạn của bạn) |

**Cố ý KHÔNG làm ở v1** (không thêm nếu chưa được yêu cầu): chat/WebSocket, story, group,
livestream, marketplace, thông báo, quên mật khẩu, xác minh email, chặn/báo cáo người dùng, xoá tài
khoản, sửa ảnh của bài đã đăng. Hạ tầng không dùng: MongoDB, Redis, Kafka, microservices, GraphQL.

Nếu thiếu thời gian, cắt theo thứ tự: gợi ý kết bạn → tìm kiếm → ảnh đại diện → sáng/tối.

## 2. Quy tắc quyền truy cập (nguồn chuẩn duy nhất)

Mọi API trừ `/auth/register|login|refresh` và `/media/**` đều yêu cầu đăng nhập.

| Tài nguyên | Ai được XEM | Ai được SỬA/XOÁ |
|---|---|---|
| Hồ sơ cơ bản (id, tên, avatar, bio) | Mọi người đã đăng nhập | Chính chủ |
| Bài viết, ảnh của bài | Tác giả + bạn bè `ACCEPTED` của tác giả | Tác giả |
| Bình luận, reaction của một bài | Ai xem được bài | — |
| Thêm bình luận / reaction | Ai xem được bài | — |
| Xoá bình luận | — | Người viết bình luận **hoặc** tác giả bài |
| Danh sách bạn bè, lời mời | Chỉ của chính mình | — |

Mã lỗi: **không xem được → 404** (không lộ sự tồn tại). **Xem được nhưng không được sửa/xoá → 403.**
Quy tắc "xem được bài" chỉ viết ở **một chỗ** (`PostAccessPolicy.canView(viewerId, post)`), mọi
service gọi lại chỗ đó. User hiện tại luôn lấy từ `SecurityContext` (helper `CurrentUser`), không bao
giờ từ body/query.

## 3. Tech stack

Khi khởi tạo, lấy bản ổn định mới nhất tại start.spring.io / npm. Không chọn bản đã hết hỗ trợ.

| Tầng | Công nghệ | Ghi chú |
|---|---|---|
| Backend | Java 21, **Spring Boot 4.x**, Maven | Boot 4 dùng Jackson 3 (package `tools.jackson`) |
| Bảo mật | Spring Security, **`spring-boot-starter-oauth2-resource-server`** (JWT HS256 qua Nimbus), BCrypt | Không tự viết filter parse JWT |
| DB | MySQL 8.4, Spring Data JPA, **Flyway** (+ `flyway-mysql`) | `ddl-auto=validate`, không bao giờ `update`/`create` |
| Ảnh | Thumbnailator + TwelveMonkeys `imageio-webp` | Đọc/xoay/resize/mã hoá lại ảnh |
| Rate limit | Bucket4j + Caffeine (trong bộ nhớ) | Chạy 1 instance, xem mục 12 |
| API docs | springdoc-openapi (bản tương thích Boot 4) | Chỉ bật ở profile `dev` |
| Test backend | JUnit Jupiter, Mockito, MockMvc, **Testcontainers MySQL** | Không dùng H2 |
| Frontend | **React 19** + TypeScript (strict) + Vite | SPA, không Next.js |
| UI | **Tailwind v4** + shadcn/ui + `lucide-react` | |
| Dữ liệu server | TanStack Query + axios | Không `useEffect` + `fetch` cho dữ liệu server |
| Form / Router | react-hook-form + zod / **React Router 7** | |
| Test frontend | Vitest + React Testing Library | |

Môi trường: JDK 21 (máy hiện chỉ có Java 8 — cài Temurin 21, đặt `JAVA_HOME`), NetBeans ≥ 20,
Maven 3.9+, **Node 22 LTS trở lên** (Node 20 hết hỗ trợ 04/2026), MySQL 8.4,
Docker Desktop + WSL2 (bắt buộc cho Testcontainers).

## 4. Cấu trúc

```
mini-facebook/
├── backend/src/main/java/com/minifb/
│   ├── common/        # ApiError/ProblemDetail, GlobalExceptionHandler, Cursor, exception nghiệp vụ
│   ├── security/      # SecurityConfig, JWT encoder/decoder, CurrentUser, RateLimiter
│   ├── auth/ user/ post/ comment/ reaction/ friendship/
│   └── media/         # StorageService, LocalStorageService, ImageProcessor, MediaController (URL ký)
├── backend/src/main/resources/db/migration/   # V1__init.sql, V2__...
├── frontend/src/{api,features,components/ui,lib,routes}/
├── docs/review-log.md
├── .github/workflows/ci.yml, .github/dependabot.yml
├── lefthook.yml, .gitleaks.toml, docker-compose.yml, README.md
```

Package-by-feature: mỗi feature có `XxxController`, `XxxService`, `XxxRepository`, entity, `dto/`.

## 5. Quy tắc backend

1. **Phân tầng**: Controller chỉ `@Valid` + gọi service + trả DTO. Nghiệp vụ và kiểm tra quyền ở
   Service. Repository chỉ truy vấn.
2. **Không trả entity ra API** — luôn map sang `record` DTO.
3. **Lỗi**: ném `NotFoundException` / `ForbiddenException` / `ConflictException` /
   `BadRequestException` → `GlobalExceptionHandler` trả `ProblemDetail` (RFC 7807). Không lộ stack
   trace. Map thêm `MaxUploadSizeExceededException` → 413, vượt rate limit → 429 + `Retry-After`.
4. **Transaction** ở Service; đọc dùng `@Transactional(readOnly = true)`.
5. **Không N+1**: danh sách bài lấy id trước, rồi nạp tác giả, ảnh, reaction của tôi theo lô
   (`WHERE post_id IN (:ids)`) — số truy vấn cố định, không phụ thuộc số bài.
6. **Thời gian**: `DATETIME(6)` UTC, `Instant` trong Java, **luôn `truncatedTo(ChronoUnit.MICROS)`**
   khi gán thời gian (Java giữ nano giây, MySQL chỉ micro giây → cursor lệch nếu không cắt).
7. **Bí mật** (`DB_PASSWORD`, `JWT_SECRET`, `MEDIA_SIGNING_SECRET`) từ biến môi trường.
8. **Log**: SLF4J, không `System.out`. Không bao giờ log mật khẩu, token, cookie, header
   `Authorization`. Chỉ mở `actuator/health`.
9. **Dọn dẹp**: job `@Scheduled` hằng ngày xoá `refresh_tokens` đã hết hạn quá 1 ngày.

### 5.1 Phân trang cursor

- Áp dụng cho feed, bài của một người, bình luận, danh sách bạn bè, tìm kiếm. Không dùng OFFSET.
- `cursor` = base64url của `createdAt|id` (bạn bè: `id` của người bạn; tìm kiếm: `id` người dùng). Cursor sai định dạng → 400.
- `size` mặc định 10, tối đa 50. Truy vấn `LIMIT size + 1` để biết còn trang sau.
- Response: `{ "items": [...], "nextCursor": "..." | null }`.
- Viết điều kiện tường minh (MySQL không dùng index tốt với so sánh bộ `(a, b) < (x, y)`), và viết
  **2 truy vấn** cho trang đầu/trang sau (không dùng `:cursor IS NULL OR ...`):

```sql
-- Feed: authorIds = tôi + bạn bè ACCEPTED
SELECT p.id FROM posts p
WHERE p.user_id IN (:authorIds)
  AND (p.created_at < :cAt OR (p.created_at = :cAt AND p.id < :cId))
ORDER BY p.created_at DESC, p.id DESC
LIMIT :sizePlusOne
```

Bình luận: cũ trước, mới sau (`>` và `ASC`). Giới hạn đã biết: feed dạng IN nhiều tác giả phải
sort gộp, chi phí tăng theo tổng số bài của bạn bè — chấp nhận ở quy mô đồ án, không khẳng định
"tốc độ không đổi".

### 5.2 Bộ đếm (cùng transaction với thao tác)

- **Luôn khoá dòng bài trước**: `SELECT id FROM posts WHERE id = ? FOR UPDATE` (không có → 404).
  Lý do: `INSERT` vào bảng con giữ khoá S trên dòng `posts` (kiểm tra khoá ngoại), rồi
  `UPDATE posts` cần khoá X → 2 người react/bình luận cùng bài đồng thời sẽ **deadlock**. Khoá X
  trước thì các transaction xếp hàng, không deadlock.
- Reaction PUT (idempotent), native query có tham số, sau khi đã khoá bài:
  1. `INSERT IGNORE INTO reactions ...` → nếu 1 dòng: `UPDATE posts SET reaction_count = reaction_count + 1`.
  2. Nếu 0 dòng (đã có reaction): `UPDATE reactions SET type = ? WHERE post_id = ? AND user_id = ?`
     (đổi loại, **không** tăng đếm). Không dựa vào số dòng trả về của câu này (đặt cùng loại cũ có
     thể trả 0 tuỳ cấu hình `useAffectedRows` của Connector/J) — sự tồn tại của bài đã được kiểm tra
     bằng `FOR UPDATE`.
- Reaction DELETE: khoá bài, chỉ giảm đếm khi `DELETE` trả về 1 dòng.
- Bình luận thêm/xoá: khoá bài, `comment_count ± 1` tương tự.
- Sprint 3 có test đồng thời: N luồng react/bình luận cùng một bài → không lỗi, đếm = N.

### 5.3 Bài viết

- Nội dung được `trim()`; bài phải có nội dung không rỗng **hoặc** ≥ 1 ảnh. Nội dung ≤ 5000 ký tự.
- PATCH chỉ sửa nội dung; bài không có ảnh thì không được sửa thành rỗng.
- Xoá bài: xoá DB trong transaction, **xoá file sau khi commit** (`@TransactionalEventListener(phase = AFTER_COMMIT)`).

### 5.4 Ảnh (media/)

Luồng upload: kiểm tra → xử lý → lưu file → ghi DB. Nếu transaction rollback → xoá các file vừa lưu
(`TransactionSynchronization.afterCompletion`). Đổi avatar → xoá file avatar cũ sau commit.

1. Chỉ nhận JPEG/PNG/WebP, nhận diện bằng **magic bytes** (không tin `Content-Type`/đuôi file).
2. ≤ 5 MB/ảnh, ≤ 4 ảnh/bài (`spring.servlet.multipart.max-file-size=5MB`, `max-request-size=21MB`).
3. Đọc kích thước từ header **trước khi giải nén**; > 40 megapixel → 400 (chống decompression bomb).
4. **Mã hoá lại** bằng Thumbnailator: xoay theo EXIF orientation, **bỏ toàn bộ metadata (GPS)**,
   cạnh dài tối đa 2048px + thumbnail 640px. Xuất JPEG (PNG nếu có kênh trong suốt).
5. Tên file = UUID; DB lưu **khoá file**, không lưu URL tuyệt đối. `LocalStorageService` chuẩn hoá
   đường dẫn và kiểm tra nằm trong thư mục `uploads/` (chống path traversal).
6. Phục vụ ảnh:
   - Ảnh bài viết: URL **ký HMAC-SHA256** `GET /api/v1/media/{key}?exp=&sig=`, tạo lúc trả DTO
     **chỉ cho người xem được bài**. `exp` = giờ tròn kế tiếp **+ 1 giờ** (URL sống 1–2 giờ, giống
     nhau trong cùng một giờ nên trình duyệt cache được; không bao giờ hết hạn ngay khi vừa tạo).
     `sig` = HMAC(`MEDIA_SIGNING_SECRET`, `key|exp`), base64url. So chữ ký bằng
     `MessageDigest.isEqual` (thời gian hằng, không dùng `equals`). Sai chữ ký hoặc hết hạn → 404.
     (`<img>` không gửi được header `Authorization` nên dùng URL ký thay cho JWT.)
   - Avatar: `GET /api/v1/media/avatars/{key}` công khai.
   - Header: `Content-Type` theo định dạng đã mã hoá lại, `X-Content-Type-Options: nosniff`,
     `Cache-Control: private, max-age=3600`.

## 6. Bảo mật

| Chủ đề | Quy định |
|---|---|
| Mật khẩu | BCrypt strength 10–12. Tối thiểu 8 ký tự, **tối đa 72 byte UTF-8** (validator riêng, BCrypt bỏ phần sau 72 byte). Không log, không trả `passwordHash` |
| Email | `trim` + chữ thường trước khi lưu/tra cứu |
| Access token | JWT HS256, 15 phút, header `Authorization: Bearer`, frontend giữ trong bộ nhớ. Không thu hồi được — đăng xuất có hiệu lực chậm tối đa 15 phút. `JWT_SECRET` và `MEDIA_SIGNING_SECRET` ≥ 32 byte ngẫu nhiên (base64) — ứng dụng **từ chối khởi động** nếu thiếu hoặc ngắn hơn |
| Refresh token | 32 byte ngẫu nhiên, 7 ngày, cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`. DB lưu SHA-256. Rotation theo **family** (mục 6.1) |
| Đăng nhập sai | Thông báo chung "Email hoặc mật khẩu không đúng" |
| Rate limit | Đăng nhập: 5 sai/15 phút theo email+IP **và** 30/15 phút theo IP. Đăng ký: 5/giờ/IP. Refresh: 30/phút/IP. Tạo bài, bình luận, upload: 30/phút/user. Vượt → 429 |
| CSRF | Tắt CSRF của Spring cho API Bearer; endpoint dùng cookie (`/auth/refresh`, `/auth/logout`) dựa vào `SameSite=Strict` + chỉ nhận `POST` |
| CORS | Chỉ `http://localhost:5173` ở dev, `allowCredentials=true`. Production: frontend và API cùng domain qua reverse proxy (cookie Strict + Secure cần điều này), bắt buộc HTTPS |
| Header | `Content-Security-Policy: default-src 'self'; img-src 'self' blob: data:; object-src 'none'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: same-origin`. Spring Security chỉ gắn header cho response của API — CSP cho SPA (`index.html`) phải đặt ở **reverse proxy** (production) |
| XSS | Cấm `dangerouslySetInnerHTML`; nội dung lưu/trả dạng text thuần |
| SQL | Chỉ truy vấn có tham số. Tìm kiếm `LIKE`: **escape `%`, `_`, `\`** trong từ khoá |
| Validation | Mọi DTO request có ràng buộc. Bình luận ≤ 1000, tên 1–50, bio ≤ 300, từ khoá tìm kiếm 2–50 ký tự |
| Swagger | Tắt ở mọi profile khác `dev` |

### 6.1 Refresh token rotation theo family

- Đăng nhập tạo `family_id` mới. Mỗi lần refresh: đánh dấu token cũ `revoked_reason='ROTATED'`, cấp
  token mới cùng family.
- Token `ROTATED` bị dùng lại **trong 10 giây** (2 tab refresh cùng lúc) → chỉ trả access token mới,
  không set cookie, không thu hồi gì.
- Dùng lại sau 10 giây, hoặc dùng token `LOGOUT`/`REUSE` → thu hồi **cả family đó** (`REUSE`), trả 401.
  Không thu hồi phiên ở thiết bị khác.
- Frontend: trong một tab, các request 401 đồng thời chờ chung **một** lần gọi refresh.

## 7. Database (MySQL 8.4, `utf8mb4_0900_ai_ci`)

Schema chuẩn nằm ở `backend/src/main/resources/db/migration/` (bắt đầu từ `V1__init.sql`). Mọi thay
đổi bằng migration mới, **không sửa migration cũ**. Bảng: `users`, `refresh_tokens`, `posts`,
`post_images`, `comments`, `reactions` (PK `(post_id, user_id)`), `friendships` (cặp `user_low < user_high`,
không CASCADE, có CHECK `chk_friend_order`, `chk_friend_requester`).

`FriendshipService` vẫn tự sắp cặp (nhỏ, lớn) trước khi ghi và từ chối tự kết bạn với chính mình
(400) — CHECK chỉ là lưới an toàn cuối. Từ chối/huỷ lời mời/huỷ kết bạn = xoá dòng. A đã gửi cho B
mà B gửi lại cho A → coi như B chấp nhận. Nếu A và B gửi **đồng thời**, một bên sẽ vi phạm
`uq_friend_pair` → bắt `DataIntegrityViolationException`, đọc lại dòng trong transaction mới và xử lý
như trên (lời mời ngược chiều → chấp nhận; cùng chiều/đã là bạn → 409). Có test cho trường hợp này.

## 8. API (tiền tố `/api/v1`)

| Method | Endpoint | Mô tả |
|---|---|---|
| POST | `/auth/register` | 201; email trùng → 409 |
| POST | `/auth/login` | `{accessToken, user}` + cookie refresh |
| POST | `/auth/refresh` | Đọc cookie → access token mới (mục 6.1) |
| POST | `/auth/logout` | Thu hồi token (`LOGOUT`), xoá cookie → 204 |
| GET / PATCH | `/users/me` | Xem / sửa `displayName`, `bio` |
| POST | `/users/me/avatar` | Multipart, 1 ảnh |
| GET | `/users/{id}` | Hồ sơ + `friendStatus` (`NONE`/`OUTGOING`/`INCOMING`/`FRIENDS`/`SELF`) |
| GET | `/users/{id}/posts?cursor=&size=` | Bài của một người (chỉ khi xem được, không thì `items` rỗng + `friendStatus` để UI giải thích) |
| GET | `/users/search?q=&cursor=&size=` | Tìm theo tên (chứa `q`, không dấu nhờ collation `ai_ci`) |
| GET | `/feed?cursor=&size=` | Bảng tin |
| POST | `/posts` | Multipart `content` + `images[]` |
| GET / PATCH / DELETE | `/posts/{id}` | Chi tiết / sửa nội dung / xoá (204) |
| PUT / DELETE | `/posts/{id}/reaction` | `{type}` thêm hoặc đổi / bỏ |
| GET / POST | `/posts/{id}/comments` | Danh sách (cursor) / thêm |
| DELETE | `/comments/{id}` | Xoá (mục 2) |
| GET | `/friends?cursor=&size=` | Bạn bè của tôi |
| GET | `/friends/requests?direction=incoming\|outgoing` | Lời mời |
| POST | `/friends/requests` | `{userId}` — gửi; đã gửi/đã là bạn → 409 |
| POST | `/friends/requests/{userId}/accept` | Chấp nhận lời mời **từ** `userId` |
| DELETE | `/friends/requests/{userId}` | Từ chối (nếu là lời mời đến) / huỷ (nếu là lời mời đi) |
| DELETE | `/friends/{userId}` | Huỷ kết bạn |
| GET | `/friends/suggestions` | Bạn của bạn, chưa có quan hệ, sắp theo số bạn chung, tối đa 20 |
| GET | `/media/{key}?exp=&sig=`, `/media/avatars/{key}` | Ảnh (mục 5.4) |

Mọi endpoint bạn bè dùng **userId của người kia** làm định danh. Bài viết trả về:
`id, author{id, displayName, avatarUrl}, content, images[{url, thumbUrl, width, height}],
reactionCount, commentCount, myReaction, createdAt, updatedAt, editable`.
Bình luận trả về thêm `deletable`.

Mã trạng thái: 200/201/204; 400 validation; 401 chưa đăng nhập/token hết hạn; 403 xem được nhưng
không được sửa/xoá; 404 không tồn tại hoặc không xem được; 409 trùng; 413 file quá lớn; 429 rate limit.

## 9. Quy tắc frontend

- Gọi API qua `src/api/`. `useInfiniteQuery` cho feed/bình luận; `useMutation` + optimistic update
  cho reaction và bình luận (rollback khi lỗi).
- axios interceptor: 401 → refresh **một lần** (request đồng thời chờ chung) → gửi lại; thất bại →
  trang đăng nhập. Access token trong bộ nhớ; tải lại trang → gọi `/auth/refresh`.
- Ảnh: hiển thị `thumbUrl` trong feed, `url` khi mở ảnh; `loading="lazy"`; ảnh lỗi (URL ký hết hạn)
  → refetch query chứa bài đó.
- Mọi trang có 3 trạng thái: đang tải (skeleton), lỗi (thông báo + nút thử lại), rỗng (hướng dẫn).
- Desktop 3 cột, điện thoại 1 cột, ≥ 360px không tràn ngang. Sáng/tối.
- Chữ hiển thị tiếng Việt; tên biến/hàm/component tiếng Anh.
- Thời gian tương đối bằng `Intl.RelativeTimeFormat('vi')`, đổi sang giờ Việt Nam khi hiển thị.

## 10. Lộ trình

| Sprint | Nội dung | Định nghĩa hoàn thành (phải có test tự động tương ứng) |
|---|---|---|
| 0 | Khởi tạo backend/frontend, `V1__init.sql`, GlobalExceptionHandler, Swagger (dev), công cụ review lớp 1, 2, 4 | `mvn verify`, `npm run check` xanh; Flyway tạo đủ bảng kể cả CHECK; commit chưa format bị lefthook chặn; commit chứa secret bị gitleaks chặn; CI xanh |
| 1 | Tài khoản + trang Đăng nhập/Đăng ký + ProtectedRoute | Email trùng → 409; sai mật khẩu → 401; mật khẩu > 72 byte → 400; token hết hạn → tự refresh; 2 refresh đồng thời cùng token **không** đăng xuất; dùng lại token sau 10 giây → cả family bị thu hồi; vượt rate limit → 429 |
| 2 | Bài viết, upload ảnh, URL ký, feed cursor, cuộn vô hạn | Người xem được nhưng không phải tác giả sửa/xoá → 403; người không xem được → 404; file giả ảnh → 400; ảnh ra không còn EXIF GPS; ảnh > 40MP → 400; URL ký sai/hết hạn → 404; feed không trùng/sót bài khi có bài mới chen vào; rollback thì không còn file mồ côi |
| 3 | Reaction, bình luận, hồ sơ, avatar, tìm kiếm | PUT reaction 2 lần → 1 dòng, đếm = 1; đổi loại không tăng đếm; đếm đúng sau thêm/xoá bình luận; chủ bài xoá được bình luận người khác; tìm `%` không trả về tất cả; N reaction/bình luận đồng thời trên 1 bài → không deadlock, đếm = N |
| 4 | Bạn bè, gợi ý, feed chỉ bài bạn bè | Không tự kết bạn; A→B rồi B→A thành bạn; A→B và B→A đồng thời → thành bạn, không 500; huỷ kết bạn → bài của nhau thành 404 |
| 5 | Hoàn thiện: rà mục 6, trạng thái tải/lỗi/rỗng, responsive, seed demo, README có ảnh | Checklist mục 6 đạt hết; người mới clone chạy được theo README trong 10 phút |

## 11. Quy trình và review

Mỗi tính năng: migration → entity/repository → service + unit test → controller + test MockMvc/
Testcontainers → API client → UI → review → sửa 🔴 → commit (`feat(post): ...`, `fix(auth): ...`).
Mỗi endpoint có ít nhất 1 integration test thành công và 1 test bị từ chối (401/403/404) nếu có
kiểm tra quyền. Không commit `.env`, `uploads/`, `node_modules/`, `target/`.

**Lớp 1 — Công cụ** (chặn build):
- Backend: Spotless (google-java-format); SpotBugs + FindSecBugs mức High; JaCoCo rule
  `element=CLASS`, include `com.minifb.**.*Service`, phủ dòng ≥ 70%.
- Frontend: `npm run check` = `tsc --noEmit && eslint . && prettier --check . && vitest run`;
  ESLint có `typescript-eslint`, `react-hooks`, `jsx-a11y`, `react/no-danger`.

**Lớp 2 — lefthook** (pre-commit, file đã stage): Spotless cho `.java`, Prettier + ESLint cho
`frontend/`, **gitleaks** cho bí mật. Không dùng `--no-verify`. README ghi bước `lefthook install`.

**Lớp 3 — Review sau mỗi tính năng** (test đã xanh):
- `/code-review` cho mọi tính năng; `/security-review` thêm khi diff đụng `auth/`, `security/`,
  `media/`, kiểm tra quyền, cookie/token, CORS/header, hoặc câu SQL tự viết. Cuối sprint `/code-review high`.
- Claude tự review code do Claude viết **không phải review độc lập**: diff đụng `auth/`,
  `security/`, `media/`, `PostAccessPolicy` phải được **người** đọc trước khi merge.
- Checklist: (1) quyền theo mục 2, user từ `SecurityContext`; (2) không trả entity/`passwordHash`;
  (3) không N+1, không OFFSET; (4) bộ đếm cùng transaction, đúng mục 5.2; (5) validation đủ;
  (6) lỗi qua handler, đúng mã mục 8; (7) ảnh đúng mục 5.4; (8) migration mới, không sửa cũ;
  (9) có test thành công và bị từ chối; (10) frontend dùng TanStack Query, đủ 3 trạng thái;
  (11) không code chết, `System.out`/`console.log`, TODO vô chủ, thư viện mới chưa ghi lý do.
- Mức: 🔴 lỗ hổng/sai dữ liệu/thiếu kiểm tra quyền/test đỏ → **sửa trước khi commit**;
  🟡 N+1, thiếu test lỗi → sửa luôn hoặc ghi `docs/review-log.md` kèm lý do hoãn; 🟢 góp ý.
- Lỗi lặp lại lần 2 → thêm thành quy tắc ở đây hoặc thành rule lint/test.

**Lớp 4 — CI** (`.github/workflows/ci.yml`, mỗi push/PR): job backend (JDK 21, `mvn -B verify`),
job frontend (Node 22, `npm ci && npm run check && npm run build`), job gitleaks. Dependabot hằng
tuần cho Maven, npm, GitHub Actions. Merge vào `main` qua PR, CI xanh, mô tả PR có tóm tắt review.
(Repo private trên gói GitHub Free không bật được branch protection → hoặc để repo public, hoặc tự
tuân thủ.)

## 12. Quyết định kiến trúc

| Quyết định | Lý do |
|---|---|
| MySQL cho mọi dữ liệu | Dữ liệu quan hệ chặt; ảnh ở storage, không trong DB |
| `post_images` lưu khoá file, không lưu URL | Đổi domain/đổi sang Cloudinary không phải migrate dữ liệu |
| Ảnh bài viết qua URL ký HMAC | `<img>` không gửi được Bearer token; vẫn giữ đúng quyền xem mục 2 |
| Mã hoá lại mọi ảnh upload | Xoá EXIF GPS, chặn file polyglot, có thumbnail cho feed nhẹ |
| `reactions` PK `(post_id, user_id)` | DB tự chặn reaction 2 lần |
| `friendships` không CASCADE + CHECK | v1 không xoá tài khoản; đổi lấy toàn vẹn cặp ở DB |
| JWT 15 phút + refresh cookie HttpOnly, rotation theo family có ân hạn 10 giây | Thu hồi được phiên (chậm tối đa 15 phút); nhiều tab không tự đăng xuất nhau |
| OAuth2 Resource Server thay vì tự viết filter JWT | Ít code bảo mật tự viết, đã được kiểm chứng |
| Rate limit trong bộ nhớ (Bucket4j + Caffeine) | Chỉ chạy 1 instance; mất bộ đếm khi khởi động lại là chấp nhận được. Khi chạy nhiều instance → chuyển sang bảng DB hoặc Redis |
| Cursor pagination | Không trùng/sót khi có bài mới; trang cá nhân không chậm dần |
| Bộ đếm lưu sẵn trong `posts` | Feed không `COUNT(*)` từng bài |
| Testcontainers thay H2 | Test đúng MySQL thật (CHECK, ENUM, collation) |
| Flyway, `ddl-auto=validate` | Schema có lịch sử phiên bản |
| gitleaks thay regex tự viết | Bộ luật phát hiện bí mật đầy đủ, chạy cả ở hook và CI |
