package my_app.hotreload;

import javafx.application.Platform;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

public class UIReloaderImpl implements Reloader {

    @Override
    public void reload(Object context, String screenClassName, String classesPath) {
        Platform.runLater(() -> doReload(context, screenClassName, classesPath));
    }

    private void doReload(Object context, String screenClassName, String classesPath) {
        try {
            if (screenClassName == null) {
                System.err.println("[UIReloader] Screen class name is null.");
                return;
            }

            if (classesPath == null) {
                classesPath = "build/classes/java/main";
            }

            URL classesUrl = new File(classesPath).toURI().toURL();
            ClassLoader parent = this.getClass().getClassLoader();
            URLClassLoader freshLoader = new URLClassLoader(new URL[]{classesUrl}, parent) {
                @Override
                public Class<?> loadClass(String name) throws ClassNotFoundException {
                    if (name.startsWith("my_app.")) {
                        try {
                            return findClass(name);
                        } catch (ClassNotFoundException e) {
                            // Fall through to parent
                        }
                    }
                    return super.loadClass(name);
                }
            };

            Class<?> screenClass = freshLoader.loadClass(screenClassName);
            System.out.println("[UIReloader] Screen class loaded by: " + screenClass.getClassLoader().getClass().getSimpleName());

            Object screenInstance = screenClass.getDeclaredConstructor().newInstance();
            Object component = screenClass.getMethod("render").invoke(screenInstance);

            context.getClass().getMethod("useView", Class.forName("megalodonte.base.ComponentInterface"))
                    .invoke(context, component);

            System.out.println("[UIReloader] UI reloaded successfully.");
            freshLoader.close();

        } catch (Exception e) {
            System.err.println("[UIReloader] Error during UI reload process.");
            e.printStackTrace();
        }
    }
}
