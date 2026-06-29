# 🚀 Building and Deploying the OpaqueBehaviour Editor Update Site

This guide provides instructions on how to build the OpaqueBehaviour Editor Eclipse plugin, package it as an update site (p2 repository), and deploy it so users can install it.

---

## 🛠️ Prerequisites

Before you start, make sure you have the following installed on your system:
- **Java JDK 17 or 21+** (The project is configured for `JavaSE-21`).
- **Apache Maven 3.8+** (Uses Tycho 4.0.13 to build Eclipse OSGi plugins).

---

## 📦 Building the Update Site Locally

To compile the plugins, execute the unit tests, and package the update site, run the following Maven command from the project root:

```bash
# Ensure JAVA_HOME points to Java 17 or 21+
export JAVA_HOME=/path/to/your/jdk-21

mvn clean verify
```

### 📂 Build Outputs
Once the build finishes successfully, you will find the generated update site artifacts in the **`OpaqueBehaviourEditor.repository`** module:

1. **P2 Repository Directory** (ready for hosting):
   - Located at: `OpaqueBehaviourEditor.repository/target/repository/`
   - Contains: `features/`, `plugins/`, `content.jar`, `artifacts.jar`, and `p2.index`.

2. **P2 Repository Zip Archive**:
   - Located at: `OpaqueBehaviourEditor.repository/target/de.tu-ilmenau.sse.opaquebehavioureditor.repository-1.0.0-SNAPSHOT.zip`

---

## 🌐 Deploying the Update Site

To allow users to install or update the plugin directly from Eclipse, you must host the contents of the `OpaqueBehaviourEditor.repository/target/repository/` directory on a web server.

### Option 1: Hosting on GitHub Pages (Recommended)
You can host the update site for free using GitHub Pages:

1. Create a dedicated branch in your repository (e.g., `gh-pages` or `updatesite`).
2. Copy the contents of the `OpaqueBehaviourEditor.repository/target/repository/` directory into that branch.
3. Commit and push the changes to GitHub.
4. Enable GitHub Pages for that branch in your repository settings.
5. Your update site URL will be: `https://<your-username>.github.io/<repository-name>/`

### Option 2: Hosting on a Static Web Server
Copy the `target/repository/` directory to any static web server (e.g., Apache, Nginx, or an AWS S3 Bucket):
```bash
# Example using rsync
rsync -avz OpaqueBehaviourEditor.repository/target/repository/ user@yourserver.com:/var/www/updatesite/opaque-editor/
```
The update site URL will be the web address pointing to that directory (e.g., `https://yourserver.com/updatesite/opaque-editor/`).

---

## 🔌 Installing the Plugin in Eclipse

Once the update site is deployed, users can install the plugin as follows:

1. Open Eclipse IDE.
2. Go to the menu: **Help ➔ Install New Software...**
3. Click the **Add...** button on the top-right.
4. Fill in the details:
   - **Name**: `OpaqueBehaviour Editor Update Site`
   - **Location**: Enter your update site URL (e.g., `https://<your-username>.github.io/<repository-name>/`)
5. Click **Add**.
6. Check the box next to **OpaqueBehaviour Editor** in the list.
7. Click **Next**, accept the license terms, and click **Finish** to complete the installation.
8. Restart Eclipse when prompted.

> [!TIP]
> **Local Installation**: If you want to test the plugin locally without hosting it on a web server, you can add it in Eclipse by clicking **Add... ➔ Archive...** and selecting the built `.zip` file under `OpaqueBehaviourEditor.repository/target/`.
