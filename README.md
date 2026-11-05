# Nhắc Việc 5 Phút

App Android cá nhân của Vân: 4 mục ưu tiên, nhắc nhở tăng dần kiểu Duolingo, bấm giờ, widget màn hình chính, đếm ngày đăng ký phần mềm và cuộc thi.

## Cài đặt và cập nhật

- Tải file `NhacViec5Phut.apk` ở mục **Releases** bên phải, mở trên điện thoại và cài.
- Mỗi lần có thay đổi, GitHub tự đóng gói bản mới. App tự kiểm tra và hiện nút **Cập nhật**; dữ liệu giữ nguyên.

## Cấu trúc code

- `data/` — dữ liệu (lưu trong một file JSON trên máy)
- `notify/` — lời nhắc, câu nhắc (`Messages.kt`), lịch chạy định kỳ
- `timer/` — bấm giờ
- `widget/` — widget màn hình chính
- `update/` — tự cập nhật từ GitHub Releases
- `ui/` — các màn hình, màu sắc, nhân vật mochi

Đóng gói: `.github/workflows/build.yml`, chạy mỗi khi đẩy code lên nhánh `main`. Cần secret `KEYSTORE_B64` (khóa ký app) để phát hành; thiếu khóa thì chỉ kiểm tra build.
