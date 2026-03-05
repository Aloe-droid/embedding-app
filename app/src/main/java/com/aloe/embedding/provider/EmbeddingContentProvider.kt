package com.aloe.embedding.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.aloe.embedding.embedder.BaseEmbedder
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking

class EmbeddingContentProvider : ContentProvider() {
    private lateinit var embedder: BaseEmbedder

    override fun call(
        authority: String,
        method: String,
        arg: String?,
        extras: Bundle?
    ): Bundle? {
        // TODO(embedder test)
        if (extras == null) return null

        val test = extras.getString("text") ?: ""
        if (test.isBlank()) {
            return null
        }

        val result = runBlocking { embedder.embed(input = test, false) }
        return super.call(authority, method, arg, extras)
    }


    override fun onCreate(): Boolean {
        val appContext = context?.applicationContext ?: run {
            Log.e("EmbeddingContentProvider", "❌ Context가 null입니다. 초기화에 실패했습니다.")
            return false
        }
        val entryPoint = EntryPointAccessors.fromApplication(
            context = appContext,
            entryPoint = EmbedderEntryPoint::class.java
        )
        embedder = entryPoint.embedder()
        return true
    }

    // ---------------------------------------------------------------------------------//

    override fun query(
        p0: Uri,
        p1: Array<out String?>?,
        p2: String?,
        p3: Array<out String?>?,
        p4: String?
    ): Cursor? = null

    override fun update(
        p0: Uri,
        p1: ContentValues?,
        p2: String?,
        p3: Array<out String?>?
    ): Int = 0

    override fun delete(
        p0: Uri,
        p1: String?,
        p2: Array<out String?>?
    ): Int = 0

    override fun getType(p0: Uri): String = ""

    override fun insert(p0: Uri, p1: ContentValues?): Uri? = null

}

@InstallIn(SingletonComponent::class)
interface EmbedderEntryPoint {
    fun embedder(): BaseEmbedder
}
