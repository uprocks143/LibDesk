-- =========================================================================
-- LibDesk Supabase PostgreSQL Database Reset & Complete Schema Script
-- Run this in Supabase Dashboard -> SQL Editor
-- This will DROP all existing tables and data, then CREATE fresh tables with:
--  1. Exact camelCase column names matching LibDesk Android App Models
--  2. Full Row Level Security (RLS) policies for Anon & Authenticated access
--  3. Realtime Replication & Publication on all tables
--  4. Automatic Supabase Auth trigger (syncs auth.users -> public.users)
--  5. Performance Indexes
--  6. Seed data for Super Admin & Subscription Plans
-- =========================================================================

-- 1. DROP ALL EXISTING TABLES & OBJECTS (CASCADE handles dependencies)
DROP TABLE IF EXISTS public.study_materials CASCADE;
DROP TABLE IF EXISTS public.ncert_catalog CASCADE;
DROP TABLE IF EXISTS public.download_logs CASCADE;
DROP TABLE IF EXISTS public.attendance CASCADE;
DROP TABLE IF EXISTS public.payments CASCADE;
DROP TABLE IF EXISTS public.seat_assignments CASCADE;
DROP TABLE IF EXISTS public.book_issues CASCADE;
DROP TABLE IF EXISTS public.physical_books CASCADE;
DROP TABLE IF EXISTS public.digital_materials CASCADE;
DROP TABLE IF EXISTS public.expenses CASCADE;
DROP TABLE IF EXISTS public.fines CASCADE;
DROP TABLE IF EXISTS public.notices CASCADE;
DROP TABLE IF EXISTS public.feedback_complaints CASCADE;
DROP TABLE IF EXISTS public.audit_logs CASCADE;
DROP TABLE IF EXISTS public.user_booking_cache CASCADE;
DROP TABLE IF EXISTS public.seats CASCADE;
DROP TABLE IF EXISTS public.students CASCADE;
DROP TABLE IF EXISTS public.membership_plans CASCADE;
DROP TABLE IF EXISTS public.shifts CASCADE;
DROP TABLE IF EXISTS public.sections CASCADE;
DROP TABLE IF EXISTS public.cabins CASCADE;
DROP TABLE IF EXISTS public.halls CASCADE;
DROP TABLE IF EXISTS public.user_subscriptions CASCADE;
DROP TABLE IF EXISTS public.subscription_plans CASCADE;
DROP TABLE IF EXISTS public.library_subscriptions CASCADE;
DROP TABLE IF EXISTS public.saas_plans CASCADE;
DROP TABLE IF EXISTS public.users CASCADE;
DROP TABLE IF EXISTS public.super_admin_users CASCADE;
DROP TABLE IF EXISTS public.libraries CASCADE;

-- Cleanup legacy functions/triggers if any
DO $$
BEGIN
  DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users CASCADE;
  DROP FUNCTION IF EXISTS public.handle_new_user() CASCADE;
  DROP FUNCTION IF EXISTS public.elevate_user_role() CASCADE;
  DROP FUNCTION IF EXISTS auth.get_app_role() CASCADE;
EXCEPTION WHEN OTHERS THEN
  NULL;
END $$;

-- =========================================================================
-- 2. CREATE FRESH TABLES (Supporting exact LibDesk Kotlin data model mapping)
-- =========================================================================

-- 1. Libraries (Multi-tenant)
CREATE TABLE public.libraries (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    code TEXT NOT NULL,
    "logoUrl" TEXT DEFAULT '',
    description TEXT DEFAULT '',
    "establishedDate" TEXT DEFAULT '',
    "regNumber" TEXT DEFAULT '',
    "ownerName" TEXT DEFAULT '',
    "ownerPhone" TEXT DEFAULT '',
    "ownerEmail" TEXT DEFAULT '',
    "ownerWhatsApp" TEXT DEFAULT '',
    "alternateContact" TEXT DEFAULT '',
    address TEXT DEFAULT '',
    landmark TEXT DEFAULT '',
    city TEXT DEFAULT '',
    district TEXT DEFAULT '',
    state TEXT DEFAULT '',
    pincode TEXT DEFAULT '',
    latitude DOUBLE PRECISION DEFAULT 0.0,
    longitude DOUBLE PRECISION DEFAULT 0.0,
    phone TEXT DEFAULT '',
    whatsapp TEXT DEFAULT '',
    email TEXT DEFAULT '',
    website TEXT DEFAULT '',
    "upiId" TEXT DEFAULT '',
    "upiPayeeName" TEXT DEFAULT '',
    "receiptPrefix" TEXT DEFAULT 'REC',
    "defaultFinePerDay" DOUBLE PRECISION DEFAULT 5.0,
    "borrowLimit" INT DEFAULT 2,
    "loanDays" INT DEFAULT 14,
    "qrAttendanceStrictShift" BOOLEAN DEFAULT FALSE,
    "subscription_active" BOOLEAN DEFAULT TRUE,
    "createdAt" BIGINT DEFAULT 0,
    "updatedAt" BIGINT DEFAULT 0
);

-- 2. Users (Staff, Admins, Students)
CREATE TABLE public.users (
    id TEXT PRIMARY KEY,
    email TEXT NOT NULL,
    password TEXT DEFAULT 'password123',
    role TEXT NOT NULL, -- 'SUPER_ADMIN', 'OWNER', 'ADMIN', 'MANAGER', 'STUDENT'
    "libraryId" TEXT DEFAULT '',
    name TEXT NOT NULL,
    phone TEXT DEFAULT '',
    "avatarUrl" TEXT DEFAULT '',
    "studentIdRef" TEXT,
    "isActive" BOOLEAN DEFAULT TRUE,
    "createdAt" BIGINT DEFAULT 0
);

