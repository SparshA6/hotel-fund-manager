package com.sparsh.myapplication

import org.json.JSONObject

data class UploadedFileInfo(
    val id: String,
    val filename: String,
    val originalName: String,
    val uploadDate: String,
    val filePath: String
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("filename", filename)
        json.put("originalName", originalName)
        json.put("uploadDate", uploadDate)
        json.put("filePath", filePath)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): UploadedFileInfo {
            return UploadedFileInfo(
                id = json.getString("id"),
                filename = json.getString("filename"),
                originalName = json.getString("originalName"),
                uploadDate = json.getString("uploadDate"),
                filePath = json.getString("filePath")
            )
        }
    }
}
