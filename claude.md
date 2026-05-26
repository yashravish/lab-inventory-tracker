You are Claude Code acting as a senior full-stack engineer. Build a complete, runnable, interview-ready project called Lab Inventory Tracker for a Java Full Stack Developer role at DeltaSoft Inc.
The app is a small scientific lab inventory system inspired by reagent inventory and R&D workflow software. It tracks reagents (name, supplier, quantity, unit, storage location, expiration date, minimum quantity) and exposes a React frontend, Spring Boot REST API, PostgreSQL persistence, a Java Swing admin viewer, and automated tests. Keep it simple enough to run locally and demo tomorrow.
Operating Principles

Do not ask follow-up questions. Make reasonable choices.
Favor simple, working code over complexity.
After writing code, run the tests yourself. Fix failures. Do not stop until backend and frontend test suites pass.
At the end, print clear run commands.

Tech Stack

Frontend: React + TypeScript + Vite
Backend: Java 17+ (or 21), Spring Boot, Spring Web, Spring Data JPA, Validation
Database: PostgreSQL (runtime), H2 (tests)
Desktop: Java Swing admin viewer
Testing: JUnit 5 + Spring Boot Test + MockMvc + H2 (backend); Vitest + React Testing Library (frontend); one Playwright E2E test if setup is light
Build: Maven (backend, swing-admin), npm (frontend)
CI: GitHub Actions running backend + frontend tests

Project Structure
lab-inventory-tracker/
  backend/
  frontend/
  swing-admin/
  .github/workflows/ci.yml
  README.md
Backend
Entity: Reagent
FieldTypeNotesidLongnameStringrequiredsupplierStringquantityBigDecimalrequired, >= 0unitStringe.g. g, mL, L, mgstorageLocationStringexpirationDateLocalDateminimumQuantityBigDecimaldefault 5, >= 0
Computed fields in API responses

lowStock — true when quantity <= minimumQuantity
expired — true when expirationDate is before today
status — "EXPIRED" if expired, else "LOW_STOCK" if low stock, else "IN_STOCK" (expired takes precedence)

Endpoints

GET /api/reagents — list all; optional search query param filters by name, supplier, or storageLocation
GET /api/reagents/{id}
POST /api/reagents
PUT /api/reagents/{id}
DELETE /api/reagents/{id}
GET /api/dashboard/summary — returns totalReagents, lowStockCount, expiredCount, inStockCount

Implementation Notes

Use DTOs if it keeps things clean, but don't over-abstract.
Add validation and useful error responses.
Configure CORS for the frontend.
Use application.properties (or yml) for PostgreSQL; separate profile/config for H2 in tests.
Provide a docker-compose.yml (in backend/ or repo root) for PostgreSQL.
Skip actuator unless trivial to add.

Seed Data (at least 8 reagents, mix of normal, low-stock, expired)
Sodium Chloride, Ethanol, Acetone, Hydrochloric Acid, Methanol, Glucose, Buffer Solution, Agarose.
Backend Tests

GET /api/reagents returns seeded or test-created data.
POST /api/reagents creates a valid reagent.
POST /api/reagents rejects negative quantity.
PUT /api/reagents/{id} updates quantity.
DELETE /api/reagents/{id} removes the reagent.
Dashboard summary returns correct counts.
Status logic:

expired reagent → EXPIRED
low-quantity reagent → LOW_STOCK
healthy reagent → IN_STOCK



Frontend
React + TypeScript + Vite.
Components

App
DashboardCards
ReagentTable
ReagentForm
SearchBar
API client module

UI

Plain CSS or simple CSS modules. No heavy UI libraries.
Header title: Lab Inventory Tracker
Subtitle: Simple reagent inventory system for scientific R&D workflows
Dashboard cards: Total Reagents, Low Stock, Expired, In Stock
Table columns: Name, Supplier, Quantity, Unit, Storage Location, Expiration Date, Status, Actions (Edit, Delete)
Form fields: name, supplier, quantity, unit, storageLocation, expirationDate, minimumQuantity
Search bar filters via backend search query param

Behavior

On load, fetch dashboard summary and reagent list.
Support add/edit/delete; refresh dashboard after each.
Loading and basic error states.
TypeScript interfaces for Reagent, ReagentInput, DashboardSummary.
Backend base URL in an env var; default http://localhost:8080.

