package com.marcogn.kartlog.di

import android.content.Context
import androidx.room.Room
import com.marcogn.kartlog.data.local.DATABASE_NAME
import com.marcogn.kartlog.data.local.KartLogDatabase
import com.marcogn.kartlog.data.local.MIGRATION_1_2
import com.marcogn.kartlog.data.local.MIGRATION_2_3
import com.marcogn.kartlog.data.local.dao.BackupDao
import com.marcogn.kartlog.data.local.dao.ConsigliamiDao
import com.marcogn.kartlog.data.local.dao.MedallionsDao
import com.marcogn.kartlog.data.local.dao.PSwitchesDao
import com.marcogn.kartlog.data.local.dao.SeedDao
import com.marcogn.kartlog.data.local.dao.SeedMetaDao
import com.marcogn.kartlog.data.local.dao.SkinDao
import com.marcogn.kartlog.data.local.dao.UserStateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KartLogDatabase =
        Room.databaseBuilder(context, KartLogDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides
    fun provideSeedDao(database: KartLogDatabase): SeedDao = database.seedDao()

    @Provides
    fun provideSeedMetaDao(database: KartLogDatabase): SeedMetaDao = database.seedMetaDao()

    @Provides
    fun provideUserStateDao(database: KartLogDatabase): UserStateDao = database.userStateDao()

    @Provides
    fun provideSkinDao(database: KartLogDatabase): SkinDao = database.skinDao()

    @Provides
    fun provideMedallionsDao(database: KartLogDatabase): MedallionsDao = database.medallionsDao()

    @Provides
    fun providePSwitchesDao(database: KartLogDatabase): PSwitchesDao = database.pSwitchesDao()

    @Provides
    fun provideConsigliamiDao(database: KartLogDatabase): ConsigliamiDao = database.consigliamiDao()

    @Provides
    fun provideBackupDao(database: KartLogDatabase): BackupDao = database.backupDao()
}
