
-- =========================================================================
-- LibDesk_Supabase_Production_Hardened.sql
-- ORDER-SAFE PRODUCTION-HARDENED RESET SCRIPT
-- PostgreSQL / Supabase
--
-- IMPORTANT:
--   * RESET script: drops/recreates the 29 public LibDesk tables below.
--   * Preserves Android/Kotlin table names and camelCase columns.
--   * Exactly 3 app roles: SUPER_ADMIN, OWNER, STUDENT.
--   * study_materials / ncert_catalog / download_logs are created BEFORE
--     any RLS, REPLICA IDENTITY, index, policy, grant, trigger or realtime
--     statement can reference them.
--   * Never deletes rows from storage.objects.
-- =========================================================================

BEGIN;

-- =========================================================================
-- 0. DROP FUNCTIONS / TRIGGERS THAT MAY REFERENCE THE TABLES
-- =========================================================================
DO $$
BEGIN
  DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
  DROP FUNCTION IF EXISTS public.handle_new_user() CASCADE;
  DROP FUNCTION IF EXISTS public.custom_access_token_hook(jsonb) CASCADE;
  DROP FUNCTION IF EXISTS public.increment_download_count(uuid) CASCADE;
  DROP FUNCTION IF EXISTS public.guard_owner_user_mutation() CASCADE;
  DROP FUNCTION IF EXISTS public.guard_owner_subscription_mutation() CASCADE;
  DROP FUNCTION IF EXISTS public.guard_owner_library_mutation() CASCADE;
  DROP FUNCTION IF EXISTS public.jwt_role() CASCADE;
  DROP FUNCTION IF EXISTS public.jwt_org_id() CASCADE;
  DROP FUNCTION IF EXISTS public.jwt_student_id() CASCADE;
  DROP FUNCTION IF EXISTS public.is_super_admin() CASCADE;
  DROP FUNCTION IF EXISTS public.is_library_owner() CASCADE;
  DROP FUNCTION IF EXISTS public.is_student() CASCADE;
EXCEPTION WHEN OTHERS THEN
  RAISE NOTICE 'Function cleanup notice: %', SQLERRM;
END $$;

-- =========================================================================
-- 1. DROP ALL 29 LIBDESK PUBLIC TABLES
--    Explicit order is used for clarity; CASCADE handles any remaining
--    dependent objects.
-- =========================================================================
DROP TABLE IF EXISTS public.download_logs CASCADE;
DROP TABLE IF EXISTS public.ncert_catalog CASCADE;
DROP TABLE IF EXISTS public.study_materials CASCADE;
DROP TABLE IF EXISTS public.user_booking_cache CASCADE;
DROP TABLE IF EXISTS public.super_admin_users CASCADE;
DROP TABLE IF EXISTS public.library_subscriptions CASCADE;
DROP TABLE IF EXISTS public.saas_plans CASCADE;
DROP TABLE IF EXISTS public.user_subscriptions CASCADE;
DROP TABLE IF EXISTS public.subscription_plans CASCADE;
DROP TABLE IF EXISTS public.audit_logs CASCADE;
DROP TABLE IF EXISTS public.feedback_complaints CASCADE;
DROP TABLE IF EXISTS public.notices CASCADE;
DROP TABLE IF EXISTS public.digital_materials CASCADE;
DROP TABLE IF EXISTS public.book_issues CASCADE;
DROP TABLE IF EXISTS public.physical_books CASCADE;
DROP TABLE IF EXISTS public.fines CASCADE;
DROP TABLE IF EXISTS public.expenses CASCADE;
DROP TABLE IF EXISTS public.payments CASCADE;
DROP TABLE IF EXISTS public.attendance CASCADE;
DROP TABLE IF EXISTS public.seat_assignments CASCADE;
DROP TABLE IF EXISTS public.seats CASCADE;
DROP TABLE IF EXISTS public.membership_plans CASCADE;
DROP TABLE IF EXISTS public.shifts CASCADE;
DROP TABLE IF EXISTS public.sections CASCADE;
DROP TABLE IF EXISTS public.cabins CASCADE;
DROP TABLE IF EXISTS public.halls CASCADE;
DROP TABLE IF EXISTS public.students CASCADE;
DROP TABLE IF EXISTS public.users CASCADE;
DROP TABLE IF EXISTS public.libraries CASCADE;

-- =========================================================================
-- 2. CREATE ALL TABLES FIRST (DEPENDENCY-SAFE PHASE)
-- =========================================================================

CREATE TABLE public.libraries (
    id TEXT PRIMARY KEY, name TEXT NOT NULL, code TEXT NOT NULL,
    "logoUrl" TEXT DEFAULT '', description TEXT DEFAULT '',
    "establishedDate" TEXT DEFAULT '', "regNumber" TEXT DEFAULT '',
    "ownerName" TEXT DEFAULT '', "ownerPhone" TEXT DEFAULT '',
    "ownerEmail" TEXT DEFAULT '', "ownerWhatsApp" TEXT DEFAULT '',
    "alternateContact" TEXT DEFAULT '', address TEXT DEFAULT '',
    landmark TEXT DEFAULT '', city TEXT DEFAULT '', district TEXT DEFAULT '',
    state TEXT DEFAULT '', pincode TEXT DEFAULT '',
    latitude DOUBLE PRECISION DEFAULT 0.0, longitude DOUBLE PRECISION DEFAULT 0.0,
    phone TEXT DEFAULT '', whatsapp TEXT DEFAULT '', email TEXT DEFAULT '',
    website TEXT DEFAULT '', "upiId" TEXT DEFAULT '', "upiPayeeName" TEXT DEFAULT '',
    "receiptPrefix" TEXT DEFAULT 'REC', "defaultFinePerDay" DOUBLE PRECISION DEFAULT 5.0,
    "borrowLimit" INT DEFAULT 2, "loanDays" INT DEFAULT 14,
    "qrAttendanceStrictShift" BOOLEAN DEFAULT FALSE,
    "subscription_active" BOOLEAN DEFAULT TRUE,
    "createdAt" BIGINT DEFAULT 0, "updatedAt" BIGINT DEFAULT 0
);

CREATE TABLE public.users (
    id TEXT PRIMARY KEY, email TEXT NOT NULL,
    role TEXT NOT NULL CHECK (role IN ('SUPER_ADMIN','OWNER','STUDENT')),
    "libraryId" TEXT DEFAULT '', name TEXT NOT NULL, phone TEXT DEFAULT '',
    "avatarUrl" TEXT DEFAULT '', "studentIdRef" TEXT,
    "isActive" BOOLEAN DEFAULT TRUE, "createdAt" BIGINT DEFAULT 0
);

CREATE TABLE public.students (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, "userId" TEXT DEFAULT '',
    "studentCode" TEXT NOT NULL, "fullName" TEXT NOT NULL, "photoUrl" TEXT DEFAULT '',
    mobile TEXT NOT NULL, email TEXT DEFAULT '', dob TEXT DEFAULT '',
    gender TEXT DEFAULT 'Other', address TEXT DEFAULT '', "parentName" TEXT DEFAULT '',
    "parentMobile" TEXT DEFAULT '', "courseClass" TEXT DEFAULT '', college TEXT DEFAULT '',
    "targetExam" TEXT DEFAULT 'General', category TEXT DEFAULT 'General',
    batch TEXT DEFAULT 'Morning Regular', "planId" TEXT DEFAULT '',
    "planName" TEXT DEFAULT 'Monthly', "shiftId" TEXT DEFAULT '',
    "shiftName" TEXT DEFAULT 'Full Day', "seatId" TEXT DEFAULT '',
    "seatNumber" TEXT DEFAULT '', "hallName" TEXT DEFAULT '',
    "joiningDate" TEXT DEFAULT '', "expiryDate" TEXT DEFAULT '',
    "totalFee" DOUBLE PRECISION DEFAULT 1000.0, discount DOUBLE PRECISION DEFAULT 0.0,
    "paidAmount" DOUBLE PRECISION DEFAULT 0.0, "dueAmount" DOUBLE PRECISION DEFAULT 0.0,
    status TEXT DEFAULT 'ACTIVE', "rfidQrCode" TEXT DEFAULT '',
    "emergencyContact" TEXT DEFAULT '', "createdAt" BIGINT DEFAULT 0
);

