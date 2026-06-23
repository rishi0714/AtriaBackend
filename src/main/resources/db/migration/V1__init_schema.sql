--
-- PostgreSQL database dump
--

\restrict Kg7ICsGvnm3tg4uKyhFMTWhBNr5VsjafqBBHuh3moDhPvmviEFloI9BWVtM8Y8Q

-- Dumped from database version 16.14 (Debian 16.14-1.pgdg13+1)
-- Dumped by pg_dump version 16.14 (Debian 16.14-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: attendance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.attendance (
    created_at timestamp(6) without time zone,
    scanned_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    attendance_id uuid NOT NULL,
    registration_id uuid NOT NULL,
    scanned_by uuid NOT NULL
);


--
-- Name: clubs; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.clubs (
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    club_id uuid NOT NULL,
    college_id uuid NOT NULL,
    managed_by uuid,
    club_category character varying(255),
    description text,
    logo_url character varying(255),
    name character varying(255) NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);


--
-- Name: college_domains; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.college_domains (
    is_primary boolean NOT NULL,
    created_at timestamp(6) without time zone,
    college_id uuid NOT NULL,
    id uuid NOT NULL,
    domain character varying(255) NOT NULL
);


--
-- Name: colleges; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.colleges (
    is_active boolean NOT NULL,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    college_id uuid NOT NULL,
    logo_url character varying(255),
    name character varying(255) NOT NULL
);


--
-- Name: events; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.events (
    is_open_to_all boolean NOT NULL,
    max_capacity integer NOT NULL,
    created_at timestamp(6) without time zone,
    event_date timestamp(6) without time zone NOT NULL,
    registration_deadline timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    club_id uuid NOT NULL,
    college_id uuid NOT NULL,
    event_id uuid NOT NULL,
    category character varying(255),
    description text,
    poster_url character varying(255),
    rejection_reason text,
    status character varying(255) NOT NULL,
    title character varying(255) NOT NULL,
    venue character varying(255) NOT NULL,
    CONSTRAINT events_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING_APPROVAL'::character varying, 'PUBLISHED'::character varying, 'REJECTED'::character varying, 'REGISTRATION_CLOSED'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[])))
);


--
-- Name: registrations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.registrations (
    is_cancelled boolean NOT NULL,
    created_at timestamp(6) without time zone,
    registered_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone,
    event_id uuid NOT NULL,
    registration_id uuid NOT NULL,
    user_id uuid NOT NULL,
    qr_code character varying(255) NOT NULL
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    profile_complete boolean NOT NULL,
    year smallint,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    phone_number character varying(15),
    college_id uuid,
    user_id uuid NOT NULL,
    registration_number character varying(50),
    stream character varying(100),
    email character varying(255) NOT NULL,
    full_name character varying(255) NOT NULL,
    google_sub character varying(255),
    picture_url character varying(255),
    role character varying(255) NOT NULL,
    refresh_token character varying(512),
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['STUDENT'::character varying, 'CLUB_ADMIN'::character varying, 'COLLEGE_ADMIN'::character varying, 'PLATFORM_OWNER'::character varying])::text[])))
);


--
-- Name: attendance attendance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT attendance_pkey PRIMARY KEY (attendance_id);


--
-- Name: attendance attendance_registration_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT attendance_registration_id_key UNIQUE (registration_id);


--
-- Name: clubs clubs_managed_by_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clubs
    ADD CONSTRAINT clubs_managed_by_key UNIQUE (managed_by);


--
-- Name: clubs clubs_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clubs
    ADD CONSTRAINT clubs_pkey PRIMARY KEY (club_id);


--
-- Name: college_domains college_domains_domain_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.college_domains
    ADD CONSTRAINT college_domains_domain_key UNIQUE (domain);


--
-- Name: college_domains college_domains_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.college_domains
    ADD CONSTRAINT college_domains_pkey PRIMARY KEY (id);


--
-- Name: colleges colleges_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.colleges
    ADD CONSTRAINT colleges_pkey PRIMARY KEY (college_id);


--
-- Name: events events_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.events
    ADD CONSTRAINT events_pkey PRIMARY KEY (event_id);


--
-- Name: registrations registrations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.registrations
    ADD CONSTRAINT registrations_pkey PRIMARY KEY (registration_id);


--
-- Name: registrations registrations_qr_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.registrations
    ADD CONSTRAINT registrations_qr_code_key UNIQUE (qr_code);


--
-- Name: clubs uq_club_name_per_college; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clubs
    ADD CONSTRAINT uq_club_name_per_college UNIQUE (college_id, name);


--
-- Name: registrations uq_user_event_registration; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.registrations
    ADD CONSTRAINT uq_user_event_registration UNIQUE (user_id, event_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_google_sub_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_google_sub_key UNIQUE (google_sub);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);


--
-- Name: uq_colleges_name_upper; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_colleges_name_upper ON public.colleges USING btree (upper(TRIM(BOTH FROM name)));


--
-- Name: events fk3g7eqy9h9kov3icbumnqmjsj3; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.events
    ADD CONSTRAINT fk3g7eqy9h9kov3icbumnqmjsj3 FOREIGN KEY (college_id) REFERENCES public.colleges(college_id);


--
-- Name: registrations fk8mi58jt1s8fxmi56jnau0cxqw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.registrations
    ADD CONSTRAINT fk8mi58jt1s8fxmi56jnau0cxqw FOREIGN KEY (event_id) REFERENCES public.events(event_id);


--
-- Name: attendance fkaxkovcn7kl3pu8qlm0fim0ntu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT fkaxkovcn7kl3pu8qlm0fim0ntu FOREIGN KEY (scanned_by) REFERENCES public.users(user_id);


--
-- Name: college_domains fkiafw37kvfk1cs7agy84htkofi; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.college_domains
    ADD CONSTRAINT fkiafw37kvfk1cs7agy84htkofi FOREIGN KEY (college_id) REFERENCES public.colleges(college_id);


--
-- Name: registrations fkl2iby9n9hp8jwkfj8i96pkxpi; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.registrations
    ADD CONSTRAINT fkl2iby9n9hp8jwkfj8i96pkxpi FOREIGN KEY (user_id) REFERENCES public.users(user_id);


--
-- Name: attendance fkmlk88rqdy5jyf5ckxmmpm00vf; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.attendance
    ADD CONSTRAINT fkmlk88rqdy5jyf5ckxmmpm00vf FOREIGN KEY (registration_id) REFERENCES public.registrations(registration_id);


--
-- Name: events fkmt9rjn9hbh6g8isda7c1g14bd; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.events
    ADD CONSTRAINT fkmt9rjn9hbh6g8isda7c1g14bd FOREIGN KEY (club_id) REFERENCES public.clubs(club_id);


--
-- Name: clubs fknbky87kgk4ayi8e8vpjht6m6j; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clubs
    ADD CONSTRAINT fknbky87kgk4ayi8e8vpjht6m6j FOREIGN KEY (college_id) REFERENCES public.colleges(college_id);


--
-- Name: users fkq8c77pl7fllv195wbwqn13375; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fkq8c77pl7fllv195wbwqn13375 FOREIGN KEY (college_id) REFERENCES public.colleges(college_id);


--
-- Name: clubs fksp8dayqfsibg1k08k5gb385oo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clubs
    ADD CONSTRAINT fksp8dayqfsibg1k08k5gb385oo FOREIGN KEY (managed_by) REFERENCES public.users(user_id);


--
-- PostgreSQL database dump complete
--

\unrestrict Kg7ICsGvnm3tg4uKyhFMTWhBNr5VsjafqBBHuh3moDhPvmviEFloI9BWVtM8Y8Q

