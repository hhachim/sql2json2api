-- Drop tables if they exist
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS users;

-- Create users table
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create customers table
CREATE TABLE customers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create products table
CREATE TABLE products (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create orders table
CREATE TABLE orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED') DEFAULT 'PENDING',
    shipping_address TEXT,
    total_amount DECIMAL(10, 2),
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create order_items table
CREATE TABLE order_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (product_id) REFERENCES products(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert sample users
INSERT INTO users (username, email, password, active) VALUES
('john_doe', 'john.doe@example.com', '$2a$10$Rw8CdVG9CmQl5b5o5e1.C.3HVhQZCeMVzrWrG5Lyw1jTYD9yMgO.e', true),
('jane_smith', 'jane.smith@example.com', '$2a$10$qJxNxhMvrMuXsKZE.IdjTuqP7NM7qh9A9QA.yXkKUFH3C8wd4gAjK', true),
('robert_johnson', 'robert.johnson@example.com', '$2a$10$8RFt.WmDLAY9icJ3YFp0VO5IHZ4bniTK02Fd6hdlsZR4C1NzXq0Om', true),
('sarah_wilson', 'sarah.wilson@example.com', '$2a$10$ICPnXsSBNo0JL8Jx0KWUUul0lXi87XTp9jkWUm5G0iw4i6c6s5Vtu', true),
('michael_brown', 'michael.brown@example.com', '$2a$10$YW7eQZnxfPkZQhzDJJXkQOw6a.7NICw01H6rvfUQ3HXpwrWnCO7V.', false);

-- Insert sample customers
INSERT INTO customers (name, email, phone, address) VALUES
('Acme Corporation', 'contact@acme.com', '123-456-7890', '123 Business Ave, New York, NY 10001'),
('Globex Industries', 'info@globex.com', '234-567-8901', '456 Enterprise Blvd, Chicago, IL 60607'),
('Stark Enterprises', 'orders@stark.com', '345-678-9012', '789 Innovation Drive, San Francisco, CA 94107'),
('Wayne Enterprises', 'info@wayne.com', '456-789-0123', '1007 Gotham Square, Metropolis, ND 58401'),
('Umbrella Corporation', 'support@umbrella.com', '567-890-1234', '5678 Research Pkwy, Boston, MA 02108');

-- Insert sample products
INSERT INTO products (name, description, price, stock) VALUES
('Enterprise Server', 'High-performance server for enterprise applications', 2499.99, 15),
('Professional Workstation', 'Powerful workstation for design and development', 1899.99, 25),
('Network Storage Array', '24TB Network Attached Storage solution', 1299.99, 10),
('Security Appliance', 'Next-generation firewall and security gateway', 749.99, 30),
('Cloud Backup Service (Annual)', 'Annual subscription for cloud backup service', 499.99, 100),
('Smart Switch', 'Managed 48-port gigabit switch', 299.99, 40),
('Wireless Access Point', 'High-speed WiFi 6 access point', 189.99, 50),
('SSD Storage Drive', '1TB NVMe SSD storage drive', 159.99, 75),
('UPS Battery Backup', '1500VA uninterruptible power supply', 199.99, 35),
('Network Cable Bundle', 'Bundle of Cat6 ethernet cables (25pcs)', 89.99, 60);

-- Insert sample orders
INSERT INTO orders (customer_id, order_date, status, shipping_address, total_amount) VALUES
(1, DATE_SUB(NOW(), INTERVAL 2 DAY), 'PENDING', '123 Business Ave, New York, NY 10001', 5099.97),
(2, DATE_SUB(NOW(), INTERVAL 5 DAY), 'PROCESSING', '456 Enterprise Blvd, Chicago, IL 60607', 2949.97),
(3, DATE_SUB(NOW(), INTERVAL 10 DAY), 'SHIPPED', '789 Innovation Drive, San Francisco, CA 94107', 3589.95),
(4, DATE_SUB(NOW(), INTERVAL 15 DAY), 'DELIVERED', '1007 Gotham Square, Metropolis, ND 58401', 2339.94),
(5, DATE_SUB(NOW(), INTERVAL 1 DAY), 'PENDING', '5678 Research Pkwy, Boston, MA 02108', 1249.95),
(1, DATE_SUB(NOW(), INTERVAL 30 DAY), 'DELIVERED', '123 Business Ave, New York, NY 10001', 899.97),
(3, DATE_SUB(NOW(), INTERVAL 3 DAY), 'PENDING', '789 Innovation Drive, San Francisco, CA 94107', 2699.97);

-- Insert sample order items
INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
-- Order 1
(1, 1, 2, 2499.99), -- 2 Enterprise Servers
(1, 5, 2, 499.99),  -- 2 Cloud Backup Services

-- Order 2
(2, 2, 1, 1899.99), -- 1 Professional Workstation
(2, 7, 3, 189.99),  -- 3 Wireless Access Points
(2, 10, 5, 89.99),  -- 5 Network Cable Bundles

-- Order 3
(3, 1, 1, 2499.99), -- 1 Enterprise Server
(3, 4, 1, 749.99),  -- 1 Security Appliance
(3, 9, 2, 199.99),  -- 2 UPS Battery Backups

-- Order 4
(4, 2, 1, 1899.99), -- 1 Professional Workstation
(4, 8, 3, 159.99),  -- 3 SSD Storage Drives

-- Order 5
(5, 3, 1, 1299.99),  -- 1 Network Storage Array
(5, 10, 5, 89.99),   -- 5 Network Cable Bundles

-- Order 6
(6, 6, 3, 299.99),  -- 3 Smart Switches

-- Order 7
(7, 1, 1, 2499.99), -- 1 Enterprise Server
(7, 5, 1, 499.99);  -- 1 Cloud Backup Service