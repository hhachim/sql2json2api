SELECT 
    id,
    username,
    email,
    active
FROM users
WHERE active = true
LIMIT 3