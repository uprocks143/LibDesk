-- =========================================================================
-- LibDesk Supabase PostgreSQL Database Reset & Schema Script
-- Run this in Supabase Dashboard -> SQL Editor
-- This will DROP all existing tables and data, then CREATE fresh tables with RLS
-- =========================================================================

-- 1. DROP ALL EXISTING TABLES & OBJECTS (CASCADE handles dependencies)
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
DROP TABLE IF EXISTS public.library_subscriptions CASCADE;
DROP TABLE IF EXISTS public.saas_plans CASCADE;
DROP TABLE IF EXISTS public.users CASCADE;
DROP TABLE IF EXISTS public.super_admin_users CASCADE;
DROP TABLE IF EXISTS public.libraries CASCADE;

-- Optional legacy function cleanup (only if permissions allow)
DO $$
BEGIN
  BEGIN
    DROP FUNCTION IF EXISTS public.elevate_user_role() CASCADE;
  EXCEPTION WHEN OTHERS THEN
    NULL;
  END;
END $$;

-- =========================================================================
-- 2. CREATE FRESH TABLES
-- =========================================================================

-- Libraries (Multi-tenant)
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

-- Users (Staff, Admins, Students)
CREATE TABLE public.users (
    id TEXT PRIMARY KEY,
    email TEXT NOT NULL,
    password TEXT DEFAULT 'password123',
    role TEXT NOT NULL, -- 'SUPER_ADMIN', 'MANAGER', 'STUDENT'
    "libraryId" TEXT DEFAULT '',
    name TEXT NOT NULL,
    phone TEXT DEFAULT '',
    "avatarUrl" TEXT DEFAULT '',
    "studentIdRef" TEXT,
    "isActive" BOOLEAN DEFAULT TRUE,
    "createdAt" BIGINT DEFAULT 0
);

-- Students / Members
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

-- Halls
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

-- Cabins
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

-- Sections
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

-- Shifts
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

-- Membership Plans
CREATE TABLE public.membership_plans (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    name TEXT NOT NULL,
    "durationMonths" INT DEFAULT 1,
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

-- Seats
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

-- Seat Assignments
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

-- Attendance
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

-- Payments & Billing
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

-- Expenses
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

-- Fines
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

-- Physical Books
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

-- Book Issues
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

-- Digital Materials
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

-- Notices
CREATE TABLE public.notices (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    category TEXT DEFAULT 'GENERAL',
    priority TEXT DEFAULT 'NORMAL',
    date TEXT NOT NULL,
    "targetAudience" TEXT DEFAULT 'ALL',
    "isActive" BOOLEAN DEFAULT TRUE
);

-- Feedback & Complaints
CREATE TABLE public.feedback_complaints (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "studentId" TEXT NOT NULL,
    "studentName" TEXT NOT NULL,
    "seatNumber" TEXT DEFAULT '',
    type TEXT DEFAULT 'COMPLAINT',
    subject TEXT NOT NULL,
    message TEXT NOT NULL,
    status TEXT DEFAULT 'PENDING',
    reply TEXT DEFAULT '',
    date TEXT NOT NULL,
    "resolvedDate" TEXT DEFAULT ''
);

-- Audit Logs
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

-- SaaS Plans & Subscriptions
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
    "durationDays" INT DEFAULT 0,
    "durationUnit" TEXT DEFAULT 'MONTHS'
);

-- Super Admin User Master
CREATE TABLE public.super_admin_users (
    id TEXT PRIMARY KEY DEFAULT 'SUPER-ADMIN-MASTER',
    email TEXT NOT NULL,
    name TEXT NOT NULL,
    mobile TEXT DEFAULT '',
    role TEXT DEFAULT 'SUPER_ADMIN',
    "accessCode" TEXT DEFAULT '',
    "is2FaEnabled" BOOLEAN DEFAULT TRUE,
    "isClaimed" BOOLEAN DEFAULT FALSE,
    "createdAt" BIGINT DEFAULT 0
);

-- User Booking Offline Cache
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

-- Enable RLS across all tables
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
ALTER TABLE public.saas_plans ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.library_subscriptions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.super_admin_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_booking_cache ENABLE ROW LEVEL SECURITY;

-- Allow authenticated users and app anon key with access policies
CREATE POLICY "Allow anon all libraries" ON public.libraries FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all users" ON public.users FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all students" ON public.students FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all seats" ON public.seats FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all halls" ON public.halls FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all cabins" ON public.cabins FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all sections" ON public.sections FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all shifts" ON public.shifts FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all membership_plans" ON public.membership_plans FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all seat_assignments" ON public.seat_assignments FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all attendance" ON public.attendance FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all payments" ON public.payments FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all expenses" ON public.expenses FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all fines" ON public.fines FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all physical_books" ON public.physical_books FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all book_issues" ON public.book_issues FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all digital_materials" ON public.digital_materials FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all notices" ON public.notices FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all feedback_complaints" ON public.feedback_complaints FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all audit_logs" ON public.audit_logs FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all saas_plans" ON public.saas_plans FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all library_subscriptions" ON public.library_subscriptions FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all super_admin_users" ON public.super_admin_users FOR ALL USING (true) WITH CHECK (true);
CREATE POLICY "Allow anon all user_booking_cache" ON public.user_booking_cache FOR ALL USING (true) WITH CHECK (true);

-- Note: The app manages roles directly in public.users. If you wish to enable the auth trigger
-- on auth.users, run it with postgres/superuser privileges:
-- CREATE OR REPLACE FUNCTION public.elevate_user_role()
-- RETURNS TRIGGER AS $$
-- BEGIN
--   NEW.raw_app_meta_data = jsonb_set(
--     COALESCE(NEW.raw_app_meta_data, '{}'::jsonb),
--     '{role}',
--     COALESCE(NEW.raw_user_meta_data->'role', '"STUDENT"'::jsonb)
--   );
--   RETURN NEW;
-- END;
-- $$ LANGUAGE plpgsql SECURITY DEFINER;
--
-- CREATE TRIGGER on_auth_user_created
--   BEFORE INSERT ON auth.users
--   FOR EACH ROW EXECUTE FUNCTION public.elevate_user_role();

-- =========================================================================
-- 4. PERFORMANCE INDEXES
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

