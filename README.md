# ShibuSmart Browser

ShibuSmart Browser is an advanced adaptive Android browser that changes mode based on internet speed, offering a range of features to enhance your browsing experience.

## Features

### Adaptive Browsing
- Ultra-lite HTML browsing under 10kbps
- Full Chromium-like browsing on fast networks
- Auto-reload on reconnect
- Bandwidth usage graph

### Download Management
- Chrome-style download accelerator
- Smart video/mp3 downloader (from YouTube, social platforms)
- Pause-resume capable download manager
- Incognito encrypted downloads

### Security & Privacy
- VPN integration
- Full security features
- Ad blocker
- Incognito mode
- App lock system with PIN and fingerprint support

### Content Tools
- Offline reader
- Page-to-PDF converter
- AI text summarizer
- Translation feature

### Customization
- Bengali language support
- Dark/light mode
- Full theme and color customization
- User-defined UI themes
- Desktop mode

### Extensibility
- Modular plugin system for future features

## Installation

1. Download the latest APK from the releases section
2. Enable installation from unknown sources in your Android settings
3. Install the APK on your Android device

## Development Setup

### Prerequisites
- Android Studio 4.0+
- Android SDK 33+
- Java Development Kit 8+

### Building from Source
1. Clone the repository:
```
git clone https://github.com/shibu389/ShibuSmartBrowser.git
```

2. Open the project in Android Studio

3. Build the project:
```
./gradlew assembleDebug
```

4. The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`

## Project Structure

- `app/src/main/java/com/shibu/shibusmart/browser/` - Main application code
- `app/src/main/res/` - Resources (layouts, strings, etc.)
- `app/src/main/AndroidManifest.xml` - App manifest

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Android Open Source Project
- WebKit
- All contributors and testers