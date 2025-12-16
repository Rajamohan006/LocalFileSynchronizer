<div align="center">

# 📁 Local File Synchronizer

### Fast • Secure • Peer-to-Peer File Sync Across Devices

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Mac%20%7C%20Windows%20%7C%20Linux-lightgrey.svg)]()

🚀 Real-Time Sync | No Cloud | No Internet | Cross-Platform | Conflict Resolution | Offline Support

</div>

<hr/>

## 📖 Table of Contents

* [Problem Statement](#-problem-statement)
* [Features](#-features)
* [Architecture](#%EF%B8%8F-architecture)
* [Project Structure](#-project-structure)
* [Getting Started](#-getting-started)
* [Configuration](#%EF%B8%8F-configuration)
* [Running the Application](#%EF%B8%8F-running-the-application)
* [CLI Commands](#-cli-commands)
* [How It Works](#-how-it-works)
* [Testing](#-testing)
* [Security](#%EF%B8%8F-security-considerations)
* [Troubleshooting](#-troubleshooting)
* [Technologies](#-technologies-used)
* [Contributing](#-contributing)
* [License](#-license)

<hr/>

## 🎯 Problem Statement

As developers, we often work across multiple devices (office desktop, home laptop). The challenges:

* 📦 **Manual file transfers** via USB drives or email
* ☁️ **Cloud dependency** - slow uploads/downloads, requires internet
* 🔀 **Incomplete commits** - pushing unfinished work just to switch devices
* 🚨 **Production deployments** - critical files needed across devices instantly
* ⚠️ **Version conflicts** - lost work due to overwriting files

### ✅ Solution

**This project solves all these problems** with automatic, real-time, peer-to-peer synchronization.

<hr/>

## ✨ Features

### Core Functionality

* ✅ **Automatic Peer Discovery** - Devices find each other on LAN without configuration
* ✅ **Bidirectional Sync** - Changes flow in both directions (Mac ↔ Windows)
* ✅ **Real-Time Monitoring** - File changes detected and synced within seconds
* ✅ **Offline Support** - Work continues offline, syncs when peer comes online
* ✅ **Conflict Resolution** - Smart handling of simultaneous edits
* ✅ **Delta Sync** - Only changed files are transferred
* ✅ **Cross-Platform** - Works on Mac, Windows, and Linux

### Technical Features

* 🔒 **No Cloud** - Completely local, your files never leave your network
* ⚡ **Fast** - LAN speed (typically 100+ MB/s)
* 🔐 **Secure** - No internet exposure, no third-party access
* 💾 **Persistent** - SQLite metadata tracking with versioning
* 🔄 **Atomic Operations** - No partial writes or corrupted files
* 📊 **CLI Interface** - Simple commands to monitor and control sync

<hr/>

## 🏗️ Architecture

<div align="center">

```
┌─────────────────────────────────────────────────────────────┐
│                     APPLICATION LAYERS                      │
├─────────────────────────────────────────────────────────────┤
│  CLI Layer          ┌──────────────────────────────────┐   │
│  (User Interface)   │  EnhancedCLIController           │   │
│                     └──────────────┬───────────────────┘   │
├────────────────────────────────────┼────────────────────────┤
│  Control Layer      ┌──────────────▼───────────────┐       │
│                     │  Main (Orchestrator)         │       │
│                     └──────────────┬───────────────┘       │
├────────────────────────────────────┼────────────────────────┤
│  Network Layer      ┌──────────────▼───────────────┐       │
│                     │  PeerDiscovery (UDP)         │       │
│                     │  NetworkSyncServer (HTTP)    │       │
│                     │  NetworkSyncClient (HTTP)    │       │
│                     │  BidirectionalSyncEngine     │       │
│                     └──────────────┬───────────────┘       │
├────────────────────────────────────┼────────────────────────┤
│  Sync Layer         ┌──────────────▼───────────────┐       │
│                     │  DirectoryWatcher (NIO)      │       │
│                     │  SyncEngine (Local)          │       │
│                     │  TransferManager (Threads)   │       │
│                     │  ConflictResolver            │       │
│                     └──────────────┬───────────────┘       │
├────────────────────────────────────┼────────────────────────┤
│  Storage Layer      ┌──────────────▼───────────────┐       │
│                     │  MetadataStore (SQLite)      │       │
│                     │  ChecksumUtil (SHA-256)      │       │
│                     └──────────────────────────────┘       │
└─────────────────────────────────────────────────────────────┘
```

</div>

<hr/>

## 📂 Project Structure

```
LocalFileSynchronizer/
├── pom.xml                          # Maven build configuration
├── config.yaml                      # Application configuration
├── README.md                        # This file
│
├── src/
│   ├── main/
│   │   ├── java/org/example/
│   │   │   ├── Main.java           # Application entry point
│   │   │   │
│   │   │   ├── cli/
│   │   │   │   └── EnhancedCLIController.java
│   │   │   │
│   │   │   ├── config/
│   │   │   │   └── AppConfig.java  # YAML configuration loader
│   │   │   │
│   │   │   ├── metadata/
│   │   │   │   ├── MetadataStore.java        # Interface
│   │   │   │   ├── SqliteMetadataStore.java  # SQLite implementation
│   │   │   │   └── JsonMetadataStore.java    # JSON implementation
│   │   │   │
│   │   │   ├── model/
│   │   │   │   ├── FileMetadata.java         # File metadata model
│   │   │   │   ├── SyncAction.java           # Sync action model
│   │   │   │   └── SyncStats.java            # Statistics tracker
│   │   │   │
│   │   │   ├── network/
│   │   │   │   ├── PeerInfo.java             # Peer information
│   │   │   │   ├── PeerDiscovery.java        # UDP peer discovery
│   │   │   │   ├── NetworkSyncServer.java    # HTTP server
│   │   │   │   └── NetworkSyncClient.java    # HTTP client
│   │   │   │
│   │   │   ├── scanner/
│   │   │   │   └── DirectoryScanner.java     # Periodic deep scan
│   │   │   │
│   │   │   ├── sync/
│   │   │   │   ├── BidirectionalSyncEngine.java # P2P sync engine
│   │   │   │   ├── ConflictResolver.java        # Conflict handler
│   │   │   │   ├── SyncEngine.java              # Local sync
│   │   │   │   └── TransferManager.java         # File transfers
│   │   │   │
│   │   │   ├── util/
│   │   │   │   └── ChecksumUtil.java         # SHA-256 checksums
│   │   │   │
│   │   │   └── watcher/
│   │   │       └── DirectoryWatcher.java     # Real-time monitor
│   │   │
│   │   └── resources/
│   │       └── logback.xml                   # Logging configuration
│   │
│   └── test/                                 # Unit tests
│
├── logs/                                     # Log files (auto-generated)
├── metadata/                                 # SQLite database (auto-generated)
└── target/                                   # Compiled JAR (auto-generated)
```

<hr/>

## 🚀 Getting Started

### Prerequisites

* ☕ **Java 17 or higher** - [Download JDK](https://adoptium.net/)
* 📦 **Maven 3.6+** - [Download Maven](https://maven.apache.org/download.cgi)
* 🌐 **Two devices on the same network** (Mac, Windows, or Linux)

**Verify installation:**

```bash
java -version   # Should show 17 or higher
mvn -version    # Should show 3.6 or higher
```

<hr/>

## 🔨 Compilation

### Step 1: Clone the Repository

```bash
git clone https://github.com/yourusername/local-file-synchronizer.git
cd local-file-synchronizer
```

### Step 2: Build the Project

```bash
mvn clean package
```

**Expected Output:**

```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 15.234 s
```

This creates: `target/LocalFileSynchronizer-1.0-SNAPSHOT.jar`

<hr/>

## ⚙️ Configuration

### Step 1: Edit `config.yaml`

#### For Mac:

```yaml
peer:
  name: "Mac-Office"        # Unique name for this device
  port: 8765                # HTTP server port

sourceDirs:
  - "/Users/yourname/Documents/WorkFolder"

targetDir: "/Users/yourname/Documents/WorkFolder-backup"

sync:
  checksumAlgorithm: "SHA-256"
  chunkSize: 1048576        # 1MB chunks
  threadPoolSize: 4
  scanIntervalSeconds: 300  # Sync every 5 minutes

conflictPolicy: "KEEP_BOTH"

excludePatterns:
  - ".*\\.tmp$"
  - "\\.DS_Store$"

metadata:
  type: "SQLITE"
  dbPath: "./metadata/sync.db"

logging:
  level: "INFO"
  logFile: "./logs/sync.log"
  maxFileSize: "10MB"
  maxHistory: 7
```

#### For Windows:

```yaml
peer:
  name: "Windows-Laptop"    # Different name!
  port: 8765

sourceDirs:
  - "C:/Users/yourname/Documents/WorkFolder"

targetDir: "C:/Users/yourname/Documents/WorkFolder-backup"
# ... rest same as Mac
```

### Step 2: Create Work Folders

**Mac:**

```bash
mkdir -p ~/Documents/WorkFolder
mkdir -p ~/Documents/WorkFolder-backup
```

**Windows:**

```cmd
mkdir C:\Users\yourname\Documents\WorkFolder
mkdir C:\Users\yourname\Documents\WorkFolder-backup
```

<hr/>

## ▶️ Running the Application

### On Mac:

```bash
java -jar target/LocalFileSynchronizer-1.0-SNAPSHOT.jar config.yaml
```

### On Windows:

```cmd
java -jar LocalFileSynchronizer-1.0-SNAPSHOT.jar config.yaml
```

### Expected Output:

```
======================================================================
    LOCAL FILE SYNCHRONIZER - NETWORK EDITION
    Peer-to-Peer Real-Time File Mirroring
======================================================================

✓ Local synchronization: ACTIVE
✓ Network discovery: ACTIVE
✓ Peer-to-peer sync: ACTIVE

Type 'help' for available commands.
Type 'peers' to see discovered devices.

> 
```

<hr/>

## 🎮 CLI Commands

| Command | Description |
|---------|-------------|
| `status` | Show synchronization status and uptime |
| `stats` | Show detailed statistics (files synced, bytes copied) |
| `peers` | List discovered peer devices (online/offline) |
| `sync` | Manually trigger sync with all online peers |
| `help` | Show available commands |
| `stop` | Stop synchronization and exit |

### Example Session:

```
> peers
--- DISCOVERED PEERS ---
Total peers: 1

  ✓ ONLINE Windows-Laptop
     Address: 192.168.1.100:8765
     ID: abc123def456

> stats
--- SYNC STATISTICS ---
Files Synced: 15
Bytes Copied: 2.4 MB
Conflicts: 0
Errors: 0
Uptime: 00:15:23

> sync
▶ Triggering manual sync...
✓ Sync initiated. Check logs for progress.
```

<hr/>

## 🔥 How It Works

### 1. Peer Discovery (UDP Broadcast)

* Every device broadcasts its presence every 30 seconds
* Devices listen for broadcasts and maintain a peer registry
* Uses UDP port 9876 for discovery

### 2. File Monitoring (Java NIO WatchService)

* Watches for CREATE, MODIFY, DELETE events
* Immediate detection of file changes
* Recursive monitoring of subdirectories

### 3. Bidirectional Sync (HTTP)

* Periodic sync every 5 minutes (configurable)
* Compares file lists between peers
* Downloads only newer or missing files
* Uses HTTP on port 8765 for transfers

### 4. Conflict Resolution

* Detects when same file modified on both devices
* Policies: `KEEP_BOTH`, `KEEP_SOURCE`, `KEEP_TARGET`, `FAIL`
* Creates `.conflict` files with timestamps

### 5. Metadata Tracking (SQLite)

* Stores file path, size, modification time, checksum, version
* SHA-256 checksums for integrity verification
* Tracks sync history for conflict detection

<hr/>

## 🧪 Testing

### Test 1: File Creation

**On Device A:**

```bash
echo "Test content" > ~/Documents/WorkFolder/test.txt
```

**Wait 3-5 seconds, then on Device B:**

```bash
cat ~/Documents/WorkFolder/test.txt
# Output: Test content
```

**Expected Notification:**

```
🔔 NOTIFICATION: Received 1 file(s) from Mac-Office
```

### Test 2: File Modification

**On Device B:**

```bash
echo "Additional line" >> ~/Documents/WorkFolder/test.txt
```

**Device A automatically receives the update!**

### Test 3: Offline Sync

1. Create files on Device A while Device B is offline
2. Start Device B
3. Device B automatically receives all queued changes

<hr/>

## 🛡️ Security Considerations

* ✅ **Local Network Only** - No internet exposure
* ✅ **No Authentication** - Designed for trusted devices on same network
* ✅ **No Encryption** - Files transmitted in plain text over LAN
* ⚠️ **Firewall Required** - Ensure devices are behind router firewall
* ⚠️ **Not for Public Networks** - Use only on private, trusted networks

**For production use, consider adding:**

* TLS/SSL encryption for transfers
* Authentication tokens
* Access control lists

<hr/>

## 🐛 Troubleshooting

### Issue: "No peers discovered"

**Solution:**

* Ensure both devices on same Wi-Fi network
* Check firewall allows UDP port 9876 and TCP port 8765
* Verify both devices have same subnet (e.g., 192.168.1.x)

**Mac Firewall:**

```bash
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add /usr/bin/java
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --unblockapp /usr/bin/java
```

**Windows Firewall:**

```powershell
New-NetFirewallRule -DisplayName "Java File Sync" -Direction Inbound -Program "C:\Program Files\Java\jdk-17\bin\java.exe" -Action Allow
```

### Issue: "Address already in use"

**Solution:**

```bash
# Find process using port 9876
lsof -i :9876

# Kill the process
kill -9 <PID>
```

### Issue: "Files not syncing"

**Solution:**

* Check logs: `tail -f logs/sync.log`
* Verify paths in config.yaml are correct
* Ensure folders exist on both devices

<hr/>

## 📊 Performance

| Metric | Value |
|--------|-------|
| **Sync Latency** | 3-5 seconds (LAN) |
| **Transfer Speed** | Up to LAN speed (100+ MB/s) |
| **File Size Limit** | No limit (chunked transfer) |
| **Concurrent Files** | Configurable thread pool (default: 4) |
| **Memory Usage** | ~50-100 MB (depending on file count) |
| **CPU Usage** | Minimal (<5% idle, 10-20% during sync) |

<hr/>

## 🎓 Technologies Used

* **Java 17** - Core language
* **Maven** - Build & dependency management
* **Java NIO** - File monitoring (WatchService)
* **UDP** - Peer discovery (DatagramSocket)
* **HTTP** - File transfers (HttpServer, HttpURLConnection)
* **SQLite** - Metadata persistence
* **SLF4J + Logback** - Logging
* **Jackson** - YAML parsing
* **Concurrency** - ExecutorService, ScheduledExecutorService, ThreadPools

<hr/>

## 👨‍💻 Author

**Rajamohgan Reddy Sura**

* GitHub: [@Rajamohan006](https://github.com/Rajamohan006)
* LinkedIn: [Rajamohan Reddy Sura](https://www.linkedin.com/in/rajamohan-reddy-sura-2076b1283)

<hr/>

## 🙏 Acknowledgments

* Inspired by real-world need for seamless multi-device workflow
* Built with Java's powerful NIO and networking APIs
* Learned from distributed systems design patterns

<hr/>

## 📈 Future Enhancements

- [ ] Web UI dashboard
- [ ] Encryption (TLS/SSL)
- [ ] Authentication & access control
- [ ] Bandwidth throttling
- [ ] File versioning with history
- [ ] Selective sync (ignore certain folders)
- [ ] Cloud sync integration (optional)
- [ ] Docker containerization

<hr/>

<div align="center">

**Built with ❤️ to solve a real problem**

---

Made by [Rajamohan Reddy Sura](https://github.com/Rajamohan006) |

</div>
