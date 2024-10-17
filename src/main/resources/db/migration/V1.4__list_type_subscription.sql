--
-- This script will populate the subscription list type table with all the
-- subscriptions which already exists in database
--
CREATE EXTENSION pgcrypto;

INSERT INTO subscription_list_type(id, user_id, list_type, list_language)
SELECT gen_random_uuid () id, user_id, list_type, STRING_TO_ARRAY('ENGLISH,WELSH', ',') list_language
FROM
  (
    SELECT user_id, list_type
    FROM subscription
    WHERE search_type = 'LOCATION_ID'
      AND list_type != '{}'
    GROUP BY user_id, list_type
    ORDER BY user_id

  )tbl



