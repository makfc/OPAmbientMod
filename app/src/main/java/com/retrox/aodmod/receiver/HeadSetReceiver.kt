package com.retrox.aodmod.receiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import com.retrox.aodmod.MainHook
import com.retrox.aodmod.extensions.LiveEvent
import android.support.v4.content.ContextCompat.getSystemService



class HeadSetReceiver : BroadcastReceiver() {

    enum class Connection(val value: Int) {
        DISCONNECTED(0),
        CONNECTING(1),
        CONNECTED(2),
        DISCONNECTING(3)
    }

    sealed class ConnectionState {
        data class HeadSetConnection(val connection: Connection) : ConnectionState()
        data class BlueToothConnection(
            val connection: Connection,
            val deviceName: String,
            val device: BluetoothDevice
        ) : ConnectionState()
        data class VolumeChange(val volValue : Int) : ConnectionState()
    }


    companion object {
        val headSetConnectLiveEvent = LiveEvent<ConnectionState>()
    }

    // 这个东西虽然没蓝牙权限 但是却可以拿到
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        MainHook.logD("音乐连接状态: $action")
        if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED == action) {
            val prev = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1)
            val current = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
            val connection = when (current) {
                0 -> Connection.DISCONNECTED
                1 -> Connection.CONNECTING
                2 -> Connection.CONNECTED
                3 -> Connection.DISCONNECTING
                else -> null
            } ?: return
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

            headSetConnectLiveEvent.postValue(ConnectionState.BlueToothConnection(connection, device.name, device))
            MainHook.logD("蓝牙连接状态变化 $prev $current ${device.name}")
        } else if (Intent.ACTION_HEADSET_PLUG == action) {
            if (intent.hasExtra("state")) {
                val state = intent.getIntExtra("state", 0)
                if (state == 1) {
                    MainHook.logD("插入耳机")
                } else if (state == 0) {
                    MainHook.logD("拔出耳机")
                }

                val connection = when (state) {
                    0 -> Connection.DISCONNECTED
                    1 -> Connection.CONNECTED
                    else -> null
                } ?: return

                headSetConnectLiveEvent.postValue(ConnectionState.HeadSetConnection(connection))
            }
        } else if ("android.media.VOLUME_CHANGED_ACTION" == action) {
            val streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
            if (streamType == AudioManager.STREAM_MUSIC) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) // 0 min - 30 max
                headSetConnectLiveEvent.postValue(ConnectionState.VolumeChange(currentVolume))
            }
        }
    }
}