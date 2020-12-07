CREATE TABLE employee_change (
  change_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  change_day date NOT NULL,
  change_kind employee_change_type NOT NULL,
  employee_id bigint NOT NULL REFERENCES employee ON DELETE cascade ON UPDATE cascade,
  id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  memo text
);