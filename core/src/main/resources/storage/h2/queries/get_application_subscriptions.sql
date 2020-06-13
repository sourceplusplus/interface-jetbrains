SELECT subscription_data
FROM source_artifact_subscription
WHERE 1=1
AND app_uuid = ?;