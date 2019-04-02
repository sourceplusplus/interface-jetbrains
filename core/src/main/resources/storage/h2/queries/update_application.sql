UPDATE source_application
SET
  app_name = COALESCE (?, (SELECT app_name FROM source_application where app_uuid = ?)),
  agent_config = COALESCE (?, (SELECT agent_config FROM source_application where app_uuid = ?))
WHERE app_uuid = ?;