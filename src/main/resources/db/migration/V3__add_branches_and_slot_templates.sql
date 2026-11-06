CREATE TABLE branches (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(170) NOT NULL,
    address     VARCHAR(500) NOT NULL,
    district    VARCHAR(100),
    city        VARCHAR(100) NOT NULL DEFAULT 'Thành phố Hồ Chí Minh',
    phone       VARCHAR(20),
    email       VARCHAR(160),
    timezone    VARCHAR(60)  NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_branches_slug UNIQUE (slug)
);

INSERT INTO branches (id, name, slug, address, district, city, phone, status) VALUES
    (1, 'PickleBook - Quận 1', 'picklebook-quan-1',
     'Đường Nguyễn Trãi, Phường Bến Thành', 'Quận 1', 'Thành phố Hồ Chí Minh',
     '02838220001', 'ACTIVE'),
    (2, 'PickleBook - Quận 7', 'picklebook-quan-7',
     'Đường Nguyễn Thị Thập, Phường Tân Phú', 'Quận 7', 'Thành phố Hồ Chí Minh',
     '02838220002', 'ACTIVE');

ALTER TABLE courts ADD COLUMN branch_id BIGINT NOT NULL DEFAULT 1;
UPDATE courts SET branch_id = CASE WHEN id <= 2 THEN 1 ELSE 2 END;
ALTER TABLE courts ALTER COLUMN branch_id DROP DEFAULT;
ALTER TABLE courts ADD CONSTRAINT fk_courts_branch
    FOREIGN KEY (branch_id) REFERENCES branches (id);
CREATE INDEX idx_courts_branch_status ON courts (branch_id, status);

ALTER TABLE pricing_rules ADD COLUMN branch_id BIGINT;
UPDATE pricing_rules SET branch_id = 1 WHERE court_id IS NULL;
ALTER TABLE pricing_rules ADD CONSTRAINT fk_pricing_branch
    FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE CASCADE;
CREATE INDEX idx_pricing_branch_day ON pricing_rules (branch_id, day_type);

CREATE TABLE slot_templates (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    branch_id   BIGINT      NOT NULL,
    day_type    VARCHAR(20) NOT NULL,
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    period_name VARCHAR(30) NOT NULL,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    sort_order  INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_slot_template_time UNIQUE (branch_id, day_type, start_time, end_time),
    CONSTRAINT fk_slot_template_branch FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE CASCADE
);
CREATE INDEX idx_slot_templates_branch_day
    ON slot_templates (branch_id, day_type, active, start_time);
