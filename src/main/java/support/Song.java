package support;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.UUID;

/**
 * An abstraction of an MP3 file, with methods that return important display information
 * such as title, album, artist, and information to assist playing the file
 * such as length, number of frames, and other. Also contains a method that returns a
 * {@link java.io.BufferedInputStream} of the file.
 *
 * @see BufferedInputStream
 */
public class Song {
    private final String uuid;
    private final String title;
    private final String album;
    private final String artist;
    private final String year;
    private final float msLength;
    private final String strLength;
    private final String filePath;
    private final int fileSize;
    private final int numFrames;
    private final float msPerFrame;

    /**
     * Creates a deep copy of the specified {@link Song}.
     * This method is NOT THREAD SAFE!
     *
     * @param song {@link Song} to copy.
     */
    public Song(Song song) {
        this.uuid = song.getUuid();
        this.title = song.getTitle();
        this.album = song.getAlbum();
        this.artist = song.getArtist();
        this.year = song.getYear();
        this.strLength = song.getStrLength();
        this.msLength = song.getMsLength();
        filePath = song.getFilePath();
        fileSize = song.getFileSize();
        numFrames = song.getNumFrames();
        msPerFrame = song.getMsPerFrame();
    }

    /**
     * Constructs a newly allocated {@link Song} object.
     *
     * @param uuid       {@link UUID}
     * @param title      Song title.
     * @param album      Song album.
     * @param artist     Song artist.
     * @param year       Song year.
     * @param strLength  Length as String in the format 00:00:00
     * @param msLength   Length in milliseconds.
     * @param filePath   File path.
     * @param fileSize   File size in bytes.
     * @param numFrames  Number of MP3 frames.
     * @param msPerFrame Number of milliseconds per MP3 frame.
     *
     * @see UUID
     */
    public Song(String uuid, String title, String album, String artist, String year, String strLength, float msLength, String filePath, int fileSize, int numFrames, float msPerFrame) {
        this.uuid = uuid;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.year = year;
        this.strLength = strLength;
        this.msLength = msLength;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.numFrames = numFrames;
        this.msPerFrame = msPerFrame;
    }

    /**
     * Returns an array with important information to be displayed about a song.
     * The information is organized in the array as:<br>
     * [0] - Title<br>
     * [1] - Album<br>
     * [2] - Artist<br>
     * [3] - Year<br>
     * [4] - Time (formatted as 00:00)<br>
     * [5] - {@link UUID}<br>
     * This method is NOT THREAD SAFE!
     *
     * @return Returns an array with important information to be displayed about a song.
     * @see UUID
     */
    public String[] getDisplayInfo() {
        String[] copy = new String[6];
        copy[0] = this.getTitle();
        copy[1] = this.getAlbum();
        copy[2] = this.getArtist();
        copy[3] = this.getYear();
        copy[4] = this.getStrLength();
        copy[5] = this.getUuid();
        return copy;
    }

    public String getUuid() {
        return uuid;
    }

    public String getTitle() {
        return title;
    }

    public String getAlbum() {
        return album;
    }

    public String getArtist() {
        return artist;
    }

    public String getYear() {
        return year;
    }

    public float getMsLength() {
        return msLength;
    }

    public String getStrLength() {
        return strLength;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getNumFrames() {
        return numFrames;
    }

    public float getMsPerFrame() {
        return msPerFrame;
    }

    /**
     * Returns a {@link java.io.BufferedInputStream} of the MP3 file, so it can be easily played.
     *
     * @see java.io.BufferedInputStream
     * @see java.io.FileInputStream
     * @return Returns a {@link java.io.BufferedInputStream} of the MP3 file.
     */
    public BufferedInputStream getBufferedInputStream() throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(this.getFilePath()));
    }
}
