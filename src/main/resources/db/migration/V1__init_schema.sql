-- ============================================================
-- V1__init_schema.sql
-- Campus Event & Attendance Management Platform
-- Initial schema — Flyway baseline migration
-- ============================================================

-- ── Enable pgcrypto for gen_random_uuid() if needed ─────────
-- (Not required on PG 13+ where gen_random_uuid() is built-in)

-- ── 1. COLLEGES (tenant root) ────────────────────────────────
CREATE TABLE colleges (
    college_id  UUID          NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(255)  NOT NULL,
    domain      VARCHAR(255)  NOT NULL,      -- e.g. sreenidhi.edu.in
    logo_url    TEXT,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_colleges            PRIMARY KEY (college_id),
    CONSTRAINT uq_colleges_domain     UNIQUE      (domain)
);

COMMENT ON TABLE  colleges           IS 'Tenant root — one row per onboarded college.';
COMMENT ON COLUMN colleges.domain    IS 'Google Workspace email domain used for tenant resolution at OAuth2 login.';

-- ── 2. USERS ─────────────────────────────────────────────────
CREATE TABLE users (
    user_id     UUID          NOT NULL DEFAULT gen_random_uuid(),
    college_id  UUID,                        -- NULL for SUPER_ADMIN
    google_sub  VARCHAR(255)  NOT NULL,      -- immutable Google identity anchor
    email       VARCHAR(255)  NOT NULL,
    full_name   VARCHAR(255)  NOT NULL,
    picture_url TEXT,
    role        VARCHAR(20)   NOT NULL DEFAULT 'STUDENT'
                              CHECK (role IN ('STUDENT', 'CLUB_ADMIN', 'SUPER_ADMIN')),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users               PRIMARY KEY (user_id),
    CONSTRAINT uq_users_google_sub    UNIQUE      (google_sub),
    CONSTRAINT uq_users_email         UNIQUE      (email),
    CONSTRAINT fk_users_college       FOREIGN KEY (college_id)
                                      REFERENCES  colleges (college_id)
                                      ON DELETE   RESTRICT
);

COMMENT ON COLUMN users.college_id  IS 'NULL for SUPER_ADMIN role — not tied to any tenant.';
COMMENT ON COLUMN users.google_sub  IS 'Google sub claim from ID token — globally unique, never changes.';

-- ── 3. CLUBS ─────────────────────────────────────────────────
CREATE TABLE clubs (
    club_id     UUID          NOT NULL DEFAULT gen_random_uuid(),
    college_id  UUID          NOT NULL,
    name        VARCHAR(255)  NOT NULL,
    description TEXT,
    logo_url    TEXT,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_clubs               PRIMARY KEY (club_id),
    CONSTRAINT uq_club_name_per_college UNIQUE    (college_id, name),
    CONSTRAINT fk_clubs_college       FOREIGN KEY (college_id)
                                      REFERENCES  colleges (college_id)
                                      ON DELETE   CASCADE
);

COMMENT ON CONSTRAINT uq_club_name_per_college ON clubs
    IS 'Club names are unique within a tenant, not globally.';

-- ── 4. EVENTS ────────────────────────────────────────────────
CREATE TABLE events (
    event_id              UUID          NOT NULL DEFAULT gen_random_uuid(),
    club_id               UUID          NOT NULL,
    college_id            UUID          NOT NULL,  -- denormalized for fast tenant filtering
    title                 VARCHAR(255)  NOT NULL,
    description           TEXT,
    venue                 VARCHAR(255)  NOT NULL,
    event_date            TIMESTAMPTZ   NOT NULL,
    registration_deadline TIMESTAMPTZ   NOT NULL,
    max_capacity          INT           NOT NULL CHECK (max_capacity > 0),
    status                VARCHAR(30)   NOT NULL DEFAULT 'DRAFT'
                          CHECK (status IN ('DRAFT','PUBLISHED','REGISTRATION_CLOSED',
                                            'COMPLETED','CANCELLED')),
    poster_url            TEXT,
    category              VARCHAR(100),
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_events              PRIMARY KEY (event_id),
    CONSTRAINT fk_events_club         FOREIGN KEY (club_id)
                                      REFERENCES  clubs (club_id)
                                      ON DELETE   CASCADE,
    CONSTRAINT fk_events_college      FOREIGN KEY (college_id)
                                      REFERENCES  colleges (college_id)
                                      ON DELETE   CASCADE
);

