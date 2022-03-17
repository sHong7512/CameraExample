package com.shong.cameraexample

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.sangcomz.fishbun.FishBun
import com.sangcomz.fishbun.adapter.image.impl.GlideAdapter
import com.shong.cameraexample.databinding.ActivityLibraryExBinding

class LibraryExActivity : AppCompatActivity() {
    private val TAG = this::class.java.simpleName + "_sHong"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLibraryExBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val ivList = listOf(binding.imageView0, binding.imageView1, binding.imageView2)

        val resultLauncherPhoto = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result ->
            if (result?.resultCode == Activity.RESULT_OK) {
                val pathList = result.data?.getParcelableArrayListExtra<Uri>(FishBun.INTENT_PATH) ?: arrayListOf()

                for(i in pathList.indices)
                    ivList[i].setImageURI(pathList[i])

            }else if(result?.resultCode == Activity.RESULT_CANCELED){
                Log.d(TAG,"Image select cancel!")
            }
        }

        binding.goGalleyButton.setOnClickListener {
            FishBun.with(this)
                .setImageAdapter(GlideAdapter())
                .setMaxCount(3)
//                .hasCameraInPickerPage(true)
                .startAlbumWithActivityResultCallback(resultLauncherPhoto)
        }
    }
}