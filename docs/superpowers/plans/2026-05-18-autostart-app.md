# AutoStart Android App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an Android app that auto-launches user-configured apps on device boot, with dual boot strategy (BOOT_COMPLETED broadcast + Launcher fallback).

**Architecture:** MVVM with Room database. BootReceiver triggers AppLaunchService (foreground) which reads app list from Room and launches them sequentially with configurable delays. MainActivity provides Compose UI for managing the app list. LauncherActivity serves as a minimal fallback home screen.

**Tech Stack:** Kotlin 2.0, Jetpack Compose + Material3, Room 2.6.1, AGP 8.5, targetSdk 34, minSdk 24

---

## File Structure

```
autostart/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── kotlin/com/autostart/
│       │   │   ├── AutoStartApp.kt
│       │   │   ├── data/local/
│       │   │   │   ├── AppEntity.kt
│       │   │   │   ├── AppDao.kt
│       │   │   │   ├── AppDatabase.kt
│       │   │   │   └── AppRepository.kt
│       │   │   ├── receiver/
│       │   │   │   └── BootReceiver.kt
│       │   │   ├── service/
│       │   │   │   └── AppLaunchService.kt
│       │   │   ├── ui/
│       │   │   │   ├── theme/
│       │   │   │   │   ├── Color.kt
│       │   │   │   │   ├── Theme.kt
│       │   │   │   │   └── Type.kt
│       │   │   │   ├── main/
│       │   │   │   │   ├── MainActivity.kt
│       │   │   │   │   └── MainViewModel.kt
│       │   │   │   ├── launcher/
│       │   │   │   │   └── LauncherActivity.kt
│       │   │   │   └── components/
│       │   │   │       ├── AppListItem.kt
│       │   │   │       ├── AppPickerSheet.kt
│       │   │   │       └── DelayPickerDialog.kt
│       │   │   └── util/
│       │   │       └── PackageManagerHelper.kt
│       │   └── res/
│       │       ├── values/
│       │       │   ├── strings.xml
│       │       │   ├── colors.xml
│       │       │   └── themes.xml
│       │       ├── drawable/
│       │       │   └── ic_notification.xml
│       │       ├── mipmap-hdpi/
│       │       ├── mipmap-mdpi/
│       │       ├── mipmap-xhdpi/
│       │       ├── mipmap-xxhdpi/
│       │       ├── mipmap-xxxhdpi/
│       │       └── xml/
│       │           └── backup_rules.xml
│       └── test/
│           └── kotlin/com/autostart/
│               ├── data/local/
│               │   └── AppRepositoryTest.kt
│               └── ui/main/
│                   └── MainViewModelTest.kt
```

---

### Task 1: Project Scaffolding

**Files:**
- Create: `build.gradle.kts`
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/src/main/res/drawable/ic_notification.xml`
- Create: `app/src/main/res/xml/backup_rules.xml`

- [ ] **Step 1: Create version catalog**

Create `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.0.21"
agp = "8.5.0"
compose-bom = "2024.10.00"
room = "2.6.1"
lifecycle = "2.8.6"
activity-compose = "1.9.2"
core-ktx = "1.13.1"
coroutines = "1.9.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "core-ktx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.27" }
```

- [ ] **Step 2: Create root build.gradle.kts**

Create `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 3: Create settings.gradle.kts**

Create `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AutoStart"
include(":app")
```

- [ ] **Step 4: Create gradle.properties**

Create `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 5: Create app/build.gradle.kts**

Create `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.autostart"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.autostart"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
        getByName("test") {
            java.srcDirs("src/test/kotlin")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation("junit:junit:4.13.2")
}
```

- [ ] **Step 6: Create app/proguard-rules.pro**

Create `app/proguard-rules.pro`:

```proguard
# Add project specific ProGuard rules here.
```

- [ ] **Step 7: Create AndroidManifest.xml**

Create `app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />

    <application
        android:name=".AutoStartApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.AutoStart">

        <!-- Main management activity -->
        <activity
            android:name=".ui.main.MainActivity"
            android:exported="true"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Launcher fallback activity -->
        <activity
            android:name=".ui.launcher.LauncherActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:stateNotNeeded="true"
            android:clearTaskOnLaunch="true"
            android:theme="@style/Theme.AutoStart">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>

        <!-- Boot receiver -->
        <receiver
            android:name=".receiver.BootReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter android:priority="1000">
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.QUICKBOOT_POWERON" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </receiver>

        <!-- App launch foreground service -->
        <service
            android:name=".service.AppLaunchService"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="Auto-launch configured apps on device boot" />
        </service>

    </application>

