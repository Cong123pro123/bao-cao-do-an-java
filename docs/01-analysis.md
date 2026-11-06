# PickleBook – Phân tích yêu cầu (Bước 1)

> Hệ thống đặt sân Pickleball online · Java 17 · Spring Boot 3.x · Thymeleaf · MySQL 8
> Múi giờ: `Asia/Ho_Chi_Minh` · Tiền tệ: VNĐ · Giao diện: Tiếng Việt

---

## 1. Mục tiêu & phạm vi

Xây dựng website cho phép khách xem sân, xem khung giờ trống và đặt sân pickleball online;
chủ sân/quản trị viên quản lý sân, giá, lịch, đơn đặt, người dùng, nội dung và cấu hình hệ thống.

**Trong phạm vi:** đặt sân theo khung giờ, chống trùng, hủy/đổi lịch, email + nhắc lịch,
dashboard, quản trị đầy đủ, phân quyền RBAC, audit log, thanh toán (mô phỏng/chuyển khoản/tại sân).

**Ngoài phạm vi hiện tại:** tích hợp cổng thanh toán thật (VNPay/MoMo), ứng dụng mobile native,
chat trực tuyến, loyalty program. Nhiều cơ sở đã được duyệt và đang được bổ sung theo từng giai đoạn.

**Nền tảng nhiều cơ sở:** `branches` sở hữu sân, khung giờ và quy tắc giá; dữ liệu sân cũ được gắn
vào hai cơ sở demo trong migration. Địa chỉ demo cần thay bằng địa chỉ vận hành đã xác minh trước khi công khai.

**Nguyên tắc tham khảo:** chỉ học *logic nghiệp vụ* của các hệ thống đặt lịch (khung giờ trống,
quy tắc khả dụng, nhắc lịch, hủy/đổi). Giao diện, logo, màu sắc, nội dung, mã nguồn **tự thiết kế 100%**
theo phong cách thể thao hiện đại (xanh lá pickleball + vàng bóng, mobile-first).

---

## 2. Tác nhân (Actors)

| Actor | Mô tả | Quyền chính |
|---|---|---|
| Guest | Khách chưa đăng nhập | Xem sân, xem lịch trống, đọc tin tức, đăng ký/đăng nhập |
| User | Khách đã có tài khoản | Đặt sân, xem/hủy/đổi lịch của mình, quản lý hồ sơ |
| Staff | Nhân viên sân | Xem & xác nhận đơn, đánh dấu hoàn thành/no-show, tạo đơn tại quầy. **Không** vào cấu hình, người dùng, giá |
| Admin | Chủ sân/quản trị | Toàn quyền hệ thống |

Phân quyền: `ROLE_USER`, `ROLE_STAFF`, `ROLE_ADMIN`. Admin kế thừa quyền Staff.

---

## 3. Danh sách Use Case

### 3.1 Tài khoản (Guest/User)
| ID | Use case |
|---|---|
| UC-01 | Đăng ký tài khoản (xác thực email trùng, mật khẩu ≥ 8 ký tự) |
| UC-02 | Đăng nhập / Đăng xuất (form login, BCrypt, CSRF) |
| UC-03 | Quên mật khẩu → gửi email chứa link token (hạn 30 phút) |
| UC-04 | Đặt lại mật khẩu bằng token (token dùng 1 lần, lưu dạng hash) |
| UC-05 | Xem/cập nhật hồ sơ (họ tên, SĐT, email, ảnh đại diện) |
| UC-06 | Đổi mật khẩu (nhập mật khẩu cũ) |

### 3.2 Đặt sân (Guest/User)
| ID | Use case |
|---|---|
| UC-07 | Xem danh sách sân + lọc theo loại/giá/tiện ích |
| UC-08 | Xem chi tiết sân (ảnh, mô tả, giá, tiện ích, chính sách) |
| UC-09 | Chọn sân + ngày → xem lưới khung giờ trống/đã đặt/bảo trì |
| UC-10 | Chọn nhiều khung giờ liên tiếp → tính tiền tự động (AJAX) |
| UC-11 | Nhập form xác nhận + câu hỏi bổ sung + dịch vụ thêm + mã giảm giá |
| UC-12 | Gửi đơn → server kiểm tra trùng + transaction + khóa bi quan → tạo booking code |
| UC-13 | Xem trang thành công (mã đặt sân, thông tin, nút hủy/đổi qua token) |

