WITH grp_discount_entities AS (
SELECT distinct
map1.col1 
, e.uuid as entity_uuid
FROM adp_imports_tmp_sapflatfiles map1
INNER JOIN llx_entity e on SUBSTRING_INDEX(map1.file_name, '.', -1) = e.external_id
WHERE map1.col1 LIKE '53M%'
)
SELECT dg.uuid as discount_group_uid
, ge.entity_uuid
FROM llx_wynd_discount_group dg
INNER JOIN grp_discount_entities ge on dg.reference = ge.col1