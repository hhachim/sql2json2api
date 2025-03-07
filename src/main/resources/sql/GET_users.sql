SELECT 
    id,
    username,
    email,
    created_at
FROM users
WHERE active = true
ORDER BY created_at DESC