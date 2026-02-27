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
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.aloe.embedding", appContext.packageName)
    }

    @Test
    fun jinaTest() = runTest {
        // 임베딩에 걸린 총 시간: 44.283593ms
        val context = ApplicationProvider.getApplicationContext<Context>()
        val jinaEmbedder = JinaEmbedder(context = context)
        // [Query: How do I use Jina ONNX models?]
        jinaEmbedder.embed(input = "How do I use Jina ONNX models?", isQuery = true)
        jinaEmbedder.close()
    }


    @Test
    fun gemmaTest() = runTest {
        // 임베딩에 걸린 총 시간: 1318ms
        val context = ApplicationProvider.getApplicationContext<Context>()
        val gemmaEmbedder: BaseEmbedder = GemmaEmbedder(context = context)
        val input = "How do I use Jina ONNX models?"
        gemmaEmbedder.embed(input = input, isQuery = true)
    }
}
