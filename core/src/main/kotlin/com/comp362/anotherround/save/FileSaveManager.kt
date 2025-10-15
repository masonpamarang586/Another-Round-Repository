/*
* UPDATED BY MASON PAMARANG 10/15/2025 @ 12:58 AM
* Notes: changed the logic for saving to files that already exist and created the load method
* made "save" return true or false as opposed to an int like it was before
 */

package com.comp362.anotherround.save


import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.SerializationException

data class GameState( // UPDATE THESE
    val level: Int,
    val playerHp: Int,
    val enemyHp: Int,
    val score: Int,
    val savedAt: Long = System.currentTimeMillis()
)

object FileSaveManager {
    private const val SAVE_NAME = "savegame.json"
    private val json = Json().apply {
        setOutputType(com.badlogic.gdx.utils.JsonWriter.OutputType.json) // compact JSON
    }

    private fun saveFile(): FileHandle = Gdx.files.local(SAVE_NAME)

    fun save(state: GameState): Boolean { // SAVE THE GAME. TRUE OR FALSE FOR SUCCESS
        return try {
            val tmp = Gdx.files.local("$SAVE_NAME.tmp")
            tmp.writeString(json.toJson(state), false, "UTF-8")
            tmp.moveTo(saveFile())   // REPLACES IT IF IT ALREADY EXISTS
            true
        } catch (t: Throwable) {
            Gdx.app.error("SAVE", "Failed to save", t)
            false
        }
    }

    fun load(): GameState? { // RETURN THE PREVIOUSLY SAVED GAMESTATE. CAN BE NULL
        val fh = saveFile()
        if (!fh.exists()) return null
        return try {
            json.fromJson(GameState::class.java, fh)
        } catch (e: SerializationException) {
            Gdx.app.error("SAVE", "Corrupt save file", e)
            null
        } catch (t: Throwable) {
            Gdx.app.error("SAVE", "Failed to load", t)
            null
        }
    }

    fun hasSave(): Boolean = saveFile().exists()
    fun delete(): Boolean = runCatching { saveFile().delete() }.isSuccess
}
