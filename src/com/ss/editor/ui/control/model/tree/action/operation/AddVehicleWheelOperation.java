package com.ss.editor.ui.control.model.tree.action.operation;

import static java.util.Objects.requireNonNull;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.math.Vector3f;
import com.ss.editor.model.undo.editor.ModelChangeConsumer;
import com.ss.editor.model.undo.impl.AbstractEditorOperation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The operation to add a wheel to vehicle control.
 *
 * @author JavaSaBr
 */
public class AddVehicleWheelOperation extends AbstractEditorOperation<ModelChangeConsumer> {

    /**
     * The vehicle control.
     */
    @NotNull
    private final VehicleControl control;

    /**
     * The connection point.
     */
    @NotNull
    private final Vector3f connectionPoint;

    /**
     * The direction.
     */
    @NotNull
    private final Vector3f direction;

    /**
     * The axle.
     */
    @NotNull
    private final Vector3f axle;

    /**
     * The wheel.
     */
    @Nullable
    private VehicleWheel createdWheel;

    /**
     * The suspension rest length.
     */
    private final float suspensionRestLength;

    /**
     * The wheel radius.
     */
    private final float wheelRadius;

    /**
     * The flag is front wheel.
     */
    private final boolean isFrontWheel;

    public AddVehicleWheelOperation(@NotNull final VehicleControl control, @NotNull final Vector3f connectionPoint,
                                    @NotNull final Vector3f direction, @NotNull final Vector3f axle,
                                    final float suspensionRestLength, final float wheelRadius,
                                    final boolean isFrontWheel) {
        this.control = control;
        this.connectionPoint = connectionPoint;
        this.direction = direction;
        this.axle = axle;
        this.suspensionRestLength = suspensionRestLength;
        this.wheelRadius = wheelRadius;
        this.isFrontWheel = isFrontWheel;
    }

    @Override
    protected void redoImpl(@NotNull final ModelChangeConsumer editor) {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> {

            final VehicleWheel vehicleWheel = control.addWheel(connectionPoint, direction, axle, suspensionRestLength,
                    wheelRadius, isFrontWheel);

            this.createdWheel = vehicleWheel;

            EXECUTOR_MANAGER.addFXTask(() -> editor.notifyAddedChild(control, vehicleWheel, -1));
        });
    }

    @Override
    protected void undoImpl(@NotNull final ModelChangeConsumer editor) {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> {

            for (int i = 0, length = control.getNumWheels(); i < length; i++) {
                final VehicleWheel wheel = control.getWheel(i);
                if (wheel == createdWheel) {
                    control.removeWheel(i);
                    break;
                }
            }

            final VehicleWheel toRemove = requireNonNull(createdWheel);

            this.createdWheel = null;

            EXECUTOR_MANAGER.addFXTask(() -> editor.notifyRemovedChild(control, toRemove));
        });
    }
}