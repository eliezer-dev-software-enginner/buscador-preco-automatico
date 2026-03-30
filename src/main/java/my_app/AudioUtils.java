package my_app;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class AudioUtils {

    private static MediaPlayer mediaPlayer;

    public static void playAudio(String resourceFileName) {
        try {
            var resource = AudioUtils.class
                    .getResource("/" + resourceFileName);

            if (resource == null) {
                System.out.println("Arquivo não encontrado: " + resourceFileName);
                return;
            }

            Media media = new Media(resource.toExternalForm());

            if (mediaPlayer != null) {
                mediaPlayer.stop();
            }

            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.play();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}