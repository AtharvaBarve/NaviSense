# NaviSense 🧭
**AI-Powered Multimodal Mobility Assistant for the Visually Impaired**

> **Hackathon Prototype Notice**: NaviSense is an experimental assistive mobility companion built as a 2–3 day hackathon vertical slice. It is **NOT** a replacement for a cane, guide dog, or medical/safety-certified mobility aid.

---

## 🌟 Vision & Problem

For visually impaired individuals, navigation is much more than finding a destination on a map. A route may be optimal on traditional GPS maps but become physically impassable due to:
- Unexpected obstacles, construction, or pedestrians
- High-traffic crossings without audio signals
- Uneven or poor pedestrian paths
- Sudden environmental blockages

Existing tools answer: **"Where am I?"** and **"Where do I go?"**  
NaviSense answers the missing gap: **"What is happening around me right now?"** and **"What action should I take immediately?"**

---

## 🚀 Key Features

1. **Multimodal Voice Guidance & Input**:
   - Spoken destination recognition (`Android SpeechRecognizer`).
   - Short, actionable TTS voice prompts (`Android TextToSpeech`) like *"Continue straight for 20 metres"*, *"Obstacle ahead. Move slightly left"*, or *"Your current path is blocked. Recalculating."*

2. **Adaptive Haptic Language System**:
   - **Forward**: Double short pulse
   - **Left / Turn Left**: Distinct left directional pulse pattern
   - **Right / Turn Right**: Distinct right directional pulse pattern
   - **Stop / Arrival**: Long strong pulse
   - **Warning**: Rapid multi-pulse pattern

3. **Accessible Route Selection**:
   - Route Engine evaluates simulated candidate routes favoring safety & accessibility (e.g., lower traffic, fewer major crossings, smooth pedestrian paths over purely shortest distance).

4. **Live Computer Vision & Perception Layer**:
   - Real-time CameraX video analysis dividing the frame into 3 perception zones: `LEFT | CENTER | RIGHT`.
   - Real-time obstacle analyzer detecting obstacles ahead or on flanks, firing directional voice and haptic guidance.
   - Throttled alert frequency to avoid overwhelming the user with speech alerts.

5. **Dynamic Rerouting & Blocked Path Detection**:
   - Automatically recalculates a clear detour route when the primary path is blocked.

6. **Interactive Hackathon Demo Panel**:
   - Dedicated on-screen controls for judges and developers to reliably trigger demo scenarios (*Start Nav*, *Simulate Obstacle*, *Simulate Blocked Path*, *Arrived*, *Reset*).

---

## 🏗️ Technical Architecture

```
                                ┌────────────────────────┐
                                │   CameraX Live Stream  │
                                └───────────┬────────────┘
                                            │
                                ┌───────────▼────────────┐
                                │    ObstacleAnalyzer    │
                                │ (LEFT | CENTER | RIGHT)│
                                └───────────┬────────────┘
                                            │
Voice / Text Input                          │ (Perception Events)
         │                                  │
         ▼                                  ▼
DestinationParser ──► RouteEngine ──► NavigationManager (State Machine)
                                            │
                                ┌───────────┴────────────┐
                                │                        │
                       VoiceManager (TTS)     HapticFeedbackManager
```

---

## 📱 60-Second Hackathon Demo Flow

1. **Speak Destination**: Tap `[ 🎙 Speak ]` or select preset *"Railway Station"*.
2. **Accessible Route Selected**: NaviSense selects **Route B (Accessible Pathway)** (1 crossing, low traffic).
3. **Start Navigation**: Tap `[ Start Guided Navigation ]`.
   - Spoken: *"Continue straight on Quiet Ave for 20 metres."*
   - Haptic: Forward double pulse.
4. **Live Perception / Camera**: Camera is active on-screen analyzing sectors `LEFT | CENTER | RIGHT`.
5. **Obstacle Detection**: Point camera at obstacle or tap `[ ⚠️ Obstacle ]`.
   - Spoken: *"Obstacle ahead. Move slightly left."*
   - Haptic: Left pulse.
6. **Blocked Path Reroute**: Tap `[ 🛑 Blocked Path ]`.
   - Spoken: *"Your current path appears blocked. Recalculating."*
   - Detour selected -> Spoken: *"Turn right in 10 metres to bypass blocked path."*
   - Haptic: Right pulse.
7. **Arrival**: Tap `[ 🏁 Arrived ]`.
   - Spoken: *"You have arrived at your destination."*
   - Haptic: Stop pulse.

---

## 🛠️ Tech Stack & Requirements

- **Language**: Kotlin 2.0
- **UI Framework**: Jetpack Compose + Material 3 (Dark, High Contrast)
- **Architecture**: Modular MVVM + Centralized State Machine
- **Camera**: CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`)
- **Voice**: Android `SpeechRecognizer` + `TextToSpeech`
- **Haptics**: Android `Vibrator` / `VibratorManager`
- **Min SDK**: 26 (Android 8.0+)
- **Target SDK**: 35

---

## 🛠️ Build & Run Instructions

```bash
# Clone repository
git clone https://github.com/example/NaviSense.git
cd NaviSense

# Build debug APK
./gradlew assembleDebug
```
The resulting APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.
# NaviSense
