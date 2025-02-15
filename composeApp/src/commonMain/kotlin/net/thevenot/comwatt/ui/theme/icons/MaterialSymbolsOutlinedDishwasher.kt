import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val Dishwasher: ImageVector
	get() {
		if (_Dishwasher != null) {
			return _Dishwasher!!
		}
		_Dishwasher = ImageVector.Builder(
            name = "Dishwasher",
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
				moveTo(200f, 440f)
				verticalLineToRelative(320f)
				horizontalLineToRelative(560f)
				verticalLineToRelative(-320f)
				close()
				moveToRelative(0f, -80f)
				horizontalLineToRelative(560f)
				verticalLineToRelative(-160f)
				horizontalLineTo(200f)
				close()
				moveToRelative(280f, 360f)
				quadToRelative(-33f, 0f, -56.5f, -23.5f)
				reflectiveQuadTo(400f, 640f)
				quadToRelative(0f, -27f, 15f, -57.5f)
				reflectiveQuadTo(480f, 480f)
				quadToRelative(50f, 72f, 65f, 102.5f)
				reflectiveQuadToRelative(15f, 57.5f)
				quadToRelative(0f, 33f, -23.5f, 56.5f)
				reflectiveQuadTo(480f, 720f)
				moveToRelative(200f, -400f)
				quadToRelative(17f, 0f, 28.5f, -11.5f)
				reflectiveQuadTo(720f, 280f)
				reflectiveQuadToRelative(-11.5f, -28.5f)
				reflectiveQuadTo(680f, 240f)
				reflectiveQuadToRelative(-28.5f, 11.5f)
				reflectiveQuadTo(640f, 280f)
				reflectiveQuadToRelative(11.5f, 28.5f)
				reflectiveQuadTo(680f, 320f)
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
				moveToRelative(0f, -480f)
				verticalLineToRelative(-160f)
				close()
			}
		}.build()
		return _Dishwasher!!
	}

private var _Dishwasher: ImageVector? = null
