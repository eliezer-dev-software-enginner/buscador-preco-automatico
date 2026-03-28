// my_app/Reloader.java
package my_app.hotreload;

public interface Reloader {
    void reload(Object context, String screenClassName, String classesPath);
}