### 3.3 Quản lý đơn của tôi (User)
| ID | Use case |
|---|---|
| UC-14 | Danh sách đơn: Sắp tới / Đã hoàn thành / Đã hủy |
| UC-15 | Hủy đơn nếu còn ≥ X giờ trước giờ chơi (X cấu hình) |
| UC-16 | Đổi lịch sang khung giờ trống khác trước hạn cho phép |
| UC-17 | Hủy/đổi lịch bằng link token trong email (không cần đăng nhập) |

### 3.4 Thông báo
| ID | Use case |
|---|---|
| UC-18 | Email xác nhận đặt sân (template Admin sửa được, có biến) |
| UC-19 | Email nhắc lịch trước X giờ (@Scheduled, chống gửi trùng) |
| UC-20 | Email khi hủy/đổi lịch |
| UC-21 | Email đặt lại mật khẩu |

### 3.5 Thanh toán (tùy chọn – đề xuất làm)
| ID | Use case |
|---|---|
| UC-22 | Chọn phương thức: tại sân / chuyển khoản (upload ảnh) / online (mô phỏng) |
| UC-23 | Admin xác nhận thanh toán, cập nhật trạng thái, hoàn tiền |

### 3.6 Quản trị (Admin/Staff)
| ID | Use case |
|---|---|
| UC-24 | Dashboard: đơn hôm nay/tuần/tháng, doanh thu, tỷ lệ lấp đầy, giờ cao điểm, sân hot, biểu đồ |
| UC-25 | Danh sách đơn sắp diễn ra trong ngày |
| UC-26 | CRUD sân + nhiều ảnh + trạng thái (hoạt động/bảo trì/ẩn) |
| UC-27 | Cấu hình khả dụng: giờ mở cửa theo thứ, độ dài slot, buffer, min notice, max advance, max slot/lần |
| UC-28 | Chặn lịch: ngày lễ, bảo trì, giải đấu (theo ngày hoặc khoảng giờ) |
| UC-29 | Bảng giá theo khung giờ + loại ngày (thường/cuối tuần/lễ) |
| UC-30 | Phụ phí dịch vụ (vợt, bóng, nước, trọng tài) |
| UC-31 | Mã giảm giá (%/VNĐ, hạn dùng, giới hạn lượt) |
| UC-32 | Danh sách đơn: lọc + tìm kiếm + phân trang |
| UC-33 | Chi tiết đơn: xác nhận/hủy/đổi lịch/hoàn thành/no-show |
| UC-34 | Tạo đơn thủ công (khách gọi điện / tại quầy) |
| UC-35 | Xem lịch dạng calendar ngày/tuần theo sân |
| UC-36 | Xuất đơn ra CSV (mở được bằng Excel, UTF-8 BOM) |
| UC-37 | Quản lý người dùng: tìm/lọc/vai trò/khóa/mở/reset mật khẩu |
| UC-38 | Xem chi tiết user + lịch sử đặt |
| UC-39 | Audit log: ai làm gì, lúc nào, IP |
| UC-40 | Cấu hình website: thông tin sân, logo, mạng xã hội, banner, tin tức, chính sách |
| UC-41 | Quản lý email template + câu hỏi bổ sung + cấu hình SMTP |

---

## 4. Danh sách màn hình & URL

> Quy ước: URL công khai dùng tiếng Việt không dấu; khu vực quản trị dùng `/admin/...`.
> Mọi thao tác thay đổi dữ liệu dùng `POST` + CSRF token.

### 4.1 Công khai
| Method | URL | Màn hình |
|---|---|---|
| GET | `/` | Trang chủ: banner, sân nổi bật, tin tức, CTA đặt sân |
| GET | `/san` | Danh sách sân + bộ lọc |
| GET | `/san/{slug}` | Chi tiết sân |
| GET | `/dat-san` | Trang đặt sân: chọn sân, calendar, lưới giờ |
| GET | `/api/courts/{courtId}/slots?date=yyyy-MM-dd` | JSON khung giờ trống |
| POST | `/api/booking/quote` | JSON báo giá tạm tính (slot + dịch vụ + coupon) |
| POST | `/dat-san` | Tạo đơn đặt sân (yêu cầu đăng nhập) |
| GET | `/dat-san/thanh-cong/{code}` | Trang thành công |
| GET | `/quan-ly-dat-san?token=...` | Quản lý đơn qua token email |
| POST | `/quan-ly-dat-san/huy` | Hủy qua token |
| GET/POST | `/quan-ly-dat-san/doi-lich` | Đổi lịch qua token |
| GET | `/tin-tuc`, `/tin-tuc/{slug}` | Tin tức / bài viết |
| GET | `/gioi-thieu`, `/chinh-sach` | Giới thiệu, quy định & chính sách |

