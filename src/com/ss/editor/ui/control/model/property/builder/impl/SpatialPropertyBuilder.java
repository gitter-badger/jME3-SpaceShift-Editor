package com.ss.editor.ui.control.model.property.builder.impl;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.ss.editor.Messages;
import com.ss.editor.model.undo.editor.ModelChangeConsumer;
import com.ss.editor.model.undo.editor.SceneChangeConsumer;
import com.ss.editor.ui.control.model.property.control.BooleanModelPropertyControl;
import com.ss.editor.ui.control.model.property.control.ColorModelPropertyControl;
import com.ss.editor.ui.control.model.property.control.DefaultModelSinglePropertyControl;
import com.ss.editor.ui.control.model.property.control.EnumModelPropertyControl;
import com.ss.editor.ui.control.model.property.control.FloatModelPropertyControl;
import com.ss.editor.ui.control.model.property.control.IntegerModelPropertyControl;
import com.ss.editor.ui.control.model.property.control.LayerModelPropertyControl;
import com.ss.editor.ui.control.model.property.control.QuaternionModelPropertyControl;
import com.ss.editor.ui.control.model.property.control.Vector2fModelPropertyControl;
import com.ss.editor.ui.control.model.property.control.Vector3fModelPropertyControl;
import com.ss.editor.ui.control.property.builder.PropertyBuilder;
import com.ss.editor.ui.control.property.builder.impl.AbstractPropertyBuilder;
import com.ss.extension.scene.SceneLayer;
import com.ss.extension.scene.SceneNode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

import javafx.scene.layout.VBox;
import rlib.ui.util.FXUtils;
import rlib.util.array.Array;
import rlib.util.array.ArrayFactory;

/**
 * The implementation of the {@link PropertyBuilder} for building property controls for {@link Spatial} objects.
 *
 * @author JavaSaBr
 */
public class SpatialPropertyBuilder extends AbstractPropertyBuilder<ModelChangeConsumer> {

    private static final CullHint[] CULL_HINTS = CullHint.values();
    private static final ShadowMode[] SHADOW_MODES = ShadowMode.values();
    private static final Bucket[] BUCKETS = Bucket.values();

    private static final PropertyBuilder INSTANCE = new SpatialPropertyBuilder();

    public static PropertyBuilder getInstance() {
        return INSTANCE;
    }

    public SpatialPropertyBuilder() {
        super(ModelChangeConsumer.class);
    }

