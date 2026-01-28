--
-- PostgreSQL database dump
--

-- Dumped from database version 18.1
-- Dumped by pg_dump version 18.1

-- Started on 2026-01-28 07:38:23

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 5 (class 2615 OID 17157)
-- Name: public; Type: SCHEMA; Schema: -; Owner: kori
--

-- *not* creating schema, since initdb creates it


ALTER SCHEMA public OWNER TO kori;

--
-- TOC entry 5114 (class 0 OID 0)
-- Dependencies: 5
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: kori
--

COMMENT ON SCHEMA public IS '';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 219 (class 1259 OID 17322)
-- Name: account_profiles; Type: TABLE; Schema: public; Owner: kori
--

CREATE TABLE public.account_profiles (
    created_at timestamp(6) with time zone NOT NULL,
    status character varying(16) NOT NULL,
    account_type character varying(32) NOT NULL,
    owner_ref character varying(64) NOT NULL
);


ALTER TABLE public.account_profiles OWNER TO kori;

--
-- TOC entry 220 (class 1259 OID 17331)
-- Name: agents; Type: TABLE; Schema: public; Owner: kori
--

CREATE TABLE public.agents (
    created_at timestamp(6) with time zone NOT NULL,
    code character varying(16) NOT NULL,
    id uuid NOT NULL,
    status character varying(16) NOT NULL
);


ALTER TABLE public.agents OWNER TO kori;

--
-- TOC entry 221 (class 1259 OID 17342)
-- Name: audit_events; Type: TABLE; Schema: public; Owner: kori
--

CREATE TABLE public.audit_events (
    occurred_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    actor_type character varying(32) NOT NULL,
    action character varying(128) NOT NULL,
    actor_id character varying(128) NOT NULL,
    metadata_json text NOT NULL
);


ALTER TABLE public.audit_events OWNER TO kori;

--
-- TOC entry 222 (class 1259 OID 17355)
-- Name: cards; Type: TABLE; Schema: public; Owner: kori
--

CREATE TABLE public.cards (
    failed_pin_attempts integer NOT NULL,
    client_id uuid NOT NULL,
    id uuid NOT NULL,
    status character varying(16) NOT NULL,
    card_uid character varying(64) NOT NULL,
    hashed_pin character varying(255) NOT NULL
);


ALTER TABLE public.cards OWNER TO kori;

--
-- TOC entry 223 (class 1259 OID 17368)
-- Name: clients; Type: TABLE; Schema: public; Owner: kori
--

CREATE TABLE public.clients (
    id uuid NOT NULL,
    status character varying(16) NOT NULL,
    phone_number character varying(32) NOT NULL
);


ALTER TABLE public.clients OWNER TO kori;

--
-- TOC entry 224 (class 1259 OID 17378)
-- Name: commission_config; Type: TABLE; Schema: public; Owner: kori
--

CREATE TABLE public.commission_config (
    card_enrollment_agent_commission numeric(19,2) NOT NULL,
    id integer NOT NULL,
    merchant_withdraw_commission_max numeric(19,2),
    merchant_withdraw_commission_min numeric(19,2),
    merchant_withdraw_commission_rate numeric(10,6) NOT NULL
);


ALTER TABLE public.commission_config OWNER TO kori;

--
-- TOC entry 225 (class 1259 OID 17386)
-- Name: fee_config; Type: TABLE; Schema: public; Owner: kori
--

CREATE TABLE public.fee_config (
    card_enrollment_price numeric(19,2) NOT NULL,
    card_payment_fee_max numeric(19,2) NOT NULL,
    card_payment_fee_min numeric(19,2) NOT NULL,
    card_payment_fee_rate numeric(10,6) NOT NULL,
    id integer NOT NULL,
    merchant_withdraw_fee_max numeric(19,2) NOT NULL,
    merchant_withdraw_fee_min numeric(19,2) NOT NULL,
    merchant_withdraw_fee_rate numeric(10,6) NOT NULL
);


ALTER TABLE public.fee_config OWNER TO kori;

--
-- TOC entry 226 (class 1259 OID 17399)
-- Name: idempotency_records; Type: TABLE; Schema: public; Owner: kori
--

CREATE TABLE public.idempotency_records (
    created_at timestamp(6) with time zone,
    idempotency_key character varying(128) NOT NULL,
    result_type character varying(256) NOT NULL,
    result_json text NOT NULL
);


ALTER TABLE public.idempotency_records OWNER TO kori;

--
-- TOC entry 227 (class 1259 OID 17409)
-- Name: ledger_entries; Type: TABLE; Schema: public; Owner: kori
--

