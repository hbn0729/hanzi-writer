# SPEECH MODULE KNOWLEDGE

## OVERVIEW
Minimal TTS module using Android system TextToSpeech. Speaks character + phrase on practice screen entry and on speaker icon tap.

## STRUCTURE
```text
speech/
├── contract/
│   └── TtsSpeakerContract.kt    # Simple speak/stop/shutdown interface
├── internal/
│   ├── SystemTtsSpeaker.kt      # Android system TTS implementation
│   └── PendingRequestHandler.kt # Queues requests while TTS initializes
└── TtsSpeakerComposables.kt     # rememberTtsSpeaker() Compose helper
```

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Change TTS interface | `contract/TtsSpeakerContract.kt` | isReady, speak, speakCharacterAndPhrase, stop, shutdown |
| Modify system TTS behavior | `internal/SystemTtsSpeaker.kt` | Chinese locale selection, utterance queueing |
| Compose integration | `TtsSpeakerComposables.kt` | `rememberTtsSpeaker(context)` |

## CONVENTIONS
- TTS speaker is created per-composition via `rememberTtsSpeaker(context)` — no DI needed.
- `SystemTtsSpeaker` handles pending requests via `PendingRequestHandler` while TTS engine initializes.
- Chinese locale is auto-detected and set on initialization.

## ANTI-PATTERNS
- Do not add model download/selection features — removed intentionally.
- Do not add TTS preference persistence — system defaults are used.
- Do not import `internal/` classes directly in feature modules; use `rememberTtsSpeaker`.