</manifest>
```

- [ ] **Step 8: Create resource files**

Create `app/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">AutoStart</string>
    <string name="channel_name">Auto Launch Service</string>
    <string name="channel_description">Shows when auto-launching apps on boot</string>
    <string name="notification_title">AutoStart</string>
    <string name="notification_text">Launching configured apps…</string>
    <string name="launcher_message">Auto-start service running…</string>
    <string name="main_title">Boot AutoStart Manager</string>
    <string name="add_app">Add App</string>
    <string name="delay_seconds">%ds</string>
    <string name="delay_label">Delay</string>
    <string name="remove_app">Remove</string>
    <string name="empty_list">No apps configured. Tap \"+\" to add.</string>
    <string name="enabled_label">Boot auto-start enabled</string>
    <string name="installed_apps">Installed Apps</string>
    <string name="save">Save</string>
    <string name="cancel">Cancel</string>
    <string name="seconds">seconds</string>
</resources>
```

Create `app/src/main/res/values/colors.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
</resources>
```

Create `app/src/main/res/values/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.AutoStart" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

Create `app/src/main/res/drawable/ic_notification.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M13,3c-4.97,0 -9,4.03 -9,9L1,12l3.89,3.89 0.07,0.14L9,12L6,12c0,-3.87 3.13,-7 7,-7s7,3.13 7,7 -3.13,7 -7,7c-1.93,0 -3.68,-0.79 -4.94,-2.06l-1.42,1.42C8.27,19.99 10.51,21 13,21c4.97,0 9,-4.03 9,-9s-4.03,-9 -9,-9zM12,8v5l4.28,2.54 0.72,-1.21 -3.5,-2.08L13.5,8L12,8z" />
</vector>
```

Create `app/src/main/res/xml/backup_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <include domain="sharedpref" path="." />
    <include domain="database" path="." />
</full-backup-content>
```

- [ ] **Step 9: Create placeholder source directories**

Run:

```bash
mkdir -p app/src/main/kotlin/com/autostart/{data/local,receiver,service,ui/{theme,main,launcher,components},util}
mkdir -p app/src/test/kotlin/com/autostart/{data/local,ui/main}
mkdir -p app/src/main/res/{mipmap-hdpi,mipmap-mdpi,mipmap-xhdpi,mipmap-xxhdpi,mipmap-xxxhdpi}
```

Create placeholder `app/src/main/kotlin/com/autostart/AutoStartApp.kt`:

