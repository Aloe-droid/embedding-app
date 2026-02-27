package com.aloe.embedding.embedder

import android.content.Context
import com.aloe.embedding.util.LoadUtil
import com.google.ai.edge.localagents.rag.models.EmbedData
import com.google.ai.edge.localagents.rag.models.EmbeddingRequest
import com.google.ai.edge.localagents.rag.models.GemmaEmbeddingModel
import kotlinx.coroutines.guava.await

class GemmaEmbedder(private val context: Context) : BaseEmbedder() {
    private lateinit var model: GemmaEmbeddingModel

    override suspend fun initInternal() {
        System.loadLibrary("native-lib")
        val modelPath = LoadUtil.copyFromAssetsToFilesDir(context = context, fileName = MODEL_PATH)
        val tokenPath =
            LoadUtil.copyFromAssetsToFilesDir(context = context, fileName = TOKENIZER_PATH)
        GemmaEmbeddingModel(modelPath, tokenPath, USE_GPU).also {
            model = it
        }
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

    companion object {
        private const val MODEL_PATH = "embeddinggemma.tflite"
        private const val TOKENIZER_PATH = "sentencepiece.model"
        private const val USE_GPU = false
    }
}