-- 3. Students / Members
CREATE TABLE public.students (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "studentCode" TEXT NOT NULL,
    "fullName" TEXT NOT NULL,
    "photoUrl" TEXT DEFAULT '',
    mobile TEXT NOT NULL,
    email TEXT DEFAULT '',
    dob TEXT DEFAULT '',
    gender TEXT DEFAULT 'Other',
    address TEXT DEFAULT '',
    "parentName" TEXT DEFAULT '',
    "parentMobile" TEXT DEFAULT '',
    "courseClass" TEXT DEFAULT '',
    college TEXT DEFAULT '',
    "targetExam" TEXT DEFAULT 'General',
    category TEXT DEFAULT 'General',
    batch TEXT DEFAULT 'Morning Regular',
    "planId" TEXT DEFAULT '',
    "planName" TEXT DEFAULT 'Monthly',
    "shiftId" TEXT DEFAULT '',
    "shiftName" TEXT DEFAULT 'Full Day',
    "seatId" TEXT DEFAULT '',
    "seatNumber" TEXT DEFAULT '',
    "hallName" TEXT DEFAULT '',
    "joiningDate" TEXT DEFAULT '',
    "expiryDate" TEXT DEFAULT '',
    "totalFee" DOUBLE PRECISION DEFAULT 1000.0,
    discount DOUBLE PRECISION DEFAULT 0.0,
    "paidAmount" DOUBLE PRECISION DEFAULT 0.0,
    "dueAmount" DOUBLE PRECISION DEFAULT 0.0,
    status TEXT DEFAULT 'ACTIVE',
    "rfidQrCode" TEXT DEFAULT '',
    "emergencyContact" TEXT DEFAULT '',
    password TEXT DEFAULT 'password123',
    "createdAt" BIGINT DEFAULT 0
);

-- 4. Halls
CREATE TABLE public.halls (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    name TEXT NOT NULL,
    type TEXT DEFAULT 'AC Hall',
    floor TEXT DEFAULT 'Ground Floor',
    "isAc" BOOLEAN DEFAULT TRUE,
    description TEXT DEFAULT '',
    "seatCount" INT DEFAULT 0,
    "openingTime" TEXT DEFAULT '06:00 AM',
    "closingTime" TEXT DEFAULT '11:00 PM',
    "isActive" BOOLEAN DEFAULT TRUE
);

-- 5. Cabins
CREATE TABLE public.cabins (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "cabinNumber" TEXT NOT NULL,
    name TEXT NOT NULL,
    floor TEXT DEFAULT '1st Floor',
    "isAc" BOOLEAN DEFAULT TRUE,
    "isPrivate" BOOLEAN DEFAULT TRUE,
    "seatCount" INT DEFAULT 1,
    "monthlyFee" DOUBLE PRECISION DEFAULT 2500.0,
    description TEXT DEFAULT '',
    "isActive" BOOLEAN DEFAULT TRUE
);

-- 6. Sections
CREATE TABLE public.sections (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT DEFAULT '',
    floor TEXT DEFAULT 'Ground Floor',
    "hallId" TEXT DEFAULT '',
    "cabinId" TEXT DEFAULT '',
    "isActive" BOOLEAN DEFAULT TRUE
);

-- 7. Shifts
CREATE TABLE public.shifts (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    name TEXT NOT NULL,
    "startTime" TEXT NOT NULL,
    "endTime" TEXT NOT NULL,
    fee DOUBLE PRECISION DEFAULT 800.0,
    description TEXT DEFAULT '',
    "isActive" BOOLEAN DEFAULT TRUE
);

-- 8. Membership Plans
CREATE TABLE public.membership_plans (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    name TEXT NOT NULL,
    "durationMonths" INT DEFAULT 1,
    "durationDays" INT DEFAULT 30,
    "durationType" TEXT DEFAULT 'MONTHS',
    "baseFee" DOUBLE PRECISION DEFAULT 1000.0,
    "maintenanceFee" DOUBLE PRECISION DEFAULT 100.0,
    "securityDeposit" DOUBLE PRECISION DEFAULT 500.0,
    discount DOUBLE PRECISION DEFAULT 0.0,
    "seatType" TEXT DEFAULT 'Standard',
    "shiftId" TEXT DEFAULT '',
    facilities TEXT DEFAULT '',
    "renewalRules" TEXT DEFAULT '',
    "isActive" BOOLEAN DEFAULT TRUE
);

-- 9. Seats
CREATE TABLE public.seats (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "seatNumber" TEXT NOT NULL,
    "hallId" TEXT DEFAULT '',
    "hallName" TEXT DEFAULT '',
    "sectionId" TEXT DEFAULT '',
    "sectionName" TEXT DEFAULT '',
    "cabinId" TEXT DEFAULT '',
    "cabinName" TEXT DEFAULT '',
    floor TEXT DEFAULT 'Ground Floor',
    "seatType" TEXT DEFAULT 'Standard',
    "monthlyFee" DOUBLE PRECISION DEFAULT 1000.0,
    status TEXT DEFAULT 'AVAILABLE',
    "assignedStudentId" TEXT DEFAULT '',
    "assignedStudentName" TEXT DEFAULT '',
    "assignedShiftId" TEXT DEFAULT '',
    "assignedShiftName" TEXT DEFAULT '',
    "validUntil" TEXT DEFAULT '',
    "gridRow" INT DEFAULT 1,
    "gridCol" INT DEFAULT 1,
    "floorZone" TEXT DEFAULT 'General Study Zone'
);

-- 10. Seat Assignments
CREATE TABLE public.seat_assignments (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "seatId" TEXT NOT NULL,
    "seatNumber" TEXT NOT NULL,
    "studentId" TEXT NOT NULL,
    "studentName" TEXT NOT NULL,
    "shiftId" TEXT DEFAULT '',
    "shiftName" TEXT DEFAULT '',
    "startDate" TEXT DEFAULT '',
    "endDate" TEXT DEFAULT '',
    "planId" TEXT DEFAULT '',
    status TEXT DEFAULT 'ACTIVE',
    notes TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT 0
);

