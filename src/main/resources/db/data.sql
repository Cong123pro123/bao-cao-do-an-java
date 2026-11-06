-- ============================================================================
--  PickleBook - Dữ liệu mẫu
--  Tài khoản demo:
--    admin@picklebook.vn  / Admin@123   (ROLE_ADMIN)
--    staff@picklebook.vn  / Staff@123   (ROLE_STAFF)
--    user@picklebook.vn   / User@123    (ROLE_USER)
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Vai trò
-- ---------------------------------------------------------------------------
INSERT INTO roles (id, name, description) VALUES
    (1, 'ROLE_ADMIN', 'Quản trị viên toàn quyền'),
    (2, 'ROLE_STAFF', 'Nhân viên sân - xử lý đơn đặt'),
    (3, 'ROLE_USER',  'Khách hàng đặt sân');

-- ---------------------------------------------------------------------------
-- Người dùng (mật khẩu BCrypt strength 10)
-- ---------------------------------------------------------------------------
INSERT INTO users (id, full_name, email, phone, password_hash, role_id, status) VALUES
    (1, 'Quản trị PickleBook', 'admin@picklebook.vn', '0909123456',
     '$2a$10$z5r5vDJ8mIuPx/Ngy1NqmOmgIs55lyfNwwk3R3XQQ6OcfKRcIquZy', 1, 'ACTIVE'),
    (2, 'Nhân viên sân', 'staff@picklebook.vn', '0909123457',
     '$2a$10$NWGNasTLV7gvclYEh0EaK.EyHqrlD2LGXf9W/UWS0DCT3TWvv4m2y', 2, 'ACTIVE'),
    (3, 'Nguyễn Minh Khang', 'user@picklebook.vn', '0909123458',
     '$2a$10$VPumbqiQd7Oj74lH8LjiyuiFEodzOWVmFPmjn.KWnSTsvx7E2r5rK', 3, 'ACTIVE');

-- ---------------------------------------------------------------------------
-- Sân
-- ---------------------------------------------------------------------------
INSERT INTO courts (id, name, slug, description, court_type, status, base_price,
                    slot_minutes, buffer_minutes, min_notice_minutes, max_advance_days,
                    max_slots_per_booking, amenities, sort_order) VALUES
    (1, 'Sân Trung Tâm', 'san-trung-tam',
     'Sân trong nhà tiêu chuẩn thi đấu, mặt sàn acrylic chống trượt, hệ thống đèn LED chống chói. Phù hợp cho cả người mới chơi và giải đấu phong trào.',
     'INDOOR', 'ACTIVE', 150000, 60, 10, 60, 30, 4,
     'Mái che|Đèn LED|Wi-Fi|Bãi xe|Phòng thay đồ|Căng tin', 1),
    (2, 'Sân Góc Phố', 'san-goc-pho',
     'Sân ngoài trời thoáng mát, view thành phố, có mái che di động khi trời mưa nhẹ. Không gian phù hợp nhóm bạn và gia đình.',
     'OUTDOOR', 'ACTIVE', 130000, 60, 10, 60, 30, 4,
     'Wi-Fi|Bãi xe|Căng tin|Nước uống', 2),
    (3, 'Sân Sunrise', 'san-sunrise',
     'Sân trong nhà hướng đông, đón nắng sớm, thảm sàn cao cấp giảm chấn. Có khu khởi động riêng và tủ đồ cá nhân.',
     'INDOOR', 'ACTIVE', 160000, 90, 15, 120, 21, 3,
     'Mái che|Điều hòa|Wi-Fi|Tủ đồ|Phòng tắm nóng lạnh', 3),
    (4, 'Sân Đồi Thông', 'san-doi-thong',
     'Cụm sân ngoài trời giữa vườn thông, không khí trong lành. Có khu BBQ và cà phê sân vườn cho nhóm đông người.',
     'OUTDOOR', 'MAINTENANCE', 140000, 60, 10, 120, 30, 4,
     'Bãi xe|Cà phê sân vườn|Khu BBQ|Wi-Fi', 4);

