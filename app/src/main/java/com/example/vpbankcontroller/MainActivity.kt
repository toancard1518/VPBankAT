package com.example.vpbankcontroller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.Xml
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.json.JSONArray
import org.xmlpull.v1.XmlPullParser
import java.util.zip.ZipInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var etMkhList: EditText
    private lateinit var etVpbankPackage: EditText
    private lateinit var etSmartOtpPin: EditText
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnOpenApp: Button
    private lateinit var btnImportFile: Button
    private lateinit var tvStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgress: TextView

    private lateinit var prefs: SharedPreferences

    // ── File picker (TXT / CSV) ──────────────────────────────────────────────────
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) importFileFromUri(uri)
    }

    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val current = intent.getIntExtra(AppConfig.EXTRA_CURRENT, 0)
            val total   = intent.getIntExtra(AppConfig.EXTRA_TOTAL, 0)
            val status  = intent.getStringExtra(AppConfig.EXTRA_STATUS) ?: ""

            tvStatus.text        = status
            tvProgress.text      = "$current / $total"
            progressBar.max      = if (total > 0) total else 1
            progressBar.progress = current

            val finished = status.startsWith("âœ…") || status == "ÄÃ£ dá»«ng" || status.startsWith("HoÃ n táº¥t")
            btnStart.isEnabled = finished
            btnStop.isEnabled  = !finished
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)

        etMkhList        = findViewById(R.id.et_mkh_list)
        etVpbankPackage  = findViewById(R.id.et_vpbank_package)
        etSmartOtpPin    = findViewById(R.id.et_smart_otp_pin)
        btnStart         = findViewById(R.id.btn_start)
        btnStop          = findViewById(R.id.btn_stop)
        btnOpenApp       = findViewById(R.id.btn_open_app)
        btnImportFile    = findViewById(R.id.btn_import_file)
        tvStatus         = findViewById(R.id.tv_status)
        progressBar      = findViewById(R.id.progress_bar)
        tvProgress       = findViewById(R.id.tv_progress)

        etVpbankPackage.setText(
            prefs.getString(AppConfig.KEY_VPBANK_PACKAGE, AppConfig.vpbankPackage)
        )

        btnStop.isEnabled = false

        // â”€â”€ Báº¯t Ä‘áº§u â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        btnStart.setOnClickListener {
            if (!isAccessibilityServiceEnabled()) {
                Toast.makeText(
                    this,
                    "Vui lÃ²ng báº­t Accessibility Service trÆ°á»›c trong CÃ i Ä‘áº·t â†’ Trá»£ nÄƒng",
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }

            val pin = etSmartOtpPin.text.toString().trim()
            if (pin.length != 6 || !pin.all { it.isDigit() }) {
                Toast.makeText(this, "PIN Smart OTP pháº£i Ä‘Ãºng 6 chá»¯ sá»‘", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val mkhRaw = etMkhList.text.toString().trim()
            if (mkhRaw.isEmpty()) {
                Toast.makeText(this, "Nháº­p danh sÃ¡ch MKH", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val vpbankPkg = etVpbankPackage.text.toString().trim().ifEmpty { AppConfig.vpbankPackage }
            val mkhLines  = mkhRaw.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val jsonArr   = JSONArray(mkhLines)

            // LÆ°u PIN chá»‰ trong memory, KHÃ”NG ghi xuá»‘ng SharedPreferences
            AppConfig.smartOtpPin   = pin
            AppConfig.vpbankPackage = vpbankPkg

            prefs.edit()
                .putString(AppConfig.KEY_MKH_LIST, jsonArr.toString())
                .putInt(AppConfig.KEY_CURRENT_INDEX, 0)
                .putBoolean(AppConfig.KEY_IS_RUNNING, true)
                .putString(AppConfig.KEY_VPBANK_PACKAGE, vpbankPkg)
                .apply()

            btnStart.isEnabled = false
            btnStop.isEnabled  = true
            tvStatus.text      = "Äang khá»Ÿi Ä‘á»™ngâ€¦"

            // Má»Ÿ VPBank NEO
            launchApp(vpbankPkg)

            LocalBroadcastManager.getInstance(this)
                .sendBroadcast(Intent(AppConfig.ACTION_START))
        }

        // â”€â”€ Dá»«ng â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        btnStop.setOnClickListener {
            AppConfig.smartOtpPin = ""
            prefs.edit().putBoolean(AppConfig.KEY_IS_RUNNING, false).apply()
            LocalBroadcastManager.getInstance(this)
                .sendBroadcast(Intent(AppConfig.ACTION_STOP))
            btnStart.isEnabled = true
            btnStop.isEnabled  = false
            tvStatus.text      = "ÄÃ£ dá»«ng"
        }

        // â”€â”€ Má»Ÿ VPBank thá»§ cÃ´ng â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        btnOpenApp.setOnClickListener {
            launchApp(etVpbankPackage.text.toString().trim())
        }

        // ── Import file TXT / CSV ─────────────────────────────────────────────
        btnImportFile.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            progressReceiver,
            IntentFilter(AppConfig.ACTION_PROGRESS)
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(progressReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        // XÃ³a PIN khá»i memory khi app Ä‘Ã³ng
        AppConfig.smartOtpPin = ""
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private fun launchApp(packageName: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            startActivity(launch)
        } else {
            Toast.makeText(this, "KhÃ´ng tÃ¬m tháº¥y app: $packageName", Toast.LENGTH_LONG).show()
        }
    }

    // ── Import file TXT / CSV / XLSX helpers ───────────────────────────────────
    private fun importFileFromUri(uri: Uri) {
        try {
            val mimeType = contentResolver.getType(uri) ?: ""
            val fileName = uri.lastPathSegment ?: ""
            val isXlsx   = mimeType.contains("spreadsheet") ||
                           mimeType.contains("excel") ||
                           fileName.endsWith(".xlsx", ignoreCase = true) ||
                           fileName.endsWith(".xls",  ignoreCase = true)

            val rawLines: List<String> = if (isXlsx) {
                parseXlsx(uri)
            } else {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: run {
                        Toast.makeText(this, "Không đọc được file", Toast.LENGTH_SHORT).show()
                        return
                    }
                parseMkhFromText(text)
            }

            // Lọc chỉ lấy MKH bắt đầu bằng PB hoặc PE
            val pbList  = rawLines.filter { it.uppercase().startsWith("PB") }
            val peList  = rawLines.filter { it.uppercase().startsWith("PE") }
            val unknown = rawLines.filter {
                !it.uppercase().startsWith("PB") && !it.uppercase().startsWith("PE")
            }
            // Nhóm PB trước, PE sau để giảm số lần chuyển màn hình loại điện
            val lines = pbList + peList

            if (lines.isEmpty()) {
                Toast.makeText(
                    this,
                    "Không tìm thấy MKH hợp lệ (PB.../PE...) trong file",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            etMkhList.setText(lines.joinToString("\n"))
            val msg = "Đã import ${lines.size} MKH " +
                      "(PB: ${pbList.size}, PE: ${peList.size}" +
                      if (unknown.isNotEmpty()) ", bỏ qua ${unknown.size} dòng lạ)" else ")"
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi đọc file: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("Import", "importFileFromUri error", e)
        }
    }

    /**
     * Parse TXT / CSV: mỗi dòng một MKH hoặc lấy cột đầu tiên nếu CSV.
     */
    private fun parseMkhFromText(text: String): List<String> {
        return text.lines()
            .map { line -> line.split(',', ';').first().trim().trim('"') }
            .filter { it.isNotEmpty() && it.any { ch -> ch.isLetterOrDigit() } }
    }

    /**
     * Parse XLSX (Office Open XML) không cần thư viện ngoài.
     * Đọc cột A của sheet1 → danh sách MKH.
     */
    private fun parseXlsx(uri: Uri): List<String> {
        val sharedStrings = mutableListOf<String>()
        val columnAValues = mutableListOf<String>()

        val entries = mutableMapOf<String, ByteArray>()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "xl/sharedStrings.xml" ||
                        entry.name == "xl/worksheets/sheet1.xml") {
                        entries[entry.name] = zis.readBytes()
                    }
                    entry = zis.nextEntry
                }
            }
        }

        // Parse sharedStrings.xml
        entries["xl/sharedStrings.xml"]?.let { bytes ->
            val parser = Xml.newPullParser()
            parser.setInput(bytes.inputStream(), "UTF-8")
            var inT = false
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> inT = parser.name == "t"
                    XmlPullParser.TEXT      -> if (inT) sharedStrings.add(parser.text ?: "")
                    XmlPullParser.END_TAG   -> if (parser.name == "t") inT = false
                }
                event = parser.next()
            }
        }

        // Parse sheet1.xml — lấy cột A
        entries["xl/worksheets/sheet1.xml"]?.let { bytes ->
            val parser = Xml.newPullParser()
            parser.setInput(bytes.inputStream(), "UTF-8")
            var isColA    = false
            var cellType  = ""
            var inV       = false
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "c" -> {
                            val ref = parser.getAttributeValue(null, "r") ?: ""
                            isColA   = ref.takeWhile { it.isLetter() } == "A"
                            cellType = parser.getAttributeValue(null, "t") ?: ""
                            inV      = false
                        }
                        "v" -> if (isColA) inV = true
                    }
                    XmlPullParser.TEXT -> if (inV && isColA) {
                        val raw = parser.text ?: ""
                        val value = if (cellType == "s") {
                            sharedStrings.getOrElse(raw.toIntOrNull() ?: -1) { "" }
                        } else raw
                        if (value.isNotEmpty()) columnAValues.add(value.trim())
                    }
                    XmlPullParser.END_TAG -> when (parser.name) {
                        "c" -> { isColA = false; cellType = ""; inV = false }
                        "v" -> inV = false
                    }
                }
                event = parser.next()
            }
        }

        return columnAValues
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${BillPaymentAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(":").any { it.equals(service, ignoreCase = true) }
    }
}
