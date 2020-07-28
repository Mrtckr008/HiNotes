package com.huawei.references.hinotes.ui.itemdetail

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.huawei.agconnect.auth.AGConnectUser
import com.huawei.hmf.tasks.Task
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.mlplugin.asr.MLAsrCaptureActivity
import com.huawei.hms.mlplugin.asr.MLAsrCaptureConstants
import com.huawei.hms.mlsdk.MLAnalyzerFactory
import com.huawei.hms.mlsdk.common.MLFrame
import com.huawei.hms.mlsdk.text.MLLocalTextSetting
import com.huawei.hms.mlsdk.text.MLText
import com.huawei.hms.site.api.model.Site
import com.huawei.references.hinotes.R
import com.huawei.references.hinotes.data.base.DataHolder
import com.huawei.references.hinotes.data.base.NoRecordFoundError
import com.huawei.references.hinotes.data.item.model.Item
import com.huawei.references.hinotes.data.item.model.Reminder
import com.huawei.references.hinotes.data.item.model.ReminderType
import com.huawei.references.hinotes.ui.base.*
import com.huawei.references.hinotes.ui.itemdetail.reminder.MapType
import com.huawei.references.hinotes.ui.itemdetail.reminder.ReminderByLocationFragment
import com.huawei.references.hinotes.ui.itemdetail.reminder.ReminderByTimeFragment
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions
import kotlinx.android.synthetic.main.activity_detail_todo_list.*
import kotlinx.android.synthetic.main.item_detail_toolbar.*
import java.io.IOException

abstract class ItemDetailBaseActivity : BaseActivity() {

    abstract fun getItemDetailViewModel() : ItemDetailViewModel

    protected var noteDetailChanged = false

    protected var isNewNote = true

    protected lateinit var itemData : Item

    private lateinit var bottomSheetBehavior:BottomSheetBehavior<View>

    private val takePictureResultCode = 201
    private val pickImageResultCode = 202
    private val recordAudioResultCode = 203

    override fun onStart() {
        super.onStart()
        observeDataHolderLiveData(getItemDetailViewModel().saveItemLiveData){
            noteDetailChanged = false
            isNewNote=false
            itemData.itemId=it.itemId

//            it.subItemIds?.let {serverSubItemList->
//                val newIds=serverSubItemList.filter {
//                        serverSubItemId->
//                    itemData.todoListSubItems?.find { it.id==serverSubItemId }==null
//                }
//                newIds.forEach {
//                    itemData.todoListSubItems?.forEach {subItem->
//                        if(subItem.id==-1) subItem.id=it
//                    }
//                }
//            }
//            it.reminderId?.let {
//                 itemData.reminder?.id=it
//            }

            onSaveSuccessful()
            customToast(this,this.getString(R.string.note_successfully_saved),false)
        }

        observeDataHolderLiveData(getItemDetailViewModel().deleteItemLiveData){
            customToast(this,this.getString(R.string.note_successfully_deleted),false)
            finish()
        }

        if(itemData.itemId!=-1)
            getItemDetailViewModel().getReminders(itemData.itemId)

        observeDataHolderLiveData(getItemDetailViewModel().reminderLiveData,{
            onGetRemindersCompleted()
            //TODO: fill no reminder ui
        }){
            itemData.reminder=it.first()
            onGetRemindersCompleted()
            //TODO: fill reminder ui
        }
    }

    override fun setupUI() {
        super.setupUI()
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
            setCustomView(R.layout.item_detail_toolbar)
        }
        add_reminder?.setOnClickListener {
            customBottomDialogs(this.getString(R.string.add_reminder_by_time),this.getString(R.string.add_reminder_by_location),getDrawable(R.drawable.calendar_reminder_icon),getDrawable(R.drawable.location_reminder_icon),{performAddReminderByTime()},{performAddReminderByLocation()})
        }

        location_icon?.setOnClickListener {
            performAddLocation()
        }

        bottomSheetBehavior = BottomSheetBehavior.
        from(findViewById<View>(R.id.mapBottomSheet)).apply {
            state = BottomSheetBehavior.STATE_HIDDEN
        }