CREATE TABLE public.halls (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, name TEXT NOT NULL,
    type TEXT DEFAULT 'AC Hall', floor TEXT DEFAULT 'Ground Floor',
    "isAc" BOOLEAN DEFAULT TRUE, description TEXT DEFAULT '', "seatCount" INT DEFAULT 0,
    "openingTime" TEXT DEFAULT '06:00 AM', "closingTime" TEXT DEFAULT '11:00 PM',
    "isActive" BOOLEAN DEFAULT TRUE
);

CREATE TABLE public.cabins (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, "cabinNumber" TEXT NOT NULL,
    name TEXT NOT NULL, floor TEXT DEFAULT '1st Floor', "isAc" BOOLEAN DEFAULT TRUE,
    "isPrivate" BOOLEAN DEFAULT TRUE, "seatCount" INT DEFAULT 1,
    "monthlyFee" DOUBLE PRECISION DEFAULT 2500.0, description TEXT DEFAULT '',
    "isActive" BOOLEAN DEFAULT TRUE
);

CREATE TABLE public.sections (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, name TEXT NOT NULL,
    description TEXT DEFAULT '', floor TEXT DEFAULT 'Ground Floor',
    "hallId" TEXT DEFAULT '', "cabinId" TEXT DEFAULT '', "isActive" BOOLEAN DEFAULT TRUE
);

CREATE TABLE public.shifts (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, name TEXT NOT NULL,
    "startTime" TEXT NOT NULL, "endTime" TEXT NOT NULL, fee DOUBLE PRECISION DEFAULT 800.0,
    description TEXT DEFAULT '', "isActive" BOOLEAN DEFAULT TRUE
);

CREATE TABLE public.membership_plans (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, name TEXT NOT NULL,
    "durationMonths" INT DEFAULT 1, "durationDays" INT DEFAULT 30,
    "durationType" TEXT DEFAULT 'MONTHS', "baseFee" DOUBLE PRECISION DEFAULT 1000.0,
    "maintenanceFee" DOUBLE PRECISION DEFAULT 100.0, "securityDeposit" DOUBLE PRECISION DEFAULT 500.0,
    discount DOUBLE PRECISION DEFAULT 0.0, "seatType" TEXT DEFAULT 'Standard',
    "shiftId" TEXT DEFAULT '', facilities TEXT DEFAULT '', "renewalRules" TEXT DEFAULT '',
    "isActive" BOOLEAN DEFAULT TRUE
);

CREATE TABLE public.seats (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, "seatNumber" TEXT NOT NULL,
    "hallId" TEXT DEFAULT '', "hallName" TEXT DEFAULT '', "sectionId" TEXT DEFAULT '',
    "sectionName" TEXT DEFAULT '', "cabinId" TEXT DEFAULT '', "cabinName" TEXT DEFAULT '',
    floor TEXT DEFAULT 'Ground Floor', "seatType" TEXT DEFAULT 'Standard',
    "monthlyFee" DOUBLE PRECISION DEFAULT 1000.0, status TEXT DEFAULT 'AVAILABLE',
    "assignedStudentId" TEXT DEFAULT '', "assignedStudentName" TEXT DEFAULT '',
    "assignedShiftId" TEXT DEFAULT '', "assignedShiftName" TEXT DEFAULT '',
    "validUntil" TEXT DEFAULT '', "gridRow" INT DEFAULT 1, "gridCol" INT DEFAULT 1,
    "floorZone" TEXT DEFAULT 'General Study Zone'
);

CREATE TABLE public.seat_assignments (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, "seatId" TEXT NOT NULL,
    "seatNumber" TEXT NOT NULL, "studentId" TEXT NOT NULL, "studentName" TEXT NOT NULL,
    "shiftId" TEXT DEFAULT '', "shiftName" TEXT DEFAULT '', "startDate" TEXT DEFAULT '',
    "endDate" TEXT DEFAULT '', "planId" TEXT DEFAULT '', status TEXT DEFAULT 'ACTIVE',
    notes TEXT DEFAULT '', "createdAt" BIGINT DEFAULT 0
);

CREATE TABLE public.attendance (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, "studentId" TEXT NOT NULL,
    "studentName" TEXT NOT NULL, "seatId" TEXT DEFAULT '', "seatNumber" TEXT DEFAULT '',
    "hallId" TEXT DEFAULT '', "hallName" TEXT DEFAULT '', "shiftId" TEXT DEFAULT '',
    "shiftName" TEXT DEFAULT '', date TEXT NOT NULL, "checkInTime" TEXT NOT NULL,
    "checkOutTime" TEXT DEFAULT '', "durationMinutes" INT DEFAULT 0,
    status TEXT DEFAULT 'CHECKED_IN', mode TEXT DEFAULT 'QR_SCAN', notes TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT 0, timestamp BIGINT DEFAULT 0
);

CREATE TABLE public.payments (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, "receiptNumber" TEXT NOT NULL,
    "studentId" TEXT NOT NULL, "studentName" TEXT NOT NULL, amount DOUBLE PRECISION NOT NULL,
    "paymentMode" TEXT DEFAULT 'UPI', date TEXT NOT NULL, purpose TEXT DEFAULT 'MEMBERSHIP_FEE',
    "referenceNumber" TEXT DEFAULT '', notes TEXT DEFAULT '', remarks TEXT DEFAULT '',
    period TEXT DEFAULT '', "dueBalance" DOUBLE PRECISION DEFAULT 0.0, "createdAt" BIGINT DEFAULT 0
);

CREATE TABLE public.expenses (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, category TEXT NOT NULL,
    amount DOUBLE PRECISION NOT NULL, date TEXT NOT NULL, description TEXT DEFAULT '',
    "paymentMode" TEXT DEFAULT 'UPI', status TEXT DEFAULT 'PAID', "receiptRef" TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT 0
);

CREATE TABLE public.fines (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, "studentId" TEXT NOT NULL,
    "studentName" TEXT NOT NULL, "bookId" TEXT DEFAULT '', "bookTitle" TEXT DEFAULT '',
    reason TEXT DEFAULT 'Late Book Return', amount DOUBLE PRECISION DEFAULT 25.0,
    paid BOOLEAN DEFAULT FALSE, date TEXT DEFAULT '', "createdAt" BIGINT DEFAULT 0
);

CREATE TABLE public.physical_books (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, title TEXT NOT NULL, author TEXT NOT NULL,
    isbn TEXT DEFAULT '', publisher TEXT DEFAULT '', edition TEXT DEFAULT '',
    category TEXT DEFAULT 'Competitive Exams', subject TEXT DEFAULT 'General Studies',
    rack TEXT DEFAULT 'Rack A', shelf TEXT DEFAULT 'Shelf 2',
    "accessionNumber" TEXT DEFAULT 'ACC-001', "totalCopies" INT DEFAULT 1,
    "availableCopies" INT DEFAULT 1, "issuedCopies" INT DEFAULT 0,
    "coverUrl" TEXT DEFAULT '', "createdAt" BIGINT DEFAULT 0, "updatedAt" BIGINT DEFAULT 0
);

CREATE TABLE public.book_issues (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, "bookId" TEXT NOT NULL,
    "bookTitle" TEXT NOT NULL, "studentId" TEXT NOT NULL, "studentName" TEXT NOT NULL,
    "studentMobile" TEXT DEFAULT '', "issueDate" TEXT NOT NULL, "dueDate" TEXT NOT NULL,
    "returnDate" TEXT DEFAULT '', "fineAmount" DOUBLE PRECISION DEFAULT 0.0,
    "finePaid" BOOLEAN DEFAULT FALSE, status TEXT DEFAULT 'ISSUED',
    notes TEXT DEFAULT '', "createdAt" BIGINT DEFAULT 0
);

