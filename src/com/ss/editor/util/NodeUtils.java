package com.ss.editor.util;

import com.jme3.asset.AssetKey;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import rlib.util.StringUtils;
import rlib.util.array.Array;

/**
 * Набор утильнымх методов для работы с узлами геометрии.
 *
 * @author Ronn
 */
public class NodeUtils {

    /**
     * Поиск геометрии в этом узле.
     */
    public static Geometry findGeometry(final Spatial spatial) {

        if (!(spatial instanceof Node)) {
            return null;
        }

        final Node node = (Node) spatial;

        for (final Spatial children : node.getChildren()) {

            final Geometry geometry = findGeometry(children);

            if (geometry != null) {
                return geometry;
            }

            if (children instanceof Geometry) {
                return (Geometry) children;
            }
        }

        return null;
    }

    /**
     * Сбор всей геометрии использующих указанный материал.
     */
    public static void addGeometryWithMaterial(final Spatial spatial, final Array<Geometry> container, final String assetPath) {

        if (StringUtils.isEmpty(assetPath)) {
            return;
        }

        if (spatial instanceof Geometry) {

            final Geometry geometry = (Geometry) spatial;
            final Material material = geometry.getMaterial();
            final String assetName = material == null ? null : material.getAssetName();

            if (StringUtils.equals(assetName, assetPath)) {
                container.add(geometry);
            }

            return;

        } else if (!(spatial instanceof Node)) {
            return;
        }

        final Node node = (Node) spatial;

        for (final Spatial children : node.getChildren()) {
            addGeometryWithMaterial(children, container, assetPath);
        }
    }

    /**
     * Сбор всех объектов использующих указанный asset path.
     */
    public static void addSpatialWithAssetPath(final Spatial spatial, final Array<Spatial> container, final String assetPath) {

        if (StringUtils.isEmpty(assetPath)) {
            return;
        }

        final AssetKey key = spatial.getKey();

        if (key != null && StringUtils.equals(key.getName(), assetPath)) {
            container.add(spatial);
        }

        if (!(spatial instanceof Node)) {
            return;
        }

        final Node node = (Node) spatial;

        for (final Spatial children : node.getChildren()) {
            addSpatialWithAssetPath(children, container, assetPath);
        }
    }

    /**
     * Сбор всей геометрии.
     */
    public static void addGeometry(final Spatial spatial, final Array<Geometry> container) {

        if (spatial instanceof Geometry) {
            container.add((Geometry) spatial);
            return;
        } else if (!(spatial instanceof Node)) {
            return;
        }

        final Node node = (Node) spatial;

        for (final Spatial children : node.getChildren()) {
            addGeometry(children, container);
        }
    }
}