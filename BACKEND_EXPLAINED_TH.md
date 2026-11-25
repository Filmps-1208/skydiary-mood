# SkyDiary — อธิบายฝั่ง Backend และการเชื่อมต่อ

เอกสารนี้อธิบายว่า backend ของ SkyDiary ทำงานยังไง เชื่อมกับแอป Android ยังไง ทำอะไรไปแล้วบ้าง และเหลืออะไร
(งานหน้าตา UI ที่ต้องส่งต่อเพื่อน ดูที่ `HANDOFF_FRONTEND_TH.md`)

---

## 1. ภาพรวม: ระบบประกอบด้วยอะไร

SkyDiary **ไม่มีเซิร์ฟเวอร์ที่เขียนเอง** แอป Android คุยกับ **Supabase** โดยตรง ซึ่ง Supabase ให้บริการ 3 อย่างที่เราใช้:

| บริการของ Supabase | ใช้ทำอะไรใน SkyDiary |
|---|---|
| **Auth** | สมัครสมาชิก, ยืนยันอีเมล, login/logout, จำ session, ลืมรหัสผ่าน |
| **Database (PostgreSQL)** | เก็บข้อมูลทั้งหมด 10 ตาราง + มี **RLS** กันไม่ให้ผู้ใช้เห็นข้อมูลของคนอื่น + มี **trigger** คำนวณระดับ DASS-21 |
| **Storage** | เก็บรูปท้องฟ้าใน bucket `daily-photos` (แบบ private) |

ฝั่งแอปใช้ library **supabase-kt** (Kotlin) เวอร์ชัน 3.0.3 ในการเรียก Supabase

### ลำดับการทำงาน (จากหน้าจอไปถึงฐานข้อมูล)

```
[หน้าจอ UI]  ──กดปุ่ม──▶  [MainActivity]  ──เรียก──▶  [Repository]  ──▶  [SupabaseClient]  ──อินเทอร์เน็ต──▶  [Supabase]
 screens/*.kt              เก็บ state,               data/remote/        data/remote/                         Auth / DB / Storage
 (ของเพื่อน)               สลับหน้า,                 repository/*.kt     SupabaseClient.kt
                           สั่งบันทึก/โหลด           (ของเรา)
```

- **หน้าจอ (screens/)** — แค่แสดงผลและส่ง event ออกมา เช่น `onSave(mood, note, period)` ไม่รู้จัก Supabase เลย
- **MainActivity** — เป็น "ตัวกลาง" เก็บข้อมูลที่โหลดมา, เลือกว่าจะแสดงหน้าไหน (`page`), และเรียก repository เมื่อผู้ใช้กดบันทึก
- **Repository** — 1 ไฟล์ต่อ 1 กลุ่มข้อมูล รวมคำสั่งอ่าน/เขียน Supabase ไว้ที่เดียว
- **SupabaseClient** — สร้างการเชื่อมต่อครั้งเดียว ใช้ร่วมกันทั้งแอป

แยกแบบนี้เพื่อให้ **เพื่อนแก้หน้าตาได้โดยไม่ต้องยุ่งกับ Supabase** และ **เราแก้ backend ได้โดยไม่กระทบหน้าตา**

---

## 2. การตั้งค่าการเชื่อมต่อ

1. ไฟล์ `local.properties` (อยู่ที่ root โปรเจกต์ ไม่ขึ้น Git) เก็บที่อยู่และ key ของ Supabase:
   ```
   SUPABASE_URL=https://dvxrodmjjctboykjyojd.supabase.co
   SUPABASE_PUBLISHABLE_KEY=sb_publishable_...
   ```
   - URL ต้องเป็นที่อยู่หลักเท่านั้น **ห้ามต่อท้าย `/rest/v1/`**
   - ใช้ **publishable key** เท่านั้น (ปลอดภัยที่จะอยู่ในแอป เพราะสิทธิ์ถูกจำกัดด้วย RLS) ห้ามใช้ `service_role` key
2. `app/build.gradle.kts` อ่านค่าจาก `local.properties` ไปสร้างเป็น `BuildConfig.SUPABASE_URL` / `BuildConfig.SUPABASE_PUBLISHABLE_KEY` ตอน build
3. `SupabaseClient.kt` ใช้ค่านี้สร้าง client และเปิดใช้ Auth, Postgrest (database), Storage
4. ต้องเปิด **Kotlin Serialization plugin** (เพิ่มแล้วใน `build.gradle.kts` ทั้ง 2 ไฟล์ และ `gradle/libs.versions.toml`) เพื่อแปลงข้อมูล JSON จาก Supabase เป็น class ใน Kotlin
5. Gradle ต้องใช้ **JDK 21** (JDK 25 ที่มากับ Android Studio รุ่นใหม่ใช้กับ Gradle 8.13 ไม่ได้)

