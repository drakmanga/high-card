# 🧪 Java Programming Exercise: Refactoring, Security, and New Features

This exercise aims to evaluate your skills in Java development with Spring Boot, focusing on:

- 🔁 Refactoring and code quality
- 🔐 Security and vulnerability management
- ⚙️ Implementation of new features
- 🧪 Writing tests and documentation

---

## 🎯 Goal

The repository contains a simple **Spring Boot** application. Your task is to improve the existing code and add new features, demonstrating proficiency in:

- 🔐 **Security**: Identifying and fixing vulnerabilities.
- ✨ **Code Quality**: Refactoring, exception handling, and documentation.
- 🚀 **Functional Development**: Extending the application with business logic.
- ✅ **Testing**: Validating features through automated tests.

---

## ⚙️ Setup and Submission

### 📦 Cloning the Repository
Fork the repository to your personal GitHub account.

### 💾 Exercise Submission
Complete all the required tasks, ensuring all changes are committed and pushed to your fork.

### 📬 Work Submission
Send the repository link via email to: **f.mori@sara.it**

---

## 📌 Tasks to Complete

1. **🧾 Data Validation**
    - In the user creation service, implement strict validation for the `email` and `phoneNumber` fields.
    - The `phoneNumber` must comply with the **Italian standard** (correct prefix and length).

2. **🛡️ SQL Injection Prevention**
    - The `PUT` endpoint is vulnerable to **SQL Injection**.
    - Identify and fix the vulnerable logic by properly validating inputs.

3. **📊 Pagination, Sorting, and Search**
    - Complete the user search method:
        - Return a paginated list after input validation.
        - Apply sorting based on the `OrderType` enum.
        - Implement a case-insensitive filter on a search string (e.g., name or email using `contains`).

4. **🚨 Exception Handling**
    - Introduce centralized exception handling consistent with the existing `StatusDTO` standard.
    - All responses, including errors, must return HTTP status code **200**.

5. **🔐 JWT Security**
    - Integrate authentication using **JSON Web Token (JWT)**.
    - The token must be validated against:
        - **Policy**: Applicable authorization rules
        - **Issuer**: Token issuing authority
        - **Expiration**: Token validity period

6. **🐞 Bug Fixing**
    - Identify and fix any bugs in the base code.

7. **🧪 Unit Testing**
    - Add unit tests for new and modified features.
    - Ensure adequate coverage.

8. **📝 Javadoc Documentation**
    - Add **Javadoc comments** for relevant classes, methods, and attributes.
    - This improves code **readability** and **maintainability**.

---

## 📎 Important Notes

- 📚 **External Libraries**: Use of third-party libraries is allowed if they are **up-to-date and free from known vulnerabilities**.
- 🧱 **Architecture**: Do **not** modify the existing layered architecture of the application.

---

## ❓ Follow-up Question

> **🧠 For larger and more complex projects, why is it discouraged to use web-exposed objects (e.g., `? extends GenericRequest`, `? extends GenericResponse`) within the Service layer?**

Please include a brief answer to this question in your submission email.

---

## 🤖 Bonus — AI-Assisted Development: Showcase Your Method

> This assessment can be completed using traditional methods, however, we consider the ability to collaborate with Artificial Intelligence a strategic asset. 

We encourage the use of agentic AI assistants such as:
- [Claude Code](https://claude.ai/code)
- [Gemini CLI](https://github.com/google-gemini/gemini-cli)
- Other similar tools (Copilot Workspace, Cursor, etc.)

not to replace your critical thinking, but to amplify your impact. We are interested in evaluating how you leverage these tools to optimize workflows while consistently safeguarding the quality and robustness of the final output.

The goal of this bonus is not to achieve a "perfect" result from the tool, but rather to demonstrate your ability to govern the process: how you prompt the AI, how you identify and correct its errors, and how you integrate its work into our architecture.

### 📁 What to Submit

Add an `/ai-assisted/` folder to your repository containing one or more Markdown files documenting the experience. The files should cover the following aspects:

#### 1. `pre-analysis.md` — Pre-Exercise Analysis
- How you presented the project context to the AI (repository, README, existing code)
- What information you provided before starting
- Any limitations encountered in the tool's initial understanding of the codebase

#### 2. `plan.md` — Change Plan
- The intervention plan generated or co-built with the AI
- How you iterated on the plan (refinement prompts, corrections, intermediate steps)
- Which tasks you fully delegated to the AI and which you handled manually

#### 3. `report.md` — Final Report
- **Key prompts used**: summary or excerpt of the most significant prompts
- **Difficulties encountered**: generated errors, hallucinations, code that failed to compile, lost context
- **Approaches adopted**: how you guided the tool toward the correct solution
- **Personal assessment**: how much did it speed up your work? Where did it fall short?

### 📌 Notes
- The AI is not required to have completed the tasks correctly — the goal is to document the *process*, not the outcome.
- Honest criticism is appreciated: a report that describes the tool's failures is more valuable than one that only highlights its successes.
- If you used multiple tools, you may create separate subfolders (`/ai-assisted/claude-code/`, `/ai-assisted/gemini-cli/`, etc.)

---

## ✅ Svolgimento

- **[IMPLEMENTATION.md](IMPLEMENTATION.md)** — riepilogo task per task, istruzioni di avvio e risposta alla domanda di approfondimento
- **[/ai-assisted/](ai-assisted/)** — documentazione del processo di collaborazione con l'AI
  - [pre-analysis.md](ai-assisted/pre-analysis.md) — analisi preliminare e limiti riscontrati
  - [plan.md](ai-assisted/plan.md) — piano di intervento e iterazioni
  - [report.md](ai-assisted/report.md) — report finale, metodo ed errori
