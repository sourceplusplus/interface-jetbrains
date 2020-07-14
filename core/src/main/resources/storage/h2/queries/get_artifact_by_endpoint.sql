SELECT
  app_uuid, artifact_qualified_name, create_date, last_updated, endpoint, auto_endpoint, auto_subscribe,
  module_name, component, endpoint_name, endpoint_ids, active_failing, latest_failed_service_instance
FROM source_artifact
WHERE 1=1
AND app_uuid = ?
AND endpoint = TRUE
AND (
    (? = TRUE AND auto_endpoint = TRUE)
    OR COALESCE(auto_endpoint, FALSE) = FALSE
);