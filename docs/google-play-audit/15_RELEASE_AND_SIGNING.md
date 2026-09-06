# Release and Signing

Release artifacts rebuilt after remediation:

- `app/build/outputs/apk/release/app-release.apk`
- `app/build/outputs/bundle/release/app-release.aab`

Current release artifact hashes:

- APK SHA-256: `ff88a30d9f41c9f9a2191c8208f403ad76d38bc21fd99475a680dec764d903f6`
- AAB SHA-256: `6e15d1e82d3e8f48c972acca199ef427b8eb93c2b76bc406755f0de8e1cdcf2a`

Badging after remediation:

- Package: `io.github.alexandrdobryn.waschedule`
- Version code: `7`
- Version name: `0.1.6`
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
- Public Privacy Policy URL: HTTP 200 verified for `/privacy/` on 2026-09-02; verify again after push/deploy if Pages content changes.
