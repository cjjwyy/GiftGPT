-- User
CREATE TABLE IF NOT EXISTS "user" (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone VARCHAR(20) UNIQUE,
    email VARCHAR(100),
    password_hash VARCHAR(255),
    nickname VARCHAR(50),
    avatar_url VARCHAR(500),
    gender TINYINT DEFAULT 0,
    auth_provider VARCHAR(20) DEFAULT 'local',
    open_id VARCHAR(100),
    status TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User Consume Profile
CREATE TABLE IF NOT EXISTS user_consume_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    price_min DECIMAL(10,2),
    price_max DECIMAL(10,2),
    category_prefs CLOB,
    brand_prefs CLOB,
    consume_level VARCHAR(20),
    taste_circle VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Recipient
CREATE TABLE IF NOT EXISTS recipient (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    relation VARCHAR(50),
    gender TINYINT DEFAULT 0,
    age_range VARCHAR(20),
    mbti VARCHAR(10),
    personality VARCHAR(500),
    recent_purchases CLOB,
    note CLOB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Recipient Tag
CREATE TABLE IF NOT EXISTS recipient_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    tag_code VARCHAR(50) NOT NULL,
    tag_name VARCHAR(50) NOT NULL,
    supplement VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Recipient Profile
CREATE TABLE IF NOT EXISTS recipient_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL,
    personality_desc CLOB,
    hobby_list CLOB,
    social_analysis CLOB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Product
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(100),
    platform VARCHAR(50),
    platform_url VARCHAR(500),
    image_url VARCHAR(500),
    description CLOB,
    sales_count INT DEFAULT 0,
    rating DOUBLE DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Gift Record
CREATE TABLE IF NOT EXISTS gift_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    occasion VARCHAR(50),
    budget DECIMAL(10,2),
    product_id BIGINT,
    greeting_card_id BIGINT,
    status VARCHAR(30) DEFAULT 'draft',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Order
CREATE TABLE IF NOT EXISTS "order" (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gift_record_id BIGINT NOT NULL,
    order_no VARCHAR(50),
    total_amount DECIMAL(10,2),
    status VARCHAR(30) DEFAULT 'pending',
    logistics_no VARCHAR(100),
    logistics_company VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Packaging
CREATE TABLE IF NOT EXISTS packaging (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    theme VARCHAR(50),
    custom_text VARCHAR(200),
    preview_image VARCHAR(500),
    price DECIMAL(10,2) DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Greeting Card
CREATE TABLE IF NOT EXISTS greeting_card (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content CLOB,
    voice_url VARCHAR(500),
    qr_code_url VARCHAR(500),
    style_template VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Feedback
CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gift_record_id BIGINT NOT NULL,
    type VARCHAR(20),
    content CLOB,
    is_public TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Story
CREATE TABLE IF NOT EXISTS story (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    gift_record_id BIGINT,
    title VARCHAR(200),
    content CLOB NOT NULL,
    images CLOB,
    likes INT DEFAULT 0,
    is_anonymous TINYINT DEFAULT 0,
    status TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Calendar Event
CREATE TABLE IF NOT EXISTS calendar_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipient_id BIGINT,
    title VARCHAR(100),
    occasion VARCHAR(50),
    event_date DATE NOT NULL,
    remind_before_days INT DEFAULT 3,
    is_repeat TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Data Authorization
CREATE TABLE IF NOT EXISTS data_authorization (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    data_type VARCHAR(50),
    authorized_scope VARCHAR(200),
    status VARCHAR(20) DEFAULT 'active',
    expire_at TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Recommendation History
CREATE TABLE IF NOT EXISTS recommendation_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipient_id BIGINT,
    scene VARCHAR(50),
    budget DECIMAL(10,2),
    result CLOB,
    feedback VARCHAR(20),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Enterprise
CREATE TABLE IF NOT EXISTS enterprise (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    license_no VARCHAR(100),
    contact_name VARCHAR(50),
    contact_phone VARCHAR(20),
    status VARCHAR(20) DEFAULT 'pending',
    subscription VARCHAR(20) DEFAULT 'free',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Story Reply
CREATE TABLE IF NOT EXISTS story_reply (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    story_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content CLOB NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Story Like (track which users liked which stories)
CREATE TABLE IF NOT EXISTS story_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    story_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(story_id, user_id)
);

-- Schema drift repairs: ensure columns exist for tables created by older schema versions
ALTER TABLE story_reply ADD COLUMN IF NOT EXISTS update_time TIMESTAMP;
ALTER TABLE story_like ADD COLUMN IF NOT EXISTS update_time TIMESTAMP;
ALTER TABLE recommendation_history ADD COLUMN IF NOT EXISTS update_time TIMESTAMP;
ALTER TABLE recipient ADD COLUMN IF NOT EXISTS mbti VARCHAR(20);
ALTER TABLE recipient ADD COLUMN IF NOT EXISTS personality VARCHAR(1000);
ALTER TABLE recipient ADD COLUMN IF NOT EXISTS recent_purchases VARCHAR(1000);
ALTER TABLE recipient_tag ADD COLUMN IF NOT EXISTS supplement VARCHAR(500);

-- Packaging: support standalone packaging plans (not tied to orders)
ALTER TABLE packaging ALTER COLUMN order_id SET NULL;
ALTER TABLE packaging ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE packaging ADD COLUMN IF NOT EXISTS gift_record_id BIGINT;
ALTER TABLE packaging ADD COLUMN IF NOT EXISTS product_id BIGINT;
ALTER TABLE packaging ADD COLUMN IF NOT EXISTS product_name VARCHAR(200);
ALTER TABLE packaging ADD COLUMN IF NOT EXISTS product_price DECIMAL(10,2);
ALTER TABLE packaging ADD COLUMN IF NOT EXISTS product_image_url VARCHAR(500);
ALTER TABLE packaging ADD COLUMN IF NOT EXISTS ribbon_text VARCHAR(50);
ALTER TABLE packaging ADD COLUMN IF NOT EXISTS ribbon_color VARCHAR(10);
ALTER TABLE packaging ADD COLUMN IF NOT EXISTS scent VARCHAR(20);
ALTER TABLE packaging ADD COLUMN IF NOT EXISTS photo_url VARCHAR(500);
ALTER TABLE packaging ADD COLUMN IF NOT EXISTS wrapping_style VARCHAR(30);

-- Logistics tracking events (Task T2)
CREATE TABLE IF NOT EXISTS logistics_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    event_time TIMESTAMP NOT NULL,
    location VARCHAR(100),
    status VARCHAR(30),
    description VARCHAR(255),
    source VARCHAR(20) DEFAULT 'simulation',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- In-app reminders generated from calendar events.
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    calendar_event_id BIGINT NOT NULL,
    occurrence_date DATE NOT NULL,
    title VARCHAR(150) NOT NULL,
    content VARCHAR(500),
    is_read TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_notification_event_occurrence
    ON notification(calendar_event_id, occurrence_date);
CREATE INDEX IF NOT EXISTS idx_notification_user_read
    ON notification(user_id, is_read, create_time);
ALTER TABLE logistics_event ADD COLUMN IF NOT EXISTS source VARCHAR(20) DEFAULT 'simulation';

-- Feedback role (Task T4)
ALTER TABLE feedback ADD COLUMN IF NOT EXISTS role VARCHAR(10) DEFAULT 'sender';

-- One-time, hashed access links let recipients respond without exposing sender APIs.
CREATE TABLE IF NOT EXISTS feedback_access_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    gift_record_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expire_at TIMESTAMP NOT NULL,
    used TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_feedback_access_token_hash
    ON feedback_access_token(token_hash);
CREATE INDEX IF NOT EXISTS idx_feedback_access_token_gift
    ON feedback_access_token(gift_record_id);

-- Recommendation interaction events for CTR / feedback-loop analysis
CREATE TABLE IF NOT EXISTS recommend_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipient_id BIGINT,
    occasion VARCHAR(50),
    product_id BIGINT,
    product_name VARCHAR(200),
    event_type VARCHAR(20) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_recommend_event_user_time
    ON recommend_event(user_id, create_time);

-- AI reliability and token-usage metrics. Prompts and responses are intentionally not stored.
CREATE TABLE IF NOT EXISTS ai_invocation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    scene VARCHAR(50),
    model VARCHAR(50),
    prompt_tokens INT DEFAULT 0,
    completion_tokens INT DEFAULT 0,
    latency_ms BIGINT DEFAULT 0,
    success TINYINT DEFAULT 0,
    fallback TINYINT DEFAULT 0,
    error_type VARCHAR(100),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_invocation_log_time
    ON ai_invocation_log(create_time);

-- Prevent duplicate platform products under concurrent searches.
CREATE UNIQUE INDEX IF NOT EXISTS ux_product_platform_name
    ON product(platform, name);

-- A gift can be checked out only once; the service also handles legacy duplicates defensively.
CREATE UNIQUE INDEX IF NOT EXISTS ux_order_gift_record
    ON "order"(gift_record_id);

ALTER TABLE packaging ADD COLUMN IF NOT EXISTS request_key VARCHAR(64);
ALTER TABLE packaging ADD COLUMN IF NOT EXISTS customizations_json VARCHAR(2000);
ALTER TABLE packaging ADD COLUMN IF NOT EXISTS price_details_json VARCHAR(2000);
ALTER TABLE packaging ADD COLUMN IF NOT EXISTS version INT DEFAULT 0;
CREATE UNIQUE INDEX IF NOT EXISTS ux_packaging_request ON packaging(user_id, request_key);

CREATE TABLE IF NOT EXISTS story_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    story_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(30) NOT NULL,
    detail VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    moderator_id BIGINT,
    decision_note VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(story_id, reporter_id)
);
CREATE INDEX IF NOT EXISTS idx_story_report_queue ON story_report(status, id);
