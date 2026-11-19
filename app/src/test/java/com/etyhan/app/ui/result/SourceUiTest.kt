package com.etyhan.app.ui.result

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.etyhan.app.MainActivity
import com.etyhan.app.data.EtyItem
import com.etyhan.app.data.HanjaChar
import com.etyhan.app.data.Segment
import com.etyhan.app.tts.Speaker
import com.etyhan.app.ui.theme.EtyHanTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * 来源标签与读音标注的渲染规则：
 * 汉/英/日/本土 同等标签；汉后接汉字、英后接原词、日后接原词+罗马音、本土后什么都不接。
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class SourceUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val sino = Segment(
        ko = "학교", rom = "hakkyo", romHi = "kk", pron = "[학꾜]", why = "紧音化",
        pos = "명사", tag = "词干", zh = "学校", hanja = "學校", origin = "sino",
    )
    private val english = Segment(
        ko = "버스", rom = "beoseu", pos = "명사", zh = "公交车",
        origin = "loan", src = "bus", lang = "en",
    )
    private val japanese = Segment(
        ko = "스시", rom = "seusi", pos = "명사", zh = "寿司",
        origin = "loan", src = "すし", srcRom = "sushi", lang = "ja",
    )
    private val native = Segment(
        ko = "집", rom = "jip", pos = "명사", zh = "房子", origin = "native",
    )
    private val friend = EtyItem(
        word = "친구", origin = "sino", hanja = "親舊", rom = "chingu",
        meaning = "朋友", literal = "亲戚旧友",
        chars = listOf(HanjaChar("친", "親", "亲"), HanjaChar("구", "舊", "旧")),
    )

    @Test
    fun sourceLabelsAndFormsRender() {
        rule.runOnUiThread {
            rule.activity.setContent {
                EtyHanTheme {
                    Column(Modifier.padding(16.dp)) {
                        SegmentRow(sino, {})
                        SegmentRow(english, {})
                        SegmentRow(japanese, {})
                        SegmentRow(native, {})
                        EtyItemCard(friend, Speaker(ApplicationProvider.getApplicationContext()), {})
                    }
                }
            }
        }
        rule.waitForIdle()

        rule.onAllNodesWithText("汉").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("英").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("日").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("本土").onFirst().assertIsDisplayed()

        rule.onAllNodesWithText("學校").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("bus").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("すし").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("sushi").onFirst().assertIsDisplayed()

        rule.onAllNodesWithText("hakkyo").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("紧音化").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("[학꾜]").onFirst().assertIsDisplayed()

        // 汉字词按词源标注：即使今义（朋友）和字面义（亲戚旧友）不同
        rule.onAllNodesWithText("親舊").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("字面：亲戚旧友").onFirst().assertIsDisplayed()
        rule.onAllNodesWithText("今义：朋友").onFirst().assertIsDisplayed()
    }
}
