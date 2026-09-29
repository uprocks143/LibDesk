-- =========================================================================
-- LibDesk Migration: Study Material Storage & NCERT Official Catalog
-- Migration: 20260925_study_materials_and_ncert.sql
-- Self-contained: includes hardened role helper functions & consolidated RLS policies
-- =========================================================================

-- 1. Helper Functions for JWT Custom Claims & Role Checking (InitPlan Optimized & Search-Path Hardened)
CREATE OR REPLACE FUNCTION public.jwt_role()
RETURNS text
LANGUAGE sql STABLE
SET search_path = public, pg_temp AS $$
  SELECT COALESCE(
    (SELECT auth.jwt())->>'role',
    (SELECT auth.jwt())->'app_metadata'->>'role',
    (SELECT auth.jwt())->'user_metadata'->>'role',
    'anon'
  );
$$;

CREATE OR REPLACE FUNCTION public.jwt_org_id()
RETURNS text
LANGUAGE sql STABLE
SET search_path = public, pg_temp AS $$
  SELECT COALESCE(
    (SELECT auth.jwt())->>'org_id',
    (SELECT auth.jwt())->'app_metadata'->>'org_id',
    (SELECT auth.jwt())->'app_metadata'->>'library_id',
    (SELECT auth.jwt())->'user_metadata'->>'org_id',
    (SELECT auth.jwt())->'user_metadata'->>'library_id',
    ''
  );
$$;

CREATE OR REPLACE FUNCTION public.jwt_student_id()
RETURNS text
LANGUAGE sql STABLE
SET search_path = public, pg_temp AS $$
  SELECT COALESCE(
    (SELECT auth.jwt())->>'student_id',
    (SELECT auth.jwt())->'app_metadata'->>'student_id',
    (SELECT auth.jwt())->'user_metadata'->>'student_id',
    ''
  );
$$;

CREATE OR REPLACE FUNCTION public.is_super_admin()
RETURNS boolean
LANGUAGE sql STABLE
SET search_path = public, pg_temp AS $$
  SELECT (SELECT public.jwt_role()) = 'SUPER_ADMIN';
$$;

CREATE OR REPLACE FUNCTION public.is_library_owner()
RETURNS boolean
LANGUAGE sql STABLE
SET search_path = public, pg_temp AS $$
  SELECT (SELECT public.jwt_role()) = 'OWNER';
$$;

CREATE OR REPLACE FUNCTION public.is_student()
RETURNS boolean
LANGUAGE sql STABLE
SET search_path = public, pg_temp AS $$
  SELECT (SELECT public.jwt_role()) = 'STUDENT';
$$;

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
RETURNS void LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp AS $$
BEGIN
    IF (SELECT auth.uid()) IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
    UPDATE public.study_materials
    SET download_count = download_count + 1,
        updated_at = NOW()
    WHERE id = p_material_id AND deleted_at IS NULL
      AND ((SELECT public.is_super_admin())
           OR (org_id = (SELECT public.jwt_org_id())
               AND ((SELECT public.is_library_owner()) OR ((SELECT public.is_student()) AND is_free = true))));
    IF NOT FOUND THEN RAISE EXCEPTION 'Material not found or access denied'; END IF;
END;
$$;

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

-- Clean drop of legacy / duplicate policies
DROP POLICY IF EXISTS "owner_full_study_materials" ON public.study_materials;
DROP POLICY IF EXISTS "student_select_study_materials" ON public.study_materials;
DROP POLICY IF EXISTS "super_admin_select_study_materials" ON public.study_materials;
DROP POLICY IF EXISTS "study_materials_select" ON public.study_materials;
DROP POLICY IF EXISTS "study_materials_insert" ON public.study_materials;
DROP POLICY IF EXISTS "study_materials_update" ON public.study_materials;
DROP POLICY IF EXISTS "study_materials_delete" ON public.study_materials;

DROP POLICY IF EXISTS "auth_view_ncert_catalog" ON public.ncert_catalog;
DROP POLICY IF EXISTS "anon_view_ncert_catalog" ON public.ncert_catalog;
DROP POLICY IF EXISTS "super_admin_manage_ncert_catalog" ON public.ncert_catalog;
DROP POLICY IF EXISTS "ncert_select" ON public.ncert_catalog;
DROP POLICY IF EXISTS "ncert_insert" ON public.ncert_catalog;
DROP POLICY IF EXISTS "ncert_update" ON public.ncert_catalog;
DROP POLICY IF EXISTS "ncert_delete" ON public.ncert_catalog;

DROP POLICY IF EXISTS "auth_insert_download_logs" ON public.download_logs;
DROP POLICY IF EXISTS "owner_view_download_logs" ON public.download_logs;
DROP POLICY IF EXISTS "download_logs_select" ON public.download_logs;
DROP POLICY IF EXISTS "download_logs_insert" ON public.download_logs;
DROP POLICY IF EXISTS "download_logs_update" ON public.download_logs;
DROP POLICY IF EXISTS "download_logs_delete" ON public.download_logs;

DROP POLICY IF EXISTS "owner_upload_study_materials_storage" ON storage.objects;
DROP POLICY IF EXISTS "owner_update_study_materials_storage" ON storage.objects;
DROP POLICY IF EXISTS "owner_delete_study_materials_storage" ON storage.objects;
DROP POLICY IF EXISTS "student_download_study_materials_storage" ON storage.objects;

