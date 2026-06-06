package application;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", new RootHandler());
        server.createContext("/login", new LoginHandler());
        server.createContext("/dashboard", new DashboardHandler());
        server.createContext("/students", new StudentsHandler());
        server.createContext("/courses", new CoursesHandler());
        server.createContext("/faculty", new FacultyHandler());
        server.createContext("/logout", new LogoutHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("Server started on http://localhost:" + port);
    }

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (isAuthenticated(exchange)) {
                redirect(exchange, "/dashboard");
                return;
            }
            showLoginPage(exchange, "", false);
        }
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String requestBody = readRequestBody(exchange);
                Map<String, String> formData = parseForm(requestBody);
                String username = formData.getOrDefault("username", "").trim();
                String password = formData.getOrDefault("password", "").trim();

                if (authenticate(username, password)) {
                    exchange.getResponseHeaders().add("Set-Cookie", "UMSSESSION=active; HttpOnly; Path=/");
                    redirect(exchange, "/dashboard");
                    return;
                }
                showLoginPage(exchange, "Invalid username or password.", true);
                return;
            }
            showLoginPage(exchange, "", false);
        }

        private boolean authenticate(String username, String password) {
            return "admin".equals(username) && "University2026".equals(password);
        }
    }

    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!ensureAuth(exchange)) {
                return;
            }
            String content = buildPage("Dashboard", "dashboard", ""
                    + "<section class=\"hero\">"
                    + "<div>"
                    + "<p class=\"eyebrow\">Welcome back, Administrator</p>"
                    + "<h1>Unified Management System</h1>"
                    + "<p class=\"lead\">Manage students, course plans, faculty assignments, and campus operations from one secure dashboard.</p>"
                    + "</div>"
                    + "<div class=\"hero-actions\">"
                    + "<a class=\"btn btn-primary\" href=\"/students\">View Students</a>"
                    + "<a class=\"btn btn-secondary\" href=\"/courses\">View Courses</a>"
                    + "</div>"
                    + "</section>"
                    + "<section class=\"grid\">"
                    + card("Students enrolled", "312", "+12% this semester")
                    + card("Active faculty", "18", "Stable staffing")
                    + card("Courses offered", "24", "Updated schedules")
                    + card("Open tasks", "8", "Review assignments")
                    + "</section>"
                    + "<section class=\"panel\">"
                    + "<div class=\"panel-header\"><h2>Recent updates</h2></div>"
                    + "<ul class=\"activity\">"
                    + activityItem("New enrollment portal launched", "120 new applications received.")
                    + activityItem("Curriculum review", "Algorithms and Database Systems updated for 2026.")
                    + activityItem("Faculty meeting scheduled", "Next update on 2026-06-12.")
                    + "</ul>"
                    + "</section>"
            );
            sendHtml(exchange, content);
        }
    }

    static class StudentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!ensureAuth(exchange)) {
                return;
            }
            String query = getQuery(exchange);
            String search = query == null ? "" : query.trim();
            List<String[]> students = sampleStudents();
            List<String[]> filtered = filterEntries(students, search);
            StringBuilder rows = new StringBuilder();
            for (String[] student : filtered) {
                rows.append("<tr><td>").append(student[0]).append("</td><td>")
                        .append(student[1]).append("</td><td>")
                        .append(student[2]).append("</td></tr>");
            }
            String results = filtered.isEmpty()
                    ? "<tr><td colspan=\"3\">No matching students found.</td></tr>"
                    : rows.toString();
            String content = buildPage("Students", "students", ""
                    + "<section class=\"page-header\">"
                    + "<div><h1>Student Registry</h1><p>Manage student profiles, filters, and enrollment status.</p></div>"
                    + "<form class=\"search-bar\" method=\"get\" action=\"/students\">"
                    + "<input type=\"text\" name=\"search\" placeholder=\"Search by name or ID\" value=\"" + escape(search) + "\">"
                    + "<button class=\"btn btn-secondary\" type=\"submit\">Search</button>"
                    + "</form>"
                    + "</section>"
                    + "<section class=\"panel\">"
                    + "<table class=\"table\"><thead><tr><th>ID</th><th>Name</th><th>Major</th></tr></thead><tbody>"
                    + results
                    + "</tbody></table>"
                    + "</section>"
            );
            sendHtml(exchange, content);
        }
    }

    static class CoursesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!ensureAuth(exchange)) {
                return;
            }
            String content = buildPage("Courses", "courses", ""
                    + "<section class=\"page-header\">"
                    + "<div><h1>Course Catalog</h1><p>Review course details, capacity, and timetable information.</p></div>"
                    + "</section>"
                    + "<section class=\"panel\">"
                    + "<table class=\"table\"><thead><tr><th>Code</th><th>Course</th><th>Instructor</th><th>Schedule</th></tr></thead><tbody>"
                    + courseRow("CS101", "Introduction to Programming", "Dr. Emma Watson", "Mon/Wed 09:00–10:30")
                    + courseRow("CS205", "Database Systems", "Prof. John Carter", "Tue/Thu 11:00–12:30")
                    + courseRow("CS310", "Data Structures", "Dr. Priya Singh", "Mon/Wed 13:00–14:30")
                    + courseRow("CS420", "Software Engineering", "Dr. Emma Watson", "Fri 10:00–12:00")
                    + "</tbody></table>"
                    + "</section>"
            );
            sendHtml(exchange, content);
        }
    }

    static class FacultyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!ensureAuth(exchange)) {
                return;
            }
            String content = buildPage("Faculty", "faculty", ""
                    + "<section class=\"page-header\">"
                    + "<div><h1>Faculty Directory</h1><p>View faculty members, departments, and contact details.</p></div>"
                    + "</section>"
                    + "<section class=\"panel\">"
                    + "<div class=\"profile-grid\">"
                    + facultyCard("Dr. Emma Watson", "Computer Science", "emma.watson@ums.example.edu")
                    + facultyCard("Prof. John Carter", "Information Systems", "john.carter@ums.example.edu")
                    + facultyCard("Dr. Priya Singh", "Software Engineering", "priya.singh@ums.example.edu")
                    + "</div>"
                    + "</section>"
            );
            sendHtml(exchange, content);
        }
    }

    static class LogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Set-Cookie", "UMSSESSION=; Max-Age=0; Path=/");
            redirect(exchange, "/");
        }
    }

    private static boolean ensureAuth(HttpExchange exchange) throws IOException {
        if (!isAuthenticated(exchange)) {
            redirect(exchange, "/");
            return false;
        }
        return true;
    }

    private static boolean isAuthenticated(HttpExchange exchange) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null) {
            return false;
        }
        for (String cookie : cookies) {
            for (String part : cookie.split(";")) {
                String trimmed = part.trim();
                if (trimmed.startsWith("UMSSESSION=")) {
                    return trimmed.substring("UMSSESSION=".length()).equals("active");
                }
            }
        }
        return false;
    }

    private static void redirect(HttpExchange exchange, String path) throws IOException {
        exchange.getResponseHeaders().add("Location", path);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream input = exchange.getRequestBody();
        StringBuilder body = new StringBuilder();
        int read;
        byte[] buffer = new byte[1024];
        while ((read = input.read(buffer)) != -1) {
            body.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
        }
        return body.toString();
    }

    private static Map<String, String> parseForm(String body) throws IOException {
        Map<String, String> result = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return result;
        }
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8.name());
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name()) : "";
            result.put(key, value);
        }
        return result;
    }

    private static String createLoginPage(String message, boolean hasError) {
        return "<!doctype html>"
                + "<html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>UMS Login</title>"
                + styleSheet()
                + scriptBlock()
                + "</head><body>"
                + "<div class=\"auth-shell\">"
                + "<div class=\"auth-panel\">"
                + "<span class=\"brand\">UMS</span>"
                + "<h1>University Management System</h1>"
                + "<p class=\"subtitle\">Secure access to student, course, and faculty workflows.</p>"
                + (hasError ? "<div class=\"alert\">" + escape(message) + "</div>" : "")
                + "<form method=\"post\" action=\"/login\" class=\"auth-form\">"
                + "<label>Username<input name=\"username\" type=\"text\" placeholder=\"admin\" required></label>"
                + "<label>Password<input name=\"password\" type=\"password\" placeholder=\"University2026\" required></label>"
                + "<button class=\"btn btn-primary btn-block\" type=\"submit\">Sign in</button>"
                + "</form>"
                + "<div class=\"auth-footer\">"
                + "<button class=\"btn btn-tertiary\" id=\"themeToggle\" type=\"button\" onclick=\"toggleTheme()\">Switch Theme</button>"
                + "</div>"
                + "</div>"
                + "</div>"
                + "</body></html>";
    }

    private static void showLoginPage(HttpExchange exchange, String message, boolean hasError) throws IOException {
        String response = createLoginPage(message, hasError);
        sendHtml(exchange, response);
    }

    private static String buildPage(String pageTitle, String activePage, String bodyContent) {
        return "<!doctype html>"
                + "<html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>UMS — " + escape(pageTitle) + "</title>"
                + styleSheet()
                + scriptBlock()
                + "</head><body>"
                + pageShell(activePage, bodyContent)
                + "</body></html>";
    }

    private static String pageShell(String activePage, String bodyContent) {
        return "<div class=\"page-layout\">"
                + "<aside class=\"sidebar\">"
                + "<div class=\"brand-pane\"><span class=\"brand\">UMS</span><p>University Management</p></div>"
                + "<nav class=\"sidebar-nav\">"
                + navLink("/dashboard", "Dashboard", activePage.equals("dashboard"))
                + navLink("/students", "Students", activePage.equals("students"))
                + navLink("/courses", "Courses", activePage.equals("courses"))
                + navLink("/faculty", "Faculty", activePage.equals("faculty"))
                + "<a class=\"nav-item logout\" href=\"/logout\">Sign out</a>"
                + "</nav>"
                + "</aside>"
                + "<main class=\"content\">"
                + "<header class=\"topbar\">"
                + "<div><p class=\"eyebrow\">Connected to local host</p><h2>Application Dashboard</h2></div>"
                + "<button class=\"btn btn-tertiary\" id=\"themeToggle\" type=\"button\" onclick=\"toggleTheme()\">Switch Theme</button>"
                + "</header>"
                + bodyContent
                + "</main>"
                + "</div>";
    }

    private static String navLink(String href, String label, boolean active) {
        return "<a class=\"nav-item" + (active ? " active" : "") + "\" href=\"" + href + "\">" + label + "</a>";
    }

    private static String card(String title, String value, String subtitle) {
        return "<article class=\"card\">"
                + "<p class=\"card-title\">" + title + "</p>"
                + "<p class=\"card-value\">" + value + "</p>"
                + "<p class=\"card-note\">" + subtitle + "</p>"
                + "</article>";
    }

    private static String activityItem(String title, String subtitle) {
        return "<li><strong>" + title + "</strong><span>" + subtitle + "</span></li>";
    }

    private static String courseRow(String code, String course, String instructor, String schedule) {
        return "<tr><td>" + code + "</td><td>" + course + "</td><td>" + instructor + "</td><td>" + schedule + "</td></tr>";
    }

    private static String facultyCard(String name, String department, String email) {
        return "<article class=\"profile\">"
                + "<p class=\"profile-name\">" + name + "</p>"
                + "<p class=\"profile-role\">" + department + "</p>"
                + "<p class=\"profile-contact\">" + email + "</p>"
                + "</article>";
    }

    private static String styleSheet() {
        return "<style>"
                + "*{box-sizing:border-box;font-family:Inter,system-ui,sans-serif;}"
                + "html{font-size:16px;background:var(--bg);color:var(--text);}"
                + ":root{--bg:#f4f7fb;--surface:#ffffff;--text:#102a43;--muted:#52606d;--accent:#2563eb;--border:#d9e2ec;--shadow:0 24px 48px rgba(16,42,67,.08);}"
                + "[data-theme='dark']{--bg:#0b1120;--surface:#111827;--text:#e2e8f0;--muted:#94a3b8;--accent:#60a5fa;--border:rgba(148,163,184,.18);--shadow:0 24px 48px rgba(0,0,0,.45);}"
                + "body{margin:0;min-height:100vh;background:var(--bg);color:var(--text);}"
                + ".page-layout{display:grid;grid-template-columns:260px 1fr;min-height:100vh;}"
                + ".sidebar{padding:32px 24px;background:var(--surface);border-right:1px solid var(--border);box-shadow:var(--shadow);}"
                + ".brand-pane{margin-bottom:40px;}"
                + ".brand{display:inline-block;padding:10px 16px;background:var(--accent);color:#fff;border-radius:10px;font-weight:700;letter-spacing:.05em;}"
                + ".sidebar p{margin:.75rem 0 0;color:var(--muted);font-size:.95rem;}"
                + ".sidebar-nav{display:flex;flex-direction:column;gap:10px;margin-top:32px;}"
                + ".nav-item{display:block;padding:14px 16px;border-radius:12px;color:var(--text);text-decoration:none;font-weight:600;transition:background .2s ease,color .2s ease;}"
                + ".nav-item:hover{background:rgba(37,99,235,.08);}"
                + ".nav-item.active{background:var(--accent);color:#fff;}"
                + ".logout{margin-top:24px;color:#b91c1c;}"
                + ".content{padding:32px 40px;}"
                + ".topbar{display:flex;justify-content:space-between;align-items:center;gap:20px;margin-bottom:28px;}"
                + ".eyebrow{margin:0;text-transform:uppercase;letter-spacing:.2em;font-size:.78rem;color:var(--muted);}"
                + ".topbar h2{margin:.3rem 0 0;font-size:1.6rem;}"
                + ".hero{display:grid;grid-template-columns:1.7fr 1fr;gap:32px;padding:32px;background:var(--surface);border:1px solid var(--border);border-radius:28px;box-shadow:var(--shadow);}"
                + ".hero h1{margin:0;font-size:2.5rem;line-height:1.05;}"
                + ".lead{max-width:42rem;color:var(--muted);line-height:1.8;}"
                + ".hero-actions{display:flex;gap:14px;margin-top:24px;flex-wrap:wrap;}"
                + ".btn{display:inline-flex;align-items:center;justify-content:center;gap:.55rem;padding:12px 20px;border:none;border-radius:999px;font-weight:700;cursor:pointer;text-decoration:none;}"
                + ".btn-primary{background:var(--accent);color:#fff;}"
                + ".btn-secondary{background:rgba(37,99,235,.08);color:var(--text);}"
                + ".btn-tertiary{background:transparent;color:var(--text);border:1px solid var(--border);}"
                + ".grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:20px;margin-top:28px;}"
                + ".card{padding:24px;background:var(--surface);border:1px solid var(--border);border-radius:24px;box-shadow:var(--shadow);}"
                + ".card-title{margin:0;color:var(--muted);text-transform:uppercase;letter-spacing:.1em;font-size:.8rem;}"
                + ".card-value{margin:.75rem 0 0;font-size:2rem;font-weight:700;}"
                + ".card-note{margin:.75rem 0 0;color:var(--muted);font-size:.95rem;}"
                + ".panel{margin-top:28px;padding:24px;background:var(--surface);border:1px solid var(--border);border-radius:24px;box-shadow:var(--shadow);}"
                + ".panel-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:18px;}"
                + ".panel-header h2{margin:0;font-size:1.3rem;}"
                + ".activity{list-style:none;padding:0;margin:0;display:grid;gap:16px;}"
                + ".activity li{padding:18px;border:1px solid var(--border);border-radius:18px;background:rgba(255,255,255,.7);}"
                + ".activity strong{display:block;margin-bottom:8px;font-size:1rem;}"
                + ".table{width:100%;border-collapse:collapse;margin-top:16px;font-size:.95rem;}"
                + ".table th,.table td{padding:14px 16px;text-align:left;border-bottom:1px solid var(--border);}"
                + ".table th{color:var(--muted);text-transform:uppercase;letter-spacing:.05em;font-size:.8rem;}"
                + ".page-header{display:flex;justify-content:space-between;align-items:center;gap:20px;margin-bottom:24px;}"
                + ".page-header h1{margin:0;font-size:2rem;}"
                + ".search-bar{display:flex;gap:12px;flex-wrap:wrap;align-items:center;}"
                + ".search-bar input{flex:1 1 240px;padding:14px 16px;border:1px solid var(--border);border-radius:14px;background:var(--bg);color:var(--text);}"
                + ".profile-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px;}"
                + ".profile{padding:24px;background:var(--surface);border:1px solid var(--border);border-radius:24px;}"
                + ".profile-name{margin:0 0 .5rem;font-size:1.15rem;font-weight:700;}"
                + ".profile-role{margin:0 0 .75rem;color:var(--muted);}"
                + ".profile-contact{margin:0;color:var(--accent);word-break:break-all;}"
                + ".auth-shell{min-height:100vh;display:grid;place-items:center;padding:40px;background:linear-gradient(180deg,rgba(37,99,235,.15) 0%,rgba(244,247,251,1) 100%);}"
                + ".auth-panel{width:min(540px,100%);padding:36px 36px 30px;border-radius:32px;background:var(--surface);border:1px solid var(--border);box-shadow:var(--shadow);}"
                + ".auth-panel h1{margin:0 0 12px;font-size:2rem;}"
                + ".auth-panel .subtitle{margin:0 0 24px;color:var(--muted);line-height:1.7;}"
                + ".auth-form{display:grid;gap:20px;}"
                + ".auth-form label{display:grid;gap:8px;font-size:.95rem;color:var(--muted);}"
                + ".auth-form input{width:100%;padding:14px 16px;border:1px solid var(--border);border-radius:14px;background:var(--bg);color:var(--text);}"
                + ".auth-footer{display:flex;justify-content:space-between;align-items:center;margin-top:24px;}"
                + ".alert{padding:14px 16px;background:#fee2e2;color:#b91c1c;border-radius:14px;margin-bottom:12px;}"
                + "@media(max-width:1024px){.page-layout{grid-template-columns:1fr;} .sidebar{position:sticky;top:0;width:100%;border-right:none;border-bottom:1px solid var(--border);} .content{padding:24px;}}"
                + "@media(max-width:720px){.hero{grid-template-columns:1fr;} .grid{grid-template-columns:1fr;} .profile-grid{grid-template-columns:1fr;} .page-header{flex-direction:column;align-items:flex-start;}}"
                + "</style>";
    }

    private static String scriptBlock() {
        return "<script>"
                + "function applyTheme(){var theme=localStorage.getItem('umsTheme')||'light';document.documentElement.setAttribute('data-theme',theme);var toggle=document.getElementById('themeToggle');if(toggle){toggle.textContent=theme==='dark'?'Switch to Light':'Switch to Dark';}}"
                + "function toggleTheme(){var current=document.documentElement.getAttribute('data-theme');var next=current==='dark'?'light':'dark';localStorage.setItem('umsTheme',next);applyTheme();}"
                + "window.addEventListener('DOMContentLoaded',applyTheme);"
                + "</script>";
    }

    private static String getQuery(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || query.isEmpty()) {
            return "";
        }
        Map<String, String> params = new HashMap<>();
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            try {
                String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name());
                String value = pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name()) : "";
                params.put(key, value);
            } catch (Exception ignored) {
            }
        }
        return params.getOrDefault("search", "");
    }

    private static List<String[]> sampleStudents() {
        List<String[]> students = new ArrayList<>();
        students.add(new String[]{"1001", "Alice Johnson", "Computer Science"});
        students.add(new String[]{"1002", "Bob Smith", "Information Systems"});
        students.add(new String[]{"1003", "Carol Davis", "Software Engineering"});
        students.add(new String[]{"1004", "Daniel Lee", "Data Analytics"});
        students.add(new String[]{"1005", "Elena Martinez", "Cybersecurity"});
        return students;
    }

    private static List<String[]> filterEntries(List<String[]> entries, String query) {
        if (query == null || query.isEmpty()) {
            return entries;
        }
        String lower = query.toLowerCase();
        List<String[]> filtered = new ArrayList<>();
        for (String[] entry : entries) {
            if (entry[0].toLowerCase().contains(lower) || entry[1].toLowerCase().contains(lower) || entry[2].toLowerCase().contains(lower)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static void sendHtml(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
