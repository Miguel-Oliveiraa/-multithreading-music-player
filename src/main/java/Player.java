import javazoom.jl.decoder.*;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;
import support.PlayerWindow;
import support.Song;

import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Semaphore;

public class Player {
    /**
     * The MPEG audio bitstream.
     */
    private Bitstream bitstream;
    /**
     * The MPEG audio decoder.
     */
    private Decoder decoder;
    /**
     * The AudioDevice where audio samples are written to.
     */
    private AudioDevice device;

    private PlayerWindow window;

    private MusicPlayerThread musicPlayerThread;
    private boolean shuffled;
    static private final ArrayList<Song> playlist = new ArrayList<>();
    static private final ArrayList<Song> copy = new ArrayList<>();
    private Song currentSong;
    static private boolean neverPlayed = true;
    // UI info
    static String[][] queue = new String[0][];

    // Região Critica
    static class Shared{
        // Songs playlist
        private static int index;

        // States
        private static int currentFrame = 0;
        private static boolean isPlaying = false;
        private static boolean playPause = false;
        private static boolean looping = false;
        private static boolean playNext;
        private static boolean playPrevious;
        private static boolean scrubberEvent;
        private static int currentTime;
        private static boolean playPauseLastState;
    }

    class MusicPlayerThread extends Thread {
        Semaphore sem;
        public MusicPlayerThread(Semaphore sem) {
            this.sem = sem;
        }

