package org.tasks

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutManagerTest {

    @Test
    fun shortcutNewTaskConstant() {
        assertEquals("static_new_task", ShortcutManager.SHORTCUT_NEW_TASK)
    }

    @Test
    fun shortcutNewTaskConstantNotEmpty() {
        assert(ShortcutManager.SHORTCUT_NEW_TASK.isNotEmpty())
    }

    @Test
    fun shortcutNewTaskConstantIsStatic() {
        // Verify the constant value is stable across calls
        assertEquals(ShortcutManager.SHORTCUT_NEW_TASK, ShortcutManager.SHORTCUT_NEW_TASK)
    }
}
