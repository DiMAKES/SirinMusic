package ru.rainedev.sirinmusic.playback

import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.Player
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture

/** ExoPlayer contains one stream; next/previous are owned by the server session. */
@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
class SessionPlayer(private val controller: PlaybackController) : ForwardingSimpleBasePlayer(controller.player) {
    override fun getState(): State {
        val state = super.getState()
        val commands = state.availableCommands.buildUpon()
        if (controller.player.mediaItemCount > 0) {
            commands.add(Player.COMMAND_SEEK_TO_NEXT).add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS).add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
        }
        return state.buildUpon().setAvailableCommands(commands.build()).build()
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        val previous = seekCommand == Player.COMMAND_SEEK_TO_PREVIOUS || seekCommand == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
        if (previous || seekCommand == Player.COMMAND_SEEK_TO_NEXT || seekCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) {
            val future = SettableFuture.create<Void>()
            (if (previous) controller.previous() else controller.skip()).invokeOnCompletion { error ->
                if (error == null) future.set(null) else future.setException(error)
            }
            return future
        }
        return super.handleSeek(mediaItemIndex, positionMs, seekCommand)
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        if (playWhenReady && controller.ui.value.ended) {
            controller.togglePlay()
            return Futures.immediateVoidFuture()
        }
        return super.handleSetPlayWhenReady(playWhenReady)
    }

    override fun handleStop(): ListenableFuture<*> {
        controller.stop()
        return Futures.immediateVoidFuture()
    }
}
