# WSIMS — Web-Based School Information Management System
### SISA · South International School Academy — Spring Boot starter project

This is the **base skeleton** for your semester project (`main` branch). It already has:

- ✅ Spring Boot 3 + Java 17, Maven project, ready to run
- ✅ Spring Security login (BCrypt passwords, role-based access per report §6.1)
- ✅ Spring Data JPA + H2 in-memory database (zero setup — swap to MySQL later, see below)
- ✅ `User`, `Student`, `Teacher`, `Parent`, `Registrar` entities with the ID formats from
  the report (`S2600001`, `T2600001`, `P2600001`, `R2600001`)
- ✅ A permanent, un-deletable Principal account, auto-seeded on first run
- ✅ A full custom UI: a premium login page built around a real campus photo and the SISA
  brand, sidebar + top bar shell, and a dashboard shell for each of the 5 roles — all in the
  official SISA colour palette (`#0B2E4F` navy / `#DCE7EE` azure / `#E8843C` orange)
- ✅ Dark mode toggle (persisted per browser), a mobile off-canvas sidebar with hamburger
  menu, a reusable toast-notification helper, a "My Profile" page, friendly placeholder
  pages for every not-yet-built module (instead of dead links / 404s), and a styled
  custom error page

None of the 8 module features (attendance, marks, timetable, etc.) are built yet on purpose —
that's the point of the branch workflow below. Each branch starts from this same clean base.

---

## 1. Run it right now

```bash
mvn spring-boot:run
```

