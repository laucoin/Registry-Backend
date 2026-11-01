-- tb_audit definition
CREATE TABLE tb_audit
(
    id            uuid                     NOT NULL DEFAULT uuid_generate_v4(),
    action        VARCHAR(50)              NOT NULL,
    resource_type VARCHAR(50)              NOT NULL,
    resource_id   uuid,
    project_id    uuid,
    actor_user_id uuid,
    outcome       VARCHAR(50)              NOT NULL,
    client_ip     VARCHAR(45),
    user_agent    VARCHAR(512),
    occurred_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT tb_audit_pkey PRIMARY KEY (id)
);

CREATE INDEX tb_audit_index_actor_user_id ON tb_audit (actor_user_id, occurred_at);
CREATE INDEX tb_audit_index_resource ON tb_audit (resource_type, resource_id);
CREATE INDEX tb_audit_index_occurred_at ON tb_audit (occurred_at);