-- 9. Consolidated RLS Policies: study_materials (1 policy per action)
CREATE POLICY "study_materials_select" ON public.study_materials FOR SELECT TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND org_id = (SELECT public.jwt_org_id()))
  OR ((SELECT public.is_student()) AND org_id = (SELECT public.jwt_org_id()) AND deleted_at IS NULL AND is_free = true)
);

CREATE POLICY "study_materials_insert" ON public.study_materials FOR INSERT TO authenticated
WITH CHECK (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND org_id = (SELECT public.jwt_org_id()))
);

CREATE POLICY "study_materials_update" ON public.study_materials FOR UPDATE TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND org_id = (SELECT public.jwt_org_id()))
)
WITH CHECK (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND org_id = (SELECT public.jwt_org_id()))
);

CREATE POLICY "study_materials_delete" ON public.study_materials FOR DELETE TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND org_id = (SELECT public.jwt_org_id()))
);

-- 10. Consolidated RLS Policies: ncert_catalog (1 policy per action)
CREATE POLICY "ncert_select" ON public.ncert_catalog FOR SELECT TO authenticated
USING (is_active = true OR (SELECT public.is_super_admin()));

CREATE POLICY "ncert_insert" ON public.ncert_catalog FOR INSERT TO authenticated
WITH CHECK ((SELECT public.is_super_admin()));

CREATE POLICY "ncert_update" ON public.ncert_catalog FOR UPDATE TO authenticated
USING ((SELECT public.is_super_admin()))
WITH CHECK ((SELECT public.is_super_admin()));

CREATE POLICY "ncert_delete" ON public.ncert_catalog FOR DELETE TO authenticated
USING ((SELECT public.is_super_admin()));

-- 11. Consolidated RLS Policies: download_logs (1 policy per action)
CREATE POLICY "download_logs_select" ON public.download_logs FOR SELECT TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND EXISTS (
    SELECT 1 FROM public.study_materials sm
    WHERE sm.id::text = material_id AND sm.org_id = (SELECT public.jwt_org_id())
  ))
);

CREATE POLICY "download_logs_insert" ON public.download_logs FOR INSERT TO authenticated
WITH CHECK (
  (SELECT public.is_student())
  AND student_id = (SELECT public.jwt_student_id())
  AND EXISTS (
    SELECT 1 FROM public.study_materials sm
    WHERE sm.id::text = material_id AND sm.org_id = (SELECT public.jwt_org_id())
      AND sm.deleted_at IS NULL AND sm.is_free = true
  )
);

CREATE POLICY "download_logs_update" ON public.download_logs FOR UPDATE TO authenticated
USING ((SELECT public.is_super_admin()))
WITH CHECK ((SELECT public.is_super_admin()));

CREATE POLICY "download_logs_delete" ON public.download_logs FOR DELETE TO authenticated
USING ((SELECT public.is_super_admin()));

-- 12. Storage Objects RLS Policies for study-materials bucket
CREATE POLICY "owner_upload_study_materials_storage" ON storage.objects
FOR INSERT TO authenticated
WITH CHECK (
  bucket_id = 'study-materials'
  AND split_part(name, '/', 1) = (SELECT public.jwt_org_id())
  AND (SELECT public.is_library_owner())
);

CREATE POLICY "owner_update_study_materials_storage" ON storage.objects
FOR UPDATE TO authenticated
USING (
  bucket_id = 'study-materials'
  AND split_part(name, '/', 1) = (SELECT public.jwt_org_id())
  AND (SELECT public.is_library_owner())
)
WITH CHECK (
  bucket_id = 'study-materials'
  AND split_part(name, '/', 1) = (SELECT public.jwt_org_id())
  AND (SELECT public.is_library_owner())
);

CREATE POLICY "owner_delete_study_materials_storage" ON storage.objects
FOR DELETE TO authenticated
USING (
  bucket_id = 'study-materials'
  AND split_part(name, '/', 1) = (SELECT public.jwt_org_id())
  AND (SELECT public.is_library_owner())
);

CREATE POLICY "student_download_study_materials_storage" ON storage.objects
FOR SELECT TO authenticated
USING (
  bucket_id = 'study-materials'
  AND (
    ((SELECT public.is_library_owner()) AND split_part(name, '/', 1) = (SELECT public.jwt_org_id()))
    OR
    ((SELECT public.is_student()) AND EXISTS(
      SELECT 1 FROM public.study_materials sm
      WHERE sm.org_id = (SELECT public.jwt_org_id()) AND sm.storage_path = name
        AND sm.deleted_at IS NULL AND sm.is_free = true
    ))
  )
);

-- 13. Realtime Enablement
ALTER TABLE public.study_materials REPLICA IDENTITY FULL;
ALTER TABLE public.ncert_catalog REPLICA IDENTITY FULL;
ALTER TABLE public.download_logs REPLICA IDENTITY FULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        CREATE PUBLICATION supabase_realtime;
    END IF;
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.study_materials;
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.ncert_catalog;
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
    BEGIN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.download_logs;
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
END $$ LANGUAGE plpgsql;

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
