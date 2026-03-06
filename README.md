# SlideControl 🎛️

Control your Linux presentation slides using your Android phone's volume buttons — even with the screen off!

---

## How It Works

```
Volume UP/DOWN (screen off)
        ↓
Android App (background foreground service)
        ↓
HTTP request over Wi-Fi
        ↓
Python Flask server on Linux
        ↓
xdotool simulates Arrow keys
        ↓
Slides change! 🎉
```

---

## Part 1: Linux Server Setup

### 1. Install dependencies
```bash
sudo apt install python3-flask xdotool -y
```

### 2. Run the server
```bash
python3 server.py
```

It will print your laptop's IP address — note it down for the Android app.

---

## Part 2: Android App Setup

### Option A: Build with Android Studio (Recommended)
1. Open Android Studio
2. Open this project folder (`SlideControl/`)
3. Click **Run** or **Build → Generate Signed APK**
4. Install the APK on your phone

### Option B: Build online
1. Upload this project to [https://appetize.io](https://appetize.io) or use GitHub Actions

---

## Using the App

1. Open **SlideControl** app on your phone
2. Enter your **laptop IP address** (shown when server.py starts)
3. Port is `5000` by default
4. Tap **Save Settings**
5. Tap **Test Connection** buttons to verify it works
6. Toggle the **Enable Volume Control** switch ON
7. Lock your phone screen
8. Press **Volume UP** → Next slide ▶
9. Press **Volume DOWN** → Previous slide ◀

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Can't connect | Make sure phone and laptop are on same Wi-Fi |
| xdotool not working | Run `export DISPLAY=:0` then try again |
| Service stops when screen off | Go to Settings → Apps → SlideControl → Battery → Unrestricted |
| Port blocked | Run `sudo ufw allow 5000` on laptop |

---

## File Structure

```
SlideControl/
├── server.py                          ← Run this on Linux
├── README.md
└── app/src/main/
    ├── AndroidManifest.xml
    ├── java/com/slidecontrol/
    │   ├── MainActivity.java          ← UI with IP input box
    │   ├── VolumeService.java         ← Foreground service
    │   └── MediaSessionHelper.java   ← Volume button interceptor
    └── res/
        └── layout/activity_main.xml  ← App UI layout
```
