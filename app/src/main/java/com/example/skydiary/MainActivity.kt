package com.example.skydiary

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.skydiary.components.DiaryRecord
import com.example.skydiary.components.TodayDate
import com.example.skydiary.components.calcStreak
import com.example.skydiary.components.thaiMonths
import com.example.skydiary.components.todayDate
import com.example.skydiary.data.remote.SupabaseClient
import com.example.skydiary.data.remote.SupabaseTestScreen
import com.example.skydiary.data.remote.repository.AuthRepository
import com.example.skydiary.data.remote.repository.DiaryRepository
import com.example.skydiary.data.remote.repository.EmotionRiskService
import com.example.skydiary.data.remote.repository.NotificationRepository
import com.example.skydiary.data.remote.repository.NotificationRow
import com.example.skydiary.data.remote.repository.PhotoRepository
import com.example.skydiary.data.remote.repository.ScreeningRepository
import com.example.skydiary.data.remote.repository.UserProfile
import com.example.skydiary.data.remote.repository.UserRepository
import com.example.skydiary.data.remote.repository.localDateOf
import com.example.skydiary.screens.AppNotification
import com.example.skydiary.screens.BehaviorScreen
import com.example.skydiary.screens.CalendarScreen
import com.example.skydiary.screens.DassRecord
import com.example.skydiary.screens.DassHistoryScreen
import com.example.skydiary.screens.DassResultScreen
import com.example.skydiary.screens.DassScreen
import com.example.skydiary.screens.ForgotPasswordScreen
import com.example.skydiary.screens.HomeScreen
import com.example.skydiary.screens.LoginScreen
import com.example.skydiary.screens.MoodScreen
import com.example.skydiary.screens.NotificationScreen
import com.example.skydiary.screens.ProfileScreen
import com.example.skydiary.screens.RegisterScreen
import com.example.skydiary.screens.SkyPhotoScreen
import com.example.skydiary.screens.SummaryScreen
import com.example.skydiary.ui.theme.SkyDiaryTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

// ข้อมูลผู้ใช้ที่ login อยู่ (โหลดจาก Supabase: auth + ตาราง USERS) ไม่เก็บรหัสผ่านจริง
data class Account(
    val name: String,
    val phone: String,
    val email: String,
    val gender: String,
    val password: String
)

private const val TAG = "SkyDiary"

private fun TodayDate.toLocalDate(): LocalDate = LocalDate.of(yearBE - 543, month + 1, day)
private fun DiaryRecord.localDate(): LocalDate = LocalDate.of(yearBE - 543, month + 1, day)

class MainActivity : ComponentActivity() {
    // หน้าที่ต้องเปิดเมื่อกดจาก Android notification (เช่น "screening")
    private val openPageRequest = mutableStateOf<String?>(null)

