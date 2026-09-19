package com.example.data.remote


object SupabaseEdgeFunctionTemplate {

    
    fun getEdgeFunctionSourceCode(): String = """
// =========================================================================
// Supabase Edge Function: create-student-user
// Path: supabase/functions/create-student-user/index.ts
// Deploy: supabase functions deploy create-student-user --no-verify-jwt
// =========================================================================

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'POST, OPTIONS'
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // 1. Verify Caller JWT (Must be logged-in OWNER or ADMIN)
    const authHeader = req.headers.get('Authorization')
    if (!authHeader) {
      return new Response(JSON.stringify({ error: 'Missing Authorization header' }), {
        status: 401,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    const supabaseUrl = Deno.env.get('SUPABASE_URL') ?? ''
    const supabaseAnonKey = Deno.env.get('SUPABASE_ANON_KEY') ?? ''
    const supabaseServiceRoleKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''

    // Client using caller's JWT
    const callerClient = createClient(supabaseUrl, supabaseAnonKey, {
      global: { headers: { Authorization: authHeader } }
    })

    const { data: { user: callerUser }, error: callerError } = await callerClient.auth.getUser()
    if (callerError || !callerUser) {
      return new Response(JSON.stringify({ error: 'Unauthorized caller token' }), {
        status: 401,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    // Role check: Only OWNER or ADMIN can create students
    const callerRole = callerUser.app_metadata?.role || callerUser.user_metadata?.role
    if (callerRole !== 'OWNER' && callerRole !== 'ADMIN' && callerRole !== 'SUPER_ADMIN') {
      return new Response(JSON.stringify({ error: 'Forbidden: Insufficient privileges to create student accounts' }), {
        status: 403,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    // 2. Parse payload
    const { email, password, fullName, mobile, libraryId, studentCode } = await req.json()
    if (!email) {
      return new Response(JSON.stringify({ error: 'Email is required' }), {
        status: 400,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    const tempPassword = password || ("Lib@" + Math.random().toString(36).slice(-8))

    // 3. Admin Client with Service Role Key - creates user server-side
    // This leaves the Owner's client-side session completely untouched!
    const adminClient = createClient(supabaseUrl, supabaseServiceRoleKey)

    const { data: createdData, error: createError } = await adminClient.auth.admin.createUser({
      email: email.trim().toLowerCase(),
      password: tempPassword,
      email_confirm: true,
      user_metadata: {
        full_name: fullName || 'Student Member',
        mobile: mobile || '',
        student_code: studentCode || ''
      },
      app_metadata: {
        role: 'STUDENT',
        library_id: libraryId || ''
      }
    })

    if (createError) {
      return new Response(JSON.stringify({ error: createError.message }), {
        status: 400,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    return new Response(
      JSON.stringify({
        success: true,
        userId: createdData.user.id,
        email: createdData.user.email,
        temporaryPassword: tempPassword,
        role: 'STUDENT'
      }),
      {
        status: 200,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      }
    )
  } catch (err: any) {
    return new Response(JSON.stringify({ error: err.message || 'Internal Server Error' }), {
      status: 500,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })
  }
})
    """.trimIndent()

    
    fun getRlsPoliciesSql(): String = """
-- =========================================================================
-- Supabase Row Level Security (RLS) Policies
-- Enforces permissions using auth.jwt() -> 'app_metadata' ->> 'role'
-- Run this in Supabase Dashboard -> SQL Editor
-- =========================================================================

-- Helper function to extract user role securely from app_metadata
CREATE OR REPLACE FUNCTION auth.get_app_role()
RETURNS TEXT AS $$
  SELECT COALESCE(
    (auth.jwt() -> 'app_metadata' ->> 'role'),
    (auth.jwt() ->> 'role'),
    'ANON'
  );
$$ LANGUAGE sql STABLE;

-- 1. Enable RLS on all core tables
ALTER TABLE public.libraries ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.students ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.seats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.attendance ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;

-- 2. Drop legacy unconstrained policies if present
DROP POLICY IF EXISTS "Allow anon all libraries" ON public.libraries;
DROP POLICY IF EXISTS "Allow anon all students" ON public.students;
DROP POLICY IF EXISTS "Allow anon all seats" ON public.seats;
DROP POLICY IF EXISTS "Allow anon all attendance" ON public.attendance;
DROP POLICY IF EXISTS "Allow anon all payments" ON public.payments;

-- =========================================================================
-- TABLE: libraries
-- =========================================================================
-- Owner: Full control
CREATE POLICY "owner_full_libraries"
ON public.libraries
FOR ALL
TO authenticated
USING (auth.get_app_role() IN ('OWNER', 'SUPER_ADMIN'));

-- Admin: Read-only access to their assigned library
CREATE POLICY "admin_select_libraries"
ON public.libraries
FOR SELECT
TO authenticated
USING (auth.get_app_role() = 'ADMIN');

-- Student: Read-only access to view their library details
CREATE POLICY "student_select_libraries"
ON public.libraries
FOR SELECT
TO authenticated
USING (auth.get_app_role() = 'STUDENT');

-- =========================================================================
-- TABLE: students
-- =========================================================================
-- Owner & Admin: Full student record management
CREATE POLICY "owner_admin_manage_students"
ON public.students
FOR ALL
TO authenticated
USING (auth.get_app_role() IN ('OWNER', 'ADMIN', 'SUPER_ADMIN'))
WITH CHECK (auth.get_app_role() IN ('OWNER', 'ADMIN', 'SUPER_ADMIN'));

-- Student: Can view only their own record
CREATE POLICY "student_view_own_profile"
ON public.students
FOR SELECT
TO authenticated
USING (
  auth.get_app_role() = 'STUDENT' AND 
  (email = (auth.jwt() ->> 'email') OR id = (auth.jwt() -> 'app_metadata' ->> 'student_id'))
);

-- =========================================================================
-- TABLE: seats
-- =========================================================================
-- Owner & Admin: Manage seats layout and assignments
CREATE POLICY "owner_admin_manage_seats"
ON public.seats
FOR ALL
TO authenticated
USING (auth.get_app_role() IN ('OWNER', 'ADMIN', 'SUPER_ADMIN'))
WITH CHECK (auth.get_app_role() IN ('OWNER', 'ADMIN', 'SUPER_ADMIN'));

-- Student: View seats
CREATE POLICY "student_view_seats"
ON public.seats
FOR SELECT
TO authenticated
USING (true);

-- =========================================================================
-- TABLE: attendance
-- =========================================================================
-- Owner & Admin: Full management of attendance logs
CREATE POLICY "owner_admin_manage_attendance"
ON public.attendance
FOR ALL
TO authenticated
USING (auth.get_app_role() IN ('OWNER', 'ADMIN', 'SUPER_ADMIN'))
WITH CHECK (auth.get_app_role() IN ('OWNER', 'ADMIN', 'SUPER_ADMIN'));

-- Student: View and insert their own attendance
CREATE POLICY "student_own_attendance_select"
ON public.attendance
FOR SELECT
TO authenticated
USING (
  auth.get_app_role() = 'STUDENT' AND 
  studentId = (auth.jwt() -> 'app_metadata' ->> 'student_id')
);

CREATE POLICY "student_own_attendance_insert"
ON public.attendance
FOR INSERT
TO authenticated
WITH CHECK (
  auth.get_app_role() = 'STUDENT' AND 
  studentId = (auth.jwt() -> 'app_metadata' ->> 'student_id')
);

-- =========================================================================
-- TABLE: payments
-- =========================================================================
-- Owner: Full financial management
CREATE POLICY "owner_manage_payments"
ON public.payments
FOR ALL
TO authenticated
USING (auth.get_app_role() IN ('OWNER', 'SUPER_ADMIN'))
WITH CHECK (auth.get_app_role() IN ('OWNER', 'SUPER_ADMIN'));

-- Admin: Record payments & view branch records
CREATE POLICY "admin_manage_payments"
ON public.payments
FOR ALL
TO authenticated
USING (auth.get_app_role() = 'ADMIN')
WITH CHECK (auth.get_app_role() = 'ADMIN');

-- Student: View only their own receipts
CREATE POLICY "student_view_own_receipts"
ON public.payments
FOR SELECT
TO authenticated
USING (
  auth.get_app_role() = 'STUDENT' AND 
  studentId = (auth.jwt() -> 'app_metadata' ->> 'student_id')
);

-- =========================================================================
-- Trigger to elevate user metadata role to app_metadata on signup
-- =========================================================================
CREATE OR REPLACE FUNCTION public.elevate_user_role()
RETURNS TRIGGER AS $$
BEGIN
  NEW.raw_app_meta_data = jsonb_set(
    COALESCE(NEW.raw_app_meta_data, '{}'::jsonb),
    '{role}',
    COALESCE(NEW.raw_user_meta_data->'role', '"STUDENT"'::jsonb)
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  BEFORE INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.elevate_user_role();
    """.trimIndent()
}
