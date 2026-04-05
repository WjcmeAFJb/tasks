package com.todoroo.astrid.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.todoroo.astrid.service.Upgrade_13_11.Companion.migrateLegacyIcon

class UpgraderExtraTest {

    // ===== migrateLegacyIcon =====

    @Test
    fun migrateLegacyIconNull() {
        assertNull(null.migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconNonNumericString() {
        assertEquals("my_custom_icon", "my_custom_icon".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconEmptyString() {
        assertEquals("", "".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconZeroReturnsNull() {
        assertNull("0".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconMinusOneReturnsNull() {
        assertNull("-1".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1ReturnsLabel() {
        assertEquals("label", "1".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex2ReturnsFilterList() {
        assertEquals("filter_list", "2".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex3ReturnsCloud() {
        assertEquals("cloud", "3".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex4ReturnsAllInbox() {
        assertEquals("all_inbox", "4".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex5ReturnsLabelOff() {
        assertEquals("label_off", "5".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex6ReturnsHistory() {
        assertEquals("history", "6".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex7ReturnsToday() {
        assertEquals("today", "7".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex8ReturnsList() {
        assertEquals("list", "8".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1000ReturnsFlag() {
        assertEquals("flag", "1000".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1062ReturnsHome() {
        assertEquals("home", "1062".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1041ReturnsWorkOutline() {
        assertEquals("work_outline", "1041".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1001ReturnsPets() {
        assertEquals("pets", "1001".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1046ReturnsDelete() {
        assertEquals("delete", "1046".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1050ReturnsPlace() {
        assertEquals("location_on", "1050".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1075ReturnsTimer() {
        assertEquals("timer", "1075".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1076ReturnsClear() {
        assertEquals("clear", "1076".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1079ReturnsNotifications() {
        assertEquals("notifications", "1079".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1124ReturnsEdit() {
        assertEquals("edit", "1124".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1170ReturnsPendingActions() {
        assertEquals("pending_actions", "1170".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1186ReturnsBlock() {
        assertEquals("block", "1186".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconUnknownNumericIndexReturnsNull() {
        assertNull("9999".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1185ReturnsPersonAdd() {
        assertEquals("person_add", "1185".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1002ReturnsPayment() {
        assertEquals("payment", "1002".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1003ReturnsAttachMoney() {
        assertEquals("attach_money", "1003".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1059ReturnsEuroSymbol() {
        assertEquals("euro_symbol", "1059".migrateLegacyIcon())
    }

    // ===== Upgrade_13_11.VERSION constant =====

    @Test
    fun upgrade13_11VersionConstant() {
        assertEquals(131100, Upgrade_13_11.VERSION)
    }

    // ===== getLegacyColor additional boundary tests =====

    @Test
    fun getLegacyColorIndex0IsBluGrey500() {
        val color = Upgrader.getLegacyColor(0, -1)
        assertTrue("Index 0 should return blue_grey_500 resource", color > 0)
    }

    @Test
    fun getLegacyColorIndex20IsWhite100() {
        val color = Upgrader.getLegacyColor(20, -1)
        assertTrue("Index 20 should return white_100 resource", color > 0)
    }

    @Test
    fun getLegacyColorAdjacentIndicesAreDifferent() {
        for (i in 0 until 20) {
            val color1 = Upgrader.getLegacyColor(i, 0)
            val color2 = Upgrader.getLegacyColor(i + 1, 0)
            assertTrue(
                "Index $i and ${i + 1} should have different colors",
                color1 != color2
            )
        }
    }

    @Test
    fun getLegacyColorMaxIntReturnsDefault() {
        assertEquals(0, Upgrader.getLegacyColor(Int.MAX_VALUE, 0))
    }

    @Test
    fun getLegacyColorMinIntReturnsDefault() {
        assertEquals(0, Upgrader.getLegacyColor(Int.MIN_VALUE, 0))
    }

    // ===== migrateLegacyIcon for broad range of known indices =====

    @Test
    fun allKnownLegacyIconsReturnNonNull() {
        val knownIndices = listOf(
            1, 2, 3, 4, 5, 6, 7, 8,
            1000, 1001, 1002, 1003, 1004, 1005,
            1006, 1007, 1008, 1009, 1010, 1011,
            1012, 1013, 1014, 1015, 1016, 1017,
            1018, 1019, 1020, 1021, 1022, 1023,
            1024, 1025, 1026, 1027, 1028, 1029,
            1030, 1031, 1032, 1033, 1034, 1035,
            1036, 1037, 1039, 1040, 1041, 1042,
            1043, 1044, 1045, 1046, 1047, 1048,
            1049, 1050, 1051, 1052, 1053, 1054,
            1055, 1056, 1057, 1058, 1059, 1060,
            1061, 1062, 1063, 1064, 1038, 1065,
            1066, 1067, 1068, 1069, 1070, 1071,
            1072, 1073, 1074, 1075, 1076, 1077,
            1078, 1079, 1080, 1081, 1082, 1083,
            1084, 1085, 1086, 1087, 1088, 1089,
            1090, 1091, 1092, 1093, 1094, 1095,
            1096, 1097, 1098, 1099, 1100, 1101,
            1102, 1103, 1104, 1105, 1106, 1107,
            1108, 1109, 1110, 1111, 1112, 1113,
            1114, 1115, 1116, 1117, 1118, 1119,
            1121, 1122, 1123, 1124, 1125, 1126,
            1127, 1128, 1129, 1130, 1131, 1132,
            1133, 1134, 1135, 1136, 1137, 1138,
            1139, 1140, 1141, 1142, 1143, 1144,
            1145, 1146, 1147, 1148, 1149, 1150,
            1151, 1152, 1153, 1154, 1155, 1156,
            1157, 1158, 1159, 1160, 1161, 1162,
            1163, 1164, 1165, 1166, 1167, 1168,
            1169, 1170, 1171, 1172, 1173, 1174,
            1175, 1176, 1177, 1178, 1179, 1180,
            1181, 1182, 1183, 1185, 1186,
        )
        for (index in knownIndices) {
            val result = index.toString().migrateLegacyIcon()
            assertNotNull("Index $index should map to a non-null icon name", result)
        }
    }

    @Test
    fun nullMappedIndicesReturnNull() {
        assertNull("-1".migrateLegacyIcon())
        assertNull("0".migrateLegacyIcon())
    }

    // ===== migrateLegacyIcon specific icon names =====

    @Test
    fun migrateLegacyIconIndex1004ReturnsHourglassEmpty() {
        assertEquals("hourglass_empty", "1004".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1005ReturnsFavoriteBorder() {
        assertEquals("favorite_border", "1005".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1023ReturnsFlight() {
        assertEquals("flight", "1023".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1057ReturnsFlightTakeoff() {
        assertEquals("flight_takeoff", "1057".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1058ReturnsFlightLand() {
        assertEquals("flight_land", "1058".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1044ReturnsSchedule() {
        assertEquals("schedule", "1044".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1049ReturnsEvent() {
        assertEquals("event", "1049".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1088ReturnsSpa() {
        assertEquals("spa", "1088".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1114ReturnsSecurity() {
        assertEquals("security", "1114".migrateLegacyIcon())
    }

    @Test
    fun migrateLegacyIconIndex1148ReturnsHouse() {
        assertEquals("house", "1148".migrateLegacyIcon())
    }
}
