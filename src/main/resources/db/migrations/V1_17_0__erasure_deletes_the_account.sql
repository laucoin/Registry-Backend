-- Erasure is a real deletion, so the "purged" soft-delete state goes away.
--
-- Rows already marked purged are anonymized shadow accounts: their personal data
-- is gone, but they kept their oidc_id, which made them both unremovable through
-- the API (every administrative read filters purged rows out) and permanently
-- blocking for the provider identity behind them. Deleting them completes the
-- erasure they were meant to be: memberships and preferences cascade, and the
-- records they created keep existing with their author reference set to null.
DELETE
FROM tb_user
WHERE purged IS TRUE;

ALTER TABLE tb_user
    DROP COLUMN purged;

-- Never set by any code path: participants are deleted outright by the retention
-- purge, never marked. The filters that read it excluded nothing.
ALTER TABLE tb_participant
    DROP COLUMN purged;
