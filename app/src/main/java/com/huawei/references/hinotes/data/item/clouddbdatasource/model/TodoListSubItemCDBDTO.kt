package com.huawei.references.hinotes.data.item.model

import com.huawei.agconnect.cloud.database.CloudDBZoneObject
import java.util.*

data class TodoListSubItemCDBDTO(val itemId:Int,
                                 val createdAt:Date?,
                                 val updatedAt: Date?,
                                 val title:String,
                                 val isChecked:Boolean
) : CloudDBZoneObject()