INSERT INTO court_images (court_id, image_url, alt_text, is_primary, sort_order) VALUES
    (1, '/img/courts/court-1.svg', 'Sân Trung Tâm - sân trong nhà', TRUE, 1),
    (1, '/img/courts/court-placeholder.svg', 'Khu vực khán đài Sân Trung Tâm', FALSE, 2),
    (2, '/img/courts/court-2.svg', 'Sân Góc Phố - sân ngoài trời', TRUE, 1),
    (2, '/img/courts/court-placeholder.svg', 'Khu căng tin Sân Góc Phố', FALSE, 2),
    (3, '/img/courts/court-3.svg', 'Sân Sunrise - sân trong nhà', TRUE, 1),
    (3, '/img/courts/court-placeholder.svg', 'Phòng thay đồ Sân Sunrise', FALSE, 2),
    (4, '/img/courts/court-4.svg', 'Sân Đồi Thông - sân ngoài trời', TRUE, 1),
    (4, '/img/courts/court-placeholder.svg', 'Khu cà phê Sân Đồi Thông', FALSE, 2);

-- Giờ mở cửa: Thứ 2 - Thứ 6 (1..5) 06:00-22:00, Thứ 7 - CN (6..7) 06:00-23:00
INSERT INTO court_schedules (court_id, day_of_week, open_time, close_time, closed) VALUES
    (1,1,'06:00','22:00',FALSE),(1,2,'06:00','22:00',FALSE),(1,3,'06:00','22:00',FALSE),
    (1,4,'06:00','22:00',FALSE),(1,5,'06:00','22:00',FALSE),(1,6,'06:00','23:00',FALSE),(1,7,'06:00','23:00',FALSE),
    (2,1,'06:00','22:00',FALSE),(2,2,'06:00','22:00',FALSE),(2,3,'06:00','22:00',FALSE),
    (2,4,'06:00','22:00',FALSE),(2,5,'06:00','22:00',FALSE),(2,6,'06:00','23:00',FALSE),(2,7,'06:00','23:00',FALSE),
    (3,1,'05:30','21:30',FALSE),(3,2,'05:30','21:30',FALSE),(3,3,'05:30','21:30',FALSE),
    (3,4,'05:30','21:30',FALSE),(3,5,'05:30','21:30',FALSE),(3,6,'05:30','22:30',FALSE),(3,7,'05:30','22:30',FALSE),
    (4,1,'06:00','21:00',FALSE),(4,2,'06:00','21:00',FALSE),(4,3,'06:00','21:00',FALSE),
    (4,4,'06:00','21:00',FALSE),(4,5,'06:00','21:00',FALSE),(4,6,'06:00','22:00',FALSE),(4,7,'06:00','22:00',FALSE);

-- Ngày lễ Tết nguyên đán 2027 (chặn toàn hệ thống, dùng cho giá ngày lễ)
INSERT INTO blocked_times (court_id, block_type, title, start_at, end_at, affects_pricing, created_by) VALUES
    (NULL, 'HOLIDAY', 'Nghỉ Tết Nguyên Đán 2027', '2027-02-15 00:00:00', '2027-02-20 23:59:59', TRUE, 1),
    (1, 'MAINTENANCE', 'Bảo trì mặt sàn định kỳ', '2026-12-10 08:00:00', '2026-12-10 12:00:00', FALSE, 1);

-- ---------------------------------------------------------------------------
-- Bảng giá
-- ---------------------------------------------------------------------------
INSERT INTO pricing_rules (court_id, day_type, start_time, end_time, price_per_hour, priority) VALUES
    (1,'WEEKDAY','05:00','16:59',130000,1),(1,'WEEKDAY','17:00','22:00',190000,2),(1,'WEEKEND','05:00','23:00',210000,1),
    (2,'WEEKDAY','05:00','16:59',120000,1),(2,'WEEKDAY','17:00','22:00',170000,2),(2,'WEEKEND','05:00','23:00',190000,1),
    (3,'WEEKDAY','05:00','16:59',150000,1),(3,'WEEKDAY','17:00','22:00',200000,2),(3,'WEEKEND','05:00','23:00',220000,1),
    (4,'WEEKDAY','05:00','16:59',125000,1),(4,'WEEKDAY','17:00','22:00',175000,2),(4,'WEEKEND','05:00','23:00',195000,1),
    (NULL,'HOLIDAY','00:00','23:59',260000,10);

