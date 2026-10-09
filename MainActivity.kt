package com.example.railonetatkal

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class Passenger(
    val name: String = "",
    val age: String = "",
    val gender: String = "Male",
    val berth: String = "No preference"
) {
    fun json() = JSONObject().put("name", name).put("age", age).put("gender", gender).put("berth", berth)
    fun summary() = "$name | Age: $age | $gender | Berth: $berth"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TatkalAssistantScreen() }
    }
}

@Composable
fun TatkalAssistantScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tatkal_assistant", Context.MODE_PRIVATE) }

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var berth by remember { mutableStateOf("No preference") }
    var from by remember { mutableStateOf(prefs.getString("from", "") ?: "") }
    var to by remember { mutableStateOf(prefs.getString("to", "") ?: "") }
    var train by remember { mutableStateOf(prefs.getString("train", "") ?: "") }
    var travelDate by remember { mutableStateOf(prefs.getString("date", "") ?: "") }
    var travelClass by remember { mutableStateOf(prefs.getString("class", "3A") ?: "3A") }
    var quota by remember { mutableStateOf(prefs.getString("quota", "Tatkal") ?: "Tatkal") }
    var savedPassengers by remember { mutableStateOf(loadPassengers(prefs.getString("passengers", "[]") ?: "[]")) }
    var countdown by remember { mutableStateOf("") }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    fun saveJourney() {
        prefs.edit()
            .putString("from", from).putString("to", to).putString("train", train)
            .putString("date", travelDate).putString("class", travelClass).putString("quota", quota)
            .apply()
        toast("Journey details saved on this device")
    }

    fun savePassenger() {
        if (name.isBlank() || age.isBlank()) {
            toast("Passenger name and age are required")
            return
        }
        val p = Passenger(name.trim(), age.trim(), gender, berth)
        savedPassengers = (savedPassengers.filterNot { it.name.equals(p.name, true) } + p)
        prefs.edit().putString("passengers", passengersJson(savedPassengers)).apply()
        toast("Passenger saved on this device")
    }

    fun openRailOne() {
        // Official RailOne Android package listed by Centre for Railway Information Systems.
        val launch = context.packageManager.getLaunchIntentForPackage("org.cris.aikyam")
        if (launch != null) {
            context.startActivity(launch)
        } else {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=org.cris.aikyam")))
        }
    }

    fun copyPassenger(p: Passenger) {
        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clip.setPrimaryClip(ClipData.newPlainText("Passenger details", p.summary()))
        toast("Passenger details copied. Paste only where appropriate.")
    }

    fun calculateCountdown() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val ac = travelClass == "1A" || travelClass == "2A" || travelClass == "3A" || travelClass == "CC" || travelClass == "EC"
        val time = if (ac) "10:00" else "11:00"
        val target = try { sdf.parse("${travelDate.trim()} $time") } catch (_: Exception) { null }
        if (target == null) {
            countdown = "Enter journey date as YYYY-MM-DD first."
            return
        }
        val diff = target.time - System.currentTimeMillis()
        countdown = if (diff <= 0) "Scheduled Tatkal opening time has passed for this date. Confirm current railway rules." else {
            val hours = TimeUnit.MILLISECONDS.toHours(diff)
            val mins = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            val secs = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
            "Approx. time until $time: %02d:%02d:%02d".format(hours, mins, secs)
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Tatkal Assistant", style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold)
            Text("Prepare details quickly; final booking stays inside official RailOne/railway flow.")
            Card(elevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Journey details", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
                    Field("From station code/name", from) { from = it }
                    Field("To station code/name", to) { to = it }
                    Field("Train number/name (optional)", train) { train = it }
                    Field("Journey date (YYYY-MM-DD)", travelDate) { travelDate = it }
                    Picker("Class", travelClass, listOf("SL", "2S", "3A", "2A", "1A", "CC", "EC", "3E")) { travelClass = it }
                    Picker("Quota", quota, listOf("Tatkal", "Premium Tatkal")) { quota = it }
                    Button(onClick = { saveJourney() }, modifier = Modifier.fillMaxWidth()) { Text("Save journey details") }
                    OutlinedButton(onClick = { calculateCountdown() }, modifier = Modifier.fillMaxWidth()) { Text("Check Tatkal countdown") }
                    if (countdown.isNotBlank()) Text(countdown)
                }
            }

            Card(elevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Passenger profile", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
                    Field("Full name (as per ID)", name) { name = it }
                    Field("Age", age, KeyboardType.Number) { age = it }
                    Picker("Gender", gender, listOf("Male", "Female", "Transgender")) { gender = it }
                    Picker("Berth preference", berth, listOf("No preference", "Lower", "Middle", "Upper", "Side Lower", "Side Upper")) { berth = it }
                    Button(onClick = { savePassenger() }, modifier = Modifier.fillMaxWidth()) { Text("Save passenger") }
                    Text("Saved profiles are stored locally on this phone. Do not store passwords, OTPs, Aadhaar numbers, or payment details here.")
                }
            }

            Card(elevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Saved passengers (${savedPassengers.size})", style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
                    if (savedPassengers.isEmpty()) Text("No saved passengers yet.")
                    savedPassengers.forEachIndexed { index, p ->
                        Divider()
                        Text(p.summary(), fontWeight = FontWeight.Medium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                name = p.name; age = p.age; gender = p.gender; berth = p.berth
                                toast("Passenger loaded into form")
                            }) { Text("Load") }
                            OutlinedButton(onClick = { copyPassenger(p) }) { Text("Copy") }
                            OutlinedButton(onClick = {
                                savedPassengers = savedPassengers.filterIndexed { i, _ -> i != index }
                                prefs.edit().putString("passengers", passengersJson(savedPassengers)).apply()
                            }) { Text("Delete") }
                        }
                    }
                }
            }

            Button(onClick = { openRailOne() }, modifier = Modifier.fillMaxWidth()) {
                Text("Open RailOne")
            }
            Text(
                "Important: This helper does not automate RailOne screens, bypass CAPTCHA/OTP, reserve inventory, or submit payment. Verify all details in the official app before booking.",
                style = MaterialTheme.typography.caption
            )
            Text("RailOne app listing: Centre for Railway Information Systems", style = MaterialTheme.typography.caption)
        }
    }
}

@Composable
fun Field(label: String, value: String, keyboardType: KeyboardType = KeyboardType.Text, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValue, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
fun Picker(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.caption)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(onClick = { onSelect(option); expanded = false }) { Text(option) }
                }
            }
        }
    }
}

fun loadPassengers(raw: String): List<Passenger> {
    return try {
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Passenger(
                obj.optString("name"), obj.optString("age"),
                obj.optString("gender", "Male"), obj.optString("berth", "No preference")
            )
        }
    } catch (_: Exception) { emptyList() }
}

fun passengersJson(passengers: List<Passenger>): String {
    val array = JSONArray()
    passengers.forEach { array.put(it.json()) }
    return array.toString()
}
