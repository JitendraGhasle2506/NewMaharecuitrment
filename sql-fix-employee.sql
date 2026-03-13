-- SQL to fix the employee_master table constraints
-- Run this if you are getting "null value in column employee_code violates not-null constraint"

ALTER TABLE employee_master ALTER COLUMN employee_code DROP NOT NULL;
