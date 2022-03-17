package com.shong.cameraexample

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import coil.load
import com.google.android.material.snackbar.Snackbar
import com.shong.cameraexample.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val TAG = this::class.java.simpleName + "_sHong"
    lateinit var resultLauncherCamera : ActivityResultLauncher<Intent>
    lateinit var resultLauncherPhoto : ActivityResultLauncher<Intent>

    private var cameraPhotoFilePath: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        resultLauncherCamera = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if (result?.resultCode == Activity.RESULT_OK) {
                val selectedImageUri : Uri = cameraPhotoFilePath ?: return@registerForActivityResult

                getNameSize(selectedImageUri, binding.textView)
                showImage(selectedImageUri, binding.imageView)
            }else if(result?.resultCode == Activity.RESULT_CANCELED){
                Log.d(TAG,"Camera shot cancel!")
            }
        }

        resultLauncherPhoto = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if (result?.resultCode == Activity.RESULT_OK) {
                val selectedImageUri= result.data?.data ?: return@registerForActivityResult

                getNameSize(selectedImageUri, binding.textView)
                showImage(selectedImageUri, binding.imageView)
            }else if(result?.resultCode == Activity.RESULT_CANCELED){
                Log.d(TAG,"Image select cancel!")
            }
        }

        findViewById<Button>(R.id.button).setOnClickListener {
            checkPermission()
        }

        findViewById<Button>(R.id.goLibraryButton).setOnClickListener {
            startActivity(Intent(this,LibraryExActivity::class.java))
        }

    }

    private val EXTERNAL_REQ = 22
    private fun checkPermission() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Log.d(TAG,"읽기 권한 요청")
            }
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), EXTERNAL_REQ
            )
        }else{
            Log.d(TAG,"already granted")
            makeCameraDialog()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            EXTERNAL_REQ ->{
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Log.d(TAG,"external read permission is granted")
                    makeCameraDialog()
                }else{
                    Log.d(TAG,"external read permission is denied")

                    if (Build.VERSION.SDK_INT >= 30){
                        Snackbar.make(findViewById(R.id.linearLayout),"외부저장공간 권한이 거부되어 있습니다.\n'확인'을 누르면 설정창으로 이동합니다.", Snackbar.LENGTH_LONG)
                            .setAction("확인") {
                                val settingIntent = Intent().apply {
                                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    data = Uri.fromParts("package", packageName, null)
                                }
                                startActivity(settingIntent)
                            }
                            .show()
                    }

                }
            }
        }
    }

    private val cameraDialog : Dialog by lazy {
        Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.item_cameradialog)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setGravity(Gravity.BOTTOM)
        }
    }
    private fun makeCameraDialog(){
        cameraDialog.apply {
            findViewById<TextView>(R.id.cameraTextView).setOnClickListener {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                val photoFile: File? = try {
                    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                    val imagePath = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    File(imagePath, "JPEG_${timeStamp}_" + ".jpg")
                } catch (e: Exception) {
                    Log.d(TAG,"file create ERROR! : $e")
                    null
                }

                photoFile?.also { file ->
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this@MainActivity,
                        "com.shong.cameraexample.provider",
                        file
                    )
                    cameraPhotoFilePath = photoURI
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                }

                intent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                resultLauncherCamera.launch(intent)
                dismiss()
            }

            findViewById<TextView>(R.id.albumTextView).setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK).apply {
                    setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
//                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                resultLauncherPhoto.launch(intent)
                dismiss()
            }
        }.show()
    }

    private fun getNameSize(selectedImageUri: Uri, textView : TextView){
        contentResolver.query(selectedImageUri, null, null, null, null)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                val name = cursor.getString(nameIndex) // 이미지 이름
                val size = cursor.getLong(sizeIndex).toString() // 이미지 사이즈
                textView.text = "name : $name\nsize : $size"
            }
    }

    private fun showImage(uri: Uri, iv: ImageView){
//        this.imageLoader.memoryCache.remove(MemoryCache.Key(id))
        iv.load(uri) {
//            memoryCacheKey(MemoryCache.Key(id))
            crossfade(true)
            crossfade(100)
//            transformations(CircleCropTransformation())
//            placeholder(R.drawable.img_profile_120)
//            error(R.drawable.img_profile_120)
        }
    }

}