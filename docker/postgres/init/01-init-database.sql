-- Initialize enterprise database with proper permissions
-- This script runs when the PostgreSQL container starts for the first time

-- Create additional schemas if needed
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS batch;

-- Grant permissions to the application user
GRANT ALL PRIVILEGES ON SCHEMA public TO enterprise_user;
GRANT ALL PRIVILEGES ON SCHEMA audit TO enterprise_user;
GRANT ALL PRIVILEGES ON SCHEMA batch TO enterprise_user;

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Set default privileges for future tables
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO enterprise_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT ALL ON TABLES TO enterprise_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA batch GRANT ALL ON TABLES TO enterprise_user;

-- Set default privileges for sequences
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO enterprise_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT ALL ON SEQUENCES TO enterprise_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA batch GRANT ALL ON SEQUENCES TO enterprise_user;