CREATE TABLE public.ledger_entries (
    amount numeric(19,2) NOT NULL,
    created_at timestamp(6) with time zone,
    entry_type character varying(16) NOT NULL,
    id uuid NOT NULL,
    transaction_id uuid NOT NULL,
    account_type character varying(32) NOT NULL,
    owner_ref character varying(128) NOT NULL
);


ALTER TABLE public.ledger_entries OWNER TO kori;

--
-- TOC entry 228 (class 1259 OID 17420)
-- Name: merchants; Type: TABLE; Schema: public; Owner: kori
--

CREATE TABLE public.merchants (
    created_at timestamp(6) with time zone NOT NULL,
    code character varying(16) NOT NULL,
    id uuid NOT NULL,
    status character varying(16) NOT NULL
);


ALTER TABLE public.merchants OWNER TO kori;

--
-- TOC entry 229 (class 1259 OID 17431)
-- Name: payouts; Type: TABLE; Schema: public; Owner: kori
--

CREATE TABLE public.payouts (
    amount numeric(19,2) NOT NULL,
    completed_at timestamp(6) with time zone,
    created_at timestamp(6) with time zone NOT NULL,
    failed_at timestamp(6) with time zone,
    agent_id uuid NOT NULL,
    id uuid NOT NULL,
    status character varying(16) NOT NULL,
    transaction_id uuid NOT NULL,
    failure_reason character varying(255)
);


ALTER TABLE public.payouts OWNER TO kori;

--
-- TOC entry 230 (class 1259 OID 17444)
-- Name: security_config; Type: TABLE; Schema: public; Owner: kori
--

CREATE TABLE public.security_config (
    id integer NOT NULL,
    max_failed_pin_attempts integer NOT NULL
);


ALTER TABLE public.security_config OWNER TO kori;

--
-- TOC entry 231 (class 1259 OID 17451)
-- Name: terminals; Type: TABLE; Schema: public; Owner: kori
--

CREATE TABLE public.terminals (
    id uuid NOT NULL,
    merchant_id uuid NOT NULL,
    status character varying(16) NOT NULL
);


ALTER TABLE public.terminals OWNER TO kori;

--
-- TOC entry 232 (class 1259 OID 17459)
-- Name: transactions; Type: TABLE; Schema: public; Owner: kori
--

CREATE TABLE public.transactions (
    amount numeric(19,2) NOT NULL,
    created_at timestamp(6) with time zone NOT NULL,
    id uuid NOT NULL,
    original_transaction_id uuid,
    type character varying(64) NOT NULL
);


ALTER TABLE public.transactions OWNER TO kori;

--
-- TOC entry 4908 (class 2606 OID 17330)
-- Name: account_profiles account_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.account_profiles
    ADD CONSTRAINT account_profiles_pkey PRIMARY KEY (account_type, owner_ref);


--
-- TOC entry 4911 (class 2606 OID 17341)
-- Name: agents agents_code_key; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.agents
    ADD CONSTRAINT agents_code_key UNIQUE (code);


--
-- TOC entry 4913 (class 2606 OID 17339)
-- Name: agents agents_pkey; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.agents
    ADD CONSTRAINT agents_pkey PRIMARY KEY (id);


--
-- TOC entry 4917 (class 2606 OID 17354)
-- Name: audit_events audit_events_pkey; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.audit_events
    ADD CONSTRAINT audit_events_pkey PRIMARY KEY (id);


--
-- TOC entry 4921 (class 2606 OID 17367)
-- Name: cards cards_card_uid_key; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.cards
    ADD CONSTRAINT cards_card_uid_key UNIQUE (card_uid);


--
-- TOC entry 4923 (class 2606 OID 17365)
-- Name: cards cards_pkey; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.cards
    ADD CONSTRAINT cards_pkey PRIMARY KEY (id);


--
-- TOC entry 4927 (class 2606 OID 17377)
-- Name: clients clients_phone_number_key; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.clients
    ADD CONSTRAINT clients_phone_number_key UNIQUE (phone_number);


--
-- TOC entry 4929 (class 2606 OID 17375)
-- Name: clients clients_pkey; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.clients
    ADD CONSTRAINT clients_pkey PRIMARY KEY (id);


--
-- TOC entry 4931 (class 2606 OID 17385)
-- Name: commission_config commission_config_pkey; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.commission_config
    ADD CONSTRAINT commission_config_pkey PRIMARY KEY (id);


--
-- TOC entry 4933 (class 2606 OID 17398)
-- Name: fee_config fee_config_pkey; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.fee_config
    ADD CONSTRAINT fee_config_pkey PRIMARY KEY (id);


--
-- TOC entry 4935 (class 2606 OID 17408)
-- Name: idempotency_records idempotency_records_pkey; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.idempotency_records
    ADD CONSTRAINT idempotency_records_pkey PRIMARY KEY (idempotency_key);


