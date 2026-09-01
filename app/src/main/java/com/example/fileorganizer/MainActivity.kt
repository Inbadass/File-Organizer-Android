package com.example.fileorganizer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : ComponentActivity() {

    private val extMap = mapOf(
        "jpg" to "Images", "jpeg" to "Images", "png" to "Images",
        "mp4" to "Video",
        "mp3" to "Audio",
        "pdf" to "Documents", "txt" to "Documents", "json" to "Documents",
        "py" to "Python", "kt" to "Kotlin"
    )

    private val baseFolder: File = Environment.getExternalStorageDirectory()
    private lateinit var historyFile: File

    private lateinit var permissionStatus: TextView
    private lateinit var srcInput: EditText
    private lateinit var dstInput: EditText
    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        historyFile = File(filesDir, "history.json")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        permissionStatus = TextView(this).apply {
            text = "Checking permission..."
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
        }

        val grantBtn = Button(this).apply {
            text = "Grant Storage Permission"
            setOnClickListener { requestPermission() }
        }

        srcInput = EditText(this).apply {
            hint = "Source Folder (e.g. Download)"
        }

        dstInput = EditText(this).apply {
            hint = "Destination Folder (e.g. Organized)"
        }

        val pathGuide = TextView(this).apply {
            text = "💡 Tip: Use '/' for subfolders (e.g., Download/Test or A_Python/Test)"
            textSize = 12f
            setPadding(0, 12, 0, 12)
            setTextColor(Color.GRAY)
        }

        val organizeBtn = Button(this).apply {
            text = "Organize Files"
            setOnClickListener { organizeFiles() }
        }

        val undoBtn = Button(this).apply {
            text = "Undo Last Action"
            setOnClickListener { undoMove() }
        }

        logView = TextView(this).apply {
            text = "Logs will appear here...\n"
            textSize = 14f
        }

        val scrollView = ScrollView(this).apply {
            addView(logView)
        }

        layout.addView(permissionStatus)
        layout.addView(grantBtn)
        layout.addView(srcInput)
        layout.addView(dstInput)
        layout.addView(pathGuide)
        layout.addView(organizeBtn)
        layout.addView(undoBtn)
        layout.addView(scrollView)

        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
    }

    // --- Permissions ---

    private fun checkPermission(): Boolean {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            val write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            read == PackageManager.PERMISSION_GRANTED && write == PackageManager.PERMISSION_GRANTED
        }

        if (hasPermission) {
            permissionStatus.text = "Status: Permission Granted ✅"
            permissionStatus.setTextColor(Color.GREEN)
        } else {
            permissionStatus.text = "Status: Permission Needed ❌"
            permissionStatus.setTextColor(Color.RED)
        }
        return hasPermission
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
        }
    }

    // --- File Logic ---

    private fun organizeFiles() {
        if (!checkPermission()) {
            log("Grant permission first!")
            return
        }

        val srcName = srcInput.text.toString().trim()
        val dstName = dstInput.text.toString().trim()

        if (srcName.isEmpty() || dstName.isEmpty()) {
            log("Please enter both folder names.")
            return
        }

        Thread {
            val srcDir = File(baseFolder, srcName)
            val dstDir = File(baseFolder, dstName)

            if (!srcDir.exists() || !srcDir.isDirectory) {
                log("Source folder does not exist!")
                return@Thread
            }

            val files = srcDir.listFiles() ?: emptyArray()
            val history = loadHistory()
            var count = 0

            for (file in files) {
                if (!file.isFile) continue

                val category = extMap[file.extension.lowercase()] ?: "Others"
                val targetFolder = File(dstDir, category)
                if (!targetFolder.exists()) targetFolder.mkdirs()

                val targetFile = File(targetFolder, file.name)

                if (moveFile(file, targetFile)) {
                    log("Moved: ${file.name} -> $category")
                    val entry = JSONObject().apply {
                        put("from", file.path)
                        put("to", targetFile.path)
                    }
                    history.put(entry)
                    count++
                }
            }

            saveHistory(history)
            log("Finished! Moved $count files.")
        }.start()
    }

    private fun undoMove() {
        if (!checkPermission()) return

        Thread {
            val history = loadHistory()
            if (history.length() == 0) {
                log("Nothing to undo.")
                return@Thread
            }

            val remainingHistory = JSONArray()
            for (i in 0 until history.length()) {
                val item = history.getJSONObject(i)
                val currentFile = File(item.getString("to"))
                val originalFile = File(item.getString("from"))

                originalFile.parentFile?.mkdirs()

                if (moveFile(currentFile, originalFile)) {
                    log("Restored: ${originalFile.name}")
                } else {
                    remainingHistory.put(item)
                }
            }

            saveHistory(remainingHistory)
            log("Undo completed.")
        }.start()
    }

    // --- Helpers ---

    private fun moveFile(src: File, dst: File): Boolean {
        if (src.renameTo(dst)) return true
        return try {
            src.copyTo(dst, overwrite = false)
            src.delete()
        } catch (e: Exception) {
            false
        }
    }

    private fun log(message: String) {
        runOnUiThread {
            logView.text = "${logView.text}\n$message"
        }
    }

    private fun loadHistory(): JSONArray {
        return try {
            if (historyFile.exists()) JSONArray(historyFile.readText()) else JSONArray()
        } catch (e: Exception) {
            JSONArray()
        }
    }

    private fun saveHistory(data: JSONArray) {
        try {
            historyFile.writeText(data.toString())
        } catch (_: Exception) {}
    }
}
