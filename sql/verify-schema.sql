-- Run in BIT240038_MID, using pgAdmin or psql.
SELECT current_database(), current_setting('server_version') AS postgres_version;
SELECT table_name, ordinal_position, column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('BIT240038_DishCategories','BIT240038_Dishes','BIT240038_DishImages')
ORDER BY table_name, ordinal_position;
-- Must return zero rows.
WITH expected(table_name,column_name) AS (VALUES
 ('BIT240038_DishCategories','BIT240038_Id'),
 ('BIT240038_DishCategories','BIT240038_Name'),
 ('BIT240038_DishCategories','BIT240038_Description'),
 ('BIT240038_Dishes','BIT240038_Id'),
 ('BIT240038_Dishes','BIT240038_Name'),
 ('BIT240038_Dishes','BIT240038_Price'),
 ('BIT240038_Dishes','BIT240038_PreparationTime'),
 ('BIT240038_Dishes','BIT240038_IsAvailable'),
 ('BIT240038_Dishes','BIT240038_Description'),
 ('BIT240038_Dishes','BIT240038_DishCategoryId'),
 ('BIT240038_DishImages','BIT240038_Id'),
 ('BIT240038_DishImages','BIT240038_ImageUrl'),
 ('BIT240038_DishImages','BIT240038_IsThumbnail'),
 ('BIT240038_DishImages','BIT240038_DishId')
), actual AS (
 SELECT table_name,column_name FROM information_schema.columns
 WHERE table_schema='public'
 AND table_name IN ('BIT240038_DishCategories','BIT240038_Dishes','BIT240038_DishImages')
)
SELECT 'missing' AS issue,* FROM (SELECT * FROM expected EXCEPT SELECT * FROM actual) missing
UNION ALL
SELECT 'unexpected' AS issue,* FROM (SELECT * FROM actual EXCEPT SELECT * FROM expected) unexpected;
SELECT tc.constraint_name, kcu.table_name, kcu.column_name,
       ccu.table_name AS referenced_table, ccu.column_name AS referenced_column
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
  ON tc.constraint_name=kcu.constraint_name AND tc.constraint_schema=kcu.constraint_schema
JOIN information_schema.constraint_column_usage ccu
  ON tc.constraint_name=ccu.constraint_name AND tc.constraint_schema=ccu.constraint_schema
WHERE tc.constraint_type='FOREIGN KEY' AND tc.constraint_schema='public'
  AND kcu.table_name IN ('BIT240038_Dishes','BIT240038_DishImages');
SELECT indexname,indexdef FROM pg_indexes WHERE schemaname='public'
  AND tablename IN ('BIT240038_DishCategories','BIT240038_Dishes','BIT240038_DishImages');
-- Must return zero rows.
SELECT "BIT240038_DishId", COUNT(*)
FROM "BIT240038_DishImages" WHERE "BIT240038_IsThumbnail"
GROUP BY "BIT240038_DishId" HAVING COUNT(*) > 1;
