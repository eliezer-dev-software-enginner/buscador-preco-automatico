package my_app.hotreload;

import javafx.application.Platform;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

public class UIReloaderImpl implements Reloader {

    @Override
    public void reload(Object context, String screenClassName, String classesPath, Boolean hasRouteDefined) {
        Platform.runLater(() -> doReload(context, screenClassName, classesPath, hasRouteDefined));
    }

    private void doReload(Object context, String screenClassName, String classesPath, Boolean hasRouteDefined) {
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

            if (Boolean.TRUE.equals(hasRouteDefined)) {
                reloadWithRouter(context, freshLoader, classesPath);
            } else {
                reloadWithoutRouter(context, screenClassName, freshLoader);
            }

            System.out.println("[UIReloader] UI reloaded successfully.");
            freshLoader.close();

        } catch (Exception e) {
            System.err.println("[UIReloader] Error during UI reload process.");
            e.printStackTrace();
        }
    }

    /**
     * Reconstrói o router carregando o AppRouter via freshLoader,
     * registra no context e exibe o entrypoint atualizado.
     */
    private void reloadWithRouter(Object context, URLClassLoader freshLoader, String classesPath) throws Exception {
        // Carrega o AppRouter pelo freshLoader para pegar as screens recompiladas
        Class<?> appRouterClass = freshLoader.loadClass("my_app.AppRouter");
        System.out.println("[UIReloader] AppRouter loaded by: " + appRouterClass.getClassLoader().getClass().getSimpleName());

        // Chama AppRouter.build() para obter o novo Router
        Object newRouter = appRouterClass.getMethod("build").invoke(null);

        Class<?> contextClass = context.getClass();

        // Chama context.useRouter(newRouter)
        Class<?> routerClass = newRouter.getClass();
        Method useRouterMethod = findMethod(contextClass, "useRouter", routerClass);
        useRouterMethod.invoke(context, newRouter);

        // Obtém o entrypoint e sua view: router.entrypoint().view()
        Object entrypoint = routerClass.getMethod("entrypoint").invoke(newRouter);
        Object entrypointView = entrypoint.getClass().getMethod("view").invoke(entrypoint);

        // Chama context.useView(entrypointView)
        Class<?> componentInterface = Class.forName("megalodonte.base.components.ComponentInterface");
        Method useViewMethod = contextClass.getMethod("useView", componentInterface);
        useViewMethod.invoke(context, entrypointView);
    }

    /**
     * Comportamento original: instancia a screen diretamente e chama useView.
     */
    private void reloadWithoutRouter(Object context, String screenClassName, URLClassLoader freshLoader) throws Exception {
        Class<?> screenClass = freshLoader.loadClass(screenClassName);
        System.out.println("[UIReloader] Screen class loaded by: " + screenClass.getClassLoader().getClass().getSimpleName());

        Object screenInstance = screenClass.getDeclaredConstructor().newInstance();
        Object component = screenClass.getMethod("render").invoke(screenInstance);

        Class<?> contextClass = context.getClass();
        Class<?> componentInterface = Class.forName("megalodonte.base.components.ComponentInterface");
        Method useViewMethod = contextClass.getMethod("useView", componentInterface);
        useViewMethod.invoke(context, component);
    }

    /**
     * Busca um método no contextClass cujo primeiro parâmetro seja
     * assignable do tipo paramType (suporta interfaces e herança).
     */
    private Method findMethod(Class<?> clazz, String methodName, Class<?> paramType) throws NoSuchMethodException {
        for (Method m : clazz.getMethods()) {
            if (m.getName().equals(methodName) && m.getParameterCount() == 1) {
                if (m.getParameterTypes()[0].isAssignableFrom(paramType)) {
                    return m;
                }
            }
        }
        throw new NoSuchMethodException(
                clazz.getName() + "." + methodName + "(" + paramType.getName() + ")"
        );
    }
}