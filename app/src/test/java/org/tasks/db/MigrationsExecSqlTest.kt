package org.tasks.db

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.tasks.caldav.FileStorage

/**
 * Tests that individual migrations execute the expected SQL statements.
 * We verify the migrations by calling migrate() with a mocked SQLiteConnection
 * and capturing the SQL strings passed to prepare().
 */
class MigrationsExecSqlTest {

    private lateinit var connection: SQLiteConnection
    private lateinit var statement: SQLiteStatement
    private val executedSql = mutableListOf<String>()

    @Before
    fun setUp() {
        statement = mock(SQLiteStatement::class.java)
        executedSql.clear()

        connection = object : SQLiteConnection {
            override fun prepare(sql: String): SQLiteStatement {
                executedSql.add(sql)
                return statement
            }

            override fun close() {}
        }
    }

    private fun getMigrations(): Array<Migration> {
        val context = mock(Context::class.java)
        val fileStorage = mock(FileStorage::class.java)
        return Migrations.migrations(context, fileStorage)
    }

    private fun findMigration(startVersion: Int, endVersion: Int): Migration {
        val migrations = getMigrations()
        return migrations.first { it.startVersion == startVersion && it.endVersion == endVersion }
    }

    // --- MIGRATION_35_36 ---
    @Test
    fun migration35To36AddColorColumn() {
        findMigration(35, 36).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `tagdata` ADD COLUMN `color`") })
    }

    // --- MIGRATION_36_37 ---
    @Test
    fun migration36To37AddDeletedColumn() {
        findMigration(36, 37).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `store` ADD COLUMN `deleted`") })
    }

    // --- MIGRATION_37_38 ---
    @Test
    fun migration37To38AddValue4Column() {
        findMigration(37, 38).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `store` ADD COLUMN `value4`") })
    }

    // --- MIGRATION_38_39 ---
    @Test
    fun migration38To39CreatesNotificationTable() {
        findMigration(38, 39).migrate(connection)
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `notification`") })
        assertTrue(executedSql.any { it.contains("CREATE UNIQUE INDEX `index_notification_task`") })
    }

    // --- MIGRATION_39_46 (noop) ---
    @Test
    fun migration39To46IsNoop() {
        findMigration(39, 46).migrate(connection)
        assertTrue(executedSql.isEmpty())
    }

    // --- MIGRATION_46_47 ---
    @Test
    fun migration46To47CreatesAlarmsTable() {
        findMigration(46, 47).migrate(connection)
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `alarms`") })
        assertTrue(executedSql.any { it.contains("INSERT INTO `alarms`") })
        assertTrue(executedSql.any { it.contains("DELETE FROM `metadata` WHERE `key` = 'alarm'") })
    }

    // --- MIGRATION_47_48 ---
    @Test
    fun migration47To48CreatesLocationsTable() {
        findMigration(47, 48).migrate(connection)
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `locations`") })
        assertTrue(executedSql.any { it.contains("INSERT INTO `locations`") })
        assertTrue(executedSql.any { it.contains("DELETE FROM `metadata` WHERE `key` = 'geofence'") })
    }

    // --- MIGRATION_48_49 ---
    @Test
    fun migration48To49CreatesTagsTable() {
        findMigration(48, 49).migrate(connection)
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `tags`") })
        assertTrue(executedSql.any { it.contains("INSERT INTO `tags`") })
        assertTrue(executedSql.any { it.contains("DELETE FROM `metadata` WHERE `key` = 'tags-tag'") })
    }

    // --- MIGRATION_49_50 ---
    @Test
    fun migration49To50CreatesGoogleTasksTable() {
        findMigration(49, 50).migrate(connection)
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `google_tasks`") })
        assertTrue(executedSql.any { it.contains("INSERT INTO `google_tasks`") })
        assertTrue(executedSql.any { it.contains("DROP TABLE IF EXISTS `metadata`") })
    }

