# SPEECH MODULE KNOWLEDGE

## OVERVIEW
TTS (Text-to-Speech) module with model management, download, preview, and preference persistence. Supports system TTS and downloadable SherpaOnnx neural models.

## STRUCTURE
```text
speech/
├── contract/              # interfaces for DI and testing
│   ├── TtsSpeakerContract.kt
│   ├── TtsEngineContract.kt
│   ├── TtsModelDownloadManagerContract.kt
│   ├── TtsModelRepositoryContract.kt
│   └── PreviewAudioPlayerContract.kt
├── internal/              # implementations
│   ├── SystemTtsSpeaker.kt
│   ├── SherpaOnnxTtsSpeaker.kt
│   ├── SherpaOnnxTtsEngine.kt
│   ├── FallbackTtsSpeaker.kt
│   ├── TtsModelDownloadManager.kt
│   └── PreviewAudioPlayer.kt
├── model/                 # data classes, registry
│   ├── TtsModelInfo.kt
│   ├── TtsModelStatus.kt
│   ├── TtsModelDownloadState.kt
│   ├── TtsModelRegistry.kt
│   └── TtsSettings.kt
├── SpeechModule.kt        # factory for speakers
└── TtsSpeakerComposables.kt
```

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Create TTS speaker | `SpeechModule.kt` | factory methods for different configs |
| Add new TTS model | `model/TtsModelRegistry.kt` | register model metadata |
| Modify download behavior | `internal/TtsModelDownloadManager.kt` | pause/resume/cancel logic |
| Change speaker interface | `contract/TtsSpeakerContract.kt` | core abstraction |
| Preview audio | `internal/PreviewAudioPlayer.kt` | plays preview URLs |
| Compose integration | `TtsSpeakerComposables.kt` | `rememberTtsSpeaker` |

## CONVENTIONS
- All public APIs go through `contract/` interfaces
- `internal/` implementations should not be imported directly by features
- Model metadata lives in `model/TtsModelRegistry`
- Download states are sealed classes for exhaustive handling

## ANTI-PATTERNS
- Do not import `internal/*` classes directly in feature modules
- Do not bypass `SpeechModule` factory for speaker creation
- Do not hardcode model IDs; use `TtsModelRepositoryContract.SYSTEM_TTS_ID`
