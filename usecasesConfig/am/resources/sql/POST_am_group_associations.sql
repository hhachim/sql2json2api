SELECT dg.uuid as discount_group_uid, d.uuid as discount_uuid
FROM llx_wynd_discount_group dg
INNER JOIN llx_wynd_discount d on dg.reference = d.reference