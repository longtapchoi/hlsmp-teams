# HLSMP-Teams

Plugin team cho HLSMP Server — convert từ DonutTeams 1.4 by EliVB.

## Thay đổi so với gốc
- Đổi tên → HLSMP-Teams
- LicenseManager bypassed
- SignManager dùng PALE_OAK_WALL_SIGN tại Y-5, line(1), format: `> TÌM KIẾM <` / `>>`
- Thêm `/teamtoggle` — toggle nhận lời mời team (tích hợp HLSMP-Settings slot 29)
- Thêm `/teamchattoggle` — toggle team chat standalone (HLSMP-Settings slot 31)
- Khi gõ `/team` sẽ mở GUI + hiển thị danh sách lệnh trong chat
- lang.yml Việt hoá đầy đủ

## Lệnh
- `/team` — Mở GUI + danh sách lệnh
- `/team create <tên>` — Tạo team
- `/team invite <player>` — Mời vào team
- `/team join <team>` — Gia nhập
- `/team leave` — Rời team
- `/team kick <player>` — Kick
- `/team delete` — Giải tán
- `/team transfer <player>` — Chuyển quyền leader
- `/team sethome / home / delhome` — Quản lý home
- `/team chat <tin nhắn>` — Chat team
- `/team chat toggle` — Bật/tắt chat team
- `/teamtoggle [on|off]` — Toggle nhận lời mời
- `/teamchattoggle [on|off]` — Toggle chat team
- `/team reload` — Reload (admin)
