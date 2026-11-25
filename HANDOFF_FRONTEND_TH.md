# SkyDiary — ส่งต่อให้ฝั่งหน้าบ้าน (UI)

Backend (Supabase) เชื่อมครบทุกฟีเจอร์แล้ว ข้อมูลทุกอย่างบันทึก/โหลดจาก Supabase จริง
เอกสารนี้บอกว่า **หน้าไหนยังไม่มีหน้าตา** และ **เรียกใช้อะไรได้บ้าง**

> เปิดโปรเจกต์: `C:\project403\SkyDiary9` (ต้องมี `local.properties` ที่มี `SUPABASE_URL` และ `SUPABASE_PUBLISHABLE_KEY`)
> Gradle JDK: ใช้ JDK 21 (Settings → Build Tools → Gradle → Gradle JDK)

---

## 1. งาน UI ที่ยังไม่มีหน้าตา (ให้ฝั่งหน้าบ้านทำ)

| # | งาน | สถานะตอนนี้ | ข้อมูล/ตัวแปรที่ใช้ได้ |
|---|---|---|---|
| 1 | **ป๊อปอัพ/แบนเนอร์เตือนให้ทำ DASS-21 ในหน้า Home** | ❌ ยังไม่มี | `needsScreening` ใน `MainActivity` (true = ครบ 14 วันและยังไม่ทำแบบประเมิน) → ส่งเข้า `HomeScreen` แล้วกดไป `page = "screening"` |
| 2 | **หน้าแบบประเมิน DASS-21** (`screens/ScreeningScreen.kt` → `ScreeningScreen`) | ⚠️ มีแบบพื้นฐาน (backend ทำไว้ให้ทดสอบ) | ปรับหน้าตาได้เลย ห้ามเปลี่ยน **ลำดับข้อ 1–21** (ใช้คิดคะแนน) ส่งคำตอบผ่าน `onSubmit(List<Int>)` ค่า 0–3 ครบ 21 ข้อ |
| 3 | **หน้าผลการประเมิน** (`ScreeningResultScreen` ไฟล์เดียวกัน) | ⚠️ มีแบบพื้นฐาน | `List<ScreeningResultRow>`: วันที่, คะแนน 3 ด้าน (0–21), ระดับ, คำแนะนำ |
| 4 | **ปุ่ม/ทางเข้าดูประวัติผล DASS-21** (เช่น ในหน้า Profile หรือ Summary) | ❌ ยังไม่มี | โหลดด้วย `ScreeningRepository().history()` แล้วไป `page = "screening_result"` |
| 5 | **แก้ไขชื่อ/เบอร์ในหน้า Profile** | ❌ ไม่มีปุ่ม | `UserRepository().updateProfile(name, phone)` |
| 6 | **ปุ่มลบรูปท้องฟ้า** | ❌ ไม่มีปุ่ม | `PhotoRepository().deletePhoto(date: LocalDate)` |
| 7 | **ลบตัวเลือก "อื่นๆ"** ใน `screens/BehaviorScreen.kt` (`activityOptions`) | ⚠️ ต้องลบ | ใน DB ไม่มี "อื่นๆ" (ตอนนี้ backend ข้ามให้เฉยๆ) |
| 8 | **หน้า Forgot password** | ⚠️ UI เดิมขอ อีเมล+เบอร์ และมีขั้นตั้งรหัสใหม่ | จริงๆ ใช้แค่อีเมล: กดยืนยันแล้วระบบส่งลิงก์ไปอีเมลและกลับหน้า login (ขั้นที่ 2 ไม่ถูกใช้) ควรปรับ UI ให้เหลือช่องอีเมลช่องเดียว |
| 9 | **รูปโปรไฟล์ (avatar)** ในหน้า Profile | ⚠️ เก็บในเครื่องชั่วคราว | ยังไม่มีตาราง/Storage สำหรับ avatar ถ้าต้องการบอกฝั่ง backend |

### ข้อ 1 ตามดีไซน์ที่ทีมวางไว้ (หน้า Home)
มี 2 ส่วน ทั้งสองแสดงเมื่อ `needsScreening == true` และหายเองหลังส่งแบบประเมิน (backend จัดการให้)

1. **การ์ดสีชมพูบนสุดของรายการบันทึก** — หัวข้อ "แจ้งเตือนการทำแบบประเมิน:" + ข้อความ + วันที่
   - ข้อความ/วันที่: ใช้แถวใน `dbNotifications` ที่ `type == "screening_reminder"` และ `status == "unread"` (`message`, `localDateOf(time)`)
   - กดการ์ด → `page = "screening"`
