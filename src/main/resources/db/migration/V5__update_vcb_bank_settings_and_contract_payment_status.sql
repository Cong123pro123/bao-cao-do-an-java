-- ============================================================================
-- V5: Cập nhật tài khoản ngân hàng Vietcombank thật và đồng bộ trạng thái hợp đồng
-- ============================================================================

-- Cập nhật thông tin VietQR chính thức của chủ sân (DOAN NGUYEN THANH CONG - Vietcombank)
UPDATE site_settings SET setting_value = 'Vietcombank (Ngân hàng TMCP Ngoại thương Việt Nam)' WHERE setting_key = 'payment.bank_name';
UPDATE site_settings SET setting_value = 'VCB' WHERE setting_key = 'payment.bank_bin';
UPDATE site_settings SET setting_value = '9342804412' WHERE setting_key = 'payment.bank_account_no';
UPDATE site_settings SET setting_value = 'DOAN NGUYEN THANH CONG' WHERE setting_key = 'payment.bank_account_name';
UPDATE site_settings SET setting_value = '30' WHERE setting_key = 'payment.deposit_percent';

-- Chuẩn hóa dữ liệu hợp đồng cố định nếu có bản ghi cũ
UPDATE recurring_contracts SET payment_status = 'UNPAID' WHERE payment_status = 'PENDING';

