package com.example.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String, // e.g. CAM-20261007-0001
    val name: String,
    val clientName: String,
    val date: String,
    val location: String,
    val description: String,
    val notes: String,
    val status: String,
    val createdTimestamp: Long
)

@Entity(tableName = "event_folders")
data class EventFolderEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val name: String,
    val createdTimestamp: Long
)

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val folderId: String,
    val folderName: String,
    val cameraId: String,
    val cameraModel: String,
    val filename: String,
    val originalFilename: String,
    val fileSizeFormatted: String,
    val fileSizeBytes: Long,
    val captureTimestamp: Long,
    val localFilePath: String,
    val mimeType: String,
    val sourceName: String,
    val transferStatusName: String,
    val fileChecksum: String,
    val iso: String,
    val shutterSpeed: String,
    val aperture: String,
    val focalLength: String
)

@Entity(tableName = "saved_cameras")
data class SavedCameraEntity(
    @PrimaryKey val id: String,
    val brandName: String,
    val model: String,
    val connectionTypeName: String,
    val ipAddress: String,
    val port: Int,
    val ftpUsername: String,
    val ftpPath: String,
    val isAutoReconnect: Boolean,
    val lastConnectedTime: Long
)

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY createdTimestamp DESC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: String): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEvent(id: String)
}

@Dao
interface EventFolderDao {
    @Query("SELECT * FROM event_folders WHERE eventId = :eventId ORDER BY createdTimestamp ASC")
    fun getFoldersForEvent(eventId: String): Flow<List<EventFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: EventFolderEntity)

    @Query("UPDATE event_folders SET name = :newName WHERE id = :id")
    suspend fun renameFolder(id: String, newName: String)

    @Query("DELETE FROM event_folders WHERE id = :id")
    suspend fun deleteFolder(id: String)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY captureTimestamp DESC")
    fun getAllPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE eventId = :eventId ORDER BY captureTimestamp DESC")
    fun getPhotosForEvent(eventId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE folderId = :folderId ORDER BY captureTimestamp DESC")
    fun getPhotosForFolder(folderId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE fileChecksum = :checksum LIMIT 1")
    suspend fun findPhotoByChecksum(checksum: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE filename = :filename AND fileSizeBytes = :size LIMIT 1")
    suspend fun findPhotoByNameAndSize(filename: String, size: Long): PhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoEntity)

    @Query("UPDATE photos SET folderId = :newFolderId, folderName = :newFolderName WHERE id = :photoId")
    suspend fun movePhotoToFolder(photoId: String, newFolderId: String, newFolderName: String)

    @Query("DELETE FROM photos WHERE id = :photoId")
    suspend fun deletePhoto(photoId: String)
}

@Dao
interface CameraDao {
    @Query("SELECT * FROM saved_cameras ORDER BY lastConnectedTime DESC")
    fun getAllCameras(): Flow<List<SavedCameraEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCamera(camera: SavedCameraEntity)

    @Query("DELETE FROM saved_cameras WHERE id = :cameraId")
    suspend fun deleteCamera(cameraId: String)
}

@Database(
    entities = [
        EventEntity::class,
        EventFolderEntity::class,
        PhotoEntity::class,
        SavedCameraEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun eventFolderDao(): EventFolderDao
    abstract fun photoDao(): PhotoDao
    abstract fun cameraDao(): CameraDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "camdrop_pro_v2.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
