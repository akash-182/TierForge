CREATE TABLE scoring_configs (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL UNIQUE REFERENCES jobs(id),
    footfall_bar INT NOT NULL,
    footfall_weight INT NOT NULL,
    revenue_bar DOUBLE PRECISION NOT NULL,
    revenue_weight INT NOT NULL,
    size_bar INT NOT NULL,
    size_weight INT NOT NULL,
    tier_large_threshold INT NOT NULL,
    tier_medium_threshold INT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- job_id is denormalized here (also reachable via store_unit_id -> store_units.job_id) purely to
-- keep the dashboard's "scores for job X, filtered by tier" query a single indexed lookup instead
-- of a join through store_units for every read.
CREATE TABLE store_scores (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES jobs(id),
    store_unit_id UUID NOT NULL UNIQUE REFERENCES store_units(id),
    score INT NOT NULL,
    tier VARCHAR(10) NOT NULL,
    computed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_store_scores_job_tier ON store_scores(job_id, tier);