2. **ป๊อปอัพ (รูปแบบเดียวกับป๊อปอัพต้อนรับเดิม)** — "เราเห็นว่าคุณมีการบันทึกอารมณ์เศร้าติดต่อกันหลายวัน..." + ปุ่ม **ทำแบบประเมิน** + คำอธิบาย DASS-21
   - ปุ่มทำแบบประเมิน → `page = "screening"`; ปุ่ม ✕ → ปิดป๊อปอัพ (การ์ดชมพูยังอยู่)
   - แนะนำ: แสดงแทนป๊อปอัพต้อนรับเดิมเมื่อ `needsScreening` เป็น true (ใช้ `showWelcome` / `welcomeShown` เดิมเป็นตัวคุมการเปิดครั้งแรก)

สิ่งที่ต้องเพิ่มใน `HomeScreen`: พารามิเตอร์เช่น `screeningReminder: AppNotification?` และ `onScreeningClick: () -> Unit` แล้วใน `MainActivity` ส่งค่าจาก `needsScreening` / `dbNotifications` เข้าไป

**ไม่ต้องออกแบบ:** Android notification (เตือนรายวัน 20:00 และเตือน DASS-21) ใช้หน้าตามาตรฐานของ Android

---

## 2. ชื่อหน้า (`page` ใน MainActivity)

`login`, `register`, `forgot`, `home`, `calendar`, `summary`, `profile`, `notification`,
`mood` → `photo` → `behavior` (ลำดับบันทึกรายวัน), `screening`, `screening_result`

---

## 3. Repository ที่เรียกใช้ได้ (`data/remote/repository/`)

ทุกฟังก์ชันเป็น `suspend` → เรียกใน `coroutineScope.launch { ... }` และต้อง login ก่อน

| Repository | ฟังก์ชัน | ใช้ทำอะไร |
|---|---|---|
| `AuthRepository` | `register(name, phone, email, gender, password)` / `login(email, password)` / `logout()` / `sendPasswordResetEmail(email)` | บัญชีผู้ใช้ (`gender` = `"female"` / `"male"`) |
| `UserRepository` | `getCurrentUserProfile()` / `updateProfile(name, phone)` | ตาราง `USERS` |
| `DiaryRepository` | `saveMood(date, mood, note, period)` / `saveBehavior(date, bed, wake, food, water, activities)` / `loadRecords(email)` | บันทึกรายวันทั้งหมด → `List<DiaryRecord>` |
| `PhotoRepository` | `savePhoto(date, bitmap)` / `loadPhotos()` / `deletePhoto(date)` | รูปใน Storage `daily-photos` (ต้องบันทึกอารมณ์ของวันนั้นก่อน) |
| `ScreeningRepository` | `save(answers)` / `history()` | DASS-21 → `SCREENING_RESULTS` |
| `NotificationRepository` | `list()` / `markRead(ids)` / `completeScreeningReminders()` | ตาราง `NOTIFICATIONS` |
| `EmotionRiskService` | `checkAndNotify(records)` | เช็คอารมณ์ลบ 14 วันติด → สร้างแจ้งเตือน |

**ค่าที่ใช้:**
- `mood`: `happy`, `sad`, `scared`, `angry`, `confused` (อารมณ์ลบ = 4 ตัวหลัง)
- `period`: `normal`, `abnormal`, `none` (ยังไม่ถึงรอบ), `null` (ไม่ได้เลือก)
- `food`: `less`, `normal`, `more`
- กิจกรรม: ชื่อต้องตรงกับใน `BehaviorScreen` (backend แปลงให้ตรงกับตาราง `ACTIVITIES` แล้ว)

---

## 4. Logic ที่ backend จัดการให้แล้ว (UI ไม่ต้องทำ)

- จำ session: เปิดแอปใหม่เข้าหน้า Home ได้เลย; logout ล้างข้อมูลทั้งหมด
- บันทึกทุกหน้าลง Supabase เบื้องหลัง ถ้าล้มเหลวจะขึ้น Toast และเขียน Logcat tag `SkyDiary`
- อารมณ์ลบ 14 วัน → สร้างแจ้งเตือนใน DB + Android notification (ค้างจนกว่าจะส่งแบบประเมิน) + จุดแดงที่กระดิ่งค้าง
- กดการ์ด "แบบประเมิน DASS-21" ในหน้ากระดิ่ง หรือกด Android notification → เปิดหน้า `screening`
- ส่งแบบประเมินแล้ว → แจ้งเตือน DASS-21 เปลี่ยนเป็น read และ Android notification หายไป
- แจ้งเตือนบันทึกรายวัน 20:00 (ขอ permission ตอน login)

## 5. ข้อควรรู้

- ข้อความคำถาม DASS-21 ในแอปเป็นฉบับร่าง ต้องเทียบกับฉบับภาษาไทยที่ได้รับการรับรองก่อนใช้จริง
- ระดับ DASS-21 (ปกติ/เล็กน้อย/...) คำนวณโดย trigger ใน DB
- รูปจากโหมดกล้องใน `SkyPhotoScreen` อาจได้ความละเอียดต่ำ (เป็นส่วนของ UI กล้อง)
