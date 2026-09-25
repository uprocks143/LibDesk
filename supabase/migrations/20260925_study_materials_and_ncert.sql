-- =========================================================================
-- LibDesk Migration: Study Material Storage & NCERT Official Catalog
-- Migration: 20260925_study_materials_and_ncert.sql
-- Self-contained: includes all required role helper functions & RLS policies
-- =========================================================================

-- 1. Helper Functions for JWT Custom Claims & Role Checking (O(1) execution)
CREATE OR REPLACE FUNCTION public.jwt_role()
RETURNS text AS $$
  SELECT COALESCE(
    (auth.jwt() ->> 'role'),
    (auth.jwt() -> 'app_metadata' ->> 'role'),
    (auth.jwt() -> 'user_metadata' ->> 'role'),
    'anon'
  );
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION public.jwt_org_id()
RETURNS text AS $$
  SELECT COALESCE(
    (auth.jwt() ->> 'org_id'),
    (auth.jwt() -> 'app_metadata' ->> 'org_id'),
    (auth.jwt() -> 'app_metadata' ->> 'library_id'),
    (auth.jwt() -> 'user_metadata' ->> 'org_id'),
    (auth.jwt() -> 'user_metadata' ->> 'library_id'),
    ''
  );
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION public.jwt_student_id()
RETURNS text AS $$
  SELECT COALESCE(
    (auth.jwt() ->> 'student_id'),
    (auth.jwt() -> 'app_metadata' ->> 'student_id'),
    (auth.jwt() -> 'user_metadata' ->> 'student_id'),
    ''
  );
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION public.is_super_admin()
RETURNS boolean AS $$
  SELECT public.jwt_role() IN ('SUPER_ADMIN', 'MASTER_ADMIN');
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION public.is_library_owner()
RETURNS boolean AS $$
  SELECT public.jwt_role() IN ('OWNER', 'MANAGER', 'ADMIN', 'STAFF');
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION public.is_student()
RETURNS boolean AS $$
  SELECT public.jwt_role() IN ('STUDENT', 'MEMBER');
$$ LANGUAGE sql STABLE;

-- 2. Create Private Supabase Storage Bucket for Study Materials (Max 20 MB, PDF only)
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'study-materials',
    'study-materials',
    false,
    20971520, -- 20 MB max limit
    ARRAY['application/pdf']::text[]
)
ON CONFLICT (id) DO UPDATE SET
    public = false,
    file_size_limit = 20971520,
    allowed_mime_types = ARRAY['application/pdf']::text[];

-- 3. Create study_materials Table
CREATE TABLE IF NOT EXISTS public.study_materials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    category TEXT NOT NULL, -- 'ncert', 'mock-tests', 'notes', 'competitive', 'other'
    storage_path TEXT NOT NULL,
    file_size_bytes BIGINT DEFAULT 0,
    page_count INT DEFAULT 0,
    is_free BOOLEAN DEFAULT true,
    shift_access TEXT[] DEFAULT NULL,
    uploaded_by TEXT NOT NULL,
    download_count INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    deleted_at TIMESTAMPTZ DEFAULT NULL
);