-- ---------------------------------------------------------------------------
-- Dịch vụ thêm
-- ---------------------------------------------------------------------------
INSERT INTO extra_services (id, name, description, price, unit, active, sort_order) VALUES
    (1, 'Thuê vợt', 'Vợt pickleball cao cấp, nhiều kích cỡ', 30000, 'cây/lần', TRUE, 1),
    (2, 'Thuê bóng', 'Ống 3 bóng thi đấu', 20000, 'ống/lần', TRUE, 2),
    (3, 'Trọng tài', 'Trọng tài có chứng nhận cho trận đấu', 150000, 'trận', TRUE, 3),
    (4, 'Nước suối', 'Chai nước 500ml', 10000, 'chai', TRUE, 4);

-- ---------------------------------------------------------------------------
-- Câu hỏi bổ sung
-- ---------------------------------------------------------------------------
INSERT INTO custom_questions (id, label, field_type, options_json, required, active, sort_order) VALUES
    (1, 'Bạn cần thuê vợt không?', 'SELECT', '["Không cần","Thuê 2 vợt","Thuê 4 vợt"]', TRUE, TRUE, 1),
    (2, 'Trình độ chơi của bạn?', 'SELECT', '["Mới chơi","Trung bình","Khá","Nâng cao"]', FALSE, TRUE, 2),
    (3, 'Bạn có cần trọng tài không?', 'CHECKBOX', '["Có"]', FALSE, TRUE, 3),
    (4, 'Yêu cầu thêm (nếu có)', 'TEXT', NULL, FALSE, TRUE, 4);

-- ---------------------------------------------------------------------------
-- Mã giảm giá
-- ---------------------------------------------------------------------------
INSERT INTO coupons (code, description, discount_type, discount_value, min_order_amount,
                     max_discount, valid_from, valid_to, usage_limit, used_count, active) VALUES
    ('PICKLE10', 'Giảm 10% tối đa 50.000đ cho đơn từ 100.000đ', 'PERCENT', 10, 100000, 50000,
     '2026-01-01 00:00:00', '2027-12-31 23:59:59', 500, 0, TRUE),
    ('NEWBIE50K', 'Giảm 50.000đ cho khách hàng mới, đơn từ 200.000đ', 'FIXED', 50000, 200000, NULL,
     '2026-01-01 00:00:00', '2027-12-31 23:59:59', 200, 0, TRUE);

