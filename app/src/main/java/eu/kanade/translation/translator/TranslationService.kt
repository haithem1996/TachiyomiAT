package eu.kanade.tachiyomi.translate

import android.content.Context
import android.icu.text.ArabicShaping
import android.icu.text.Bidi
import android.os.Build
import android.text.Html
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.IOException
import java.util.*

// --- DATABASE CACHE ---

@Entity(
    tableName = "translation_cache",
    primaryKeys = ["sourceText","sourceLang","targetLang"]
)
data class TranslationCache(
    val sourceText: String,
    val sourceLang: String,
    val targetLang: String,
    val translatedText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TranslationCacheDao {
    @Query("SELECT translatedText FROM translation_cache WHERE sourceText = :s AND sourceLang = :sl AND targetLang = :tl")
    suspend fun getCached(s: String, sl: String, tl: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(cache: TranslationCache)
}

@Database(entities = [TranslationCache::class], version = 1)
abstract class TranslationCacheDb : RoomDatabase() {
    abstract fun dao(): TranslationCacheDao

    companion object {
        @Volatile private var INSTANCE: TranslationCacheDb? = null
        fun getInstance(context: Context): TranslationCacheDb {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TranslationCacheDb::class.java, "translation_cache.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

// --- TRANSLATION SERVICE ---

class TranslationService(private val context: Context, private val apiKey: String) {

    private val cacheDao = TranslationCacheDb.getInstance(context).dao()
    private val client = OkHttpClient()

    suspend fun translate(text: String, sourceLang: String = "auto", targetLang: String = "ar"): String {
        val cached = cacheDao.getCached(text, sourceLang, targetLang)
        if (cached != null) return cached

        val translated = translateWithGoogle(text, targetLang)

        cacheDao.put(TranslationCache(text, sourceLang, targetLang, translated))

        return translated
    }

    private suspend fun translateWithGoogle(text: String, targetLang: String): String {
        val form = FormBody.Builder()
            .add("q", text)
            .add("target", targetLang)
            .add("format", "text")
            .build()

        val request = Request.Builder()
            .url("https://translation.googleapis.com/language/translate/v2?key=$apiKey")
            .post(form)
            .build()

        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("Translation failed: ${resp.code}")
                val json = resp.body!!.string()
                val translated = Regex("\"translatedText\"\\s*:\\s*\"([^\"]+)\"")
                    .find(json)?.groupValues?.get(1)
                    ?: throw IOException("Failed to parse translation")
                Html.fromHtml(translated, Html.FROM_HTML_MODE_LEGACY).toString()
            }
        }
    }

    companion object {
        fun shapeArabic(text: String): String {
            return try {
                val shaped = ArabicShaping(ArabicShaping.LETTERS_SHAPE).shape(text)
                val bidi = Bidi(shaped, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT)
                bidi.writeReordered(Bidi.REORDER_DEFAULT)
            } catch (e: Throwable) {
                text
            }
        }
    }
}
