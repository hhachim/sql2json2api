SELECT 
    MAX(m2.col3) AS default_label,
    MAX(m1.col1) AS reference,
    MAX(m2.col12) AS external_ref,
    CASE 
        WHEN MAX(m2.col12) IN ('996', '998', '997', '999', '990') THEN 'RELATIVE'
        WHEN MAX(m2.col12) = '995' THEN 'ABSOLUTE'
        ELSE NULL
    END AS condition_type,
    'true' AS auto,
    CASE 
        WHEN MAX(m2.col12) IN ('996', '997', '995', '990') THEN 'AMOUNT'
        WHEN MAX(m2.col12) IN ('998', '999') THEN 'PERCENT'
        ELSE NULL
    END AS discount_type,
    MAX(m2.col11) / 100 AS value,
    CASE 
        WHEN MAX(m3.col4) = '1' THEN 'true'
        WHEN MAX(m3.col4) = '2' THEN 'false'
        ELSE NULL
    END AS basket_impacted,
    CASE 
        WHEN MAX(m1.col14) = '0' THEN 'true'
        WHEN MAX(m1.col14) = '1' THEN 'false'
        ELSE NULL
    END AS repeatable,
    CASE 
        WHEN MAX(m1.col26) = '1' THEN 'true'
        WHEN MAX(m1.col26) = '0' THEN 'false'
        ELSE NULL
    END AS cumulable,
    e.rowid AS entity,
    'true' AS enabled,
    STR_TO_DATE(MAX(m1.col7), '%d%m%Y') AS condition_time_date_start,
    STR_TO_DATE(MAX(m1.col8), '%d%m%Y') AS condition_time_date_end,
    CASE 
        WHEN MAX(m1.col13) IN ('0', '1') THEN 'LESS_EXPENSIVE'
        WHEN MAX(m1.col13) = '2' THEN 'MOST_EXPENSIVE'
        ELSE NULL
    END AS impacted_product_price,
    /*'7a522420-c40f-4677-a9de-2f7a94a3f689' as product_group_uuid,*/
        pg.uuid AS product_group_uuid,
    CASE 
        WHEN MAX(m3.col9) = '3' THEN MAX(m3.col11)
        WHEN MAX(m2.col4) = '1' THEN NULL
        ELSE NULL
    END AS product_groups_min_item_number,
    CASE 
        WHEN MAX(m3.col9) = '3' THEN 'true'
        WHEN MAX(m2.col4) = '1' THEN NULL
        ELSE NULL
    END AS product_groups_product_group_impacted,
    CASE 
        WHEN MAX(m2.col4) = '2' THEN MAX(m3.col11)
        WHEN MAX(m2.col4) = '3' THEN MAX(m3.col12)
        WHEN MAX(m2.col4) = '1' THEN NULL
        ELSE NULL
    END AS product_groups_number_impacted
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
