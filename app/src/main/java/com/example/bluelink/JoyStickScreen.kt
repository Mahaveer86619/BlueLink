package com.example.bluelink

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bluelink.databinding.JoystickScreenBinding
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class JoyStickScreen : AppCompatActivity() {

    private lateinit var binding: JoystickScreenBinding

    val bluetoothEnable = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    private lateinit var permissionlauncher: ActivityResultLauncher<Array<String>>
    private var isBluetoothConnectGranted = false

    private lateinit var bluetoothDevice: BluetoothDevice
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var outputStream: OutputStream

    // UUID of HC-05
    var uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    var pressed = false


    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = JoystickScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvData.text = "Data sent = X"


        // Bluetooth
        // Get Bluetooth adapter
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        Log.d("Dev", "Starting bluetooth connection")

        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled) {
            // Request user to enable Bluetooth
            startActivityForResult(bluetoothEnable, 1)
            Toast.makeText(this@JoyStickScreen, "Enable Bluetooth", Toast.LENGTH_SHORT).show()
        }

        // Get paired devices
        // check if permission is granted to
        isBluetoothConnectGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

        val permissionRequest: MutableList<String> = ArrayList()

        if (!isBluetoothConnectGranted) {
            permissionRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (permissionRequest.isNotEmpty()) {
            permissionlauncher.launch(permissionRequest.toTypedArray())
        }

        permissionlauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){permission ->
            isBluetoothConnectGranted = permission[Manifest.permission.BLUETOOTH_CONNECT]?: isBluetoothConnectGranted
        }


        // gets all paired devices
        val pairedDevices = bluetoothAdapter.bondedDevices

        Log.d("Dev", "Listing Paired devices")
        pairedDevices.forEach{device ->
            Log.d("Dev", "Name = ${device.name}, MAC = ${device.address}")

            if (device.name == "HC-05") {
                bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.address)
            }
        }

        // connecting to HC-05 in another thread
