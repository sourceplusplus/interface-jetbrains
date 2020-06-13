UPDATE SOURCE_ARTIFACT_SUBSCRIPTION
SET
  subscription_data = ?
WHERE 1=1
AND subscription_data = ?;