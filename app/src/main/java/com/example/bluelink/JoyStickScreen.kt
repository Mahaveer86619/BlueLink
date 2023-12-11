package com.example.bluelink

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bluelink.databinding.JoystickScreenBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream
import java.util.*

class JoyStickScreen : AppCompatActivity() {

    private lateinit var binding: JoystickScreenBinding
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var outputStream: OutputStream
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var connected = false

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this@JoyStickScreen, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = JoystickScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Change status bar color
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = getColor(R.color.deep_green)

        // Change navigation bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = getColor(R.color.deep_green)
        }

        binding.tvData.text = "Data sent = X"

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), 1)
            }
            startActivityForResult(enableBtIntent, 1)
            Toast.makeText(this, "Enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        val hc05Device = pairedDevices.find { it.name == "HC-05" }

        if (hc05Device != null) {
            connectToDevice(hc05Device)
        } else {
            Log.d("Dev", "HC-05 not found in paired devices")
            Toast.makeText(this, "HC-05 not found", Toast.LENGTH_SHORT).show()
        }

        if (connected) {
            binding.btnConnect.setImageResource(R.drawable.disconnect)
        } else {
            binding.btnConnect.setImageResource(R.drawable.connect)
        }

        binding.IBVerticalUp.setOnClickListener { sendData("U") }
        binding.IBVerticalDown.setOnClickListener { sendData("D") }
        binding.IBVerticalNull.setOnClickListener { sendData("Y") }
        binding.IBXyForward.setOnClickListener { sendData("F") }
        binding.IBXyLeft.setOnClickListener { sendData("L") }
        binding.IBXyRight.setOnClickListener { sendData("R") }
        binding.IBXyBackward.setOnClickListener { sendData("B") }
        binding.IBXyNull.setOnClickListener { sendData("X") }
        binding.btnClawClose.setOnClickListener { sendData("C") }
        binding.btnClawOpen.setOnClickListener { sendData("O") }
        binding.btnConnect.setOnClickListener {
            if (connected) {
                if (hc05Device != null) {
                    disconnectFromDevice()
                    Log.d("Dev", "Connection closing")
                }
            } else {
                if (hc05Device != null) {
                    Log.d("Dev", "Connection restoring")
                    connectToDevice(hc05Device)
                } else {
                    Log.d("Dev", "HC-05 not found in paired devices")
                    Toast.makeText(this, "HC-05 not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun disconnectFromDevice() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                bluetoothSocket.close()
                outputStream.close()
                connected = false
                Log.d("Dev", "Disconnected from HC-05")
                runOnUiThread {
                    Toast.makeText(this@JoyStickScreen, "Disconnected from HC-05", Toast.LENGTH_SHORT).show()
                }
                binding.btnConnect.setImageResource(R.drawable.connect)
            } catch (e: IOException) {
                Log.e("Dev", "Error disconnecting from HC-05: $e")
                runOnUiThread {
                    Toast.makeText(this@JoyStickScreen, "Error disconnecting from HC-05", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (ActivityCompat.checkSelfPermission(
                        this@JoyStickScreen,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return@launch
                }
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket.connect()
                outputStream = bluetoothSocket.outputStream
                connected = true
                Log.d("Dev", "Connected to HC-05")
                runOnUiThread {
                    Toast.makeText(this@JoyStickScreen, "Connected to HC-05", Toast.LENGTH_SHORT).show()
                }
                binding.btnConnect.setImageResource(R.drawable.disconnect)
            } catch (e: IOException) {
                Log.d("Dev", "Error connecting to HC-05: $e")
                runOnUiThread {
                    Toast.makeText(this@JoyStickScreen, "Error connecting to HC-05", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            outputStream.close()
            bluetoothSocket.close()
            Log.d("Dev", "Connection closed")
        } catch (e: IOException) {
            Log.d("Dev", "Error closing connection: $e")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun sendData(data: String) {
        if (::outputStream.isInitialized) {
            try {
                outputStream.write(data.toByteArray())
                binding.tvData.text = "Data sent = $data"
                Log.d("Dev", "Sent data: $data")
                Thread.sleep(100)
            } catch (e: IOException) {
                Log.d("Dev", "Error sending data: $e")
                Toast.makeText(this, "Error sending data", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Bluetooth not connected", Toast.LENGTH_SHORT).show()
        }
    }
}