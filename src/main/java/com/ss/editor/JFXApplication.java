package com.ss.editor;

import static com.jme3x.jfx.injfx.JmeToJFXIntegrator.bind;
import static com.ss.rlib.util.ObjectUtils.notNull;
import static java.nio.file.Files.newOutputStream;
import com.jme3.util.BufferAllocatorFactory;
import com.jme3x.jfx.injfx.JmeToJFXApplication;
import com.jme3x.jfx.injfx.processor.FrameTransferSceneProcessor;
import com.ss.editor.analytics.google.GAEvent;
import com.ss.editor.analytics.google.GAnalytics;
import com.ss.editor.annotation.FXThread;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.config.CommandLineConfig;
import com.ss.editor.config.Config;
import com.ss.editor.config.EditorConfig;
import com.ss.editor.executor.impl.JMEThreadExecutor;
import com.ss.editor.file.converter.FileConverterRegistry;
import com.ss.editor.manager.*;
import com.ss.editor.task.CheckNewVersionTask;
import com.ss.editor.ui.builder.EditorFXSceneBuilder;
import com.ss.editor.ui.component.asset.tree.AssetTreeContextMenuFillerRegistry;
import com.ss.editor.ui.component.creator.FileCreatorRegistry;
import com.ss.editor.ui.component.editor.EditorRegistry;
import com.ss.editor.ui.component.log.LogView;
import com.ss.editor.ui.control.tree.node.TreeNodeFactoryRegistry;
import com.ss.editor.ui.css.CSSRegistry;
import com.ss.editor.ui.dialog.ConfirmDialog;
import com.ss.editor.ui.scene.EditorFXScene;
import com.ss.editor.util.OpenGLVersion;
import com.ss.editor.util.SynchronizedByteBufferAllocator;
import com.ss.rlib.logging.Logger;
import com.ss.rlib.logging.LoggerManager;
import com.ss.rlib.manager.InitializeManager;
import de.codecentric.centerdevice.javafxsvg.SvgImageLoaderFactory;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.Configuration;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;

/**
 * The starter of the JavaFX application.
 *
 * @author JavaSaBr
 */
public class JFXApplication extends Application {

    @NotNull
    private static final Logger LOGGER = LoggerManager.getLogger(JFXApplication.class);

    @Nullable
    private static JFXApplication instance;

    /**
     * Gets instance.
     *
     * @return the instance
     */
    @NotNull
    @FromAnyThread
    public static JFXApplication getInstance() {
        return notNull(instance);
    }

    /**
     * Get the current stage of JavaFX.
     *
     * @return the current stage.
     */
    @Nullable
    @FromAnyThread
    private static Stage getStage() {
        final JFXApplication instance = JFXApplication.instance;
        return instance == null ? null : instance.stage;
    }

    /**
     * Main.
     *
     * @param args the args
     * @throws IOException the io exception
     */
    public static void main(final String[] args) throws IOException {

        // need to disable to work on macos
        Configuration.GLFW_CHECK_THREAD0.set(false);
        // use jemalloc
        Configuration.MEMORY_ALLOCATOR.set("jemalloc");

        // JavaFX
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.text", "t2k");
        System.setProperty("javafx.animation.fullspeed", "true");

        // FIXME need to remove after jME upgrading
        System.setProperty(BufferAllocatorFactory.PROPERTY_BUFFER_ALLOCATOR_IMPLEMENTATION,
                SynchronizedByteBufferAllocator.class.getName());

        final EditorConfig editorConfig = EditorConfig.getInstance();
        final OpenGLVersion openGLVersion = editorConfig.getOpenGLVersion();

        // set a render if it isn't override
        if(System.getProperty("jfx.background.render") == null) {
            System.setProperty("jfx.background.render", openGLVersion.getRender());
        }

        // some settings for the render of JavaFX
        //System.setProperty("prism.cacheshapes", "true");
        //System.setProperty("prism.scrollcacheopt", "true");
        //System.setProperty("prism.allowhidpi", "true");

        //System.setProperty("prism.order", "sw");
        //System.setProperty("prism.showdirty", "true");
        //System.setProperty("prism.showoverdraw", "true");
        //System.setProperty("prism.printrendergraph", "true");
        //System.setProperty("prism.debug", "true");
        //System.setProperty("prism.verbose", "true");

        CommandLineConfig.args(args);

        JmeToJFXApplication application;
        try {
            application = Editor.prepareToStart();
        } catch (final Throwable e) {
            printError(e);
            System.exit(-1);
            return;
        }

        InitializeManager.register(ResourceManager.class);
        InitializeManager.register(JavaFXImageManager.class);
        InitializeManager.register(FileIconManager.class);
        InitializeManager.register(WorkspaceManager.class);
        InitializeManager.register(CustomClasspathManager.class);
        InitializeManager.register(PluginManager.class);
        InitializeManager.initialize();

        new EditorThread(new ThreadGroup("LWJGL"),
                () -> startJMEApplication(application), "LWJGL Render").start();
    }

