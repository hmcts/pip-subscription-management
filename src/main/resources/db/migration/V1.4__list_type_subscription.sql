--
-- This script will populate the subscription list type table with all the
-- subscriptions which already exists in database
--

INSERT INTO subscription_list_type(id, user_id, location_id, list_type, list_language)
SELECT gen_random_uuid () id, user_id, CAST(search_value AS Integer) locationId, list_type, STRING_TO_ARRAY('ENGLISH,WELSH', ',') list_language
FROM
  (
    SELECT user_id, search_value, list_type
    FROM subscription
    WHERE search_type = 'LOCATION_ID'
      AND list_type != '{}'
    GROUP BY user_id, search_value, list_type
    ORDER BY user_id, search_value

  )tbl

--
-- This script will delete list_type column from subscription table
-- because we have moved the data into new table subscription_list_type
--
ALTER TABLE subscription
DROP COLUMN list_type;



