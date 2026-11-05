package com.ruhitek.downloader

import android.Manifest
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: EditText
    private lateinit var fetchBtn: Button
    private lateinit var errorText: TextView
    private lateinit var preview: LinearLayout
    private lateinit var thumb: ImageView
    private lateinit var titleText: TextView
    private lateinit var subText: TextView
    private lateinit var quality: Spinner
    private lateinit var downloadBtn: Button
    private lateinit var progress: ProgressBar
    private lateinit var status: TextView

    private var currentUrl = ""
    private var busy = false

    private val qualities = listOf(
        "Best quality (MP4)" to "bv*[ext=mp4]+ba[ext=m4a]/bv*+ba/b",
        "1080p (MP4)" to "bv*[height<=1080][ext=mp4]+ba[ext=m4a]/bv*[height<=1080]+ba/b[height<=1080]/b",
        "720p (MP4)" to "bv*[height<=720][ext=mp4]+ba[ext=m4a]/bv*[height<=720]+ba/b[height<=720]/b",
        "480p (MP4)" to "bv*[height<=480][ext=mp4]+ba[ext=m4a]/bv*[height<=480]+ba/b[height<=480]/b",
        "Audio only (MP3)" to "ba/b",
    )

    private val supported = Regex(
        "https?://([a-z0-9-]+\\.)*(youtube\\.com|youtu\\.be|instagram\\.com|facebook\\.com|fb\\.watch|fb\\.com)/\\S*",
        RegexOption.IGNORE_CASE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlInput = findViewById(R.id.urlInput)
        fetchBtn = findViewById(R.id.fetchBtn)
        errorText = findViewById(R.id.errorText)
        preview = findViewById(R.id.preview)
        thumb = findViewById(R.id.thumb)
        titleText = findViewById(R.id.title)
        subText = findViewById(R.id.sub)
        quality = findViewById(R.id.quality)
        downloadBtn = findViewById(R.id.downloadBtn)
        progress = findViewById(R.id.progress)
        status = findViewById(R.id.status)

        quality.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, qualities.map { it.first }
        )

        findViewById<Button>(R.id.pasteBtn).setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
            urlInput.setText(extractUrl(text) ?: text)
        }
        fetchBtn.setOnClickListener { fetchInfo() }
        urlInput.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_GO) { fetchInfo(); true } else false
        }
        downloadBtn.setOnClickListener { startDownload() }

        handleShare(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    private fun handleShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            val url = extractUrl(shared)
            if (url != null) {
                urlInput.setText(url)
                fetchInfo()
            }
        }
    }

    private fun extractUrl(text: String): String? = supported.find(text)?.value

    private fun showError(msg: String?) {
        errorText.text = msg.orEmpty()
        errorText.visibility = if (msg.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    private fun waitForSetup(): String? {
        var waited = 0
        while (!App.ready && App.initError == null && waited < 60_000) {
            Thread.sleep(250); waited += 250
        }
        return App.initError ?: if (!App.ready) "App is still setting up, try again in a moment." else null
    }

    private fun friendly(e: Throwable): String {
        val m = (e.message ?: "").lowercase()
        return when {
            "login" in m || "private" in m -> "This video is private or needs a login, so it can't be downloaded."
            "unsupported url" in m -> "That link doesn't point to a video."
            "unable to resolve host" in m || "network" in m -> "No internet connection."
            else -> "Couldn't get this video. Check the link and try again."
        }
    }

    private fun fetchInfo() {
        if (busy) return
        val url = extractUrl(urlInput.text.toString().trim())
        if (url == null) {
            showError("Please paste a YouTube, Instagram or Facebook video link.")
            return
        }
        showError(null)
        preview.visibility = View.GONE
        busy = true
        fetchBtn.isEnabled = false
        fetchBtn.text = "Loading..."

        Thread {
            val setupErr = waitForSetup()
            if (setupErr != null) {
                runOnUiThread { doneFetching(); showError(setupErr) }
                return@Thread
            }
            try {
                val req = YoutubeDLRequest(url).addOption("--no-playlist")
                val info = YoutubeDL.getInstance().getInfo(req)
                val bmp = try {
                    info.thumbnail?.let { URL(it).openStream().use { s -> BitmapFactory.decodeStream(s) } }
                } catch (_: Exception) { null }
                runOnUiThread {
                    currentUrl = url
                    titleText.text = info.title ?: "Video"
                    val dur = if (info.duration > 0) "%d:%02d".format(info.duration / 60, info.duration % 60) else ""
                    subText.text = listOf(info.extractorKey, info.uploader, dur)
                        .filter { !it.isNullOrBlank() }.joinToString("  |  ")
                    if (bmp != null) thumb.setImageBitmap(bmp) else thumb.setImageDrawable(null)
                    status.text = ""
                    progress.visibility = View.GONE
                    preview.visibility = View.VISIBLE
                    doneFetching()
                }
            } catch (e: Exception) {
                runOnUiThread { doneFetching(); showError(friendly(e)) }
            }
        }.start()
    }

    private fun doneFetching() {
        busy = false
        fetchBtn.isEnabled = true
        fetchBtn.text = "Get video"
    }

    private fun startDownload() {
        if (busy || currentUrl.isEmpty()) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            return
        }
        val audioOnly = quality.selectedItemPosition == qualities.lastIndex
        val fmt = qualities[quality.selectedItemPosition].second

        busy = true
        downloadBtn.isEnabled = false
        fetchBtn.isEnabled = false
        showError(null)
        progress.visibility = View.VISIBLE
        progress.isIndeterminate = true
        status.text = "Starting download..."

        Thread {
            val work = File(cacheDir, "dl_" + System.currentTimeMillis()).apply { mkdirs() }
            try {
                val req = YoutubeDLRequest(currentUrl)
                    .addOption("--no-playlist")
                    .addOption("-f", fmt)
                    .addOption("-o", File(work, "%(title).80B.%(ext)s").absolutePath)
                if (audioOnly) {
                    req.addOption("-x").addOption("--audio-format", "mp3").addOption("--audio-quality", "192K")
                } else {
                    req.addOption("--merge-output-format", "mp4")
                }
                YoutubeDL.getInstance().execute(request = req, processId = "dl", callback = { pct, eta, _ ->
                    runOnUiThread {
                        if (pct > 0) {
                            progress.isIndeterminate = false
                            progress.progress = pct.toInt()
                            status.text = "Downloading ${pct.toInt()}%" + if (eta > 0) "  (about ${eta}s left)" else ""
                        }
                    }
                })
                val out = work.listFiles()
                    ?.filter { !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") }
                    ?.maxByOrNull { it.length() }
                    ?: throw IllegalStateException("Download failed")
                runOnUiThread { status.text = "Saving to Downloads..." }
                val savedName = saveToDownloads(out)
                runOnUiThread {
                    progress.isIndeterminate = false
                    progress.progress = 100
                    status.text = "Done. Saved to Downloads as $savedName"
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progress.visibility = View.GONE
                    status.text = ""
                    showError(friendly(e))
                }
            } finally {
                work.deleteRecursively()
                runOnUiThread {
                    busy = false
                    downloadBtn.isEnabled = true
                    fetchBtn.isEnabled = true
                }
            }
        }.start()
    }

    private fun saveToDownloads(src: File): String {
        val name = src.name
        val mime = when (src.extension.lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "webm" -> "video/webm"
            else -> "video/mp4"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/RuhiTek")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Couldn't save file")
            contentResolver.openOutputStream(uri)!!.use { o -> src.inputStream().use { it.copyTo(o) } }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "RuhiTek")
            dir.mkdirs()
            src.copyTo(File(dir, name), overwrite = true)
        }
        return "RuhiTek/$name"
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code == 1 && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) startDownload()
    }
}