---

## 3. ฐานข้อมูล (10 ตาราง)

```
auth.users (Supabase Auth)
    │  trigger on_auth_user_created → สร้างแถวใน USERS อัตโนมัติตอนสมัคร
    ▼
USERS (user_id, full_name, phone_number, gender)
    │
    ├── DAILY_RECORDS (record_id, user_id, record_date, note)   ← 1 คน 1 แถวต่อวัน
    │       ├── EMOTION_RECORDS   (record_id, emotion_type)          อารมณ์
    │       ├── MENSTRUAL_RECORDS (record_id, period_status)         ประจำเดือน
    │       ├── BEHAVIOR_RECORDS  (record_id, sleep_time, wake_time, food_status, water_intake_ml)
    │       ├── PHOTO_RECORDS     (record_id, storage_path, uploaded_at)  → ไฟล์จริงอยู่ใน Storage
    │       └── DAILY_ACTIVITIES  (record_id, activity_id) ──▶ ACTIVITIES (activity_id, activity_name)
    │
    ├── SCREENING_RESULTS (user_id, screening_date, คะแนน 3 ด้าน, ระดับ 3 ด้าน, recommendation)
    │       trigger calculate_dass21_levels → คำนวณระดับให้อัตโนมัติ
    │
    └── NOTIFICATIONS (user_id, notification_time, message, notification_type, status)
```

**แนวคิดหลัก:** `DAILY_RECORDS` เป็น "แม่" ของบันทึกแต่ละวัน ตารางอื่นของวันนั้นอ้างอิงผ่าน `record_id`
ดังนั้นต้องมีแถวใน `DAILY_RECORDS` ก่อน (สร้างตอนบันทึกอารมณ์) ถึงจะบันทึกรูป/พฤติกรรมของวันนั้นได้

**ค่าที่เก็บ:**
- อารมณ์: `happy`, `sad`, `scared`, `angry`, `confused`
- ประจำเดือน: `normal`, `abnormal`, `not_due` (ในแอปเรียก `none` → backend แปลงให้)
- อาหาร: `less`, `normal`, `more`
- แจ้งเตือน: type `daily_reminder` / `screening_reminder`, status `unread` / `read`

**ความปลอดภัย (RLS):** ทุกตารางเปิด Row Level Security ผู้ใช้อ่าน/เขียนได้เฉพาะแถวของตัวเอง (`auth.uid()`) รูปใน Storage ก็แยกโฟลเดอร์ตาม user id (`daily-photos/{user_id}/{วันที่}.jpg`)
ฝั่งแอปก็กรองตาม user อีกชั้นด้วย

---

## 4. แต่ละฟีเจอร์ทำงานยังไง

