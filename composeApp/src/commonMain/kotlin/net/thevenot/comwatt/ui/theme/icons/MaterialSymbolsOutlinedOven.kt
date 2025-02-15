
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val Oven: ImageVector
	get() {
		if (_Oven != null) {
			return _Oven!!
		}
		_Oven = ImageVector.Builder(
            name = "Oven",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
			path(
    			fill = SolidColor(Color.Black),
    			fillAlpha = 1.0f,
    			stroke = null,
    			strokeAlpha = 1.0f,
    			strokeLineWidth = 1.0f,
    			strokeLineCap = StrokeCap.Butt,
    			strokeLineJoin = StrokeJoin.Miter,
    			strokeLineMiter = 1.0f,
    			pathFillType = PathFillType.NonZero
			) {
				moveTo(640f, 280f)
				quadToRelative(17f, 0f, 28.5f, -11.5f)
				reflectiveQuadTo(680f, 240f)
				reflectiveQuadToRelative(-11.5f, -28.5f)
				reflectiveQuadTo(640f, 200f)
				reflectiveQuadToRelative(-28.5f, 11.5f)
				reflectiveQuadTo(600f, 240f)
				reflectiveQuadToRelative(11.5f, 28.5f)
				reflectiveQuadTo(640f, 280f)
				moveToRelative(-160f, 0f)
				quadToRelative(17f, 0f, 28.5f, -11.5f)
				reflectiveQuadTo(520f, 240f)
				reflectiveQuadToRelative(-11.5f, -28.5f)
				reflectiveQuadTo(480f, 200f)
				reflectiveQuadToRelative(-28.5f, 11.5f)
				reflectiveQuadTo(440f, 240f)
				reflectiveQuadToRelative(11.5f, 28.5f)
				reflectiveQuadTo(480f, 280f)
				moveToRelative(-160f, 0f)
				quadToRelative(17f, 0f, 28.5f, -11.5f)
				reflectiveQuadTo(360f, 240f)
				reflectiveQuadToRelative(-11.5f, -28.5f)
				reflectiveQuadTo(320f, 200f)
				reflectiveQuadToRelative(-28.5f, 11.5f)
				reflectiveQuadTo(280f, 240f)
				reflectiveQuadToRelative(11.5f, 28.5f)
				reflectiveQuadTo(320f, 280f)
				moveTo(200f, 400f)
				verticalLineToRelative(360f)
				horizontalLineToRelative(560f)
				verticalLineToRelative(-360f)
				close()
				moveToRelative(200f, 160f)
				horizontalLineToRelative(160f)
				verticalLineToRelative(-80f)
				horizontalLineTo(400f)
				close()
				moveTo(200f, 840f)
				quadToRelative(-33f, 0f, -56.5f, -23.5f)
				reflectiveQuadTo(120f, 760f)
				verticalLineToRelative(-560f)
				quadToRelative(0f, -33f, 23.5f, -56.5f)
				reflectiveQuadTo(200f, 120f)
				horizontalLineToRelative(560f)
				quadToRelative(33f, 0f, 56.5f, 23.5f)
				reflectiveQuadTo(840f, 200f)
				verticalLineToRelative(560f)
				quadToRelative(0f, 33f, -23.5f, 56.5f)
				reflectiveQuadTo(760f, 840f)
				close()
				moveToRelative(280f, -440f)
			}
		}.build()
		return _Oven!!
	}

private var _Oven: ImageVector? = null