    // --- MIGRATION_50_51 ---
    @Test
    fun migration50To51CreatesFiltersTable() {
        findMigration(50, 51).migrate(connection)
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `filters`") })
        assertTrue(executedSql.any { it.contains("INSERT INTO `filters`") })
        assertTrue(executedSql.any { it.contains("DELETE FROM `store` WHERE `type` = 'filter'") })
    }

    // --- MIGRATION_51_52 ---
    @Test
    fun migration51To52CreatesGoogleTaskListsTable() {
        findMigration(51, 52).migrate(connection)
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `google_task_lists`") })
        assertTrue(executedSql.any { it.contains("INSERT INTO `google_task_lists`") })
        assertTrue(executedSql.any { it.contains("DROP TABLE IF EXISTS `store`") })
    }

    // --- MIGRATION_52_53 ---
    @Test
    fun migration52To53RenamesToTempTables() {
        findMigration(52, 53).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `tagdata` RENAME TO") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `userActivity` RENAME TO") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `task_attachments` RENAME TO") })
        assertTrue(executedSql.any { it.contains("DROP TABLE `tagdata-temp`") })
        assertTrue(executedSql.any { it.contains("DROP TABLE `userActivity-temp`") })
        assertTrue(executedSql.any { it.contains("DROP TABLE `task_attachments-temp`") })
    }

    // --- MIGRATION_54_58 ---
    @Test
    fun migration54To58CreatesCaldavTables() {
        findMigration(54, 58).migrate(connection)
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `caldav_account`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `caldav_calendar`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `caldav_tasks`") })
    }

    // --- MIGRATION_58_59 ---
    @Test
    fun migration58To59AddsAccountColumns() {
        findMigration(58, 59).migrate(connection)
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `google_task_accounts`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `google_task_lists` ADD COLUMN `account`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `caldav_account` ADD COLUMN `error`") })
    }

    // --- MIGRATION_59_60 ---
    @Test
    fun migration59To60AddsLocationFields() {
        findMigration(59, 60).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `locations` ADD COLUMN `address`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `locations` ADD COLUMN `phone`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `locations` ADD COLUMN `url`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `locations` ADD COLUMN `arrival`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `locations` ADD COLUMN `departure`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `notification` ADD COLUMN `location`") })
    }

    // --- MIGRATION_60_61 ---
    @Test
    fun migration60To61CreatesPlacesAndGeofences() {
        findMigration(60, 61).migrate(connection)
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `places`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `geofences`") })
        assertTrue(executedSql.any { it.contains("INSERT INTO `places`") })
        assertTrue(executedSql.any { it.contains("INSERT INTO `geofences`") })
        assertTrue(executedSql.any { it.contains("DROP TABLE `locations`") })
    }

    // --- MIGRATION_61_62 ---
    @Test
    fun migration61To62AddsEtag() {
        findMigration(61, 62).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `google_task_accounts` ADD COLUMN `etag`") })
    }

    // --- MIGRATION_64_65 ---
    @Test
    fun migration64To65AddsParentColumns() {
        findMigration(64, 65).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `caldav_tasks` ADD COLUMN `cd_parent`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `caldav_tasks` ADD COLUMN `cd_remote_parent`") })
    }

    // --- MIGRATION_65_66 ---
    @Test
    fun migration65To66CreatesIndexes() {
        findMigration(65, 66).migrate(connection)
        assertTrue(executedSql.any { it.contains("CREATE UNIQUE INDEX `place_uid`") })
        assertTrue(executedSql.any { it.contains("CREATE INDEX `geo_task`") })
        assertTrue(executedSql.any { it.contains("CREATE INDEX `tag_task`") })
        assertTrue(executedSql.any { it.contains("CREATE INDEX `gt_list_parent`") })
        assertTrue(executedSql.any { it.contains("CREATE INDEX `gt_task`") })
        assertTrue(executedSql.any { it.contains("CREATE INDEX `cd_calendar_parent`") })
        assertTrue(executedSql.any { it.contains("CREATE INDEX `cd_task`") })
    }

    // --- MIGRATION_66_67 ---
    @Test
    fun migration66To67AddsRepeatColumn() {
        findMigration(66, 67).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `caldav_accounts` ADD COLUMN `cda_repeat`") })
    }

    // --- MIGRATION_67_68 ---
    @Test
    fun migration67To68CreatesActiveIndex() {
        findMigration(67, 68).migrate(connection)
        assertTrue(executedSql.any { it.contains("CREATE INDEX `active_and_visible`") })
    }

    // --- MIGRATION_68_69 ---
    @Test
    fun migration68To69AddsCollapsedColumn() {
        findMigration(68, 69).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `tasks` ADD COLUMN `collapsed`") })
    }

    // --- MIGRATION_69_70 ---
    @Test
    fun migration69To70AddsParentColumns() {
        findMigration(69, 70).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `tasks` ADD COLUMN `parent`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `tasks` ADD COLUMN `parent_uuid`") })
    }

    // --- MIGRATION_70_71 ---
    @Test
    fun migration70To71AddsEncryptionAndAccountType() {
        findMigration(70, 71).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `caldav_accounts` ADD COLUMN `cda_encryption_key`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `caldav_accounts` ADD COLUMN `cda_account_type`") })
    }

    // --- MIGRATION_71_72 ---
    @Test
    fun migration71To72AddsCollapsedColumns() {
        findMigration(71, 72).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `caldav_accounts` ADD COLUMN `cda_collapsed`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `google_task_accounts` ADD COLUMN `gta_collapsed`") })
    }

    // --- MIGRATION_72_73 ---
    @Test
    fun migration72To73AddsPlaceColorAndIcon() {
        findMigration(72, 73).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `places` ADD COLUMN `place_color`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `places` ADD COLUMN `place_icon`") })
    }

    // --- MIGRATION_74_75 ---
    @Test
    fun migration74To75AddsCdOrder() {
        findMigration(74, 75).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `caldav_tasks` ADD COLUMN `cd_order`") })
    }

    // --- MIGRATION_76_77 ---
    @Test
    fun migration76To77AddsCdlAccess() {
        findMigration(76, 77).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `caldav_lists` ADD COLUMN `cdl_access`") })
    }

    // --- MIGRATION_78_79 ---
    @Test
    fun migration78To79AddsCdaServerType() {
        findMigration(78, 79).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `caldav_accounts` ADD COLUMN `cda_server_type`") })
    }

    // --- MIGRATION_79_80 ---
    @Test
    fun migration79To80RecreatesPrincipalsTable() {
        findMigration(79, 80).migrate(connection)
        assertTrue(executedSql.any { it.contains("DROP TABLE `principals`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `principals`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `principal_access`") })
    }

    // --- MIGRATION_80_81 ---
    @Test
    fun migration80To81AddsAlarmColumnsAndInsertsAlarms() {
        findMigration(80, 81).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `alarms` ADD COLUMN `type`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `alarms` ADD COLUMN `repeat`") })
        assertTrue(executedSql.any { it.contains("ALTER TABLE `alarms` ADD COLUMN `interval`") })
        // Should insert alarms for various notification types
        val insertCount = executedSql.count { it.startsWith("INSERT INTO `alarms`") }
        assertTrue("Expected multiple alarm inserts, got $insertCount", insertCount >= 5)
    }

    // --- MIGRATION_82_83 ---
    @Test
    fun migration82To83AddsRadiusAndRecreatesTables() {
        findMigration(82, 83).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `places` ADD COLUMN `radius`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `_new_alarms`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `_new_google_tasks`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `_new_tags`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `_new_notification`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `_new_caldav_tasks`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `_new_geofences`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `_new_task_list_metadata`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `_new_tasks`") })
    }

    // --- MIGRATION_85_86 ---
    @Test
    fun migration85To86CreatesAttachmentTables() {
        findMigration(85, 86).migrate(connection)
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `attachment_file`") })
        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `attachment`") })
        assertTrue(executedSql.any { it.contains("DROP TABLE `task_attachments`") })
    }

    // --- MIGRATION_86_87 ---
    @Test
    fun migration86To87AddsReadOnlyColumn() {
        findMigration(86, 87).migrate(connection)
        assertTrue(executedSql.any { it.contains("ALTER TABLE `tasks` ADD COLUMN `read_only`") })
        assertTrue(executedSql.any { it.contains("UPDATE `tasks` SET `read_only` = 1") })
    }

    // --- MIGRATION_89_90 ---
    @Test
    fun migration89To90CreatesCalDavIndexes() {
        findMigration(89, 90).migrate(connection)
        assertTrue(executedSql.any { it.contains("CREATE INDEX IF NOT EXISTS `index_caldav_tasks_cd_remote_id`") })
        assertTrue(executedSql.any { it.contains("CREATE INDEX IF NOT EXISTS `index_caldav_tasks_cd_calendar_cd_remote_id`") })
        assertTrue(executedSql.any { it.contains("CREATE INDEX IF NOT EXISTS `index_caldav_tasks_cd_calendar_cd_remote_parent`") })
    }

    // --- MIGRATION_90_91 ---
    @Test
    fun migration90To91CreatesParentIndexAndCleansUp() {
        findMigration(90, 91).migrate(connection)
        assertTrue(executedSql.any { it.contains("CREATE INDEX IF NOT EXISTS `index_tasks_parent`") })
        assertTrue(executedSql.any { it.contains("DELETE FROM `caldav_tasks`") })
        assertTrue(executedSql.any { it.contains("CREATE INDEX IF NOT EXISTS `index_caldav_tasks_cd_calendar_cd_object`") })
    }

    // --- migrations() returns all expected migrations ---
    @Test
    fun migrationsArrayIsNotEmpty() {
        assertTrue("Expected migrations array to be non-empty", getMigrations().isNotEmpty())
    }

    @Test
    fun migrationsArrayContainsExpectedCount() {
        val migrations = getMigrations()
        assertTrue("Expected at least 40 migrations, got ${migrations.size}", migrations.size >= 40)
    }

    @Test
    fun migrationsStartVersionsAreOrderedNonDecreasing() {
        val migrations = getMigrations()
        for (i in 1 until migrations.size) {
            assertTrue(
                "Migration at index $i (${migrations[i].startVersion} -> ${migrations[i].endVersion}) " +
                        "should have startVersion >= previous (${migrations[i - 1].startVersion} -> ${migrations[i - 1].endVersion})",
                migrations[i].startVersion >= migrations[i - 1].startVersion
            )
        }
    }

    @Test
    fun allMigrationsHaveEndVersionGreaterThanStartVersion() {
        getMigrations().forEach { migration ->
            assertTrue(
                "Migration ${migration.startVersion} -> ${migration.endVersion} should have endVersion > startVersion",
                migration.endVersion > migration.startVersion
            )
        }
    }

    @Test
    fun firstMigrationStartsAt35() {
        assertEquals(35, getMigrations().first().startVersion)
    }

    @Test
    fun lastMigrationEndsAt91() {
        assertEquals(91, getMigrations().last().endVersion)
    }

    // --- SQL statement counting ---
    @Test
    fun migration35To36ExecutesExactlyOneSql() {
        findMigration(35, 36).migrate(connection)
        assertEquals(1, executedSql.size)
    }

    @Test
    fun migration38To39ExecutesExactlyTwoSql() {
        findMigration(38, 39).migrate(connection)
        assertEquals(2, executedSql.size)
    }

    @Test
    fun migration46To47ExecutesExactlyThreeSql() {
        findMigration(46, 47).migrate(connection)
        assertEquals(3, executedSql.size)
    }

    @Test
    fun migration54To58ExecutesExactlyThreeSql() {
        findMigration(54, 58).migrate(connection)
        assertEquals(3, executedSql.size)
    }

    @Test
    fun migration59To60ExecutesSixSql() {
        findMigration(59, 60).migrate(connection)
        assertEquals(6, executedSql.size)
    }

    @Test
    fun migration65To66ExecutesSevenSql() {
        findMigration(65, 66).migrate(connection)
        assertEquals(7, executedSql.size)
    }
}
