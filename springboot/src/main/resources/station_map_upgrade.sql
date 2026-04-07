SET @has_latitude = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'station'
      AND COLUMN_NAME = 'latitude'
);
SET @sql_latitude = IF(
    @has_latitude = 0,
    'ALTER TABLE station ADD COLUMN latitude DECIMAL(10,7) NULL COMMENT ''纬度''',
    'SELECT 1'
);
PREPARE stmt_latitude FROM @sql_latitude;
EXECUTE stmt_latitude;
DEALLOCATE PREPARE stmt_latitude;

SET @has_longitude = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'station'
      AND COLUMN_NAME = 'longitude'
);
SET @sql_longitude = IF(
    @has_longitude = 0,
    'ALTER TABLE station ADD COLUMN longitude DECIMAL(10,7) NULL COMMENT ''经度''',
    'SELECT 1'
);
PREPARE stmt_longitude FROM @sql_longitude;
EXECUTE stmt_longitude;
DEALLOCATE PREPARE stmt_longitude;