### 4.2 Xác thực
| Method | URL | Màn hình |
|---|---|---|
| GET/POST | `/dang-nhap` | Đăng nhập |
| GET/POST | `/dang-ky` | Đăng ký |
| POST | `/dang-xuat` | Đăng xuất |
| GET/POST | `/quen-mat-khau` | Quên mật khẩu |
| GET/POST | `/dat-lai-mat-khau` | Đặt lại mật khẩu (`?token=`) |

### 4.3 Người dùng
| Method | URL | Màn hình |
|---|---|---|
| GET | `/lich-cua-toi` | Danh sách đơn (tab Sắp tới/Hoàn thành/Đã hủy) |
| GET | `/lich-cua-toi/{id}` | Chi tiết đơn của tôi |
| POST | `/lich-cua-toi/{id}/huy` | Hủy đơn |
| GET/POST | `/lich-cua-toi/{id}/doi-lich` | Đổi lịch |
| GET/POST | `/tai-khoan` | Hồ sơ cá nhân |
| GET/POST | `/tai-khoan/mat-khau` | Đổi mật khẩu |

### 4.4 Quản trị
| Method | URL | Màn hình |
|---|---|---|
| GET | `/admin` | Redirect → dashboard |
| GET | `/admin/dashboard` | Dashboard + Chart.js |
| GET | `/admin/courts` | Danh sách sân |
| GET/POST | `/admin/courts/new`, `/admin/courts/{id}/edit` | Thêm/sửa sân |
| POST | `/admin/courts/{id}/delete` | Ẩn/xóa sân |
| GET/POST | `/admin/courts/{id}/images` | Quản lý ảnh sân |
| GET/POST | `/admin/courts/{id}/schedule` | Giờ mở cửa theo thứ |
| GET/POST | `/admin/courts/{id}/blocked-times` | Chặn lịch |
| GET/POST | `/admin/pricing` | Bảng giá |
| GET/POST | `/admin/coupons` | Mã giảm giá |
| GET/POST | `/admin/services` | Dịch vụ & phụ phí |
| GET | `/admin/bookings` | Danh sách đơn (lọc/tìm/phân trang) |
| GET/POST | `/admin/bookings/new` | Tạo đơn thủ công |
| GET | `/admin/bookings/{id}` | Chi tiết đơn |
| POST | `/admin/bookings/{id}/status` | Xác nhận/hủy/hoàn thành/no-show |
| GET/POST | `/admin/bookings/{id}/reschedule` | Đổi lịch hộ khách |
| GET | `/admin/bookings/calendar` | Calendar ngày/tuần |
| GET | `/admin/bookings/export` | Xuất CSV |
| GET | `/admin/users` | Danh sách người dùng |
| GET/POST | `/admin/users/new`, `/admin/users/{id}/edit` | Thêm/sửa user |
| POST | `/admin/users/{id}/lock`, `/unlock`, `/reset-password` | Khóa/mở/reset |
| GET | `/admin/audit-logs` | Nhật ký hoạt động |
| GET/POST | `/admin/settings` | Cấu hình chung + chính sách + SMTP |
| GET/POST | `/admin/settings/policies` | Chính sách hủy/đổi, cọc, nhắc lịch |
| GET/POST | `/admin/email-templates` | Email template |
| GET/POST | `/admin/questions` | Câu hỏi bổ sung |
| GET/POST | `/admin/banners` | Banner trang chủ |
| GET/POST | `/admin/news` | Tin tức |

### 4.5 Trang lỗi
`/403`, `/404`, `/500`, `/error` – thân thiện, có nút về trang chủ.

---

## 5. Quy tắc nghiệp vụ (Business Rules)

