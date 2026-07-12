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

-- Packaging: support standalone packaging plans (not tied to orders)
ALTER TABLE packaging ALTER COLUMN order_id SET NULL;
ALTER TABLE packaging ADD COLUMN IF NOT EXISTS user_id BIGINT;
ALTER TABLE packaging ADD COLUMN IF NOT EXISTS gift_record_id BIGINT;
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
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Feedback role (Task T4)
ALTER TABLE feedback ADD COLUMN IF NOT EXISTS role VARCHAR(10) DEFAULT 'sender';