-- 11. Attendance
CREATE TABLE public.attendance (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "studentId" TEXT NOT NULL,
    "studentName" TEXT NOT NULL,
    "seatNumber" TEXT DEFAULT '',
    "hallName" TEXT DEFAULT '',
    "shiftName" TEXT DEFAULT '',
    date TEXT NOT NULL,
    "checkInTime" TEXT NOT NULL,
    "checkOutTime" TEXT DEFAULT '',
    "durationMinutes" INT DEFAULT 0,
    status TEXT DEFAULT 'CHECKED_IN',
    mode TEXT DEFAULT 'QR_SCAN',
    notes TEXT DEFAULT '',
    timestamp BIGINT DEFAULT 0
);

-- 12. Payments & Billing
CREATE TABLE public.payments (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "receiptNumber" TEXT NOT NULL,
    "studentId" TEXT NOT NULL,
    "studentName" TEXT NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    "paymentMode" TEXT DEFAULT 'UPI',
    date TEXT NOT NULL,
    purpose TEXT DEFAULT 'MEMBERSHIP_FEE',
    "referenceNumber" TEXT DEFAULT '',
    notes TEXT DEFAULT '',
    remarks TEXT DEFAULT '',
    period TEXT DEFAULT '',
    "dueBalance" DOUBLE PRECISION DEFAULT 0.0,
    "createdAt" BIGINT DEFAULT 0
);

-- 13. Expenses
CREATE TABLE public.expenses (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    category TEXT NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    date TEXT NOT NULL,
    description TEXT DEFAULT '',
    "paymentMode" TEXT DEFAULT 'UPI',
    status TEXT DEFAULT 'PAID',
    "receiptRef" TEXT DEFAULT ''
);

-- 14. Fines
CREATE TABLE public.fines (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "studentId" TEXT NOT NULL,
    "studentName" TEXT NOT NULL,
    "bookId" TEXT DEFAULT '',
    "bookTitle" TEXT DEFAULT '',
    reason TEXT DEFAULT 'Late Book Return',
    amount DOUBLE PRECISION DEFAULT 25.0,
    paid BOOLEAN DEFAULT FALSE,
    date TEXT DEFAULT ''
);

-- 15. Physical Books
CREATE TABLE public.physical_books (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    title TEXT NOT NULL,
    author TEXT NOT NULL,
    isbn TEXT DEFAULT '',
    publisher TEXT DEFAULT '',
    edition TEXT DEFAULT '',
    category TEXT DEFAULT 'Competitive Exams',
    subject TEXT DEFAULT 'General Studies',
    rack TEXT DEFAULT 'Rack A',
    shelf TEXT DEFAULT 'Shelf 2',
    "accessionNumber" TEXT DEFAULT 'ACC-001',
    "totalCopies" INT DEFAULT 1,
    "availableCopies" INT DEFAULT 1,
    "issuedCopies" INT DEFAULT 0,
    "coverUrl" TEXT DEFAULT ''
);

-- 16. Book Issues
CREATE TABLE public.book_issues (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "bookId" TEXT NOT NULL,
    "bookTitle" TEXT NOT NULL,
    "studentId" TEXT NOT NULL,
    "studentName" TEXT NOT NULL,
    "studentMobile" TEXT DEFAULT '',
    "issueDate" TEXT NOT NULL,
    "dueDate" TEXT NOT NULL,
    "returnDate" TEXT DEFAULT '',
    "fineAmount" DOUBLE PRECISION DEFAULT 0.0,
    "finePaid" BOOLEAN DEFAULT FALSE,
    status TEXT DEFAULT 'ISSUED',
    notes TEXT DEFAULT ''
);

-- 17. Digital Materials
CREATE TABLE public.digital_materials (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT DEFAULT '',
    category TEXT DEFAULT 'UPSC',
    subject TEXT DEFAULT 'General Studies',
    exam TEXT DEFAULT 'All Exams',
    "fileType" TEXT DEFAULT 'PDF',
    "fileSize" TEXT DEFAULT '4.2 MB',
    "fileUrl" TEXT DEFAULT '',
    "accessPolicy" TEXT DEFAULT 'ALL_STUDENTS',
    "allowedGroup" TEXT DEFAULT 'All',
    "downloadCount" INT DEFAULT 0,
    "uploadDate" TEXT DEFAULT '',
    "isBookmarked" BOOLEAN DEFAULT FALSE
);

-- 18. Notices
CREATE TABLE public.notices (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    category TEXT DEFAULT 'GENERAL',
    priority TEXT DEFAULT 'NORMAL',
    date TEXT NOT NULL,
    "targetAudience" TEXT DEFAULT 'ALL',
    "senderName" TEXT DEFAULT 'LibDesk Admin',
    "isActive" BOOLEAN DEFAULT TRUE
);

-- 19. Feedback & Complaints
CREATE TABLE public.feedback_complaints (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "studentId" TEXT NOT NULL,
    "studentName" TEXT NOT NULL,
    email TEXT DEFAULT '',
    "seatNumber" TEXT DEFAULT '',
    type TEXT DEFAULT 'COMPLAINT',
    subject TEXT NOT NULL,
    message TEXT NOT NULL,
    status TEXT DEFAULT 'PENDING',
    reply TEXT DEFAULT '',
    date TEXT NOT NULL,
    "resolvedDate" TEXT DEFAULT ''
);

-- 20. Audit Logs
CREATE TABLE public.audit_logs (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "performedBy" TEXT NOT NULL,
    action TEXT NOT NULL,
    "recordType" TEXT NOT NULL,
    "recordId" TEXT NOT NULL,
    details TEXT DEFAULT '',
    timestamp BIGINT DEFAULT 0
);

-- 21. SaaS Subscription Plans (User-facing real plan catalog)
CREATE TABLE public.subscription_plans (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT DEFAULT '',
    price DOUBLE PRECISION NOT NULL,
    "durationMonths" INT DEFAULT 1,
    "durationDays" INT DEFAULT 30,
    "durationType" TEXT DEFAULT 'MONTHS',
    "maxSeats" INT DEFAULT 100,
    features TEXT DEFAULT '',
    badge TEXT DEFAULT '',
    "discountPercentage" DOUBLE PRECISION DEFAULT 0.0,
    "upiId" TEXT DEFAULT 'libdesk.billing@upi',
    "upiPayeeName" TEXT DEFAULT 'LibDesk Cloud Subscriptions',
    "supportWhatsApp" TEXT DEFAULT '',
    "isActive" BOOLEAN DEFAULT TRUE,
    "displayOrder" INT DEFAULT 1,
    "createdAt" BIGINT DEFAULT 0
);

