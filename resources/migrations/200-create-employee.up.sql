CREATE TABLE employee (
  account text,
  bank_id text REFERENCES bank ON DELETE cascade ON UPDATE cascade,
  birthday date,
  company_id text NOT NULL UNIQUE,
  direct_kind direct_type,
  education text,
  education_period text,
  employee_kind employee_type,
  exception text,
  factory text,
  gender gender_type,
  id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  job_title text,
  job_title_2 text,
  mail_addr text,
  memo text,
  mobile text,
  name text NOT NULL,
  phone text,
  price_kind price_type,
  reg_addr text,
  salary_kind text GENERATED ALWAYS AS (
    CASE
      employee_kind
      WHEN '計時' THEN '計時'
      WHEN '計件' THEN '計件'
      ELSE '月薪'
    END
  ) STORED,
  taiwan_id text NOT NULL UNIQUE,
  unit_id text,
  work_place text
);
--;;
CREATE INDEX idx_employee_by_name ON employee (name);