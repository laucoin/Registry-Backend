-- tb_user_permission insert scheduled job permissions
INSERT INTO tb_user_permission(name)
VALUES ('REGISTRY_JOB_C');

-- tb_user_role_permission insert scheduled job in role/permission mapping
INSERT INTO tb_user_role_permission(role, permission)
VALUES ('USER_ADMINISTRATOR', 'REGISTRY_JOB_C');