COMMENT ON COLUMN events.college_id
    IS 'Denormalized FK — avoids events→clubs→college join on the most-queried table.';

-- ── 5. REGISTRATIONS ─────────────────────────────────────────
CREATE TABLE registrations (
    registration_id UUID          NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID          NOT NULL,
    event_id        UUID          NOT NULL,
    qr_code         VARCHAR(255)  NOT NULL,   -- UUID v4 token, globally unique
    registered_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    is_cancelled    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_registrations           PRIMARY KEY (registration_id),
    CONSTRAINT uq_registrations_qr_code   UNIQUE      (qr_code),
    CONSTRAINT uq_user_event_registration UNIQUE      (user_id, event_id),
    CONSTRAINT fk_registrations_user      FOREIGN KEY (user_id)
                                          REFERENCES  users (user_id)
                                          ON DELETE   CASCADE,
    CONSTRAINT fk_registrations_event     FOREIGN KEY (event_id)
                                          REFERENCES  events (event_id)
                                          ON DELETE   CASCADE
);

COMMENT ON CONSTRAINT uq_user_event_registration ON registrations
    IS 'Prevents duplicate registration by the same student for the same event.';

-- ── 6. ATTENDANCE ─────────────────────────────────────────────
CREATE TABLE attendance (
    attendance_id   UUID          NOT NULL DEFAULT gen_random_uuid(),
    registration_id UUID          NOT NULL,
    scanned_by      UUID          NOT NULL,   -- Club Admin user_id
    scanned_at      TIMESTAMPTZ   NOT NULL,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_attendance               PRIMARY KEY (attendance_id),
    CONSTRAINT uq_attendance_registration  UNIQUE      (registration_id),  -- no duplicate scans
    CONSTRAINT fk_attendance_registration  FOREIGN KEY (registration_id)
                                           REFERENCES  registrations (registration_id)
                                           ON DELETE   CASCADE,
    CONSTRAINT fk_attendance_scanned_by    FOREIGN KEY (scanned_by)
                                           REFERENCES  users (user_id)
                                           ON DELETE   RESTRICT
);

COMMENT ON CONSTRAINT uq_attendance_registration ON attendance
    IS 'Enforces single attendance record per registration — prevents duplicate QR scans at DB level.';

-- ── 7. INDEXES ───────────────────────────────────────────────
-- Tenant resolution at login (hot path)
CREATE INDEX idx_colleges_domain           ON colleges       (domain);

-- Tenant-scoped event browsing (most frequent query)
CREATE INDEX idx_events_college_status     ON events         (college_id, status);
CREATE INDEX idx_events_college_date       ON events         (college_id, event_date);

-- Student's own registrations
CREATE INDEX idx_registrations_user_id     ON registrations  (user_id);
-- Participant list for an event
CREATE INDEX idx_registrations_event_id    ON registrations  (event_id);
-- QR scan lookup
CREATE INDEX idx_registrations_qr_code     ON registrations  (qr_code);

-- Attendance report per event (traverses via registration)
CREATE INDEX idx_attendance_registration   ON attendance     (registration_id);

-- User lookup by google_sub (OAuth2 login, after domain check)
CREATE INDEX idx_users_google_sub          ON users          (google_sub);
CREATE INDEX idx_users_college_id          ON users          (college_id);

-- ── 8. updated_at auto-trigger ────────────────────────────────
-- Automatically keeps updated_at current without relying solely on JPA
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_colleges_updated_at
    BEFORE UPDATE ON colleges
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_clubs_updated_at
    BEFORE UPDATE ON clubs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_events_updated_at
    BEFORE UPDATE ON events
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_registrations_updated_at
    BEFORE UPDATE ON registrations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_attendance_updated_at
    BEFORE UPDATE ON attendance
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
