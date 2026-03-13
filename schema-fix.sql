-- SQL Script to add missing HR-related columns to agency_candidate_pre_onboarding
-- Run this script in your database console (e.g., pgAdmin or psql)

ALTER TABLE agency_candidate_pre_onboarding 
ADD COLUMN IF NOT EXISTS hr_onboarding_date date,
ADD COLUMN IF NOT EXISTS hr_onboarding_location varchar(255),
ADD COLUMN IF NOT EXISTS hr_verified boolean NOT NULL DEFAULT false,
ADD COLUMN IF NOT EXISTS hr_user_id bigint,
ADD COLUMN IF NOT EXISTS onboarded_at timestamp;

-- Update submitted_at to be not null if it was nullable before (hibernate requirement in entity)
ALTER TABLE agency_candidate_pre_onboarding 
ALTER COLUMN submitted_at SET NOT NULL;
