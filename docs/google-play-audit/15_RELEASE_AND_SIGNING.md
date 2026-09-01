# Release and Signing

Release artifacts rebuilt after remediation:

- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`

Current release artifact hashes:

- APK SHA-256: `4B0A8B95EE2F7AAD9DC0B963F42B6EF5181DE1894753A7E28FBF473B6F7652C5`
- AAB SHA-256: `06C2ECB3305082076EC82C004773EC0F5DEF43CDA5A7F1F2443186E80D70C7C4`

Badging after remediation:

- Package: `io.github.alexandrdobryn.waschedule`
- Version code: `6`
- Version name: `0.1.5`
- Min SDK: `31`
- Target SDK: `36`
- Compile SDK: `36`
- Label: `WA Schedule`

APK signing verification:

- APK Signature Scheme v2: true
- Number of signers: 1
- Signer DN: `CN=WhatsAppScheduler, OU=Private Release, O=Personal, L=Kyiv, ST=Kyiv, C=UA`
- Certificate SHA-256: `12212b2e1975f119547537892a775b1fb983c61717bc2ef4341da35b26c1e80f`

AAB signing verification:

- `jarsigner -verify -certs app/build/outputs/bundle/release/app-release.aab`
- Result: `jar verified`
- Warnings: self-signed certificate, no timestamp, and normal Android App Bundle ZIP/JAR consistency warnings.

Play readiness:

- AAB exists: PASS
- AAB signed by Gradle: PASS
- APK signature verified: PASS
- Target SDK requirement: PASS
- Runtime validation: NOT TESTED
- Public Privacy Policy URL: PREPARED, pending GitHub Pages deployment verification
