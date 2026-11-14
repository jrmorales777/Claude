import SwiftUI
import Translation
import Speech
import AVFoundation

private enum Palette {
    static let navy = Color(red: 0.08, green: 0.19, blue: 0.36)
    static let blue = Color(red: 0.13, green: 0.39, blue: 0.86)
    static let sky = Color(red: 0.30, green: 0.65, blue: 0.98)
    static let orange = Color(red: 1.0, green: 0.64, blue: 0.33)
    static let muted = Color(red: 0.40, green: 0.48, blue: 0.59)
    static let canvas = Color(red: 0.95, green: 0.98, blue: 1.0)
}

private struct LanguageOption: Identifiable, Hashable {
    let code: String
    let label: String
    var id: String { code }
}
private let choices: [LanguageOption] = [
    ("af", "Afrikaans"), ("ar", "Arabic"), ("bn", "Bengali"), ("bg", "Bulgarian"),
    ("ca", "Catalan"), ("zh-Hans", "Chinese (Simplified)"), ("zh-Hant", "Chinese (Traditional)"),
    ("hr", "Croatian"), ("cs", "Czech"), ("da", "Danish"), ("nl", "Dutch"),
    ("en", "English"), ("et", "Estonian"), ("fi", "Finnish"), ("fr", "French"),
    ("de", "German"), ("el", "Greek"), ("gu", "Gujarati"), ("he", "Hebrew"),
    ("hi", "Hindi"), ("hu", "Hungarian"), ("id", "Indonesian"), ("ga", "Irish"),
    ("it", "Italian"), ("ja", "Japanese"), ("kn", "Kannada"), ("ko", "Korean"),
    ("lv", "Latvian"), ("lt", "Lithuanian"), ("ms", "Malay"), ("ml", "Malayalam"),
    ("mr", "Marathi"), ("no", "Norwegian"), ("fa", "Persian"), ("pl", "Polish"),
    ("pt", "Portuguese"), ("pa", "Punjabi"), ("ro", "Romanian"), ("ru", "Russian"),
    ("sr", "Serbian"), ("sk", "Slovak"), ("sl", "Slovenian"), ("es", "Spanish"),
    ("sw", "Swahili"), ("sv", "Swedish"), ("ta", "Tamil"), ("te", "Telugu"),
    ("th", "Thai"), ("tr", "Turkish"), ("uk", "Ukrainian"), ("ur", "Urdu"),
    ("vi", "Vietnamese"), ("cy", "Welsh")
].map { LanguageOption(code: $0.0, label: $0.1) }

struct TranslatorView: View {
    @StateObject private var microphone = SpeechInput()
    @StateObject private var voice = SpeechOutput()
    @State private var from = "en"
    @State private var to = "es"
    @State private var sourceText = ""
    @State private var translatedText = ""
    @State private var status = "Ready. Choose a language and tap the microphone."
    @State private var automaticSpeech = true
    @State private var translating = false
    @State private var configuration: TranslationSession.Configuration?
    @State private var textToTranslate = ""
    @State private var history: [String] = UserDefaults.standard.stringArray(forKey: "worldvoice_history") ?? []
    private var fromName: String { choices.first { $0.code == from }?.label ?? from }
    private var toName: String { choices.first { $0.code == to }?.label ?? to }

