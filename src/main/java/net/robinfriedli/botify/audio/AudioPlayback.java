package net.robinfriedli.botify.audio;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;
import net.robinfriedli.botify.exceptions.TrackLoadingExceptionHandler;

public class AudioPlayback {

    private final Guild guild;
    private final AudioPlayer audioPlayer;
    private final AudioQueue audioQueue;
    private final Logger logger;
    private final ExecutorService executorService;
    private VoiceChannel voiceChannel;
    private MessageChannel communicationChannel;
    // register Track loading Threads here so that they can be interrupted when a different playlist is being played
    private Thread trackLoadingThread;
    private Message lastPlaybackNotification;

    public AudioPlayback(AudioPlayer player, Guild guild) {
        this.guild = guild;
        audioPlayer = player;
        this.logger = LoggerFactory.getLogger(getClass());
        audioQueue = new AudioQueue();
        executorService = Executors.newFixedThreadPool(3, r -> {
            Thread thread = new Thread(r);
            thread.setName("botify track loading thread");
            thread.setUncaughtExceptionHandler(new TrackLoadingExceptionHandler(LoggerFactory.getLogger(getClass()), communicationChannel));
            return thread;
        });
    }

    public boolean isPlaying() {
        return !isPaused() && audioPlayer.getPlayingTrack() != null;
    }

    public void pause() {
        audioPlayer.setPaused(true);
    }

    public void unpause() {
        audioPlayer.setPaused(false);
    }

    public boolean isPaused() {
        return audioPlayer.isPaused();
    }

    public void stop() {
        audioPlayer.stopTrack();
    }

    public Guild getGuild() {
        return guild;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public AudioQueue getAudioQueue() {
        return audioQueue;
    }

    public VoiceChannel getVoiceChannel() {
        return voiceChannel;
    }

    public void setVoiceChannel(VoiceChannel voiceChannel) {
        this.voiceChannel = voiceChannel;
    }

    public MessageChannel getCommunicationChannel() {
        return communicationChannel;
    }

    public void setCommunicationChannel(MessageChannel communicationChannel) {
        this.communicationChannel = communicationChannel;
    }

    public boolean isRepeatOne() {
        return audioQueue.isRepeatOne();
    }

    public void setRepeatOne(boolean repeatOne) {
        audioQueue.setRepeatOne(repeatOne);
    }

    public boolean isRepeatAll() {
        return audioQueue.isRepeatAll();
    }

    public void setRepeatAll(boolean repeatAll) {
        audioQueue.setRepeatAll(repeatAll);
    }

    public boolean isShuffle() {
        return audioQueue.isShuffle();
    }

    public void setShuffle(boolean isShuffle) {
        audioQueue.setShuffle(isShuffle);
    }

    public long getCurrentPositionMs() {
        AudioTrack playingTrack = audioPlayer.getPlayingTrack();
        return playingTrack != null ? playingTrack.getPosition() : 0;
    }

    public void setPosition(long ms) {
        audioPlayer.getPlayingTrack().setPosition(ms);
    }

    public int getVolume() {
        return audioPlayer.getVolume();
    }

    public void setVolume(int volume) {
        audioPlayer.setVolume(volume);
    }

    public void load(Runnable r, boolean singleThread) {
        if (singleThread) {
            Thread thread = new Thread(r);
            thread.setName("botify interruptible track loading thread");
            thread.setUncaughtExceptionHandler(new TrackLoadingExceptionHandler(logger, communicationChannel));
            registerTrackLoading(thread);
            thread.start();
        } else {
            executorService.execute(r);
        }
    }

    public void interruptTrackLoading() {
        if (trackLoadingThread != null && trackLoadingThread.isAlive()) {
            trackLoadingThread.interrupt();
        }
    }

    public void setLastPlaybackNotification(Message message) {
        if (lastPlaybackNotification != null) {
            try {
                lastPlaybackNotification.delete().queue();
            } catch (InsufficientPermissionException e) {
                logger.warn("Cannot delete playback notification message for channel " + communicationChannel, e);
            }
        }
        this.lastPlaybackNotification = message;
    }

    private void registerTrackLoading(Thread thread) {
        if (this.trackLoadingThread != null) {
            interruptTrackLoading();
        }
        this.trackLoadingThread = thread;
    }

}
