package com.angylabs.mydcabtconor.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CryptoDbHelper(private val ctx: Context) : SQLiteOpenHelper(
    ctx, DB_FILE_NAME, null, 1
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS dca_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha TEXT NOT NULL,
                mxn_gastado REAL NOT NULL,
                tipo_cambio_mxn_usd REAL NOT NULL,
                usd_equivalente REAL NOT NULL,
                btc_adquirido REAL NOT NULL,
                precio_compra_btc_usd REAL NOT NULL,
                moneda_gasto TEXT,
                notas TEXT
            )"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS fee_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha TEXT NOT NULL,
                btc_fee REAL NOT NULL,
                precio_btc_usd REAL NOT NULL,
                usd_fee REAL NOT NULL,
                tipo_movimiento TEXT NOT NULL,
                notas TEXT
            )"""
        )
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS sol_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fecha TEXT NOT NULL,
                mxn_gastado REAL NOT NULL,
                tipo_cambio_mxn_usd REAL NOT NULL,
                usd_equivalente REAL NOT NULL,
                sol_adquirido REAL NOT NULL,
                precio_compra_sol_usd REAL NOT NULL,
                moneda_gasto TEXT,
                notas TEXT
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {}
}

data class BtcRecord(
    val id: Long,
    val fecha: String,
    val mxnGastado: Double,
    val tipoCambio: Double,
    val usdEquivalente: Double,
    val btcAdquirido: Double,
    val precioBtcUsd: Double,
    val monedaGasto: String?,
    val notas: String?
)

data class SolRecord(
    val id: Long,
    val fecha: String,
    val mxnGastado: Double,
    val tipoCambio: Double,
    val usdEquivalente: Double,
    val solAdquirido: Double,
    val precioSolUsd: Double,
    val monedaGasto: String?,
    val notas: String?
)

data class ImportStats(val btc: Int, val sol: Int, val fees: Int)

data class FeeRecord(
    val id: Long,
    val fecha: String,
    val btcFee: Double,
    val precioBtcUsd: Double,
    val usdFee: Double,
    val tipoMovimiento: String,
    val notas: String?
)

object Repo {
    @Volatile private var helper: CryptoDbHelper? = null
    fun db(ctx: Context): SQLiteDatabase {
        val h = helper ?: synchronized(this) {
            helper ?: CryptoDbHelper(ctx.applicationContext).also { helper = it }
        }
        return h.writableDatabase
    }

    fun closeAndReset() {
        synchronized(this) {
            helper?.close()
            helper = null
        }
    }

    fun fechaAhora(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("America/Mexico_City")
        return fmt.format(Date())
    }

    fun insertBtc(ctx: Context, r: BtcRecord): Long {
        val cv = ContentValues().apply {
            put("fecha", r.fecha)
            put("mxn_gastado", r.mxnGastado)
            put("tipo_cambio_mxn_usd", r.tipoCambio)
            put("usd_equivalente", r.usdEquivalente)
            put("btc_adquirido", r.btcAdquirido)
            put("precio_compra_btc_usd", r.precioBtcUsd)
            put("moneda_gasto", r.monedaGasto)
            put("notas", r.notas)
        }
        return db(ctx).insert("dca_records", null, cv)
    }

    fun updateBtc(ctx: Context, r: BtcRecord): Int {
        val cv = ContentValues().apply {
            put("mxn_gastado", r.mxnGastado)
            put("tipo_cambio_mxn_usd", r.tipoCambio)
            put("usd_equivalente", r.usdEquivalente)
            put("btc_adquirido", r.btcAdquirido)
            put("precio_compra_btc_usd", r.precioBtcUsd)
            put("moneda_gasto", r.monedaGasto)
            put("notas", r.notas)
        }
        return db(ctx).update("dca_records", cv, "id=?", arrayOf(r.id.toString()))
    }

    fun deleteBtc(ctx: Context, id: Long) {
        db(ctx).delete("dca_records", "id=?", arrayOf(id.toString()))
    }

    fun listBtc(ctx: Context): List<BtcRecord> {
        val out = mutableListOf<BtcRecord>()
        db(ctx).rawQuery(
            "SELECT id,fecha,mxn_gastado,tipo_cambio_mxn_usd,usd_equivalente,btc_adquirido,precio_compra_btc_usd,moneda_gasto,notas FROM dca_records ORDER BY fecha DESC",
            null
        ).use { c ->
            while (c.moveToNext()) out += BtcRecord(
                c.getLong(0), c.getString(1), c.getDouble(2), c.getDouble(3),
                c.getDouble(4), c.getDouble(5), c.getDouble(6),
                if (c.isNull(7)) null else c.getString(7),
                if (c.isNull(8)) null else c.getString(8)
            )
        }
        return out
    }

    fun insertSol(ctx: Context, r: SolRecord): Long {
        val cv = ContentValues().apply {
            put("fecha", r.fecha)
            put("mxn_gastado", r.mxnGastado)
            put("tipo_cambio_mxn_usd", r.tipoCambio)
            put("usd_equivalente", r.usdEquivalente)
            put("sol_adquirido", r.solAdquirido)
            put("precio_compra_sol_usd", r.precioSolUsd)
            put("moneda_gasto", r.monedaGasto)
            put("notas", r.notas)
        }
        return db(ctx).insert("sol_records", null, cv)
    }

    fun deleteSol(ctx: Context, id: Long) {
        db(ctx).delete("sol_records", "id=?", arrayOf(id.toString()))
    }

    fun listSol(ctx: Context): List<SolRecord> {
        val out = mutableListOf<SolRecord>()
        db(ctx).rawQuery(
            "SELECT id,fecha,mxn_gastado,tipo_cambio_mxn_usd,usd_equivalente,sol_adquirido,precio_compra_sol_usd,moneda_gasto,notas FROM sol_records ORDER BY fecha DESC",
            null
        ).use { c ->
            while (c.moveToNext()) out += SolRecord(
                c.getLong(0), c.getString(1), c.getDouble(2), c.getDouble(3),
                c.getDouble(4), c.getDouble(5), c.getDouble(6),
                if (c.isNull(7)) null else c.getString(7),
                if (c.isNull(8)) null else c.getString(8)
            )
        }
        return out
    }

    fun insertFee(ctx: Context, r: FeeRecord): Long {
        val cv = ContentValues().apply {
            put("fecha", r.fecha)
            put("btc_fee", r.btcFee)
            put("precio_btc_usd", r.precioBtcUsd)
            put("usd_fee", r.usdFee)
            put("tipo_movimiento", r.tipoMovimiento)
            put("notas", r.notas)
        }
        return db(ctx).insert("fee_records", null, cv)
    }

    fun deleteFee(ctx: Context, id: Long) {
        db(ctx).delete("fee_records", "id=?", arrayOf(id.toString()))
    }

    fun listFees(ctx: Context): List<FeeRecord> {
        val out = mutableListOf<FeeRecord>()
        db(ctx).rawQuery(
            "SELECT id,fecha,btc_fee,precio_btc_usd,usd_fee,tipo_movimiento,notas FROM fee_records ORDER BY fecha DESC",
            null
        ).use { c ->
            while (c.moveToNext()) out += FeeRecord(
                c.getLong(0), c.getString(1), c.getDouble(2),
                c.getDouble(3), c.getDouble(4), c.getString(5),
                if (c.isNull(6)) null else c.getString(6)
            )
        }
        return out
    }

    fun bytes(ctx: Context): ByteArray {
        db(ctx).rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
        closeAndReset()
        val f = ctx.getDatabasePath(DB_FILE_NAME)
        return if (f.exists()) f.readBytes() else ByteArray(0)
    }

    fun replaceFromBytes(ctx: Context, data: ByteArray): Result<ImportStats> = runCatching {
        closeAndReset()
        val tmp = File(ctx.cacheDir, "$DB_FILE_NAME.tmp")
        tmp.writeBytes(data)
        val test = SQLiteDatabase.openDatabase(tmp.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        val req = setOf("dca_records", "fee_records", "sol_records")
        val have = mutableSetOf<String>()
        test.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { c ->
            while (c.moveToNext()) have += c.getString(0)
        }
        if (!have.containsAll(req)) {
            test.close()
            tmp.delete()
            throw RuntimeException("Archivo invalido: faltan tablas")
        }
        fun count(t: String): Int =
            test.rawQuery("SELECT COUNT(*) FROM $t", null).use { c -> c.moveToFirst(); c.getInt(0) }
        val stats = ImportStats(count("dca_records"), count("sol_records"), count("fee_records"))
        test.close()
        val dest = ctx.getDatabasePath(DB_FILE_NAME)
        dest.parentFile?.mkdirs()
        if (dest.exists()) {
            File(dest.parentFile, "$DB_FILE_NAME.bak_${System.currentTimeMillis()}").writeBytes(dest.readBytes())
        }
        File(dest.parentFile, "$DB_FILE_NAME-wal").delete()
        File(dest.parentFile, "$DB_FILE_NAME-shm").delete()
        File(dest.parentFile, "$DB_FILE_NAME-journal").delete()
        tmp.copyTo(dest, overwrite = true)
        tmp.delete()
        stats
    }
}
