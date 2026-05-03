-- Set initial admin user
-- This migration sets the first admin for the system

UPDATE users
SET role = 'ADMIN'
WHERE email = 'kurekadam314@gmail.com';
