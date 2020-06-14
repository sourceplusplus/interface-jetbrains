UPDATE SOURCE_ARTIFACT_SUBSCRIPTION
SET
  subscription_data = ?,
  last_accessed = CURRENT_TIMESTAMP
WHERE 1=1
AND subscription_data = ?;