    // เปิดจากลิงก์ยืนยันอีเมล (skydiary://login-callback) -> "email_confirmed" / "email_link_error"
    // ไม่เช่นนั้นอ่านหน้าที่ขอจาก Android notification
    private fun pageRequestOf(intent: Intent?): String? {
        val data = intent?.data
        if (data != null && data.scheme == "skydiary") {
            val hasError = (data.fragment ?: "").contains("error") || data.getQueryParameter("error") != null
            return if (hasError) "email_link_error" else "email_confirmed"
        }
        return intent?.getStringExtra(ScreeningAlert.EXTRA_OPEN_PAGE)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openPageRequest.value = pageRequestOf(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openPageRequest.value = pageRequestOf(intent)
        enableEdgeToEdge()
        setContent {
            SkyDiaryTheme {
                val context = LocalContext.current
                val authRepository = remember { AuthRepository() }
                val userRepository = remember { UserRepository() }
                val diaryRepository = remember { DiaryRepository() }
                val photoRepository = remember { PhotoRepository() }
                val screeningRepository = remember { ScreeningRepository() }
                val notificationRepository = remember { NotificationRepository() }
                val riskService = remember { EmotionRiskService(notificationRepository) }
                // บันทึกทีละรายการตามลำดับ (อารมณ์ต้องถูกบันทึกก่อนรูป/พฤติกรรม เพราะต้องมี DAILY_RECORDS ก่อน)
                val saveMutex = remember { Mutex() }
                val coroutineScope = rememberCoroutineScope()

                val records = remember { mutableStateListOf<DiaryRecord>() }
                var currentUser by remember { mutableStateOf<Account?>(null) }
                var welcomeShown by remember { mutableStateOf(false) }

                var page by remember { mutableStateOf("login") }
                // หน้าที่เปิดกระดิ่งมา เพื่อให้ปุ่ม ✕ กลับถูกหน้า
                var pageBeforeBell by remember { mutableStateOf("home") }
                var loginError by remember { mutableStateOf<String?>(null) }
                var loginNotice by remember { mutableStateOf<String?>(null) }
                var registerError by remember { mutableStateOf<String?>(null) }

                // ข้อมูลจาก Supabase
                var dbNotifications by remember { mutableStateOf<List<NotificationRow>>(emptyList()) }

                // ----- DASS-21 -----
                var dassAnswers by remember { mutableStateOf<List<Int>>(emptyList()) }
                var dassPopupDismissed by remember { mutableStateOf(false) }
                // ประวัติผลประเมินจาก SCREENING_RESULTS (ล่าสุดก่อน) แสดงในหน้าสรุป
                var dassHistory by remember { mutableStateOf<List<DassRecord>>(emptyList()) }
                // ผลของเดือนที่กดเข้าไปดูจากหน้าสรุป
                var dassMonthOpened by remember { mutableStateOf<List<DassRecord>>(emptyList()) }

                val isFemale = currentUser?.gender != "male"
                val userRecords = records.filter { it.owner == currentUser?.email }
                val today = todayDate()

                fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                // ----- แจ้งเตือนรายวันบนเครื่อง (20:00) -----
                val notificationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted -> if (granted) ReminderScheduler.schedule(context) }

                fun enableDailyReminder() {
                    if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        ReminderScheduler.schedule(context)
                    }
                }

                // ----- โหลด / บันทึกข้อมูล -----
                suspend fun refreshNotifications() {
                    runCatching { notificationRepository.list() }
                        .onSuccess { dbNotifications = it }
                        .onFailure { Log.e(TAG, "load notifications", it) }
                }

                suspend fun refreshDassHistory() {
                    runCatching { screeningRepository.history() }
                        .onSuccess { rows ->
                            dassHistory = rows.map { r ->
                                val d = runCatching { LocalDate.parse(r.date) }.getOrNull()
                                DassRecord(
                                    dateText = d?.let { "${it.dayOfMonth} ${thaiMonths[it.monthValue - 1]} ${it.year + 543}" } ?: r.date,
                                    depression = r.depressionScore,
                                    anxiety = r.anxietyScore,
                                    stress = r.stressScore,
                                    yearBE = (d?.year ?: 0) + 543,
                                    month = (d?.monthValue ?: 1) - 1
                                )
                            }
                        }
                        .onFailure { Log.e(TAG, "load screening history", it) }
                }

                suspend fun loadUserData(email: String) {
                    val loaded = runCatching { diaryRepository.loadRecords(email) }
                        .onFailure { Log.e(TAG, "load records", it); toast("โหลดบันทึกไม่สำเร็จ: ${it.message}") }
                        .getOrNull() ?: return
                    if (currentUser?.email != email) return // สลับบัญชีระหว่างโหลด
                    records.clear()
                    records.addAll(loaded)
                    runCatching { riskService.checkAndNotify(loaded) }.onFailure { Log.e(TAG, "risk check", it) }
                    refreshNotifications()
                    refreshDassHistory()
                    // รูปโหลดช้ากว่า แยกโหลดทีหลังแล้วค่อยเติมเข้า record
                    val photos = runCatching { photoRepository.loadPhotos() }
                        .onFailure { Log.e(TAG, "load photos", it) }
                        .getOrNull() ?: return
                    if (currentUser?.email != email) return
                    records.indices.forEach { i ->
                        photos[records[i].localDate()]?.let { records[i] = records[i].copy(photo = it) }
                    }
                }

                fun onSignedIn(profile: UserProfile, email: String) {
                    currentUser = Account(
                        name = profile.fullName ?: "",
                        phone = profile.phoneNumber ?: "",
                        email = email,
                        gender = profile.gender,
                        password = ""
                    )
                    records.clear()
                    loginError = null
                    loginNotice = null
                    welcomeShown = false
                    dassPopupDismissed = false
                    page = "home"
                    enableDailyReminder()
                    coroutineScope.launch { loadUserData(email) }
                }

                /** บันทึกลง Supabase เบื้องหลัง (UI อัปเดตไปก่อนแล้ว) ถ้าพลาดจะแจ้ง Toast และเขียน Logcat */
                fun persist(label: String, block: suspend () -> Unit) {
                    coroutineScope.launch {
                        saveMutex.withLock {
                            runCatching { block() }.onFailure {
                                Log.e(TAG, label, it)
                                toast("$label ไม่สำเร็จ: ${it.message}")
                            }
                        }
                    }
                }

                // เปิดแอปใหม่: ถ้ายังมี session อยู่ให้เข้าหน้า home เลย
                LaunchedEffect(Unit) {
                    runCatching {
                        val auth = SupabaseClient.client.auth
                        auth.awaitInitialization()
                        val user = auth.currentUserOrNull() ?: return@runCatching
                        if (currentUser != null) return@runCatching
                        userRepository.getCurrentUserProfile()
                            .onSuccess { onSignedIn(it, user.email ?: "") }
                    }.onFailure { Log.e(TAG, "restore session", it) }
                }

                // session หลุด (เช่น refresh token ใช้ไม่ได้แล้ว) ขณะที่หน้าจอยังเป็นสถานะ login อยู่
                // -> กลับไปหน้า login แทนที่จะโชว์ข้อมูลไม่ครบ/บันทึกไม่ได้แบบเงียบๆ
                LaunchedEffect(Unit) {
                    SupabaseClient.client.auth.sessionStatus.collect { status ->
                        // isSignOut = กดออกจากระบบเอง (onLogout จัดการอยู่แล้ว) ไม่ต้องทำซ้ำ
                        if (status is SessionStatus.NotAuthenticated && !status.isSignOut && currentUser != null) {
                            Log.w(TAG, "session lost")
                            ReminderScheduler.cancel(context)
                            ScreeningAlert.cancel(context)
                            currentUser = null
                            records.clear()
                            dbNotifications = emptyList()
                            dassHistory = emptyList()
                            dassMonthOpened = emptyList()
                            loginError = null
                            loginNotice = "เซสชันหมดอายุ กรุณาเข้าสู่ระบบอีกครั้ง"
                            page = "login"
                        }
                    }
                }

                // ----- แจ้งเตือน -----
                // วันที่ (key) ที่ผู้ใช้เปิดอ่านแจ้งเตือนแล้ว ใช้ตัดสินจุดแดง
                val readNotifDays = remember { mutableStateListOf<String>() }
                val todayKey = "${currentUser?.email}-${today.yearBE}-${today.month}-${today.day}"
                val doneToday = userRecords.any {
                    it.yearBE == today.yearBE && it.month == today.month && it.day == today.day
                }
                val localNotifications: List<AppNotification> =
                    if (currentUser != null && !doneToday) {
                        listOf(
                            AppNotification(
                                "แจ้งเตือนการบันทึก:",
                                "วันนี้คุณเป็นยังไงบ้าง? อย่าลืมเข้ามาบอก/บันทึกกับเรานะ 💗",
                                "${today.day}/${today.month + 1}/${today.yearBE % 100}"
                            )
                        )
                    } else emptyList()
                val notifications: List<AppNotification> = localNotifications + dbNotifications.map { n ->
                    val date = localDateOf(n.time)
                    val isScreening = n.type == "screening_reminder"
                    AppNotification(
                        title = if (isScreening) "ข้อเสนอแนะการทำแบบประเมิน" else "แจ้งเตือนการบันทึก:",
                        message = n.message,
                        dateText = date?.let { "${it.dayOfMonth}/${it.monthValue}/${(it.year + 543) % 100}" } ?: "",
                        onClick = if (isScreening) ({ page = "dass" }) else null,
                        actionText = if (isScreening) "ทำแบบประเมิน ›" else null
                    )
                }
                val hasUnread = (localNotifications.isNotEmpty() && todayKey !in readNotifDays) ||
                        dbNotifications.any { it.status == "unread" }

                // true = ครบเงื่อนไขอารมณ์ลบ 14 วัน (EmotionRiskService) และยังไม่ได้ส่งแบบประเมิน DASS-21
                // ใช้แสดงแบนเนอร์/ป๊อปอัพในหน้า Home
                val needsScreening = dbNotifications.any { it.type == "screening_reminder" && it.status == "unread" }

                // Android notification ค้างไว้จนกว่าจะทำแบบประเมิน
                LaunchedEffect(needsScreening, currentUser) {
                    if (needsScreening && currentUser != null) ScreeningAlert.show(context)
                    else ScreeningAlert.cancel(context)
                }

                // กดจาก Android notification -> เปิดหน้าแบบประเมิน (หลัง login/restore session แล้ว)
                val openRequest = openPageRequest.value
                LaunchedEffect(openRequest, currentUser) {
                    // กดลิงก์ยืนยันอีเมลแล้วเด้งกลับมา -> หน้าเข้าสู่ระบบ (ถ้ายังไม่ได้ login อยู่)
                    if ((openRequest == "email_confirmed" || openRequest == "email_link_error") && currentUser == null) {
                        if (openRequest == "email_confirmed") {
                            loginError = null
                            loginNotice = "ยืนยันอีเมลสำเร็จ กรุณาเข้าสู่ระบบ"
                        } else {
                            loginNotice = null
                            loginError = "ลิงก์ยืนยันหมดอายุหรือถูกใช้ไปแล้ว ลองเข้าสู่ระบบ หรือสมัครใหม่อีกครั้ง"
                        }
                        page = "login"
                        openPageRequest.value = null
                    } else if (openRequest == "email_confirmed" || openRequest == "email_link_error") {
                        openPageRequest.value = null
                    }
                    if (openRequest == "screening" && currentUser != null) {
                        page = "dass"
                        openPageRequest.value = null
                    }
                }

                fun openBell(from: String) {
                    pageBeforeBell = from
                    if (todayKey !in readNotifDays) readNotifDays.add(todayKey)
                    // แจ้งเตือน DASS-21 ไม่ถูก mark read ตอนเปิดกระดิ่ง ต้องค้างไว้จนกว่าจะส่งแบบประเมิน
                    val unread = dbNotifications
                        .filter { it.status == "unread" && it.type != "screening_reminder" }.map { it.id }
                    if (unread.isNotEmpty()) {
                        dbNotifications = dbNotifications.map { if (it.id in unread) it.copy(status = "read") else it }
                        coroutineScope.launch {
                            runCatching { notificationRepository.markRead(unread) }
                                .onFailure { Log.e(TAG, "mark read", it) }
                        }
                    }
                    page = "notification"
                }

                fun todayIndex(): Int = records.indexOfFirst {
                    it.owner == currentUser?.email &&
                            it.yearBE == today.yearBE && it.month == today.month && it.day == today.day
                }

                fun updateToday(create: () -> DiaryRecord, update: (DiaryRecord) -> DiaryRecord) {
                    val idx = todayIndex()
                    if (idx >= 0) records[idx] = update(records[idx]) else records.add(create())
                }

                when (page) {
                    "supabase_test" -> SupabaseTestScreen()

                    "login" -> LoginScreen(
                        errorMessage = loginError,
                        noticeMessage = loginNotice,
                        onLoginClick = { user, pass ->
                            loginError = null
                            loginNotice = null

                            coroutineScope.launch {
                                val result = authRepository.login(
                                    email = user,
                                    password = pass
                                )

                                result.onSuccess {
                                    // Login สำเร็จแล้ว ดึงข้อมูลโปรไฟล์จาก USERS
                                    val profileResult = userRepository.getCurrentUserProfile()

                                    profileResult.onSuccess { profile ->
                                        onSignedIn(profile, user.trim())
                                    }

                                    profileResult.onFailure {
                                        Log.e(TAG, "load profile", it)
                                        loginError = "เข้าสู่ระบบสำเร็จ แต่ไม่สามารถโหลดข้อมูลโปรไฟล์ได้"
                                    }
                                }

                                result.onFailure { error ->
                                    Log.e(TAG, "login", error)
                                    loginError = when {
                                        error.message?.contains(
                                            "Invalid login credentials",
                                            ignoreCase = true
                                        ) == true ->
                                            "อีเมลหรือรหัสผ่านไม่ถูกต้อง"

                                        error.message?.contains(
                                            "Email not confirmed",
                                            ignoreCase = true
                                        ) == true ->
                                            "กรุณายืนยันอีเมลก่อนเข้าสู่ระบบ"

                                        else ->
                                            "เข้าสู่ระบบไม่สำเร็จ กรุณาตรวจสอบการเชื่อมต่อแล้วลองใหม่"
                                    }
                                }
                            }
                        },
                        onRegisterClick = {
                            loginError = null
                            loginNotice = null
                            registerError = null
                            page = "register"
                        },
                        onForgotClick = {
                            loginError = null
                            loginNotice = null
                            page = "forgot"
                        }
                    )
                    "register" -> RegisterScreen(
                        errorMessage = registerError,

                        onRegisterClick = { name, phone, email, gender, password ->
                            registerError = null

                            coroutineScope.launch {
                                val result = authRepository.register(
                                    name = name,
                                    phone = phone,
                                    email = email,
                                    gender = gender,
                                    password = password
                                )

                                result.onSuccess {
                                    registerError = null
                                    loginError = null
                                    loginNotice = "สมัครสมาชิกสำเร็จ กรุณาตรวจสอบอีเมลเพื่อยืนยันบัญชีก่อนเข้าสู่ระบบ"
                                    page = "login"
                                }

                                result.onFailure { error ->
                                    Log.e(TAG, "register", error)
                                    registerError = when {
                                        error.message?.contains(
                                            "already registered",
                                            ignoreCase = true
                                        ) == true -> "อีเมลนี้ถูกใช้สมัครสมาชิกแล้ว"

                                        // Supabase จำกัดจำนวนเมลยืนยันต่อชั่วโมง
                                        error.message?.contains(
                                            "rate limit",
                                            ignoreCase = true
                                        ) == true -> "ระบบส่งอีเมลยืนยันเกินจำนวนที่กำหนด กรุณารอสักครู่แล้วลองใหม่"

                                        else -> "สมัครสมาชิกไม่สำเร็จ กรุณาตรวจสอบข้อมูลและลองใหม่อีกครั้ง"
                                    }
                                }
                            }
                        },

                        onLoginClick = { registerError = null; page = "login" }
                    )
                    "forgot" -> ForgotPasswordScreen(
                        // UI เดิมเป็น 2 ขั้น (ยืนยันตัวตน -> ตั้งรหัสใหม่) แต่การเปลี่ยนรหัสต้องทำผ่านลิงก์ในอีเมลของ Supabase
                        // จึงส่งลิงก์ reset ไปที่อีเมล แล้วกลับหน้า login ทันที (ไม่ถึงขั้นที่ 2)
                        onVerify = { email, _ ->
                            loginError = null
                            loginNotice = "กำลังส่งลิงก์ตั้งรหัสผ่านใหม่ไปที่อีเมล..."
                            page = "login"
                            coroutineScope.launch {
                                authRepository.sendPasswordResetEmail(email)
                                    .onSuccess {
                                        loginNotice = "ส่งลิงก์ตั้งรหัสผ่านใหม่ไปที่อีเมลแล้ว กรุณาตรวจสอบอีเมล"
                                    }
                                    .onFailure {
                                        Log.e(TAG, "reset password", it)
                                        loginNotice = null
                                        loginError = "ส่งอีเมลตั้งรหัสผ่านไม่สำเร็จ กรุณาลองใหม่"
                                    }
                            }
                            true
                        },
                        onResetPassword = { page = "login" },
                        onBack = { page = "login" }
                    )
                    "notification" -> NotificationScreen(
                        notifications = notifications,
                        onClose = { page = pageBeforeBell }
                    )
                    "dass" -> DassScreen(
                        onClose = { page = "home" },
                        onSubmit = { answers ->
                            // แสดงผลทันที แล้วบันทึกลง SCREENING_RESULTS เบื้องหลัง
                            dassAnswers = answers
                            page = "dassResult"
                            persist("บันทึกแบบประเมิน") {
                                screeningRepository.save(answers)
                                runCatching { notificationRepository.completeScreeningReminders() }
                                    .onFailure { Log.e(TAG, "complete screening reminder", it) }
                                refreshNotifications()
                                refreshDassHistory()
                            }
                        }
                    )
                    "dassResult" -> DassResultScreen(
                        answers = dassAnswers,
                        onClose = { page = "home" }
                    )
                    // เปิดดูผลย้อนหลังจากหน้าสรุป
                    "dassHistory" -> {
                        if (dassMonthOpened.isNotEmpty()) {
                            DassHistoryScreen(
                                records = dassMonthOpened,
                                onClose = { page = "summary" }
                            )
                        } else {
                            LaunchedEffect(Unit) { page = "summary" }
                        }
                    }
                    "mood" -> MoodScreen(
                        isFemale = isFemale,
                        dateText = today.fullText,
                        streak = calcStreak(userRecords),
                        onClose = { page = "home" },
                        onSave = { mood, note, period ->
                            val owner = currentUser?.email ?: ""
                            updateToday(
                                create = {
                                    DiaryRecord(
                                        owner = owner, yearBE = today.yearBE, month = today.month,
                                        day = today.day, mood = mood, note = note, period = period
                                    )
                                },
                                update = { it.copy(mood = mood, note = note, period = period) }
                            )
                            val date = today.toLocalDate()
                            val snapshot = records.filter { it.owner == owner }
                            persist("บันทึกอารมณ์") {
                                diaryRepository.saveMood(date, mood, note, period)
                                // เช็คอารมณ์ลบ 14 วันติด -> สร้างแจ้งเตือนให้ทำ DASS-21
                                riskService.checkAndNotify(snapshot)
                                refreshNotifications()
                            }
                            page = "photo"
                        }
                    )
                    "photo" -> SkyPhotoScreen(
                        dateText = today.slashText,
                        onSkip = { page = "behavior" },
                        onSend = { bmp ->
                            val idx = todayIndex()
                            if (idx >= 0) {
                                records[idx] = records[idx].copy(photo = bmp)
                                val date = today.toLocalDate()
                                persist("อัปโหลดรูป") { photoRepository.savePhoto(date, bmp) }
                            }
                            page = "behavior"
                        }
                    )
                    "behavior" -> BehaviorScreen(
                        dateText = today.slashText,
                        onSave = { bed, wake, minutes, food, water, activities ->
                            val idx = todayIndex()
                            if (idx >= 0) {
                                records[idx] = records[idx].copy(
                                    bedTime = bed, wakeTime = wake, sleepMinutes = minutes,
                                    food = food, waterMl = water, activities = activities
                                )
                                val date = today.toLocalDate()
                                persist("บันทึกพฤติกรรม") {
                                    diaryRepository.saveBehavior(date, bed, wake, food, water, activities)
                                }
                            }
                            page = "home"
                        },
                        onSkip = { page = "home" }
                    )
                    "calendar" -> CalendarScreen(
                        records = userRecords,
                        isFemale = isFemale,
                        onNavigate = { page = it },
                        onAddClick = { page = "mood" },
                        onBellClick = { openBell("calendar") },
                        hasUnread = hasUnread
                    )
                    "summary" -> SummaryScreen(
                        records = userRecords,
                        isFemale = isFemale,
                        onNavigate = { page = it },
                        onAddClick = { page = "mood" },
                        onBellClick = { openBell("summary") },
                        hasUnread = hasUnread,
                        dassResults = dassHistory,
                        onDassResultClick = { dassMonthOpened = it; page = "dassHistory" }
                    )
                    "profile" -> ProfileScreen(
                        name = currentUser?.name ?: "",
                        email = currentUser?.email ?: "",
                        phone = currentUser?.phone ?: "",
                        password = currentUser?.password ?: "",
                        onNavigate = { page = it },
                        onAddClick = { page = "mood" },
                        onBellClick = { openBell("profile") },
                        hasUnread = hasUnread,
                        onLogout = {
                            coroutineScope.launch {
                                val result = authRepository.logout()

                                result.onSuccess {
                                    ReminderScheduler.cancel(context)
                                    ScreeningAlert.cancel(context)
                                    currentUser = null
                                    records.clear()
                                    readNotifDays.clear()
                                    dbNotifications = emptyList()
                                    dassAnswers = emptyList()
                                    dassHistory = emptyList()
                                    dassMonthOpened = emptyList()
                                    loginError = null
                                    loginNotice = null
                                    welcomeShown = false
                                    page = "login"
                                }

                                result.onFailure {
                                    Log.e(TAG, "logout", it)
                                    toast("ออกจากระบบไม่สำเร็จ กรุณาลองใหม่")
                                }
                            }
                        }

                    )
                    else -> HomeScreen(
                        records = userRecords,
                        showWelcome = !welcomeShown,
                        onCloseWelcome = { welcomeShown = true },
                        showDassBanner = needsScreening,
                        showDassPopup = needsScreening && !dassPopupDismissed,
                        onDassStart = { dassPopupDismissed = true; page = "dass" },
                        onDassDismiss = { dassPopupDismissed = true },
                        onNavigate = { page = it },
                        onAddClick = { page = "mood" },
                        onBellClick = { openBell("home") },
                        hasUnread = hasUnread
                    )
                }
            }
        }
    }
}