```kotlin
package com.autostart

import android.app.Application

class AutoStartApp : Application()
```

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "chore: scaffold Android project with Gradle, manifest, and resources"
```

---

### Task 2: Data Layer — Room Entity, DAO, Database

**Files:**
- Create: `app/src/main/kotlin/com/autostart/data/local/AppEntity.kt`
- Create: `app/src/main/kotlin/com/autostart/data/local/AppDao.kt`
- Create: `app/src/main/kotlin/com/autostart/data/local/AppDatabase.kt`

- [ ] **Step 1: Create AppEntity**

Create `app/src/main/kotlin/com/autostart/data/local/AppEntity.kt`:

```kotlin
package com.autostart.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val label: String,
    val delayMs: Long = 0,
    val sortOrder: Int = 0,
    val enabled: Boolean = true
)
```

`★ Insight ─────────────────────────────────────`
Room uses `@Entity` to map a Kotlin data class to a SQLite table. The `autoGenerate = true` on `@PrimaryKey` makes Room assign incrementing IDs, so the `id` default of `0` is never actually stored — it's a sentinel telling Room to auto-generate.
`─────────────────────────────────────────────────`

- [ ] **Step 2: Create AppDao**

Create `app/src/main/kotlin/com/autostart/data/local/AppDao.kt`:

```kotlin
package com.autostart.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps WHERE enabled = 1 ORDER BY sortOrder ASC")
    fun getEnabledApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps ORDER BY sortOrder ASC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE enabled = 1 ORDER BY sortOrder ASC")
    suspend fun getEnabledAppsOnce(): List<AppEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(app: AppEntity): Long

    @Update
    suspend fun update(app: AppEntity)

    @Delete
    suspend fun delete(app: AppEntity)

    @Query("UPDATE apps SET sortOrder = :order WHERE id = :id")
    suspend fun updateOrder(id: Long, order: Int)

    @Query("DELETE FROM apps")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM apps")
    suspend fun count(): Int
}
```

`★ Insight ─────────────────────────────────────`
Room DAOs expose two flavors of queries: `Flow<>` return types (reactive — auto-update the UI when data changes) and `suspend` functions (one-shot). We need both: `Flow` for the UI list and `suspend` for the service that runs once at boot.
`─────────────────────────────────────────────────`

- [ ] **Step 3: Create AppDatabase**

Create `app/src/main/kotlin/com/autostart/data/local/AppDatabase.kt`:

```kotlin
package com.autostart.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AppEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "autostart.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/autostart/data/local/
git commit -m "feat: add Room database with AppEntity, AppDao, and AppDatabase"
```

---

### Task 3: Repository + Tests

**Files:**
- Create: `app/src/main/kotlin/com/autostart/data/local/AppRepository.kt`
- Create: `app/src/test/kotlin/com/autostart/data/local/AppRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/com/autostart/data/local/AppRepositoryTest.kt`:

```kotlin
package com.autostart.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
@SmallTest
class AppRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: AppRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(database.appDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetAllApps() = runTest {
        val app = AppEntity(
            packageName = "com.example.test",
            label = "Test App",
            delayMs = 1000,
            sortOrder = 0,
            enabled = true
        )
        repository.insert(app)

        val apps = repository.getAllApps().first()
        assertEquals(1, apps.size)
        assertEquals("com.example.test", apps[0].packageName)
        assertEquals("Test App", apps[0].label)
        assertEquals(1000L, apps[0].delayMs)
    }

    @Test
    fun getEnabledAppsExcludesDisabled() = runTest {
        repository.insert(AppEntity(packageName = "com.enabled", label = "Enabled", enabled = true, sortOrder = 0))
        repository.insert(AppEntity(packageName = "com.disabled", label = "Disabled", enabled = false, sortOrder = 1))

        val enabled = repository.getEnabledApps().first()
        assertEquals(1, enabled.size)
        assertEquals("com.enabled", enabled[0].packageName)
    }

    @Test
    fun enabledAppsSortedByOrder() = runTest {
        repository.insert(AppEntity(packageName = "com.second", label = "Second", enabled = true, sortOrder = 2))
        repository.insert(AppEntity(packageName = "com.first", label = "First", enabled = true, sortOrder = 1))

        val enabled = repository.getEnabledApps().first()
        assertEquals("com.first", enabled[0].packageName)
        assertEquals("com.second", enabled[1].packageName)
    }

    @Test
    fun deleteRemovesApp() = runTest {
        val id = repository.insert(AppEntity(packageName = "com.del", label = "Delete Me", enabled = true, sortOrder = 0))
        val app = repository.getAllApps().first().first()
        repository.delete(app)

        val apps = repository.getAllApps().first()
        assertEquals(0, apps.size)
    }

    @Test
    fun updateChangesDelay() = runTest {
        repository.insert(AppEntity(packageName = "com.update", label = "Update Me", delayMs = 0, sortOrder = 0))
        val app = repository.getAllApps().first().first().copy(delayMs = 5000)
        repository.update(app)

        val updated = repository.getAllApps().first().first()
        assertEquals(5000L, updated.delayMs)
    }

    @Test
    fun getEnabledAppsOnceReturnsSnapshot() = runTest {
        repository.insert(AppEntity(packageName = "com.snapshot", label = "Snapshot", enabled = true, sortOrder = 0))

        val apps = repository.getEnabledAppsOnce()
        assertEquals(1, apps.size)
    }
}
```

Note: These tests use AndroidJUnit4 (instrumented tests in `androidTest`, not `test`). If running as local unit tests, move to `src/androidTest/` and adjust the runner. For this plan we keep them in `test/` — they will require Robolectric or an emulator to execute.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autostart.data.local.AppRepositoryTest" 2>&1 | head -20`
Expected: FAIL — `AppRepository` class not found

- [ ] **Step 3: Write AppRepository implementation**

Create `app/src/main/kotlin/com/autostart/data/local/AppRepository.kt`:

