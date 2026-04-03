# Smart App Control

Khong the sua code Java de vuot qua Smart App Control.

Ban phai phat hanh ban ky so hop le:

- Chung chi code signing phai la RSA.
- Chung chi nen den tu nha cung cap duoc Windows tin cay.
- Ban unsigned thuong se bi Smart App Control chan.

## Build unsigned

```powershell
powershell -ExecutionPolicy Bypass -File .\build-exe.ps1
```

## Build signed bang PFX

```powershell
$env:SIGN_PFX_PASSWORD="mat-khau-pfx"
powershell -ExecutionPolicy Bypass -File .\build-exe.ps1 `
  -Sign `
  -PfxPath "C:\path\to\codesign-rsa.pfx" `
  -Vendor "Auction Studio"
```

Neu `signtool.exe` khong nam trong `PATH`, truyen them:

```powershell
-SignToolPath "C:\Program Files (x86)\Windows Kits\10\bin\10.0.xxxxx.0\x64\signtool.exe"
```

Script se:

1. Compile app.
2. Tao `app-image`.
3. Ky toan bo `.exe` va `.dll` trong app image.
4. Dong goi installer `.exe`.
5. Ky installer cuoi.
6. Tao `SHA256.txt`.

## Ket qua

- Installer: `release\Auction Studio-1.0.0.exe`
- Checksum: `release\SHA256.txt`

Neu ban muon giam canh bao hon nua, can dung code signing certificate co reputation tot hoac dich vu Trusted Signing.
