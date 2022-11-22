--
-- Create the table if doesn't exist. Only used in test DBs
--
CREATE TABLE IF NOT EXISTS subscription (
     id uuid NOT NULL PRIMARY KEY,
     case_name varchar(255),
     case_number varchar(255),
     channel varchar(255),
     created_date timestamp,
     location_name varchar(255),
     search_type varchar(255),
     search_value varchar(255),
     urn varchar(255),
     user_id varchar(255),
     list_type text[],
     last_updated_date timestamp
);

--
-- If the table already exists without the new columns, add them in
--
ALTER TABLE subscription
  ADD COLUMN IF NOT EXISTS last_updated_date timestamp;

--
-- Set last_updated_date to created date if it's not already been set
--
UPDATE subscription
SET last_updated_date = created_date
WHERE subscription.last_updated_date IS NULL;
