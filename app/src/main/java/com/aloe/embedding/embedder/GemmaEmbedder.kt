package com.aloe.embedding.embedder

import android.content.Context
import com.aloe.embedding.util.LoadUtil
import com.google.ai.edge.localagents.rag.models.EmbedData
import com.google.ai.edge.localagents.rag.models.EmbeddingRequest
import com.google.ai.edge.localagents.rag.models.GemmaEmbeddingModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.guava.await

class GemmaEmbedder(private val context: Context) : BaseEmbedder() {
    private lateinit var model: GemmaEmbeddingModel
    private val tokenizerReady = CompletableDeferred<Unit>()

    override suspend fun initInternal() {
        System.loadLibrary("native-lib")
        val modelPath = LoadUtil.copyFromAssetsToFilesDir(context = context, fileName = MODEL_PATH)
        val tokenPath =
            LoadUtil.copyFromAssetsToFilesDir(context = context, fileName = TOKENIZER_PATH)
        GemmaEmbeddingModel(modelPath, tokenPath, USE_GPU).also {
            model = it
        }

        val isLoad = loadTokenizerModel(path = tokenPath)
        if (isLoad) tokenizerReady.complete(Unit)
        else throw IllegalStateException("Tokenizer load failed")
    }

    override suspend fun embedInternal(input: String, isQuery: Boolean): FloatArray {
        val request = preprocess(input = input, isQuery = isQuery)
        val result = model.getEmbeddings(request).await()
        return result.toFloatArray()
    }

    private fun preprocess(input: String, isQuery: Boolean): EmbeddingRequest<String> {
        val task =
            if (isQuery) EmbedData.TaskType.RETRIEVAL_QUERY else EmbedData.TaskType.RETRIEVAL_DOCUMENT

        val data: EmbedData<String> = EmbedData.builder<String>()
            .setData(input)
            .setTask(task)
            .setIsQuery(isQuery)
            .build()

        val request: EmbeddingRequest<String> = EmbeddingRequest.builder<String>()
            .addEmbedData(data)
            .build()

        return request
    }

    suspend fun tokenize(text: String): Array<String> {
        tokenizerReady.await()
        return nativeTokenize(text)
    }

    suspend fun encode(text: String): IntArray {
        tokenizerReady.await()
        return nativeEncode(text)
    }

    private external fun loadTokenizerModel(path: String): Boolean
    private external fun nativeTokenize(text: String): Array<String>
    private external fun nativeEncode(text: String): IntArray

    companion object {
        private const val MODEL_PATH = "embeddinggemma.tflite"
        private const val TOKENIZER_PATH = "sentencepiece.model"
        private const val USE_GPU = false
    }
}