Frontend Tests (Vitest + RTL, mock fetch/API client)

App renders title.
Dashboard cards render when API is mocked.
Reagent table renders mocked reagents.
Form submits a new reagent.
Delete button calls delete API.

Swing Admin Viewer (swing-admin/)
Lightweight internal admin consuming the same REST API. Read-only.

Window title: Lab Inventory Admin Viewer
JTable columns: ID, Name, Supplier, Quantity, Unit, Storage Location, Expiration Date, Status
Refresh button → GET /api/reagents
Search box + button → GET /api/reagents?search=value
Use java.net.http.HttpClient; Jackson for JSON (preferred if Maven setup is easy).
Runnable via Maven.
README section explaining how to run.
No create/edit/delete in Swing.

Swing Tests
Add a small unit test for JSON parsing or the table model if practical. At minimum, ensure it compiles with Maven.
CI — .github/workflows/ci.yml

Triggers: push, pull_request
Set up Java → run backend tests with Maven
Set up Node → install frontend deps → run frontend tests
Optionally compile swing-admin with Maven

README
Polished README.md covering:

Project title
Description: A simple scientific lab inventory tracker inspired by reagent inventory and R&D data-management workflows.
Why this is relevant to DeltaSoft: DeltaSoft builds scientific/R&D workflow software. This project focuses on reagent inventory, lab data tracking, CRUD, REST APIs, PostgreSQL, React, TypeScript, Java, and Swing.
Tech stack
Features
Architecture diagram (text):

   React Frontend     ─┐
                       ├─► Spring Boot REST API ─► PostgreSQL
   Swing Admin Viewer ─┘

API endpoints
Database schema
How to run backend
How to run frontend
How to run Swing admin viewer
How to run tests
Demo script: open dashboard → show total/low-stock/expired counts → add a reagent → edit quantity to trigger low-stock → delete a reagent → open Swing viewer and refresh
Interview pitch: "I built this because DeltaSoft works in scientific R&D software, and I wanted a project directly connected to reagent inventory and lab workflow management while using the role's stack: React, TypeScript, Java, PostgreSQL, REST APIs, and Swing."
Screenshot placeholders

Build Order

Scaffold all folders and files.
Implement backend → write tests → run tests → fix failures.
Implement frontend → write tests → run tests → fix failures.
Implement Swing admin viewer → compile.
Add CI workflow.
Write README.
Final verification: backend tests pass, frontend tests pass, Swing project compiles.
Print the final run commands. This section is not optional polish — follow it as strictly as the functional requirements. The goal is a UI that looks like real LIMS software (LabWare, CDD Vault, SciSure, LABWORKS), not a generic AI-generated dashboard.
Anti-patterns — do NOT do any of these
Real lab software is built for technicians who stare at it for eight hours. It is information-dense, restrained, and slightly utilitarian. Avoid the default "AI dashboard" look:

❌ No purple/indigo gradients, no gradient buttons, no gradient backgrounds anywhere.
❌ No glassmorphism, no backdrop-filter: blur, no translucent cards floating over blurred shapes.
❌ No oversized rounded corners. Cap border-radius at 6px. Buttons and inputs: 4px. Status pills: 3px or fully rounded (9999px) — pick one and stick with it.
❌ No emoji icons (🧪⚗️🔬). If you need icons, use Lucide React outline icons at 16px, single stroke weight, currentColor.
❌ No drop shadows on cards beyond 0 1px 2px rgba(0,0,0,0.04). No shadow-2xl, no glow effects.
❌ No hero sections, no large marketing-style headers, no centered single-column layouts with huge whitespace.
❌ No "✨ AI-powered" language anywhere. No motivational microcopy ("Let's get started!", "You're all set!").
❌ No full-bleed colored backgrounds on cards. Cards are white (or --surface) with a 1px border.
❌ No font sizes above 20px except the app title (24px). Most UI text is 13–14px. Table cells are 13px.
❌ No font-family of Inter, Geist, or anything trendy. Use the system stack (below).
❌ No animations beyond a 120ms opacity/background transition on hover. No spring physics, no slide-ins, no Framer Motion.