| ฟีเจอร์ | เกิดอะไรขึ้นเมื่อผู้ใช้ใช้งาน | Repository |
|---|---|---|
| **สมัครสมาชิก** | ส่งอีเมล/รหัสผ่าน + ชื่อ เบอร์ เพศ ไปที่ Supabase Auth → trigger สร้างแถวใน `USERS` → Supabase ส่งอีเมลยืนยัน | `AuthRepository.register` |
| **Login** | ตรวจรหัสผ่านกับ Auth → โหลดโปรไฟล์จาก `USERS` → โหลดบันทึกทั้งหมด → โหลดรูป → เช็คอารมณ์ลบ 14 วัน → โหลดแจ้งเตือน → ตั้งเตือนรายวัน 20:00 | `AuthRepository.login`, `UserRepository`, `DiaryRepository.loadRecords`, `PhotoRepository.loadPhotos` |
| **จำ session** | ปิดแอปแล้วเปิดใหม่ ถ้ายังมี session อยู่จะเข้าหน้า Home เลยโดยไม่ต้อง login | (ใน `MainActivity`) |
| **Logout** | ออกจาก Auth + ล้างข้อมูลในแอป + ยกเลิกการเตือน | `AuthRepository.logout` |
| **บันทึกอารมณ์** | สร้าง/หาแถว `DAILY_RECORDS` ของวันนี้ → บันทึกโน้ต, `EMOTION_RECORDS`, `MENSTRUAL_RECORDS` → เช็คอารมณ์ลบ 14 วัน | `DiaryRepository.saveMood` |
| **อัปโหลดรูป** | ย่อรูป (ด้านยาวสุด 1600px) → อัปโหลดลง Storage → บันทึก path ใน `PHOTO_RECORDS` | `PhotoRepository.savePhoto` |
| **บันทึกพฤติกรรม** | บันทึก `BEHAVIOR_RECORDS` → ลบกิจกรรมเดิมของวันนั้น → ใส่ `DAILY_ACTIVITIES` ใหม่ (แปลงชื่อกิจกรรมเป็น id จาก `ACTIVITIES`) | `DiaryRepository.saveBehavior` |
| **Home / Calendar / Summary** | ใช้ข้อมูลที่โหลดมาตอน login (ไม่ต้องโหลดใหม่ทุกหน้า) | — |
| **อารมณ์ลบ 14 วัน** | ถ้า 14 วันล่าสุด (รวมวันนี้) เป็นอารมณ์ลบทุกวันติดกัน (ขาดวันไหนหรืออารมณ์ไม่ลบ = เริ่มนับใหม่, นับเฉพาะหลังทำแบบประเมินครั้งล่าสุด) → สร้างแถว `screening_reminder` ใน `NOTIFICATIONS` (ไม่สร้างซ้ำถ้ายังมีอันที่ยังไม่ได้ทำ) | `EmotionRiskService.checkAndNotify` |
| **แจ้งเตือน (กระดิ่ง)** | รวมแจ้งเตือน "วันนี้ยังไม่บันทึก" + แจ้งเตือนจาก DB เปิดกระดิ่ง = mark read **ยกเว้น** DASS-21 ที่ค้างไว้จนกว่าจะทำ | `NotificationRepository` |
| **Android notification DASS-21** | ตอนที่มีแจ้งเตือน DASS-21 ค้าง → เด้งแจ้งเตือนในแถบด้านบนของมือถือแบบค้าง กดแล้วเปิดหน้าแบบประเมิน | `ScreeningAlert.kt` |
| **แบบประเมิน DASS-21** | ตอบ 21 ข้อ (0–3) → แอปรวมคะแนนดิบแต่ละด้าน (0–21) → บันทึกลง `SCREENING_RESULTS` → trigger ใน DB คำนวณระดับ (เกณฑ์คะแนนดิบ = เกณฑ์มาตรฐาน DASS หาร 2, แอปใช้เกณฑ์เดียวกัน) → แจ้งเตือน DASS-21 เปลี่ยนเป็น read | `ScreeningRepository.save`, `history` |
| **เตือนรายวัน 20:00** | ตั้งเวลาในเครื่องด้วย AlarmManager (ไม่ใช่ push จากเซิร์ฟเวอร์) เปิดเครื่องใหม่ก็ตั้งให้อีกรอบ | `DailyReminderReceiver.kt` |
| **ลืมรหัสผ่าน** | ส่งลิงก์ตั้งรหัสใหม่ไปที่อีเมลผ่าน Supabase | `AuthRepository.sendPasswordResetEmail` |

**การบันทึกทำเบื้องหลัง:** กดบันทึกแล้วหน้าจออัปเดตทันที แล้วค่อยส่งขึ้น Supabase ตามลำดับ (อารมณ์ → รูป → พฤติกรรม) ถ้าส่งไม่สำเร็จจะขึ้น Toast และเขียน Logcat tag `SkyDiary`

**การคิดคะแนน DASS-21:** ข้อที่รวมเป็นแต่ละด้าน (ตามมาตรฐาน)
- ซึมเศร้า: ข้อ 3, 5, 10, 13, 16, 17, 21
- วิตกกังวล: ข้อ 2, 4, 7, 9, 15, 19, 20
- เครียด: ข้อ 1, 6, 8, 11, 12, 14, 18

---

## 5. ไฟล์ที่แก้/เพิ่ม (เทียบกับ SkyDiary8 ที่ได้รับมา)

| ไฟล์ | ทำอะไร |
|---|---|
| `MainActivity.kt` | เปลี่ยนจากเก็บข้อมูลในหน่วยความจำ → เชื่อม repository ทุกหน้า, จำ session, แจ้งเตือน, DASS-21 |
| `data/remote/repository/DiaryRepository.kt` | กรองข้อมูลเฉพาะของผู้ใช้, แปลงชื่อกิจกรรม, ข้าม "อื่นๆ" |
| `data/remote/repository/PhotoRepository.kt` | ย่อรูป, ใส่ `uploaded_at`, เพิ่ม `deletePhoto` |
| `data/remote/repository/NotificationRepository.kt` | แก้ status เป็น null, เวลาแบบ timezone ไทย, ค้างแจ้งเตือน DASS-21 จนกว่าจะทำ |
| `ScreeningAlert.kt` (ใหม่) | Android notification DASS-21 |
| `screens/ScreeningScreen.kt` (ใหม่) | หน้าแบบประเมิน + หน้าผล (แบบพื้นฐาน) |
| `screens/NotificationScreen.kt` | ให้การ์ดแจ้งเตือนกดได้ (หน้าตาเหมือนเดิม) |
| `build.gradle.kts`, `app/build.gradle.kts`, `gradle/libs.versions.toml` | เปิด Kotlin Serialization plugin |
| `local.properties` | ใส่ Supabase URL + key |

