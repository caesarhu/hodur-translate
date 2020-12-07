CREATE TABLE bank (
  addr text,
  bank_id text NOT NULL PRIMARY KEY,
  change_date date,
  head_id text NOT NULL GENERATED ALWAYS AS (LEFT(bank_id, 3)) STORED,
  name text NOT NULL,
  phone text,
  principal text
);
--;;
CREATE INDEX idx_bank_by_head ON bank (head_id);