-- Payment flow support for Toss Payments normal payment MVP.

ALTER TABLE payment
    ALTER COLUMN subscription_id DROP NOT NULL;

ALTER TABLE payment
    ADD COLUMN creator_id BIGINT;

UPDATE payment p
SET creator_id = s.creator_id
FROM subscription s
WHERE p.subscription_id = s.id;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM payment WHERE creator_id IS NULL) THEN
        RAISE EXCEPTION 'payment.creator_id backfill failed';
    END IF;
END $$;

ALTER TABLE payment
    ALTER COLUMN creator_id SET NOT NULL;

ALTER TABLE payment
    ADD CONSTRAINT fk_payment_creator FOREIGN KEY (creator_id) REFERENCES member (id);

ALTER TABLE payment
    ADD COLUMN customer_key VARCHAR(50);

UPDATE payment
SET customer_key = 'legacy-' || substring(md5(order_id || ':' || id::text), 1, 32)
WHERE customer_key IS NULL;

ALTER TABLE payment
    ALTER COLUMN customer_key SET NOT NULL;

ALTER TABLE payment
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE payment
    DROP CONSTRAINT ck_payment_status;

ALTER TABLE payment
    ADD CONSTRAINT ck_payment_status CHECK (
        status IN ('PENDING', 'CONFIRMING', 'SUCCESS', 'FAILED', 'CANCELED')
    );

CREATE INDEX idx_payment_member_creator_status_created
    ON payment (member_id, creator_id, status, created_at DESC);
