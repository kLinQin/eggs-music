package com.eggs.musicplayer

import android.app.Application
import com.eggs.musicplayer.player.EqualizerController
import kotlinx.coroutines.flow.StateFlow

class App : Application() {
    val equalizerController: EqualizerController by lazy { EqualizerController(this) }


    val equalizerState: StateFlow<com.eggs.musicplayer.player.EqState>
        get() = equalizerController.state
}
