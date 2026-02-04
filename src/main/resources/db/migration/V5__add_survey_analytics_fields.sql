-- V5__add_survey_analytics_fields.sql
-- Add analytics metadata for survey questions + snapshot fields for survey answers

-- 1) survey_questions: metadata for analytics
ALTER TABLE survey_questions
    ADD COLUMN IF NOT EXISTS dimension_code varchar(50),
    ADD COLUMN IF NOT EXISTS measurable boolean DEFAULT true,
    ADD COLUMN IF NOT EXISTS scale_min int DEFAULT 1,
    ADD COLUMN IF NOT EXISTS scale_max int DEFAULT 5;

-- 2) survey_answers: snapshot metadata + multi-choice support
ALTER TABLE survey_answers
    ADD COLUMN IF NOT EXISTS question_type varchar(30),
    ADD COLUMN IF NOT EXISTS dimension_code varchar(50),
    ADD COLUMN IF NOT EXISTS value_choices jsonb;

-- 3) Optional indexes for faster analytics (safe to create)
CREATE INDEX IF NOT EXISTS idx_survey_questions_company_dimension
    ON survey_questions (company_id, dimension_code);

CREATE INDEX IF NOT EXISTS idx_survey_answers_company_dimension
    ON survey_answers (company_id, dimension_code);

CREATE INDEX IF NOT EXISTS idx_survey_answers_response
    ON survey_answers (survey_response_id);


ALTER TABLE survey_templates
    ADD COLUMN IF NOT EXISTS version int DEFAULT 1,
    ADD COLUMN IF NOT EXISTS is_default boolean DEFAULT false;


ALTER TABLE survey_templates
    ADD COLUMN IF NOT EXISTS description text;

-- responses survey
ALTER TABLE survey_responses
    ADD COLUMN IF NOT EXISTS updated_at timestamptz DEFAULT now();
ALTER TABLE survey_responses
    ADD COLUMN IF NOT EXISTS updated_by varchar(36);

