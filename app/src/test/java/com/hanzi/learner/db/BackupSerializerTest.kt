package com.hanzi.learner.db

import org.junit.Assert.fail
import org.junit.Test

class BackupSerializerTest {
    @Test
    fun decode_throwsOnUnsupportedVersion() {
        val serializer = BackupSerializer()
        try {
            serializer.decode("""{"version":999}""")
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
        }
    }
}
