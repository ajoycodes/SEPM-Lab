// Target: the interface the client expects
interface MediaPlayer {
    void play(String fileType, String fileName);
}

// Adaptee: existing class with an incompatible interface
class AdvancedPlayer {
    public void playMp4(String fileName) {
        System.out.println("Playing MP4 file: " + fileName);
    }

    public void playVlc(String fileName) {
        System.out.println("Playing VLC file: " + fileName);
    }
}

// Adapter: converts MediaPlayer calls into AdvancedPlayer calls
class MediaAdapter implements MediaPlayer {
    private AdvancedPlayer advancedPlayer = new AdvancedPlayer();

    public void play(String fileType, String fileName) {
        if (fileType.equalsIgnoreCase("mp4"))
            advancedPlayer.playMp4(fileName);
        else if (fileType.equalsIgnoreCase("vlc"))
            advancedPlayer.playVlc(fileName);
    }
}

// Client class
class AudioPlayer implements MediaPlayer {
    private MediaAdapter adapter = new MediaAdapter();

    public void play(String fileType, String fileName) {
        if (fileType.equalsIgnoreCase("mp3"))
            System.out.println("Playing MP3 file: " + fileName);
        else if (fileType.equalsIgnoreCase("mp4") || fileType.equalsIgnoreCase("vlc"))
            adapter.play(fileType, fileName);
        else
            System.out.println("Invalid format: " + fileType);
    }
}

public class Adapter {
    public static void main(String[] args) {
        AudioPlayer player = new AudioPlayer();

        player.play("mp3", "song.mp3");
        player.play("mp4", "movie.mp4");
        player.play("vlc", "video.vlc");
        player.play("avi", "clip.avi");
    }
}