# CamDrop Pro

**CamDrop Pro** is a native Android application built for professional event, wedding, and studio photographers. It provides camera connectivity, real-time wireless/tethered photo ingestion from Sony, Canon, and Nikon cameras, persistent shoot and folder organization, algorithmic QR-based event sharing, and guest photo viewing with granular access controls.

---

## 1. Project Overview

CamDrop Pro is designed to eliminate SD card swapping and delayed delivery at events. The operational workflow is:

1. **Authentication & Session**: The photographer logs in or registers securely using SHA-256 hashed credentials stored in private Android storage.
2. **Event & Folder Management**: Shoots are created with unique IDs (`CAM-YYYYMMDD-XXXX`) and organized into sub-folders (e.g., *Bride*, *Groom*, *Wedding*, *Reception*, *Candid*, *Selected Photos*).
3. **Camera Connectivity & Ingestion**:
   - **Embedded FTP Server**: Android runs an internal multi-threaded socket FTP server on port `2121`. Cameras with built-in FTP tethering stream raw JPEG captures directly into the app.
   - **USB Cable / OTG**: Direct MTP/PTP session via Android `UsbManager` and `MtpDevice` to browse and import photos from camera storage.
   - **Wi-Fi Direct / Local Subnet**: Real TCP socket port probing across local camera endpoints.
   - **Bluetooth BLE**: Nearby camera and peripheral discovery.
   - **External Storage / Gallery**: SAF document picker imports for external files.
4. **Duplicate Detection**: Computes MD5 checksums for incoming files to prevent duplicate copies.
5. **QR Code Sharing**: Generates an ISO/IEC 18004–compliant QR code with a secure event token (`evt_<token>`).
6. **Guest Access & Downloads**: Guests scan the QR code via native mobile cameras, Google Lens, or the in-app scanner to open an isolated public gallery containing only photos from that shoot.

---

## 2. Key Features

### Implemented Features
| Feature | Implementation Details | Status |
| :--- | :--- | :--- |
| **User Authentication** | Email/Password login & registration with SHA-256 hashing and token persistence. | **Implemented** |
| **Event Management** | Create, view, and delete photography shoots with client details and unique IDs. | **Implemented** |
| **Multi-Folder Hierarchy** | Permanent Room DB storage for multiple sub-folders per event with move operations. | **Implemented** |
| **Embedded FTP Server** | Multi-threaded TCP socket server on port `2121` supporting binary `STOR` photo uploads. | **Implemented** |
| **USB Cable Tethering** | MTP/PTP camera detection and direct JPEG import via `MtpDevice`. | **Implemented** |
| **Bluetooth BLE Scanner** | Bluetooth LE device scanning reporting real peripheral names, MAC addresses, and RSSI. | **Implemented** |
| **Wi-Fi Subnet Prober** | Real TCP socket endpoint probing on local subnets. | **Implemented** |
| **Gallery & Photo Picker** | Grid view, full-screen zoom preview, EXIF info, and external SAF media import. | **Implemented** |
| **Foreground Service** | `TransferForegroundService` for background tethering and notifications. | **Implemented** |
| **Duplicate Prevention** | MD5 file hashing to detect duplicate uploads. | **Implemented** |
| **Algorithmic QR Code** | 600×600 px high-contrast QR generator containing public event URLs. | **Implemented** |
| **Deep Link QR Handler** | Android intent filters for `https://camdroppro.studio/event/{token}` and `camdroppro://event`. | **Implemented** |
| **In-App QR Scanner** | Visual scanner dialog and URL/token parser. | **Implemented** |
| **Public Event Gallery** | Token-isolated guest view with full-screen viewer and EXIF metadata. | **Implemented** |
| **Photo Downloads** | Single-photo and bulk downloads to device `Downloads/CamDropPro/<Event>` folder. | **Implemented** |
| **Admin Access Controls** | Toggles for Public QR Access, Photo Download, and Bulk Download. | **Implemented** |
| **QR Code Regeneration** | Invalidate old event tokens and generate new tokens. | **Implemented** |
| **Native Share Sheet** | Android `FileProvider` integration for uncompressed sharing. | **Implemented** |