CREATE TABLE public.digital_materials (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, title TEXT NOT NULL,
    description TEXT DEFAULT '', category TEXT DEFAULT 'UPSC', subject TEXT DEFAULT 'General Studies',
    exam TEXT DEFAULT 'All Exams', "fileType" TEXT DEFAULT 'PDF', "fileSize" TEXT DEFAULT '4.2 MB',
    "fileUrl" TEXT DEFAULT '', "accessPolicy" TEXT DEFAULT 'ALL_STUDENTS',
    "allowedGroup" TEXT DEFAULT 'All', "downloadCount" INT DEFAULT 0,
    "uploadDate" TEXT DEFAULT '', "isBookmarked" BOOLEAN DEFAULT FALSE,
    "createdAt" BIGINT DEFAULT 0, "updatedAt" BIGINT DEFAULT 0
);

CREATE TABLE public.notices (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, title TEXT NOT NULL,
    content TEXT NOT NULL, category TEXT DEFAULT 'GENERAL', priority TEXT DEFAULT 'NORMAL',
    date TEXT NOT NULL, "targetAudience" TEXT DEFAULT 'ALL',
    "senderName" TEXT DEFAULT 'LibDesk Admin', "senderId" TEXT DEFAULT '',
    "isGlobal" BOOLEAN DEFAULT FALSE, "isActive" BOOLEAN DEFAULT TRUE,
    "createdAt" BIGINT DEFAULT 0, "updatedAt" BIGINT DEFAULT 0
);

CREATE TABLE public.feedback_complaints (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, "studentId" TEXT NOT NULL,
    "studentName" TEXT NOT NULL, email TEXT DEFAULT '', "seatId" TEXT DEFAULT '',
    "seatNumber" TEXT DEFAULT '', type TEXT DEFAULT 'COMPLAINT', subject TEXT NOT NULL,
    message TEXT NOT NULL, status TEXT DEFAULT 'PENDING', reply TEXT DEFAULT '',
    date TEXT NOT NULL, "resolvedDate" TEXT DEFAULT '', "createdAt" BIGINT DEFAULT 0
);

CREATE TABLE public.audit_logs (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, "userId" TEXT DEFAULT '',
    "userName" TEXT DEFAULT '', "performedBy" TEXT NOT NULL, role TEXT DEFAULT 'OWNER',
    action TEXT NOT NULL, "recordType" TEXT NOT NULL, "recordId" TEXT NOT NULL,
    details TEXT DEFAULT '', "createdAt" BIGINT DEFAULT 0, timestamp BIGINT DEFAULT 0
);

CREATE TABLE public.subscription_plans (
    id TEXT PRIMARY KEY, name TEXT NOT NULL, description TEXT DEFAULT '',
    price DOUBLE PRECISION NOT NULL, "durationMonths" INT DEFAULT 1, "durationDays" INT DEFAULT 30,
    "durationType" TEXT DEFAULT 'MONTHS', "maxSeats" INT DEFAULT 100, features TEXT DEFAULT '',
    badge TEXT DEFAULT '', "discountPercentage" DOUBLE PRECISION DEFAULT 0.0,
    "upiId" TEXT DEFAULT 'libdesk.billing@upi',
    "upiPayeeName" TEXT DEFAULT 'LibDesk Cloud Subscriptions',
    "supportWhatsApp" TEXT DEFAULT '', "isActive" BOOLEAN DEFAULT TRUE,
    "displayOrder" INT DEFAULT 1, "createdAt" BIGINT DEFAULT 0
);

CREATE TABLE public.user_subscriptions (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, "userId" TEXT DEFAULT '',
    "ownerName" TEXT DEFAULT '', "ownerMobile" TEXT DEFAULT '', "ownerEmail" TEXT DEFAULT '',
    "libraryName" TEXT DEFAULT '', "planId" TEXT NOT NULL, "planName" TEXT NOT NULL,
    "amountPaid" DOUBLE PRECISION NOT NULL, "billingCycle" TEXT DEFAULT 'MONTHLY',
    status TEXT DEFAULT 'ACTIVE', "startDate" TEXT DEFAULT '', "expiryDate" TEXT DEFAULT '',
    "paymentMethod" TEXT DEFAULT 'UPI_MANUAL', "paymentReferenceId" TEXT DEFAULT '',
    "receiptImageUrl" TEXT DEFAULT '', "isVerifiedByAdmin" BOOLEAN DEFAULT FALSE,
    notes TEXT DEFAULT '', "createdAt" BIGINT DEFAULT 0, "updatedAt" BIGINT DEFAULT 0
);

CREATE TABLE public.saas_plans (
    id TEXT PRIMARY KEY, name TEXT NOT NULL, "durationMonths" INT NOT NULL,
    price DOUBLE PRECISION NOT NULL, "maxSeats" INT NOT NULL, features TEXT DEFAULT '',
    "isActive" BOOLEAN DEFAULT TRUE, badge TEXT DEFAULT ''
);

CREATE TABLE public.library_subscriptions (
    id TEXT PRIMARY KEY, "libraryId" TEXT NOT NULL, "libraryName" TEXT NOT NULL,
    "planId" TEXT NOT NULL, "planName" TEXT NOT NULL, status TEXT DEFAULT 'ACTIVE',
    "subscription_active" BOOLEAN DEFAULT TRUE, "startDate" TEXT DEFAULT '',
    "expiryDate" TEXT DEFAULT '', price DOUBLE PRECISION NOT NULL, discount DOUBLE PRECISION DEFAULT 0.0,
    "maxSeats" INT DEFAULT 100, "autoRenew" BOOLEAN DEFAULT TRUE, notes TEXT DEFAULT '',
    "updatedAt" BIGINT DEFAULT 0, "durationDays" INT DEFAULT 30, "durationUnit" TEXT DEFAULT 'MONTHS'
);

CREATE TABLE public.super_admin_users (
    id TEXT PRIMARY KEY DEFAULT 'SUPER-ADMIN-MASTER', email TEXT NOT NULL, name TEXT NOT NULL,
    mobile TEXT DEFAULT '', phone TEXT DEFAULT '', role TEXT NOT NULL DEFAULT 'SUPER_ADMIN'
      CHECK (role = 'SUPER_ADMIN'), "accessCode" TEXT DEFAULT 'ADMIN99',
    "is2FaEnabled" BOOLEAN DEFAULT TRUE, "isClaimed" BOOLEAN DEFAULT FALSE,
    "upiId" TEXT DEFAULT 'libdesk.billing@upi',
    "upiPayeeName" TEXT DEFAULT 'LibDesk Cloud Subscriptions',
    "supportWhatsApp" TEXT DEFAULT '', "createdAt" BIGINT DEFAULT 0, "updatedAt" BIGINT DEFAULT 0
);

CREATE TABLE public.user_booking_cache (
    "studentId" TEXT PRIMARY KEY, "studentCode" TEXT DEFAULT '', "fullName" TEXT DEFAULT '',
    email TEXT DEFAULT '', phone TEXT DEFAULT '', "seatId" TEXT DEFAULT '',
    "seatNumber" TEXT DEFAULT '', "seatType" TEXT DEFAULT '', "hallId" TEXT DEFAULT '',
    "hallName" TEXT DEFAULT '', floor TEXT DEFAULT '', "shiftId" TEXT DEFAULT '',
    "shiftName" TEXT DEFAULT '', "shiftTimings" TEXT DEFAULT '', "planId" TEXT DEFAULT '',
    "planName" TEXT DEFAULT '', "membershipStatus" TEXT DEFAULT '', "startDate" TEXT DEFAULT '',
    "expiryDate" TEXT DEFAULT '', "rfidQrCode" TEXT DEFAULT '', "libraryId" TEXT DEFAULT '',
    "libraryName" TEXT DEFAULT '', "libraryAddress" TEXT DEFAULT '',
    "lastCheckInTime" TEXT DEFAULT '', "isCheckedIn" BOOLEAN DEFAULT FALSE,
    "hasAc" BOOLEAN DEFAULT TRUE, "hasPowerSocket" BOOLEAN DEFAULT TRUE,
    "hasReadingLamp" BOOLEAN DEFAULT TRUE, "hasLocker" BOOLEAN DEFAULT FALSE,
    "cachedTimestamp" BIGINT DEFAULT 0
);