-- 22. User Subscription Orders & Proofs
CREATE TABLE public.user_subscriptions (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "userId" TEXT DEFAULT '',
    "ownerName" TEXT DEFAULT '',
    "ownerMobile" TEXT DEFAULT '',
    "ownerEmail" TEXT DEFAULT '',
    "libraryName" TEXT DEFAULT '',
    "planId" TEXT NOT NULL,
    "planName" TEXT NOT NULL,
    "amountPaid" DOUBLE PRECISION NOT NULL,
    "billingCycle" TEXT DEFAULT 'MONTHLY',
    status TEXT DEFAULT 'ACTIVE',
    "startDate" TEXT DEFAULT '',
    "expiryDate" TEXT DEFAULT '',
    "paymentMethod" TEXT DEFAULT 'UPI_MANUAL',
    "paymentReferenceId" TEXT DEFAULT '',
    "receiptImageUrl" TEXT DEFAULT '',
    "isVerifiedByAdmin" BOOLEAN DEFAULT FALSE,
    notes TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT 0,
    "updatedAt" BIGINT DEFAULT 0
);

-- 23. Legacy SaaS Plans (for backward compatibility)
CREATE TABLE public.saas_plans (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    "durationMonths" INT NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    "maxSeats" INT NOT NULL,
    features TEXT DEFAULT '',
    "isActive" BOOLEAN DEFAULT TRUE,
    badge TEXT DEFAULT ''
);

-- 24. Library Subscriptions (Live Organization Status)
CREATE TABLE public.library_subscriptions (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "libraryName" TEXT NOT NULL,
    "planId" TEXT NOT NULL,
    "planName" TEXT NOT NULL,
    status TEXT DEFAULT 'ACTIVE',
    "subscription_active" BOOLEAN DEFAULT TRUE,
    "startDate" TEXT DEFAULT '',
    "expiryDate" TEXT DEFAULT '',
    price DOUBLE PRECISION NOT NULL,
    discount DOUBLE PRECISION DEFAULT 0.0,
    "maxSeats" INT DEFAULT 100,
    "autoRenew" BOOLEAN DEFAULT TRUE,
    notes TEXT DEFAULT '',
    "updatedAt" BIGINT DEFAULT 0,
    "durationDays" INT DEFAULT 30,
    "durationUnit" TEXT DEFAULT 'MONTHS'
);

-- 25. Super Admin User Master & Central Helpline Mapping
CREATE TABLE public.super_admin_users (
    id TEXT PRIMARY KEY DEFAULT 'SUPER-ADMIN-MASTER',
    email TEXT NOT NULL,
    name TEXT NOT NULL,
    mobile TEXT DEFAULT '',
    phone TEXT DEFAULT '',
    role TEXT DEFAULT 'SUPER_ADMIN',
    "accessCode" TEXT DEFAULT 'ADMIN99',
    "is2FaEnabled" BOOLEAN DEFAULT TRUE,
    "isClaimed" BOOLEAN DEFAULT FALSE,
    "upiId" TEXT DEFAULT 'libdesk.billing@upi',
    "upiPayeeName" TEXT DEFAULT 'LibDesk Cloud Subscriptions',
    "supportWhatsApp" TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT 0,
    "updatedAt" BIGINT DEFAULT 0
);

-- 26. User Booking Offline Cache
CREATE TABLE public.user_booking_cache (
    "studentId" TEXT PRIMARY KEY,
    "studentCode" TEXT DEFAULT '',
    "fullName" TEXT DEFAULT '',
    email TEXT DEFAULT '',
    phone TEXT DEFAULT '',
    "seatId" TEXT DEFAULT '',
    "seatNumber" TEXT DEFAULT '',
    "seatType" TEXT DEFAULT '',
    "hallId" TEXT DEFAULT '',
    "hallName" TEXT DEFAULT '',
    floor TEXT DEFAULT '',
    "shiftId" TEXT DEFAULT '',
    "shiftName" TEXT DEFAULT '',
    "shiftTimings" TEXT DEFAULT '',
    "planId" TEXT DEFAULT '',
    "planName" TEXT DEFAULT '',
    "membershipStatus" TEXT DEFAULT '',
    "startDate" TEXT DEFAULT '',
    "expiryDate" TEXT DEFAULT '',
    "rfidQrCode" TEXT DEFAULT '',
    "libraryId" TEXT DEFAULT '',
    "libraryName" TEXT DEFAULT '',
    "libraryAddress" TEXT DEFAULT '',
    "lastCheckInTime" TEXT DEFAULT '',
    "isCheckedIn" BOOLEAN DEFAULT FALSE,
    "hasAc" BOOLEAN DEFAULT TRUE,
    "hasPowerSocket" BOOLEAN DEFAULT TRUE,
    "hasReadingLamp" BOOLEAN DEFAULT TRUE,
    "hasLocker" BOOLEAN DEFAULT FALSE,
    "cachedTimestamp" BIGINT DEFAULT 0
);

-- =========================================================================
-- 3. ENABLE ROW LEVEL SECURITY (RLS) & ACCESS POLICIES
-- =========================================================================

-- 3.1. JWT Helper Functions (O(1) execution without per-row subqueries)
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

-- Supabase Auth custom access token hook (injects org_id, role, student_id directly into JWT payload)
CREATE OR REPLACE FUNCTION public.custom_access_token_hook(event jsonb)
RETURNS jsonb AS $$
DECLARE
  claims jsonb;
  user_role text;
  user_org_id text;
  user_student_id text;
