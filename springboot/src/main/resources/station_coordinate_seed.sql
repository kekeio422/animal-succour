-- 为地图找宠功能补充救助站经纬度（仅填充空值，避免覆盖已有坐标）
-- 执行前请先备份数据库，并在测试环境验证。

UPDATE station
SET latitude = 39.9042000,
    longitude = 116.4074000
WHERE (latitude IS NULL OR longitude IS NULL)
  AND address LIKE '%北京%';

UPDATE station
SET latitude = 31.2304000,
    longitude = 121.4737000
WHERE (latitude IS NULL OR longitude IS NULL)
  AND address LIKE '%上海%';

UPDATE station
SET latitude = 23.1291000,
    longitude = 113.2644000
WHERE (latitude IS NULL OR longitude IS NULL)
  AND address LIKE '%广州%';

UPDATE station
SET latitude = 22.5431000,
    longitude = 114.0579000
WHERE (latitude IS NULL OR longitude IS NULL)
  AND address LIKE '%深圳%';

UPDATE station
SET latitude = 30.2741000,
    longitude = 120.1551000
WHERE (latitude IS NULL OR longitude IS NULL)
  AND address LIKE '%杭州%';

UPDATE station
SET latitude = 30.5728000,
    longitude = 104.0668000
WHERE (latitude IS NULL OR longitude IS NULL)
  AND address LIKE '%成都%';

UPDATE station
SET latitude = 32.0603000,
    longitude = 118.7969000
WHERE (latitude IS NULL OR longitude IS NULL)
  AND address LIKE '%南京%';

UPDATE station
SET latitude = 34.3416000,
    longitude = 108.9398000
WHERE (latitude IS NULL OR longitude IS NULL)
  AND address LIKE '%西安%';

UPDATE station
SET latitude = 29.5630000,
    longitude = 106.5516000
WHERE (latitude IS NULL OR longitude IS NULL)
  AND address LIKE '%重庆%';

UPDATE station
SET latitude = 30.5928000,
    longitude = 114.3055000
WHERE (latitude IS NULL OR longitude IS NULL)
  AND address LIKE '%武汉%';
