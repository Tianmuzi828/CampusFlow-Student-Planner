# CampusFlow — Student Planner

[![Build and test](https://github.com/Tianmuzi828/CampusFlow-Student-Planner/actions/workflows/maven.yml/badge.svg)](https://github.com/Tianmuzi828/CampusFlow-Student-Planner/actions/workflows/maven.yml)

<p align="center">
  <img src="src/main/resources/com/campusflow/app/assets/campusflow-logo-a1.png" alt="CampusFlow logo" width="96">
</p>

CampusFlow is a JavaFX desktop planner for managing recurring classes and assignment deadlines in one place. It provides an Outlook-style weekly schedule, an assignment deadline calendar, persistent local storage, themes, and English/Chinese interface options.

## Main features

- Add, edit, and view recurring courses on a weekly calendar.
- Set course start dates, meeting days, locations, instructors, and five-minute start/end times.
- Use eight course colors to distinguish lecture, recitation, lab, and other sections.
- Add assignments with a course, deadline date and time, priority, description, and completion status.
- Browse unfinished assignments by date in a monthly deadline calendar.
- See today's classes, upcoming work, overdue work, and completion progress on the dashboard.
- Switch between light, dark, and system themes with configurable accent colors and display density.
- Use the interface in English or Simplified Chinese without an internet connection.
- Store data locally in SQLite; export CSV files and back up or restore the database from Settings.

## Technology

- Java 21
- JavaFX 21 with FXML and CSS
- Maven and Maven Wrapper
- SQLite through JDBC
- JUnit 5

No MySQL server, web server, account, or internet connection is required after Maven has downloaded the dependencies for the first build.

## Requirements

1. Install a JDK 21 distribution.
2. Confirm the active version:

   ```bash
   java -version
   ```

   The output should start with Java 21.

The repository includes Maven Wrapper, so a separate Maven installation is not required.

## Run from Terminal

### macOS or Linux

```bash
git clone https://github.com/Tianmuzi828/CampusFlow-Student-Planner.git
cd CampusFlow-Student-Planner
chmod +x mvnw
./mvnw clean test
./mvnw javafx:run
```

### Windows PowerShell or Command Prompt

```bat
git clone https://github.com/Tianmuzi828/CampusFlow-Student-Planner.git
cd CampusFlow-Student-Planner
mvnw.cmd clean test
mvnw.cmd javafx:run
```

The first run may take longer because Maven downloads JavaFX, SQLite, and test dependencies. Later runs use the local Maven cache.

## Open in IntelliJ IDEA

1. Choose **Open** and select the cloned `CampusFlow-Student-Planner` folder.
2. Open the project as a Maven project when IntelliJ asks.
3. Set **Project SDK** and **Language level** to Java 21.
4. Wait for Maven synchronization to finish.
5. Open the Maven tool window and run **Plugins → javafx → javafx:run**, or run this in IntelliJ's terminal:

   ```bash
   ./mvnw javafx:run
   ```

IntelliJ's `.idea` directory is intentionally not included because it contains computer-specific editor settings. The project is reconstructed from `pom.xml` when opened.

## Local data

CampusFlow creates its database automatically at:

```text
data/campusflow.db
```

This file contains the current user's courses, assignments, and preferences. It is intentionally ignored by Git so personal data is never uploaded to GitHub. Deleting it resets the app to an empty database on the next launch. Use the Settings screen to create a backup before deleting or replacing it.

## Project structure

```text
src/main/java/com/campusflow/app/
├── data/       SQLite initialization and repository operations
├── i18n/       language selection and resource bundles
├── model/      course, assignment, and settings models
├── service/    dashboard and calendar calculations
├── ui/         reusable forms and time picker controls
└── *Controller.java

src/main/resources/com/campusflow/app/
├── assets/     application logo
├── i18n/       English and Chinese text
├── *.fxml      JavaFX screen layouts
└── styles.css  light/dark themes and component styling
```

## Tests

Run the complete test suite with:

```bash
./mvnw clean test
```

The tests cover SQLite persistence, course colors, localization, dashboard calculations, schedule scaling, and form validation. GitHub Actions runs the same command automatically for pushes and pull requests.

## Troubleshooting

### `package com.campusflow.app.data does not exist`

Make sure the repository contains `src/main/java/com/campusflow/app/data`. Older incomplete uploads accidentally ignored every directory named `data`; the current `.gitignore` only ignores the root `/data/` directory that contains the personal SQLite database.

### JavaFX or Maven dependencies are unresolved

Confirm Java 21 is selected, connect to the internet for the first dependency download, and run:

```bash
./mvnw clean test
```

In IntelliJ, use **Reload All Maven Projects** after the command completes.

### The app opens with no courses

That is the expected first-run state. Add a course from **Overview** or **Courses**. CampusFlow does not upload or synchronize personal schedules with another service.

---

## 中文说明

CampusFlow 是一个使用 JavaFX 开发的本地课程表与作业管理软件。它支持周课程表、作业截止日期日历、课程与作业编辑、完成度统计、深色主题，以及英文和简体中文界面。

运行要求为 **JDK 21**。macOS/Linux 使用：

```bash
./mvnw clean test
./mvnw javafx:run
```

Windows 使用：

```bat
mvnw.cmd clean test
mvnw.cmd javafx:run
```

课程、作业和设置保存在本地 `data/campusflow.db` 中，不会上传到 GitHub。程序界面切换语言不需要网络，也不需要额外下载语言包。