//        Thread {
//            kotlin.run {
//                if (isBluetoothConnectGranted) {
//                    if (Build.VERSION.SDK_INT > 31) {
//                        permissionlauncher.launch(permissionRequest.toTypedArray())
//                        return@run
//                    }
//                }
//
//                try {
//                    bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid)
//                    bluetoothSocket.connect()
//
//                    outputStream = bluetoothSocket.outputStream
//                    Log.d("Dev", "Connected to HC-05")
//
//                    runOnUiThread{
//                        kotlin.run {
//                            Toast.makeText(this@JoyStickScreen, "Connected to HC-05", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                } catch (e: IOException) {
//                    Log.d("Dev", "$e error while connecting to HC-05")
//                    runOnUiThread{
//                        kotlin.run {
//                            Toast.makeText(this@JoyStickScreen, "||$e|| error while connecting to HC-05", Toast.LENGTH_SHORT).show()
//                        }
//                    }
//                    throw RuntimeException(e)
//                }
//            }
//        }.start()
        if (isBluetoothConnectGranted) {
            if (Build.VERSION.SDK_INT > 31) {
                permissionlauncher.launch(permissionRequest.toTypedArray())
            }
        }

        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket.connect()

            outputStream = bluetoothSocket.outputStream
            Log.d("Dev", "Connected to HC-05")

            runOnUiThread{
                kotlin.run {
                    Toast.makeText(this@JoyStickScreen, "Connected to HC-05", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: IOException) {
            Log.d("Dev", "$e error while connecting to HC-05")
            runOnUiThread{
                kotlin.run {
                    Toast.makeText(this@JoyStickScreen, "error while connecting to HC-05", Toast.LENGTH_LONG).show()
                }
            }
            throw RuntimeException(e)
        }




        // forward backward joystick
        binding.IBVerticalUp.setOnClickListener {
            binding.tvData.text = "Data sent = U"

            try {
                outputStream.write("U".toByteArray())
                Log.d("Dev", "sent U")
            } catch (e: IOException) {
                Log.d("Dev", "error in sending output stream")
                Toast.makeText(this@JoyStickScreen, "error in sending output stream", Toast.LENGTH_LONG).show()
                throw RuntimeException(e)
            }
            Thread.sleep(200)
        }
        binding.IBVerticalDown.setOnClickListener {
            binding.tvData.text = "Data sent = D"

            try {
                outputStream.write("D".toByteArray())
                Log.d("Dev", "sent D")
            } catch (e: IOException) {
                Log.d("Dev", "error in sending output stream")
                Toast.makeText(this@JoyStickScreen, "error in sending output stream", Toast.LENGTH_LONG).show()
                throw RuntimeException(e)
            }
            Thread.sleep(200)
        }
        binding.IBVerticalNull.setOnClickListener {
            binding.tvData.text = "Data sent = Y"

            try {
                outputStream.write("Y".toByteArray())
                Log.d("Dev", "sent Y")
            } catch (e: IOException) {
                Log.d("Dev", "error in sending output stream")
                Toast.makeText(this@JoyStickScreen, "error in sending output stream", Toast.LENGTH_LONG).show()
                throw RuntimeException(e)
            }
            Thread.sleep(200)
        }

        // left right and up down joystick
        binding.IBXyForward.setOnClickListener {
            binding.tvData.text = "Data sent = F"

            try {
                outputStream.write("F".toByteArray())
                Log.d("Dev", "sent F")
            } catch (e: IOException) {
                Log.d("Dev", "error in sending output stream")
                Toast.makeText(this@JoyStickScreen, "error in sending output stream", Toast.LENGTH_LONG).show()
                throw RuntimeException(e)
            }
            Thread.sleep(200)
        }
        binding.IBXyLeft.setOnClickListener {
            binding.tvData.text = "Data sent = L"

            try {
                outputStream.write("L".toByteArray())
                Log.d("Dev", "sent L")
            } catch (e: IOException) {
                Log.d("Dev", "error in sending output stream")
                Toast.makeText(this@JoyStickScreen, "error in sending output stream", Toast.LENGTH_LONG).show()
                throw RuntimeException(e)
            }
            Thread.sleep(200)
        }
        binding.IBXyRight.setOnClickListener {
            binding.tvData.text = "Data sent = R"

            try {
                outputStream.write("R".toByteArray())
                Log.d("Dev", "sent R")
            } catch (e: IOException) {
                Log.d("Dev", "error in sending output stream")
                Toast.makeText(this@JoyStickScreen, "error in sending output stream", Toast.LENGTH_LONG).show()
                throw RuntimeException(e)
            }
            Thread.sleep(200)
        }
        binding.IBXyBackward.setOnClickListener {
            binding.tvData.text = "Data sent = B"

            try {
                outputStream.write("B".toByteArray())
                Log.d("Dev", "sent B")
            } catch (e: IOException) {
                Log.d("Dev", "error in sending output stream")
                Toast.makeText(this@JoyStickScreen, "error in sending output stream", Toast.LENGTH_LONG).show()
                throw RuntimeException(e)
            }
            Thread.sleep(200)
        }
        binding.IBXyNull.setOnClickListener {
            binding.tvData.text = "Data sent = X"

            try {
                outputStream.write("X".toByteArray())
                Log.d("Dev", "sent X")
            } catch (e: IOException) {
                Log.d("Dev", "error in sending output stream")
                Toast.makeText(this@JoyStickScreen, "error in sending output stream", Toast.LENGTH_LONG).show()
                throw RuntimeException(e)
            }
            Thread.sleep(200)
        }

        // Claw toggle
        binding.btnClawClose.setOnClickListener {
            binding.tvData.text = "Data sent = C"

            try {
                outputStream.write("C".toByteArray())
                Log.d("Dev", "sent C")
            } catch (e: IOException) {
                Log.d("Dev", "error in sending output stream")
                Toast.makeText(this@JoyStickScreen, "error in sending output stream", Toast.LENGTH_LONG).show()
                throw RuntimeException(e)
            }
            Thread.sleep(200)
        }

        binding.btnClawOpen.setOnClickListener {
            binding.tvData.text = "Data sent = O"

            try {
                outputStream.write("O".toByteArray())
                Log.d("Dev", "sent O")
            } catch (e: IOException) {
                Log.d("Dev", "error in sending output stream")
                Toast.makeText(this@JoyStickScreen, "error in sending output stream", Toast.LENGTH_LONG).show()
                throw RuntimeException(e)
            }
            Thread.sleep(200)
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            bluetoothSocket.close()
            Log.d("Dev", "Connection closed")
        } catch (e: IOException) {
            Log.d("Dev", "error while closing the connection. $e")
        }

    }
}
