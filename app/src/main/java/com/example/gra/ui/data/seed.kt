package com.example.gra.ui.data

import android.content.Context
import android.util.Log
import com.opencsv.CSVReader
import java.io.StringReader
import java.nio.charset.Charset

private const val TAG = "FoodsSeed"

private fun parseDoubleSafe(s: String?): Double? {
    if (s.isNullOrBlank()) return null
    val cleaned = s
        .replace("\u00A0", "")
        .replace("\u2007", "")
        .replace("\u202F", "")
        .replace(",", "")     // 千分位
        .replace("−", "-")    // Unicode 负号
        .trim()
    return cleaned.toDoubleOrNull()
}

/** 识别 BOM；UTF-8 → MS949 → GBK 依次尝试，返回 (CSVReader, 编码名) */
private fun openCsvReaderMultiCharset(context: Context, asset: String): Pair<CSVReader, String> {
    val bytes = context.assets.open(asset).readBytes()
    // BOM
    if (bytes.size >= 3 && bytes[0]==0xEF.toByte() && bytes[1]==0xBB.toByte() && bytes[2]==0xBF.toByte()) {
        return CSVReader(StringReader(bytes.toString(Charsets.UTF_8))) to "UTF-8(BOM)"
    }
    fun tryCs(cs: Charset, name: String): Pair<CSVReader,String>? = try {
        val txt = bytes.toString(cs)
        val head = txt.take(600)
        val badRate = head.count{it=='?'}.toDouble() / head.length.coerceAtLeast(1)
        if (badRate > 0.2) null else CSVReader(StringReader(txt)) to name
    } catch (_: Throwable) { null }
    return tryCs(Charsets.UTF_8, "UTF-8")
        ?: tryCs(Charset.forName("MS949"), "MS949")
        ?: tryCs(Charset.forName("GBK"), "GBK")
        ?: (CSVReader(StringReader(bytes.toString(Charsets.UTF_8))) to "UTF-8(fallback)")
}

/** 只在表为空时从 assets/foods_slim_ko.csv 导入；异常会打详细日志而不是崩溃 */
suspend fun prepopulateFromAssetsIfEmpty(context: Context, db: AppDatabase) {
    val dao = db.foodDao()
    val cnt = kotlin.runCatching { dao.count() }.getOrDefault(0)
    if (cnt > 0) { Log.i(TAG, "DB already populated: $cnt"); return }

    runCatching {
        val (reader, cs) = openCsvReaderMultiCharset(context, "foods_slim_ko.csv")
        Log.i(TAG, "CSV charset: $cs")

        val header = reader.readNext()
        Log.i(TAG, "CSV header: ${header?.joinToString("|")}")

        var lineNo = 1
        val batch = ArrayList<FoodEntity>(400)

        while (true) {
            val row = reader.readNext() ?: break
            lineNo++
            try {
                // 索引必须与 CSV 列顺序一致
                val id       = row.getOrNull(0)?.trim()
                val category = row.getOrNull(1)?.trim().orEmpty()
                val name     = row.getOrNull(2)?.trim().orEmpty()
                val state    = row.getOrNull(3)?.trim().orEmpty()
                val kcal     = parseDoubleSafe(row.getOrNull(5))
                val protein  = parseDoubleSafe(row.getOrNull(6))
                val fat      = parseDoubleSafe(row.getOrNull(7))
                val carb     = parseDoubleSafe(row.getOrNull(8))
                val sugar    = parseDoubleSafe(row.getOrNull(9))
                val synonyms = row.getOrNull(10)

                if (id.isNullOrBlank() || name.isBlank()) {
                    Log.w(TAG, "skip line $lineNo: empty id/name, row=${row.joinToString("|")}")
                    continue
                }

                batch += FoodEntity(
                    id = id, category = category, name = name, state = state, nameLc = name.lowercase(),
                    kcal100g = kcal, protein100g = protein, fat100g = fat, carb100g = carb, sugar100g = sugar,
                    synonyms = synonyms
                )
                if (batch.size >= 400) { dao.upsertAll(batch); batch.clear() }
            } catch (e: Throwable) {
                Log.e(TAG, "line $lineNo failed, row=${row.joinToString("|")}", e)
            }
        }
        if (batch.isNotEmpty()) dao.upsertAll(batch)
        Log.i(TAG, "Seeding finished. rows=${dao.count()}")
    }.onFailure { e ->
        Log.e(TAG, "Seeding failed", e)
    }
}