Visual language
Palette — neutral, low-saturation. Define as CSS variables on :root:
css--bg:            #f6f7f8;   /* app background */
--surface:       #ffffff;   /* cards, table, form */
--surface-alt:   #fafbfc;   /* table header, zebra rows */
--border:        #e4e7eb;   /* all borders, dividers */
--border-strong: #cbd2d9;   /* input borders, focus-adjacent */
--text:          #1f2933;   /* primary text */
--text-muted:    #616e7c;   /* secondary text, labels */
--text-faint:    #9aa5b1;   /* placeholder, table meta */
--accent:        #1e5fbf;   /* primary action, links — a flat, slightly desaturated blue */
--accent-hover:  #174a96;

/* Status colors — muted, NOT candy-bright */
--ok-bg:    #e8f3ec;  --ok-fg:    #1f6b3a;   /* IN_STOCK */
--warn-bg:  #fdf3e0;  --warn-fg:  #8a5a00;   /* LOW_STOCK */
--err-bg:   #fbe9e9;  --err-fg:   #9b2c2c;   /* EXPIRED */
Typography — system stack, no web fonts:
cssfont-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
             "Helvetica Neue", Arial, sans-serif;
Numbers in the table (quantity, min qty) use font-variant-numeric: tabular-nums so columns line up. Use a monospace stack (ui-monospace, "SF Mono", Menlo, Consolas, monospace) for the ID column in the Swing/admin-style table if you expose IDs.
Spacing — 4px grid. Use 8/12/16/24 px almost exclusively. Tight, not airy.
Layout
Two-zone shell, not a centered card:

Top bar (full width, 56px tall, white, 1px bottom border)

Left: small square logo placeholder (24×24, just a filled --accent square with white "LI" text) + app title "Lab Inventory Tracker" in 15px semibold + small environment badge ("DEV" or "LOCAL") in a 11px uppercase pill.
Right: a static user chip (e.g. "yash.s · Lab Tech") — purely visual, no auth. Real LIMS always shows the logged-in user.
Subtitle ("Simple reagent inventory system…") goes in the README, not the top bar. Real software doesn't tagline itself.


Main content (max-width 1280px, padded 24px, left-aligned, NOT centered):

Page heading row: "Inventory" (18px semibold) on the left, primary action button "+ New Reagent" on the right. 16px below the top bar.
Dashboard summary strip
Filter/search bar
Reagent table



Dashboard cards
A horizontal strip of 4 compact stat tiles, not big colorful cards.

Equal width, 1px --border, white background, 16px padding, 6px radius.
Each tile: small uppercase label (11px, --text-muted, letter-spacing 0.04em) on top — e.g. TOTAL REAGENTS — then the number below (22px semibold, --text, tabular-nums).
Optional tiny delta or sub-label in 12px --text-faint ("across 4 locations", "expires within 30d", etc.).
Color-code only the value, not the whole tile: LOW STOCK number uses --warn-fg, EXPIRED uses --err-fg, IN STOCK uses --ok-fg, TOTAL stays --text.
Tiles are clickable and act as filters on the table below (clicking "LOW STOCK" filters to low-stock rows). Active filter tile gets a 2px --accent left border.

Reagent table — this is the centerpiece
Real LIMS UIs are table-first. Make this dense and serious.

Full-width inside the content area. White surface, 1px --border, 6px radius, overflow: hidden.
Toolbar row above the table (inside the same bordered surface, with a bottom border separating it from the header):

Search input on the left (28px tall, 240px wide, placeholder "Search name, supplier, location…", small magnifier icon at 14px inside on the left).
A status filter dropdown ("All statuses ▾").
On the far right: a row count ("82 reagents") in 12px --text-muted.


Header row: --surface-alt background, 11px uppercase labels, --text-muted, letter-spacing 0.04em, 32px tall, 1px bottom border. Columns are sortable — show a small ▲/▼ caret next to the active sort column.
Body rows: 36px tall, 13px text, 1px bottom border between rows (no zebra striping — borders only). Hover background --surface-alt. No row click action; actions live in the Actions column.
Columns and alignment:

