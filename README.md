# NaviSense 🧭
**AI-Powered Multimodal Mobility Assistant for the Visually Impaired**

> **Notice**: NaviSense is an experimental assistive mobility companion built as a hackathon prototype. It is designed to demonstrate how multimodal AI can aid navigation. It is **NOT** a replacement for a cane, guide dog, or medical/safety-certified mobility aid.

---

## 🌟 Vision & Problem

For visually impaired individuals, navigation is much more than finding a destination on a map. Traditional GPS answers: **"Where am I?"** and **"Where do I go?"**  
NaviSense bridges the critical gap by answering: **"What is happening around me right now?"** and **"What action should I take immediately?"**

We operate on a strict accessibility philosophy:  
**1. Voice First** | **2. Haptics Second** | **3. Visual UI Third**

---

## 🚀 Key Features

### 🌍 Real-World Autonomy (Zero Fake Data)
NaviSense is powered entirely by real data streams:
- **Real Geocoding**: Native Android Geocoder with OpenStreetMap Nominatim fallback.
- **Real Routing**: Live pedestrian pathways generated via OpenStreetMap OSRM API.
- **Real Perception**: Live Google ML Kit on-device object detection—no fabricated obstacles or mock confidence values.

### 🗣️ Multimodal Localization & Voice
- **Indian Language Support**: Onboard selection for English, Hindi, Marathi, Bengali, Tamil, and Telugu. Fully integrated with Android `TextToSpeech` and `SpeechRecognizer`.
- **Natural Language Parsing**: Strips conversational filler (*"Can you please take me to..."*) so users can speak naturally.
- **Distance-Aware Warnings**: Warns users if a walking route is exceptionally long ($>4.0$ km) before starting.

### 📷 Live ML Perception & Sensors
- **CameraX + Google ML Kit**: Live bounding-box analysis running locally.
- **Spatial Awareness**: Evaluates obstacle occupancy ratio to provide honest, qualitative proximity alerts (*"Person appears close"*, *"Object ahead"*).
- **Zonal Mapping**: Splits the camera frame into `LEFT | CENTER | RIGHT` sectors to direct the user safely around hazards.
- **Motion Sensors**: Tracks device pitch via the accelerometer, warning users if the phone is pointed at the ground (*"Point phone camera forward for obstacle perception"*).

### 📳 Adaptive Haptic Language
- **Forward**: Double short pulse
- **Left / Turn Left**: Distinct left directional pulse pattern
- **Right / Turn Right**: Distinct right directional pulse pattern
- **Stop / Arrival**: Long strong confirmation pulse
- **Warning**: Rapid multi-pulse pattern for immediate hazards

### 🎨 Accessible "Light Mode" UI
- **TalkBack Optimized**: Semantic content descriptions mapped to all buttons.
- **Minimalist Design**: Zero cluttered dashboards, tiny charts, or confusing grids. 
- **High-Contrast**: Huge directional typography, $>56\text{dp}$ touch targets, and color-coded banners (Red = Danger, Green = Clear, Amber = Warning).

---

## 🏗️ Technical Architecture

```text
                                ┌────────────────────────┐
                                │   CameraX Live Stream  │
                                └───────────┬────────────┘
                                            │
                                ┌───────────▼────────────┐
                                │ ML Kit Object Detection│
                                │ (LEFT | CENTER | RIGHT)│
                                └───────────┬────────────┘
                                            │
Voice / Text Input                          │ (Perception Events)
         │                                  │
         ▼                                  ▼
GeocodingService ──► OSRM Routing ──► NavigationManager (State Machine)
                                            │
                                ┌───────────┴────────────┐
                                │                        │
                       VoiceManager (TTS)     HapticFeedbackManager
```

---

## 📱 Standard Demo Flow

1. **Language Selection**: On first launch, select your preferred language.
2. **Speak Destination**: Tap the massive `[ 🎙 ]` button and say a destination.
3. **Accessible Route**: NaviSense geocodes the request, evaluates OSRM paths, and displays route distances.
4. **Start Navigation**: Tap `[ Start Guided Navigation ]`.
   - Spoken: *"Continue straight on Walkway for 120 metres."* + Forward haptic.
5. **Live Perception**: The camera actively runs ML Kit in the top half of the screen.
6. **Obstacle Detection**: Point the camera at a person or object.
   - Spoken: *"Person ahead. Move slightly left."* + Left haptic.
   - UI: Red `🚨` obstacle banner displays dynamically.
7. **Off-Route & Rerouting**: (Simulated by physically walking away from the GPS waypoints or repeatedly facing blocked paths).
   - Spoken: *"You're off route. I'm finding a new path."*
8. **Arrival**: Reach the GPS target coordinate.
   - Spoken: *"You've arrived at your destination."* + Stop haptic.

> *Note: A dedicated Developer Simulation Panel is hidden behind a `🛠 Dev` toggle in the header for indoor judging environments where live walking isn't feasible.*

---

## 🛠️ Tech Stack & Requirements

- **Language**: Kotlin 2.0
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: Modular MVVM + Kotlin Coroutines/Flows
- **Computer Vision**: CameraX + Google ML Kit `object-detection`
- **Location & Routing**: `LocationManager` (GPS/Network), OSRM API, Haversine Math
- **Testing**: JUnit 4, Robolectric
- **Min SDK**: 26 (Android 8.0+)
- **Target SDK**: 35

---

## 🚀 Build & Run Instructions

```bash
# Clone repository
git clone https://github.com/your-username/NaviSense.git
cd NaviSense

# Run Unit Tests
./gradlew test

# Build Debug APK
./gradlew assembleDebug
```
The resulting APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.