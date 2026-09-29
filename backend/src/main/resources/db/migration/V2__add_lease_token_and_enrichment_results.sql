ALTER TABLE store_units ADD COLUMN lease_token UUID;

CREATE TABLE enrichment_results (
    id UUID PRIMARY KEY,
    store_unit_id UUID NOT NULL UNIQUE REFERENCES store_units(id),
    estimated_monthly_footfall INT NOT NULL,
    estimated_monthly_revenue DOUBLE PRECISION NOT NULL,
    store_size_sqft INT NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
