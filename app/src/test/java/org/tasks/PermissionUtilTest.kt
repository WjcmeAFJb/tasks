package org.tasks

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionUtilTest {

    // PERMISSION_GRANTED = 0, PERMISSION_DENIED = -1
    private val PERMISSION_GRANTED = 0
    private val PERMISSION_DENIED = -1

    @Test
    fun emptyArrayReturnsFalse() {
        assertFalse(PermissionUtil.verifyPermissions(intArrayOf()))
    }

    @Test
    fun singleGrantedReturnsTrue() {
        assertTrue(PermissionUtil.verifyPermissions(intArrayOf(PERMISSION_GRANTED)))
    }

    @Test
    fun singleDeniedReturnsFalse() {
        assertFalse(PermissionUtil.verifyPermissions(intArrayOf(PERMISSION_DENIED)))
    }

    @Test
    fun multipleAllGrantedReturnsTrue() {
        assertTrue(
            PermissionUtil.verifyPermissions(
                intArrayOf(PERMISSION_GRANTED, PERMISSION_GRANTED, PERMISSION_GRANTED)
            )
        )
    }

    @Test
    fun mixedPermissionsReturnsFalse() {
        assertFalse(
            PermissionUtil.verifyPermissions(
                intArrayOf(PERMISSION_GRANTED, PERMISSION_DENIED, PERMISSION_GRANTED)
            )
        )
    }

    @Test
    fun multipleDeniedReturnsFalse() {
        assertFalse(
            PermissionUtil.verifyPermissions(
                intArrayOf(PERMISSION_DENIED, PERMISSION_DENIED)
            )
        )
    }

    @Test
    fun twoGrantedReturnsTrue() {
        assertTrue(
            PermissionUtil.verifyPermissions(
                intArrayOf(PERMISSION_GRANTED, PERMISSION_GRANTED)
            )
        )
    }

    @Test
    fun deniedThenGrantedReturnsFalse() {
        assertFalse(
            PermissionUtil.verifyPermissions(
                intArrayOf(PERMISSION_DENIED, PERMISSION_GRANTED)
            )
        )
    }

    @Test
    fun grantedThenDeniedReturnsFalse() {
        assertFalse(
            PermissionUtil.verifyPermissions(
                intArrayOf(PERMISSION_GRANTED, PERMISSION_DENIED)
            )
        )
    }
}