    var body: some View {
        ZStack {
            LinearGradient(colors: [Palette.canvas, .white, Palette.canvas], startPoint: .topLeading, endPoint: .bottomTrailing)
                .ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    header
                    hero
                    languagePanel
                    sourcePanel
                    translationPanel
                    optionsPanel
                    historyPanel
                    Text("WORLD VOICE  •  iOS EDITION")
                        .font(.system(size: 11, weight: .medium, design: .rounded))
                        .foregroundStyle(Palette.muted)
                        .frame(maxWidth: .infinity)
                        .padding(.bottom, 25)
                }
                .padding(.horizontal, 19)
                .padding(.top, 15)
            }
        }
        .translationTask(configuration) { session in
            let original = textToTranslate
            await MainActor.run {
                translating = true
                status = "Translating… the phone may need to download language models."
            }
            do {
                let response = try await session.translate(original)
                await MainActor.run {
                    translatedText = response.targetText
                    translating = false
                    status = "Translated from \(fromName) to \(toName)."
                    let item = "\(fromName) → \(toName)\n\(original)\n\(response.targetText)"
                    history.insert(item, at: 0)
                    history = Array(history.prefix(20))
                    UserDefaults.standard.set(history, forKey: "worldvoice_history")
                    if automaticSpeech {
                        if !voice.speak(response.targetText, language: to) {
                            status = "Translation ready. No installed speaking voice is available for \(toName)."
                        }
                    }
                }
            } catch {
                await MainActor.run {
                    translating = false
                    status = "Translation unavailable: \(error.localizedDescription). This language pair may not be supported by the Apple translation engine."
                }
            }
        }
    }

    private var header: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 19)
                    .fill(.white)
                    .frame(width: 70, height: 70)
                    .shadow(color: Palette.blue.opacity(0.12), radius: 12, y: 4)
                Image(systemName: "globe.americas.fill")
                    .font(.system(size: 43, weight: .medium))
                    .foregroundStyle(LinearGradient(colors: [Palette.blue, Palette.sky], startPoint: .top, endPoint: .bottom))
                Image(systemName: "waveform")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(.white)
                    .padding(5)
                    .background(Palette.blue, in: Capsule())
                    .offset(y: 13)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text("WORLD VOICE")
                    .font(.system(size: 23, weight: .heavy, design: .rounded))
                    .tracking(-0.7)
                    .foregroundStyle(Palette.navy)
                Text("T R A N S L A T O R")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(Palette.blue)
                Text("Speak beyond borders.")
                    .font(.system(size: 12))
                    .foregroundStyle(Palette.muted)
            }
            Spacer(minLength: 0)
        }
    }

    private var hero: some View {
        VStack(alignment: .leading, spacing: 9) {
            Text("The world, in your voice.")
                .font(.system(size: 21, weight: .bold, design: .rounded))
            Text("Speak, translate, and connect in your chosen language.")
                .font(.system(size: 14))
                .opacity(0.94)
            RoundedRectangle(cornerRadius: 2)
                .fill(LinearGradient(colors: [Palette.orange, .white.opacity(0)], startPoint: .leading, endPoint: .trailing))
                .frame(height: 3)
                .padding(.top, 3)
        }
        .foregroundStyle(.white)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(20)
        .background(LinearGradient(colors: [Color(red: 0.1, green: 0.31, blue: 0.68), Palette.blue, Palette.sky], startPoint: .topLeading, endPoint: .bottomTrailing), in: RoundedRectangle(cornerRadius: 25))
    }

    private var languagePanel: some View {
        card {
            title("CHOOSE YOUR LANGUAGES")
            HStack(spacing: 8) {
                picker("From", selected: $from)
                Button { swap() } label: {
                    Image(systemName: "arrow.left.arrow.right")
                        .font(.system(size: 18, weight: .bold))
                        .frame(width: 46, height: 52)
                }
                .buttonStyle(SecondaryStyle())
                .accessibilityLabel("Switch languages")
                picker("To", selected: $to)
            }
            Text("Voice recognition and spoken playback depend on iPhone language availability. Not every listed language pair supports Apple translation.")
                .font(.system(size: 11))
                .foregroundStyle(Palette.muted)
        }
    }

    private func picker(_ label: String, selected: Binding<String>) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(label.uppercased()).font(.system(size: 11, weight: .bold)).foregroundStyle(Palette.muted)
            Picker(label, selection: selected) {
                ForEach(choices) { option in
                    Text(option.label).tag(option.code)
                }
            }
            .labelsHidden()
            .tint(Palette.blue)
            .frame(maxWidth: .infinity)
            .frame(height: 42)
            .background(Palette.canvas, in: RoundedRectangle(cornerRadius: 12))
        }
        .frame(maxWidth: .infinity)
    }

    private var sourcePanel: some View {
        card {
            title("01  YOUR MESSAGE")
            ZStack(alignment: .topLeading) {
                if sourceText.isEmpty {
                    Text("Speak or type your message…")
                        .foregroundStyle(Palette.muted)
                        .padding(.top, 12)
                        .padding(.leading, 12)
                }
                TextEditor(text: $sourceText)
                    .scrollContentBackground(.hidden)
                    .frame(height: 100)
                    .padding(6)
            }
            .background(Palette.canvas, in: RoundedRectangle(cornerRadius: 17))
            .font(.system(size: 18))
            .foregroundStyle(Palette.navy)

            Button {
                if microphone.isRecording {
                    microphone.stop()
                    status = "Stopped listening."
                } else {
                    voice.stop()
                    microphone.start(language: from) { words in
                        sourceText = words
                        status = "Recognized your words. Translating…"
                        translate()
                    }
                }
            } label: {
                VStack(spacing: 11) {
                    ZStack {
                        Circle()
                            .fill(LinearGradient(colors: [Palette.blue, Palette.sky], startPoint: .topLeading, endPoint: .bottomTrailing))
                            .frame(width: 105, height: 105)
                            .overlay(Circle().stroke(Color.white.opacity(0.8), lineWidth: 5))
                            .shadow(color: Palette.blue.opacity(0.25), radius: 12, y: 5)
                        Image(systemName: microphone.isRecording ? "stop.fill" : "mic.fill")
                            .font(.system(size: 41, weight: .medium))
                            .foregroundStyle(.white)
                    }
                    .scaleEffect(microphone.isRecording ? 1.07 : 1.0)
                    .animation(.easeInOut(duration: 0.5), value: microphone.isRecording)
                    Text(microphone.isRecording ? "TAP TO STOP" : "TAP TO SPEAK")
                        .font(.system(size: 13, weight: .bold))
                        .tracking(1)
                        .foregroundStyle(Palette.blue)
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(.plain)
            Button(action: translate) {
                HStack {
                    if translating { ProgressView().tint(.white) }
                    Text(translating ? "Translating…" : "Translate my message")
                    Image(systemName: "arrow.right")
                }
                .frame(maxWidth: .infinity)
            }
            .buttonStyle(PrimaryStyle())
            .disabled(translating)
        }
    }

    private var translationPanel: some View {
        card {
            title("02  YOUR TRANSLATION")
            Text(translatedText.isEmpty ? "Your translation will appear here." : translatedText)
                .font(.system(size: 20))
                .foregroundStyle(Palette.navy)
                .frame(maxWidth: .infinity, minHeight: 90, alignment: .topLeading)
                .padding(17)
                .background(Palette.canvas, in: RoundedRectangle(cornerRadius: 17))

            HStack(spacing: 9) {
                Button {
                    if !voice.speak(translatedText, language: to) {
                        status = "No iPhone voice available for \(toName). You can still read the translation."
                    }
                } label: {
                    Label("Play voice", systemImage: "play.fill").frame(maxWidth: .infinity)
                }.buttonStyle(SecondaryStyle())
                Button { voice.stop() } label: {
                    Label("Stop", systemImage: "stop.fill").frame(maxWidth: .infinity)
                }.buttonStyle(SecondaryStyle())
            }
            Button {
                swap()
                sourceText = ""
                translatedText = ""
                microphone.start(language: from) { words in
                    sourceText = words
                    translate()
                }
            } label: {
                Label("Switch speakers & reply", systemImage: "arrow.left.arrow.right")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(PrimaryStyle())
        }
    }

    private var optionsPanel: some View {
        card {
            Toggle("Speak translations automatically", isOn: $automaticSpeech)
                .tint(Palette.blue)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(Palette.navy)
            Divider()
            Text(microphone.isRecording ? microphone.message : status)
                .font(.system(size: 13))
                .foregroundStyle(Palette.muted)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(10)
                .background(Palette.canvas, in: RoundedRectangle(cornerRadius: 11))
        }
    }

    private var historyPanel: some View {
        card {
            HStack {
                title("RECENT CONVERSATIONS")
                Spacer()
                Button("Clear") {
                    history = []
                    UserDefaults.standard.removeObject(forKey: "worldvoice_history")
                }
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(Palette.blue)
            }
            if history.isEmpty {
                Text("No translations yet.")
                    .font(.system(size: 13))
                    .foregroundStyle(Palette.muted)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(12)
                    .background(Palette.canvas, in: RoundedRectangle(cornerRadius: 13))
            } else {
                ForEach(Array(history.prefix(8).enumerated()), id: \.offset) { _, entry in
                    Text(entry)
                        .font(.system(size: 13))
                        .foregroundStyle(Palette.navy)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(12)
                        .background(Palette.canvas, in: RoundedRectangle(cornerRadius: 13))
                }
            }
        }
    }

    private func card<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 14, content: content)
            .padding(17)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.white, in: RoundedRectangle(cornerRadius: 24))
            .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color(red: 0.88, green: 0.92, blue: 0.98), lineWidth: 1))
            .shadow(color: Palette.blue.opacity(0.05), radius: 10, y: 3)
    }
    private func title(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 12, weight: .bold))
            .tracking(1)
            .foregroundStyle(Palette.muted)
    }
    private func swap() {
        let previous = from
        from = to
        to = previous
        configuration = nil
        voice.stop()
        microphone.stop()
        status = "Languages switched."
    }
    private func translate() {
        let original = sourceText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !original.isEmpty else {
            status = "Speak or type a message before tapping Translate."
            return
        }
        voice.stop()
        if from == to {
            translatedText = original
            status = "Both languages are the same."
            if automaticSpeech { _ = voice.speak(original, language: to) }
            return
        }
        translating = true
        textToTranslate = original
        translatedText = ""
        status = "Preparing translation. Download permission may be requested."
        if configuration == nil {
            configuration = TranslationSession.Configuration(source: Locale.Language(identifier: from), target: Locale.Language(identifier: to))
        } else {
            configuration?.invalidate()
        }
    }
}
private struct PrimaryStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 15, weight: .semibold))
            .foregroundStyle(.white)
            .padding(.vertical, 17)
            .padding(.horizontal, 15)
            .background(Palette.blue, in: RoundedRectangle(cornerRadius: 16))
            .opacity(configuration.isPressed ? 0.83 : 1)
    }
}
private struct SecondaryStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.system(size: 14, weight: .semibold))
            .foregroundStyle(Palette.blue)
            .padding(.vertical, 16)
            .padding(.horizontal, 10)
            .background(Palette.canvas, in: RoundedRectangle(cornerRadius: 15))
            .opacity(configuration.isPressed ? 0.80 : 1)
    }
}