BEGIN
  claims := event->'claims';
  
  user_role := COALESCE(
    event->'user'->'app_metadata'->>'role',
    event->'user'->'user_metadata'->>'role',
    'STUDENT'
  );
  
  user_org_id := COALESCE(
    event->'user'->'app_metadata'->>'org_id',
    event->'user'->'app_metadata'->>'library_id',
    event->'user'->'user_metadata'->>'org_id',
    event->'user'->'user_metadata'->>'library_id',
    ''
  );

  user_student_id := COALESCE(
    event->'user'->'app_metadata'->>'student_id',
    event->'user'->'user_metadata'->>'student_id',
    ''
  );

  claims := jsonb_set(claims, '{role}', to_jsonb(user_role));
  claims := jsonb_set(claims, '{org_id}', to_jsonb(user_org_id));
  claims := jsonb_set(claims, '{student_id}', to_jsonb(user_student_id));

  event := jsonb_set(event, '{claims}', claims);
  RETURN event;
END;
$$ LANGUAGE plpgsql STABLE SECURITY DEFINER;

-- 3.2. Enable RLS on all tables
ALTER TABLE public.libraries ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.students ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.seats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.halls ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cabins ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sections ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shifts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.membership_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.seat_assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.attendance ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.fines ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.physical_books ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.book_issues ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.digital_materials ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.feedback_complaints ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.subscription_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.saas_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.library_subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.super_admin_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_booking_cache ENABLE ROW LEVEL SECURITY;

-- 3.3. RLS POLICIES: Libraries
CREATE POLICY "super_admin_all_libraries" ON public.libraries
  FOR ALL TO authenticated USING (public.is_super_admin()) WITH CHECK (public.is_super_admin());
CREATE POLICY "owner_select_own_library" ON public.libraries
  FOR SELECT TO authenticated USING (public.is_library_owner() AND id = public.jwt_org_id());
CREATE POLICY "owner_update_own_library" ON public.libraries
  FOR UPDATE TO authenticated USING (public.is_library_owner() AND id = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND id = public.jwt_org_id());
CREATE POLICY "student_select_own_library" ON public.libraries
  FOR SELECT TO authenticated USING (public.is_student() AND id = public.jwt_org_id());
CREATE POLICY "anon_select_libraries_for_enrollment" ON public.libraries
  FOR SELECT TO anon USING (true);
CREATE POLICY "anon_insert_new_library" ON public.libraries
  FOR INSERT TO anon WITH CHECK (true);

-- 3.4. RLS POLICIES: Super Admin Master Profile
CREATE POLICY "super_admin_manage_profile" ON public.super_admin_users
  FOR ALL TO authenticated USING (public.is_super_admin()) WITH CHECK (public.is_super_admin());
CREATE POLICY "anon_select_super_admin_helpline" ON public.super_admin_users
  FOR SELECT TO anon, authenticated USING (true);

-- 3.5. RLS POLICIES: SaaS Subscription Plans
CREATE POLICY "super_admin_all_subscription_plans" ON public.subscription_plans
  FOR ALL TO authenticated USING (public.is_super_admin()) WITH CHECK (public.is_super_admin());
CREATE POLICY "public_read_subscription_plans" ON public.subscription_plans
  FOR SELECT TO anon, authenticated USING (true);
CREATE POLICY "super_admin_all_saas_plans" ON public.saas_plans
  FOR ALL TO authenticated USING (public.is_super_admin()) WITH CHECK (public.is_super_admin());
CREATE POLICY "public_read_saas_plans" ON public.saas_plans
  FOR SELECT TO anon, authenticated USING (true);

-- 3.6. RLS POLICIES: Subscriptions (License Management & UPI Verification)
CREATE POLICY "super_admin_all_library_subscriptions" ON public.library_subscriptions
  FOR ALL TO authenticated USING (public.is_super_admin()) WITH CHECK (public.is_super_admin());
CREATE POLICY "owner_view_own_library_subscriptions" ON public.library_subscriptions
  FOR SELECT TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "super_admin_all_user_subscriptions" ON public.user_subscriptions
  FOR ALL TO authenticated USING (public.is_super_admin()) WITH CHECK (public.is_super_admin());
CREATE POLICY "owner_view_own_user_subscriptions" ON public.user_subscriptions
  FOR SELECT TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "owner_insert_user_subscription_payment" ON public.user_subscriptions
  FOR INSERT TO authenticated WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());

-- 3.7. RLS POLICIES: Users
CREATE POLICY "super_admin_all_users" ON public.users
  FOR ALL TO authenticated USING (public.is_super_admin()) WITH CHECK (public.is_super_admin());
CREATE POLICY "owner_manage_org_users" ON public.users
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "user_self_access" ON public.users
  FOR ALL TO authenticated USING (id = auth.uid()::text OR email = (auth.jwt() ->> 'email')) WITH CHECK (id = auth.uid()::text OR email = (auth.jwt() ->> 'email'));
CREATE POLICY "anon_insert_users" ON public.users
  FOR INSERT TO anon WITH CHECK (true);

-- 3.8. RLS POLICIES: Students (Strict PII Isolation - Super Admin has NO Student PII access)
CREATE POLICY "owner_manage_org_students" ON public.students
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_self_profile" ON public.students
  FOR ALL TO authenticated USING (
    public.is_student() AND (
      id = public.jwt_student_id() OR
      "userId" = auth.uid()::text OR
      email = (auth.jwt() ->> 'email')
    )
  ) WITH CHECK (
    public.is_student() AND (
      id = public.jwt_student_id() OR
      "userId" = auth.uid()::text OR
      email = (auth.jwt() ->> 'email')
    )
  );
CREATE POLICY "anon_student_qr_enrollment" ON public.students
  FOR INSERT TO anon WITH CHECK (true);

-- 3.9. RLS POLICIES: Desks, Infrastructure & Shifts (Tenant Isolated)
CREATE POLICY "owner_manage_seats" ON public.seats
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_view_org_seats" ON public.seats
  FOR SELECT TO authenticated USING (public.is_student() AND "libraryId" = public.jwt_org_id());

CREATE POLICY "owner_manage_halls" ON public.halls
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_view_org_halls" ON public.halls
  FOR SELECT TO authenticated USING (public.is_student() AND "libraryId" = public.jwt_org_id());

