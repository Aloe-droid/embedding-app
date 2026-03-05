package com.aloe.embedding.embedder

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EmbedderModule {

    @Provides
    @Singleton
    fun provideBaseEmbedder(
        @ApplicationContext context: Context
    ): BaseEmbedder = JinaEmbedder(context)
}
