package com.hanzi.learner.db

import org.json.JSONArray
import org.json.JSONObject

class BackupSerializer {
    private val versionRegex = Regex("\"version\"\\s*:\\s*(\\d+)")

    fun encode(data: BackupData): String {
        val obj = JSONObject()
        obj.put("version", data.version)

        obj.put("progress", JSONArray().apply {
            for (p in data.progress) {
                put(
                    JSONObject()
                        .put("char", p.char)
                        .put("correctCount", p.correctCount)
                        .put("wrongCount", p.wrongCount)
                        .put("lastStudiedDay", p.lastStudiedDay)
                        .put("nextDueDay", p.nextDueDay)
                        .put("intervalDays", p.intervalDays)
                )
            }
        })

        obj.put("phraseOverrides", JSONArray().apply {
            for (po in data.phraseOverrides) {
                put(
                    JSONObject()
                        .put("char", po.char)
                        .put("phrasesJson", po.phrasesJson)
                )
            }
        })

        obj.put("disabledChars", JSONArray().apply {
            for (ch in data.disabledChars) put(ch)
        })

        val settings = data.settings
        if (settings != null) {
            obj.put(
                "settings",
                JSONObject()
                    .put("duePickLimit", settings.duePickLimit)
                    .put("hintAfterMisses", settings.hintAfterMisses)
                    .put("useExternalDataset", settings.useExternalDataset)
            )
        }

        return obj.toString()
    }

    fun decode(json: String): BackupData {
        val version = versionRegex.find(json)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        if (version != 1) {
            throw IllegalArgumentException("不支持的备份版本: $version")
        }

        val root = JSONObject(json)
        val progressArr = root.optJSONArray("progress") ?: JSONArray()
        val progress = buildList(progressArr.length()) {
            for (i in 0 until progressArr.length()) {
                val p = progressArr.getJSONObject(i)
                add(
                    HanziProgressEntity(
                        char = p.getString("char"),
                        correctCount = p.optInt("correctCount", 0),
                        wrongCount = p.optInt("wrongCount", 0),
                        lastStudiedDay = p.optLong("lastStudiedDay", 0),
                        nextDueDay = p.optLong("nextDueDay", 0),
                        intervalDays = p.optInt("intervalDays", 0),
                    )
                )
            }
        }

        val phraseArr = root.optJSONArray("phraseOverrides") ?: JSONArray()
        val phraseOverrides = buildList(phraseArr.length()) {
            for (i in 0 until phraseArr.length()) {
                val po = phraseArr.getJSONObject(i)
                add(
                    PhraseOverrideEntity(
                        char = po.getString("char"),
                        phrasesJson = po.getString("phrasesJson"),
                    )
                )
            }
        }

        val disabledArr = root.optJSONArray("disabledChars") ?: JSONArray()
        val disabledChars = buildList(disabledArr.length()) {
            for (i in 0 until disabledArr.length()) add(disabledArr.getString(i))
        }

        val settingsObj = root.optJSONObject("settings")
        val settings = if (settingsObj != null) {
            AppSettingsEntity(
                id = 1,
                duePickLimit = settingsObj.optInt("duePickLimit", 50),
                hintAfterMisses = settingsObj.optInt("hintAfterMisses", 2),
                useExternalDataset = settingsObj.optBoolean("useExternalDataset", false),
            )
        } else {
            null
        }

        return BackupData(
            version = version,
            progress = progress,
            phraseOverrides = phraseOverrides,
            disabledChars = disabledChars,
            settings = settings,
        )
    }
}
