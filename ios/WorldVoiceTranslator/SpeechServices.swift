import Foundation
import Speech
import AVFoundation
import Combine

final class SpeechInput: NSObject, ObservableObject {
    @Published var isRecording = false
    @Published var message = ""
    private let engine = AVAudioEngine()
    private var request: SFSpeechAudioBufferRecognitionRequest?
    private var task: SFSpeechRecognitionTask?
    private var recognizer: SFSpeechRecognizer?
    private var delivered = false

    func start(language: String, completion: @escaping (String) -> Void) {
        stop()
        delivered = false
        SFSpeechRecognizer.requestAuthorization { [weak self] permission in
            DispatchQueue.main.async {
                guard let self = self else { return }
                guard permission == .authorized else {
                    self.message = "Speech recognition access is required. Enable it in Settings."
                    return
                }
                AVAudioSession.sharedInstance().requestRecordPermission { granted in
                    DispatchQueue.main.async {
                        if granted {
                            self.begin(language: language, completion: completion)
                        } else {
                            self.message = "Microphone access is required. Enable it in Settings."
                        }
                    }
                }
            }
        }
    }

    private func begin(language: String, completion: @escaping (String) -> Void) {
        guard !engine.isRunning else { return }
        let recognizer = SFSpeechRecognizer(locale: Locale(identifier: language))
        guard let recognizer = recognizer, recognizer.isAvailable else {
            message = "Voice recognition isn't available for this language on this device. You can still type and translate."
            return
        }
        self.recognizer = recognizer
        let request = SFSpeechAudioBufferRecognitionRequest()
        request.shouldReportPartialResults = true
        self.request = request
        do {
            try AVAudioSession.sharedInstance().setCategory(.record, mode: .measurement, options: .duckOthers)
            try AVAudioSession.sharedInstance().setActive(true, options: .notifyOthersOnDeactivation)
            let input = engine.inputNode
            input.removeTap(onBus: 0)
            let format = input.outputFormat(forBus: 0)
            input.installTap(onBus: 0, bufferSize: 1024, format: format) { buffer, _ in
                request.append(buffer)
            }
            engine.prepare()
            try engine.start()
            isRecording = true
            message = "Listening… speak now."
            task = recognizer.recognitionTask(with: request) { [weak self] result, error in
                DispatchQueue.main.async {
                    guard let self = self else { return }
                    if let result = result {
                        self.message = "Recognizing: " + result.bestTranscription.formattedString
                        if result.isFinal && !self.delivered {
                            self.delivered = true
                            let text = result.bestTranscription.formattedString
                            self.stop()
                            completion(text)
                        }
                    }
                    if let error = error, !self.delivered {
                        self.message = "Speech recognition error: " + error.localizedDescription
                        self.stop()
                    }
                }
            }
        } catch {
            message = "Could not start microphone: " + error.localizedDescription
            stop()
        }
    }

    func stop() {
        if engine.isRunning { engine.stop() }
        engine.inputNode.removeTap(onBus: 0)
        request?.endAudio()
        task?.cancel()
        task = nil
        request = nil
        isRecording = false
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    deinit {
        if engine.isRunning { engine.stop() }
    }
}

final class SpeechOutput: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {
    @Published var speaking = false
    private let synthesizer = AVSpeechSynthesizer()

    override init() {
        super.init()
        synthesizer.delegate = self
    }

    func speak(_ text: String, language: String) -> Bool {
        guard !text.isEmpty else { return false }
        synthesizer.stopSpeaking(at: .immediate)
        let voice = AVSpeechSynthesisVoice(language: language)
            ?? AVSpeechSynthesisVoice(language: Locale(identifier: language).language.languageCode?.identifier ?? language)
        guard let voice = voice else { return false }
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = voice
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.93
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [.defaultToSpeaker, .duckOthers])
        try? AVAudioSession.sharedInstance().setActive(true, options: [])
        speaking = true
        synthesizer.speak(utterance)
        return true
    }
    func stop() {
        synthesizer.stopSpeaking(at: .immediate)
        speaking = false
    }
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        DispatchQueue.main.async { self.speaking = false }
    }
}
