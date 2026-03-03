-- Create skills table if not exists
CREATE TABLE IF NOT EXISTS skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    full_name VARCHAR(255) UNIQUE NOT NULL,
    owner VARCHAR(255),
    repository VARCHAR(255),
    language VARCHAR(100),
    star_count INTEGER DEFAULT 0,
    fork_count INTEGER DEFAULT 0,
    html_url VARCHAR(500),
    readme_content TEXT,
    topics TEXT,
    avatar_url VARCHAR(500),
    last_updated TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_skills_star_count ON skills(star_count DESC);
CREATE INDEX IF NOT EXISTS idx_skills_language ON skills(language);
CREATE INDEX IF NOT EXISTS idx_skills_name ON skills(name);