    private static void startJMEApplication(@NotNull final JmeToJFXApplication application) {
        final PluginManager pluginManager = PluginManager.getInstance();
        pluginManager.onBeforeCreateJMEContext();
        try {
            application.start();
        } finally {
            pluginManager.onBeforeCreateJMEContext();
        }
    }

    /**
     * Start.
     */
    @FromAnyThread
    public static void start() {
        launch();
    }

    private static void printError(final Throwable throwable) {
        throwable.printStackTrace();

        final String userHome = System.getProperty("user.home");
        final String fileName = "jme3-spaceshift-editor-error.log";

        try (final PrintStream out = new PrintStream(newOutputStream(Paths.get(userHome, fileName)))) {
            throwable.printStackTrace(out);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The JavaFX scene.
     */
    @Nullable
    private volatile EditorFXScene scene;

    /**
     * The scene processor.
     */
    @Nullable
    private volatile FrameTransferSceneProcessor sceneProcessor;

    /**
     * The stage.
     */
    @Nullable
    private Stage stage;

    @Override
    public void start(final Stage stage) throws Exception {
        JFXApplication.instance = this;
        this.stage = stage;

        try {

            final PluginManager pluginManager = PluginManager.getInstance();
            pluginManager.onBeforeCreateJavaFXContext();
            pluginManager.handlePlugins(editorPlugin -> editorPlugin.register(CSSRegistry.getInstance()));

            LogView.getInstance();
            SvgImageLoaderFactory.install();

            ImageIO.read(getClass().getResourceAsStream("/ui/icons/test/test.jpg"));

            final ObservableList<Image> icons = stage.getIcons();
            icons.add(new Image("/ui/icons/app/SSEd256.png"));
            icons.add(new Image("/ui/icons/app/SSEd128.png"));
            icons.add(new Image("/ui/icons/app/SSEd64.png"));
            icons.add(new Image("/ui/icons/app/SSEd32.png"));
            icons.add(new Image("/ui/icons/app/SSEd16.png"));

            final EditorConfig config = EditorConfig.getInstance();

            stage.initStyle(StageStyle.DECORATED);
            stage.setMinHeight(600);
            stage.setMinWidth(800);
            stage.setWidth(config.getScreenWidth());
            stage.setHeight(config.getScreenHeight());
            stage.setMaximized(config.isMaximized());
            stage.setTitle(Config.TITLE);
            stage.show();

            if (!stage.isMaximized()) stage.centerOnScreen();

            stage.widthProperty().addListener((observable, oldValue, newValue) -> {
                if (stage.isMaximized()) return;
                config.setScreenWidth(newValue.intValue());
            });
            stage.heightProperty().addListener((observable, oldValue, newValue) -> {
                if (stage.isMaximized()) return;
                config.setScreenHeight(newValue.intValue());
            });

            stage.maximizedProperty().addListener((observable, oldValue, newValue) -> config.setMaximized(newValue));

            buildScene();

        } catch (final Exception e) {
            LOGGER.error(this, e);
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        onExit();
    }

    /**
     * On exit.
     */
    @FXThread
    protected void onExit() {

        GAnalytics.forceSendEvent(GAEvent.Category.APPLICATION,
                GAEvent.Action.APPLICATION_CLOSED, GAEvent.Label.THE_EDITOR_APP_WAS_CLOSED);

        final EditorConfig config = EditorConfig.getInstance();
        config.save();

        final JMEThreadExecutor executor = JMEThreadExecutor.getInstance();
        executor.addToExecute(() -> {
            final Editor editor = Editor.getInstance();
            editor.destroy();
        });

        GAnalytics.waitForSend();
    }

    /**
     * Build the scene.
     */
    @FXThread
    private void buildScene() {
        this.scene = EditorFXSceneBuilder.build(notNull(stage));

        final PluginManager pluginManager = PluginManager.getInstance();
        pluginManager.onAfterCreateJavaFXContext();
        pluginManager.handlePlugins(editorPlugin -> {
            editorPlugin.register(FileCreatorRegistry.getInstance());
            editorPlugin.register(EditorRegistry.getInstance());
            editorPlugin.register(FileIconManager.getInstance());
            editorPlugin.register(FileConverterRegistry.getInstance());
            editorPlugin.register(AssetTreeContextMenuFillerRegistry.getInstance());
            editorPlugin.register(TreeNodeFactoryRegistry.getInstance());
        });

        final EditorFXScene scene = getScene();

        final Editor editor = Editor.getInstance();
        final JMEThreadExecutor executor = JMEThreadExecutor.getInstance();
        executor.addToExecute(() -> createSceneProcessor(scene, editor));

        JMEFilePreviewManager.getInstance();

        GAnalytics.forceSendEvent(GAEvent.Category.APPLICATION,
                GAEvent.Action.APPLICATION_LAUNCHED, GAEvent.Label.THE_EDITOR_APP_WAS_LAUNCHED);

        final ExecutorManager executorManager = ExecutorManager.getInstance();
        executorManager.addBackgroundTask(new CheckNewVersionTask());

        final EditorConfig editorConfig = EditorConfig.getInstance();
        if (editorConfig.isAnalyticsQuestion()) return;

        editorConfig.setAnalytics(false);
        editorConfig.save();

        Platform.runLater(() -> {

            final Stage stage = notNull(getStage());
            final ConfirmDialog confirmDialog = new ConfirmDialog(result -> {

                editorConfig.setAnalyticsQuestion(true);
                editorConfig.setAnalytics(result);
                editorConfig.save();

            }, Messages.ANALYTICS_CONFIRM_DIALOG_MESSAGE);

            confirmDialog.show(stage);
        });
    }

    private void createSceneProcessor(@NotNull final EditorFXScene scene, @NotNull final Editor editor) {

        final FrameTransferSceneProcessor sceneProcessor = bind(editor, scene.getCanvas(), editor.getViewPort());
        sceneProcessor.setEnabled(false);

        this.sceneProcessor = sceneProcessor;

        final Stage stage = notNull(getStage());
        stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            final EditorConfig editorConfig = EditorConfig.getInstance();
            editor.setPaused(editorConfig.isStopRenderOnLostFocus() && !newValue);
        });

        Platform.runLater(scene::notifyFinishBuild);
    }

    /**
     * Get the current JavaFX scene.
     *
     * @return the JavaFX scene.
     */
    @NotNull
    @FromAnyThread
    public EditorFXScene getScene() {
        return notNull(scene, "Scene can't be null.");
    }

    /**
     * Get the current scene processor of this application.
     *
     * @return the scene processor.
     */
    @NotNull
    @FromAnyThread
    public FrameTransferSceneProcessor getSceneProcessor() {
        return notNull(sceneProcessor, "Scene processor can't be null.");
    }
}
