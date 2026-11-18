package com.etyhan.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * 真机之外的最低限度运行时验证：Activity 能起、Compose 树能组、导航能跑、Room/DataStore 能初始化。
 * （不是 UI 全量测试，只是防“一点就崩”。）
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AppSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesWithBrandAndSearchEntry() {
        rule.onNodeWithText("EtyHan").assertIsDisplayed()
        rule.onNodeWithText("分析").assertIsDisplayed()
    }

    @Test
    fun bottomNavReachesSettings() {
        rule.onNodeWithText("设置").performClick()
        rule.onNodeWithText("模型服务").assertIsDisplayed()
    }

    @Test
    fun bottomNavReachesBank() {
        rule.onNodeWithText("词库").performClick()
        rule.onNodeWithText("查过的词与句子都会留在这里，可离线翻看").assertIsDisplayed()
    }
}
