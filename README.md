# VPBank Controller – Hướng dẫn cài đặt

## Workflow thực tế (từ ảnh chụp)

```
[Màn hình Điện – nhập MKH]
        ↓  nhập MKH → bấm Tiếp tục
[Thông tin KH + hóa đơn]
        ↓  (nếu có popup "Kích hoạt tự động" → bấm "Không, tiếp tục thanh toán")
        ↓  bấm Tiếp tục
[Xác nhận tổng tiền]
        ↓  bấm Xác nhận
[Smart OTP PIN – nhập 6 số]
        ↓  (nếu có màn hình OTP nâng cao → bấm "Xác nhận giao dịch")
[Thành công ✓]
        ↓  bấm Giao dịch mới → lặp MKH tiếp theo
```

---

## Cấu trúc project

```
VPBankController/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/example/vpbankcontroller/
│   │   ├── AppConfig.kt                       ← text constants, timing
│   │   ├── BillPaymentAccessibilityService.kt ← state machine 9 bước
│   │   └── MainActivity.kt                    ← UI: MKH list + PIN + start/stop
│   └── res/
│       ├── layout/activity_main.xml
│       ├── values/strings.xml
│       └── xml/accessibility_service_config.xml
```

---

## Bước 1 – Mở trong Android Studio

1. Android Studio → **Open** → chọn thư mục `VPBankController/`
2. Để Gradle sync xong.
3. Kết nối điện thoại (bật USB Debugging) hoặc dùng emulator.

---

## Bước 2 – Tìm package name VPBank NEO

Mở VPBank NEO, rồi chạy:

```bash
adb shell dumpsys window | findstr mCurrentFocus
```

Kết quả: `mCurrentFocus=Window{... com.vpbank.digitalmb/...}`  
Package = `com.vpbank.digitalmb` (điền vào ô đầu trong app).

---

## Bước 3 – Build & cài

```
Ctrl+F9  → Make Project
Shift+F10 → Run 'app'
```

---

## Bước 4 – Bật Accessibility Service

**Cài đặt → Trợ năng → Dịch vụ đã tải xuống → VPBank Auto Bill Payment → BẬT**

---

## Bước 5 – Sử dụng

1. Mở **VPBank Controller**.
2. Điền **Package name** VPBank NEO.
3. Điền **PIN Smart OTP** (6 chữ số) — chỉ lưu trong RAM, không ghi file.
4. Dán **danh sách MKH** (mỗi dòng 1 mã, ví dụ `PB05090014481`).
5. Bấm **Mở VPBank** → điều hướng đến màn hình **Thanh toán hóa đơn → Điện**.
6. Bấm **Bắt đầu** → service tự động xử lý từng MKH.

---

## Điều chỉnh timing (`AppConfig.kt`)

| Hằng số                | Mặc định | Khi nào cần tăng |
|------------------------|----------|------------------|
| `DELAY_AFTER_TEXT_MS`  | 700 ms   | Điện thoại chậm load bàn phím |
| `DELAY_AFTER_CLICK_MS` | 3000 ms  | Mạng yếu / màn hình load lâu |
| `POLL_INTERVAL_MS`     | 800 ms   | Giữ nguyên |
| `MAX_RETRY`            | 15       | Tăng nếu hay bị skip nhầm |

---

## Lưu ý quan trọng

- **PIN Smart OTP** được xóa khỏi memory khi bấm Dừng hoặc đóng app.
- Service dùng **text-based search** (tìm theo nội dung nút) thay vì resource ID vì VPBank NEO obfuscate IDs.
- Nếu VPBank cập nhật và đổi text nút → sửa các hằng `TEXT_*` trong `AppConfig.kt`.
- Nếu **popup "Kích hoạt thanh toán tự động"** xuất hiện, service tự bấm "Không, tiếp tục thanh toán".
- Nếu có màn hình **OTP nâng cao**, service tự bấm "Xác nhận giao dịch" (OTP tự điền).