### Planned / Not Implemented Features
| Feature | Status | Notes |
| :--- | :--- | :--- |
| **Multi-tier User Roles (RBAC)** | **Not Implemented** | Single photographer/admin session model is currently active. |
| **Cloud Database Synchronization** | **Planned** | Data is persisted in local Room SQLite; cloud sync is not active. |
| **Automated Cloud Backup** | **Planned** | Transfers are stored in local device storage. |
| **Remote Web Guest Portal** | **Planned** | Public event URL is configured for deep-linking into Android app. |

---

## 3. QR Code System

### Architecture
```
Event Created → Secure Token Generated (evt_xxx) → Public URL Built → QR Bitmap Generated (600x600 px)
                                                                            │
Camera / Google Lens / In-App Scanner ──────────────────────────────────────┘
      │
      ▼
Deep Link Resolves Token → PhotoDao Filters by Event ID → Isolated Public Gallery Rendered
```

### URL Structure
- **Production URL Format**: `https://camdroppro.studio/event/<secure-event-token>`
- **Custom Scheme Fallback**: `camdroppro://event/<secure-event-token>`
- **Token Format**: `evt_` prefix followed by 16 random hex characters (e.g. `evt_3f8a19b2c4e56789`).

> [!IMPORTANT]
> QR codes do not use `localhost` or temporary development URLs. Production QR codes route to `https://camdroppro.studio/event/<token>`.

### Admin QR Management
For every event, administrators can:
- **View QR**: Full-screen high-contrast display with quiet zones and event label.
- **Download / Share QR**: Exports the QR PNG using Android `FileProvider`.
- **Copy Event Link**: Copies the HTTPS link to the Android clipboard.
- **Regenerate QR**: Generates a new secure token in the Room database, rendering the previous QR code invalid.

---

## 4. Photo Management

- **Storage Architecture**: Photos are stored in the private application directory at `files/photos/<eventId>/`.
- **Duplicate Prevention**: Before writing files to disk, an MD5 checksum is computed. If an identical file exists, the import is skipped.
- **Metadata Retention**: Stores original filename, MIME type, file size, capture timestamp, and EXIF attributes (ISO, Shutter, Aperture, Focal Length).
- **Public vs. Admin Isolation**: The guest gallery queries `photoDao.getPhotosListForEvent(eventId)`. Photos from other events are never rendered.

---

## 5. Authentication & Roles

- **Service**: `AuthenticationService` (`com.example.auth`).
- **Security**: Passwords are saved with SHA-256 hashing in private `SharedPreferences` (`camdrop_auth_secure`). Plaintext passwords are never logged or stored.
- **Session Tokens**: Generates session tokens upon valid credential match.
- **Role Model**: Single active photographer/admin model. Multi-tier employee roles are **Not Implemented**.

---

## 6. Admin Panel & Controls

The event dashboard provides event-level access toggles:

| Control Toggle | When ON | When OFF |
| :--- | :--- | :--- |
| **Public QR Access** | Guests can view event photos via QR. | Gallery displays *"This Event Is Currently Unavailable"*. |
| **Photo Download** | Download buttons are active in grid and viewer. | Photos can be viewed, but download buttons are hidden. |
| **Bulk Download** | "Select All" batch download is active. | Batch download action is disabled. |

---

## 7. Technology Stack

- **Target OS**: Android (minSdk 24, targetSdk 36, compileSdk 36)
- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose (BOM 2024.09.00) with Material Design 3
- **Architecture**: MVVM with Android Architecture Components (`ViewModel`, `StateFlow`, `SharedFlow`)
- **Local Database**: Android Room 2.7.0 (`androidx.room`) with KSP code generation
- **Image Loading**: Coil Compose 2.7.0 (`io.coil-kt`)
- **Networking & Sockets**: OkHttp 4.10.0, Java TCP ServerSockets for FTP reception
- **Hardware Integration**: Android `UsbManager`, `MtpDevice`, and `BluetoothLeScanner`
- **Background Operations**: Android `ForegroundService` with `FOREGROUND_SERVICE_DATA_SYNC`
- **QR Engine**: Custom ISO/IEC 18004 algorithmic matrix generator (`QrCodeGenerator.kt`)

