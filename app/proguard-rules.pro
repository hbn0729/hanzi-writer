# Room entities & DAOs
-keep class com.hanzi.learner.data.local.entity.** { *; }
-keep class com.hanzi.learner.data.local.dao.** { *; }
# TTS callback
-keep class * extends android.speech.tts.UtteranceProgressListener { *; }
# Keep data classes used with JSON parsing
-keep class com.hanzi.learner.character_writer.model.CharacterData { *; }
-keep class com.hanzi.learner.character_writer.model.Point { *; }
-keep class com.hanzi.learner.character_writer.data.CharIndexItem { *; }