--
-- TOC entry 4940 (class 2606 OID 17419)
-- Name: ledger_entries ledger_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.ledger_entries
    ADD CONSTRAINT ledger_entries_pkey PRIMARY KEY (id);


--
-- TOC entry 4943 (class 2606 OID 17430)
-- Name: merchants merchants_code_key; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.merchants
    ADD CONSTRAINT merchants_code_key UNIQUE (code);


--
-- TOC entry 4945 (class 2606 OID 17428)
-- Name: merchants merchants_pkey; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.merchants
    ADD CONSTRAINT merchants_pkey PRIMARY KEY (id);


--
-- TOC entry 4949 (class 2606 OID 17441)
-- Name: payouts payouts_pkey; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.payouts
    ADD CONSTRAINT payouts_pkey PRIMARY KEY (id);


--
-- TOC entry 4951 (class 2606 OID 17443)
-- Name: payouts payouts_transaction_id_key; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.payouts
    ADD CONSTRAINT payouts_transaction_id_key UNIQUE (transaction_id);


--
-- TOC entry 4953 (class 2606 OID 17450)
-- Name: security_config security_config_pkey; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.security_config
    ADD CONSTRAINT security_config_pkey PRIMARY KEY (id);


--
-- TOC entry 4957 (class 2606 OID 17458)
-- Name: terminals terminals_pkey; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.terminals
    ADD CONSTRAINT terminals_pkey PRIMARY KEY (id);


--
-- TOC entry 4961 (class 2606 OID 17467)
-- Name: transactions transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: kori
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_pkey PRIMARY KEY (id);


--
-- TOC entry 4909 (class 1259 OID 17468)
-- Name: idx_account_profiles_status; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_account_profiles_status ON public.account_profiles USING btree (status);


--
-- TOC entry 4914 (class 1259 OID 17469)
-- Name: idx_agent_code; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_agent_code ON public.agents USING btree (code);


--
-- TOC entry 4915 (class 1259 OID 17470)
-- Name: idx_agent_status; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_agent_status ON public.agents USING btree (status);


--
-- TOC entry 4918 (class 1259 OID 17472)
-- Name: idx_audit_action; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_audit_action ON public.audit_events USING btree (action);


--
-- TOC entry 4919 (class 1259 OID 17471)
-- Name: idx_audit_occurred_at; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_audit_occurred_at ON public.audit_events USING btree (occurred_at);


--
-- TOC entry 4924 (class 1259 OID 17473)
-- Name: idx_cards_client_id; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_cards_client_id ON public.cards USING btree (client_id);


--
-- TOC entry 4925 (class 1259 OID 17474)
-- Name: idx_cards_status; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_cards_status ON public.cards USING btree (status);


--
-- TOC entry 4936 (class 1259 OID 17476)
-- Name: idx_ledger_account; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_ledger_account ON public.ledger_entries USING btree (account_type, owner_ref);


--
-- TOC entry 4937 (class 1259 OID 17477)
-- Name: idx_ledger_created_at; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_ledger_created_at ON public.ledger_entries USING btree (created_at);


--
-- TOC entry 4938 (class 1259 OID 17475)
-- Name: idx_ledger_tx; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_ledger_tx ON public.ledger_entries USING btree (transaction_id);


--
-- TOC entry 4941 (class 1259 OID 17478)
-- Name: idx_merchant_status; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_merchant_status ON public.merchants USING btree (status);


--
-- TOC entry 4946 (class 1259 OID 17479)
-- Name: idx_payouts_agent; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_payouts_agent ON public.payouts USING btree (agent_id);


--
-- TOC entry 4947 (class 1259 OID 17480)
-- Name: idx_payouts_status; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_payouts_status ON public.payouts USING btree (status);


--
-- TOC entry 4954 (class 1259 OID 17481)
-- Name: idx_terminal_merchant; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_terminal_merchant ON public.terminals USING btree (merchant_id);


--
-- TOC entry 4955 (class 1259 OID 17482)
-- Name: idx_terminal_status; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_terminal_status ON public.terminals USING btree (status);


--
-- TOC entry 4958 (class 1259 OID 17483)
-- Name: idx_transactions_created_at; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_transactions_created_at ON public.transactions USING btree (created_at);


--
-- TOC entry 4959 (class 1259 OID 17484)
-- Name: idx_transactions_original_tx; Type: INDEX; Schema: public; Owner: kori
--

CREATE INDEX idx_transactions_original_tx ON public.transactions USING btree (original_transaction_id);


--
-- TOC entry 5115 (class 0 OID 0)
-- Dependencies: 5
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: kori
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;


-- Completed on 2026-01-28 07:38:23

--
-- PostgreSQL database dump complete
--