package com.aloe.embedding

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aloe.embedding.embedder.BaseEmbedder
import com.aloe.embedding.embedder.GemmaEmbedder
import com.aloe.embedding.embedder.JinaEmbedder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val input = "How do I use Jina ONNX models?"

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.aloe.embedding", appContext.packageName)
    }

    @Test
    fun jinaTest() = runTest {
        // 임베딩에 걸린 총 시간: 44.283593ms
        val jinaEmbedder = JinaEmbedder(context = context)
        jinaEmbedder.embed(input = input, isQuery = true)
        jinaEmbedder.close()
    }

    @Test
    fun gemmaTest() = runTest {
        // 임베딩에 걸린 총 시간: 1318ms
        val gemmaEmbedder: BaseEmbedder = GemmaEmbedder(context = context)
        gemmaEmbedder.embed(input = input, isQuery = true)
    }

    @Test
    fun tokenizesInputTextUsingSentencepieceTokenizer() = runTest {
        val gemmaEmbedder = GemmaEmbedder(context = context)
        val tokens = gemmaEmbedder.tokenize(text = input)
        val expected = arrayOf("How", "▁do", "▁I", "▁use", "▁J", "ina", "▁ON", "NX", "▁models", "?")
        assertEquals(expected.toList(), tokens.toList())
    }

    @Test
    fun encodesInputTextToIdUsingSentencepiece() = runTest {
        val gemmaEmbedder = GemmaEmbedder(context = context)
        val ids = gemmaEmbedder.encode(text = input)
        val expected = intArrayOf(3910, 776, 564, 1161, 730, 1630, 8203, 107310, 4681, 236881)
        assertEquals(expected.toList(), ids.toList())
    }
}