    @Override
    protected void buildForImpl(@NotNull final Object object, @Nullable final Object parent, @NotNull final VBox container,
                                @NotNull final ModelChangeConsumer changeConsumer) {

        if (!(object instanceof Spatial)) return;

        final Spatial spatial = (Spatial) object;
        final CullHint cullHint = spatial.getLocalCullHint();
        final ShadowMode shadowMode = spatial.getLocalShadowMode();
        final Bucket queueBucket = spatial.getLocalQueueBucket();

        if (changeConsumer instanceof SceneChangeConsumer) {

            final SceneLayer layer = SceneLayer.getLayer(spatial);
            final LayerModelPropertyControl propertyControl = new LayerModelPropertyControl(layer, (SceneChangeConsumer) changeConsumer);
            propertyControl.setEditObject(spatial);

            FXUtils.addToPane(propertyControl, container);
        }

        final EnumModelPropertyControl<Spatial, CullHint> cullHintControl =
                new EnumModelPropertyControl<>(cullHint, Messages.MODEL_PROPERTY_CULL_HINT, changeConsumer, CULL_HINTS);
        cullHintControl.setApplyHandler(Spatial::setCullHint);
        cullHintControl.setSyncHandler(Spatial::getLocalCullHint);
        cullHintControl.setEditObject(spatial);

        final EnumModelPropertyControl<Spatial, ShadowMode> shadowModeControl =
                new EnumModelPropertyControl<>(shadowMode, Messages.MODEL_PROPERTY_SHADOW_MODE, changeConsumer, SHADOW_MODES);
        shadowModeControl.setApplyHandler(Spatial::setShadowMode);
        shadowModeControl.setSyncHandler(Spatial::getLocalShadowMode);
        shadowModeControl.setEditObject(spatial);

        final EnumModelPropertyControl<Spatial, Bucket> queueBucketControl =
                new EnumModelPropertyControl<>(queueBucket, Messages.MODEL_PROPERTY_QUEUE_BUCKET, changeConsumer, BUCKETS);
        queueBucketControl.setApplyHandler(Spatial::setQueueBucket);
        queueBucketControl.setSyncHandler(Spatial::getLocalQueueBucket);
        queueBucketControl.setEditObject(spatial);

        FXUtils.addToPane(cullHintControl, container);
        FXUtils.addToPane(shadowModeControl, container);
        FXUtils.addToPane(queueBucketControl, container);

        if (canEditTransformation(spatial)) {

            final Vector3f location = spatial.getLocalTranslation().clone();
            final Vector3f scale = spatial.getLocalScale().clone();
            final Quaternion rotation = spatial.getLocalRotation().clone();

            final Vector3fModelPropertyControl<Spatial> locationControl =
                    new Vector3fModelPropertyControl<>(location, Messages.MODEL_PROPERTY_LOCATION, changeConsumer);
            locationControl.setApplyHandler(Spatial::setLocalTranslation);
            locationControl.setSyncHandler(Spatial::getLocalTranslation);
            locationControl.setEditObject(spatial);

            final Vector3fModelPropertyControl<Spatial> scaleControl =
                    new Vector3fModelPropertyControl<>(scale, Messages.MODEL_PROPERTY_SCALE, changeConsumer);
            scaleControl.setApplyHandler(Spatial::setLocalScale);
            scaleControl.setSyncHandler(Spatial::getLocalScale);
            scaleControl.setEditObject(spatial);

            final QuaternionModelPropertyControl rotationControl =
                    new QuaternionModelPropertyControl(rotation, Messages.MODEL_PROPERTY_ROTATION, changeConsumer);
            rotationControl.setApplyHandler(Spatial::setLocalRotation);
            rotationControl.setSyncHandler(Spatial::getLocalRotation);
            rotationControl.setEditObject(spatial);

            addSplitLine(container);

            FXUtils.addToPane(locationControl, container);
            FXUtils.addToPane(scaleControl, container);
            FXUtils.addToPane(rotationControl, container);
        }

        final Collection<String> userDataKeys = spatial.getUserDataKeys();
        if (userDataKeys.isEmpty()) return;

        addSplitLine(container);

        final Array<String> sortedKeys = ArrayFactory.newSortedArray(String.class);
        sortedKeys.addAll(userDataKeys);

        for (final String key : sortedKeys) {
            if (SceneLayer.KEY.equals(key)) continue;

            final Object data = spatial.getUserData(key);

            if (data instanceof Float) {

                final Float value = (Float) data;

                final FloatModelPropertyControl<Spatial> control = new FloatModelPropertyControl<>(value, key, changeConsumer);
                control.setApplyHandler((sp, newValue) -> sp.setUserData(key, newValue));
                control.setSyncHandler(sp -> sp.getUserData(key));
                control.setEditObject(spatial);

                FXUtils.addToPane(control, container);

            } else if (data instanceof Integer) {

                final Integer value = (Integer) data;

                final IntegerModelPropertyControl<Spatial> control = new IntegerModelPropertyControl<>(value, key, changeConsumer);
                control.setApplyHandler((sp, newValue) -> sp.setUserData(key, newValue));
                control.setSyncHandler(sp -> sp.getUserData(key));
                control.setEditObject(spatial);

                FXUtils.addToPane(control, container);

            } else if (data instanceof Boolean) {

                final Boolean value = (Boolean) data;

                final BooleanModelPropertyControl<Spatial> control = new BooleanModelPropertyControl<>(value, key, changeConsumer);
                control.setApplyHandler((sp, newValue) -> sp.setUserData(key, newValue));
                control.setSyncHandler(sp -> sp.getUserData(key));
                control.setEditObject(spatial);

                FXUtils.addToPane(control, container);

            } else if (data instanceof Vector3f) {

                final Vector3f value = (Vector3f) data;

                final Vector3fModelPropertyControl<Spatial> control = new Vector3fModelPropertyControl<>(value, key, changeConsumer);
                control.setApplyHandler((sp, newValue) -> sp.setUserData(key, newValue));
                control.setSyncHandler(sp -> sp.getUserData(key));
                control.setEditObject(spatial);

                FXUtils.addToPane(control, container);

            } else if (data instanceof Vector2f) {

                final Vector2f value = (Vector2f) data;

                final Vector2fModelPropertyControl<Spatial> control = new Vector2fModelPropertyControl<>(value, key, changeConsumer);
                control.setApplyHandler((sp, newValue) -> sp.setUserData(key, newValue));
                control.setSyncHandler(sp -> sp.getUserData(key));
                control.setEditObject(spatial);

                FXUtils.addToPane(control, container);

            } else if (data instanceof ColorRGBA) {

                final ColorRGBA value = (ColorRGBA) data;

                final ColorModelPropertyControl<Spatial> control = new ColorModelPropertyControl<>(value, key, changeConsumer);
                control.setApplyHandler((sp, newValue) -> sp.setUserData(key, newValue));
                control.setSyncHandler(sp -> sp.getUserData(key));
                control.setEditObject(spatial);

                FXUtils.addToPane(control, container);

            } else if (data instanceof String) {

                final String value = (String) data;

                final DefaultModelSinglePropertyControl<Spatial, String> control = new DefaultModelSinglePropertyControl<>(value, key, changeConsumer);
                control.setApplyHandler((sp, newValue) -> sp.setUserData(key, newValue));
                control.setSyncHandler(sp -> sp.getUserData(key));
                control.setEditObject(spatial);

                FXUtils.addToPane(control, container);
            }
        }
    }

    private boolean canEditTransformation(@NotNull final Spatial spatial) {
        return !(spatial instanceof SceneNode || spatial instanceof SceneLayer);
    }
}
