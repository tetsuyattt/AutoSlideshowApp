package jp.techacademy.tetsuya.autoslideshowapp

import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import jp.techacademy.tetsuya.autoslideshowapp.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    //追加１
    //パーミッション用 permission=許可
    private val PERMISSIONS_REQUEST_CODE =100
    //↓Imageパーミッションの許可ダイアログ
    private val readImagesPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) android.Manifest.permission.READ_MEDIA_IMAGES
        else android.Manifest.permission.READ_EXTERNAL_STORAGE

    //追加２　タイマー作成　→onCreate()内へ
    private var timer: Timer? = null
    private var imageList: MutableList<Uri>? = null
    private var currentImageIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        //追加３　再生・停止ボタン押したらstartSlidshow()開始
        binding.startAndStopButton.setOnClickListener {
           if (timer == null) {
               startSlidshow()
               binding.startAndStopButton.text = "停止"  //再生中なのでテキストは「停止」へ
               binding.nextButton.isEnabled = false     //スライドショー中は押せないようにする
               binding.backButton.isEnabled = false     //Swiftと一緒だった。よかった
//
           } else {
               //timerが存在する時に「再生・停止ボタン」押したらタイマーを止める　→停止中なのでテキスト「再生」へ
               timer!!.cancel()
               timer = null
               binding.startAndStopButton.text = "再生"
               binding.nextButton.isEnabled = true     //停止中は押せるようにする
               binding.backButton.isEnabled = true
           }
        }
        //追加３　「進むボタン」押したら画像が１つ進む
        binding.nextButton.setOnClickListener {
            nextImage() //
        }
        //追加３　「進むボタン」押したら画像が1つ戻る
        binding.backButton.setOnClickListener {
            backImage()
        }

        //追加１
        //パーミッションの許可状況確認
        //アプリ立ち上げた時に、許可されてたら画像読み込みとスライドショーを開始
        if (checkSelfPermission(readImagesPermission) == PackageManager.PERMISSION_GRANTED) {
            getContentsInfo()
            firstImage()

//            binding.imageView.setImageURI()
//            startSlidshow() //ーーーーーーーーーーーーーーーーー

            //一致してなければ、許可されていないため、許可ダイアログを表示させる
        } else {
            requestPermissions(
                arrayOf(readImagesPermission),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    //追加３　アプリ立ち上げ時の最初の静止画　（→これしか解決策が思いつかなかった）
    private fun firstImage() {
        if (imageList != null && imageList!!.isNotEmpty()) {
//            currentImageIndex = 0
            val imageUri = imageList!![0]
            binding.imageView.setImageURI(imageUri)
        }
    }

    //追加３　画像送り機能（nextButton）　startSlidshow()のパクリ
    private fun nextImage() {
        if (imageList != null && imageList!!.isNotEmpty()) {
            currentImageIndex = (currentImageIndex + 1) % imageList!!.size
            val imageUri = imageList!![currentImageIndex]
            binding.imageView.setImageURI(imageUri)
        }
    }
    //追加３　画像戻り機能（backButton）　最初の画像の時に押したら最後の画像に
    private fun backImage() {
        if (imageList != null && imageList!!.isNotEmpty()) {
            currentImageIndex = (currentImageIndex - 1 + imageList!!.size) % imageList!!.size
            val imageUri = imageList!![currentImageIndex]
            binding.imageView.setImageURI(imageUri)
        }
    }

    //追加１　パーミッションが許可・拒否された時の処理
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContentsInfo()
                    firstImage() //

//                    startSlidshow() //暫定的に、立ち上げ時からスライドショーしたけど、ここをsetImage〜にすれば停止画になる？
//                    binding.imageView.setImageURI(imageUri)　 →難しかった
                } else {

                    //拒否された時はユーザーにメッセージを送信
                    Toast.makeText(
                        this,
                        "画像の読み込みが許可されていません。パーミッションを許可してください。",
                    Toast.LENGTH_LONG
                    ).show()
                    return
                }
        }
    }


    //追加２
    //画像情報を取得する処理
    private fun getContentsInfo() {
        //cursor＝カーソル　query＝問い合わせ
        val resolver = contentResolver
        val cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, //以下のデータの種類を問い合わせ？
            null,
            null,
            null,
            null
        )

//            imageList = mutableListOf()
//            if (cursor != null) {

        //端末に画像がある時に読み込む
        if (cursor != null && cursor.count > 0) {
            imageList = mutableListOf()
            while (cursor.moveToNext()) {
                val fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val id = cursor.getLong(fieldIndex)
                val imageUri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                imageList?.add(imageUri)
            }
            cursor.close()

        } else {
            Toast.makeText(
                this,
                "端末に画像がありません。",
                Toast.LENGTH_LONG
            ).show()
            return
        }

    }

    //追加２　「再生・停止ボタン」押した時、2秒経過ごとに画像を表示する
    private fun startSlidshow() {
        //タイマー作成
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                //ここに画像送り機能を実装する…はず　
                runOnUiThread {
                    if (imageList != null && imageList!!.isNotEmpty()) {
                        currentImageIndex = (currentImageIndex + 1) % imageList!!.size
                        val imageUri = imageList!![currentImageIndex]
                        binding.imageView.setImageURI(imageUri)
                    }
                }
            }
        }, 2000, 2000)
    }

    //画面が変わった時、タイマーを停止
    override fun onPause() {
        super.onPause()
        timer?.cancel()
        timer = null
        binding.startAndStopButton.text = "再生"
        binding.nextButton.isEnabled = true
        binding.backButton.isEnabled = true
    }




}