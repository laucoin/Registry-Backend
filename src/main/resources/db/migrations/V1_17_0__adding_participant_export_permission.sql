-- tb_project_permission insert participant export in project permissions
INSERT INTO tb_project_permission(name)
VALUES ('REGISTRY_PROJECT_PARTICIPANT_EXPORT')
;

-- tb_project_role_permission insert participant export in project role/permission mapping (administrators only)
INSERT INTO tb_project_role_permission(role, permission)
VALUES ('PROJECT_ADMINISTRATOR', 'REGISTRY_PROJECT_PARTICIPANT_EXPORT')
;