-- CRITICAL ORDER FIX: these 3 tables are created HERE, before any reference to them.
CREATE TABLE public.study_materials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    category TEXT NOT NULL,
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

CREATE TABLE public.ncert_catalog (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_level INT NOT NULL CHECK (class_level BETWEEN 1 AND 12),
    subject TEXT NOT NULL,
    book_title TEXT NOT NULL,
    medium TEXT NOT NULL,
    language TEXT DEFAULT 'en',
    edition_year TEXT DEFAULT '2026-27',
    source_name TEXT NOT NULL DEFAULT 'ncert',
    source_url TEXT NOT NULL,
    thumbnail_url TEXT DEFAULT '',
    page_count INT DEFAULT 0,
    file_size_bytes BIGINT DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    last_verified TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE public.download_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id TEXT NOT NULL,
    material_id TEXT NOT NULL,
    source TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- =========================================================================
-- 3. JWT HELPER FUNCTIONS (SEARCH_PATH HARDENED & INITPLAN READY)
-- =========================================================================
CREATE OR REPLACE FUNCTION public.jwt_role() RETURNS text
LANGUAGE sql STABLE
SET search_path = public, pg_temp AS $$
  SELECT COALESCE(
    (SELECT auth.jwt())->>'role',
    (SELECT auth.jwt())->'app_metadata'->>'role',
    'anon'
  );
$$;

CREATE OR REPLACE FUNCTION public.jwt_org_id() RETURNS text
LANGUAGE sql STABLE
SET search_path = public, pg_temp AS $$
  SELECT COALESCE(
    (SELECT auth.jwt())->>'org_id',
    (SELECT auth.jwt())->'app_metadata'->>'org_id',
    (SELECT auth.jwt())->'app_metadata'->>'library_id',
    ''
  );
$$;

CREATE OR REPLACE FUNCTION public.jwt_student_id() RETURNS text
LANGUAGE sql STABLE
SET search_path = public, pg_temp AS $$
  SELECT COALESCE(
    (SELECT auth.jwt())->>'student_id',
    (SELECT auth.jwt())->'app_metadata'->>'student_id',
    ''
  );
$$;

CREATE OR REPLACE FUNCTION public.is_super_admin() RETURNS boolean
LANGUAGE sql STABLE
SET search_path = public, pg_temp AS $$
  SELECT (SELECT public.jwt_role()) = 'SUPER_ADMIN';
$$;

CREATE OR REPLACE FUNCTION public.is_library_owner() RETURNS boolean
LANGUAGE sql STABLE
SET search_path = public, pg_temp AS $$
  SELECT (SELECT public.jwt_role()) = 'OWNER';
$$;

CREATE OR REPLACE FUNCTION public.is_student() RETURNS boolean
LANGUAGE sql STABLE
SET search_path = public, pg_temp AS $$
  SELECT (SELECT public.jwt_role()) = 'STUDENT';
$$;

-- =========================================================================
-- 4. SECURITY-INTEGRITY GUARDS
-- =========================================================================
CREATE OR REPLACE FUNCTION public.guard_owner_user_mutation()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_catalog, pg_temp AS $$
BEGIN
  IF (SELECT public.is_library_owner()) THEN
    IF TG_OP = 'DELETE' THEN
      IF OLD.role <> 'STUDENT' OR OLD."libraryId" <> (SELECT public.jwt_org_id()) THEN
        RAISE EXCEPTION 'Owner may delete student accounts only in their own library';
      END IF;
      RETURN OLD;
    END IF;
    IF NEW."libraryId" <> (SELECT public.jwt_org_id()) THEN
      RAISE EXCEPTION 'Cross-tenant user mutation denied';
    END IF;
    IF NEW.id = (SELECT auth.uid())::text THEN
      IF NEW.role <> 'OWNER' THEN
        RAISE EXCEPTION 'Owner cannot change their own application role';
      END IF;
    ELSE
      IF NEW.role <> 'STUDENT' THEN
        RAISE EXCEPTION 'Owner cannot assign elevated roles';
      END IF;
    END IF;
  END IF;
  RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION public.guard_owner_library_mutation()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_catalog, pg_temp AS $$
BEGIN
  IF (SELECT public.is_library_owner()) AND NEW.id = OLD.id
     AND NEW."subscription_active" IS DISTINCT FROM OLD."subscription_active" THEN
    RAISE EXCEPTION 'Subscription status is controlled by the platform administrator';
  END IF;
  RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION public.guard_owner_subscription_mutation()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_catalog, pg_temp AS $$
BEGIN
  IF (SELECT public.is_library_owner()) THEN
    IF TG_OP = 'UPDATE' THEN
      IF NEW."isVerifiedByAdmin" IS DISTINCT FROM OLD."isVerifiedByAdmin"
         OR NEW.status IS DISTINCT FROM OLD.status THEN
        RAISE EXCEPTION 'Subscription verification/status is controlled by the platform administrator';
      END IF;
      IF NEW."libraryId" <> (SELECT public.jwt_org_id()) THEN
        RAISE EXCEPTION 'Cross-tenant subscription mutation denied';
      END IF;
    ELSIF TG_OP = 'INSERT' THEN
      IF NEW."libraryId" <> (SELECT public.jwt_org_id()) THEN
        RAISE EXCEPTION 'Cross-tenant subscription creation denied';
      END IF;
      IF NEW."isVerifiedByAdmin" IS TRUE THEN
        RAISE EXCEPTION 'Owner cannot create an already-verified subscription';
      END IF;
    END IF;
  END IF;
  RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION public.custom_access_token_hook(event jsonb)
RETURNS jsonb LANGUAGE plpgsql STABLE SECURITY DEFINER
SET search_path = public, pg_temp AS $$
DECLARE
  claims jsonb := event->'claims';
  user_role text;
  user_org_id text;
  user_student_id text;
BEGIN
  user_role := UPPER(TRIM(COALESCE(event->'user'->'app_metadata'->>'role','STUDENT')));
  IF user_role NOT IN ('SUPER_ADMIN','OWNER','STUDENT') THEN user_role := 'STUDENT'; END IF;
  user_org_id := COALESCE(event->'user'->'app_metadata'->>'org_id',
                          event->'user'->'app_metadata'->>'library_id','');
  user_student_id := COALESCE(event->'user'->'app_metadata'->>'student_id','');
  claims := jsonb_set(claims,'{role}',to_jsonb(user_role));
  claims := jsonb_set(claims,'{org_id}',to_jsonb(user_org_id));
  claims := jsonb_set(claims,'{student_id}',to_jsonb(user_student_id));
  RETURN jsonb_set(event,'{claims}',claims);
END $$;

-- =========================================================================
-- 5. RLS
-- =========================================================================
DO $$
DECLARE t text;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'libraries','users','students','halls','cabins','sections','shifts','membership_plans',
    'seats','seat_assignments','attendance','payments','expenses','fines','physical_books',
    'book_issues','digital_materials','notices','feedback_complaints','audit_logs',
    'subscription_plans','user_subscriptions','saas_plans','library_subscriptions',
    'super_admin_users','user_booking_cache','study_materials','ncert_catalog','download_logs'
  ] LOOP
    EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY',t);
  END LOOP;
END $$;

-- 5.1 Libraries (1 policy per action)
CREATE POLICY "libraries_select" ON public.libraries FOR SELECT TO authenticated
USING ((SELECT public.is_super_admin()) OR id = (SELECT public.jwt_org_id()));

CREATE POLICY "libraries_insert" ON public.libraries FOR INSERT TO authenticated
WITH CHECK ((SELECT public.is_super_admin()));

CREATE POLICY "libraries_update" ON public.libraries FOR UPDATE TO authenticated
USING ((SELECT public.is_super_admin()) OR ((SELECT public.is_library_owner()) AND id = (SELECT public.jwt_org_id())))
WITH CHECK ((SELECT public.is_super_admin()) OR ((SELECT public.is_library_owner()) AND id = (SELECT public.jwt_org_id())));