CREATE POLICY "owner_manage_cabins" ON public.cabins
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_view_org_cabins" ON public.cabins
  FOR SELECT TO authenticated USING (public.is_student() AND "libraryId" = public.jwt_org_id());

CREATE POLICY "owner_manage_sections" ON public.sections
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_view_org_sections" ON public.sections
  FOR SELECT TO authenticated USING (public.is_student() AND "libraryId" = public.jwt_org_id());

CREATE POLICY "owner_manage_shifts" ON public.shifts
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_view_org_shifts" ON public.shifts
  FOR SELECT TO authenticated USING (public.is_student() AND "libraryId" = public.jwt_org_id());

CREATE POLICY "owner_manage_plans" ON public.membership_plans
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_view_org_plans" ON public.membership_plans
  FOR SELECT TO authenticated USING (public.is_student() AND "libraryId" = public.jwt_org_id());

CREATE POLICY "owner_manage_seat_assignments" ON public.seat_assignments
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_view_seat_assignments" ON public.seat_assignments
  FOR SELECT TO authenticated USING (public.is_student() AND "libraryId" = public.jwt_org_id());

-- 3.10. RLS POLICIES: Attendance (Owner full control; Student self check-in & view only)
CREATE POLICY "owner_manage_attendance" ON public.attendance
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_view_own_attendance" ON public.attendance
  FOR SELECT TO authenticated USING (
    public.is_student() AND (
      "studentId" = public.jwt_student_id() OR
      "studentId" = auth.uid()::text
    )
  );
CREATE POLICY "student_self_punch_attendance" ON public.attendance
  FOR INSERT TO authenticated WITH CHECK (
    public.is_student() AND (
      "studentId" = public.jwt_student_id() OR
      "studentId" = auth.uid()::text
    ) AND "libraryId" = public.jwt_org_id()
  );

-- 3.11. RLS POLICIES: Fees & Finance (Payments, Expenses, Fines)
CREATE POLICY "owner_manage_payments" ON public.payments
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_view_own_payments" ON public.payments
  FOR SELECT TO authenticated USING (
    public.is_student() AND (
      "studentId" = public.jwt_student_id() OR
      "studentId" = auth.uid()::text
    )
  );

CREATE POLICY "owner_manage_expenses" ON public.expenses
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());

CREATE POLICY "owner_manage_fines" ON public.fines
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_view_own_fines" ON public.fines
  FOR SELECT TO authenticated USING (
    public.is_student() AND (
      "studentId" = public.jwt_student_id() OR
      "studentId" = auth.uid()::text
    )
  );

-- 3.12. RLS POLICIES: Digital Library & Catalog
CREATE POLICY "owner_manage_physical_books" ON public.physical_books
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_view_physical_books" ON public.physical_books
  FOR SELECT TO authenticated USING (public.is_student() AND "libraryId" = public.jwt_org_id());

CREATE POLICY "owner_manage_book_issues" ON public.book_issues
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_view_own_book_issues" ON public.book_issues
  FOR SELECT TO authenticated USING (
    public.is_student() AND (
      "studentId" = public.jwt_student_id() OR
      "studentId" = auth.uid()::text
    )
  );

CREATE POLICY "owner_manage_digital_materials" ON public.digital_materials
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_view_digital_materials" ON public.digital_materials
  FOR SELECT TO authenticated USING (public.is_student() AND "libraryId" = public.jwt_org_id());

-- 3.13. RLS POLICIES: Notices & Communications
CREATE POLICY "super_admin_broadcast_notices" ON public.notices
  FOR ALL TO authenticated USING (public.is_super_admin()) WITH CHECK (public.is_super_admin());
CREATE POLICY "owner_manage_org_notices" ON public.notices
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_view_org_notices" ON public.notices
  FOR SELECT TO authenticated USING (
    public.is_student() AND (
      "libraryId" = public.jwt_org_id() OR
      "libraryId" = '' OR
      "isGlobal" = true
    )
  );

-- 3.14. RLS POLICIES: Complaints & Helpdesk
CREATE POLICY "owner_manage_complaints" ON public.feedback_complaints
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_self_complaints" ON public.feedback_complaints
  FOR ALL TO authenticated USING (
    public.is_student() AND (
      "studentId" = public.jwt_student_id() OR
      "studentId" = auth.uid()::text OR
      email = (auth.jwt() ->> 'email')
    )
  ) WITH CHECK (
    public.is_student() AND (
      "studentId" = public.jwt_student_id() OR
      "studentId" = auth.uid()::text OR
      email = (auth.jwt() ->> 'email')
    )
  );

-- 3.15. RLS POLICIES: Audit Logs
CREATE POLICY "super_admin_view_platform_audit" ON public.audit_logs
  FOR SELECT TO authenticated USING (public.is_super_admin());
CREATE POLICY "owner_manage_org_audit_logs" ON public.audit_logs
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());

-- 3.16. RLS POLICIES: User Booking Cache
CREATE POLICY "owner_view_org_cache" ON public.user_booking_cache
  FOR ALL TO authenticated USING (public.is_library_owner() AND "libraryId" = public.jwt_org_id()) WITH CHECK (public.is_library_owner() AND "libraryId" = public.jwt_org_id());
CREATE POLICY "student_manage_own_cache" ON public.user_booking_cache
  FOR ALL TO authenticated USING (
    public.is_student() AND (
      "studentId" = public.jwt_student_id() OR
      "studentId" = auth.uid()::text
    )
  ) WITH CHECK (
    public.is_student() AND (
      "studentId" = public.jwt_student_id() OR
      "studentId" = auth.uid()::text
    )
  );

-- =========================================================================
-- 4. REPLICA IDENTITY (Required for Realtime UPDATE & DELETE full payloads)
-- =========================================================================

