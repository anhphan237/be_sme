CREATE TABLE IF NOT EXISTS candidate_fit_audits (
    candidate_fit_audit_id varchar(36) PRIMARY KEY,
    company_id             varchar(36) NOT NULL,
    employee_id            varchar(36) NOT NULL,
    jd_match_score         double precision NOT NULL,
    competency_score       double precision NOT NULL,
    interview_score        double precision NOT NULL,
    jd_weight              double precision NOT NULL,
    competency_weight      double precision NOT NULL,
    interview_weight       double precision NOT NULL,
    fit_score              double precision NOT NULL,
    fit_level              varchar(20) NOT NULL, -- HIGH|MEDIUM|LOW
    note                   varchar(1000),
    assessed_by            varchar(36),
    assessed_at            timestamptz NOT NULL DEFAULT now(),
    created_at             timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_candidate_fit_audits_company_employee
    ON candidate_fit_audits (company_id, employee_id, assessed_at DESC);

