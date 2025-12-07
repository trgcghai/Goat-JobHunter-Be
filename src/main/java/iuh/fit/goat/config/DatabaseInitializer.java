package iuh.fit.goat.config;

import iuh.fit.goat.entity.*;
import iuh.fit.goat.entity.Role;
import iuh.fit.goat.entity.embeddable.Contact;
import iuh.fit.goat.enumeration.*;
import iuh.fit.goat.repository.*;
import iuh.fit.goat.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import net.datafaker.Faker;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final CareerRepository careerRepository;
    private final JobRepository jobRepository;
    private final BlogRepository blogRepository;
    private final CommentRepository commentRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationRepository notificationRepository;
    private final SubscriberRepository subscriberRepository;
    private final PasswordEncoder passwordEncoder;

    private final Faker faker = new Faker(new Locale("vi"));
    private final Random random = new Random();

    private final List<Permission> permissions = new ArrayList<>();
    private final List<Recruiter> recruiters = new ArrayList<>();
    private final List<Applicant> applicants = new ArrayList<>();
    private final List<Skill> skills = new ArrayList<>();
    private final List<Career> careers = new ArrayList<>();
    private final List<Job> jobs = new ArrayList<>();
    private final List<Blog> blogs = new ArrayList<>();
    private final List<Comment> comments = new ArrayList<>();
    private final List<Application> applications = new ArrayList<>();
    private final List<Notification> notifications = new ArrayList<>();
    private final List<Subscriber> subscribers = new ArrayList<>();

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Database initialization is starting...");

        long countPermissions = this.permissionRepository.count();
        long countRoles = this.roleRepository.count();
        long countUsers = this.userRepository.count();
        long countSkills = this.skillRepository.count();
        long countCareers = this.careerRepository.count();

        if(countPermissions == 0){
            initPermissions();
        }

        if(countRoles == 0){
            initRoles();
        }

        if(countSkills == 0){
            initSkills();
        }

        if(countCareers == 0){
            initCareers();
        }

        if(countUsers == 0){
            initUsers();
            initJobs();
            initBlogsAndComments();
            initApplications();
            initNotifications();
            initSubscribers();
        }

        if (countPermissions > 0 && countRoles > 0 && countUsers > 0 && countSkills > 0 && countCareers > 0) {
            System.out.println("Skip init database ~ Already have data...");
        } else {
            System.out.println("Database initialization ended...");
        }
    }

    private <T> T getRandom(List<T> list, Random random) {
        if (list == null || list.isEmpty()) return null;
        return list.get(random.nextInt(list.size()));
    }

    private void initPermissions() {
        // PERMISSION
        permissions.add(new Permission("Create permission", "/api/v1/permissions", "POST", "PERMISSIONS"));
        permissions.add(new Permission("Update permission", "/api/v1/permissions", "PUT", "PERMISSIONS"));
        permissions.add(new Permission("Delete permission", "/api/v1/permissions/{id}", "DELETE", "PERMISSIONS"));
        permissions.add(new Permission("Get permission", "/api/v1/permissions/{id}", "GET", "PERMISSIONS"));
        permissions.add(new Permission("Get all permissions", "/api/v1/permissions", "GET", "PERMISSIONS"));

        // ROLE
        permissions.add(new Permission("Create role", "/api/v1/roles", "POST", "ROLES"));
        permissions.add(new Permission("Update role", "/api/v1/roles", "PUT", "ROLES"));
        permissions.add(new Permission("Delete role", "/api/v1/roles/{id}", "DELETE", "ROLES"));
        permissions.add(new Permission("Get role", "/api/v1/roles/{id}", "GET", "ROLES"));
        permissions.add(new Permission("Get all roles", "/api/v1/roles", "GET", "ROLES"));

        // USER
        permissions.add(new Permission("Update password", "/api/v1/users/update-password", "PUT", "USERS"));
        permissions.add(new Permission("Reset password", "/api/v1/users/reset-password", "PUT", "USERS"));
        permissions.add(new Permission("Get all users", "/api/v1/users", "GET", "USERS"));
        permissions.add(new Permission("Get a user by id", "/api/v1/users/{id}", "GET", "USERS"));
        permissions.add(new Permission("Create a new user", "/api/v1/users", "POST", "USERS"));
        permissions.add(new Permission("Activate users", "/api/v1/users/activate", "PUT", "USERS"));
        permissions.add(new Permission("Deactivate users", "/api/v1/users/deactivate", "PUT", "USERS"));

        // RECRUITER
        permissions.add(new Permission("Create recruiter", "/api/v1/recruiters", "POST", "RECRUITERS"));
        permissions.add(new Permission("Update recruiter", "/api/v1/recruiters", "PUT", "RECRUITERS"));
        permissions.add(new Permission("Delete recruiter", "/api/v1/recruiters/{id}", "DELETE", "RECRUITERS"));
        permissions.add(new Permission("Get recruiter", "/api/v1/recruiters/{id}", "GET", "RECRUITERS"));
        permissions.add(new Permission("Get all recruiters", "/api/v1/recruiters", "GET", "RECRUITERS"));

        // APPLICANT
        permissions.add(new Permission("Create applicant", "/api/v1/applicants", "POST", "APPLICANTS"));
        permissions.add(new Permission("Update applicant", "/api/v1/applicants", "PUT", "APPLICANTS"));
        permissions.add(new Permission("Delete applicant", "/api/v1/applicants/{id}", "DELETE", "APPLICANTS"));
        permissions.add(new Permission("Get applicant", "/api/v1/applicants/{id}", "GET", "APPLICANTS"));
        permissions.add(new Permission("Get all applicants", "/api/v1/applicants", "GET", "APPLICANTS"));

        // CAREER
        permissions.add(new Permission("Create career", "/api/v1/careers", "POST", "CAREERS"));
        permissions.add(new Permission("Update career", "/api/v1/careers", "PUT", "CAREERS"));
        permissions.add(new Permission("Delete career", "/api/v1/careers/{id}", "DELETE", "CAREERS"));
        permissions.add(new Permission("Get career", "/api/v1/careers/{id}", "GET", "CAREERS"));
        permissions.add(new Permission("Get all careers", "/api/v1/careers", "GET", "CAREERS"));

        // SKILL
        permissions.add(new Permission("Create skill", "/api/v1/skills", "POST", "SKILLS"));
        permissions.add(new Permission("Update skill", "/api/v1/skills", "PUT", "SKILLS"));
        permissions.add(new Permission("Delete skill", "/api/v1/skills/{id}", "DELETE", "SKILLS"));
        permissions.add(new Permission("Get skill", "/api/v1/skills/{id}", "GET", "SKILLS"));
        permissions.add(new Permission("Get all skills", "/api/v1/skills", "GET", "SKILLS"));

        // JOB
        permissions.add(new Permission("Create job", "/api/v1/jobs", "POST", "JOBS"));
        permissions.add(new Permission("Update job", "/api/v1/jobs", "PUT", "JOBS"));
        permissions.add(new Permission("Activate job", "/api/v1/jobs/activate", "PUT", "JOBS"));
        permissions.add(new Permission("Deactivate job", "/api/v1/jobs/deactivate", "PUT", "JOBS"));
        permissions.add(new Permission("Delete job", "/api/v1/jobs/{id}", "DELETE", "JOBS"));
        permissions.add(new Permission("Get job", "/api/v1/jobs/{id}", "GET", "JOBS"));
        permissions.add(new Permission("Get all jobs", "/api/v1/jobs", "GET", "JOBS"));
        permissions.add(new Permission("Get all applicants for job", "/api/v1/jobs/{jobId}/applicants", "GET", "JOBS"));
        permissions.add(new Permission("Count jobs for recruiter", "/api/v1/jobs/recruiters/count", "GET", "JOBS"));
        permissions.add(new Permission("Count applications for job", "/api/v1/jobs/count-applications", "GET", "JOBS"));
        permissions.add(new Permission("Enable jobs", "/api/v1/jobs/enabled", "PUT", "JOBS"));
        permissions.add(new Permission("Disable jobs", "/api/v1/jobs/disabled", "PUT", "JOBS"));


        // FILE
        permissions.add(new Permission("Upload file", "/api/v1/files", "POST", "FILES"));
        permissions.add(new Permission("Download file", "/api/v1/files", "GET", "FILES"));

        // APPLICATION
        permissions.add(new Permission("Create application", "/api/v1/applications", "POST", "APPLICATIONS"));
        permissions.add(new Permission("Update application", "/api/v1/applications", "PUT", "APPLICATIONS"));
        permissions.add(new Permission("Accept application", "/api/v1/applications/accepted", "PUT", "APPLICATIONS"));
        permissions.add(new Permission("Reject application", "/api/v1/applications/rejected", "PUT", "APPLICATIONS"));
        permissions.add(new Permission("Delete application", "/api/v1/applications/{id}", "DELETE", "APPLICATIONS"));
        permissions.add(new Permission("Get application", "/api/v1/applications/{id}", "GET", "APPLICATIONS"));
        permissions.add(new Permission("Get all applications by recruiter", "/api/v1/applications", "GET", "APPLICATIONS"));
        permissions.add(new Permission("Get all applications by applicant", "/api/v1/applications/by-applicant", "GET", "APPLICATIONS"));
        permissions.add(new Permission("Get all applications", "/api/v1/all-applications", "GET", "APPLICATIONS"));

        // NOTIFICATION
        permissions.add(new Permission("Get all notifications", "/api/v1/notifications", "GET", "NOTIFICATIONS"));
        permissions.add(new Permission("Mark seen notifications", "/api/v1/notifications", "PUT", "NOTIFICATIONS"));

        // SUBSCRIBER
        permissions.add(new Permission("Create a subscriber", "/api/v1/subscribers", "POST", "SUBSCRIBERS"));
        permissions.add(new Permission("Update a subscriber", "/api/v1/subscribers", "PUT", "SUBSCRIBERS"));
        permissions.add(new Permission("Delete a subscriber", "/api/v1/subscribers/{id}", "DELETE", "SUBSCRIBERS"));
        permissions.add(new Permission("Get a subscriber by id", "/api/v1/subscribers/{id}", "GET", "SUBSCRIBERS"));
        permissions.add(new Permission("Get subscribers with pagination", "/api/v1/subscribers", "GET", "SUBSCRIBERS"));

        // BLOG
        permissions.add(new Permission("Create a blog", "/api/v1/blogs", "POST", "BLOGS"));
        permissions.add(new Permission("Update a blog", "/api/v1/blogs", "PUT", "BLOGS"));
        permissions.add(new Permission("Delete a blog", "/api/v1/blogs", "DELETE", "BLOGS"));
        permissions.add(new Permission("Get a blog by id", "/api/v1/blogs/{id}", "GET", "BLOGS"));
        permissions.add(new Permission("Get blogs with pagination", "/api/v1/blogs", "GET", "BLOGS"));
        permissions.add(new Permission("Like blogs", "/api/v1/blogs/liked-blogs", "PUT", "BLOGS"));
        permissions.add(new Permission("Enable blogs", "/api/v1/blogs/enabled", "PUT", "BLOGS"));
        permissions.add(new Permission("Disable blogs", "/api/v1/blogs/disabled", "PUT", "BLOGS"));

        // COMMENT
        permissions.add(new Permission("Create a comment", "/api/v1/comments", "POST", "COMMENTS"));
        permissions.add(new Permission("Update a comment", "/api/v1/comments", "PUT", "COMMENTS"));
        permissions.add(new Permission("Delete a comment", "/api/v1/comments/{id}", "DELETE", "COMMENTS"));
        permissions.add(new Permission("Get comments with pagination", "/api/v1/comments", "GET", "COMMENTS"));
        permissions.add(new Permission("Get a comment by id", "/api/v1/comments/{id}", "GET", "COMMENTS"));

        // AUTH
        permissions.add(new Permission("Verify recruiter", "/api/v1/auth/verify/recruiter/{id}", "PATCH", "AUTH"));

        // DASHBOARD
        permissions.add(new Permission("User dashboard", "/api/v1/dashboard/users", "GET", "DASHBOARD"));
        permissions.add(new Permission("Job dashboard", "/api/v1/dashboard/jobs", "GET", "DASHBOARD"));
        permissions.add(new Permission("Applications dashboard", "/api/v1/dashboard/applications", "GET", "DASHBOARD"));
        permissions.add(new Permission("Applications year dashboard", "/api/v1/dashboard/applications-year", "GET", "DASHBOARD"));

        // ADMIN
        permissions.add(new Permission("Backup Database", "/api/v1/admin/backup", "GET", "ADMIN"));

        // CONVERSATION
        permissions.add(new Permission("Create a conversation", "/api/v1/conversations", "POST", "CONVERSATIONS"));
        permissions.add(new Permission("Update a conversation", "/api/v1/conversations", "PUT", "CONVERSATIONS"));
        permissions.add(new Permission("Delete a conversation", "/api/v1/conversations", "DELETE", "CONVERSATIONS"));
        permissions.add(new Permission("Get conversations with pagination", "/api/v1/conversations", "GET", "CONVERSATIONS"));
        permissions.add(new Permission("Get a conversation by id", "/api/v1/conversations/{id}", "GET", "CONVERSATIONS"));
        permissions.add(new Permission("Pin conversations", "/api/v1/conversations/pin", "PATCH", "CONVERSATIONS"));
        permissions.add(new Permission("Unpin conversations", "/api/v1/conversations/unpin", "PATCH", "CONVERSATIONS"));

        this.permissionRepository.saveAll(permissions);

        System.out.println("Initialized permissions.");
    }

    private void initRoles() {
//      SUPER_ADMIN
        Role superAdmin = new Role();
        superAdmin.setName("SUPER_ADMIN");
        superAdmin.setDescription("Admin will gain full permissions");
        superAdmin.setActive(true);
        superAdmin.setPermissions(permissions);
        this.roleRepository.save(superAdmin);

//      APPLICANT
        Role applicantRole = new Role();
        applicantRole.setName("APPLICANT");
        applicantRole.setDescription("Role for job applicants");
        applicantRole.setActive(true);

        List<Permission> applicantPermissions = new ArrayList<>();
        applicantPermissions.add(this.permissionRepository.findByName("Update password"));
        applicantPermissions.add(this.permissionRepository.findByName("Reset password"));
        applicantPermissions.add(this.permissionRepository.findByName("Update applicant"));
        applicantPermissions.add(this.permissionRepository.findByName("Upload file"));
        applicantPermissions.add(this.permissionRepository.findByName("Download file"));
        applicantPermissions.add(this.permissionRepository.findByName("Create application"));
        applicantPermissions.add(this.permissionRepository.findByName("Get all applications by applicant"));
        applicantPermissions.add(this.permissionRepository.findByName("Get all notifications"));
        applicantPermissions.add(this.permissionRepository.findByName("Mark seen notifications"));
        applicantPermissions.add(this.permissionRepository.findByName("Create a subscriber"));
        applicantPermissions.add(this.permissionRepository.findByName("Update a subscriber"));
        applicantPermissions.add(this.permissionRepository.findByName("Delete a subscriber"));
        applicantPermissions.add(this.permissionRepository.findByName("Get a subscriber by id"));
        applicantPermissions.add(this.permissionRepository.findByName("Get subscribers with pagination"));
        applicantPermissions.add(this.permissionRepository.findByName("Like blogs"));
        applicantPermissions.add(this.permissionRepository.findByName("Create a comment"));
        applicantPermissions.add(this.permissionRepository.findByName("Update a comment"));
        applicantPermissions.add(this.permissionRepository.findByName("Delete a comment"));
        applicantPermissions.add(this.permissionRepository.findByName("Create a conversation"));
        applicantPermissions.add(this.permissionRepository.findByName("Update a conversation"));
        applicantPermissions.add(this.permissionRepository.findByName("Delete a conversation"));
        applicantPermissions.add(this.permissionRepository.findByName("Get conversations with pagination"));
        applicantPermissions.add(this.permissionRepository.findByName("Get a conversation by id"));
        applicantPermissions.add(this.permissionRepository.findByName("Pin conversations"));
        applicantPermissions.add(this.permissionRepository.findByName("Unpin conversations"));

        applicantRole.setPermissions(applicantPermissions);
        this.roleRepository.save(applicantRole);

//      HR
        Role hrRole = new Role();
        hrRole.setName("HR");
        hrRole.setDescription("Role for HR / Recruiters");
        hrRole.setActive(true);

        List<Permission> hrPermissions = new ArrayList<>();
        hrPermissions.add(this.permissionRepository.findByName("Update password"));
        hrPermissions.add(this.permissionRepository.findByName("Reset password"));
        hrPermissions.add(this.permissionRepository.findByName("Update recruiter"));
        hrPermissions.add(this.permissionRepository.findByName("Create career"));
        hrPermissions.add(this.permissionRepository.findByName("Get career"));
        hrPermissions.add(this.permissionRepository.findByName("Get all careers"));
        hrPermissions.add(this.permissionRepository.findByName("Create skill"));
        hrPermissions.add(this.permissionRepository.findByName("Get skill"));
        hrPermissions.add(this.permissionRepository.findByName("Get all skills"));
        hrPermissions.add(this.permissionRepository.findByName("Create job"));
        hrPermissions.add(this.permissionRepository.findByName("Update job"));
        hrPermissions.add(this.permissionRepository.findByName("Delete job"));
        hrPermissions.add(this.permissionRepository.findByName("Activate job"));
        hrPermissions.add(this.permissionRepository.findByName("Deactivate job"));
        hrPermissions.add(this.permissionRepository.findByName("Get job"));
        hrPermissions.add(this.permissionRepository.findByName("Get all jobs"));
        hrPermissions.add(this.permissionRepository.findByName("Get all applicants for job"));
        hrPermissions.add(this.permissionRepository.findByName("Count jobs for recruiter"));
        hrPermissions.add(this.permissionRepository.findByName("Count applications for job"));
        hrPermissions.add(this.permissionRepository.findByName("Upload file"));
        hrPermissions.add(this.permissionRepository.findByName("Download file"));
        hrPermissions.add(this.permissionRepository.findByName("Update application"));
        hrPermissions.add(this.permissionRepository.findByName("Accept application"));
        hrPermissions.add(this.permissionRepository.findByName("Reject application"));
        hrPermissions.add(this.permissionRepository.findByName("Get application"));
        hrPermissions.add(this.permissionRepository.findByName("Get all applications by recruiter"));
        hrPermissions.add(this.permissionRepository.findByName("Get all notifications"));
        hrPermissions.add(this.permissionRepository.findByName("Mark seen notifications"));
        hrPermissions.add(this.permissionRepository.findByName("Create a subscriber"));
        hrPermissions.add(this.permissionRepository.findByName("Update a subscriber"));
        hrPermissions.add(this.permissionRepository.findByName("Delete a subscriber"));
        hrPermissions.add(this.permissionRepository.findByName("Get a subscriber by id"));
        hrPermissions.add(this.permissionRepository.findByName("Get subscribers with pagination"));
        hrPermissions.add(this.permissionRepository.findByName("Create a blog"));
        hrPermissions.add(this.permissionRepository.findByName("Update a blog"));
        hrPermissions.add(this.permissionRepository.findByName("Delete a blog"));
        hrPermissions.add(this.permissionRepository.findByName("Get a blog by id"));
        hrPermissions.add(this.permissionRepository.findByName("Get blogs with pagination"));
        hrPermissions.add(this.permissionRepository.findByName("Like blogs"));
        hrPermissions.add(this.permissionRepository.findByName("Create a comment"));
        hrPermissions.add(this.permissionRepository.findByName("Update a comment"));
        hrPermissions.add(this.permissionRepository.findByName("Delete a comment"));
        hrPermissions.add(this.permissionRepository.findByName("Job dashboard"));
        hrPermissions.add(this.permissionRepository.findByName("Applications dashboard"));
        hrPermissions.add(this.permissionRepository.findByName("Applications year dashboard"));
        hrPermissions.add(this.permissionRepository.findByName("Create a conversation"));
        hrPermissions.add(this.permissionRepository.findByName("Update a conversation"));
        hrPermissions.add(this.permissionRepository.findByName("Delete a conversation"));
        hrPermissions.add(this.permissionRepository.findByName("Get conversations with pagination"));
        hrPermissions.add(this.permissionRepository.findByName("Get a conversation by id"));
        hrPermissions.add(this.permissionRepository.findByName("Pin conversations"));
        hrPermissions.add(this.permissionRepository.findByName("Unpin conversations"));

        hrRole.setPermissions(hrPermissions);
        this.roleRepository.save(hrRole);

        System.out.println("Initialized roles.");
    }

    private void initSkills() {
        skills.add(new Skill("3ds Max"));
        skills.add(new Skill("ABAP"));
        skills.add(new Skill("Adobe"));
        skills.add(new Skill("Adobe Photoshop"));
        skills.add(new Skill("Adobe XD"));
        skills.add(new Skill("Agile"));
        skills.add(new Skill("AI"));
        skills.add(new Skill("Android"));
        skills.add(new Skill("Android studio"));
        skills.add(new Skill("Angular"));
        skills.add(new Skill("AngularJS"));
        skills.add(new Skill("Ansible"));
        skills.add(new Skill("Anti-malware"));
        skills.add(new Skill("Antivirus"));
        skills.add(new Skill("Apache Airflow"));
        skills.add(new Skill("Apache HttpClient"));
        skills.add(new Skill("Apache Spark"));
        skills.add(new Skill("API"));
        skills.add(new Skill("Appium"));
        skills.add(new Skill("Application Security"));
        skills.add(new Skill("ASP.NET"));
        skills.add(new Skill("Automation Test"));
        skills.add(new Skill("AUTOSAR"));
        skills.add(new Skill("AVR"));
        skills.add(new Skill("AWS"));
        skills.add(new Skill("AWS CloudFormation"));
        skills.add(new Skill("AWS Lambda"));
        skills.add(new Skill("Axure"));
        skills.add(new Skill("Azure"));
        skills.add(new Skill("Bash Shell"));
        skills.add(new Skill("Big Data"));
        skills.add(new Skill("Black box testing"));
        skills.add(new Skill("Blazor"));
        skills.add(new Skill("BLE"));
        skills.add(new Skill("Blender"));
        skills.add(new Skill("Blockchain"));
        skills.add(new Skill("Bootstrap"));
        skills.add(new Skill("BPMN"));
        skills.add(new Skill("Bridge Engineer"));
        skills.add(new Skill("BrowserStack"));
        skills.add(new Skill("Burp Suite"));
        skills.add(new Skill("Business Analysis"));
        skills.add(new Skill("Business Intelligence"));
        skills.add(new Skill("C#"));
        skills.add(new Skill("C++"));
        skills.add(new Skill("Change Management"));
        skills.add(new Skill("Chinese"));
        skills.add(new Skill("CI/CD"));
        skills.add(new Skill("Cisco"));
        skills.add(new Skill("C language"));
        skills.add(new Skill("Clean Architecture"));
        skills.add(new Skill("Cloud"));
        skills.add(new Skill("Cloud-native Architecture"));
        skills.add(new Skill("Cloud Security"));
        skills.add(new Skill("CompTIA Security+"));
        skills.add(new Skill("Computer Vision"));
        skills.add(new Skill("CRM"));
        skills.add(new Skill("CSS"));
        skills.add(new Skill("CSS 3"));
        skills.add(new Skill("Cucumber"));
        skills.add(new Skill("Cybersecurity"));
        skills.add(new Skill("Cypress"));
        skills.add(new Skill("Dart"));
        skills.add(new Skill("Data Analysis"));
        skills.add(new Skill("Database"));
        skills.add(new Skill("Databricks"));
        skills.add(new Skill("Data cleaning"));
        skills.add(new Skill("Data-driven"));
        skills.add(new Skill("Data Engineer"));
        skills.add(new Skill("Data mining"));
        skills.add(new Skill("Data modeling"));
        skills.add(new Skill("Data Quality Tools"));
        skills.add(new Skill("Data Science"));
        skills.add(new Skill("Data Warehousing"));
        skills.add(new Skill("DBA"));
        skills.add(new Skill("Deep Learning"));
        skills.add(new Skill("Design"));
        skills.add(new Skill("Design Systems"));
        skills.add(new Skill("DevOps"));
        skills.add(new Skill("DevSecOps"));
        skills.add(new Skill("Digital Forensics"));
        skills.add(new Skill("Django"));
        skills.add(new Skill("Docker"));
        skills.add(new Skill("Domain-Driven Design"));
        skills.add(new Skill("Ec-cube"));
        skills.add(new Skill("Elasticsearch"));
        skills.add(new Skill("Elixir"));
        skills.add(new Skill("ELK Stack"));
        skills.add(new Skill("ELT"));
        skills.add(new Skill("Embedded"));
        skills.add(new Skill("Embedded C"));
        skills.add(new Skill("English"));
        skills.add(new Skill("Enterprise Architecture"));
        skills.add(new Skill("Entity Framework"));
        skills.add(new Skill("ERP"));
        skills.add(new Skill("Ethers.js"));
        skills.add(new Skill("ETL"));
        skills.add(new Skill("Exploit Development"));
        skills.add(new Skill("Express"));
        skills.add(new Skill("ExpressJS"));
        skills.add(new Skill("F#"));
        skills.add(new Skill("FastAPI"));
        skills.add(new Skill("Figma"));
        skills.add(new Skill("Fiori"));
        skills.add(new Skill("Firebase"));
        skills.add(new Skill("Firewall"));
        skills.add(new Skill("Flask"));
        skills.add(new Skill("Flutter"));
        skills.add(new Skill("Fullstack"));
        skills.add(new Skill("Functional specifications"));
        skills.add(new Skill("Games"));
        skills.add(new Skill("GCP"));
        skills.add(new Skill("Generative AI"));
        skills.add(new Skill("Gin"));
        skills.add(new Skill("Git"));
        skills.add(new Skill("GitHub"));
        skills.add(new Skill("GitHub Actions"));
        skills.add(new Skill("GitLab"));
        skills.add(new Skill("Golang"));
        skills.add(new Skill("Google Cloud"));
        skills.add(new Skill("Governance, Risk & Compliance"));
        skills.add(new Skill("Grafana"));
        skills.add(new Skill("GraphQL"));
        skills.add(new Skill("Groovy"));
        skills.add(new Skill("Hadoop"));
        skills.add(new Skill("Hardware Troubleshooting"));
        skills.add(new Skill("Helm"));
        skills.add(new Skill("Hibernate"));
        skills.add(new Skill("HPE LoadRunner"));
        skills.add(new Skill("HTML"));
        skills.add(new Skill("HTML5"));
        skills.add(new Skill("Hugging Face Transformers"));
        skills.add(new Skill("Illustrator"));
        skills.add(new Skill("Information Security"));
        skills.add(new Skill("Integration test"));
        skills.add(new Skill("Interaction Design"));
        skills.add(new Skill("iOS"));
        skills.add(new Skill("IoT"));
        skills.add(new Skill("ISO 27001"));
        skills.add(new Skill("IT Audit"));
        skills.add(new Skill("IT Communication/Translation"));
        skills.add(new Skill("IT Governance"));
        skills.add(new Skill("ITIL Foundation"));
        skills.add(new Skill("IT Support"));
        skills.add(new Skill("J2EE"));
        skills.add(new Skill("Japanese"));
        skills.add(new Skill("Japanese IT Communication"));
        skills.add(new Skill("Java"));
        skills.add(new Skill("JavaScript"));
        skills.add(new Skill("Jenkins"));
        skills.add(new Skill("Jest"));
        skills.add(new Skill("Jira"));
        skills.add(new Skill("Jmeter"));
        skills.add(new Skill("JQuery"));
        skills.add(new Skill("JSON"));
        skills.add(new Skill("Juniper"));
        skills.add(new Skill("JUnit"));
        skills.add(new Skill("Kafka"));
        skills.add(new Skill("Katalon"));
        skills.add(new Skill("Koa"));
        skills.add(new Skill("Korean"));
        skills.add(new Skill("Kotlin"));
        skills.add(new Skill("Kubernetes"));
        skills.add(new Skill("Laravel"));
        skills.add(new Skill("Leadership"));
        skills.add(new Skill("LINQ"));
        skills.add(new Skill("Linux"));
        skills.add(new Skill("Live2D"));
        skills.add(new Skill("LLM"));
        skills.add(new Skill("Lua"));
        skills.add(new Skill("Machine Learning"));
        skills.add(new Skill("Magento"));
        skills.add(new Skill("Market research"));
        skills.add(new Skill("Maven"));
        skills.add(new Skill("Maya"));
        skills.add(new Skill("MFA"));
        skills.add(new Skill("MFC"));
        skills.add(new Skill("Microservices"));
        skills.add(new Skill("Microservices Architecture"));
        skills.add(new Skill("Microsoft Azure SQL Database"));
        skills.add(new Skill("Microsoft Dynamics 365"));
        skills.add(new Skill("Microsoft Power Apps"));
        skills.add(new Skill("Microsoft SQL Server"));
        skills.add(new Skill("MLOps"));
        skills.add(new Skill("Mobile Apps"));
        skills.add(new Skill("MongoDB"));
        skills.add(new Skill("Motion Design"));
        skills.add(new Skill("MVC"));
        skills.add(new Skill("MVP"));
        skills.add(new Skill("MVVM"));
        skills.add(new Skill("MySQL"));
        skills.add(new Skill("Neo4j"));
        skills.add(new Skill("NestJS"));
        skills.add(new Skill(".NET"));
        skills.add(new Skill(".Net Core"));
        skills.add(new Skill("Networking"));
        skills.add(new Skill("NextJS"));
        skills.add(new Skill("NLP"));
        skills.add(new Skill("Nmap"));
        skills.add(new Skill("NodeJS"));
        skills.add(new Skill("NoSQL"));
        skills.add(new Skill("NumPy"));
        skills.add(new Skill("Nuxt.js"));
        skills.add(new Skill("Objective C"));
        skills.add(new Skill("OCR"));
        skills.add(new Skill("Odoo"));
        skills.add(new Skill("OLTP"));
        skills.add(new Skill("OOP"));
        skills.add(new Skill("OpenCV"));
        skills.add(new Skill("OpenStack"));
        skills.add(new Skill("Oracle"));
        skills.add(new Skill("OutSystems"));
        skills.add(new Skill("Penetration Testing"));
        skills.add(new Skill("Pentest"));
        skills.add(new Skill("PHP"));
        skills.add(new Skill("Playwright"));
        skills.add(new Skill("PL/SQL"));
        skills.add(new Skill("PostgreSql"));
        skills.add(new Skill("Postman"));
        skills.add(new Skill("Power BI"));
        skills.add(new Skill("PowerShell"));
        skills.add(new Skill("PQA"));
        skills.add(new Skill("Presale"));
        skills.add(new Skill("Product Design"));
        skills.add(new Skill("Product Management"));
        skills.add(new Skill("Product Metrics"));
        skills.add(new Skill("Product Owner"));
        skills.add(new Skill("Product roadmap"));
        skills.add(new Skill("Product strategy"));
        skills.add(new Skill("Project Management"));
        skills.add(new Skill("Prometheus"));
        skills.add(new Skill("Prompt Engineering"));
        skills.add(new Skill("Prototyping"));
        skills.add(new Skill("Python"));
        skills.add(new Skill("PyTorch"));
        skills.add(new Skill("QA QC"));
        skills.add(new Skill("R"));
        skills.add(new Skill("Razor"));
        skills.add(new Skill("ReactJS"));
        skills.add(new Skill("React Native"));
        skills.add(new Skill("Redis"));
        skills.add(new Skill("Redux"));
        skills.add(new Skill("REST Assured"));
        skills.add(new Skill("Retrofit"));
        skills.add(new Skill("Risk Management"));
        skills.add(new Skill("Robot Framework"));
        skills.add(new Skill("ROS"));
        skills.add(new Skill("Ruby"));
        skills.add(new Skill("Ruby on Rails"));
        skills.add(new Skill("Rust"));
        skills.add(new Skill("RxJS"));
        skills.add(new Skill("Salesforce"));
        skills.add(new Skill("SAP"));
        skills.add(new Skill("Sass"));
        skills.add(new Skill("Scala"));
        skills.add(new Skill("Scrum"));
        skills.add(new Skill("SCSS"));
        skills.add(new Skill("Security"));
        skills.add(new Skill("Security Awareness Training"));
        skills.add(new Skill("Selenium"));
        skills.add(new Skill("Sharepoint"));
        skills.add(new Skill("Shopify"));
        skills.add(new Skill("SIEM"));
        skills.add(new Skill("Sketch"));
        skills.add(new Skill("SOAP"));
        skills.add(new Skill("SOAR"));
        skills.add(new Skill("Software Architecture"));
        skills.add(new Skill("SOLID Principles"));
        skills.add(new Skill("Solution Architecture"));
        skills.add(new Skill("Spark"));
        skills.add(new Skill("Splunk"));
        skills.add(new Skill("Spring"));
        skills.add(new Skill("Spring Boot"));
        skills.add(new Skill("SQL"));
        skills.add(new Skill("SQLite"));
        skills.add(new Skill("SRS"));
        skills.add(new Skill("Stakeholder management"));
        skills.add(new Skill("Statistical Analysis"));
        skills.add(new Skill("Strategy planning"));
        skills.add(new Skill("Swift"));
        skills.add(new Skill("Symfony"));
        skills.add(new Skill("System Admin"));
        skills.add(new Skill("System Architecture"));
        skills.add(new Skill("Tableau"));
        skills.add(new Skill("Tailwind"));
        skills.add(new Skill("Team Management"));
        skills.add(new Skill("TensorFlow"));
        skills.add(new Skill("Terraform"));
        skills.add(new Skill("TestComplete"));
        skills.add(new Skill("Tester"));
        skills.add(new Skill("TestNG"));
        skills.add(new Skill("Trello"));
        skills.add(new Skill("T-SQL"));
        skills.add(new Skill("TypeScript"));
        skills.add(new Skill("Ui5"));
        skills.add(new Skill("UI-UX"));
        skills.add(new Skill("UML"));
        skills.add(new Skill("Unit test"));
        skills.add(new Skill("Unity"));
        skills.add(new Skill("Unix"));
        skills.add(new Skill("Unreal Engine"));
        skills.add(new Skill("Usability testing"));
        skills.add(new Skill("User diagram"));
        skills.add(new Skill("User flows"));
        skills.add(new Skill("VBA"));
        skills.add(new Skill("vb.net"));
        skills.add(new Skill("Visual Basic"));
        skills.add(new Skill("Visual Design"));
        skills.add(new Skill("VMware"));
        skills.add(new Skill("VueJS"));
        skills.add(new Skill("Vuex"));
        skills.add(new Skill("Waterfall Methodology"));
        skills.add(new Skill("Web3.js"));
        skills.add(new Skill("Web API"));
        skills.add(new Skill("Web Application Firewall"));
        skills.add(new Skill("White box testing"));
        skills.add(new Skill("Windows"));
        skills.add(new Skill("Windows PowerShell"));
        skills.add(new Skill("Windows Server"));
        skills.add(new Skill("WinForms"));
        skills.add(new Skill("Wireframing"));
        skills.add(new Skill("Wordpress"));
        skills.add(new Skill("WPF"));
        skills.add(new Skill("XML"));
        skills.add(new Skill("Zend"));
        skills.add(new Skill("Lập kế hoạch bán hàng"));
        skills.add(new Skill("Đàm phán"));
        skills.add(new Skill("Phát triển kinh doanh"));
        skills.add(new Skill("Tạo khách hàng tiềm năng"));
        skills.add(new Skill("Tiếp thị kỹ thuật số"));
        skills.add(new Skill("Tiếp thị nội dung"));
        skills.add(new Skill("Quảng cáo Google"));
        skills.add(new Skill("Quảng cáo Facebook"));
        skills.add(new Skill("Tối ưu hóa công cụ tìm kiếm (SEO)"));
        skills.add(new Skill("Tiếp thị qua email"));
        skills.add(new Skill("Nghiên cứu thị trường"));
        skills.add(new Skill("Quản lý mạng xã hội"));
        skills.add(new Skill("Chăm sóc khách hàng"));
        skills.add(new Skill("Bán hàng qua điện thoại"));
        skills.add(new Skill("Quản lý tài khoản khách hàng"));
        skills.add(new Skill("Tiếp thị liên kết"));
        skills.add(new Skill("Lên kế hoạch bài giảng"));
        skills.add(new Skill("Thiết kế chương trình đào tạo"));
        skills.add(new Skill("Quản lý lớp học"));
        skills.add(new Skill("Công nghệ giáo dục"));
        skills.add(new Skill("Học trực tuyến"));
        skills.add(new Skill("Thiết kế bài kiểm tra"));
        skills.add(new Skill("Thuyết trình trước công chúng"));
        skills.add(new Skill("Cố vấn/Hướng dẫn"));
        skills.add(new Skill("Phát triển chương trình học"));
        skills.add(new Skill("Dạy học qua Zoom"));
        skills.add(new Skill("Google Classroom"));
        skills.add(new Skill("Hệ thống quản lý học tập (LMS)"));
        skills.add(new Skill("Quản lý khách sạn – nhà hàng"));
        skills.add(new Skill("Dịch vụ ăn uống (F&B)"));
        skills.add(new Skill("Tổ chức sự kiện"));
        skills.add(new Skill("Quản lý đặt chỗ"));
        skills.add(new Skill("Hệ thống POS"));
        skills.add(new Skill("Pha chế"));
        skills.add(new Skill("Dọn phòng khách sạn"));
        skills.add(new Skill("Hướng dẫn du lịch"));
        skills.add(new Skill("Giao tiếp đa ngôn ngữ"));
        skills.add(new Skill("Lễ tân khách sạn"));
        skills.add(new Skill("Phần mềm quản lý khách sạn"));
        skills.add(new Skill("AutoCAD"));
        skills.add(new Skill("Revit"));
        skills.add(new Skill("SolidWorks"));
        skills.add(new Skill("Thiết kế mô hình 3D"));
        skills.add(new Skill("Đọc bản vẽ kỹ thuật"));
        skills.add(new Skill("Kỹ thuật xây dựng dân dụng"));
        skills.add(new Skill("Dự toán công trình"));
        skills.add(new Skill("Thiết kế cơ khí"));
        skills.add(new Skill("Thiết kế hệ thống ống dẫn"));
        skills.add(new Skill("Đo đạc – Trắc địa"));
        skills.add(new Skill("Giám sát công trình"));
        skills.add(new Skill("Thiết kế hệ thống điện"));
        skills.add(new Skill("Phân tích kết cấu"));
        skills.add(new Skill("Tuân thủ tiêu chuẩn an toàn"));
        skills.add(new Skill("Chăm sóc điều dưỡng cơ bản"));
        skills.add(new Skill("Theo dõi dấu hiệu sinh tồn"));
        skills.add(new Skill("Sơ cứu"));
        skills.add(new Skill("Thuật ngữ y khoa"));
        skills.add(new Skill("Chăm sóc bệnh nhân"));
        skills.add(new Skill("Cấp phát thuốc"));
        skills.add(new Skill("Giáo dục sức khỏe"));
        skills.add(new Skill("Thủ thuật lâm sàng"));
        skills.add(new Skill("Hồ sơ y tế điện tử (EHR)"));
        skills.add(new Skill("Chứng nhận hồi sức tim phổi (CPR)"));
        skills.add(new Skill("Kiểm soát nhiễm khuẩn"));
        skills.add(new Skill("Chăm sóc người bệnh/già yếu"));

        this.skillRepository.saveAll(skills);

        System.out.println("Initialized skills.");
    }

    private void initCareers() {
        careers.add(new Career("Công nghệ thông tin"));
        careers.add(new Career("Thiết kế đồ họa"));
        careers.add(new Career("Thiết kế UI/UX"));
        careers.add(new Career("Phát triển phần mềm"));
        careers.add(new Career("Khoa học dữ liệu"));
        careers.add(new Career("Trí tuệ nhân tạo"));
        careers.add(new Career("An ninh mạng"));
        careers.add(new Career("Quản trị mạng"));
        careers.add(new Career("Kỹ thuật phần cứng"));
        careers.add(new Career("Kỹ thuật điện – điện tử"));
        careers.add(new Career("Kỹ thuật cơ khí"));
        careers.add(new Career("Kỹ thuật xây dựng"));
        careers.add(new Career("Kiến trúc"));
        careers.add(new Career("Kinh doanh"));
        careers.add(new Career("Bán hàng"));
        careers.add(new Career("Marketing"));
        careers.add(new Career("Marketing kỹ thuật số"));
        careers.add(new Career("Tài chính"));
        careers.add(new Career("Kế toán"));
        careers.add(new Career("Kiểm toán"));
        careers.add(new Career("Ngân hàng"));
        careers.add(new Career("Nhân sự"));
        careers.add(new Career("Hành chính – văn phòng"));
        careers.add(new Career("Giáo dục – đào tạo"));
        careers.add(new Career("Biên – phiên dịch"));
        careers.add(new Career("Báo chí – Truyền thông"));
        careers.add(new Career("Luật – Pháp lý"));
        careers.add(new Career("Y tế – Điều dưỡng"));
        careers.add(new Career("Chăm sóc sức khỏe – Spa"));
        careers.add(new Career("Dược phẩm – Công nghệ sinh học"));
        careers.add(new Career("Nông – Lâm – Ngư nghiệp"));
        careers.add(new Career("Chế biến thực phẩm"));
        careers.add(new Career("Logistics – Chuỗi cung ứng"));
        careers.add(new Career("Xuất nhập khẩu"));
        careers.add(new Career("Vận tải – Lái xe"));
        careers.add(new Career("Hàng không"));
        careers.add(new Career("Hàng hải"));
        careers.add(new Career("Du lịch – Nhà hàng – Khách sạn"));
        careers.add(new Career("Tư vấn – Chăm sóc khách hàng"));
        careers.add(new Career("Thiết kế thời trang"));
        careers.add(new Career("Thủ công mỹ nghệ"));
        careers.add(new Career("Sản xuất – Vận hành – Bảo trì"));
        careers.add(new Career("Bảo vệ – An ninh"));
        careers.add(new Career("Bất động sản"));
        careers.add(new Career("Quản lý dự án"));
        careers.add(new Career("Thống kê – Phân tích dữ liệu"));
        careers.add(new Career("Thư viện – Lưu trữ"));
        careers.add(new Career("Môi trường – Tài nguyên"));
        careers.add(new Career("Thể thao – Giải trí"));
        careers.add(new Career("Mỹ thuật – Nhiếp ảnh – Điện ảnh"));
        careers.add(new Career("Tôn giáo – Xã hội – Tình nguyện"));
        careers.add(new Career("Game – Trò chơi điện tử"));
        careers.add(new Career("Bảo hiểm"));
        careers.add(new Career("Startup – Khởi nghiệp"));

        this.careerRepository.saveAll(careers);

        System.out.println("Initialized careers.");
    }

    private void initUsers() throws IOException {
//      ADMIN
        Role role = this.roleRepository.findByName("SUPER_ADMIN");

        User user = new Recruiter();
        user.setAddress("12 Nguyễn Văn Bảo, Gò Vấp, Thành phồ Hồ Chí Minh");
        user.setContact(new Contact("admin@gmail.com", "0987654321"));
        user.setDob(LocalDate.of(1956, 11, 11));
        user.setFullName("Admin");
        user.setGender(Gender.MALE);
        user.setUsername("admin");
        user.setPassword(this.passwordEncoder.encode("12345678"));
        user.setRole(role);
        user.setEnabled(true);
        user.setAvatar("https://res.cloudinary.com/dfwttyfwk/image/upload/v1765005016/logo_irwjdg.png");

        this.userRepository.save(user);

//      RECRUITERS
        List<String> usernameRecruiters = FileUploadUtil.getUsernameRecruiters();
        for (int i = 1; i <= usernameRecruiters.size(); i++) {
            Recruiter r = new Recruiter();
            r.setUsername(usernameRecruiters.get(i - 1));
            r.setFullName(usernameRecruiters.get(i - 1).toUpperCase());
            r.setContact(new Contact(usernameRecruiters.get(i - 1) + "@gmail.com", faker.phoneNumber().cellPhone()));
            r.setAddress(faker.address().fullAddress());
            r.setDob(LocalDate.of(1980 + random.nextInt(20), 1 + random.nextInt(12), 1 + random.nextInt(28)));
            r.setGender(random.nextBoolean() ? Gender.MALE : Gender.FEMALE);
            r.setAvatar(FileUploadUtil.getAvatarRecruiter(usernameRecruiters.get(i-1)));
            r.setPassword(this.passwordEncoder.encode("12345678"));
            r.setRole(this.roleRepository.findByName("HR"));
            r.setEnabled(true);
            recruiters.add(r);
        }
        this.userRepository.saveAll(recruiters);

//      APPLICANTS
        List<String> emails = FileUploadUtil.getEmailApplicants();
        for (int i = 1; i <= emails.size(); i++) {
            Applicant a = new Applicant();
            a.setUsername(faker.twitter().userName());
            a.setFullName(faker.name().fullName());
            a.setContact(new Contact(emails.get(i - 1), faker.phoneNumber().cellPhone()));
            a.setAddress(faker.address().fullAddress());
            a.setDob(LocalDate.of(1990 + random.nextInt(10), 1 + random.nextInt(12), 1 + random.nextInt(28)));
            a.setGender(random.nextBoolean() ? Gender.MALE : Gender.FEMALE);
            a.setAvatar(FileUploadUtil.AVATAR + faker.twitter().userName());
            a.setPassword(passwordEncoder.encode("12345678"));
            a.setRole(this.roleRepository.findByName("APPLICANT"));
            a.setEnabled(true);
            applicants.add(a);
        }
        this.userRepository.saveAll(applicants);

        System.out.println("Initialized users.");
    }

    private void initJobs() {
        for (Recruiter r : recruiters) {
            for (int j = 0; j < 10; j++) {
                Job job = new Job();
                job.setTitle(faker.job().title());
                job.setDescription(faker.lorem().paragraph());
                job.setRecruiter(r);
                job.setCareer(getRandom(careers, random));
                job.setLocation(faker.address().city());
                job.setSalary(10000000 + random.nextInt(15000000));

                LocalDate startDate = LocalDate.now().plusDays(random.nextInt(30));
                job.setStartDate(startDate);

                LocalDate endDate = startDate.plusDays(30 + random.nextInt(61));
                job.setEndDate(endDate);

                job.setActive(random.nextBoolean());

                Level[] levels = Level.values();
                job.setLevel(levels[random.nextInt(levels.length)]);

                job.setQuantity(1 + random.nextInt(5));

                WorkingType[] types = WorkingType.values();
                job.setWorkingType(types[random.nextInt(types.length)]);

                List<Skill> jobSkills = new ArrayList<>();
                int skillCount = 3 + random.nextInt(3);
                for (int k = 0; k < skillCount; k++) {
                    Skill skill = skills.get(random.nextInt(skills.size()));
                    if(!jobSkills.contains(skill)){
                        jobSkills.add(skill);
                    }
                }
                job.setSkills(jobSkills);

                jobs.add(job);
            }
        }

        this.jobRepository.saveAll(jobs);

        System.out.println("Initialized jobs.");
    }

    private void initBlogsAndComments() {
        for (Recruiter r : recruiters) {
            for (int b = 0; b < 10; b++) {
                Blog blog = new Blog();
                blog.setTitle(faker.book().title());
                blog.setBanner("https://boringapi.com/api/v1/static/photos/" + random.nextInt(300) + ".jpeg");
                blog.setDescription(faker.lorem().paragraph());
                blog.setContent(faker.lorem().paragraph(10));

                List<String> tags = new ArrayList<>();
                int tagCount = 3 + random.nextInt(3);
                for (int t = 0; t < tagCount; t++) {
                    tags.add(faker.job().keySkills());
                }
                blog.setTags(tags);

                blog.setDraft(random.nextBoolean());
                blog.setEnabled(true);
                blog.setAuthor(r);
                blog.getActivity().setTotalReads(random.nextInt(1000));
                blog.getActivity().setTotalParentComments(0);
                blogs.add(blog);
            }
        }

        blogRepository.saveAll(blogs);
        blogRepository.flush();

        List<Comment> parentComments = new ArrayList<>();
        List<Comment> childComments = new ArrayList<>();

        for (Blog blog : blogs) {
            for (int c = 0; c < 5; c++) {
                User commentAuthor = random.nextBoolean() ? getRandom(recruiters, random)
                        : getRandom(applicants, random);

                Comment parentComment = new Comment();
                parentComment.setBlog(blog);
                parentComment.setComment(faker.lorem().sentence(12));
                parentComment.setCommentedBy(commentAuthor);
                parentComment.setReply(false);
                parentComment.setParent(null);
                parentComment.setChildren(new ArrayList<>());
                parentComments.add(parentComment);

                blog.getActivity().setTotalParentComments(blog.getActivity().getTotalParentComments() + 1);

                for (int cc = 0; cc < 5; cc++) {
                    User childAuthor = random.nextBoolean() ? getRandom(recruiters, random)
                            : getRandom(applicants, random);

                    Comment child = new Comment();
                    child.setBlog(blog);
                    child.setComment(faker.lorem().sentence(10));
                    child.setCommentedBy(childAuthor);
                    child.setReply(true);
                    child.setParent(parentComment);
                    child.setChildren(new ArrayList<>());
                    childComments.add(child);

                    parentComment.getChildren().add(child);
                }
            }
        }

        commentRepository.saveAll(parentComments);
        commentRepository.flush();

        commentRepository.saveAll(childComments);
        commentRepository.flush();

        for (Comment c : parentComments) {
            Blog blog = c.getBlog();
            blog.getComments().add(c);
        }
        for (Comment c : childComments) {
            Blog blog = c.getBlog();
            blog.getComments().add(c);
        }

        for (Blog blog : blogs) {
            long totalComments = blog.getComments().size();
            blog.getActivity().setTotalComments(totalComments);
        }

        blogRepository.saveAll(blogs);

        System.out.println("Initialized blogs and comments.");
    }

    private void initApplications() {
        List<Job> jobs = this.jobRepository.findAll();
        for (Applicant a : applicants) {
            for (int j = 0; j < 5; j++) {
                Job job = getRandom(jobs, random);

                Application app = new Application();
                app.setJob(job);
                app.setApplicant(a);
                app.setEmail(a.getContact().getEmail());
                app.setResumeUrl("https://res.cloudinary.com/dfwttyfwk/image/upload/v1754836676/jobhunter/resumes/sample-corporate-resume_10082025213751.pdf");

                Status[] types = Status.values();
                app.setStatus(types[random.nextInt(types.length)]);

                applications.add(app);
            }
        }

        this.applicationRepository.saveAll(applications);
        System.out.println("Initialized applications.");
    }

    private void initNotifications() {
        List<User> allUsers = new ArrayList<>();
        allUsers.addAll(recruiters);
        allUsers.addAll(applicants);

        List<Comment> commentSaved = this.commentRepository.findAll();

        for (User recipient : allUsers) {
            for (int i = 0; i < 3; i++) {
                Notification n = new Notification();

                n.setRecipient(recipient);

                User actor;
                do {
                    actor = getRandom(allUsers, random);
                } while (actor.equals(recipient));
                n.setActor(actor);

                NotificationType type = NotificationType.values()[random.nextInt(NotificationType.values().length)];
                n.setType(type);

                n.setSeen(random.nextBoolean());

                if (type == NotificationType.LIKE) {
                    n.setBlog(getRandom(blogs, random));
                } else if (type == NotificationType.COMMENT) {
                    Comment comment = getRandom(commentSaved, random);
                    n.setComment(comment);
                    n.setBlog(comment != null && comment.getBlog() != null ? comment.getBlog() : blogs.getFirst());
                } else if (type == NotificationType.REPLY) {
                    Comment reply = getRandom(commentSaved, random);
                    n.setReply(reply);
                    n.setRepliedOnComment(reply != null && reply.getParent() != null ? reply.getParent() : commentSaved.getFirst());
                    n.setBlog(reply != null && reply.getBlog() != null ? reply.getBlog() : blogs.getFirst());
                }

                notifications.add(n);
            }
        }

        this.notificationRepository.saveAll(notifications);
        System.out.println("Initialized notifications.");
    }

    private void initSubscribers() {
        List<Skill> allSkills = this.skillRepository.findAll();

        for (Applicant a : applicants) {
            Subscriber s = new Subscriber();
            s.setName(a.getFullName());
            s.setEmail(a.getContact().getEmail());

            Collections.shuffle(allSkills, random);
            int skillCount = 1 + random.nextInt(5);
            s.setSkills(new ArrayList<>(allSkills.subList(0, Math.min(skillCount, allSkills.size()))));

            subscribers.add(s);
        }

        this.subscriberRepository.saveAll(subscribers);
        System.out.println("Initialized subscribers.");
    }

}
