-- =====================================================================
-- LibDesk Library Management Platform - Supabase PostgreSQL Schema
-- Complete multi-tenant schema matching LibDesk Android data contracts
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------------------------------------------------------------------
-- 1. Helper Functions for JWT Claims & Tenant Isolation
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.jwt_org_id()
RETURNS text
LANGUAGE sql
STABLE
AS $$
  SELECT coalesce(
    nullif(current_setting('request.jwt.claims', true)::jsonb->'app_metadata'->>'org_id', ''),
    nullif(current_setting('request.jwt.claims', true)::jsonb->'user_metadata'->>'libraryId', ''),
    ''
  );
$$;

CREATE OR REPLACE FUNCTION public.jwt_role()
RETURNS text
LANGUAGE sql
STABLE
AS $$
  SELECT coalesce(
    nullif(current_setting('request.jwt.claims', true)::jsonb->'app_metadata'->>'role', ''),
    nullif(current_setting('request.jwt.claims', true)::jsonb->'user_metadata'->>'role', ''),
    'STUDENT'
  );
$$;

-- ---------------------------------------------------------------------
-- 2. Core Organization & Tenancy
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.libraries (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    code TEXT UNIQUE NOT NULL,
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
    "borrowLimit" INTEGER DEFAULT 2,
    "loanDays" INTEGER DEFAULT 14,
    "qrAttendanceStrictShift" BOOLEAN DEFAULT false,
    "wifiSsid" TEXT DEFAULT 'LibDesk_HighSpeed_5G',
    "wifiPassword" TEXT DEFAULT 'StudyQuiet@2026',
    "wifiLastRotated" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint,
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint,
    "updatedAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

-- ---------------------------------------------------------------------
-- 3. Users & Auth Synchronization
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.users (
    id TEXT PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    password TEXT DEFAULT '',
    role TEXT NOT NULL DEFAULT 'OWNER',
    "libraryId" TEXT NOT NULL,
    name TEXT NOT NULL,
    phone TEXT DEFAULT '',
    "avatarUrl" TEXT DEFAULT '',
    "studentIdRef" TEXT DEFAULT NULL,
    "isActive" BOOLEAN DEFAULT true,
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

CREATE TABLE IF NOT EXISTS public.super_admin_users (
    id TEXT PRIMARY KEY DEFAULT 'SUPER-ADMIN-MASTER',
    email TEXT UNIQUE NOT NULL,
    name TEXT DEFAULT 'Super Administrator',
    mobile TEXT DEFAULT '',
    role TEXT DEFAULT 'SUPER_ADMIN',
    "accessCode" TEXT DEFAULT '',
    "is2FaEnabled" BOOLEAN DEFAULT true,
    "isClaimed" BOOLEAN DEFAULT false,
    "upiId" TEXT DEFAULT '',
    "upiPayeeName" TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

-- ---------------------------------------------------------------------
-- 4. Infrastructure (Halls, Cabins, Sections, Shifts)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.halls (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    type TEXT NOT NULL DEFAULT 'READING_HALL',
    floor TEXT DEFAULT 'Ground Floor',
    "isAc" BOOLEAN DEFAULT true,
    description TEXT DEFAULT '',
    "seatCount" INTEGER DEFAULT 0,
    "openingTime" TEXT DEFAULT '06:00 AM',
    "closingTime" TEXT DEFAULT '11:00 PM',
    "isActive" BOOLEAN DEFAULT true
);

CREATE TABLE IF NOT EXISTS public.cabins (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    "cabinNumber" TEXT NOT NULL,
    name TEXT NOT NULL,
    floor TEXT DEFAULT '1st Floor',
    "isAc" BOOLEAN DEFAULT true,
    "isPrivate" BOOLEAN DEFAULT true,
    "seatCount" INTEGER DEFAULT 1,
    "monthlyFee" DOUBLE PRECISION DEFAULT 2500.0,
    description TEXT DEFAULT '',
    "isActive" BOOLEAN DEFAULT true
);

CREATE TABLE IF NOT EXISTS public.sections (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT DEFAULT '',
    floor TEXT DEFAULT 'Ground Floor',
    "hallId" TEXT DEFAULT '',
    "cabinId" TEXT DEFAULT '',
    "isActive" BOOLEAN DEFAULT true
);

CREATE TABLE IF NOT EXISTS public.shifts (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    "startTime" TEXT NOT NULL,
    "endTime" TEXT NOT NULL,
    fee DOUBLE PRECISION DEFAULT 800.0,
    description TEXT DEFAULT '',
    "isActive" BOOLEAN DEFAULT true
);

-- ---------------------------------------------------------------------
-- 5. Membership Plans & Seats
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.membership_plans (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    "durationMonths" INTEGER DEFAULT 1,
    "durationDays" INTEGER DEFAULT 30,
    "durationType" TEXT DEFAULT 'MONTHS',
    "baseFee" DOUBLE PRECISION DEFAULT 1000.0,
    "maintenanceFee" DOUBLE PRECISION DEFAULT 100.0,
    "securityDeposit" DOUBLE PRECISION DEFAULT 500.0,
    discount DOUBLE PRECISION DEFAULT 0.0,
    "seatType" TEXT DEFAULT 'Standard',
    "shiftId" TEXT DEFAULT '',
    facilities TEXT DEFAULT 'High-Speed Wi-Fi, RO Water, Charging Socket, Silent AC',
    "renewalRules" TEXT DEFAULT 'Grace period of 3 days before seat release',
    "isActive" BOOLEAN DEFAULT true
);

CREATE TABLE IF NOT EXISTS public.seats (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
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
    "gridRow" INTEGER DEFAULT 1,
    "gridCol" INTEGER DEFAULT 1,
    "floorZone" TEXT DEFAULT 'General Study Zone'
);

-- ---------------------------------------------------------------------
-- 6. Students & Enrolments
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.students (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    "userId" TEXT DEFAULT '',
    "studentCode" TEXT NOT NULL,
    "fullName" TEXT NOT NULL,
    "photoUrl" TEXT DEFAULT '',
    mobile TEXT NOT NULL,
    email TEXT DEFAULT '',
    dob TEXT DEFAULT '',
    gender TEXT DEFAULT 'Male',
    address TEXT DEFAULT '',
    "parentName" TEXT DEFAULT '',
    "parentMobile" TEXT DEFAULT '',
    "courseClass" TEXT DEFAULT '',
    college TEXT DEFAULT '',
    "targetExam" TEXT DEFAULT 'UPSC CSE',
    "targetExamDate" TEXT DEFAULT '2026-05-24',
    category TEXT DEFAULT 'General',
    batch TEXT DEFAULT 'Morning Regular',
    "planId" TEXT DEFAULT '',
    "planName" TEXT DEFAULT '',
    "shiftId" TEXT DEFAULT '',
    "shiftName" TEXT DEFAULT '',
    "seatId" TEXT DEFAULT '',
    "seatNumber" TEXT DEFAULT '',
    "hallName" TEXT DEFAULT '',
    "joiningDate" TEXT DEFAULT '',
    "expiryDate" TEXT DEFAULT '',
    "totalFee" DOUBLE PRECISION DEFAULT 1000.0,
    discount DOUBLE PRECISION DEFAULT 0.0,
    "paidAmount" DOUBLE PRECISION DEFAULT 1000.0,
    "dueAmount" DOUBLE PRECISION DEFAULT 0.0,
    status TEXT DEFAULT 'ACTIVE',
    "rfidQrCode" TEXT DEFAULT '',
    "emergencyContact" TEXT DEFAULT '',
    password TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

CREATE TABLE IF NOT EXISTS public.seat_assignments (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    "seatId" TEXT NOT NULL,
    "seatNumber" TEXT NOT NULL,
    "studentId" TEXT NOT NULL REFERENCES public.students(id) ON DELETE CASCADE,
    "studentName" TEXT NOT NULL,
    "shiftId" TEXT DEFAULT '',
    "shiftName" TEXT DEFAULT '',
    "startDate" TEXT NOT NULL,
    "endDate" TEXT NOT NULL,
    "planId" TEXT DEFAULT '',
    status TEXT DEFAULT 'ACTIVE',
    notes TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

-- ---------------------------------------------------------------------
-- 7. QR Attendance & Logs (Used by CameraX & ZXing Attendance)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.attendance (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    "studentId" TEXT NOT NULL,
    "studentName" TEXT NOT NULL,
    "seatId" TEXT DEFAULT '',
    "seatNumber" TEXT DEFAULT '',
    "hallId" TEXT DEFAULT '',
    "hallName" TEXT DEFAULT '',
    "shiftId" TEXT DEFAULT '',
    "shiftName" TEXT DEFAULT '',
    date TEXT NOT NULL,
    "checkInTime" TEXT NOT NULL,
    "checkOutTime" TEXT DEFAULT '',
    "durationMinutes" INTEGER DEFAULT 0,
    status TEXT DEFAULT 'CHECKED_IN',
    mode TEXT DEFAULT 'QR_SCAN',
    notes TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint,
    timestamp BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

CREATE INDEX IF NOT EXISTS idx_attendance_lib_date ON public.attendance("libraryId", date);
CREATE INDEX IF NOT EXISTS idx_attendance_student_date ON public.attendance("studentId", date);

-- ---------------------------------------------------------------------
-- 8. Library Catalog & Books
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.physical_books (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
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
    "totalCopies" INTEGER DEFAULT 1,
    "availableCopies" INTEGER DEFAULT 1,
    "issuedCopies" INTEGER DEFAULT 0,
    "coverUrl" TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint,
    "updatedAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

CREATE TABLE IF NOT EXISTS public.book_issues (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    "bookId" TEXT NOT NULL REFERENCES public.physical_books(id) ON DELETE CASCADE,
    "bookTitle" TEXT NOT NULL,
    "studentId" TEXT NOT NULL REFERENCES public.students(id) ON DELETE CASCADE,
    "studentName" TEXT NOT NULL,
    "studentMobile" TEXT DEFAULT '',
    "issueDate" TEXT NOT NULL,
    "dueDate" TEXT NOT NULL,
    "returnDate" TEXT DEFAULT '',
    "fineAmount" DOUBLE PRECISION DEFAULT 0.0,
    "finePaid" BOOLEAN DEFAULT false,
    status TEXT DEFAULT 'ISSUED',
    notes TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

CREATE TABLE IF NOT EXISTS public.digital_materials (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
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
    "downloadCount" INTEGER DEFAULT 0,
    "uploadDate" TEXT DEFAULT '',
    "isBookmarked" BOOLEAN DEFAULT false,
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint,
    "updatedAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

-- ---------------------------------------------------------------------
-- 9. Finance (Payments, Expenses, Fines)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.payments (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
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
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

CREATE TABLE IF NOT EXISTS public.expenses (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    category TEXT NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    date TEXT NOT NULL,
    description TEXT DEFAULT '',
    "paymentMode" TEXT DEFAULT 'UPI',
    status TEXT DEFAULT 'PAID',
    "receiptRef" TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

CREATE TABLE IF NOT EXISTS public.fines (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    "studentId" TEXT NOT NULL,
    "studentName" TEXT NOT NULL,
    "bookId" TEXT DEFAULT '',
    "bookTitle" TEXT DEFAULT '',
    reason TEXT DEFAULT 'Late Book Return',
    amount DOUBLE PRECISION DEFAULT 25.0,
    paid BOOLEAN DEFAULT false,
    date TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

-- ---------------------------------------------------------------------
-- 10. Communication, Notices & Complaints
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.notices (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    category TEXT DEFAULT 'GENERAL',
    priority TEXT DEFAULT 'NORMAL',
    date TEXT NOT NULL,
    "targetAudience" TEXT DEFAULT 'ALL',
    "senderName" TEXT DEFAULT 'LibDesk Admin',
    "senderId" TEXT DEFAULT '',
    "isGlobal" BOOLEAN DEFAULT false,
    "isActive" BOOLEAN DEFAULT true,
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint,
    "updatedAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

CREATE TABLE IF NOT EXISTS public.feedback_complaints (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    "studentId" TEXT NOT NULL,
    "studentName" TEXT NOT NULL,
    "seatId" TEXT DEFAULT '',
    "seatNumber" TEXT DEFAULT '',
    type TEXT DEFAULT 'COMPLAINT',
    subject TEXT NOT NULL,
    message TEXT NOT NULL,
    status TEXT DEFAULT 'PENDING',
    reply TEXT DEFAULT '',
    date TEXT NOT NULL,
    "resolvedDate" TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

CREATE TABLE IF NOT EXISTS public.chat_messages (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    "studentId" TEXT NOT NULL,
    "studentName" TEXT DEFAULT '',
    "senderRole" TEXT DEFAULT 'STUDENT',
    "senderName" TEXT DEFAULT '',
    message TEXT NOT NULL,
    timestamp BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint,
    "timeFormatted" TEXT DEFAULT '',
    "dateFormatted" TEXT DEFAULT '',
    status TEXT DEFAULT 'SENT',
    "isRead" BOOLEAN DEFAULT false
);

CREATE TABLE IF NOT EXISTS public.audit_logs (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "userId" TEXT DEFAULT '',
    "userName" TEXT DEFAULT '',
    "performedBy" TEXT DEFAULT '',
    role TEXT DEFAULT 'OWNER',
    action TEXT NOT NULL,
    "recordType" TEXT NOT NULL,
    "recordId" TEXT NOT NULL,
    details TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint,
    timestamp BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

-- ---------------------------------------------------------------------
-- 11. SaaS Subscriptions & Platform Management
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.subscription_plans (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    "durationMonths" INTEGER NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    "maxSeats" INTEGER NOT NULL,
    features TEXT NOT NULL,
    "isActive" BOOLEAN DEFAULT true,
    badge TEXT DEFAULT ''
);

CREATE TABLE IF NOT EXISTS public.library_subscriptions (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "libraryName" TEXT NOT NULL,
    "planId" TEXT NOT NULL,
    "planName" TEXT NOT NULL,
    status TEXT DEFAULT 'ACTIVE',
    "startDate" TEXT NOT NULL,
    "expiryDate" TEXT NOT NULL,
    "durationDays" INTEGER DEFAULT 30,
    "durationUnit" TEXT DEFAULT 'MONTHS',
    price DOUBLE PRECISION NOT NULL,
    discount DOUBLE PRECISION DEFAULT 0.0,
    "maxSeats" INTEGER DEFAULT 100,
    "autoRenew" BOOLEAN DEFAULT true,
    notes TEXT DEFAULT '',
    "updatedAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

CREATE TABLE IF NOT EXISTS public.user_subscriptions (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "planId" TEXT NOT NULL,
    status TEXT DEFAULT 'ACTIVE',
    "expiryDate" TEXT NOT NULL,
    "updatedAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

-- ---------------------------------------------------------------------
-- 12. Lockers, Visitor Passes, Requests, Events & Streaks
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.lockers (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    "lockerNumber" TEXT NOT NULL,
    size TEXT DEFAULT 'Standard',
    floor TEXT DEFAULT 'Ground Floor',
    "monthlyRent" DOUBLE PRECISION DEFAULT 200.0,
    "depositAmount" DOUBLE PRECISION DEFAULT 300.0,
    status TEXT DEFAULT 'AVAILABLE',
    "assignedStudentId" TEXT DEFAULT '',
    "assignedStudentName" TEXT DEFAULT '',
    "assignedStudentPhone" TEXT DEFAULT '',
    "startDate" TEXT DEFAULT '',
    "expiryDate" TEXT DEFAULT '',
    "keyNumber" TEXT DEFAULT '',
    notes TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

CREATE TABLE IF NOT EXISTS public.visitor_passes (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    "passNumber" TEXT NOT NULL,
    "visitorName" TEXT NOT NULL,
    mobile TEXT NOT NULL,
    purpose TEXT DEFAULT '1-Day Demo / Trial',
    "idProofType" TEXT DEFAULT 'Aadhar Card',
    "idProofNumber" TEXT DEFAULT '',
    "visitDate" TEXT NOT NULL,
    "timeSlot" TEXT DEFAULT 'Full Day (8 AM - 8 PM)',
    "assignedSeatNumber" TEXT DEFAULT 'Temp-01',
    "feeAmount" DOUBLE PRECISION DEFAULT 100.0,
    "paymentMode" TEXT DEFAULT 'CASH',
    status TEXT DEFAULT 'ACTIVE',
    "checkInTime" TEXT DEFAULT '',
    "checkOutTime" TEXT DEFAULT '',
    notes TEXT DEFAULT '',
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

CREATE TABLE IF NOT EXISTS public.seat_shift_requests (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    "studentId" TEXT NOT NULL REFERENCES public.students(id) ON DELETE CASCADE,
    "studentName" TEXT NOT NULL,
    "studentPhone" TEXT DEFAULT '',
    "currentSeatNumber" TEXT NOT NULL,
    "currentShiftName" TEXT NOT NULL,
    "requestedSeatNumber" TEXT DEFAULT '',
    "requestedShiftId" TEXT DEFAULT '',
    "requestedShiftName" TEXT DEFAULT '',
    "requestType" TEXT DEFAULT 'SEAT_AND_SHIFT',
    reason TEXT DEFAULT '',
    status TEXT DEFAULT 'PENDING',
    "adminRemarks" TEXT DEFAULT '',
    "requestedAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint,
    "reviewedAt" BIGINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS public.study_streaks (
    "studentId" TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL,
    "studentName" TEXT DEFAULT '',
    "streakDays" INTEGER DEFAULT 1,
    "totalMinutesToday" INTEGER DEFAULT 0,
    "totalStudyMinutesAllTime" INTEGER DEFAULT 0,
    "lastStudyDate" TEXT DEFAULT '',
    "dailyTargetMinutes" INTEGER DEFAULT 360,
    "longestStreakDays" INTEGER DEFAULT 1,
    "totalSessions" INTEGER DEFAULT 0,
    "unlockedBadges" TEXT DEFAULT 'ROOKIE_SCHOLAR'
);

CREATE TABLE IF NOT EXISTS public.floor_elements (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    "hallId" TEXT DEFAULT 'MAIN_HALL',
    type TEXT NOT NULL,
    label TEXT NOT NULL,
    "gridX" INTEGER NOT NULL,
    "gridY" INTEGER NOT NULL,
    width INTEGER DEFAULT 1,
    height INTEGER DEFAULT 1,
    "seatNumberRef" TEXT DEFAULT '',
    "isOccupied" BOOLEAN DEFAULT false
);

CREATE TABLE IF NOT EXISTS public.library_events (
    id TEXT PRIMARY KEY,
    "libraryId" TEXT NOT NULL REFERENCES public.libraries(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT DEFAULT 'WORKSHOP',
    date TEXT NOT NULL,
    time TEXT NOT NULL,
    venue TEXT DEFAULT 'Seminar Hall / Main Zone',
    "speakerOrOrganizer" TEXT DEFAULT 'Library Academic Team',
    "maxParticipants" INTEGER DEFAULT 50,
    "registeredStudentIds" JSONB DEFAULT '[]'::jsonb,
    "isOnline" BOOLEAN DEFAULT false,
    "meetingLink" TEXT DEFAULT '',
    "bannerEmoji" TEXT DEFAULT '📚',
    "isFeatured" BOOLEAN DEFAULT true,
    "createdAt" BIGINT DEFAULT (extract(epoch from now()) * 1000)::bigint
);

-- ---------------------------------------------------------------------
-- 13. Initial SaaS Subscription Plans Seed
-- ---------------------------------------------------------------------
INSERT INTO public.subscription_plans (id, name, "durationMonths", price, "maxSeats", features, "isActive", badge)
VALUES
  ('PLAN-TRIAL', 'Free Starter Trial', 1, 0.0, 30, 'Seat Layout, Basic Attendance, WhatsApp Receipts, 1 Branch', true, 'TRIAL'),
  ('PLAN-SILVER', 'Silver Growth Plan', 3, 1499.0, 75, 'Full QR Scanner Check-in, SMS/WhatsApp Alerts, Expense Tracker', true, 'POPULAR'),
  ('PLAN-GOLD', 'Gold Pro Plan', 6, 2699.0, 150, 'RFID/Barcode, GPS Geo-fenced Gate QR, Multi-shift Scheduler', true, 'BEST VALUE'),
  ('PLAN-PLATINUM', 'Platinum Enterprise', 12, 4999.0, 500, 'Unlimited Seats, Custom Student Portal, Dedicated Support', true, 'ENTERPRISE')
ON CONFLICT (id) DO NOTHING;

-- ---------------------------------------------------------------------
-- 14. Realtime Publication
-- ---------------------------------------------------------------------
DO $$
BEGIN
  ALTER PUBLICATION supabase_realtime ADD TABLE 
    public.libraries,
    public.attendance,
    public.students,
    public.seats,
    public.seat_assignments,
    public.chat_messages,
    public.notices,
    public.feedback_complaints,
    public.seat_shift_requests,
    public.payments,
    public.library_subscriptions;
EXCEPTION
  WHEN duplicate_object THEN NULL;
  WHEN undefined_object THEN NULL;
END $$;