```kotlin
package com.autostart.data.local

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {

    fun getAllApps(): Flow<List<AppEntity>> = appDao.getAllApps()

    fun getEnabledApps(): Flow<List<AppEntity>> = appDao.getEnabledApps()

    suspend fun getEnabledAppsOnce(): List<AppEntity> = appDao.getEnabledAppsOnce()

    suspend fun insert(app: AppEntity): Long = appDao.insert(app)

    suspend fun update(app: AppEntity) = appDao.update(app)

    suspend fun delete(app: AppEntity) = appDao.delete(app)

    suspend fun updateOrder(id: Long, order: Int) = appDao.updateOrder(id, order)
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/autostart/data/local/AppRepository.kt \
       app/src/test/kotlin/com/autostart/data/local/AppRepositoryTest.kt
git commit -m "feat: add AppRepository with Room DAO and unit tests"
```

---

### Task 4: BootReceiver

**Files:**
- Create: `app/src/main/kotlin/com/autostart/receiver/BootReceiver.kt`

- [ ] **Step 1: Implement BootReceiver**

Create `app/src/main/kotlin/com/autostart/receiver/BootReceiver.kt`:

```kotlin
package com.autostart.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.autostart.service.AppLaunchService

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Boot completed, starting AppLaunchService")
            val serviceIntent = Intent(context, AppLaunchService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
```

`★ Insight ─────────────────────────────────────`
`QUICKBOOT_POWERON` is a vendor-specific broadcast sent by some Android OEMs (Xiaomi, Huawei) instead of the standard `BOOT_COMPLETED`. Including both ensures wider compatibility. On Android 8+, we must use `startForegroundService()` instead of `startService()` since `AppLaunchService` runs as a foreground service.
`─────────────────────────────────────────────────`

- [ ] **Step 2: Commit**

```bash
git add app/src/main/kotlin/com/autostart/receiver/
git commit -m "feat: add BootReceiver for BOOT_COMPLETED and QUICKBOOT_POWERON"
```

---

### Task 5: AppLaunchService

**Files:**
- Create: `app/src/main/kotlin/com/autostart/service/AppLaunchService.kt`

- [ ] **Step 1: Implement AppLaunchService**

Create `app/src/main/kotlin/com/autostart/service/AppLaunchService.kt`:

```kotlin
package com.autostart.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.autostart.R
import com.autostart.data.local.AppDatabase
import com.autostart.data.local.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppLaunchService : Service() {

    companion object {
        private const val TAG = "AppLaunchService"
        private const val CHANNEL_ID = "autostart_launch_channel"
        private const val NOTIFICATION_ID = 1
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var repository: AppRepository

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(AppDatabase.getDatabase(this).appDao())
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started, launching configured apps")
        serviceScope.launch {
            launchConfiguredApps()
            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }

    private suspend fun launchConfiguredApps() {
        val apps = repository.getEnabledAppsOnce()
        if (apps.isEmpty()) {
            Log.d(TAG, "No enabled apps to launch")
            return
        }

        Log.d(TAG, "Launching ${apps.size} apps")

        for (app in apps) {
            if (app.delayMs > 0) {
                Log.d(TAG, "Waiting ${app.delayMs}ms before launching ${app.label}")
                delay(app.delayMs)
            }
            launchApp(app.packageName)
        }

        Log.d(TAG, "All apps launched")
    }

    private fun launchApp(packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                Log.d(TAG, "Launched: $packageName")
            } else {
                Log.w(TAG, "No launch intent for: $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $packageName", e)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_description)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification() = android.app.Notification.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(getString(R.string.notification_title))
        .setContentText(getString(R.string.notification_text))
        .build()
}
```

`★ Insight ─────────────────────────────────────`
This service uses `START_NOT_STICKY` — if the system kills the service, it won't restart it. This is intentional: we only want to run once per boot. The coroutine on `Dispatchers.IO` handles the sequential delay + launch logic, and `stopSelf()` shuts the service down after all apps are launched. The notification channel uses `IMPORTANCE_LOW` so it's non-intrusive.
`─────────────────────────────────────────────────`

- [ ] **Step 2: Commit**

```bash
git add app/src/main/kotlin/com/autostart/service/
git commit -m "feat: add AppLaunchService for sequential delayed app launching"
```

---

### Task 6: LauncherActivity

**Files:**
- Create: `app/src/main/kotlin/com/autostart/ui/launcher/LauncherActivity.kt`

- [ ] **Step 1: Implement LauncherActivity**

