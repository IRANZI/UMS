-- PostgreSQL Database Routines for Utility Billing System
-- Each block ends with @@ (see spring.sql.init.separator in application.properties)

-- Fix must_change_password column for existing users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN DEFAULT FALSE@@
UPDATE users SET must_change_password = FALSE WHERE must_change_password IS NULL@@
ALTER TABLE users ALTER COLUMN must_change_password SET DEFAULT FALSE@@
ALTER TABLE users ALTER COLUMN must_change_password SET NOT NULL@@

CREATE OR REPLACE FUNCTION format_bill_message(
    p_customer_name VARCHAR,
    p_month INT,
    p_year INT,
    p_amount NUMERIC
) RETURNS TEXT AS $fmt$
DECLARE
    month_names TEXT[] := ARRAY['January','February','March','April','May','June',
                                'July','August','September','October','November','December'];
BEGIN
    RETURN 'Dear ' || p_customer_name || ', Your ' ||
           month_names[p_month] || '/' || p_year ||
           ' utility bill of ' || p_amount || ' FRW has been successfully processed.';
END;
$fmt$ LANGUAGE plpgsql@@

CREATE OR REPLACE FUNCTION trg_bill_notification()
RETURNS TRIGGER AS $trg1$
BEGIN
    INSERT INTO notifications (customer_id, message, type, read, created_at, updated_at, created_by, updated_by)
    SELECT NEW.customer_id,
           format_bill_message(c.full_names, NEW.billing_month, NEW.billing_year, NEW.total_amount),
           'BILL_GENERATED',
           false,
           NOW(),
           NOW(),
           COALESCE(NEW.created_by, 'system'),
           COALESCE(NEW.updated_by, 'system')
    FROM customers c WHERE c.id = NEW.customer_id;
    RETURN NEW;
END;
$trg1$ LANGUAGE plpgsql@@

CREATE OR REPLACE FUNCTION trg_payment_notification()
RETURNS TRIGGER AS $trg2$
DECLARE
    v_bill RECORD;
BEGIN
    SELECT b.*, c.full_names AS customer_name
    INTO v_bill
    FROM bills b
    JOIN customers c ON c.id = b.customer_id
    WHERE b.id = NEW.bill_id;

    IF v_bill.outstanding_balance <= 0 AND v_bill.status = 'PAID' THEN
        INSERT INTO notifications (customer_id, message, type, read, created_at, updated_at, created_by, updated_by)
        VALUES (
            v_bill.customer_id,
            'Dear ' || v_bill.customer_name || ', Your ' ||
            TO_CHAR(TO_DATE(v_bill.billing_month::TEXT, 'MM'), 'Month') || v_bill.billing_year ||
            ' utility bill of ' || v_bill.total_amount || ' FRW has been fully paid. Thank you!',
            'PAYMENT_RECEIVED',
            false,
            NOW(),
            NOW(),
            COALESCE(NEW.recorded_by, 'system'),
            COALESCE(NEW.recorded_by, 'system')
        );
    END IF;
    RETURN NEW;
END;
$trg2$ LANGUAGE plpgsql@@

CREATE OR REPLACE PROCEDURE sp_customer_bill_summary(
    IN p_customer_id BIGINT,
    INOUT p_total_billed NUMERIC DEFAULT 0,
    INOUT p_total_paid NUMERIC DEFAULT 0,
    INOUT p_total_outstanding NUMERIC DEFAULT 0
)
LANGUAGE plpgsql
AS $proc$
DECLARE
    bill_cursor CURSOR FOR
        SELECT total_amount, paid_amount, outstanding_balance
        FROM bills
        WHERE customer_id = p_customer_id;
    bill_rec RECORD;
BEGIN
    p_total_billed := 0;
    p_total_paid := 0;
    p_total_outstanding := 0;

    OPEN bill_cursor;
    LOOP
        FETCH bill_cursor INTO bill_rec;
        EXIT WHEN NOT FOUND;
        p_total_billed := p_total_billed + bill_rec.total_amount;
        p_total_paid := p_total_paid + bill_rec.paid_amount;
        p_total_outstanding := p_total_outstanding + bill_rec.outstanding_balance;
    END LOOP;
    CLOSE bill_cursor;
END;
$proc$@@

DROP TRIGGER IF EXISTS after_bill_insert ON bills@@

CREATE TRIGGER after_bill_insert
    AFTER INSERT ON bills
    FOR EACH ROW
    EXECUTE PROCEDURE trg_bill_notification()@@

DROP TRIGGER IF EXISTS after_payment_insert ON payments@@

CREATE TRIGGER after_payment_insert
    AFTER INSERT ON payments
    FOR EACH ROW
    EXECUTE PROCEDURE trg_payment_notification()@@
