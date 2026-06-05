@file:OptIn(ExperimentalWasmJsInterop::class)

package com.wojtek.holds.database

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import web.events.EventHandler
import web.idb.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js

internal fun parseToJsObject(@Suppress("unused") jsonString: String): JsAny =
    js("JSON.parse(jsonString)")

internal fun stringifyJsObject(@Suppress("unused") jsObject: JsAny): String =
    js("JSON.stringify(jsObject)")

class ProblemRepository(coroutineScope: CoroutineScope) {
    private val dbName = "ProblemDatabase"
    private val storeName = "problems"
    private val dbVersion = 1.0

    private val db: Deferred<IDBDatabase> = coroutineScope.async(start = CoroutineStart.LAZY) { _getDatabase() }

    private suspend fun _getDatabase(): IDBDatabase = suspendCancellableCoroutine { cont ->
        val request = indexedDB.open(dbName, dbVersion)

        request.onupgradeneeded = EventHandler {
            val db = request.result
            if (!db.objectStoreNames.contains(storeName)) {
                db.createObjectStore(storeName)
            }
        }

        request.onsuccess = EventHandler { cont.resume(request.result) }
        request.onerror = EventHandler { cont.resumeWithException(Exception("Failed to open IndexedDB")) }
    }

    suspend fun save(problem: Problem) {
        val db = db.await()

        suspendCancellableCoroutine { cont ->
            val transaction = db.transaction(storeName, IDBTransactionMode.readwrite)
            val store = transaction.objectStore(storeName)

            val jsObject = try {
                problem.toJsAny()
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
            val request = store.put(jsObject, IDBValidKey(problem.id))

            request.onsuccess = EventHandler { cont.resume(Unit) }
            request.onerror = EventHandler { cont.resumeWithException(Exception("Failed to save problem")) }
        }
    }

    suspend fun getById(id: Int): Problem? {
        val db = db.await()

        return suspendCancellableCoroutine { cont ->
            val transaction = db.transaction(storeName, IDBTransactionMode.readonly)
            val store = transaction.objectStore(storeName)
            val request = store.get(IDBValidKey(id))

            request.onsuccess = EventHandler {
                val resultObj = request.result
                if (resultObj != null) {
                    try {
                        cont.resume(resultObj.decode())
                    } catch (e: Exception) {
                        cont.resumeWithException(Exception("Failed to deserialize problem", e))
                    }
                } else {
                    cont.resume(null)
                }
            }
            request.onerror = EventHandler { cont.resumeWithException(Exception("Failed to fetch problem")) }
        }
    }

    suspend fun delete(id: Int) {
        val db = db.await()

        suspendCancellableCoroutine { cont ->
            val transaction = db.transaction(storeName, IDBTransactionMode.readwrite)
            val store = transaction.objectStore(storeName)
            val request = store.delete(IDBValidKey(id))

            request.onsuccess = EventHandler { cont.resume(Unit) }
            request.onerror = EventHandler { cont.resumeWithException(Exception("Failed to delete problem")) }
        }
    }

    fun close() {
        // Only attempt to close if it successfully finished without being cancelled
        if (db.isCompleted && !db.isCancelled) {
            try {
                db.getCompleted().close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        db.cancel()
    }

    private inline fun <reified T> T.toJsAny(): JsAny {
        val jsonString = Json.encodeToString(this)
        return parseToJsObject(jsonString)
    }

    private inline fun <reified T> JsAny.decode(): T {
        val resultString = stringifyJsObject(this)
        return Json.decodeFromString<T>(resultString)
    }
}

@Serializable
data class Problem(
    val id: Int,
    val name: String,
    val version: String,
    val holdsIds: List<Int>,
    val createdAt: Long,
    val updatedAt: Long,
)