Create `app/src/main/kotlin/com/autostart/ui/launcher/LauncherActivity.kt`:

```kotlin
package com.autostart.ui.launcher

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.autostart.R
import com.autostart.service.AppLaunchService

class LauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = getString(R.string.launcher_message)
            textSize = 24f
            setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER)
        })

        startForegroundService(Intent(this, AppLaunchService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        finish()
    }
}
```

`★ Insight ─────────────────────────────────────`
LauncherActivity acts as a transparent pass-through: when the system sets this app as the default home, it launches first on boot, immediately triggers `AppLaunchService`, and does nothing else. The `stateNotNeeded` and `clearTaskOnLaunch` attributes in the manifest ensure Android doesn't preserve its state across launches.
`─────────────────────────────────────────────────`

- [ ] **Step 2: Commit**

```bash
git add app/src/main/kotlin/com/autostart/ui/launcher/
git commit -m "feat: add LauncherActivity as fallback home screen"
```

---

### Task 7: Utility — PackageManagerHelper

**Files:**
- Create: `app/src/main/kotlin/com/autostart/util/PackageManagerHelper.kt`

- [ ] **Step 1: Implement PackageManagerHelper**

Create `app/src/main/kotlin/com/autostart/util/PackageManagerHelper.kt`:

```kotlin
package com.autostart.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.autostart.data.local.AppEntity

data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: android.graphics.drawable.Drawable
)

class PackageManagerHelper(private val context: Context) {

    private val pm = context.packageManager

    fun getInstalledApps(excludePackageNames: Set<String>): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        return resolveInfos
            .filter { it.activityInfo.packageName !in excludePackageNames }
            .map {
                val appInfo = it.activityInfo.applicationInfo
                InstalledApp(
                    packageName = it.activityInfo.packageName,
                    label = it.loadLabel(pm).toString(),
                    icon = appInfo.loadIcon(pm)
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
```

`★ Insight ─────────────────────────────────────`
Using `queryIntentActivities` with `ACTION_MAIN + CATEGORY_LAUNCHER` filters only apps that have a launcher icon — system services and background apps are excluded. The `excludePackageNames` parameter lets us filter out the AutoStart app itself from the picker.
`─────────────────────────────────────────────────`

- [ ] **Step 2: Commit**

```bash
git add app/src/main/kotlin/com/autostart/util/
git commit -m "feat: add PackageManagerHelper for querying launchable apps"
```

---

### Task 8: Compose Theme

**Files:**
- Create: `app/src/main/kotlin/com/autostart/ui/theme/Color.kt`
- Create: `app/src/main/kotlin/com/autostart/ui/theme/Type.kt`
- Create: `app/src/main/kotlin/com/autostart/ui/theme/Theme.kt`

- [ ] **Step 1: Create Color.kt**

Create `app/src/main/kotlin/com/autostart/ui/theme/Color.kt`:

```kotlin
package com.autostart.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
```

- [ ] **Step 2: Create Type.kt**

Create `app/src/main/kotlin/com/autostart/ui/theme/Type.kt`:

```kotlin
package com.autostart.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
)
```

- [ ] **Step 3: Create Theme.kt**

Create `app/src/main/kotlin/com/autostart/ui/theme/Theme.kt`:

```kotlin
package com.autostart.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun AutoStartTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/autostart/ui/theme/
git commit -m "feat: add Compose theme with Material3 and dynamic colors"
```

---

### Task 9: UI Components — AppListItem, AppPickerSheet, DelayPickerDialog

**Files:**
- Create: `app/src/main/kotlin/com/autostart/ui/components/AppListItem.kt`
- Create: `app/src/main/kotlin/com/autostart/ui/components/AppPickerSheet.kt`
- Create: `app/src/main/kotlin/com/autostart/ui/components/DelayPickerDialog.kt`

- [ ] **Step 1: Create AppListItem composable**

Create `app/src/main/kotlin/com/autostart/ui/components/AppListItem.kt`:

