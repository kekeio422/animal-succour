ALTER TABLE station
    ADD COLUMN latitude DECIMAL(10, 7) NULL COMMENT '纬度' AFTER address,
    ADD COLUMN longitude DECIMAL(10, 7) NULL COMMENT '经度' AFTER latitude;
