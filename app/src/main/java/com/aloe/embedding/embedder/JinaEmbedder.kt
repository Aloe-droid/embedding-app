package com.aloe.embedding.embedder

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import com.aloe.embedding.util.LoadUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class JinaEmbedder(private val context: Context) : BaseEmbedder() {
    private lateinit var ortSession: OrtSession
    private lateinit var ortEnv: OrtEnvironment

    override suspend fun initInternal() {
        val modelPath = LoadUtil.copyFromAssetsToFilesDir(context = context, fileName = ONNX_MODEL)
        ortEnv = OrtEnvironment.getEnvironment()
        ortSession = ortEnv.createSession(modelPath)
    }

    override suspend fun embedInternal(input: String, isQuery: Boolean): FloatArray {
        val tokenArray = tokenize(input = input, isQuery = isQuery)
        val onnxInput = preprocess(tokenArray)
        val output = ortSession.run(onnxInput)
        return postprocess(output)
    }

    private fun tokenize(input: String, isQuery: Boolean): IntArray {
        val prefix = if (isQuery) "Query: " else "Document: "
        val prefixedInput = "$prefix$input"
        // TODO("문자열 -> IntArray 토크나이저 구현 필요")
        return intArrayOf(2929, 25, 2650, 656, 358, 1005, 622, 2259, 6328, 44404, 4211, 30, 128001)
    }

    private fun preprocess(tokenArray: IntArray): Map<String, OnnxTensor> {
        val attentionArray = LongArray(tokenArray.size) { MASK_ACTIVE.toLong() }
        val order = ByteOrder.nativeOrder()

        val inputLongArray = tokenArray.map { it.toLong() }.toLongArray()
        val buffer = ByteBuffer
            .allocateDirect(inputLongArray.size * Long.SIZE_BYTES)
            .order(order)
            .asLongBuffer()
        buffer.put(inputLongArray)
        buffer.rewind()

        val inputShape = longArrayOf(BATCH_SIZE, inputLongArray.size.toLong())

        val attentionBuffer = ByteBuffer
            .allocateDirect(attentionArray.size * Long.SIZE_BYTES)
            .order(order)
            .asLongBuffer()
        attentionBuffer.put(attentionArray)
        attentionBuffer.rewind()

        val attentionShape = longArrayOf(BATCH_SIZE, attentionArray.size.toLong())

        val inputTensor = OnnxTensor.createTensor(ortEnv, buffer, inputShape)
        val attentionTensor = OnnxTensor.createTensor(ortEnv, attentionBuffer, attentionShape)

        return mapOf(
            INPUT_NAME to inputTensor,
            ATTENTION_NAME to attentionTensor
        )
    }

    private fun postprocess(rawResult: OrtSession.Result): FloatArray {
        val rawEmbeddings = (rawResult.get(HIDDEN_STATE).get().value as Array<*>)[0] as Array<*>
        return rawEmbeddings.last() as FloatArray
    }

    public override fun close() {
        runCatching { ortSession.close() }
            .onFailure { it.printStackTrace() }
    }

    companion object {
        private const val ONNX_MODEL = "model_merged.onnx"
        private const val INPUT_NAME = "input_ids"
        private const val ATTENTION_NAME = "attention_mask"
        private const val HIDDEN_STATE = "last_hidden_state"
        private const val MASK_ACTIVE: Int = 1
        private const val BATCH_SIZE: Long = 1L
    }
}
