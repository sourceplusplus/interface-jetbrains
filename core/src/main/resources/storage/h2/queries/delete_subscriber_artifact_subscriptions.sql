DELETE FROM source_artifact_subscription
WHERE 1=1
AND subscriber_uuid = ?
AND app_uuid = ?
AND artifact_qualified_name = ?
AND subscription_type = ?;