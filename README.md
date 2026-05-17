# Class Schedule

![Tests](https://github.com/w1xes/schedule-testing/actions/workflows/tests.yml/badge.svg)

## General info

This repository contains the source code of the Class Schedule Project.
The main goal of the project is designing a website where the university or institute staff can create, store and display their training schedules.


## Creating a local repository

1. Download and install the latest version of Git: https://git-scm.com/downloads
2. Open a terminal and go to the directory where you want to clone the files.
3. Clone this repository:
```bash
   git clone 
```

## Database

1. Download and install the latest version of PostgreSQL: https://www.postgresql.org/download/
2. Create a database for the project.
3. Copy `src/main/resources/application.yaml` to `src/main/resources/application-local.yaml` and fill in your database connection settings (url, username, password).

## Redis

1. Download and install the latest version of Redis: https://redis.io/download
2. Configure the connection url in `src/main/resources/application-local.yaml`.

## Starting the backend server

### Option 1: IntelliJ IDEA

1. Download and install IntelliJ IDEA (Ultimate, trial, or EAP): https://www.jetbrains.com/idea/download
2. Open the project from the folder where you previously cloned it.
3. Set the active Spring profile to `local`:
   - `Run` → `Edit Configurations…` → in the Spring Boot run configuration, add `local` to **Active profiles**
4. Run the application.

### Option 2: Gradle (command line)

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Starting the frontend server

1. Download and install Node.js LTS: https://nodejs.org/en/
2. Open a terminal in the `/frontend` directory and run:
   ```bash
   npm install
   ```
3. After installation, start the frontend server:
   ```bash
   npm start
   ```