CREATE POLICY "libraries_delete" ON public.libraries FOR DELETE TO authenticated
USING ((SELECT public.is_super_admin()));

-- 5.2 Users (1 policy per action, InitPlan auth.uid())
CREATE POLICY "users_select" ON public.users FOR SELECT TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
  OR ((SELECT public.is_student()) AND id = (SELECT auth.uid())::text)
);

CREATE POLICY "users_insert" ON public.users FOR INSERT TO authenticated
WITH CHECK (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()) AND role = 'STUDENT')
);

CREATE POLICY "users_update" ON public.users FOR UPDATE TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id())
      AND (role = 'STUDENT' OR id = (SELECT auth.uid())::text))
)
WITH CHECK (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id())
      AND ((id = (SELECT auth.uid())::text AND role = 'OWNER') OR (id <> (SELECT auth.uid())::text AND role = 'STUDENT')))
);

CREATE POLICY "users_delete" ON public.users FOR DELETE TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()) AND role = 'STUDENT')
);

-- 5.3 Students (1 policy per action, InitPlan auth.uid())
CREATE POLICY "students_select" ON public.students FOR SELECT TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
  OR ((SELECT public.is_student()) AND "libraryId" = (SELECT public.jwt_org_id())
      AND (id = (SELECT public.jwt_student_id()) OR "userId" = (SELECT auth.uid())::text))
);

CREATE POLICY "students_insert" ON public.students FOR INSERT TO authenticated
WITH CHECK (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
);

CREATE POLICY "students_update" ON public.students FOR UPDATE TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
)
WITH CHECK (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
);

CREATE POLICY "students_delete" ON public.students FOR DELETE TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
);

-- 5.4 Library Infrastructure & Catalog (Owner Full CRUD, Student Read Entire Table for Library)
DO $$
DECLARE
  t text;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'halls','cabins','sections','shifts','membership_plans','seats',
    'physical_books','digital_materials'
  ] LOOP
    EXECUTE format('
      CREATE POLICY "%1$s_select" ON public.%1$I FOR SELECT TO authenticated
      USING (
        (SELECT public.is_super_admin())
        OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
        OR ((SELECT public.is_student()) AND "libraryId" = (SELECT public.jwt_org_id()))
      );
      CREATE POLICY "%1$s_insert" ON public.%1$I FOR INSERT TO authenticated
      WITH CHECK (
        (SELECT public.is_super_admin())
        OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
      );
      CREATE POLICY "%1$s_update" ON public.%1$I FOR UPDATE TO authenticated
      USING (
        (SELECT public.is_super_admin())
        OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
      )
      WITH CHECK (
        (SELECT public.is_super_admin())
        OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
      );
      CREATE POLICY "%1$s_delete" ON public.%1$I FOR DELETE TO authenticated
      USING (
        (SELECT public.is_super_admin())
        OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
      );
    ', t);
  END LOOP;
END $$;

-- 5.5 Student-Personal Tenant Tables (Owner Full CRUD, Student Read ONLY Own Records)
DO $$
DECLARE
  t text;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'seat_assignments','attendance','payments','fines','book_issues','user_booking_cache'
  ] LOOP
    EXECUTE format('
      CREATE POLICY "%1$s_select" ON public.%1$I FOR SELECT TO authenticated
      USING (
        (SELECT public.is_super_admin())
        OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
        OR ((SELECT public.is_student()) AND "libraryId" = (SELECT public.jwt_org_id()) AND "studentId" = (SELECT public.jwt_student_id()))
      );
      CREATE POLICY "%1$s_insert" ON public.%1$I FOR INSERT TO authenticated
      WITH CHECK (
        (SELECT public.is_super_admin())
        OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
      );
      CREATE POLICY "%1$s_update" ON public.%1$I FOR UPDATE TO authenticated
      USING (
        (SELECT public.is_super_admin())
        OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
      )
      WITH CHECK (
        (SELECT public.is_super_admin())
        OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
      );
      CREATE POLICY "%1$s_delete" ON public.%1$I FOR DELETE TO authenticated
      USING (
        (SELECT public.is_super_admin())
        OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
      );
    ', t);
  END LOOP;
END $$;

-- 5.6 Notices (Owner Full CRUD, Student Read Active Notices)
CREATE POLICY "notices_select" ON public.notices FOR SELECT TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
  OR ((SELECT public.is_student()) AND "libraryId" = (SELECT public.jwt_org_id()) AND "isActive" = true)
);
CREATE POLICY "notices_insert" ON public.notices FOR INSERT TO authenticated
WITH CHECK (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
);
CREATE POLICY "notices_update" ON public.notices FOR UPDATE TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
)
WITH CHECK (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
);
CREATE POLICY "notices_delete" ON public.notices FOR DELETE TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
);

-- 5.7 Feedback & Complaints (Owner Full CRUD, Student Read/Insert Own)
CREATE POLICY "feedback_select" ON public.feedback_complaints FOR SELECT TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
  OR ((SELECT public.is_student()) AND "libraryId" = (SELECT public.jwt_org_id()) AND "studentId" = (SELECT public.jwt_student_id()))
);
CREATE POLICY "feedback_insert" ON public.feedback_complaints FOR INSERT TO authenticated
WITH CHECK (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
  OR ((SELECT public.is_student()) AND "libraryId" = (SELECT public.jwt_org_id()) AND "studentId" = (SELECT public.jwt_student_id()))
);
CREATE POLICY "feedback_update" ON public.feedback_complaints FOR UPDATE TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
)
WITH CHECK (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
);
CREATE POLICY "feedback_delete" ON public.feedback_complaints FOR DELETE TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
);

-- 5.8 Expenses & Audit Logs
CREATE POLICY "expenses_select" ON public.expenses FOR SELECT TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
);
CREATE POLICY "expenses_insert" ON public.expenses FOR INSERT TO authenticated
WITH CHECK (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
);
CREATE POLICY "expenses_update" ON public.expenses FOR UPDATE TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
)
WITH CHECK (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
);
CREATE POLICY "expenses_delete" ON public.expenses FOR DELETE TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
);

CREATE POLICY "audit_select" ON public.audit_logs FOR SELECT TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
);
CREATE POLICY "audit_insert" ON public.audit_logs FOR INSERT TO authenticated
WITH CHECK (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
);
CREATE POLICY "audit_update" ON public.audit_logs FOR UPDATE TO authenticated
USING ((SELECT public.is_super_admin()))
WITH CHECK ((SELECT public.is_super_admin()));
CREATE POLICY "audit_delete" ON public.audit_logs FOR DELETE TO authenticated
USING ((SELECT public.is_super_admin()));

-- 5.9 Subscription & SaaS Plans (Anon and Auth read active, Admin manages)
CREATE POLICY "sub_plans_anon_select" ON public.subscription_plans FOR SELECT TO anon
USING ("isActive" = true);
CREATE POLICY "sub_plans_auth_select" ON public.subscription_plans FOR SELECT TO authenticated
USING ("isActive" = true OR (SELECT public.is_super_admin()));
CREATE POLICY "sub_plans_insert" ON public.subscription_plans FOR INSERT TO authenticated
WITH CHECK ((SELECT public.is_super_admin()));
CREATE POLICY "sub_plans_update" ON public.subscription_plans FOR UPDATE TO authenticated
USING ((SELECT public.is_super_admin()))
WITH CHECK ((SELECT public.is_super_admin()));
CREATE POLICY "sub_plans_delete" ON public.subscription_plans FOR DELETE TO authenticated
USING ((SELECT public.is_super_admin()));

CREATE POLICY "saas_plans_anon_select" ON public.saas_plans FOR SELECT TO anon
USING ("isActive" = true);
CREATE POLICY "saas_plans_auth_select" ON public.saas_plans FOR SELECT TO authenticated
USING ("isActive" = true OR (SELECT public.is_super_admin()));
CREATE POLICY "saas_plans_insert" ON public.saas_plans FOR INSERT TO authenticated
WITH CHECK ((SELECT public.is_super_admin()));
CREATE POLICY "saas_plans_update" ON public.saas_plans FOR UPDATE TO authenticated
USING ((SELECT public.is_super_admin()))
WITH CHECK ((SELECT public.is_super_admin()));
CREATE POLICY "saas_plans_delete" ON public.saas_plans FOR DELETE TO authenticated
USING ((SELECT public.is_super_admin()));

