-- Index des clés étrangères utilisées par les filtres et les suppressions en cascade
CREATE INDEX IF NOT EXISTS tb_movement_index_activity_id ON tb_movement (activity_id);

CREATE INDEX IF NOT EXISTS tb_movement_content_index_vehicle_id ON tb_movement_content (vehicle_id);

CREATE INDEX IF NOT EXISTS tb_communication_index_alert_id ON tb_communication (alert_id);