Then open **[http://localhost:8080/login]()**

### Demo accounts

`DataSeeder` creates one full, pre-approved demo roster on every fresh boot — a whole
class's worth of Principal/Registrar/Teacher/Student/Parent logins, ready to use with no
manual registration or approval step. The login page has a small "Demo login" chip row in
the bottom-right corner (P / R / T / S / P) that one-click logs in as the *first* seeded
account of each role — this table is the full reference copy, including every extra
teacher/student/parent beyond that first one.

Every account's **username is its auto-generated ID** (not a chosen name) — IDs are
guaranteed unique, so this is true for every account in the system, not just the demo
roster: create a Teacher/Student/Parent/Registrar through the UI and you log in with the
ID shown on the success screen, e.g. `T2600005`. The Principal is the one fixed exception
(`principal`), since there is only ever one.

| Role | Name(s) | Username (= ID) | Password |
|---|---|---|---|
| Principal | Dr. Madhawa Gunasekara | `principal` | `Principal@123` |
| Registrar | Malinga Rodrigo | `R2600001` | `Registrar@123` |
| Registrar | Chandimal Senanayake | `R2600002` | `Registrar@123` |
| Teacher | Rashmika Jayawardena (Mathematics, Class Teacher of Grade 6 - A) | `T2600001` | `Teacher@123` |
| Teacher | Saduni Wickramasinghe (Science, Class Teacher of Grade 7 - A) | `T2600002` | `Teacher@123` |
| Teacher | Chamod Fernando (English) | `T2600003` | `Teacher@123` |
| Teacher | Harshana Peiris (History) | `T2600004` | `Teacher@123` |
| Student | Kasun Meepalagama .. Ushan Bandara (Grade 6 - A) | `S2600001` .. `S2600005` | `Student@123` |
| Student | Sayuri Karunaratne .. Vishmitha Abeysekera (Grade 7 - A) | `S2600006` .. `S2600010` | `Student@123` |
| Parent | one per student, same order | `P2600001` .. `P2600010` | `Parent@123` |

(Grade 6 - A order is Kasun Meepalagama, Venuwarshika Silva, Dewmini Fernando,
Thisarani Gunawardena, Ushan Bandara = `S2600001`-`S2600005`;
Grade 7 - A order is Sayuri Karunaratne, Habhishyani Dissanayake, Thushara Wijesinghe,
Senath Rathnayake, Vishmitha Abeysekera = `S2600006`-`S2600010`;
each student's guardian shares the same sequence number and family surname, e.g.
Kasun Meepalagama -> `S2600001` and his father Sunil Meepalagama -> `P2600001`.)

The H2 database console is at **http://localhost:8080/h2-console**
(JDBC URL: `jdbc:h2:mem:wsims`, user `sa`, empty password) — handy for checking your data
without installing MySQL.

> Requires JDK 17+ and Maven installed. In IntelliJ/Eclipse/VS Code you can also just
> right-click `WsimsApplication.java` → Run.

### Switching to MySQL for your real deployment
Edit `src/main/resources/application.yml` — comment out the H2 block and uncomment the
MySQL block at the bottom (the MySQL driver is already in `pom.xml`).

---

## 2. This zip = `main` branch only

This download is just the plain project on `main` — no git repo, no branches created
for you. Set those up yourself whenever you're ready:

```bash
git init
git add .
git commit -m "chore: WSIMS base skeleton (Spring Boot + Security + JPA + UI shell)"
```

Then create the 8 branches (all from `main`, so create them up front and everyone can
start immediately):

```bash
git branch feature/01-user-access-management
git branch feature/02-student-information
git branch feature/03-attendance-management
git branch feature/04-timetable-assignment
git branch feature/05-academic-management
git branch feature/06-communication-notification
git branch feature/07-administration-reporting
git branch feature/08-resources-facilities

git checkout feature/01-user-access-management
```

**Suggested build order** (matches the report's sprint plan, §2.3):
`01 → 02 → 03 → 04 → 05 → 06 → 07 → 08`, merging each into `main` via a pull request once
it's tested, so later modules can build on top of earlier ones (e.g. Attendance needs
Student Information to exist first).

Each branch has a matching, ready-to-paste prompt in **`WSIMS-Branch-Prompts.zip`**
(provided alongside this project) — hand that prompt to your AI coding assistant
(Claude Code, etc.) at the start of that branch, in this same repo, so it has the full
context of the entities and UI shell already in place.

---

## 3. Project layout

```
src/main/java/com/sisa/wsims/
 ├─ entity/       User, Student, Teacher, Parent, Registrar, Role
 ├─ repository/   Spring Data JPA repositories
 ├─ service/      IdGeneratorService (S/T/P/R id formats), WsimsUserDetailsService
 ├─ config/       SecurityConfig, DataSeeder (seeds the Principal + the full demo roster)
 └─ controller/   AuthController, DashboardController

src/main/resources/
 ├─ templates/
 │   ├─ login.html
 │   ├─ layout/sidebar.html, layout/topbar.html   (shared fragments)
 │   └─ dashboard/{principal,registrar,teacher,student,parent}.html
 ├─ static/css/styles.css      (design system: colours, cards, tables, forms)
 ├─ static/css/login.css       (login page: campus photo + glass card)
 ├─ static/images/logo.svg, static/images/sisa-campus.jpg
 └─ static/js/main.js
```

## 4. Design system quick reference

| Token | Value | Use |
|---|---|---|
| `--navy` | `#0B2E4F` | Top bar, headings, primary buttons |
| `--azure` | `#DCE7EE` | Sidebar, section backgrounds |
| `--orange` | `#E8843C` | Active nav item, CTA buttons, alerts |
| `--charcoal` | `#2E3A46` | Body text |
| Fonts | Inter (UI) / JetBrains Mono (ID badges) | loaded via Google Fonts in each page |

Reusable CSS classes: `.card`, `.grid.grid-3/.grid-4`, `.stat-card`, `.id-badge`,
`.wsims-table`, `.status.present/.absent/.late/.approved/.pending/.rejected`,
`.btn.btn-primary/.btn-outline`, `.field` (form rows), `.empty-state`.
Use these instead of inventing new styles, so all 8 modules look like one product.

## 5. What's deliberately NOT built yet
Attendance marking, marks/report cards, timetable, messaging, reporting exports,
and room booking are all left as empty-state placeholders in the dashboards above —
that's exactly what each module branch implements.
