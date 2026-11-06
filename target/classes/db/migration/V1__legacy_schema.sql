-- ============================================================================
--  PickleBook - Schema cơ sở dữ liệu
--  Tương thích: MySQL 8 (utf8mb4) và H2 2.x MODE=MySQL (profile dev)
--  Thứ tự tạo bảng tuân theo phụ thuộc khóa ngoại.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1. Phân quyền
-- ---------------------------------------------------------------------------
CREATE TABLE roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(30)  NOT NULL,
    description VARCHAR(200),
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

-- ---------------------------------------------------------------------------
-- 2. Người dùng
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(160) NOT NULL,
    phone         VARCHAR(20),
    password_hash VARCHAR(100) NOT NULL,
    avatar_url    VARCHAR(500),
    role_id       BIGINT       NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
);
CREATE INDEX idx_users_role_status ON users (role_id, status);

-- ---------------------------------------------------------------------------
-- 3. Token đặt lại mật khẩu
-- ---------------------------------------------------------------------------
CREATE TABLE password_reset_tokens (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME    NOT NULL,
    used_at    DATETIME,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_prt_token UNIQUE (token_hash),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_prt_user ON password_reset_tokens (user_id);

-- ---------------------------------------------------------------------------
-- 4. Sân
-- ---------------------------------------------------------------------------
CREATE TABLE courts (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    name                  VARCHAR(150)  NOT NULL,
    slug                  VARCHAR(170)  NOT NULL,
    description           TEXT,
    court_type            VARCHAR(20)   NOT NULL DEFAULT 'OUTDOOR',
    status                VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    base_price            DECIMAL(12,0) NOT NULL DEFAULT 0,
    slot_minutes          INT           NOT NULL DEFAULT 60,
    buffer_minutes        INT           NOT NULL DEFAULT 0,
    min_notice_minutes    INT           NOT NULL DEFAULT 60,
    max_advance_days      INT           NOT NULL DEFAULT 30,
    max_slots_per_booking INT           NOT NULL DEFAULT 4,
    amenities             VARCHAR(500),
    sort_order            INT           NOT NULL DEFAULT 0,
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_courts_slug UNIQUE (slug)
);

-- ---------------------------------------------------------------------------
-- 5. Ảnh sân
-- ---------------------------------------------------------------------------
CREATE TABLE court_images (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    court_id   BIGINT       NOT NULL,
    image_url  VARCHAR(500) NOT NULL,
    alt_text   VARCHAR(200),
    is_primary BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_court_images_court FOREIGN KEY (court_id) REFERENCES courts (id) ON DELETE CASCADE
);
CREATE INDEX idx_court_images_court ON court_images (court_id);

-- ---------------------------------------------------------------------------
-- 6. Giờ mở cửa theo thứ (1 = Thứ 2 ... 7 = Chủ nhật)
-- ---------------------------------------------------------------------------
CREATE TABLE court_schedules (
    id          BIGINT    NOT NULL AUTO_INCREMENT,
    court_id    BIGINT    NOT NULL,
    day_of_week SMALLINT  NOT NULL,
    open_time   TIME      NOT NULL,
    close_time  TIME      NOT NULL,
    closed      BOOLEAN   NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_court_schedule_day UNIQUE (court_id, day_of_week),
    CONSTRAINT fk_court_schedule_court FOREIGN KEY (court_id) REFERENCES courts (id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------------
-- 7. Thời gian bị chặn (ngày lễ, bảo trì, giải đấu)
-- ---------------------------------------------------------------------------
CREATE TABLE blocked_times (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    court_id        BIGINT,
    block_type      VARCHAR(20)  NOT NULL DEFAULT 'MAINTENANCE',
    title           VARCHAR(200) NOT NULL,
    start_at        DATETIME     NOT NULL,
    end_at          DATETIME     NOT NULL,
    affects_pricing BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by      BIGINT,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_blocked_court FOREIGN KEY (court_id) REFERENCES courts (id) ON DELETE CASCADE,
    CONSTRAINT fk_blocked_creator FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
);
CREATE INDEX idx_blocked_court_time ON blocked_times (court_id, start_at, end_at);

-- ---------------------------------------------------------------------------
-- 8. Bảng giá
-- ---------------------------------------------------------------------------
CREATE TABLE pricing_rules (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    court_id       BIGINT,
    day_type       VARCHAR(20)   NOT NULL DEFAULT 'WEEKDAY',
    start_time     TIME          NOT NULL,
    end_time       TIME          NOT NULL,
    price_per_hour DECIMAL(12,0) NOT NULL,
    priority       INT           NOT NULL DEFAULT 0,
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT fk_pricing_court FOREIGN KEY (court_id) REFERENCES courts (id) ON DELETE CASCADE
);
CREATE INDEX idx_pricing_court_day ON pricing_rules (court_id, day_type);

-- ---------------------------------------------------------------------------
-- 9. Mã giảm giá
-- ---------------------------------------------------------------------------
CREATE TABLE coupons (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    code             VARCHAR(40)   NOT NULL,
    description      VARCHAR(255),
    discount_type    VARCHAR(20)   NOT NULL DEFAULT 'PERCENT',
    discount_value   DECIMAL(12,0) NOT NULL,
    min_order_amount DECIMAL(12,0) NOT NULL DEFAULT 0,
    max_discount     DECIMAL(12,0),
    valid_from       DATETIME      NOT NULL,
    valid_to         DATETIME      NOT NULL,
    usage_limit      INT           NOT NULL DEFAULT 0,
    used_count       INT           NOT NULL DEFAULT 0,
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_coupons_code UNIQUE (code)
);

-- ---------------------------------------------------------------------------
-- 10. Dịch vụ / phụ phí
-- ---------------------------------------------------------------------------
CREATE TABLE extra_services (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(120)  NOT NULL,
    description VARCHAR(255),
    price       DECIMAL(12,0) NOT NULL,
    unit        VARCHAR(40)   NOT NULL DEFAULT 'lần',
    active      BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order  INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- ---------------------------------------------------------------------------
-- 11. Câu hỏi bổ sung trong form đặt sân
-- ---------------------------------------------------------------------------
CREATE TABLE custom_questions (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    label        VARCHAR(255) NOT NULL,
    field_type   VARCHAR(20)  NOT NULL DEFAULT 'TEXT',
    options_json TEXT,
    required     BOOLEAN      NOT NULL DEFAULT FALSE,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order   INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- ---------------------------------------------------------------------------
-- 12. Đơn đặt sân
-- ---------------------------------------------------------------------------
CREATE TABLE bookings (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    booking_code    VARCHAR(20)   NOT NULL,
    user_id         BIGINT        NOT NULL,
    court_id        BIGINT        NOT NULL,
    customer_name   VARCHAR(120)  NOT NULL,
    customer_phone  VARCHAR(20)   NOT NULL,
    customer_email  VARCHAR(160)  NOT NULL,
    player_count    INT           NOT NULL DEFAULT 2,
    note            TEXT,
    start_at        DATETIME      NOT NULL,
    end_at          DATETIME      NOT NULL,
    slot_amount     DECIMAL(12,0) NOT NULL DEFAULT 0,
    extra_amount    DECIMAL(12,0) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12,0) NOT NULL DEFAULT 0,
    final_amount    DECIMAL(12,0) NOT NULL DEFAULT 0,
    coupon_id       BIGINT,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    payment_status  VARCHAR(20)   NOT NULL DEFAULT 'UNPAID',
    payment_method  VARCHAR(20)   NOT NULL DEFAULT 'CASH',
    manage_token_hash VARCHAR(64),
    reminder_sent_at  DATETIME,
    cancelled_at    DATETIME,
    cancel_reason   VARCHAR(500),
    created_by      BIGINT,
    version         BIGINT        NOT NULL DEFAULT 0,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_bookings_code UNIQUE (booking_code),
    CONSTRAINT uk_bookings_token UNIQUE (manage_token_hash),
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_bookings_court FOREIGN KEY (court_id) REFERENCES courts (id),
    CONSTRAINT fk_bookings_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id) ON DELETE SET NULL,
    CONSTRAINT fk_bookings_creator FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
);
CREATE INDEX idx_bookings_court_start ON bookings (court_id, start_at);
CREATE INDEX idx_bookings_status_start ON bookings (status, start_at);
CREATE INDEX idx_bookings_user_created ON bookings (user_id, created_at);

-- ---------------------------------------------------------------------------
-- 13. Khung giờ của từng đơn (nguồn chống đặt trùng)
--     active_key = 1 khi đang giữ chỗ, NULL khi đã hủy/đổi (nhường slot).
--     UNIQUE(court_id, start_at, active_key) cho phép nhiều NULL trùng nhau.
-- ---------------------------------------------------------------------------
CREATE TABLE booking_slots (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    booking_id BIGINT        NOT NULL,
    court_id   BIGINT        NOT NULL,
    start_at   DATETIME      NOT NULL,
    end_at     DATETIME      NOT NULL,
    price      DECIMAL(12,0) NOT NULL DEFAULT 0,
    active_key INT,
    PRIMARY KEY (id),
    CONSTRAINT uk_booking_slot_active UNIQUE (court_id, start_at, active_key),
    CONSTRAINT fk_slots_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_slots_court FOREIGN KEY (court_id) REFERENCES courts (id)
);
CREATE INDEX idx_slots_booking ON booking_slots (booking_id);
CREATE INDEX idx_slots_court_time ON booking_slots (court_id, start_at);

-- ---------------------------------------------------------------------------
-- 14. Dịch vụ thêm của đơn
-- ---------------------------------------------------------------------------
CREATE TABLE booking_extra_services (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    booking_id       BIGINT        NOT NULL,
    extra_service_id BIGINT        NOT NULL,
    quantity         INT           NOT NULL DEFAULT 1,
    unit_price       DECIMAL(12,0) NOT NULL,
    amount           DECIMAL(12,0) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_bes_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_bes_service FOREIGN KEY (extra_service_id) REFERENCES extra_services (id)
);
CREATE INDEX idx_bes_booking ON booking_extra_services (booking_id);

-- ---------------------------------------------------------------------------
-- 15. Câu trả lời của khách
-- ---------------------------------------------------------------------------
CREATE TABLE booking_answers (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    booking_id  BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer      TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_answers_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE,
    CONSTRAINT fk_answers_question FOREIGN KEY (question_id) REFERENCES custom_questions (id)
);
CREATE INDEX idx_answers_booking ON booking_answers (booking_id);

-- ---------------------------------------------------------------------------
-- 16. Thanh toán
-- ---------------------------------------------------------------------------
CREATE TABLE payments (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    booking_id      BIGINT        NOT NULL,
    amount          DECIMAL(12,0) NOT NULL,
    method          VARCHAR(20)   NOT NULL DEFAULT 'CASH',
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    proof_image_url VARCHAR(500),
    transaction_ref VARCHAR(100),
    paid_at         DATETIME,
    note            VARCHAR(500),
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE CASCADE
);
CREATE INDEX idx_payments_booking ON payments (booking_id);

-- ---------------------------------------------------------------------------
-- 17. Mẫu email
-- ---------------------------------------------------------------------------
CREATE TABLE email_templates (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    code       VARCHAR(50)  NOT NULL,
    name       VARCHAR(150) NOT NULL,
    subject    VARCHAR(255) NOT NULL,
    body_html  TEXT         NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_email_templates_code UNIQUE (code)
);

-- ---------------------------------------------------------------------------
-- 18. Cấu hình website (key - value)
-- ---------------------------------------------------------------------------
CREATE TABLE site_settings (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    setting_key   VARCHAR(100) NOT NULL,
    setting_value TEXT,
    setting_group VARCHAR(50)  NOT NULL DEFAULT 'GENERAL',
    description   VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_site_settings_key UNIQUE (setting_key)
);

-- ---------------------------------------------------------------------------
-- 19. Banner trang chủ
-- ---------------------------------------------------------------------------
CREATE TABLE banners (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    title      VARCHAR(200) NOT NULL,
    image_url  VARCHAR(500) NOT NULL,
    link_url   VARCHAR(500),
    sort_order INT          NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
);

-- ---------------------------------------------------------------------------
-- 20. Tin tức / bài viết
-- ---------------------------------------------------------------------------
CREATE TABLE news_posts (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    slug          VARCHAR(200) NOT NULL,
    title         VARCHAR(255) NOT NULL,
    summary       TEXT,
    content       TEXT,
    thumbnail_url VARCHAR(500),
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    published_at  DATETIME,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_news_slug UNIQUE (slug)
);

-- ---------------------------------------------------------------------------
-- 21. Nhật ký hoạt động
-- ---------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT,
    action      VARCHAR(30)  NOT NULL,
    entity_name VARCHAR(80),
    entity_id   BIGINT,
    description TEXT,
    ip_address  VARCHAR(64),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);
CREATE INDEX idx_audit_created ON audit_logs (created_at);
