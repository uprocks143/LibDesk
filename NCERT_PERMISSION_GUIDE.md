# NCERT Copyright & Official Permission Guide

## 1. Legal Framework & Copyright Policy (India)

Under the **National Council of Educational Research and Training (NCERT)** policy and the **Copyright Act, 1957 (India)**:
* **Textbook Copyright**: All textbooks, syllabus documents, and learning materials created by NCERT are copyrighted property of the National Council of Educational Research and Training, Government of India.
* **No Re-hosting Allowed**: Third-party commercial or SaaS platforms are **strictly prohibited** from downloading, caching, re-hosting, or distributing NCERT textbook PDFs from private cloud storage buckets (such as AWS S3, Supabase Storage, or Firebase Storage).
* **Direct Linking Permitted**: Educational apps and library software may **provide direct, unaltered links** to official government repositories (`https://ncert.nic.in`, `https://epathshala.nic.in`, and `https://diksha.gov.in`) provided explicit attribution is maintained and no fee is charged for accessing the official NCERT books.

LibDesk complies with this rule 100% by:
1. **Never storing NCERT PDFs in Supabase Storage**.
2. Storing only metadata (class, subject, title, edition year) in PostgreSQL.
3. Enabling students to download textbooks directly from official government servers (`ncert.nic.in`) to their own Android device storage.

---

## 2. Steps to Request Formal Written Permission from NCERT

For long-term commercial SaaS or institutional operations, send a formal written permission letter to the NCERT Publication Division:

### Contact Details:
* **Addressee**: The Head, Publication Division, National Council of Educational Research and Training (NCERT)
* **Office Address**: Sri Aurobindo Marg, New Delhi – 110016, India
* **Official Email**: `pd.ncert@nic.in`, `secretary.ncert@nic.in`, `epathshala.ncert@nic.in`
* **Website**: https://ncert.nic.in

### Email Template:

```text
Subject: Request for Permission to Link Official NCERT Digital Textbooks in Educational Library SaaS App (LibDesk)

Respected Head of Publication Division,
NCERT, New Delhi,

We are developing LibDesk, an academic study library management application designed to support students and reading libraries across India.

In accordance with NCERT's digital policy, we wish to formally inform and seek written acknowledgment for providing direct links to official NCERT textbooks (hosted on ncert.nic.in, ePathshala, and DIKSHA) within our student interface.

Key Architectural Guarantees:
1. No Re-Hosting: LibDesk does NOT upload, store, or cache NCERT PDFs on any cloud server or database.
2. Direct Government Server Connection: All downloads originate directly from the official NCERT portal (ncert.nic.in) to the student's personal mobile device.
3. No Commercial Fee for NCERT Books: Access to NCERT digital textbooks is provided 100% free of charge to all enrolled students.
4. Mandatory Copyright Attribution: Every textbook listing and reading view displays clear attribution: "Source: NCERT (ncert.nic.in). All rights reserved by NCERT, Government of India."

Please find our app details and organizational credentials attached. We look forward to your formal acknowledgment.

Warm regards,
LibDesk Academic Operations
Contact: contact@libdesk.in | +91-XXXXXXXXXX
```

---

## 3. Mandatory In-App Attribution Requirements

Whenever displaying NCERT resources, the application must display:
```
"Books sourced from NCERT (ncert.nic.in), DIKSHA, and ePathshala.
Links provided with permission. All rights reserved by NCERT."
```
In LibDesk, this is rendered via `NcertAttributionFooter.kt` and pinned to the footer of both the **NcertCatalogScreen** and the **NcertReaderScreen**.

---

## 4. NEP 2020 Edition Transition Schedule (2026-27 Context)

As part of the National Education Policy (NEP 2020) syllabus rationalization:
* **Classes 1 to 8**: Revised editions released and currently valid.
* **Class 9**: Revised NEP textbooks introduced for the **2026-27** academic session.
* **Classes 10 & 11**: Current editions remain valid for **2026-27**; new revised textbooks will be introduced in **2027-28**.
* **Class 12**: Current rationalized editions valid.