ALTER TABLE public.libraries REPLICA IDENTITY FULL;
ALTER TABLE public.users REPLICA IDENTITY FULL;
ALTER TABLE public.students REPLICA IDENTITY FULL;
ALTER TABLE public.seats REPLICA IDENTITY FULL;
ALTER TABLE public.halls REPLICA IDENTITY FULL;
ALTER TABLE public.cabins REPLICA IDENTITY FULL;
ALTER TABLE public.sections REPLICA IDENTITY FULL;
ALTER TABLE public.shifts REPLICA IDENTITY FULL;
ALTER TABLE public.membership_plans REPLICA IDENTITY FULL;
ALTER TABLE public.seat_assignments REPLICA IDENTITY FULL;
ALTER TABLE public.attendance REPLICA IDENTITY FULL;
ALTER TABLE public.payments REPLICA IDENTITY FULL;
ALTER TABLE public.expenses REPLICA IDENTITY FULL;
ALTER TABLE public.fines REPLICA IDENTITY FULL;
ALTER TABLE public.physical_books REPLICA IDENTITY FULL;
ALTER TABLE public.book_issues REPLICA IDENTITY FULL;
ALTER TABLE public.digital_materials REPLICA IDENTITY FULL;
ALTER TABLE public.notices REPLICA IDENTITY FULL;
ALTER TABLE public.feedback_complaints REPLICA IDENTITY FULL;
ALTER TABLE public.audit_logs REPLICA IDENTITY FULL;
ALTER TABLE public.super_admin_users REPLICA IDENTITY FULL;
ALTER TABLE public.subscription_plans REPLICA IDENTITY FULL;
ALTER TABLE public.user_subscriptions REPLICA IDENTITY FULL;
ALTER TABLE public.library_subscriptions REPLICA IDENTITY FULL;

-- =========================================================================
-- 5. REALTIME PUBLICATION (Ensures instant cloud sync for mobile clients)
-- =========================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        CREATE PUBLICATION supabase_realtime;
    END IF;
END $$ LANGUAGE plpgsql;

-- =========================================================================
-- 6. SUPABASE AUTHENTICATION AUTOMATIC USER PROFILE TRIGGER
-- =========================================================================

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
DECLARE
  v_role text;
  v_org_id text;
  v_name text;
BEGIN
  v_role := COALESCE(
    new.raw_app_meta_data->>'role',
    new.raw_user_meta_data->>'role',
    'STUDENT'
  );
  v_org_id := COALESCE(
    new.raw_app_meta_data->>'org_id',
    new.raw_app_meta_data->>'library_id',
    new.raw_user_meta_data->>'org_id',
    new.raw_user_meta_data->>'library_id',
    ''
  );
  v_name := COALESCE(
    new.raw_user_meta_data->>'full_name',
    new.raw_user_meta_data->>'name',
    split_part(COALESCE(new.email, ''), '@', 1),
    'LibDesk User'
  );

  INSERT INTO public.users (
    id,
    email,
    name,
    role,
    "libraryId",
    "isActive",
    "createdAt"
  ) VALUES (
    new.id::text,
    COALESCE(new.email, ''),
    v_name,
    v_role,
    v_org_id,
    true,
    EXTRACT(EPOCH FROM now())::BIGINT * 1000
  )
  ON CONFLICT (id) DO UPDATE SET
    email = EXCLUDED.email,
    name = CASE WHEN EXCLUDED.name <> '' THEN EXCLUDED.name ELSE public.users.name END,
    role = CASE WHEN EXCLUDED.role <> '' THEN EXCLUDED.role ELSE public.users.role END,
    "libraryId" = CASE WHEN EXCLUDED."libraryId" <> '' THEN EXCLUDED."libraryId" ELSE public.users."libraryId" END;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- =========================================================================
-- 7. PERFORMANCE INDEXES
-- =========================================================================

CREATE INDEX IF NOT EXISTS idx_users_email ON public.users(email);
CREATE INDEX IF NOT EXISTS idx_users_library_id ON public.users("libraryId");
CREATE INDEX IF NOT EXISTS idx_students_library_id ON public.students("libraryId");
CREATE INDEX IF NOT EXISTS idx_students_mobile ON public.students(mobile);
CREATE INDEX IF NOT EXISTS idx_students_status ON public.students(status);
CREATE INDEX IF NOT EXISTS idx_seats_library_id ON public.seats("libraryId");
CREATE INDEX IF NOT EXISTS idx_seats_status ON public.seats(status);
CREATE INDEX IF NOT EXISTS idx_attendance_library_date ON public.attendance("libraryId", date);
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

-- =========================================================================
-- 8. INITIAL SEED DATA (Super Admin Master & Subscription Plans)
-- =========================================================================

INSERT INTO public.super_admin_users (
    id, email, name, mobile, phone, role, "accessCode", "is2FaEnabled", "isClaimed", "upiId", "upiPayeeName", "supportWhatsApp", "createdAt", "updatedAt"
) VALUES (
    'SUPER-ADMIN-MASTER', 'smtsharma282.sks@gmail.com', 'Super Administrator', '', '', 'SUPER_ADMIN', 'ADMIN99', TRUE, TRUE, 'libdesk.billing@upi', 'LibDesk Cloud Subscriptions', '', 1700000000000, 1700000000000
) ON CONFLICT (id) DO UPDATE SET email = EXCLUDED.email;