-- 4. Create ncert_catalog Table (Metadata & Official Links Only - No Hosted PDFs)
CREATE TABLE IF NOT EXISTS public.ncert_catalog (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_level INT NOT NULL CHECK (class_level BETWEEN 1 AND 12),
    subject TEXT NOT NULL,
    book_title TEXT NOT NULL,
    medium TEXT NOT NULL, -- 'english', 'hindi', 'urdu'
    language TEXT DEFAULT 'en',
    edition_year TEXT DEFAULT '2026-27',
    source_name TEXT NOT NULL DEFAULT 'ncert', -- 'ncert', 'epathshala', 'diksha'
    source_url TEXT NOT NULL,
    thumbnail_url TEXT DEFAULT '',
    page_count INT DEFAULT 0,
    file_size_bytes BIGINT DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    last_verified TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Create download_logs Table (Auditing & Analytics)
CREATE TABLE IF NOT EXISTS public.download_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id TEXT NOT NULL,
    material_id TEXT NOT NULL,
    source TEXT NOT NULL, -- 'OWNER_UPLOAD' or 'NCERT_OFFICIAL'
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. RPC Function: Increment Download Count Safely
CREATE OR REPLACE FUNCTION public.increment_download_count(p_material_id UUID)
RETURNS void AS $$
BEGIN
    UPDATE public.study_materials
    SET download_count = download_count + 1,
        updated_at = NOW()
    WHERE id = p_material_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 7. Indexes for High Performance
CREATE INDEX IF NOT EXISTS idx_study_materials_org_cat ON public.study_materials(org_id, category);
CREATE INDEX IF NOT EXISTS idx_study_materials_deleted ON public.study_materials(deleted_at);
CREATE INDEX IF NOT EXISTS idx_study_materials_is_free ON public.study_materials(is_free);
CREATE INDEX IF NOT EXISTS idx_ncert_class_subj_med ON public.ncert_catalog(class_level, subject, medium);
CREATE INDEX IF NOT EXISTS idx_ncert_is_active ON public.ncert_catalog(is_active);
CREATE INDEX IF NOT EXISTS idx_download_logs_material ON public.download_logs(material_id);
CREATE INDEX IF NOT EXISTS idx_download_logs_student ON public.download_logs(student_id);

-- 8. Enable RLS
ALTER TABLE public.study_materials ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ncert_catalog ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.download_logs ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if already defined to allow idempotent re-runs
DROP POLICY IF EXISTS "owner_full_study_materials" ON public.study_materials;
DROP POLICY IF EXISTS "student_select_study_materials" ON public.study_materials;
DROP POLICY IF EXISTS "super_admin_select_study_materials" ON public.study_materials;
DROP POLICY IF EXISTS "auth_view_ncert_catalog" ON public.ncert_catalog;
DROP POLICY IF EXISTS "anon_view_ncert_catalog" ON public.ncert_catalog;
DROP POLICY IF EXISTS "super_admin_manage_ncert_catalog" ON public.ncert_catalog;
DROP POLICY IF EXISTS "auth_insert_download_logs" ON public.download_logs;
DROP POLICY IF EXISTS "owner_view_download_logs" ON public.download_logs;
DROP POLICY IF EXISTS "owner_upload_study_materials_storage" ON storage.objects;
DROP POLICY IF EXISTS "owner_delete_study_materials_storage" ON storage.objects;
DROP POLICY IF EXISTS "student_download_study_materials_storage" ON storage.objects;

-- 9. RLS Policies: study_materials
CREATE POLICY "owner_full_study_materials" ON public.study_materials
    FOR ALL TO authenticated
    USING (public.is_library_owner() AND org_id = public.jwt_org_id())
    WITH CHECK (public.is_library_owner() AND org_id = public.jwt_org_id());

CREATE POLICY "student_select_study_materials" ON public.study_materials
    FOR SELECT TO authenticated
    USING (
        public.is_student()
        AND org_id = public.jwt_org_id()
        AND deleted_at IS NULL
        AND is_free = true
    );

CREATE POLICY "super_admin_select_study_materials" ON public.study_materials
    FOR SELECT TO authenticated
    USING (public.is_super_admin());

-- 10. RLS Policies: ncert_catalog
CREATE POLICY "auth_view_ncert_catalog" ON public.ncert_catalog
    FOR SELECT TO authenticated
    USING (is_active = true);

CREATE POLICY "anon_view_ncert_catalog" ON public.ncert_catalog
    FOR SELECT TO anon
    USING (is_active = true);

CREATE POLICY "super_admin_manage_ncert_catalog" ON public.ncert_catalog
    FOR ALL TO authenticated
    USING (public.is_super_admin())
    WITH CHECK (public.is_super_admin());

-- 11. RLS Policies: download_logs
CREATE POLICY "auth_insert_download_logs" ON public.download_logs
    FOR INSERT TO authenticated
    WITH CHECK (true);

CREATE POLICY "owner_view_download_logs" ON public.download_logs
    FOR SELECT TO authenticated
    USING (public.is_library_owner() OR public.is_super_admin());

-- 12. Storage Objects RLS Policies for study-materials bucket
CREATE POLICY "owner_upload_study_materials_storage" ON storage.objects
    FOR INSERT TO authenticated
    WITH CHECK (
        bucket_id = 'study-materials'
        AND split_part(name, '/', 1) = public.jwt_org_id()
        AND public.is_library_owner()
    );

CREATE POLICY "owner_delete_study_materials_storage" ON storage.objects
    FOR DELETE TO authenticated
    USING (
        bucket_id = 'study-materials'
        AND split_part(name, '/', 1) = public.jwt_org_id()
        AND public.is_library_owner()
    );

CREATE POLICY "student_download_study_materials_storage" ON storage.objects
    FOR SELECT TO authenticated
    USING (
        bucket_id = 'study-materials'
        AND split_part(name, '/', 1) = public.jwt_org_id()
        AND (public.is_student() OR public.is_library_owner())
    );

-- 13. Realtime Enablement
ALTER TABLE public.study_materials REPLICA IDENTITY FULL;
ALTER TABLE public.ncert_catalog REPLICA IDENTITY FULL;

DO $$
BEGIN
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.study_materials;
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.ncert_catalog;
    EXCEPTION WHEN duplicate_object THEN NULL;
    END;
END $$;

-- 14. Initial NCERT Catalog Seed
INSERT INTO public.ncert_catalog (class_level, subject, book_title, medium, language, edition_year, source_name, source_url, is_active)
VALUES
  (10, 'Science', 'Science - Class X', 'english', 'en', '2026-27', 'ncert', 'https://ncert.nic.in/textbook/pdf/jesc1dd.zip', true),
  (10, 'Mathematics', 'Mathematics - Class X', 'english', 'en', '2026-27', 'ncert', 'https://ncert.nic.in/textbook/pdf/jemh1dd.zip', true),
  (10, 'Mathematics', 'गणित - कक्षा 10', 'hindi', 'hi', '2026-27', 'ncert', 'https://ncert.nic.in/textbook/pdf/jhmh1dd.zip', true),
  (9, 'Science', 'Science - Class IX (NEP 2020 Revised)', 'english', 'en', '2026-27', 'ncert', 'https://ncert.nic.in/textbook/pdf/iesc1dd.zip', true),
  (9, 'Mathematics', 'Mathematics - Class IX (NEP 2020 Revised)', 'english', 'en', '2026-27', 'ncert', 'https://ncert.nic.in/textbook/pdf/iemh1dd.zip', true),
  (12, 'Physics', 'Physics Part - I', 'english', 'en', '2026-27', 'ncert', 'https://ncert.nic.in/textbook/pdf/leph1dd.zip', true)
ON CONFLICT DO NOTHING;