-- 5.10 Subscriptions (Library & User)
CREATE POLICY "lib_subs_select" ON public.library_subscriptions FOR SELECT TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
);
CREATE POLICY "lib_subs_insert" ON public.library_subscriptions FOR INSERT TO authenticated
WITH CHECK ((SELECT public.is_super_admin()));
CREATE POLICY "lib_subs_update" ON public.library_subscriptions FOR UPDATE TO authenticated
USING ((SELECT public.is_super_admin()))
WITH CHECK ((SELECT public.is_super_admin()));
CREATE POLICY "lib_subs_delete" ON public.library_subscriptions FOR DELETE TO authenticated
USING ((SELECT public.is_super_admin()));

CREATE POLICY "user_subs_select" ON public.user_subscriptions FOR SELECT TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()))
);
CREATE POLICY "user_subs_insert" ON public.user_subscriptions FOR INSERT TO authenticated
WITH CHECK (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()) AND "isVerifiedByAdmin" = false)
);
CREATE POLICY "user_subs_update" ON public.user_subscriptions FOR UPDATE TO authenticated
USING (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()) AND "isVerifiedByAdmin" = false)
)
WITH CHECK (
  (SELECT public.is_super_admin())
  OR ((SELECT public.is_library_owner()) AND "libraryId" = (SELECT public.jwt_org_id()) AND "isVerifiedByAdmin" = false)
);
CREATE POLICY "user_subs_delete" ON public.user_subscriptions FOR DELETE TO authenticated
USING ((SELECT public.is_super_admin()));

-- 5.11 Super Admin Users
CREATE POLICY "super_admin_users_select" ON public.super_admin_users FOR SELECT TO authenticated
USING ((SELECT public.is_super_admin()));
CREATE POLICY "super_admin_users_insert" ON public.super_admin_users FOR INSERT TO authenticated
WITH CHECK ((SELECT public.is_super_admin()));
CREATE POLICY "super_admin_users_update" ON public.super_admin_users FOR UPDATE TO authenticated
USING ((SELECT public.is_super_admin()))
WITH CHECK ((SELECT public.is_super_admin()));
CREATE POLICY "super_admin_users_delete" ON public.super_admin_users FOR DELETE TO authenticated
USING ((SELECT public.is_super_admin()));

-- 5.12 Study Materials & NCERT
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

CREATE POLICY "ncert_select" ON public.ncert_catalog FOR SELECT TO authenticated
USING (is_active = true OR (SELECT public.is_super_admin()));
CREATE POLICY "ncert_insert" ON public.ncert_catalog FOR INSERT TO authenticated
WITH CHECK ((SELECT public.is_super_admin()));
CREATE POLICY "ncert_update" ON public.ncert_catalog FOR UPDATE TO authenticated
USING ((SELECT public.is_super_admin()))
WITH CHECK ((SELECT public.is_super_admin()));
CREATE POLICY "ncert_delete" ON public.ncert_catalog FOR DELETE TO authenticated
USING ((SELECT public.is_super_admin()));

-- 5.13 Download Logs
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

-- =========================================================================
-- 6. TRIGGERS / AUTH
-- =========================================================================
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger LANGUAGE plpgsql SECURITY DEFINER
SET search_path=public,pg_temp AS $$
DECLARE
  v_role text;
  v_org_id text;
  v_name text;
  v_phone text;
BEGIN
  v_role:=UPPER(TRIM(COALESCE(new.raw_app_meta_data->>'role','STUDENT')));
  IF v_role NOT IN ('SUPER_ADMIN','OWNER','STUDENT') THEN v_role:='STUDENT'; END IF;
  v_org_id:=COALESCE(new.raw_app_meta_data->>'org_id',
                    new.raw_app_meta_data->>'library_id',
                    new.raw_app_meta_data->>'libraryId','');
  IF v_role='OWNER' AND v_org_id='' THEN
    SELECT id INTO v_org_id FROM public.libraries
    WHERE lower(COALESCE("ownerEmail",''))=lower(COALESCE(new.email,''))
    ORDER BY "createdAt" DESC LIMIT 1;
  ELSIF v_role='STUDENT' AND v_org_id='' THEN
    SELECT "libraryId" INTO v_org_id FROM public.students
    WHERE lower(COALESCE(email,''))=lower(COALESCE(new.email,''))
    ORDER BY "createdAt" DESC LIMIT 1;
  END IF;
  v_org_id:=COALESCE(v_org_id,'');
  v_name:=COALESCE(new.raw_user_meta_data->>'full_name',
                   new.raw_user_meta_data->>'name',
                   split_part(COALESCE(new.email,''),'@',1),'LibDesk User');
  v_phone:=COALESCE(new.raw_user_meta_data->>'phone',
                    new.raw_user_meta_data->>'mobile',
                    new.raw_app_meta_data->>'phone',
                    new.raw_app_meta_data->>'mobile','');
  INSERT INTO public.users(id,email,name,role,"libraryId",phone,"isActive","createdAt")
  VALUES(new.id::text,COALESCE(new.email,''),v_name,v_role,v_org_id,v_phone,true,
         (extract(epoch from now())*1000)::bigint)
  ON CONFLICT(id) DO UPDATE SET
    email=EXCLUDED.email,
    name=CASE WHEN EXCLUDED.name<>'' THEN EXCLUDED.name ELSE public.users.name END,
    role=EXCLUDED.role,
    "libraryId"=CASE WHEN EXCLUDED."libraryId"<>'' THEN EXCLUDED."libraryId" ELSE public.users."libraryId" END,
    phone=CASE WHEN EXCLUDED.phone<>'' THEN EXCLUDED.phone ELSE public.users.phone END;
  RETURN new;
END $$;

CREATE TRIGGER on_auth_user_created AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

CREATE TRIGGER trg_guard_owner_user_mutation
BEFORE INSERT OR UPDATE OR DELETE ON public.users
FOR EACH ROW EXECUTE FUNCTION public.guard_owner_user_mutation();

CREATE TRIGGER trg_guard_owner_library_mutation
BEFORE UPDATE ON public.libraries
FOR EACH ROW EXECUTE FUNCTION public.guard_owner_library_mutation();

CREATE TRIGGER trg_guard_owner_subscription_mutation
BEFORE INSERT OR UPDATE ON public.user_subscriptions
FOR EACH ROW EXECUTE FUNCTION public.guard_owner_subscription_mutation();

-- =========================================================================
-- 7. STORAGE BUCKET + STORAGE POLICIES
-- =========================================================================
INSERT INTO storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
VALUES('study-materials','study-materials',false,20971520,ARRAY['application/pdf']::text[])
ON CONFLICT(id) DO UPDATE SET public=false,file_size_limit=20971520,
allowed_mime_types=ARRAY['application/pdf']::text[];

DROP POLICY IF EXISTS "owner_upload_study_materials_storage" ON storage.objects;
DROP POLICY IF EXISTS "owner_update_study_materials_storage" ON storage.objects;
DROP POLICY IF EXISTS "owner_delete_study_materials_storage" ON storage.objects;
DROP POLICY IF EXISTS "student_download_study_materials_storage" ON storage.objects;

CREATE POLICY "owner_upload_study_materials_storage" ON storage.objects
FOR INSERT TO authenticated
WITH CHECK(
  bucket_id = 'study-materials'
  AND split_part(name, '/', 1) = (SELECT public.jwt_org_id())
  AND (SELECT public.is_library_owner())
);

CREATE POLICY "owner_update_study_materials_storage" ON storage.objects
FOR UPDATE TO authenticated
USING(
  bucket_id = 'study-materials'
  AND split_part(name, '/', 1) = (SELECT public.jwt_org_id())
  AND (SELECT public.is_library_owner())
)
WITH CHECK(
  bucket_id = 'study-materials'
  AND split_part(name, '/', 1) = (SELECT public.jwt_org_id())
  AND (SELECT public.is_library_owner())
);

