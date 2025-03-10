SELECT distinct CONCAT(s.col1," - ",s.col3) as product_group_label,
	DATE_FORMAT(CONVERT_TZ(NOW(), @@session.time_zone, '+01:00'), '%Y-%m-%dT%T+01:00') AS instructions_label
FROM adp_imports_tmp_sapflatfiles s
LEFT JOIN llx_wynd_product_group g on concat(s.col1, ' - ' , s.col3) = g.label
WHERE col1 like '54M%'
-- and g.label is null