| ID | Quy tắc |
|---|---|
| BR-01 | Khung giờ sinh động từ `court_schedules` của thứ tương ứng + `slot_minutes`; không lưu sẵn bảng slot |
| BR-02 | Trạng thái khung giờ: `AVAILABLE`, `BOOKED`, `MAINTENANCE`, `PAST`, `OUT_OF_HOURS` (không hiển thị) |
| BR-03 | Buffer: một slot bị coi là không trống nếu chồng lấn với `[start - buffer, end + buffer)` của booking khác; các slot liền nhau **trong cùng một đơn** không bị buffer |
| BR-04 | Không cho đặt nếu `start < now + min_notice_minutes` |
| BR-05 | Không cho đặt quá `max_advance_days` ngày kể từ hôm nay |
| BR-06 | Tối đa `max_slots_per_booking` khung giờ mỗi đơn; các slot phải **liên tiếp cùng sân, cùng ngày** |
| BR-07 | Chống trùng: UNIQUE `(court_id, start_at, active_key)` trên `booking_slots` + khóa bi quan trên hàng `courts` + transaction; bắt `DataIntegrityViolationException` → báo "khung giờ vừa được đặt" |
| BR-08 | Hủy: chỉ khi `now <= start_at - cancel_before_hours` và trạng thái `PENDING/CONFIRMED`; hủy giải phóng slot (`active_key = NULL`) |
| BR-09 | Đổi lịch: cùng điều kiện BR-08; tạo slot mới + giải phóng slot cũ trong 1 transaction |
| BR-10 | Giá: chọn `pricing_rules` khớp loại ngày (HOLIDAY > WEEKEND > WEEKDAY) và khung giờ; nếu không có → giá mặc định của sân. Giá được **snapshot** vào `booking_slots.price` |
| BR-11 | Coupon: kiểm tra hạn, `min_order_amount`, `usage_limit`; tăng `used_count` trong transaction |
| BR-12 | Đơn `PENDING` quá 15 phút chưa xác nhận/thanh toán → scheduler tự hủy (cấu hình được) |
| BR-13 | Sau giờ kết thúc + 30 phút, đơn `CONFIRMED` tự chuyển `COMPLETED` (scheduler) |
| BR-14 | Nhắc lịch: gửi 1 lần trước `reminder_hours` giờ, đánh dấu `reminder_sent_at` để chống trùng |
| BR-15 | No-show do Staff/Admin đánh dấu; không hoàn tiền tự động |
| BR-16 | Mọi thao tác quản trị ghi `audit_logs` (ai, làm gì, entity, thời gian, IP) |
| BR-17 | Staff bị chặn ở: cấu hình, người dùng, giá, coupon, nội dung, email template (trả 403) |
| BR-18 | Token trong email: chuỗi 32 bytes ngẫu nhiên, lưu **hash SHA-256**, có hạn, dùng 1 lần |
| BR-19 | Xóa mềm: sân/user có đơn → chỉ đổi trạng thái, không xóa vật lý |
| BR-20 | Tiền dùng `BigDecimal`, làm tròn 0 chữ số thập phân khi hiển thị VNĐ |

---

## 6. Yêu cầu phi chức năng

- **Hiệu năng:** phân trang mặc định 20; index cho các truy vấn lọc đơn; slot generation O(số slot) trong ngày.
- **Bảo mật:** BCrypt(10), CSRF mọi POST, XSS do Thymeleaf escape mặc định + sanitize nội dung HTML của Admin (Jsoup whitelist), chống SQL Injection nhờ JPA, session timeout 30 phút, cookie HttpOnly, `Content-Security-Policy` cơ bản, rate-limit đơn giản cho login/forgot-password.
- **Đồng thời:** transaction + pessimistic lock; optimistic `@Version` trên `Booking`.
- **Khả dụng:** mobile-first, responsive, accessibility cơ bản (label, focus, contrast).
- **Vận hành:** log SLF4J, profile `dev`/`prod`, secret qua biến môi trường.
- **Kiểm thử:** JUnit 5 + Mockito cho SlotService, BookingService, PricingService, CancellationService.

---

## 7. Giả định & điểm cần xác nhận

1. Một cơ sở sân duy nhất (single-tenant), mọi giờ lưu theo `Asia/Ho_Chi_Minh`.
2. Một sân có 1 khung mở/đóng cho mỗi thứ (chưa hỗ trợ nhiều ca/ngày như 6–10h và 16–22h); có thể mở rộng sau.
3. Giá tính theo khung giờ (mỗi slot lấy giá tại `start_at` của slot).
4. Ảnh upload lưu local `uploads/` (có thể đổi sang S3 sau), giới hạn 5MB, jpg/png/webp.
5. Email khi chưa cấu hình SMTP sẽ ghi ra log (chế độ demo) để dự án chạy được ngay.
6. Thanh toán online chỉ **mô phỏng** (trang giả lập xác nhận), không gọi cổng thật.
7. Không có đăng nhập bằng Google/Facebook.