หน้าจออื่นทั้งหมด **ไม่ได้แก้**

---

## 6. บั๊กที่เจอระหว่างทดสอบบนมือถือจริง และแก้แล้ว

| อาการ | สาเหตุ | แก้ยังไง |
|---|---|---|
| สมัครสมาชิกไม่ได้ | `local.properties` ไม่มี Supabase URL/key แอปเลยยิงไป `localhost` | ใส่ค่าที่ถูกต้อง |
| Login ได้แต่ "โหลดโปรไฟล์ไม่ได้" | ไม่ได้เปิด Kotlin Serialization plugin | เพิ่ม plugin |
| รูปขึ้น Storage แต่ไม่ลงตาราง | `PHOTO_RECORDS.uploaded_at` ห้ามว่าง | ส่งเวลาอัปโหลดไปด้วย |
| กิจกรรมไม่ถูกบันทึก | แอปสะกด "ซีรี่ย์" แต่ DB สะกด "ซีรีย์"; "อื่นๆ" ไม่มีใน DB | แปลงชื่อให้ตรง; ข้าม "อื่นๆ" |
| แจ้งเตือน 14 วันไม่ถูกสร้าง | ค่า `status` ไม่ถูกส่งไป (ค่า default ถูกข้ามตอนแปลงเป็น JSON) | ส่งค่า `unread` ตรงๆ |
| แบบประเมินบันทึกไม่ได้ถ้าคะแนนเป็นเลขคี่ | DB มี constraint ซ้อน 2 ชุด ชุดหนึ่งบังคับเลขคู่ | **ยังรอแก้ใน DB** (ดูข้อ 7) |

---

## 7. สถานะตอนนี้

### ผ่านการทดสอบบนมือถือจริงแล้ว ✅
สมัคร → ยืนยันอีเมล → Login → จำ session, บันทึกอารมณ์/โน้ต/ประจำเดือน/การนอน/อาหาร/น้ำ/กิจกรรม/รูป ลง DB และโหลดกลับได้, สร้างแจ้งเตือน 14 วัน, Android notification DASS-21 ขึ้นแบบค้าง, trigger คำนวณระดับ DASS-21 ถูกต้อง

### ยังค้าง ⏳
1. **Constraint คะแนน DASS-21 ใน DB** — ต้องลบ constraint ที่บังคับเลขคู่ (`screening_*_score_check`) ใน SQL Editor ให้เหลือแค่ `chk_*_score` (0–21)
2. **ทดสอบที่ยังไม่ได้ทำ** (รอ UI จากเพื่อนก่อน): ทำแบบประเมินจนจบ, Calendar/Summary, logout/สลับบัญชี, ลืมรหัสผ่าน, เตือน 20:00, เช็คบั๊ก "Unknown Error" ตอนสมัคร
3. **ลิงก์ในอีเมล** (ยืนยันบัญชี/ตั้งรหัสใหม่) เปิดไป `localhost` — ต้องตั้ง Site URL ใน Supabase (Authentication → URL Configuration) หรือทำ deep link เข้าแอป
4. ข้อความคำถาม DASS-21 เป็นฉบับร่าง ต้องเทียบกับฉบับไทยที่ได้รับการรับรอง
5. `recommendation` ในผลประเมินยังว่าง (trigger ไม่ได้ใส่)

---

## 8. สรุปสั้นๆ สำหรับอธิบายคนอื่น

> แอป SkyDiary ใช้ Supabase เป็น backend ทั้งหมด — Auth สำหรับบัญชีผู้ใช้, PostgreSQL 10 ตารางสำหรับบันทึกรายวัน แบบประเมิน และแจ้งเตือน, และ Storage สำหรับรูป
> ฝั่งแอปแบ่งเป็น 3 ชั้น: หน้าจอ (UI) → MainActivity (ตัวกลางจัดการ state) → Repository (คุยกับ Supabase) ทำให้แก้หน้าตากับแก้ backend แยกกันได้
> ข้อมูลของแต่ละคนถูกแยกด้วย Row Level Security ใน database
> ระบบตรวจจับอารมณ์ลบติดกัน 14 วัน แล้วแจ้งเตือนให้ทำแบบประเมิน DASS-21 ซึ่งคะแนนจะถูกคำนวณระดับอัตโนมัติด้วย trigger ใน database