Name (left, --text, 500 weight)
Supplier (left, --text-muted)
Quantity (right, tabular-nums) — render as 12.50 mL with the unit in --text-muted next to the number, both in the same cell. Drop the separate "Unit" column from the table view and keep unit only in the form/detail; this matches how real LIMS displays amounts.
Storage Location (left, --text-muted, 12px) — render as a small monospace breadcrumb-style string like Lab-2 / Shelf-B / Cab-3 if multi-part, otherwise plain text.
Expiration (left, tabular-nums, format 2025-09-14 — ISO format, never "Sept 14, 2025"). If within 30 days of today, append a small · 12d in --warn-fg. If past, append · expired in --err-fg.
Status pill (see below)
Actions (right-aligned): two text buttons "Edit" and "Delete" in 12px --accent and --err-fg, no borders, no backgrounds. Separator dot between them.



Status pills
Small, flat, no shadow, no gradient. 3px radius, 11px text, 500 weight, uppercase, letter-spacing 0.04em, 4px vertical / 8px horizontal padding.
IN_STOCK   →  bg --ok-bg,   fg --ok-fg,   label "In stock"
LOW_STOCK  →  bg --warn-bg, fg --warn-fg, label "Low stock"
EXPIRED    →  bg --err-bg,  fg --err-fg,  label "Expired"
Display the label in sentence case ("Low stock") even though the underlying enum is LOW_STOCK.
Reagent form
Opens as a right-side drawer (420px wide, slides in over a 20%-opacity black overlay), not a centered modal. Real LIMS uses side panels so the table stays visible behind.

Drawer header: "New reagent" or "Edit reagent — Sodium Chloride", 15px semibold, 1px bottom border, 16px padding. Close × on the right.
Form body: single column, 16px padding. Labels above inputs, 12px --text-muted, 4px gap to the input.
Inputs: 32px tall, 13px text, 1px --border-strong, 4px radius, white background. Focus state is a 2px --accent outline (use outline, not box-shadow, so it doesn't pulse).
Required fields marked with a small * in --err-fg after the label.
Quantity and Minimum Quantity sit side-by-side (two columns) with Unit as a small dropdown attached to Quantity's right edge.
Expiration Date is a native <input type="date"> — do not pull in a date picker library.
Footer (sticky, 1px top border, 16px padding): "Cancel" (ghost button) on the left, "Save reagent" (--accent background, white text) on the right. Disabled state during submit shows the button text as "Saving…" — no spinner.

Buttons

Primary: --accent background, white text, 4px radius, 32px tall, 13px 500 weight, 12px horizontal padding. Hover: --accent-hover. No shadow.
Secondary / ghost: transparent background, 1px --border-strong, --text color, same dimensions.
Destructive inline: plain text in --err-fg, no background.
Never use icon-only buttons without a tooltip.

Empty, loading, and error states

Loading: show a subtle 2px progress bar in --accent along the top of the table surface — not a centered spinner. Table rows render as 6 shimmer placeholders (light gray blocks, no animation more elaborate than a 1.4s opacity fade between --surface-alt and --border).
Empty (no reagents): inside the table body, centered, two lines — "No reagents yet." in 14px --text, then "Add one to get started." in 12px --text-faint. Plus a single secondary "+ New reagent" button. No illustrations, no SVG mascots.
Empty (search returned 0): "No reagents match 'xyz'." with a "Clear search" link button.
Error: thin 1px red-bordered banner above the table, --err-bg background, --err-fg text, 12px padding, with the error message and a small "Retry" link on the right. Auto-dismiss after success.

Swing admin viewer styling
Even though it's Swing, apply the same restraint:

Use the system look-and-feel (UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())).
Window minimum size 1000×600.
Top toolbar with: search text field (200px wide), "Search" button, "Refresh" button, and a right-aligned "Last updated: HH:mm:ss" label.
JTable with alternating row colors disabled. Single 1px grid. Status column renders via a custom TableCellRenderer that paints a small filled rectangle (matching the web pill colors) behind the status text.
No emoji, no colored headers — just the OS-native table chrome.

Reference aesthetic
Aim for the visual register of LabWare LIMS, CDD Vault, or a Jira issue list — dense, neutral, tabular, slightly boring on purpose. If a screenshot of your UI could plausibly be mistaken for a Linear, Retool, or internal-tool admin panel, you're on target. If it looks like a Vercel landing page or a generic Tailwind starter, start over.