--
-- This script will delete list_type column from subscription table
-- because we have moved the data into new table subscription_list_type
--
ALTER TABLE subscription
DROP COLUMN list_type;