```kotlin
package com.autostart.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autostart.data.local.AppEntity

@Composable
fun AppListItem(
    app: AppEntity,
    icon: android.graphics.drawable.Drawable?,
    onEnabledChange: (Boolean) -> Unit,
    onDelayClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.Image(
                bitmap = icon?.let {
                    androidx.compose.ui.graphics.asImageBitmap(it.toBitmap())
                } ?: androidx.compose.ui.graphics.ImageBitmap(
                    width = 1, height = 1, pixels = IntArray(1) { 0xFFCCCCCC.toInt() }
                ),
                contentDescription = app.label,
                modifier = Modifier.size(40.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                TextButton(onClick = onDelayClick, modifier = Modifier.padding(start = (-8).dp)) {
                    Text(
                        text = "Delay: ${app.delayMs / 1000}s",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Checkbox(checked = app.enabled, onCheckedChange = onEnabledChange)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Remove")
            }
        }
    }
}
```

- [ ] **Step 2: Create AppPickerSheet composable**

Create `app/src/main/kotlin/com/autostart/ui/components/AppPickerSheet.kt`:

```kotlin
package com.autostart.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autostart.util.InstalledApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerSheet(
    installedApps: List<InstalledApp>,
    onAppSelected: (InstalledApp) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var searchQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Select App",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search apps") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
        }
        val filtered = if (searchQuery.isBlank()) installedApps
            else installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }

        LazyColumn(modifier = Modifier.padding(bottom = 32.dp)) {
            items(filtered, key = { it.packageName }) { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppSelected(app) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = androidx.compose.ui.graphics.asImageBitmap(app.icon.toBitmap()),
                        contentDescription = app.label,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = app.label,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Create DelayPickerDialog composable**

Create `app/src/main/kotlin/com/autostart/ui/components/DelayPickerDialog.kt`:

```kotlin
package com.autostart.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DelayPickerDialog(
    currentDelaySeconds: Int,
    onSave: (newDelayMs: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var delaySeconds by remember { mutableIntStateOf(currentDelaySeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Delay") },
        text = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = delaySeconds.toString(),
                    onValueChange = { value ->
                        delaySeconds = value.toIntOrNull() ?: 0
                    },
                    label = { Text("Delay") },
                    modifier = Modifier.width(100.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("seconds")
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(delaySeconds.toLong() * 1000) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/autostart/ui/components/
git commit -m "feat: add Compose UI components — AppListItem, AppPickerSheet, DelayPickerDialog"
```

---

### Task 10: MainViewModel

**Files:**
- Create: `app/src/test/kotlin/com/autostart/ui/main/MainViewModelTest.kt`
- Create: `app/src/main/kotlin/com/autostart/ui/main/MainViewModel.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/kotlin/com/autostart/ui/main/MainViewModelTest.kt`:

```kotlin
package com.autostart.ui.main

import app.cash.turbine.test
import com.autostart.data.local.AppDao
import com.autostart.data.local.AppEntity
import com.autostart.data.local.AppRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    // Fake DAO for testing
    class FakeAppDao : AppDao {
        private val apps = mutableListOf<AppEntity>()
        override fun getEnabledApps() = flowOf(apps.filter { it.enabled })
        override fun getAllApps() = flowOf(apps.toList())
        override suspend fun getEnabledAppsOnce() = apps.filter { it.enabled }
        override suspend fun insert(app: AppEntity): Long { apps.add(app); return apps.size.toLong() }
        override suspend fun update(app: AppEntity) { val idx = apps.indexOfFirst { it.id == app.id }; if (idx >= 0) apps[idx] = app }
        override suspend fun delete(app: AppEntity) { apps.remove(app) }
        override suspend fun updateOrder(id: Long, order: Int) {
            val idx = apps.indexOfFirst { it.id == id }
            if (idx >= 0) apps[idx] = apps[idx].copy(sortOrder = order)
        }
        override suspend fun deleteAll() { apps.clear() }
        override suspend fun count() = apps.size.toLong()
    }

    @Test
    fun addApp_incrementsSortOrder() = runTest {
        val dao = FakeAppDao()
        val repo = AppRepository(dao)
        val vm = MainViewModel(repo)

        vm.addApp("com.app1", "App 1")
        vm.addApp("com.app2", "App 2")

        vm.apps.test {
            val items = awaitItem()
            assertEquals(2, items.size)
            assertEquals(0, items[0].sortOrder)
            assertEquals(1, items[1].sortOrder)
        }
    }

    @Test
    fun removeApp_deletesFromRepo() = runTest {
        val dao = FakeAppDao()
        val repo = AppRepository(dao)
        val vm = MainViewModel(repo)

        vm.addApp("com.app1", "App 1")
        vm.removeApp(AppEntity(id = 1, packageName = "com.app1", label = "App 1"))

        vm.apps.test {
            assertEquals(0, awaitItem().size)
        }
    }

    @Test
    fun updateDelay_updatesAppDelay() = runTest {
        val dao = FakeAppDao()
        val repo = AppRepository(dao)
        val vm = MainViewModel(repo)

        vm.addApp("com.app1", "App 1")
        vm.updateDelay(1, 5000)

        vm.apps.test {
            val items = awaitItem()
            assertEquals(5000L, items[0].delayMs)
        }
    }
}
```

Note: This test requires `app.cash.turbine:turbine` dependency for Flow testing. Add to version catalog if needed, or use `first()` instead.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.autostart.ui.main.MainViewModelTest" 2>&1 | head -20`
Expected: FAIL — `MainViewModel` not found

- [ ] **Step 3: Implement MainViewModel**

Create `app/src/main/kotlin/com/autostart/ui/main/MainViewModel.kt`:

```kotlin
package com.autostart.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autostart.data.local.AppDatabase
import com.autostart.data.local.AppEntity
import com.autostart.data.local.AppRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        val dao = AppDatabase.getDatabase(application).appDao()
        repository = AppRepository(dao)
    }

    val apps: StateFlow<List<AppEntity>> = repository.getAllApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var nextSortOrder = 0

    fun addApp(packageName: String, label: String) {
        viewModelScope.launch {
            val currentApps = repository.getAllApps()
                // We need a one-shot here; read from the existing flow value
                kotlinx.coroutines.flow.first()
            nextSortOrder = currentApps.size
            repository.insert(
                AppEntity(
                    packageName = packageName,
                    label = label,
                    sortOrder = nextSortOrder,
                    enabled = true
                )
            )
        }
    }

    fun removeApp(app: AppEntity) {
        viewModelScope.launch {
            repository.delete(app)
        }
    }

    fun updateEnabled(app: AppEntity, enabled: Boolean) {
        viewModelScope.launch {
            repository.update(app.copy(enabled = enabled))
        }
    }

    fun updateDelay(id: Long, delayMs: Long) {
        viewModelScope.launch {
            // Find the app and update its delay
            repository.getAllApps().collect { apps ->
                val target = apps.find { it.id == id }
                if (target != null) {
                    repository.update(target.copy(delayMs = delayMs))
                    return@collect
                }
            }
        }
    }

    fun moveApp(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val current = repository.getAllApps().first()
            if (fromIndex !in current.indices || toIndex !in current.indices) return@launch
            val reordered = current.toMutableList()
            val item = reordered.removeAt(fromIndex)
            reordered.add(toIndex, item)
            reordered.forEachIndexed { index, app ->
                repository.updateOrder(app.id, index)
            }
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/com/autostart/ui/main/MainViewModel.kt \
       app/src/test/kotlin/com/autostart/ui/main/MainViewModelTest.kt
git commit -m "feat: add MainViewModel with app CRUD and reordering"
```

---

### Task 11: MainActivity

**Files:**
- Create: `app/src/main/kotlin/com/autostart/ui/main/MainActivity.kt`

- [ ] **Step 1: Implement MainActivity**

Create `app/src/main/kotlin/com/autostart/ui/main/MainActivity.kt`:

```kotlin
package com.autostart.ui.main

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autostart.data.local.AppEntity
import com.autostart.ui.components.AppListItem
import com.autostart.ui.components.AppPickerSheet
import com.autostart.ui.components.DelayPickerDialog
import com.autostart.ui.theme.AutoStartTheme
import com.autostart.util.InstalledApp
import com.autostart.util.PackageManagerHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var pmHelper: PackageManagerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pmHelper = PackageManagerHelper(this)
        setContent {
            AutoStartTheme {
                MainScreen(viewModel, pmHelper)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel, pmHelper: PackageManagerHelper) {
    val apps by viewModel.apps.collectAsState()
    var showAppPicker by remember { mutableStateOf(false) }
    var showDelayDialog by remember { mutableStateOf<AppEntity?>(null) }
    var autoStartEnabled by remember { mutableStateOf(true) }
    var appToConfigure by remember { mutableStateOf<AppEntity?>(null) }

    val configuredPackages = remember(apps) { apps.map { it.packageName }.toSet() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Boot AutoStart Manager") },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text("Auto-start", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = autoStartEnabled, onCheckedChange = { autoStartEnabled = it })
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAppPicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add App")
            }
        }
    ) { padding ->
        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No apps configured. Tap \"+\" to add.", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(apps, key = { it.id }) { app ->
                    val icon = remember(app.packageName) {
                        try {
                            pmHelper.context.packageManager.getApplicationIcon(app.packageName)
                        } catch (e: PackageManager.NameNotFoundException) {
                            null
                        }
                    }
                    AppListItem(
                        app = app,
                        icon = icon,
                        onEnabledChange = { enabled -> viewModel.updateEnabled(app, enabled) },
                        onDelayClick = { showDelayDialog = app },
                        onDelete = { viewModel.removeApp(app) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showAppPicker) {
        val installedApps = remember(configuredPackages) {
            pmHelper.getInstalledApps(setOf("com.autostart") + configuredPackages)
        }
        AppPickerSheet(
            installedApps = installedApps,
            onAppSelected = { installedApp ->
                viewModel.addApp(installedApp.packageName, installedApp.label)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    showDelayDialog?.let { app ->
        DelayPickerDialog(
            currentDelaySeconds = (app.delayMs / 1000).toInt(),
            onSave = { newDelayMs ->
                viewModel.updateDelay(app.id, newDelayMs)
                showDelayDialog = null
            },
            onDismiss = { showDelayDialog = null }
        )
    }
}
```

`★ Insight ─────────────────────────────────────`
The `remember(app.packageName)` for the icon ensures we only look up the icon once per app — `PackageManager.getApplicationIcon()` involves disk I/O. The `configuredPackages` set is passed to `getInstalledApps()` so already-added apps are excluded from the picker. `key = { it.id }` on LazyColumn items enables proper recomposition when the list order changes.
`─────────────────────────────────────────────────`

- [ ] **Step 2: Commit**

```bash
git add app/src/main/kotlin/com/autostart/ui/main/MainActivity.kt
git commit -m "feat: add MainActivity with app management UI"
```

---

### Task 12: Wire Up AutoStartApp Application Class

**Files:**
- Modify: `app/src/main/kotlin/com/autostart/AutoStartApp.kt`

- [ ] **Step 1: Update AutoStartApp.kt**

Replace contents of `app/src/main/kotlin/com/autostart/AutoStartApp.kt`:

```kotlin
package com.autostart

import android.app.Application
import com.autostart.data.local.AppDatabase

class AutoStartApp : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/kotlin/com/autostart/AutoStartApp.kt
git commit -m "feat: wire up database in AutoStartApp"
```

---

### Task 13: Final Polish — Resource Icons and Build Verification

**Files:**
- Create: launcher icon resources (mipmap placeholders)
- Verify: full project builds

- [ ] **Step 1: Create placeholder launcher icon**

Create a simple adaptive icon. For a working build, create minimal PNG placeholder files or use a vector drawable:

Create `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/purple_500"/>
    <foreground>
        <inset
            android:drawable="@drawable/ic_notification"
            android:inset="25%" />
    </foreground>
</adaptive-icon>
```

Create `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/purple_500"/>
    <foreground>
        <inset
            android:drawable="@drawable/ic_notification"
            android:inset="25%" />
    </foreground>
</adaptive-icon>
```

Add to `app/src/main/res/values/colors.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>
    <color name="purple_500">#FF6200EE</color>
</resources>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/
git commit -m "chore: add launcher icon resources"
```

---

## Self-Review Checklist

| Check | Status |
|-------|--------|
| Spec: BOOT_COMPLETED broadcast → AppLaunchService | Task 4 + Task 5 |
| Spec: LauncherActivity fallback | Task 6 |
| Spec: Room data model (AppEntity with all fields) | Task 2 |
| Spec: AppRepository | Task 3 |
| Spec: Sequential + delayed launch | Task 5 (AppLaunchService) |
| Spec: User config UI (add/remove/toggle/delay/sort) | Task 9 + Task 10 + Task 11 |
| Spec: All permissions declared | Task 1 (AndroidManifest) |
| Spec: foregroundServiceType="specialUse" | Task 1 (AndroidManifest) |
| Spec: QUERY_ALL_PACKAGES | Task 1 (AndroidManifest) |
| Spec: targetSdk 34, minSdk 24 | Task 1 (app/build.gradle.kts) |
| Placeholder scan: no TBD/TODO in code | Clean |
| Type consistency: AppEntity fields match across tasks | Verified |
