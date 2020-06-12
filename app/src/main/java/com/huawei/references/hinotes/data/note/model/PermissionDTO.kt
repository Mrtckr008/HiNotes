package com.huawei.references.hinotes.data.note.model

import com.huawei.agconnect.cloud.database.CloudDBZoneObject

class PermissionDTO(val itemId:Int,
                    val userId:String,
                    val role:Int) : CloudDBZoneObject() {
}