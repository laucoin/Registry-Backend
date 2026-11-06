-- tb_participant unlink the users that hold no visible accepted or invited profile on the participant project
UPDATE tb_participant p
SET user_id = NULL
WHERE p.user_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM tb_project_profile pp
    WHERE pp.project_id = p.project_id
      AND pp.user_id = p.user_id
      AND pp.visible IS TRUE
      AND pp.status IN ('ACCEPTED', 'INVITED')
  )
;
