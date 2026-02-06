# 🛡️ ToxiGuard – AI Toxicity Detection Android App

![Kotlin](https://img.shields.io/badge/Kotlin-Android-blue) ![Android](https://img.shields.io/badge/Platform-Android-green) ![ONNX](https://img.shields.io/badge/AI-ONNX-orange) ![NLP](https://img.shields.io/badge/Model-MiniLM-red)

ToxiGuard is a **production-focused AI-powered Android application** designed to detect toxic text in real time. The app performs **offline NLP inference** using a MiniLM ONNX model to analyze social media content and generate a toxicity score instantly — ensuring privacy, speed, and reliability without cloud dependency.

---

## 🚀 Key Features

* 🔍 Real-time toxicity detection from on-screen text
* 🤖 Offline AI inference using MiniLM ONNX model
* 📊 Toxicity score output (0–10 scale)
* 📱 Floating UI overlay for instant feedback
* 🔔 Smart notifications with toxicity results
* ⚡ Lightweight, fast, and privacy-first architecture

---

## 📸 Screenshots

![Home Screen](assets/screenshots/home_screen.jpeg)

![Analytical Dashboard](assets/screenshots/analytics_dashboard.jpeg)

![Notification](assets/screenshots/notifications.jpeg)

![Settings Screen](assets/screenshots/settings)

## 🧠 Architecture Diagram

![Architecture Diagram](assets/architecture/architectural_model.jpeg)

User Screen → Accessibility Service → Text Extraction → ONNX Runtime → MiniLM Model → Toxicity Score → Floating UI + Notifications

## 🛠️ Tech Stack

**Android Development**

* Kotlin
* Android Studio
* Jetpack Navigation
* Material Design


**AI / Machine Learning**

* MiniLM NLP Model
* ONNX Runtime (Offline Inference)
* Text Toxicity Scoring

**Core Components**

* Accessibility Service
* Floating Overlay System
* Background Processing

---

## ⚙️ How It Works

1. The app reads visible text using Android Accessibility Service.
2. Extracted text is processed locally through ONNX Runtime.
3. MiniLM model predicts a toxicity score.
4. Results are displayed using floating overlay and notifications.

This architecture ensures **fast inference, offline privacy, and efficient mobile AI deployment**.

---

## 📂 Project Structure

* `app/` – Main Android application code
* `ui/` – Activities, Fragments, and UI components
* `model/` – ONNX model loading and inference logic
* `service/` – Accessibility service & background tasks
* `utils/` – Helper classes and utilities

---

## 🎯 Use Cases

* Detect toxic comments on social media platforms
* Promote safer online communication
* Demonstrate offline mobile AI deployment
* NLP experimentation on Android devices

---

## 📈 Future Improvements

* Multi-class toxicity detection
* Model quantization for faster inference
* Analytics dashboard for toxicity trends
* Multi-language NLP support

---

## 🤝 Contributing

Contributions and improvements are welcome. Fork the repo and create a pull request.

---

## 📄 License

This project is built for educational, research.