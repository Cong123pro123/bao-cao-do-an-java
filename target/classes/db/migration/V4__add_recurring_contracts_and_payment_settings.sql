-- ============================================================================
-- V4: Bổ sung Hợp đồng đặt cố định theo tháng và Cấu hình ngân hàng VietQR
-- ============================================================================

-- Bảng hợp đồng đặt sân cố định theo tuần/tháng
CREATE TABLE recurring_contracts (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    contract_code       VARCHAR(50)     NOT NULL,
    court_id            BIGINT          NOT NULL,
    user_id             BIGINT,
    customer_name       VARCHAR(120)    NOT NULL,
    customer_phone      VARCHAR(20)     NOT NULL,
    customer_email      VARCHAR(160)    NOT NULL,
    start_date          DATE            NOT NULL,
    end_date            DATE            NOT NULL,
    days_of_week        VARCHAR(100)    NOT NULL,
    start_time          TIME            NOT NULL,
    end_time            TIME            NOT NULL,
    total_sessions      INT             NOT NULL DEFAULT 0,
    price_per_session   DECIMAL(12, 0)  NOT NULL DEFAULT 0,
    total_amount        DECIMAL(12, 0)  NOT NULL DEFAULT 0,
    discount_amount     DECIMAL(12, 0)  NOT NULL DEFAULT 0,
    deposit_amount      DECIMAL(12, 0)  NOT NULL DEFAULT 0,
    final_amount        DECIMAL(12, 0)  NOT NULL DEFAULT 0,
    payment_method      VARCHAR(30)     NOT NULL DEFAULT 'BANK_TRANSFER',
    payment_status      VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    note                TEXT,
    manage_token_hash   VARCHAR(64),
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_recurring_contract_code UNIQUE (contract_code),
    CONSTRAINT fk_recurring_contract_court FOREIGN KEY (court_id) REFERENCES courts (id) ON DELETE CASCADE,
    CONSTRAINT fk_recurring_contract_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_recurring_court_dates ON recurring_contracts (court_id, start_date, end_date, status);
CREATE INDEX idx_recurring_user ON recurring_contracts (user_id);

-- Bảng chi tiết từng buổi trong hợp đồng cố định
CREATE TABLE recurring_contract_sessions (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    contract_id         BIGINT          NOT NULL,
    session_date        DATE            NOT NULL,
    start_time          TIME            NOT NULL,
    end_time            TIME            NOT NULL,
    price               DECIMAL(12, 0)  NOT NULL DEFAULT 0,
    status              VARCHAR(30)     NOT NULL DEFAULT 'SCHEDULED',
    booking_id          BIGINT,
    note                VARCHAR(255),
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_recurring_session_contract FOREIGN KEY (contract_id) REFERENCES recurring_contracts (id) ON DELETE CASCADE,
    CONSTRAINT fk_recurring_session_booking FOREIGN KEY (booking_id) REFERENCES bookings (id) ON DELETE SET NULL
);

CREATE INDEX idx_recurring_session_date ON recurring_contract_sessions (session_date, start_time, end_time, status);

-- Cấu hình tài khoản ngân hàng thật nhận tiền VietQR
INSERT INTO site_settings (setting_key, setting_value, setting_group, description) VALUES
    ('payment.bank_name', 'VietinBank (Ngân hàng TMCP Công Thương Việt Nam)', 'PAYMENT', 'Tên ngân hàng thụ hưởng'),
    ('payment.bank_bin', 'ICB', 'PAYMENT', 'Mã ngân hàng VietQR (ICB, VCB, MB, TCB...)'),
    ('payment.bank_account_no', '102874659281', 'PAYMENT', 'Số tài khoản ngân hàng thụ hưởng'),
    ('payment.bank_account_name', 'CONG BAO PICKLEBALL', 'PAYMENT', 'Tên chủ tài khoản ngân hàng thụ hưởng'),
    ('payment.deposit_percent', '30', 'PAYMENT', 'Phần trăm đặt cọc tối thiểu (%)');
