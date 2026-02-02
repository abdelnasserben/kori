CREATE TABLE public.admins (
    id uuid NOT NULL,
    status character varying(16) NOT NULL,
    created_at timestamp without time zone NOT NULL
);

ALTER TABLE public.admins OWNER TO kori;

ALTER TABLE ONLY public.admins
    ADD CONSTRAINT admins_pkey PRIMARY KEY (id);

CREATE INDEX idx_admin_status ON public.admins USING btree (status);