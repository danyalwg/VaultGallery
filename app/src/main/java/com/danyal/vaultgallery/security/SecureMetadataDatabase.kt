package com.danyal.vaultgallery.security

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Entity(tableName = "secure_media", primaryKeys = ["id"])
internal data class SecureItemEntity(
    val id: String,
    val name: String,
    val mimeType: String,
    val dateTakenMs: Long,
    val sizeBytes: Long,
    val albumName: String,
    val width: Int,
    val height: Int,
    val durationMs: Long,
    val isFavourite: Boolean,
    val isTrashed: Boolean,
    val trashedAtMs: Long,
    val tag: String,
    val latitude: Double?,
    val longitude: Double?,
    val storagePolicy: String,
    val rating: Int,
)

@Dao
internal interface SecureItemDao {
    @Query("SELECT * FROM secure_media ORDER BY dateTakenMs DESC")
    fun all(): List<SecureItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(items: List<SecureItemEntity>)

    @Query("DELETE FROM secure_media")
    fun clear()

    @Transaction
    fun replace(items: List<SecureItemEntity>) {
        clear()
        if (items.isNotEmpty()) insert(items)
    }
}

@Database(entities = [SecureItemEntity::class], version = 4, exportSchema = false)
internal abstract class SecureMetadataDatabase : RoomDatabase() {
    abstract fun items(): SecureItemDao

    companion object {
        const val NAME = "secure-gallery-metadata.db"

        fun open(
            context: Context,
            passphrase: ByteArray,
            databaseName: String = NAME,
        ): SecureMetadataDatabase {
            System.loadLibrary("sqlcipher")
            return Room.databaseBuilder(context, SecureMetadataDatabase::class.java, databaseName)
                // SQLCipher may reopen the helper during a long WorkManager job. Keep this
                // caller-owned passphrase alive until SecureVault explicitly closes the database.
                .openHelperFactory(SupportOpenHelperFactory(passphrase, null, false))
                .addMigrations(object : Migration(1, 2) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE secure_media ADD COLUMN tag TEXT NOT NULL DEFAULT ''")
                        db.execSQL("ALTER TABLE secure_media ADD COLUMN latitude REAL")
                        db.execSQL("ALTER TABLE secure_media ADD COLUMN longitude REAL")
                    }
                })
                .addMigrations(object : Migration(2, 3) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE secure_media ADD COLUMN storagePolicy TEXT NOT NULL DEFAULT 'ENCRYPTED'")
                    }
                })
                .addMigrations(object : Migration(3, 4) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE secure_media ADD COLUMN rating INTEGER NOT NULL DEFAULT 0")
                    }
                })
                .build()
        }
    }
}

internal fun SecureItem.toEntity() = SecureItemEntity(
    id, name, mimeType, dateTakenMs, sizeBytes, albumName, width, height, durationMs,
    isFavourite, isTrashed, trashedAtMs, tag, latitude, longitude, storagePolicy.name, rating,
)

internal fun SecureItemEntity.toItem() = SecureItem(
    id, name, mimeType, dateTakenMs, sizeBytes, albumName, width, height, durationMs,
    isFavourite, isTrashed, trashedAtMs, tag, latitude, longitude,
    runCatching { SecureStoragePolicy.valueOf(storagePolicy) }.getOrDefault(SecureStoragePolicy.ENCRYPTED), rating.coerceIn(0, 5),
)
