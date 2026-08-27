<div align="center">

<img src="assets/Logo.png" alt="Logo" width="40%" />
<br>

A Mobile Application for Filipino Sign Language Interpretation Using Real-Time Gesture Recognition to Aid in Deaf Communication

<p>
  <img src="https://img.shields.io/badge/Android-8.0+-3DDC84?style=flat-square&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white"/>
  <img src="https://img.shields.io/badge/MediaPipe-0F9D58?style=flat-square&logo=google&logoColor=white"/>
  <img src="https://img.shields.io/badge/TensorFlow%20Lite-FF6F00?style=flat-square&logo=tensorflow&logoColor=white"/>
</p>

<br>

<p>
  <a href="#features"><strong>Features</strong></a> ·
  <a href="#project-structure--tech-stack"><strong>Project Structure</strong></a> ·
  <a href="#installation"><strong>Installation</strong></a> ·
  <a href="#download"><strong>Download</strong></a>
</p>
<br>
</div>

<h2 align="center">Preview</h2>

<p align="center">
  <img src="assets/Start.jpg" width="22.8%"/><img src="assets/Signup.jpg" width="22.8%"/><img src="assets/Login.jpg" width="22.8%"/><img src="assets/ForgotPass.jpg" width="22.8%"/>
</p>

<p align="center">
  <img src="assets/Library.jpg" width="22.8%"/><img src="assets/Sign.jpg" width="22.8%"/><img src="assets/Mastery.jpg" width="22.8%"/><img src="assets/Quiz1.jpg" width="22.8%"/>
</p>

<p align="center">
  <img src="assets/Quiz2.jpg" width="22.8%"/><img src="assets/Quiz3.jpg" width="22.8%"/><img src="assets/SignToText.jpg" width="22.8%"/><img src="assets/TextToSign.jpg" width="22.8%"/>
</p>

<br><h2 align="center">Features</h2>

<table align="center" width="100%">
<tr>
<td width="50%" valign="top">

<div align="center"><h3>Learn</h3></div><br>

- **Categorized vocabulary** — Signs organized by topic for easy browsing.
- **Video demonstrations** — Clear video for every sign in the dictionary.
- **Searchable FSL dictionary** — Look for any sign you need to find quickly.
<br>
</td>
<td width="50%" valign="top">

<div align="center"><h3>Translate</h3></div><br>

- **FSL ↔ Text translation** — Convert between FSL and written text instantly.
- **Real-time gesture recognition** — Detects hand signs live through the camera.
- **On-device inference** — Runs fully offline for speed and privacy.
<br>
</td>
</tr>
<tr>
<td width="50%" valign="top">

<div align="center"><h3>Practice</h3></div><br>

- **Interactive quizzes** — Test your knowledge with hands-on exercises.
- **Progress tracking** — Monitor your improvement across lessons.
- **Multiple difficulty levels** — Learn at a pace that fits your skill level.
<br>
</td>
<td width="50%" valign="top">

<div align="center"><h3>Accounts & Sync</h3></div><br>

- **Google Sign-In and Guest account** — Sign in with little to no setup required.
- **Cloud sync with Firebase** — Keep your data backed up and up to date.
- **Personalized learning progress** — Pick up right where you left off.
<br>
</td>
</tr>
</table>

<br><h2 align="center">Project Structure & Tech Stack</h2>

```text
SignSaya
│
├── app
│   ├── screens/         # UI screens
│   ├── components/      # Reusable Compose components
│   ├── navigation/      # Navigation graph
│   ├── viewmodel/       # State management
│   ├── models/          # Data models
│   ├── firebase/        # Authentication & Firestore services
│   ├── utils/           # Shared utilities
│   └── assets/          # ML models and application resources
│
└── assets/              # README images and media
```

<table align="center" width="100%">
<tr>
<td width="30%"><b>Layer</b></td>
<td width="70%"><b>Technology</b></td>
</tr>
<tr>
<td>Frontend</td>
<td>Kotlin • Jetpack Compose • Material 3 • Lottie</td>
</tr>
<tr>
<td>Database</td>
<td>Firebase</td>
</tr>
<tr>
<td>Computer Vision</td>
<td>MediaPipe Hands • OpenCV</td>
</tr>
<tr>
<td>Machine Learning</td>
<td>TensorFlow Lite • Python • Google Collab</td>
</tr>
<tr>
<td>Design</td>
<td>Canva • Figma</td>
</tr>
</table>

<br><h2 align="center">Installation</h2>

Clone the repository and open it in Android Studio.

```bash
git clone https://github.com/Leewenctt/SignSaya.git
cd SignSaya
```

Add your Firebase configuration file:

```text
app/google-services.json
```

Sync Gradle and run the project on an Android device or emulator.

<br><h2 align="center">Requirements</h2>

- **Android Studio** (Hedgehog or newer)
- **Android SDK 24+**
- **Firebase project** with Authentication and Firestore enabled

<br><h2 align="center">Download</h2>

<div align="center">

Click the button below or get the APK from the [Releases](https://github.com/Leewenctt/SignSaya/releases/latest) page.


[![Download APK](https://img.shields.io/badge/Download-APK-brightgreen?style=for-the-badge&logo=android)](https://github.com/Leewenctt/SignSaya/releases/latest)

<br>
<br>
</div>