        @Override
        public void run() {
            try {
                sem.acquire();

                // Start music state
                Shared.isPlaying = true;
                Shared.playPause = true;
                Shared.currentFrame = 0;

                // Get song
                getSong();
                // Initialize player objects
                initializePlayerObjects();

                // Verify scrubber event
                verifyScrubber();

                // Update UI
                updateUI();

                while (Shared.isPlaying){
                    try {
                        Thread.onSpinWait();
                        if (Shared.playPause) {
                            // Set UI states
                            EventQueue.invokeLater(()->{
                                window.setEnabledScrubber(true);
                                window.setTime((Shared.currentFrame++ * (int) currentSong.getMsPerFrame()), (int) currentSong.getMsLength());
                                window.setPlayPauseButtonIcon((Shared.isPlaying) ? 1 : 0);
                                window.setEnabledPlayPauseButton(true);
                                window.setEnabledStopButton(true);
                            });

                            if(!playNextFrame()) {
                                // If not last music play next
                                if (Shared.looping || Shared.index < playlist.size()-1) {
                                    musicPlayerThread.stopThread();
                                    Shared.playNext = true;
                                    Semaphore sem = new Semaphore(1);
                                    musicPlayerThread = new MusicPlayerThread(sem);
                                    musicPlayerThread.start();
                                } else {
                                    musicPlayerThread.stopThread();
                                }
                            }
                        }
                    }
                    catch (JavaLayerException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            } catch (InterruptedException exc) {
                throw new RuntimeException(exc);
            }
        }

        public void stopThread(){
            stopMusic(device, bitstream, window);
            sem.release();
            this.interrupt();
        }

        private void stopMusic(AudioDevice device, Bitstream bitstream, PlayerWindow window) {
            Shared.isPlaying = false;
            // Always close bitstream and device
            boolean cleanObjects = true;
            while (cleanObjects) {
                try {
                    bitstream.close();
                    device.close();
                } catch (BitstreamException ex) {
                    throw new RuntimeException(ex);
                }
                cleanObjects = false;
            }
            EventQueue.invokeLater(window::resetMiniPlayer);
        }

        private void getSong(){
            if (Shared.playNext) Shared.index = ++Shared.index%playlist.size();
            else if (Shared.playPrevious) Shared.index--;
            currentSong = playlist.get(Shared.index);
            Shared.playNext = false;
            Shared.playPrevious = false;
        }

        private void initializePlayerObjects(){
            try {
                device = FactoryRegistry.systemRegistry().createAudioDevice();
                decoder = new Decoder();
                device.open(decoder);
                bitstream = new Bitstream(currentSong.getBufferedInputStream());
            } catch (JavaLayerException | FileNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }

        private void verifyScrubber(){
            if (Shared.scrubberEvent) {
                try {
                    // Jump to scrubber time
                    skipToFrame(Shared.currentTime);
                } catch (BitstreamException ex) {
                    throw new RuntimeException(ex);
                }
                // Set UI states
                Shared.playPause = Shared.playPauseLastState;
                EventQueue.invokeLater(() -> {
                    window.setEnabledScrubber(true);
                    window.setTime((Shared.currentFrame++ * (int) currentSong.getMsPerFrame()), (int) currentSong.getMsLength());
                    window.setPlayPauseButtonIcon((Shared.playPause) ? 1 : 0);
                    window.setEnabledPlayPauseButton(true);
                    window.setEnabledStopButton(true);
                });
            }
            Shared.scrubberEvent = false;
        }

        private void updateUI(){
            EventQueue.invokeLater(() -> {
                if (Shared.isPlaying) {
                    window.setPlayingSongInfo(currentSong.getTitle(), currentSong.getAlbum(), currentSong.getArtist());
                    // Verify next and previous button
                    if (Shared.looping) {
                        window.setEnabledNextButton(true);
                        window.setEnabledPreviousButton(Shared.index != 0);
                    } else {
                        window.setEnabledNextButton(Shared.index != playlist.size() - 1);
                        window.setEnabledPreviousButton(Shared.index != 0);
                    }
                }
            });
        }
    }

    private final ActionListener buttonListenerPlayNow = e -> {
        if (neverPlayed) neverPlayed = false;
        // Mata a Thread para acessar a região critica
        if (musicPlayerThread != null && musicPlayerThread.isAlive()){
            musicPlayerThread.stopThread();
        }

        Shared.index = window.getSelectedSongIndex();
        Semaphore sem = new Semaphore(1);
        musicPlayerThread = new MusicPlayerThread(sem);
        musicPlayerThread.start();
    };

    private final ActionListener buttonListenerRemove = e -> {
        int selectedIndex = window.getSelectedSongIndex();
        // Toca a proxima musica se tiver tocando (exceto se for a ultima sem loop)
        if (selectedIndex == Shared.index && Shared.isPlaying) {
            musicPlayerThread.stopThread();
            Shared.index = window.getSelectedSongIndex();
            if (Shared.index != playlist.size()-1 || Shared.looping) {
                if (Shared.index == playlist.size()-1 && Shared.looping) Shared.index = 0;
                Semaphore sem = new Semaphore(1);
                musicPlayerThread = new MusicPlayerThread(sem);
                musicPlayerThread.start();
            }
        }
        // Caso a musica for anterior a atual
        if (selectedIndex < Shared.index) {
            Shared.index--;
        }
        // Remova a musica da playlist e atualiza a UI
        copy.remove(playlist.get(selectedIndex));
        playlist.remove(selectedIndex);
        queue = removeMusic(queue, selectedIndex);
        EventQueue.invokeLater(() -> {
            if (queue.length == 0) {
                window.setEnabledShuffleButton(false);
                window.setEnabledLoopButton(false);
            }
            window.setQueueList(queue);
        });
        if (Shared.isPlaying) musicPlayerThread.updateUI();
    };

    private final ActionListener buttonListenerAddSong = e -> {
        Song newMusic = window.openFileChooser();
        // Add song to playlist and update UI
        playlist.add(newMusic);
        copy.add(newMusic);
        String[] musicInfo = newMusic.getDisplayInfo();
        queue = addMusic(queue, musicInfo);
        EventQueue.invokeLater(() -> {
            window.setQueueList(queue);
            window.setEnabledShuffleButton(true);
            window.setEnabledLoopButton(true);
        });
        if (Shared.isPlaying) musicPlayerThread.updateUI();
    };
    private final ActionListener buttonListenerPlayPause = e -> {
        Shared.playPause = !Shared.playPause;
        EventQueue.invokeLater(() -> window.setPlayPauseButtonIcon((Shared.playPause) ? 1 : 0));
    };
    private final ActionListener buttonListenerStop = e -> musicPlayerThread.stopThread();
    private final ActionListener buttonListenerNext = e -> {
        musicPlayerThread.stopThread();
        Shared.playNext = true;
        Semaphore sem = new Semaphore(1);
        musicPlayerThread = new MusicPlayerThread(sem);
        musicPlayerThread.start();
    };
    private final ActionListener buttonListenerPrevious = e -> {
        // Kill the Thread to access a critical region
        musicPlayerThread.stopThread();
        Shared.playPrevious = true;
        Semaphore sem = new Semaphore(1);
        musicPlayerThread = new MusicPlayerThread(sem);
        musicPlayerThread.start();
    };
    private final ActionListener buttonListenerShuffle = e -> {
        if (shuffled) {
            Shared.index = copy.indexOf(playlist.get(Shared.index));
            playlist.clear();
            playlist.addAll(copy);
        } else {
            copy.clear();
            copy.addAll(playlist);
            if (Shared.isPlaying) {
                playlist.remove(Shared.index);
                Collections.shuffle(playlist);
                playlist.add(0, copy.get(Shared.index));
            } else {
                Collections.shuffle(playlist);
            }
            Shared.index = 0;
        }
        shuffled = !shuffled;

        // Update list
        updateQueue(queue);
        EventQueue.invokeLater(() -> window.setQueueList(queue));
        // Only update UI if there's something to update
        if (!neverPlayed) musicPlayerThread.updateUI();
    };

    private final ActionListener buttonListenerLoop = e -> {
        Shared.looping = !Shared.looping;
        // Only update UI if there's something to update
        if (!neverPlayed) musicPlayerThread.updateUI();
    };

    private final MouseInputAdapter scrubberMouseInputAdapter = new MouseInputAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
            // Set new time and start new Thread
            EventQueue.invokeLater(() -> window.setTime(Shared.currentTime* (int) currentSong.getMsPerFrame(),(int) currentSong.getMsLength()));
            musicPlayerThread.stopThread();
            Semaphore sem = new Semaphore(1);
            musicPlayerThread = new MusicPlayerThread(sem);
            musicPlayerThread.start();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            // Set scrubber states
            Shared.scrubberEvent = true;
            Shared.playPauseLastState = Shared.playPause;
            Shared.isPlaying = false;
            EventQueue.invokeLater(() -> window.setPlayPauseButtonIcon(0));
            Shared.currentTime = (int) (window.getScrubberValue()/currentSong.getMsPerFrame());
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Shared.currentTime = (int) (window.getScrubberValue()/currentSong.getMsPerFrame());
            EventQueue.invokeLater(() -> window.setTime(Shared.currentTime* (int) currentSong.getMsPerFrame(),(int) currentSong.getMsLength()));
        }
    };