CREATE POLICY "owner_delete_study_materials_storage" ON storage.objects
FOR DELETE TO authenticated
USING(
  bucket_id = 'study-materials'
  AND split_part(name, '/', 1) = (SELECT public.jwt_org_id())
  AND (SELECT public.is_library_owner())
);

CREATE POLICY "student_download_study_materials_storage" ON storage.objects
FOR SELECT TO authenticated
USING(
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

-- =========================================================================
-- 8. RPC / INDEXES
-- =========================================================================
CREATE OR REPLACE FUNCTION public.increment_download_count(p_material_id UUID)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp AS $$
BEGIN
  IF (SELECT auth.uid()) IS NULL THEN RAISE EXCEPTION 'Authentication required'; END IF;
  UPDATE public.study_materials
  SET download_count = download_count + 1, updated_at = now()
  WHERE id = p_material_id AND deleted_at IS NULL
    AND ((SELECT public.is_super_admin())
         OR (org_id = (SELECT public.jwt_org_id())
             AND ((SELECT public.is_library_owner()) OR ((SELECT public.is_student()) AND is_free = true))));
  IF NOT FOUND THEN RAISE EXCEPTION 'Material not found or access denied'; END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_users_email ON public.users(email);
CREATE INDEX IF NOT EXISTS idx_users_library_id ON public.users("libraryId");
CREATE INDEX IF NOT EXISTS idx_students_library_id ON public.students("libraryId");
CREATE INDEX IF NOT EXISTS idx_students_user_id ON public.students("userId");
CREATE INDEX IF NOT EXISTS idx_students_mobile ON public.students(mobile);
CREATE INDEX IF NOT EXISTS idx_students_status ON public.students(status);
CREATE INDEX IF NOT EXISTS idx_seats_library_id ON public.seats("libraryId");
CREATE INDEX IF NOT EXISTS idx_seats_status ON public.seats(status);
CREATE INDEX IF NOT EXISTS idx_attendance_library_date ON public.attendance("libraryId",date);
CREATE INDEX IF NOT EXISTS idx_attendance_student_id ON public.attendance("studentId");
CREATE INDEX IF NOT EXISTS idx_payments_library_id ON public.payments("libraryId");
CREATE INDEX IF NOT EXISTS idx_payments_student_id ON public.payments("studentId");
CREATE INDEX IF NOT EXISTS idx_expenses_library_id ON public.expenses("libraryId");
CREATE INDEX IF NOT EXISTS idx_fines_library_id ON public.fines("libraryId");
CREATE INDEX IF NOT EXISTS idx_book_issues_library_id ON public.book_issues("libraryId");
CREATE INDEX IF NOT EXISTS idx_book_issues_student_id ON public.book_issues("studentId");
CREATE INDEX IF NOT EXISTS idx_seat_assignments_library ON public.seat_assignments("libraryId");
CREATE INDEX IF NOT EXISTS idx_seat_assignments_seat ON public.seat_assignments("seatId");
CREATE INDEX IF NOT EXISTS idx_notices_library_id ON public.notices("libraryId");
CREATE INDEX IF NOT EXISTS idx_feedback_library_id ON public.feedback_complaints("libraryId");
CREATE INDEX IF NOT EXISTS idx_sub_plans_active ON public.subscription_plans("isActive");
CREATE INDEX IF NOT EXISTS idx_user_sub_library ON public.user_subscriptions("libraryId");
CREATE INDEX IF NOT EXISTS idx_study_materials_org_cat ON public.study_materials(org_id,category);
CREATE INDEX IF NOT EXISTS idx_study_materials_deleted ON public.study_materials(deleted_at);
CREATE INDEX IF NOT EXISTS idx_ncert_class_subj_med ON public.ncert_catalog(class_level,subject,medium);
CREATE INDEX IF NOT EXISTS idx_ncert_is_active ON public.ncert_catalog(is_active);

-- =========================================================================
-- 9. SEED DATA
-- =========================================================================
INSERT INTO public.super_admin_users
(id,email,name,mobile,phone,role,"accessCode","is2FaEnabled","isClaimed",
 "upiId","upiPayeeName","supportWhatsApp","createdAt","updatedAt")
VALUES('SUPER-ADMIN-MASTER','CHANGE-THIS-EMAIL-IN-ADMIN-PANEL','Platform Owner','','',
       'SUPER_ADMIN','',true,false,'libdesk.billing@upi','LibDesk Cloud Subscriptions','',
       1700000000000,1700000000000)
ON CONFLICT(id) DO NOTHING;

INSERT INTO public.subscription_plans
(id,name,description,price,"durationMonths","durationDays","durationType","maxSeats",features,
 badge,"discountPercentage","upiId","upiPayeeName","supportWhatsApp","isActive","displayOrder","createdAt")
VALUES
('SUB-PLAN-STARTER','Starter Launch','Essential digital library suite for small halls & study rooms',499,1,30,'MONTHS',60,
 'Up to 60 Dedicated Seats; Smart Gate QR Code Attendance; Cash & UPI Fee Ledger; Real-time Student Directory; Digital Notice Board Broadcast; Instant Setup in 2 Minutes',
 'Starter Pack',0,'libdesk.billing@upi','LibDesk Cloud Subscriptions','',true,1,1700000000000),
('SUB-PLAN-PRO','Growth Pro','Most popular choice for growing libraries with multiple shifts',999,1,30,'MONTHS',160,
 'Up to 160 Dedicated & Flexible Seats; 3 Shifts Support; Direct WhatsApp Fee Slips & Reminders; Student Self-Service Portal; Digital E-Book Catalog & Issues; Full Daily P&L Expense Tracking; Cloud-Synchronized Multi-Tenant Security',
 'Most Popular',15,'libdesk.billing@upi','LibDesk Cloud Subscriptions','',true,2,1700000000000),
('SUB-PLAN-ENTERPRISE','Enterprise Annual','Maximum scale with unlimited seats, custom branding & VIP support',7999,12,365,'MONTHS',9999,
 'Unlimited Seats & Multi-Halls; Custom UPI QR Code; 2 Months Free on Annual Billing; Automated Cloud Sync & Backup; Priority WhatsApp VIP Support; Biometric & RFID Turnstile Ready; Advanced Monthly Financial Reports',
 'Best Value (Save 35%)',35,'libdesk.billing@upi','LibDesk Cloud Subscriptions','',true,3,1700000000000)
ON CONFLICT(id) DO NOTHING;

-- A compact official NCERT catalog seed. Additional rows can be inserted safely later.
INSERT INTO public.ncert_catalog
(class_level,subject,book_title,medium,language,edition_year,source_name,source_url,is_active)
VALUES
(1,'Mathematics','Joyful Mathematics – Class 1','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/aejm101.pdf',true),
(1,'Mathematics','आनन्दमय गणित – Class 1','hindi','hi','2026-27','ncert','https://ncert.nic.in/textbook/pdf/ahjm101.pdf',true),
(1,'English','Mridang English – Class 1','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/aeen101.pdf',true),
(1,'Hindi','सारंगी (Sarangi Hindi) – Class 1','hindi','hi','2026-27','ncert','https://ncert.nic.in/textbook/pdf/ahhn101.pdf',true),
(2,'Mathematics','Joyful Mathematics – Class 2','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/bejm101.pdf',true),
(2,'English','Mridang English – Class 2','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/been101.pdf',true),
(2,'Hindi','सारंगी (Sarangi Hindi) – Class 2','hindi','hi','2026-27','ncert','https://ncert.nic.in/textbook/pdf/bhhn101.pdf',true),
(3,'Mathematics','Math-Magic – Class 3','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/cemh101.pdf',true),
(3,'EVS','Looking Around (EVS) – Class 3','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/ceap101.pdf',true),
(3,'English','Santoor English – Class 3','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/ceen101.pdf',true),
(4,'Mathematics','Math-Magic – Class 4','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/demh101.pdf',true),
(4,'EVS','Looking Around (EVS) – Class 4','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/deap101.pdf',true),
(5,'Mathematics','Math-Magic – Class 5','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/eemh101.pdf',true),
(5,'EVS','Looking Around (EVS) – Class 5','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/eeap101.pdf',true),
(6,'Science','Curiosity Science – Class 6','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/fecu101.pdf',true),
(6,'Mathematics','Ganita Prakash Mathematics – Class 6','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/femh101.pdf',true),
(7,'Science','Science – Class 7','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/gesc101.pdf',true),
(7,'Mathematics','Mathematics – Class 7','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/gemh101.pdf',true),
(8,'Science','Science – Class 8','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/hesc101.pdf',true),
(8,'Mathematics','Mathematics – Class 8','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/hemh101.pdf',true),
(9,'Science','Science – Class 9','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/iesc101.pdf',true),
(9,'Mathematics','Mathematics – Class 9','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/iemh101.pdf',true),
(10,'Science','Science – Class 10 (Board Exam)','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/jesc101.pdf',true),
(10,'Mathematics','Mathematics – Class 10 (Board Exam)','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/jemh101.pdf',true),
(11,'Physics','Physics Part 1 – Class 11','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/keph101.pdf',true),
(11,'Chemistry','Chemistry Part 1 – Class 11','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/kech101.pdf',true),
(11,'Biology','Biology – Class 11','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/kebo101.pdf',true),
(11,'Mathematics','Mathematics – Class 11','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/kemh101.pdf',true),
(12,'Physics','Physics Part 1 – Class 12','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/leph101.pdf',true),
(12,'Chemistry','Chemistry Part 1 – Class 12','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/lech101.pdf',true),
(12,'Biology','Biology – Class 12','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/lebo101.pdf',true),
(12,'Mathematics','Mathematics Part 1 – Class 12','english','en','2026-27','ncert','https://ncert.nic.in/textbook/pdf/lemh101.pdf',true)
ON CONFLICT DO NOTHING;

-- =========================================================================
-- 10. REPLICA IDENTITY — ALL 29 TABLES NOW EXIST
-- =========================================================================
DO $$
DECLARE t text;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'libraries','users','students','halls','cabins','sections','shifts','membership_plans',
    'seats','seat_assignments','attendance','payments','expenses','fines','physical_books',
    'book_issues','digital_materials','notices','feedback_complaints','audit_logs',
    'subscription_plans','user_subscriptions','saas_plans','library_subscriptions',
    'super_admin_users','user_booking_cache','study_materials','ncert_catalog','download_logs'
  ] LOOP
    EXECUTE format('ALTER TABLE public.%I REPLICA IDENTITY FULL',t);
  END LOOP;
