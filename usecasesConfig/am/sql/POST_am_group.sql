SELECT 
    MAX(m2.col3) AS default_label,
    MAX(m1.col1) AS reference
FROM adp_imports_tmp_sapflatfiles m1
JOIN adp_imports_tmp_sapflatfiles m2 
    ON RIGHT(m1.col1, 6) = RIGHT(m2.col1, 6) 
    AND m1.col1 LIKE '53M%' 
    AND m2.col1 LIKE '54M%'
LEFT JOIN adp_imports_tmp_sapflatfiles m3 
    ON RIGHT(m1.col1, 6) = RIGHT(m3.col1, 6) 
    AND m3.col1 LIKE '55M%'
LEFT JOIN llx_wynd_product_group pg 
    ON m2.col1= LEFT(pg.label, 9) 
INNER JOIN llx_entity e on SUBSTRING_INDEX(m3.file_name, '.', -1) = e.external_id
WHERE m1.col12 = '2'
  AND (m3.col4 = '1' OR m3.col4 = '2')
   AND NOT EXISTS (
      SELECT 1 
      FROM llx_wynd_discount d 
      WHERE d.reference = m1.col1
  )
GROUP BY pg.uuid;
