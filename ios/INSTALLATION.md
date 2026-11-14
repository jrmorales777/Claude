# World Voice Translator: installing on iPhone

## Current status
The native iPhone app compiles and has launched in an iPhone simulator. The simulator build is not installable on a real iPhone.

## Official TestFlight release
This project includes a signed release GitHub Actions workflow. It requires an Apple Developer Program account and signed distribution credentials.
Create an App Store Connect record for bundle ID `com.jrmorales.worldvoicetranslator`, an Apple Distribution certificate, an App Store provisioning profile and an App Store Connect API team key.
Store the following securely as GitHub Actions Secrets, never in public source code or the chat:

- APPLE_TEAM_ID
- APPLE_CERTIFICATE_P12_BASE64
- APPLE_CERTIFICATE_PASSWORD
- APPLE_PROVISION_PROFILE_BASE64
- APPLE_PROFILE_NAME
- APPSTORE_API_KEY_ID
- APPSTORE_API_ISSUER_ID
- APPSTORE_API_KEY_P8_BASE64

The base64 certificate, provisioning profile and API key secrets must be encoded from their original files. Run `Signed iPhone TestFlight Release` in GitHub Actions after configuration. It will create a signed IPA and attempt an upload to Apple TestFlight.
After Apple processing and any required review, invite the iPhone tester and install using Apple's TestFlight app.

A GitHub ZIP, simulator app or unsigned IPA will not install directly on a normal iPhone. Some languages have limited translation and speech support in Apple's built-in frameworks.