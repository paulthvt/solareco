package net.thevenot.comwatt.ui.preview

import de.drick.compose.hotpreview.HotPreview

private const val mdpi = 160f

@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION
)
@HotPreview(name = "Phone", widthDp = 411, heightDp = 891, density = 420 / mdpi)
@HotPreview(name = "Phone - Landscape", widthDp = 891, heightDp = 411, density = 420 / mdpi)
@HotPreview(name = "Unfolded Foldable", widthDp = 673, heightDp = 841, density = 420 / mdpi)
@HotPreview(name = "Tablet", widthDp = 1280, heightDp = 800, density = 240 / mdpi)
@HotPreview(name = "Desktop", widthDp = 1920, heightDp = 1080, density = 160 / mdpi)
annotation class HotPreviewScreenSizes

@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION
)
@HotPreview(name = "Light", darkMode = false)
@HotPreview(name = "Dark", darkMode = true)
annotation class HotPreviewLightDark