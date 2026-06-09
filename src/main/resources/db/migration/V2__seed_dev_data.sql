-- ============================================================
-- V2__seed_dev_data.sql
-- Development seed data — DO NOT run in production.
-- Gate this migration with Flyway's "locations" config:
--   spring.flyway.locations=classpath:db/migration,classpath:db/seed
-- and keep seed scripts in a separate folder excluded from prod.
-- ============================================================

-- ── 1. Colleges ──────────────────────────────────────────────
INSERT INTO colleges (college_id, name, domain, logo_url, is_active)
VALUES
    ('11111111-0000-0000-0000-000000000001',
     'Sreenidhi Institute of Science & Technology',
     'sreenidhi.edu.in',
     'https://storage.example.com/logos/sreenidhi.png',
     TRUE),
    ('11111111-0000-0000-0000-000000000002',
     'Demo College',
     'demo.edu.in',
     NULL,
     TRUE),
    ('11111111-0000-0000-0000-000000000003',
     'Inactive College',
     'inactive.edu.in',
     NULL,
     FALSE);

-- ── 2. Users ─────────────────────────────────────────────────
-- Super Admin (no college)
INSERT INTO users (user_id, college_id, google_sub, email, full_name, role)
VALUES
    ('22222222-0000-0000-0000-000000000001',
     NULL,
     'google-sub-super-admin',
     'superadmin@platform.com',
     'Platform Super Admin',
     'SUPER_ADMIN');

-- Club Admin — Sreenidhi
INSERT INTO users (user_id, college_id, google_sub, email, full_name, role)
VALUES
    ('22222222-0000-0000-0000-000000000002',
     '11111111-0000-0000-0000-000000000001',
     'google-sub-club-admin-1',
     'clubadmin@sreenidhi.edu.in',
     'Ravi Kumar',
     'CLUB_ADMIN');

-- Students — Sreenidhi
INSERT INTO users (user_id, college_id, google_sub, email, full_name, role)
VALUES
    ('22222222-0000-0000-0000-000000000003',
     '11111111-0000-0000-0000-000000000001',
     'google-sub-student-1',
     'student1@sreenidhi.edu.in',
     'Priya Sharma',
     'STUDENT'),
    ('22222222-0000-0000-0000-000000000004',
     '11111111-0000-0000-0000-000000000001',
     'google-sub-student-2',
     'student2@sreenidhi.edu.in',
     'Arjun Reddy',
     'STUDENT');

-- ── 3. Clubs ─────────────────────────────────────────────────
INSERT INTO clubs (club_id, college_id, name, description)
VALUES
    ('33333333-0000-0000-0000-000000000001',
     '11111111-0000-0000-0000-000000000001',
     'Coding Club',
     'Competitive programming and software development'),
    ('33333333-0000-0000-0000-000000000002',
     '11111111-0000-0000-0000-000000000001',
     'Robotics Club',
     'Embedded systems and hardware projects');

-- ── 4. Events ────────────────────────────────────────────────
INSERT INTO events (event_id, club_id, college_id, title, description,
                    venue, event_date, registration_deadline, max_capacity, status, category)
VALUES
    ('44444444-0000-0000-0000-000000000001',
     '33333333-0000-0000-0000-000000000001',
     '11111111-0000-0000-0000-000000000001',
     'Hackathon 2025',
     'A 24-hour coding challenge open to all students.',
     'Main Auditorium',
     NOW() + INTERVAL '14 days',
     NOW() + INTERVAL '10 days',
     100,
     'PUBLISHED',
     'Technical'),
    ('44444444-0000-0000-0000-000000000002',
     '33333333-0000-0000-0000-000000000002',
     '11111111-0000-0000-0000-000000000001',
     'Robo Wars',
     'Battle of autonomous robots!',
     'Sports Complex',
     NOW() + INTERVAL '21 days',
     NOW() + INTERVAL '14 days',
     60,
     'PUBLISHED',
     'Technical'),
    ('44444444-0000-0000-0000-000000000003',
     '33333333-0000-0000-0000-000000000001',
     '11111111-0000-0000-0000-000000000001',
     'Draft Workshop',
     'An unpublished workshop still being planned.',
     'Lab 301',
     NOW() + INTERVAL '30 days',
     NOW() + INTERVAL '25 days',
     30,
     'DRAFT',
     'Workshop');

-- ── 5. Registrations (student1 → Hackathon) ──────────────────
INSERT INTO registrations (registration_id, user_id, event_id, qr_code, registered_at, is_cancelled)
VALUES
    ('55555555-0000-0000-0000-000000000001',
     '22222222-0000-0000-0000-000000000003',
     '44444444-0000-0000-0000-000000000001',
     'qr-priya-hackathon-2025',
     NOW() - INTERVAL '2 days',
     FALSE);

-- student2 → Hackathon (attended)
INSERT INTO registrations (registration_id, user_id, event_id, qr_code, registered_at, is_cancelled)
VALUES
    ('55555555-0000-0000-0000-000000000002',
     '22222222-0000-0000-0000-000000000004',
     '44444444-0000-0000-0000-000000000001',
     'qr-arjun-hackathon-2025',
     NOW() - INTERVAL '2 days',
     FALSE);

-- ── 6. Attendance (Arjun already scanned in) ─────────────────
INSERT INTO attendance (attendance_id, registration_id, scanned_by, scanned_at)
VALUES
    ('66666666-0000-0000-0000-000000000001',
     '55555555-0000-0000-0000-000000000002',
     '22222222-0000-0000-0000-000000000002',
     NOW() - INTERVAL '1 hour');
