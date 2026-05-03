-- Add role column to users table with default value 'USER'
-- Uses conditional logic to handle cases where column already exists

DO $$
BEGIN
    -- Add role column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'role'
    ) THEN
        ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
    END IF;

    -- Add constraint if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'users' AND constraint_name = 'chk_users_role'
    ) THEN
        ALTER TABLE users ADD CONSTRAINT chk_users_role
            CHECK (role IN ('USER', 'ADMIN', 'MODERATOR'));
    END IF;
END $$;

-- Create index for role queries (IF NOT EXISTS)
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
