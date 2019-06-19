WITH existing_application(app_uuid, app_name, agent_config) AS (
    SELECT app_uuid, app_name, agent_config
    FROM source_application
    WHERE app_uuid = ?
)
UPDATE source_application
SET
  app_name = COALESCE (?, (SELECT app_name FROM existing_application)),
  agent_config = COALESCE (?, (SELECT agent_config FROM existing_application))
WHERE app_uuid = (SELECT app_uuid FROM existing_application);