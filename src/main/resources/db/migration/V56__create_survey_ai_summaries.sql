CREATE TABLE IF NOT EXISTS survey_ai_summaries (
                                                   summary_id VARCHAR(50) PRIMARY KEY,
    company_id VARCHAR(50) NOT NULL,
    template_id VARCHAR(50),
    start_date DATE,
    end_date DATE,
    language VARCHAR(10) NOT NULL,
    input_hash VARCHAR(128) NOT NULL,
    health_level VARCHAR(30),
    summary_json TEXT NOT NULL,
    generated_at TIMESTAMP NOT NULL,
    generated_by VARCHAR(50)
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_survey_ai_summary_cache
    ON survey_ai_summaries (
    company_id,
    COALESCE(template_id, ''),
    COALESCE(start_date, DATE '1970-01-01'),
    COALESCE(end_date, DATE '1970-01-01'),
    language,
    input_hash
    );

CREATE INDEX IF NOT EXISTS idx_survey_ai_summary_company_generated
    ON survey_ai_summaries (company_id, generated_at DESC);

CREATE INDEX IF NOT EXISTS idx_survey_ai_summary_user_generated
    ON survey_ai_summaries (company_id, generated_by, generated_at DESC);