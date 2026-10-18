package app.etsprivate.mail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

internal object GmailIcons {
    val Reply: ImageVector by lazy {
        symbol("Reply", "M760-200v-160q0-50-35-85t-85-35H273l144 144-57 56-240-240 240-240 57 56-144 144h367q83 0 141.5 58.5T840-360v160h-80Z")
    }
    val ReplyAll: ImageVector by lazy {
        symbol("ReplyAll", "M240-280 0-520l240-240 56 57-183 183 183 183-56 57Zm480 80v-160q0-50-35-85t-85-35H433l144 144-57 56-240-240 240-240 57 56-144 144h167q83 0 141.5 58.5T800-360v160h-80Z")
    }
    val Forward: ImageVector by lazy {
        symbol("Forward", "M120-200v-160q0-83 58.5-141.5T320-560h367L543-704l57-56 240 240-240 240-57-56 144-144H320q-50 0-85 35t-35 85v160h-80Z")
    }

    val Edit: ImageVector by lazy {
        symbol("Edit", "M120-120v-170l528-527q12-11 26.5-17t30.5-6q16 0 31 6t26 18l55 56q12 11 17.5 26t5.5 30q0 16-5.5 30.5T817-647L290-120H120Zm584-528 56-56-56-56-56 56 56 56Z", mirror = false)
    }

    private fun symbol(name: String, path: String, mirror: Boolean = true) = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f,
        autoMirror = mirror,
    ).addGroup(translationY = 960f)
        .addPath(PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
        .clearGroup()
        .build()
}
