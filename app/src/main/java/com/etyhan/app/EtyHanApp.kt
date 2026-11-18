package com.etyhan.app

import android.app.Application
import android.content.Context
import com.etyhan.app.ai.AnalysisParser
import com.etyhan.app.ai.LlmClient
import com.etyhan.app.data.AnalysisRepository
import com.etyhan.app.data.EtyHanDatabase
import com.etyhan.app.data.SecretStore
import com.etyhan.app.data.SettingsRepository
import com.etyhan.app.tts.Speaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val json = AnalysisParser.json

    val settings by lazy { SettingsRepository(appContext) }
    val secrets by lazy { SecretStore(appContext) }
    val database by lazy { EtyHanDatabase.build(appContext) }
    val llm by lazy { LlmClient(json) }
    val speaker by lazy { Speaker(appContext) }

    val repository by lazy {
        AnalysisRepository(
            dao = database.analysisDao(),
            settings = settings,
            secrets = secrets,
            client = llm,
            json = json,
        )
    }

    init {
        // “韩语朗读”开关是全局的：说话器自己判断，UI 不用到处传设置。
        appScope.launch {
            settings.flow.collect { speaker.setEnabled(it.ttsEnabled) }
        }
    }
}

class EtyHanApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
