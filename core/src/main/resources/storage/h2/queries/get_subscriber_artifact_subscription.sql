SELECT subscriber_uuid, app_uuid, artifact_qualified_name, subscription_type, last_accessed
FROM source_artifact_subscription
WHERE 1=1
AND subscriber_uuid = ?
AND app_uuid = ?
AND artifact_qualified_name = ?;