-- ---------------------------------------------------------------------------
-- Mẫu email (Admin có thể sửa trong /admin/email-templates)
-- Biến: {customerName} {bookingCode} {court} {date} {time} {total}
--       {manageUrl} {siteName} {sitePhone} {paymentMethod} {note}
-- ---------------------------------------------------------------------------
INSERT INTO email_templates (code, name, subject, body_html, active) VALUES
('BOOKING_CONFIRMATION', 'Xác nhận đặt sân',
 'Xác nhận đặt sân {court} ngày {date} - {bookingCode}',
 '<div style="font-family:Arial,Helvetica,sans-serif;max-width:620px;margin:0 auto;background:#f3f7f2;padding:24px">
   <div style="background:#0f7b3f;color:#ffffff;padding:22px 26px;border-radius:14px 14px 0 0">
     <h2 style="margin:0;font-size:22px">{siteName}</h2>
     <p style="margin:6px 0 0;opacity:.85">Sân chơi đỉnh cao - đặt lịch trong 30 giây</p>
   </div>
   <div style="background:#ffffff;padding:26px;border-radius:0 0 14px 14px;color:#1f2d24">
     <p>Xin chào <strong>{customerName}</strong>,</p>
     <p>Đơn đặt sân của bạn đã được ghi nhận với mã <strong style="color:#0f7b3f">{bookingCode}</strong>.</p>
     <table style="width:100%;border-collapse:collapse;margin:18px 0">
       <tr><td style="padding:9px 0;color:#6b7a70">Sân</td><td style="text-align:right;font-weight:600">{court}</td></tr>
       <tr><td style="padding:9px 0;color:#6b7a70">Ngày</td><td style="text-align:right;font-weight:600">{date}</td></tr>
       <tr><td style="padding:9px 0;color:#6b7a70">Khung giờ</td><td style="text-align:right;font-weight:600">{time}</td></tr>
       <tr><td style="padding:9px 0;color:#6b7a70">Thanh toán</td><td style="text-align:right;font-weight:600">{paymentMethod}</td></tr>
       <tr><td style="padding:9px 0;color:#6b7a70">Tổng tiền</td><td style="text-align:right;font-weight:700;color:#0f7b3f">{total}</td></tr>
     </table>
     <p style="text-align:center;margin:26px 0">
       <a href="{manageUrl}" style="background:#0f7b3f;color:#ffffff;padding:13px 26px;border-radius:9px;text-decoration:none;font-weight:600">Hủy hoặc đổi lịch</a>
     </p>
     <p style="font-size:13px;color:#6b7a70">Vui lòng đến trước giờ chơi 10 phút. Cần hỗ trợ, gọi {sitePhone}.</p>
   </div>
 </div>', TRUE),

('BOOKING_REMINDER', 'Nhắc lịch chơi',
 'Nhắc lịch: {court} lúc {time} ngày {date}',
 '<div style="font-family:Arial,Helvetica,sans-serif;max-width:620px;margin:0 auto;padding:24px;background:#f3f7f2">
   <div style="background:#ffffff;padding:26px;border-radius:14px;color:#1f2d24">
     <h2 style="color:#0f7b3f;margin-top:0">Sắp đến giờ chơi rồi!</h2>
     <p>Xin chào <strong>{customerName}</strong>, đơn <strong>{bookingCode}</strong> của bạn sẽ bắt đầu sau ít phút.</p>
     <p style="font-size:16px"><strong>{court}</strong><br>{time} - {date}</p>
     <p style="font-size:13px;color:#6b7a70">Đến sớm 10 phút để khởi động và nhận sân. Hẹn gặp bạn tại {siteName}.</p>
   </div>
 </div>', TRUE),

('BOOKING_CANCELLED', 'Hủy đơn đặt sân',
 'Đơn đặt sân {bookingCode} đã được hủy',
 '<div style="font-family:Arial,Helvetica,sans-serif;max-width:620px;margin:0 auto;padding:24px;background:#f3f7f2">
   <div style="background:#ffffff;padding:26px;border-radius:14px;color:#1f2d24">
     <h2 style="color:#b3261e;margin-top:0">Đơn đặt sân đã hủy</h2>
     <p>Xin chào <strong>{customerName}</strong>,</p>
     <p>Đơn <strong>{bookingCode}</strong> tại <strong>{court}</strong> vào {time} ngày {date} đã được hủy.</p>
     <p>Nếu bạn cần đặt lại, hãy truy cập {siteName}. Cần hỗ trợ, gọi {sitePhone}.</p>
   </div>
 </div>', TRUE),

('BOOKING_RESCHEDULED', 'Đổi lịch đặt sân',
 'Đơn {bookingCode} đã được đổi sang lịch mới',
 '<div style="font-family:Arial,Helvetica,sans-serif;max-width:620px;margin:0 auto;padding:24px;background:#f3f7f2">
   <div style="background:#ffffff;padding:26px;border-radius:14px;color:#1f2d24">
     <h2 style="color:#0f7b3f;margin-top:0">Đổi lịch thành công</h2>
     <p>Xin chào <strong>{customerName}</strong>, đơn <strong>{bookingCode}</strong> đã được chuyển sang:</p>
     <p style="font-size:16px"><strong>{court}</strong><br>{time} - {date}</p>
     <p style="font-size:13px;color:#6b7a70">Xem chi tiết và quản lý đơn tại liên kết bên dưới.</p>
   </div>
 </div>', TRUE),

('PAYMENT_CONFIRMED', 'Xác nhận thanh toán',
 'Thanh toán đơn {bookingCode} đã được xác nhận',
 '<div style="font-family:Arial,Helvetica,sans-serif;max-width:620px;margin:0 auto;padding:24px;background:#f3f7f2">
   <div style="background:#ffffff;padding:26px;border-radius:14px;color:#1f2d24">
     <h2 style="color:#0f7b3f;margin-top:0">Đã nhận thanh toán</h2>
     <p>Xin chào <strong>{customerName}</strong>,</p>
     <p>Chúng tôi đã xác nhận thanh toán <strong>{total}</strong> cho đơn <strong>{bookingCode}</strong>.</p>
     <p style="font-size:13px;color:#6b7a70">Cảm ơn bạn đã sử dụng {siteName}.</p>
   </div>
 </div>', TRUE),

('PASSWORD_RESET', 'Đặt lại mật khẩu',
 'Đặt lại mật khẩu PickleBook',
 '<div style="font-family:Arial,Helvetica,sans-serif;max-width:620px;margin:0 auto;padding:24px;background:#f3f7f2">
   <div style="background:#ffffff;padding:26px;border-radius:14px;color:#1f2d24">
     <h2 style="color:#0f7b3f;margin-top:0">Đặt lại mật khẩu</h2>
     <p>Xin chào <strong>{customerName}</strong>,</p>
     <p>Bạn (hoặc ai đó) đã yêu cầu đặt lại mật khẩu. Nhấn nút bên dưới để tạo mật khẩu mới. Liên kết có hiệu lực trong 30 phút.</p>
     <p style="text-align:center;margin:24px 0">
       <a href="{resetUrl}" style="background:#0f7b3f;color:#ffffff;padding:13px 26px;border-radius:9px;text-decoration:none;font-weight:600">Đặt lại mật khẩu</a>
     </p>
     <p style="font-size:13px;color:#6b7a70">Nếu không phải bạn, hãy bỏ qua email này. Tài khoản của bạn vẫn an toàn.</p>
   </div>
 </div>', TRUE);

-- ---------------------------------------------------------------------------
-- Cấu hình website
-- ---------------------------------------------------------------------------
INSERT INTO site_settings (setting_key, setting_value, setting_group, description) VALUES
    ('site.name', 'PickleBook Arena', 'GENERAL', 'Tên sân hiển thị trên website'),
    ('site.slogan', 'Sân chơi đỉnh cao, đặt lịch trong 30 giây', 'GENERAL', 'Khẩu hiệu'),
    ('site.logo', '/img/logo.svg', 'GENERAL', 'Đường dẫn logo'),
    ('site.address', '128 Nguyễn Văn Linh, Quận 7, TP. Hồ Chí Minh', 'GENERAL', 'Địa chỉ'),
    ('site.phone', '0909 123 456', 'GENERAL', 'Hotline'),
    ('site.email', 'hotline@picklebook.vn', 'GENERAL', 'Email liên hệ'),
    ('site.open_hours', '06:00 - 22:00 hằng ngày', 'GENERAL', 'Giờ làm việc'),
    ('site.facebook', 'https://facebook.com/picklebook', 'GENERAL', 'Fanpage'),
    ('site.instagram', 'https://instagram.com/picklebook', 'GENERAL', 'Instagram'),
    ('site.youtube', 'https://youtube.com/@picklebook', 'GENERAL', 'YouTube'),
    ('home.hero_title', 'Đặt sân Pickleball. Chơi là ghiền.', 'GENERAL', 'Tiêu đề hero trang chủ'),
    ('home.hero_subtitle', 'Xem khung giờ trống theo thời gian thực, đặt sân trong 30 giây, hủy đổi linh hoạt.', 'GENERAL', 'Mô tả hero'),
    ('home.hero_image', '/img/banners/banner-1.svg', 'GENERAL', 'Ảnh hero'),
    ('policy.cancel', '<p>Quý khách được hủy hoặc đổi lịch miễn phí trước giờ chơi ít nhất 12 giờ. Hủy trong vòng 12 giờ trước giờ chơi sẽ không được hoàn cọc.</p>', 'POLICY', 'Chính sách hủy sân'),
    ('policy.rules', '<ul><li>Vui lòng có mặt trước giờ chơi 10 phút.</li><li>Mang giày thể thao, không mang giày đế đen.</li><li>Không hút thuốc trong khu vực sân.</li><li>Giữ gìn vệ sinh chung và tôn trọng người chơi khác.</li></ul>', 'POLICY', 'Nội quy sân'),
    ('booking.cancel_before_hours', '12', 'BOOKING', 'Số giờ tối thiểu trước giờ chơi để được hủy'),
    ('booking.reschedule_before_hours', '12', 'BOOKING', 'Số giờ tối thiểu trước giờ chơi để được đổi lịch'),
    ('booking.deposit_percent', '30', 'BOOKING', 'Phần trăm tiền cọc'),
    ('booking.reminder_hours', '3', 'BOOKING', 'Gửi email nhắc trước X giờ'),
    ('booking.pending_timeout_minutes', '15', 'BOOKING', 'Tự hủy đơn PENDING sau X phút'),
    ('booking.auto_complete_minutes', '30', 'BOOKING', 'Tự hoàn thành đơn sau khi kết thúc X phút'),
    ('mail.from', 'PickleBook <no-reply@picklebook.vn>', 'MAIL', 'Tên và email người gửi'),
    ('mail.enabled', 'false', 'MAIL', 'Bật gửi email thật (cần cấu hình SMTP)'),
    ('seo.description', 'PickleBook - đặt sân pickleball online nhanh chóng, minh bạch giá và khung giờ.', 'GENERAL', 'Mô tả SEO'),
    ('footer.about', 'PickleBook mang đến trải nghiệm đặt sân pickleball hiện đại, minh bạch và tiện lợi cho mọi người chơi.', 'GENERAL', 'Giới thiệu ngắn ở footer');

-- ---------------------------------------------------------------------------
-- Banner & tin tức
-- ---------------------------------------------------------------------------
INSERT INTO banners (title, image_url, link_url, sort_order, active) VALUES
    ('Giải đấu PickleBook Open 2026', '/img/banners/banner-1.svg', '/tin-tuc/giai-dau-picklebook-open-2026', 1, TRUE),
    ('Ưu đãi khách mới - giảm 50.000đ', '/img/banners/banner-2.svg', '/dat-san', 2, TRUE);

INSERT INTO news_posts (slug, title, summary, content, thumbnail_url, status, published_at) VALUES
    ('giai-dau-picklebook-open-2026',
     'PickleBook Open 2026 chính thức khởi tranh',
     'Giải đấu phong trào dành cho mọi trình độ, tổng giải thưởng 50 triệu đồng.',
     '<p>PickleBook Open 2026 sẽ diễn ra trong hai ngày với 4 nội dung: đôi nam, đôi nữ, đôi nam nữ và đơn mở rộng. Giải dành cho người chơi phong trào yêu thích pickleball tại TP. Hồ Chí Minh.</p><p>Đăng ký tại quầy hoặc qua hotline của sân. Số lượng đội giới hạn.</p>',
     '/img/banners/banner-1.svg', 'PUBLISHED', '2026-10-20 09:00:00'),
    ('5-meo-choi-pickleball-hieu-qua',
     '5 mẹo chơi pickleball hiệu quả cho người mới',
     'Bắt đầu từ tư thế, nhịp thở đến cách chọn vợt phù hợp.',
     '<p>Pickleball dễ học nhưng để chơi tốt cần luyện tập đúng cách. Dưới đây là 5 mẹo dành cho người mới: giữ tư thế thấp, đánh bóng vào vùng kitchen, phối hợp với đồng đội, chọn vợt nhẹ và khởi động kỹ trước khi chơi.</p>',
     '/img/banners/banner-2.svg', 'PUBLISHED', '2026-10-28 08:30:00');
