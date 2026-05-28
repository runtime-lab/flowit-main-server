# Local Development JWT Keys

These RSA keys are checked in only to keep local development environments consistent.
They are not production keys.

이 디렉터리의 RSA 키는 로컬 개발 환경을 모든 개발자가 동일하게 맞추기 위해 저장소에 포함합니다.
운영 환경에서 사용할 키가 아니며, 배포 환경에서는 별도의 신뢰 가능한 키를 주입해야 합니다.

The current local development key pair was generated with OpenSSL:

현재 로컬 개발용 키 쌍은 OpenSSL로 생성했습니다.

```powershell
& 'C:\Program Files\Git\usr\bin\openssl.exe' genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out src\main\resources\jwt\local-dev-private.pem
& 'C:\Program Files\Git\usr\bin\openssl.exe' rsa -pubout -in src\main\resources\jwt\local-dev-private.pem -out src\main\resources\jwt\local-dev-public.pem
```

The private key is PKCS#8 PEM (`BEGIN PRIVATE KEY`), and the public key is X.509 SubjectPublicKeyInfo PEM (`BEGIN PUBLIC KEY`).

private key는 PKCS#8 PEM(`BEGIN PRIVATE KEY`) 형식이고, public key는 X.509 SubjectPublicKeyInfo PEM(`BEGIN PUBLIC KEY`) 형식입니다.
