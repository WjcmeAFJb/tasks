package org.tasks.icons

import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.ITypeface
import java.util.LinkedList

/**
 * Minimal stub of OutlinedGoogleMaterial for JaCoCo coverage builds.
 *
 * The real module has 3249 enum entries whose <clinit> exceeds 64KB after
 * JaCoCo instrumentation. This stub provides just enough entries to satisfy
 * the compiler and any tests that call getIcon().
 */
@Suppress("EnumEntryName")
object OutlinedGoogleMaterial : ITypeface {

    override val fontRes: Int
        get() = R.font.google_material_font_40811b1

    override val characters: Map<String, Char> by lazy {
        Icon.values().associate { it.name to it.character }
    }

    override val mappingPrefix: String get() = "gmo"
    override val fontName: String get() = "Google Material"
    override val version: String get() = "1"
    override val iconCount: Int get() = characters.size
    override val icons: List<String> get() = characters.keys.toCollection(LinkedList())
    override val author: String get() = "Google"
    override val url: String get() = "https://github.com/google/material-design-icons"
    override val description: String get() = "Google Material Icons (stub)"
    override val license: String get() = "Apache 2.0"
    override val licenseUrl: String get() = "https://github.com/google/material-design-icons/blob/master/LICENSE"

    override fun getIcon(key: String): IIcon = try {
        Icon.valueOf(key)
    } catch (e: IllegalArgumentException) {
        OutlinedGoogleMaterial2.getIcon(key)
    }

    enum class Icon constructor(override val character: Char) : IIcon {
        gmo_check('\uf0c8'),
        gmo_list('\uf0c9'),
        gmo_home('\uf0ca'),
        gmo_settings('\uf0cb'),
        gmo_search('\uf0cc'),
        ;

        override val typeface: ITypeface by lazy { OutlinedGoogleMaterial }
    }
}
