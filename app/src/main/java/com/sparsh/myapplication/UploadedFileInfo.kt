package com.sparsh.myapplication

import org.json.JSONObject

data class UploadedFileInfo(
    val id: String,
    val filename: String,
    val originalName: String,
    val uploadDate: String,
    val filePath: String,
    val startDate: String? = null,
    val endDate: String? = null
) {
    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("filename", filename)
        json.put("originalName", originalName)
        json.put("uploadDate", uploadDate)
        json.put("filePath", filePath)
        json.put("startDate", startDate ?: "")
        json.put("endDate", endDate ?: "")
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): UploadedFileInfo {
            return UploadedFileInfo(
                id = json.getString("id"),
                filename = json.getString("filename"),
                originalName = json.getString("originalName"),
                uploadDate = json.getString("uploadDate"),
                filePath = json.getString("filePath"),
                startDate = json.optString("startDate", "").ifEmpty { null },
                endDate = json.optString("endDate", "").ifEmpty { null }
            )
        }
    }
}