END $$;

-- =========================================================================
-- 11. PRIVILEGE HARDENING
-- =========================================================================
DO $$
DECLARE t text;
BEGIN
  FOREACH t IN ARRAY ARRAY[
    'libraries','users','students','halls','cabins','sections','shifts','membership_plans',
    'seats','seat_assignments','attendance','payments','expenses','fines','physical_books',
    'book_issues','digital_materials','notices','feedback_complaints','audit_logs',
    'subscription_plans','user_subscriptions','saas_plans','library_subscriptions',
    'super_admin_users','user_booking_cache','study_materials','ncert_catalog','download_logs'
  ] LOOP
    EXECUTE format('REVOKE ALL ON TABLE public.%I FROM anon',t);
    EXECUTE format('GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE public.%I TO authenticated',t);
  END LOOP;
END $$;

GRANT SELECT ON public.subscription_plans,public.saas_plans TO anon,authenticated;

REVOKE ALL ON TABLE storage.buckets FROM anon;
REVOKE ALL ON TABLE storage.objects FROM anon;

REVOKE ALL ON FUNCTION public.jwt_role() FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.jwt_org_id() FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.jwt_student_id() FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.is_super_admin() FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.is_library_owner() FROM PUBLIC,anon;
REVOKE ALL ON FUNCTION public.is_student() FROM PUBLIC,anon;

GRANT EXECUTE ON FUNCTION public.jwt_role() TO authenticated;
GRANT EXECUTE ON FUNCTION public.jwt_org_id() TO authenticated;
GRANT EXECUTE ON FUNCTION public.jwt_student_id() TO authenticated;
GRANT EXECUTE ON FUNCTION public.is_super_admin() TO authenticated;
GRANT EXECUTE ON FUNCTION public.is_library_owner() TO authenticated;
GRANT EXECUTE ON FUNCTION public.is_student() TO authenticated;

REVOKE ALL ON FUNCTION public.custom_access_token_hook(jsonb) FROM PUBLIC,anon,authenticated;
GRANT EXECUTE ON FUNCTION public.custom_access_token_hook(jsonb) TO supabase_auth_admin;
REVOKE ALL ON FUNCTION public.handle_new_user() FROM PUBLIC,anon,authenticated;
REVOKE ALL ON FUNCTION public.increment_download_count(uuid) FROM PUBLIC,anon;
GRANT EXECUTE ON FUNCTION public.increment_download_count(uuid) TO authenticated;
REVOKE ALL ON FUNCTION public.guard_owner_user_mutation() FROM PUBLIC,anon,authenticated;
REVOKE ALL ON FUNCTION public.guard_owner_library_mutation() FROM PUBLIC,anon,authenticated;
REVOKE ALL ON FUNCTION public.guard_owner_subscription_mutation() FROM PUBLIC,anon,authenticated;

REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- =========================================================================
-- 12. REALTIME — LAST, AFTER ALL 29 TABLES EXIST
-- =========================================================================
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_publication WHERE pubname='supabase_realtime') THEN
    ALTER PUBLICATION supabase_realtime SET TABLE
      public.libraries,public.users,public.students,public.halls,public.cabins,
      public.sections,public.shifts,public.membership_plans,public.seats,
      public.seat_assignments,public.attendance,public.payments,public.expenses,
      public.fines,public.physical_books,public.book_issues,public.digital_materials,
      public.notices,public.feedback_complaints,public.audit_logs,public.subscription_plans,
      public.user_subscriptions,public.saas_plans,public.library_subscriptions,
      public.super_admin_users,public.user_booking_cache,public.study_materials,
      public.ncert_catalog,public.download_logs;
  ELSE
    CREATE PUBLICATION supabase_realtime FOR TABLE
      public.libraries,public.users,public.students,public.halls,public.cabins,
      public.sections,public.shifts,public.membership_plans,public.seats,
      public.seat_assignments,public.attendance,public.payments,public.expenses,
      public.fines,public.physical_books,public.book_issues,public.digital_materials,
      public.notices,public.feedback_complaints,public.audit_logs,public.subscription_plans,
      public.user_subscriptions,public.saas_plans,public.library_subscriptions,
      public.super_admin_users,public.user_booking_cache,public.study_materials,
      public.ncert_catalog,public.download_logs;
  END IF;
END $$;

COMMIT;

-- =========================================================================
-- 13. POST-DEPLOYMENT CHECKS
-- =========================================================================
-- SELECT count(*) AS public_table_count
-- FROM information_schema.tables
-- WHERE table_schema='public' AND table_name IN (
-- 'libraries','users','students','halls','cabins','sections','shifts','membership_plans',
-- 'seats','seat_assignments','attendance','payments','expenses','fines','physical_books',
-- 'book_issues','digital_materials','notices','feedback_complaints','audit_logs',
-- 'subscription_plans','user_subscriptions','saas_plans','library_subscriptions',
-- 'super_admin_users','user_booking_cache','study_materials','ncert_catalog','download_logs');
--
-- SELECT tablename,rowsecurity FROM pg_tables
-- WHERE schemaname='public' ORDER BY tablename;
--
-- IMPORTANT: register public.custom_access_token_hook(jsonb) in
-- Supabase Authentication -> Hooks as the Custom Access Token hook.
-- Do not put service_role/database secrets in the Android APK.
-- Storage object deletion must use the Supabase Storage API.
-- =========================================================================
-- END
-- =========================================================================
