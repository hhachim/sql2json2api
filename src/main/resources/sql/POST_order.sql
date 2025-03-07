-- SQL query to retrieve order details for transformation
SELECT 
    o.id as order_id,
    o.order_date,
    o.status,
    c.id as customer_id,
    c.name as customer_name,
    c.email as customer_email,
    p.id as product_id,
    p.name as product_name,
    p.price,
    oi.quantity,
    (p.price * oi.quantity) as total_price
FROM 
    orders o
JOIN 
    customers c ON o.customer_id = c.id
JOIN 
    order_items oi ON o.id = oi.order_id
JOIN 
    products p ON oi.product_id = p.id
WHERE 
    o.status = 'PENDING'
ORDER BY 
    o.order_date DESC