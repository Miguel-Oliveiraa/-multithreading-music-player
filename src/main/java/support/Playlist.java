package support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Abstraction of a playlist. The data structure used to store the song is an {@link ArrayList}.
 * It has an index to point at a specified position, representing the song that is currently playing,
 * flags for loop and shuffle, methods to toggle shuffle modes, return previous and next song in the
 * playlist and more.
 *
 * @see ArrayList
 */
public class Playlist {

    public final int SONG_NOT_FOUND = 0;
    public final int SONG_REMOVED = 1;
    public final int CURRENT_SONG_REMOVED = 2;
    private int currentIndex;
    private boolean looping;
    private boolean shuffled;
    private ArrayList<Song> list = new ArrayList<>();
    private ArrayList<Song> copy = new ArrayList<>();

    /**
     * Appends the specified song to the end of the list.
     *
     * @param song song to be appended to this list.
     */
    public void add(Song song) {
        if (shuffled) copy.add(song);
        list.add(song);
    }

    /**
     * Removes the song at the specified position in this list.
     * Shifts any subsequent elements to the left.
     *
     * @param index the index of song to be removed.
     * @return 0 - if index is out of bounds;<br>
     *         1 - if song is removed;<br>
     *         2 - if song is removed and its index equals currentIndex.
     */
    public int remove(int index) {
        if (shuffled) {
            String uuid = list.get(index).getUuid();
            copy = copy.stream()
                    .filter(song -> !song.getUuid().equals(uuid))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            if (index == currentIndex) return CURRENT_SONG_REMOVED;
            return SONG_REMOVED;
        }
        return SONG_NOT_FOUND;
    }

    /**
     * Returns the song at the specified position in this list.
     *
     * @param index index of the song to be returned.
     * @return the element at the specified position in this list, or null if index is out of bounds.
     */
    public Song get(int index) {
        if (index >= 0 && index < list.size()) return new Song(list.get(index));
        return null;
    }

    /**
     * Returns the index of the song with the specified UUID, or -1 if there is no song with the specified UUID.
     *
     * @param uuid UUID of the song to search for.
     * @return index of the song with the specified UUID, or -1 if there is no song with the specified UUID.
     */
    public int findIndex(String uuid) {
        Song temp = list.stream()
                .filter(song -> song.getUuid().equals(uuid))
                .collect(Collectors.toCollection(ArrayList::new)).get(0);
        return list.indexOf(temp);
    }

    /**
     * Returns the number of songs in this playlist.
     *
     * @return the number of songs in this playlist.
     */
    public int size() {
        return list.size();
    }

    /**
     * Returns an array containing information about each song in this playlist in proper sequence
     * (from first to last element).<br>
     * Each line represents a song and the columns are organized as follows:<br>
     * [0] - Title<br>
     * [1] - Album<br>
     * [2] - Artist<br>
     * [3] - Year<br>
     * [4] - Time (formatted as 00:00)<br>
     * [5] - UUID<br>
     * The returned array will be "safe" in that no references to it
     * are maintained by this list. (In other words, this method must allocate a new array).
     * The caller is thus free to modify the returned array.
     *
     * @return an array containing information about each song in this playlist in proper sequence.
     */
    public String[][] getDisplayInfo() {
        return list.stream().map(Song::getDisplayInfo).toArray(String[][]::new);
    }

    /**
     * Returns the integer defined as the current index of the playlist,
     * as in the index of the song that is currently playing.
     *
     * @return the integer defined as the current index of the playlist.
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Sets the value of the integer defined as the current index of the playlist,
     * as in the index of the song that is currently playing.
     *
     * @param newIndex new value of the integer defined as the current index of the playlist.
     */
    public void setCurrentIndex(int newIndex) {
        if (newIndex >= 0 && newIndex < list.size()) currentIndex = newIndex;
    }

    /**
     * Returns the integer defined as the index before the current index of the playlist,
     * as in the index of the song that comes before the one currently playing (currentIndex - 1),
     * taking into account if the playlist is set to loop or not.
     *
     * @return the integer defined as the index before the current index of the playlist.
     */
    public int getPreviousIndex() {
        if (currentIndex > 0) return currentIndex - 1;
        else return looping ? list.size() - 1 : 0;
    }

    /**
     * Returns the integer defined as the index after the current index of the playlist,
     * as in the index of the song that comes after the one currently playing (currentIndex + 1),
     * taking into account if the playlist is set to loop or not.
     *
     * @return the integer defined as the index after the current index of the playlist.
     */
    public int getNextIndex() {
        if (currentIndex < list.size() - 1) return currentIndex + 1;
        else return looping ? 0 : list.size() - 1;
    }

    /**
     *
     * @return true - if playlist is set to loop.
     */
    public boolean isLooping() {
        return looping;
    }

    /**
     * Toggle looping flag of playlist between true and false.
     */
    public void toggleLooping() {
        looping = !looping;
    }

    /**
     * @return True if playlist was set to shuffle.
     */
    public boolean isShuffled() {
        return shuffled;
    }

    /**
     * Toggle playlist order between shuffled and "as added".
     * @param keepCurrent True to keep song in current index at the top when shuffling.
     *                    Has no effect when undoing shuffle.
     */
    public void toggleShuffle(boolean keepCurrent) {
        if (shuffled) {
            currentIndex = copy.indexOf(list.get(currentIndex));
            list.clear();
            list.addAll(copy);
        } else {
            copy.clear();
            copy.addAll(list);
            if (keepCurrent) {
                list.remove(currentIndex);
                Collections.shuffle(list);
                list.add(0, copy.get(currentIndex));
            } else {
                Collections.shuffle(list);
            }
            currentIndex = 0;
        }
        shuffled = !shuffled;
    }

    /**
     * @return True if playlist is empty.
     */
    public boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * @return True if there is a song after currentIndex, taking into account if playlist is set to loop.
     */
    public boolean hasNext() {
        return !isEmpty() && (looping || currentIndex < size() - 1);
    }

    /**
     * @return True if there is a song before currentIndex, taking into account if playlist is set to loop.
     */
    public boolean hasPrevious() {
        return !isEmpty() && (looping || currentIndex > 0);
    }
}
