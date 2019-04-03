MERGE INTO SOURCE_ARTIFACT_SUBSCRIPTION(
  subscriber_uuid, app_uuid, artifact_qualified_name, subscription_type, last_accessed
)
KEY(subscriber_uuid, app_uuid, artifact_qualified_name, subscription_type)
SELECT ?, ?, ?, ?, ?;