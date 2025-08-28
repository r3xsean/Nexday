# Nexday - Daily Task Planning App

> Simple daily planning for tomorrow. Plan in the evening, execute the next day.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📱 About

Nexday is a beautifully simple Android app focused on tomorrow's tasks. It helps you plan your day in advance and stay motivated with a built-in gamification system. With a clean, premium dark theme and intuitive 3-tab interface, managing your daily tasks has never been easier.

## ✨ Key Features

### 🎯 **Simple Task Management**
- **3-Tab Interface**: Yesterday | Today | Tomorrow
- **Intuitive Gestures**: Swipe left/right to move tasks between days
- **Smart Scheduling**: Add time-specific reminders to tasks
- **5 Difficulty Levels**: Very Easy to Very Hard with XP rewards

### 🎮 **Gamification System**
- **XP & Leveling**: Earn experience points for completing tasks
- **Difficulty-Based Rewards**: Higher difficulty = more XP (1-8 points)
- **Level Up Celebrations**: Full-screen confetti animations
- **Progress Tracking**: Visual progress bar showing XP towards next level

### 🔄 **Automatic Day Management**
- **Smart Rollover**: Tasks automatically migrate Tomorrow → Today → Yesterday
- **Customizable Timing**: Set your preferred rollover time (default: 3:00 AM)
- **Auto Cleanup**: Old tasks are automatically removed to keep your list clean

### 🎨 **Premium User Experience**
- **Multiple Themes**: Blue Ocean, Forest Green, Sunset Orange, Purple Night
- **Dark Mode Design**: Eye-friendly interface with Material 3 components
- **Smooth Animations**: XP gains, level-ups, and task interactions
- **Professional Drag & Drop**: Reorder tasks with smooth multi-position dragging

### 🔔 **Smart Notifications**
- **Daily Planning Reminders**: Never forget to plan your tomorrow
- **Task Reminders**: Get notified for time-specific tasks
- **Level Up Alerts**: Celebrate your achievements

## 📸 Screenshots

*Screenshots will be available once the app is released*

## 🚀 Download

### Latest Release
Download the APK from the [Releases](../../releases) section.

**System Requirements:**
- Android 7.0 (API level 24) or higher
- ~15MB storage space

### Installation
1. Download the APK from releases
2. Enable "Install from unknown sources" in your device settings
3. Install the APK
4. Open Nexday and start planning your tomorrow!

## 🏗️ Technical Architecture

### Built With
- **Language**: Kotlin 100%
- **Architecture**: MVVM with Clean Architecture
- **Database**: Room (SQLite) - completely offline
- **UI**: ViewBinding with Material 3 components
- **Dependency Injection**: Hilt
- **Background Tasks**: WorkManager for day rollover

### Key Components
- **Local-First**: All data stored locally, no internet required
- **Offline Capable**: Works completely without network access  
- **Battery Optimized**: Efficient background processing
- **Privacy Focused**: No data collection or user tracking

## 🌟 How It Works

### Daily Workflow
1. **Evening Planning**: Add tasks to Tomorrow tab with difficulty levels
2. **Automatic Migration**: At your set time (3 AM default), tasks move from Tomorrow → Today
3. **Daily Execution**: Complete today's tasks and earn XP
4. **Progress Tracking**: Watch your level increase as you complete more challenging tasks
5. **Cleanup**: Yesterday's tasks are automatically archived and cleaned up

### XP System
- **Very Easy**: 1 XP 🟢
- **Easy**: 2 XP 🟡  
- **Medium**: 3 XP 🟠
- **Hard**: 5 XP 🔴
- **Very Hard**: 8 XP 🟣

Level progression follows an exponential curve: `Level = floor(sqrt(totalXP/2.5)) + 1`

## 🛠️ Development

### Project Structure
```
app/src/main/java/com/nexday/app/
├── data/          # Database entities, DAOs, repositories
├── ui/            # Activities, fragments, adapters  
├── services/      # Background services and workers
├── utils/         # Utility classes and helpers
└── di/            # Dependency injection modules
```

### Phase-by-Phase Development
The app was built following an 11-phase development methodology:

- ✅ **Phase 1**: Foundation & Project Setup
- ✅ **Phase 2**: Database Layer (Room, entities, DAOs)
- ✅ **Phase 3**: Core UI Structure (3-tab interface)
- ✅ **Phase 4**: Task Display & Interaction
- ✅ **Phase 5**: Task Management (add, edit, delete)
- ✅ **Phase 6**: Gamification System (XP, levels, animations)
- ✅ **Phase 7**: Day Management (automatic rollover)
- ✅ **Phase 8**: Settings & Theme System
- ✅ **Phase 9**: Notification System  
- ✅ **Phase 10**: Polish & UX Enhancements
- ✅ **Phase 11**: Production-Ready Drag & Drop

## 🤝 Contributing

While this is currently a personal project, suggestions and feedback are welcome! Feel free to:

- Open an issue for bug reports or feature requests
- Share your experience using the app
- Suggest improvements to the user experience

## 🔒 Privacy & Security

- **100% Local**: All data stays on your device
- **No Network Access**: App works completely offline
- **No Analytics**: We don't track or collect any user data
- **No Accounts**: No sign-up or login required
- **Open Source**: Code is available for review

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Built with [Android Jetpack](https://developer.android.com/jetpack) components
- Uses [Material Design 3](https://material.io/design) for UI components
- Powered by [Room Database](https://developer.android.com/training/data-storage/room) for local storage
- Background tasks managed by [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)

---

**Made with ❤️ for productive people who like to plan ahead**

*Planning tomorrow, achieving today*