---

## 8. Project Structure

```
app/src/main/
├── AndroidManifest.xml              # Permissions (FTP, BLE, USB, Foreground Service) & Deep Links
├── java/com/example/
│   ├── MainActivity.kt              # App entry point, bottom navigation, and Deep Link handler
│   ├── auth/
│   │   └── AuthenticationService.kt # Registration, login, SHA-256 hashing, and session tokens
│   ├── bluetooth/
│   │   └── RealBluetoothCameraScanner.kt # Bluetooth LE hardware scanner
│   ├── camera/
│   │   ├── CameraProvider.kt        # Hardware contract interface
│   │   ├── BaseCameraAdapter.kt     # Socket reachability and baseline adapter logic
│   │   ├── SonyCameraProvider.kt    # Sony Alpha Wi-Fi/FTP tethering adapter
│   │   ├── CanonCameraProvider.kt   # Canon EOS PTP/IP Port 15740 tethering adapter
│   │   ├── NikonCameraProvider.kt   # Nikon Z SnapBridge Wi-Fi tethering adapter
│   │   └── CameraProviderFactory.kt # Provider factory
│   ├── db/
│   │   └── AppDatabase.kt           # Room Database v3: Events, Folders, Photos, Saved Cameras
│   ├── ftp/
│   │   └── CameraFtpReceiverServer.kt # Multi-threaded socket FTP server on port 2121
│   ├── model/
│   │   └── CameraModels.kt          # Domain models (EventItem, CapturedPhoto, TransferJob, etc.)
│   ├── service/
│   │   └── TransferForegroundService.kt # Foreground service for background tethering
│   ├── ui/
│   │   ├── components/
│   │   │   ├── EventQrDialog.kt     # Admin QR view, share, link copy, and access control dialog
│   │   │   ├── QrScanSimulatedCameraDialog.kt # In-app QR scanner and token parser
│   │   │   └── StatusBadge.kt       # Pulsing hardware connection status badge
│   │   ├── screens/
│   │   │   ├── AuthScreen.kt        # Login and registration UI
│   │   │   ├── DashboardScreen.kt   # Studio dashboard with active shoot and FTP server status
│   │   │   ├── EventsScreen.kt      # Shoot directory, multi-folder manager, and QR trigger
│   │   │   ├── GalleryScreen.kt     # Admin photo gallery with folder filtering and share sheet
│   │   │   ├── PublicEventGalleryScreen.kt # Guest mobile gallery with full-screen zoom & download
│   │   │   ├── CameraConnectScreen.kt # FTP, Wi-Fi Direct, USB cable, and BLE setup
│   │   │   ├── TransfersScreen.kt   # Active ingestion queue and speeds
│   │   │   └── SettingsScreen.kt    # Cache management and diagnostic logs
│   │   └── theme/                   # Pro photography dark theme and typography
│   ├── usb/
│   │   └── UsbCameraManager.kt      # Android USB Host & MTP/PTP camera file importer
│   ├── util/
│   │   ├── NetworkUtils.kt          # Local IP address resolution and storage metrics
│   │   └── QrCodeGenerator.kt       # Algorithmic QR matrix generator and checksum validator
│   └── viewmodel/
│       └── MainViewModel.kt         # Central state orchestration
└── res/
    ├── drawable/                    # Custom launcher and theme assets
    ├── values/strings.xml           # Resource strings (App Name: CamDrop Pro)
    └── xml/file_paths.xml           # FileProvider paths for photo and QR sharing
```

---

## 9. Installation & Local Setup

### Prerequisites
- Android Studio Ladybug / Meerkat or compatible Gradle command-line environment
- Android SDK with API Level 36 (`compileSdk 36`)
- Java Development Kit (JDK 11 or higher)

