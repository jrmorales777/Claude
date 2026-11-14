import SwiftUI

@main
struct WorldVoiceTranslatorApp: App {
    var body: some Scene {
        WindowGroup {
            TranslatorView()
                .preferredColorScheme(.light)
        }
    }
}
