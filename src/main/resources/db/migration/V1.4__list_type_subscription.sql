--
-- This script will populate the subscription list type table with all the
-- subscriptions which already exists in database
--

INSERT INTO subscription_list_type(id, user_id, list_type, list_language)
SELECT uuid(user_id) id, user_id, list_type, STRING_TO_ARRAY('ENGLISH,WELSH', ',') list_language
FROM
  (
    SELECT user_id, CASE WHEN list_type IS NULL THEN '{}' else list_type END
    FROM subscription
    WHERE search_type = 'LOCATION_ID'
    GROUP BY user_id, CASE WHEN list_type IS NULL THEN '{}' else list_type END
    ORDER BY user_id

  )tbl



