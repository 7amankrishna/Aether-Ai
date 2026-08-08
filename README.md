<div align="center">

# ⚡ Aether AI

### Frontier AI models in your pocket — powered by *your* API keys

**Run Claude Opus 4.8, GPT-5.5, Gemini 2.5 Pro, DeepSeek R1, Llama 3.3 and more — right from your smartphone.**

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](#-license)

[📥 Download](#-download) • [✨ Features](#-features) • [🤖 Supported Models](#-supported-providers--models) • [🚀 Getting Started](#-getting-started) • [🛠 Build From Source](#-build-from-source)

</div>

---

## 🌌 What is Aether AI?

**Aether AI** is a *bring-your-own-key* (BYOK) AI client for Android. Instead of paying for yet another subscription, you plug in your own API keys and get direct access to the world's most powerful AI models from a single, beautiful mobile app.

No middleman. No markup. No rate-limited "free tier" wrapper. **Your keys, your models, your data.**

> 💡 **Ever wanted Claude Opus 4.8 on your phone?** With Aether AI, just paste your Anthropic API key and start chatting with a frontier model on the go — the same goes for GPT-5.5, Gemini 2.5 Pro, and open-source giants like DeepSeek R1 and Llama 3.3 70B.

---

## ✨ Features

### 🔑 API-Key Based Model Access
- Connect **multiple AI providers simultaneously** with your own API keys
- Switch between models on the fly — use Gemini Flash for quick tasks, Opus 4.8 for deep reasoning
- **Smart fallback routing**: if one provider fails or hits a rate limit, Aether automatically retries with your backup provider
- Free-tier friendly — mix free models (Groq, OpenRouter, Gemini Flash) with premium ones

### 🧠 Offline Memory
- **On-device memory log** stored in a local database — your conversations and knowledge persist even without internet
- **Natural language memory search**: ask *"what was that warranty date I saved last month?"* and Aether finds it
- Memory stays on your device — searchable, private, and available offline

### 🎙️ Voice Assistant
- Built-in voice assistant sheet with support for **realtime voice-capable models** (Gemini Live Voice, GPT-5.5, Llama 3.3)
- Talk to frontier AI hands-free

### 📄 AI Document Intelligence
- **AI-powered document sorting** with automatic categorization
- **Image → PDF conversion** built in
- **Google Drive auto-organization** — Aether files your documents into the right Drive folders automatically
- **Expiry tracking**: Aether extracts expiry dates (IDs, warranties, subscriptions) and reminds you before they lapse

### 🔒 Security First
- **End-to-end encryption** for your stored documents and data
- API keys stored securely on-device — never sent to any third-party server
- Firebase Auth for account security

### 🖥️ Local Models Too
- **Ollama support** — point Aether at your own Ollama server and run models fully offline / self-hosted

---

## 🤖 Supported Providers & Models

| Provider | Example Models | Notes |
|---|---|---|
| **Anthropic** | Claude Opus 4.8, Sonnet, Haiku | Frontier reasoning on your phone |
| **OpenAI** | GPT-5.5, GPT-5.5 Mini | Flagship + fast automation |
| **Google Gemini** | Gemini 2.5 Pro, 2.5 Flash, Live Voice | Free tier available |
| **DeepSeek** | DeepSeek R1 | Open source reasoning |
| **Groq** | Llama 3.3 70B, Mistral Large | Blazing-fast free tier |
| **OpenRouter** | Qwen 3 72B, hundreds more | One key, many models |
| **Together AI** | Open-source model catalog | OpenAI-compatible |
| **Mistral** | Mistral Large | European AI |
| **NVIDIA NIM** | NIM-hosted models | Enterprise-grade inference |
| **Ollama** | Any local model | 100% offline, self-hosted |

*Any OpenAI-compatible endpoint works — the list keeps growing.*

---

## 📥 Download

<div align="center">

### [⬇️ Download Aether AI v1.0 (APK)](https://github.com/7amankrishna/Aether-Ai/raw/main/com.aman.ai_1.0.apk)

[![Direct APK](https://img.shields.io/badge/Direct%20APK-v1.0-3DDC84?logo=android&logoColor=white&style=for-the-badge)](https://github.com/7amankrishna/Aether-Ai/raw/main/com.aman.ai_1.0.apk)
[![Get it on Google Play](https://img.shields.io/badge/Google%20Play-Coming%20Soon-414141?logo=googleplay&logoColor=white&style=for-the-badge)](#)

*Requires Android 8.0+ · Enable "Install from unknown sources" to install the APK*

</div>

---

## 🚀 Getting Started

1. **Install** Aether AI (see [Download](#-download))
2. **Sign in** to create your account
3. Open **Settings → AI Providers** and paste an API key for any provider:
   - Anthropic → [console.anthropic.com](https://console.anthropic.com/)
   - OpenAI → [platform.openai.com](https://platform.openai.com/)
   - Gemini → [aistudio.google.com](https://aistudio.google.com/) *(free tier)*
   - Groq → [console.groq.com](https://console.groq.com/) *(free tier)*
   - OpenRouter → [openrouter.ai](https://openrouter.ai/)
4. **Pick your model** and start chatting — Opus 4.8 on a smartphone, no laptop required
5. *(Optional)* Connect Google Drive for document auto-organization

---

## 🛠 Build From Source

**Prerequisites:** [Android Studio](https://developer.android.com/studio) (latest), JDK 17

```bash
git clone https://github.com/7amankrishna/Aether-Ai.git
cd Aether-Ai
```

1. Open the project in Android Studio and let Gradle sync
2. Copy `.env.example` → `.env` and add your `GEMINI_API_KEY` (used as the default provider)
3. Add your own `google-services.json` if you use your own Firebase project
4. Run on an emulator or physical device ▶️

### Tech Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- **Room** — offline memory & document database
- **WorkManager** — background expiry notifications
- **Firebase** — Auth & Firestore (provider config sync)
- **OkHttp** — unified client for all OpenAI-compatible APIs
- **Google Drive API** — document auto-organization

---

## 🗺 Roadmap

- [ ] Native Anthropic Messages API client (streaming)
- [ ] Chat export & sync
- [ ] More voice models
- [ ] iOS version
- [ ] Plugin / tool-use support

---

## 🤝 Contributing

Contributions are welcome! Feel free to open an [issue](https://github.com/7amankrishna/Aether-Ai/issues) or submit a pull request.

## 📄 License

This project is licensed under the MIT License — see the `LICENSE` file for details.

---

<div align="center">

**Aether AI** — *frontier intelligence, untethered.* ⚡

Made with ❤️ by [Aman Krishna](https://github.com/7amankrishna)

</div>