INSERT INTO public.subscription_plans (
    id, name, description, price, "durationMonths", "durationDays", "durationType", "maxSeats", features, badge, "discountPercentage", "upiId", "upiPayeeName", "supportWhatsApp", "isActive", "displayOrder", "createdAt"
) VALUES 
('SUB-PLAN-STARTER', 'Starter Launch', 'Essential digital library suite for small halls & study rooms', 499.0, 1, 30, 'MONTHS', 60, 'Up to 60 Dedicated Seats
Smart Gate QR Code Attendance
Cash & UPI Fee Ledger
Real-time Student Directory
Digital Notice Board Broadcast
Instant Setup in 2 Minutes', 'Starter Pack', 0.0, 'libdesk.billing@upi', 'LibDesk Cloud Subscriptions', '', TRUE, 1, 1700000000000),

('SUB-PLAN-PRO', 'Growth Pro', 'Most popular choice for growing libraries with multiple shifts', 999.0, 1, 30, 'MONTHS', 160, 'Up to 160 Dedicated & Flexible Seats
3 Shifts Support (Morning/Evening/Full Day)
Direct WhatsApp Fee Slips & Reminders
Student Self-Service Portal Access
Digital E-Book Catalog & Issues
Full Daily P&L Expense Tracking
Cloud-Synchronized Multi-Tenant Security', 'Most Popular', 15.0, 'libdesk.billing@upi', 'LibDesk Cloud Subscriptions', '', TRUE, 2, 1700000000000),

('SUB-PLAN-ENTERPRISE', 'Enterprise Annual', 'Maximum scale with unlimited seats, custom branding & VIP support', 7999.0, 12, 365, 'MONTHS', 9999, 'Unlimited Seats & Multi-Halls
Custom UPI QR Code for Member Fees
2 Months Free on Annual Billing
Automated Cloud Sync & Backup
Priority 24x7 WhatsApp VIP Support
Biometric & RFID Turnstile Ready
Advanced Monthly Financial Reports', 'Best Value (Save 35%)', 35.0, 'libdesk.billing@upi', 'LibDesk Cloud Subscriptions', '', TRUE, 3, 1700000000000)
ON CONFLICT (id) DO NOTHING;

-- =========================================================================
-- 9. STUDY MATERIAL STORAGE & NCERT OFFICIAL CATALOG
-- =========================================================================

-- Create Private Supabase Storage Bucket for Study Materials (Max 20 MB, PDF only)
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'study-materials',
    'study-materials',
    false,
    20971520,
    ARRAY['application/pdf']::text[]
)
ON CONFLICT (id) DO UPDATE SET
    public = false,
    file_size_limit = 20971520,
    allowed_mime_types = ARRAY['application/pdf']::text[];

-- Table: study_materials (Owner PDF Uploads)
CREATE TABLE IF NOT EXISTS public.study_materials (
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

-- Table: ncert_catalog (Official Textbooks Metadata & Direct Links Only)
CREATE TABLE IF NOT EXISTS public.ncert_catalog (
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

-- Table: download_logs (Auditing & Analytics)
CREATE TABLE IF NOT EXISTS public.download_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id TEXT NOT NULL,
    material_id TEXT NOT NULL,
    source TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- RPC: increment_download_count
CREATE OR REPLACE FUNCTION public.increment_download_count(p_material_id UUID)
RETURNS void AS $$
BEGIN
    UPDATE public.study_materials
    SET download_count = download_count + 1,
        updated_at = NOW()
    WHERE id = p_material_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_study_materials_org_cat ON public.study_materials(org_id, category);
CREATE INDEX IF NOT EXISTS idx_study_materials_deleted ON public.study_materials(deleted_at);
CREATE INDEX IF NOT EXISTS idx_ncert_class_subj_med ON public.ncert_catalog(class_level, subject, medium);
CREATE INDEX IF NOT EXISTS idx_ncert_is_active ON public.ncert_catalog(is_active);

-- Enable RLS
ALTER TABLE public.study_materials ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ncert_catalog ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.download_logs ENABLE ROW LEVEL SECURITY;

-- Policies: study_materials
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

-- Policies: ncert_catalog
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

-- Policies: download_logs
CREATE POLICY "auth_insert_download_logs" ON public.download_logs
    FOR INSERT TO authenticated
    WITH CHECK (true);

CREATE POLICY "owner_view_download_logs" ON public.download_logs
    FOR SELECT TO authenticated
    USING (public.is_library_owner() OR public.is_super_admin());

-- Storage RLS Policies
DROP POLICY IF EXISTS "owner_upload_study_materials_storage" ON storage.objects;
DROP POLICY IF EXISTS "owner_delete_study_materials_storage" ON storage.objects;
DROP POLICY IF EXISTS "student_download_study_materials_storage" ON storage.objects;

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

-- Realtime Publication for all 27 LibDesk Tables
ALTER TABLE public.study_materials REPLICA IDENTITY FULL;
ALTER TABLE public.ncert_catalog REPLICA IDENTITY FULL;
ALTER TABLE public.download_logs REPLICA IDENTITY FULL;

ALTER PUBLICATION supabase_realtime SET TABLE 
    public.libraries,
    public.users,
    public.students,
    public.seats,
    public.halls,
    public.cabins,
    public.sections,
    public.shifts,
    public.membership_plans,
    public.seat_assignments,
    public.attendance,
    public.payments,
    public.expenses,
    public.fines,
    public.physical_books,
    public.book_issues,
    public.digital_materials,
    public.notices,
    public.feedback_complaints,
    public.audit_logs,
    public.super_admin_users,
    public.subscription_plans,
    public.user_subscriptions,
    public.library_subscriptions,
    public.study_materials,
    public.ncert_catalog,
    public.download_logs;

INSERT INTO public.ncert_catalog (class_level, subject, book_title, medium, language, edition_year, source_name, source_url, is_active)
VALUES
  (10, 'Science', 'Science - Class X', 'english', 'en', '2026-27', 'ncert', 'https://ncert.nic.in/textbook/pdf/jesc1dd.zip', true),
  (10, 'Mathematics', 'Mathematics - Class X', 'english', 'en', '2026-27', 'ncert', 'https://ncert.nic.in/textbook/pdf/jemh1dd.zip', true),
  (10, 'Mathematics', 'गणित - कक्षा 10', 'hindi', 'hi', '2026-27', 'ncert', 'https://ncert.nic.in/textbook/pdf/jhmh1dd.zip', true),
  (9, 'Science', 'Science - Class IX (NEP 2020 Revised)', 'english', 'en', '2026-27', 'ncert', 'https://ncert.nic.in/textbook/pdf/iesc1dd.zip', true),
  (9, 'Mathematics', 'Mathematics - Class IX (NEP 2020 Revised)', 'english', 'en', '2026-27', 'ncert', 'https://ncert.nic.in/textbook/pdf/iemh1dd.zip', true),
  (12, 'Physics', 'Physics Part - I', 'english', 'en', '2026-27', 'ncert', 'https://ncert.nic.in/textbook/pdf/leph1dd.zip', true)
ON CONFLICT DO NOTHING;