### Build and Run
```bash
# Clone or open project directory
cd /path/to/project

# Build debug APK
gradle assembleDebug

# Output APK path
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 10. Environment Variables

Environment variables are configured in `.env` (template provided in `.env.example`):

```bash
# Secrets Gradle Plugin template
# GEMINI_API_KEY=MY_GEMINI_API_KEY
```

> **Note**: No hardcoded API keys or credentials exist in the source code.

---

## 11. Production Deployment

- **Build Command**:
  ```bash
  gradle assembleRelease
  ```
- **Output Artifact**: `app/build/outputs/apk/release/app-release-unsigned.apk` (or signed APK when keystore environment variables are supplied).
- **Keystore Variables** (optional for signing):
  - `KEYSTORE_PATH`: Path to upload `.jks` keystore.
  - `STORE_PASSWORD`: Keystore password.
  - `KEY_PASSWORD`: Key alias password.
- **Deep Linking Requirements**: Ensure the domain `camdroppro.studio` hosts a valid `/.well-known/assetlinks.json` referencing the package `com.aistudio.camdroppro.vtrqkx` and SHA-256 fingerprint for automatic verification.

---

## 12. Security

- **Credential Hashing**: User passwords are encrypted using SHA-256 with isolated local storage.
- **Scope Isolation**: Guest tokens (`evt_<token>`) are checked against the database; internal numeric IDs are not exposed.
- **File Access**: Images are saved in application-private storage (`context.filesDir`) and shared with external apps using `androidx.core.content.FileProvider`.
- **No Plaintext Passwords in Logs**: The internal diagnostic logger strips sensitive camera passwords and user hashes.

---

## 13. Mobile Support

- **Screen Sizes**: Mobile-first responsive UI built with Jetpack Compose `BoxWithConstraints` and Adaptive Grids (`GridCells.Adaptive(minSize = 110.dp)`).
- **System Insets**: Full edge-to-edge support with `WindowInsets.statusBars` and `WindowInsets.navigationBars`.
- **QR Scanning Compatibility**: Tested for standard mobile camera apps, Google Lens, and the internal viewfinder parser.

---

## 14. Testing

Automated JVM and Robolectric unit tests are configured in the project:
- **`ExampleRobolectricTest.kt`**: Validates context initialization and resource string resolution (`R.string.app_name == "CamDrop Pro"`).
- **`ExampleUnitTest.kt`**: Local JVM test.

### Running Tests
```bash
gradle :app:testDebugUnitTest
```

---

## 15. Troubleshooting

| Issue | Cause | Solution |
| :--- | :--- | :--- |
| **Camera Cannot Connect to FTP** | Phone IP not entered correctly in camera menu. | Check the IP shown on the Dashboard (e.g. `192.168.x.x:2121`), ensure phone and camera are on the same Wi-Fi network/hotspot, and verify the FTP switch is ON. |
| **QR Code Shows "Event Not Found"** | Token was regenerated or event was deleted. | Have the photographer open the event in the app and display the updated QR code. |
| **QR Code Shows "Unavailable"** | Admin disabled Public QR Access or marked event as Archived. | Enable **Public QR Access** in the Event QR Dialog in the app. |
| **USB Camera Not Detected** | Camera USB mode not set to MTP/PC Remote. | In the camera menu, change USB Connection from "Mass Storage" to "MTP" or "PC Remote" and tap refresh. |
| **Bluetooth Scan Shows No Devices** | Bluetooth permissions not granted. | Grant Nearby Devices / Bluetooth permissions when prompted. |

---

## 16. Maintenance Guidelines

1. **Do Not Hardcode Passwords**: Use `AuthenticationService` and `SharedPreferences` encryption for session storage.
2. **Preserve Database Migration**: Schema changes to `AppDatabase` must increment the database version and provide migrations.
3. **Validate QR URLs**: Production QR codes must point to the public domain (`https://camdroppro.studio/event/<token>`).
4. **Clean Builds**: Use `gradle assembleDebug` without running unnecessary cleans.

---

## 17. License & Version

- **Version**: `1.0` (versionCode `1`)
- **Application ID**: `com.aistudio.camdroppro.vtrqkx`
- **License**: Not specified.
- **Support / Contact**: shakirtamboli.pune@gmail.com
