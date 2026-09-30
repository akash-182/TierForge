-- Set (only on a requeue after a transient failure) to delay when a unit becomes eligible for
-- reclaim again, so retries back off instead of being picked up on the very next poll tick.
-- NULL for units that have never failed (fresh PENDING, or currently IN_PROGRESS) - always
-- immediately eligible.
ALTER TABLE store_units ADD COLUMN next_attempt_at TIMESTAMPTZ;
