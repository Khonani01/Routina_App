Project README Documentation Report 

Project Title 

Routina App – Student Productivity & Habit Management System 

Module: OPSC6312 (Open Source Coding, Intermediate) 

Institution: Rosebank International 

Repository: https://github.com/Khonani01/Routina_App 

First, the TEAM TABLE: 

Name 

Student number 

Roles and responsibilityies 

Khonani Mutobvu 

ST10439622 

Project Manager, Backend Integration & Build Configuration 

Ofentse  Mashingoane 

ST10456959 

Developer, Kotlin Unit Testing & Code Comments 

 

Vusikosi Sono 

st10453078 

Developer, Local Data & Architecture Support 

 

Charity Taulene 

ST10438951 

Developer, Store Material & Voice-Over Script 

 

Khumbelo Tshikororo 

ST10441369 

Developer, Repository Documentation & Quality Assurance (Re-Test Sheet) 

 

1. Executive Summary & Purpose 

Background & Problem Statement 

Varsity life places heavy demands on students, requiring them to juggle lecture timetables, assignment deadlines, study schedules, and personal routines. Most students rely on separate applications for task management, habit tracking, and lecture notes, leading to fragmented organization and reduced focus. 

The Routina Solution 

Routina is a centralized, native Android productivity hub created specifically for Varsity students. It combines three core academic utilities into a single, cohesive platform: 

Habit Tracking: Empowers users to build long-term academic discipline using progress goals and daily routines. 

Task & Reminder Management: Enables prioritization of assignments and daily responsibilities with custom notifications. 

Structured Notes: Offers accessible note-taking tools to quickly capture lecture summaries and study checklists. 

2. Technical Architecture & Tech Stack 

Frontend: Android Studio (Kotlin, Native UI Component Architecture) 

Backend: Node.js / Express RESTful API (Hosted on Render) 

Database: Firebase Firestore & Local Storage Strategy 

Version Control: Git & GitHub 

Routina_App/ 
├── api/          # Node.js/Express API Endpoints & Firebase Connectivity 
└── app/          # Native Android Application Source Code & Test Modules 
 

3. GitHub & CI/CD Integration 

The team utilized GitHub to ensure continuous integration, structured repository management, and code tracking throughout the development lifecycle: 

Version Control & Branching Strategy: Feature branches were used to isolate fixes, code formatting, and test additions before merging directly to the release branch. 

Automated Build Checks (GitHub Actions): GitHub Actions workflows were integrated to execute Android build verification steps, validating project integrity upon each push. 

Issue Tracking & Commit Discipline: Commit messages were structured logically (e.g., Docs: Update README with project details) to maintain a clear audit trail for evaluation. 

4. Final Deliverables 

(<img width="738" height="1599" alt="WhatsApp Image 2026-11-05 at 23 49 20" src="https://github.com/user-attachments/assets/bead9285-4d81-4ddf-9888-bdb3982220ff" />
) 

Demonstration Video 

Video Walkthrough Link: https://youtu.be/HLMnNm5wRCA?si=bXJgDU7gtPuf9z4f 
Release Notes (Part 3 Final Release) 

Version: v1.0.0-final 

Build Target: Android 15.0 (API Level 35) 

Status: All core functionalities, backend API endpoints, and test suites validated for final submission.
Added NotesScreen.kt and NotesViewModel.kt
Pushed by Charity Taulene ST10438951
