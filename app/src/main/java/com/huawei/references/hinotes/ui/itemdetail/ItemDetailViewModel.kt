package com.huawei.references.hinotes.ui.itemdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.huawei.references.hinotes.data.base.DataHolder
import com.huawei.references.hinotes.data.item.ItemRepository
import com.huawei.references.hinotes.data.item.ReminderRepository
import com.huawei.references.hinotes.data.item.model.Item
import com.huawei.references.hinotes.data.item.model.ItemSaveResult
import com.huawei.references.hinotes.data.item.model.Reminder
import com.huawei.references.hinotes.ui.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class ItemDetailViewModel(private val itemRepository: ItemRepository,
                                   private val reminderRepository: ReminderRepository
) : BaseViewModel(){

    private val _deleteItemLiveData = MutableLiveData<DataHolder<Any>>()
    val deleteItemLiveData : LiveData<DataHolder<Any>>
        get() = _deleteItemLiveData

    private val _saveItemLiveData = MutableLiveData<DataHolder<ItemSaveResult>>()
    val saveItemLiveData : LiveData<DataHolder<ItemSaveResult>>
        get() = _saveItemLiveData

    private val _reminderLiveData = MutableLiveData<DataHolder<List<Reminder>>>()
    val reminderLiveData : LiveData<DataHolder<List<Reminder>>>
        get() = _reminderLiveData

    fun getReminders(itemId:Int){
        _reminderLiveData.value=DataHolder.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            _reminderLiveData.postValue(reminderRepository.getRemindersByItemId(itemId))
        }
    }

    fun saveItem(item: Item, userId:String, isNew:Boolean){
        _saveItemLiveData.value=DataHolder.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            _saveItemLiveData.postValue(itemRepository.upsertItem(item,userId,isNew))
        }
    }

    fun deleteItem(item: Item, userId:String){
        _deleteItemLiveData.value=DataHolder.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            _deleteItemLiveData.postValue(itemRepository.deleteItem(item,userId))
        }
    }

}