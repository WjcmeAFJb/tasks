package org.tasks.icons

import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.ITypeface
import java.util.LinkedList

@Suppress("EnumEntryName")
object OutlinedGoogleMaterial2 : ITypeface {

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

    override fun getIcon(key: String): IIcon = Icon.valueOf(key)

    enum class Icon constructor(override val character: Char) : IIcon {
        gmo_visibility('\uf191'),
        gmo_warning('\uf171'),
        gmo_zoom_in('\uf106'),
        ;

        override val typeface: ITypeface by lazy { OutlinedGoogleMaterial2 }
    }
}
