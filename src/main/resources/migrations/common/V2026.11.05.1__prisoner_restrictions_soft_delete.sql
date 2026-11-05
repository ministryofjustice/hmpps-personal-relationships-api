-- Soft deletion of prisoner restrictions, aligned with the Alerts API's nullable deleted_at column.
alter table prisoner_restrictions add column deleted_at timestamp;
