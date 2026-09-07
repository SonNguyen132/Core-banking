-- FinAegis Core Banking (Pure Event Sourcing) - Initial Schema
-- Flyway Migration V1

-- ============ Event Store (hand-written event sourcing) ============
CREATE TABLE stored_events (
    event_id VARCHAR(36) PRIMARY KEY,
    stream_id VARCHAR(36) NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    event_type VARCHAR(200) NOT NULL,
    event_version BIGINT NOT NULL,
    event_data JSON NOT NULL,
    created_at TIMESTAMP NOT NULL,
    INDEX idx_stored_stream (stream_id),
    INDEX idx_stored_aggregate (aggregate_id, aggregate_type)
);

-- ============ Security & Users ============
CREATE TABLE permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_perm FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id VARCHAR(36) NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- ============ Assets ============
CREATE TABLE assets (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    precision TINYINT UNSIGNED NOT NULL DEFAULT 2,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_asset_type (type)
);

CREATE TABLE exchange_rates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_asset_code VARCHAR(10) NOT NULL,
    to_asset_code VARCHAR(10) NOT NULL,
    rate DECIMAL(20,10) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    valid_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NULL,
    CONSTRAINT fk_rate_from FOREIGN KEY (from_asset_code) REFERENCES assets(code),
    CONSTRAINT fk_rate_to FOREIGN KEY (to_asset_code) REFERENCES assets(code),
    INDEX idx_rate_pair (from_asset_code, to_asset_code)
);

-- ============ Account read model (projection) ============
CREATE TABLE account_view (
    account_id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    asset_code VARCHAR(10) NOT NULL,
    balance DECIMAL(20,8) NOT NULL DEFAULT 0,
    frozen BOOLEAN NOT NULL DEFAULT FALSE,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_account_user (user_id)
);

-- ============ Transfer read model ============
CREATE TABLE transfer_view (
    transfer_id VARCHAR(36) PRIMARY KEY,
    from_account_id VARCHAR(36) NOT NULL,
    to_account_id VARCHAR(36) NOT NULL,
    amount DECIMAL(20,8) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    initiated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_transfer_from (from_account_id),
    INDEX idx_transfer_to (to_account_id),
    INDEX idx_transfer_status (status)
);

-- ============ Governance ============
CREATE TABLE polls (
    id VARCHAR(36) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    end_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_poll_status (status)
);

CREATE TABLE votes (
    id VARCHAR(36) PRIMARY KEY,
    poll_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    option_key VARCHAR(255) NOT NULL,
    voting_power DECIMAL(20,8) NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_vote (poll_id, user_id),
    CONSTRAINT fk_vote_poll FOREIGN KEY (poll_id) REFERENCES polls(id) ON DELETE CASCADE
);

-- ============ Lending ============
CREATE TABLE loans (
    id VARCHAR(36) PRIMARY KEY,
    borrower_id VARCHAR(36) NOT NULL,
    lender_id VARCHAR(36),
    principal DECIMAL(20,8) NOT NULL,
    interest_rate DECIMAL(8,4) NOT NULL,
    term_months INT NOT NULL,
    asset_code VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    monthly_payment DECIMAL(20,8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_loan_borrower (borrower_id),
    INDEX idx_loan_lender (lender_id),
    INDEX idx_loan_status (status)
);

-- ============ Stablecoin ============
CREATE TABLE stablecoin_supply (
    symbol VARCHAR(20) PRIMARY KEY,
    total_supply DECIMAL(20,8) NOT NULL DEFAULT 0,
    reserve_amount DECIMAL(20,8) NOT NULL DEFAULT 0,
    reserve_asset VARCHAR(10) NOT NULL DEFAULT 'USD',
    min_collateral_ratio DECIMAL(8,2) NOT NULL DEFAULT 100.00,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============ Seed base assets ============
INSERT INTO assets (code, name, type, precision, is_active) VALUES
    ('USD', 'US Dollar', 'fiat', 2, TRUE),
    ('EUR', 'Euro', 'fiat', 2, TRUE),
    ('GBP', 'British Pound', 'fiat', 2, TRUE),
    ('BTC', 'Bitcoin', 'crypto', 8, TRUE),
    ('ETH', 'Ethereum', 'crypto', 8, TRUE),
    ('XAU', 'Gold', 'commodity', 4, TRUE);

-- ============ Seed stablecoin supplies ============
INSERT INTO stablecoin_supply (symbol, total_supply, reserve_amount, reserve_asset, min_collateral_ratio) VALUES
    ('USDC', 1000000, 1000000, 'USD', 100.00),
    ('GCU', 500000, 500000, 'USD', 100.00);