    public Player() {
        EventQueue.invokeLater(() -> window = new PlayerWindow(
                "Music Player Lock",
                queue,
                buttonListenerPlayNow,
                buttonListenerRemove,
                buttonListenerAddSong,
                buttonListenerShuffle,
                buttonListenerPrevious,
                buttonListenerPlayPause,
                buttonListenerStop,
                buttonListenerNext,
                buttonListenerLoop,
                scrubberMouseInputAdapter)
        );
    }

    //<editor-fold desc="Essential">
    /**
     * @return False if there are no more frames to play.
     */
    private boolean playNextFrame() throws JavaLayerException {
        if (device != null) {
            Header h = bitstream.readFrame();
            if (h == null) return false;

            SampleBuffer output = (SampleBuffer) decoder.decodeFrame(h, bitstream);
            device.write(output.getBuffer(), 0, output.getBufferLength());
            bitstream.closeFrame();
        }
        return true;
    }

    /**
     * @return False if there are no more frames to skip.
     */
    private boolean skipNextFrame() throws BitstreamException {
        Header h = bitstream.readFrame();
        if (h == null) return false;
        bitstream.closeFrame();
        Shared.currentFrame++;
        return true;
    }

    /**
     * Skips bitstream to the target frame if the new frame is higher than the current one.
     *
     * @param newFrame Frame to skip to.
     */
    private void skipToFrame(int newFrame) throws BitstreamException {
        int framesToSkip = newFrame - Shared.currentFrame;
        boolean condition = true;
        while (framesToSkip-- > 0 && condition) condition = skipNextFrame();
    }
    //</editor-fold>

    private static String[][] removeMusic(String[][] queue, int removedIndex){
        String[][] newQueue = new String[queue.length-1][];
        // Two pointers queue update
        for (int i = 0, j = 0; i < queue.length; i++) {
            if (i != removedIndex) {
                newQueue[j] = queue[i];
                j++;
            }
        }
        return newQueue;
    }
    
    private static String[][] addMusic(String[][] queue, String[] musicInfo) {
        String[][] newQueue = new String[queue.length+1][];
        System.arraycopy(queue, 0, newQueue, 0, queue.length);
        newQueue[queue.length] = musicInfo;
        return newQueue;
    }

    private static void updateQueue(String[][] queue) {
        for (int i = 0; i < playlist.size(); i++) queue[i] = Player.playlist.get(i).getDisplayInfo();
    }
}