        delete_icon.setOnClickListener {
            if(!isNewNote){
                runWithAGConnectUserOrOpenLogin {
                    customPopup(this.getString(R.string.delete_note_popup_warning),
                        this.getString(R.string.delete_note_popup_accept),
                        this.getString(R.string.delete_note_popup_reject)
                    ) { getItemDetailViewModel().deleteItem(itemData,it.uid) }
                }
            }
        }

        microphone_icon.setOnClickListener {
            performSpeechToText()
        }

        image_icon.setOnClickListener {
            hideKeyboard()
            customBottomDialogs("Take Picture","Pick Image",
                getDrawable(R.drawable.camera_icon),getDrawable(R.drawable.gallery_icon),
                {performTakePicture()},{performPickImage()})
        }

        findViewById<TextView>(R.id.item_detail_title)?.text = itemData.title
        findViewById<TextView>(R.id.item_detail_description)?.
        text=itemData.description ?: ""

    }

    private fun performAddLocation(){
        runWithPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET){
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                val bottomSheetFragment =
                    LocationFragment(
                        itemData
                    )
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            }
            else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }
    }

    protected open fun onGetRemindersCompleted(){}

    private fun performAddReminderByTime() =
        runWithPermissions(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET){
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                val bottomSheetFragment = ReminderByTimeFragment()
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            }
            else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }

    private fun performAddReminderByLocation() =
        runWithPermissions(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET){
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                val bottomSheetFragment = ReminderByLocationFragment(itemData)
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            }
            else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
        }

    override fun onBackPressed() {
        if(noteDetailChanged) {
            runWithAGConnectUserOrOpenLogin {
                customPopup(this.getString(R.string.delete_item_changes_popup_warning),
                    this.getString(R.string.delete_item_changes_popup_accept),
                    this.getString(R.string.delete_item_changes_popup_reject)
                ) {finish()}
            }
        }
        else{
            super.onBackPressed()
        }
    }

    open fun onSaveSuccessful(){}

    protected fun runWithAGConnectUserOrOpenLogin(runBlock: (agConnectUser: AGConnectUser) -> Unit){
        agConnectAuth?.currentUser?.let {
            runBlock.invoke(it)
        } ?: kotlin.run {
            openLoginActivity()
        }
    }

    private fun performTakePicture() =
        runWithPermissions(Manifest.permission.CAMERA){
            startActivityForResult(Intent(MediaStore.ACTION_IMAGE_CAPTURE), takePictureResultCode)
        }

    private fun performPickImage() =
        runWithPermissions(Manifest.permission.READ_EXTERNAL_STORAGE){
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, pickImageResultCode)
        }

    private fun performTextRecognition(selectedImageBitmap: Bitmap){
        val setting = MLLocalTextSetting.Factory()
            .setOCRMode(MLLocalTextSetting.OCR_DETECT_MODE) // Specify languages that can be recognized.
            .setLanguage("en")
            .create()
        val analyzer = MLAnalyzerFactory.getInstance().getLocalTextAnalyzer(setting)
        val frame = MLFrame.fromBitmap(selectedImageBitmap)

        val task: Task<MLText> = analyzer.asyncAnalyseFrame(frame)
        task.addOnSuccessListener {
            if(it.stringValue == ""){
                Toast.makeText(this,this.getString(R.string.text_recognition_could_not_read), Toast.LENGTH_LONG).show()
            }
            val resultText=it.stringValue.replace("\n"," ")
            findViewById<TextView>(R.id.item_detail_description)?.apply {
                append(resultText)
            }
        }.addOnFailureListener {
            Toast.makeText(this,this.getString(R.string.text_recognition_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun performSpeechToText() = runWithPermissions(Manifest.permission.RECORD_AUDIO){
        val intent = Intent(this, MLAsrCaptureActivity::class.java)
            .putExtra(MLAsrCaptureConstants.LANGUAGE, "en-US")
            .putExtra(MLAsrCaptureConstants.FEATURE, MLAsrCaptureConstants.FEATURE_WORDFLUX)
        startActivityForResult(intent, recordAudioResultCode)
    }

    fun locationSelected(lat:Double,lng:Double,mapType: MapType,radius: Double){
        when(mapType){
            MapType.ITEM_LOCATION ->{
                itemData.lat=lat
                itemData.lng=lng
                //TODO: set location ui
            }
            MapType.GEOFENCE->{
                val reminder=itemData.reminder ?:
                    Reminder(-1,reminderType = ReminderType.ByGeofence).apply {
                    itemData.reminder=this
                }
                reminder.apply {
                    location= LatLng(lat,lng)
                    this.radius=radius
                }
                //TODO: set location reminder ui
            }
        }
    }

    fun poiSelected(site: Site, mapType: MapType,radius: Double){
        when(mapType){
            MapType.ITEM_LOCATION ->{
                itemData.lat=site.location.lat
                itemData.lng=site.location.lng
                //TODO: set poi location ui
            }
            MapType.GEOFENCE->{
                val reminder=itemData.reminder ?: Reminder(-1,reminderType = ReminderType.ByGeofence).apply {
                    itemData.reminder=this
                }
                reminder.apply {
                    location= LatLng(site.location.lat,site.location.lng)
                    reminderType=ReminderType.ByGeofence
                    title=site.name
                    this.radius=radius
                }
                //TODO: set location reminder ui
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == takePictureResultCode) {
            if (resultCode == Activity.RESULT_OK) {
                if (data?.data == null) {
                    performTextRecognition(data?.extras!!["data"] as Bitmap)
                } else {
                    performTextRecognition(MediaStore.Images.Media.getBitmap( this.contentResolver,data.data))
                }
            }
        }
        if (requestCode == pickImageResultCode) {
            if (resultCode == Activity.RESULT_OK) {
                val pickedImage: Uri? = data?.data
                try {
                    performTextRecognition(MediaStore.Images.Media.getBitmap(this.contentResolver, pickedImage))
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        if (requestCode == recordAudioResultCode) {
            when (resultCode) {
                MLAsrCaptureConstants.ASR_SUCCESS -> if (data != null) {
                    val bundle = data.extras
                    if (bundle != null && bundle.containsKey(MLAsrCaptureConstants.ASR_RESULT)) {
                        val text = bundle.getString(MLAsrCaptureConstants.ASR_RESULT).toString()
                        findViewById<TextView>(R.id.item_detail_description)?.append(text)
                    }
                }
                MLAsrCaptureConstants.ASR_FAILURE -> if (data != null) {
                    val bundle = data.extras
                    if (bundle != null && bundle.containsKey(MLAsrCaptureConstants.ASR_ERROR_CODE)) {
                        val errorCode = bundle.getInt(MLAsrCaptureConstants.ASR_ERROR_CODE)
                        Log.e("SpeechToTextCode",  errorCode.toString())
                    }
                    if (bundle != null && bundle.containsKey(MLAsrCaptureConstants.ASR_ERROR_MESSAGE)) {
                        val errorMsg = bundle.getString(MLAsrCaptureConstants.ASR_ERROR_MESSAGE)
                        Log.e("SpeechToTextMessage", errorMsg.toString())
                    }
                    Toast.makeText(this, this.getString(R.string.speech_to_text_failed),Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    protected fun <T : Any>observeDataHolderLiveData(liveData: LiveData<DataHolder<T>>,
                                                     noResultBlock: () -> Unit= {},
                                                     runBlock: (data:T) -> Unit){
        liveData.observe(this, Observer {
            when(it){
                is DataHolder.Success->{
                    detail_progress_bar.hide()
                    runBlock.invoke(it.data)
                }
                is DataHolder.Fail->{
                    detail_progress_bar.hide()
                    if(it.baseError is NoRecordFoundError){
                        noResultBlock.invoke()
                    }
                    else {
                        customToast(this,it.errStr,true)
                    }
                }
                is DataHolder.Loading->{
                    detail_progress_bar.show()
                }
            }
        })
    }
}