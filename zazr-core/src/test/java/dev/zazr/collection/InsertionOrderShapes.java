package dev.zazr.collection;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/// The shapes of the insertion order of a LinkedHashMap, read by reflection, so that the tests can show that their
/// receivers cover each of them: the keys in the last block only, a root that is a leaf or a branch, an emptied last
/// block, keys removed from a leaf, and a branch that lost a whole child.
final class InsertionOrderShapes {

    static final Set<String> ALL = Set.of(
            "last block only",
            "leaf root",
            "branch root",
            "empty last block",
            "a leaf without some of its keys",
            "a branch without some of its children");

    private InsertionOrderShapes() {}

    static Set<String> of(LinkedHashMap<?, ?> map) throws ReflectiveOperationException {
        Set<String> shapes = new HashSet<>();
        Object order = read(LinkedHashMap.class, map, "order");
        Class<?> type = order.getClass();
        Object root = read(type, order, "root");
        Object[] tailKeys = (Object[]) read(read(type, order, "tail").getClass(), read(type, order, "tail"), "keys");
        if (map.isEmpty()) {
            return shapes;
        }
        if (root == null) {
            shapes.add("last block only");
        } else {
            shapes.add(root.getClass().getSimpleName().equals("Leaf") ? "leaf root" : "branch root");
            if (tailKeys.length == 0) {
                shapes.add("empty last block");
            }
            collect(root, shapes);
        }
        if (holes((int) read(read(type, order, "tail").getClass(), read(type, order, "tail"), "bitmap"))) {
            shapes.add("a leaf without some of its keys");
        }
        return shapes;
    }

    private static void collect(Object node, Set<String> shapes) throws ReflectiveOperationException {
        boolean leaf = node.getClass().getSimpleName().equals("Leaf");
        if (holes((int) read(node.getClass(), node, "bitmap"))) {
            shapes.add(leaf ? "a leaf without some of its keys" : "a branch without some of its children");
        }
        if (!leaf) {
            for (Object child : (Object[]) read(node.getClass(), node, "children")) {
                collect(child, shapes);
            }
        }
    }

    // whether a bit is clear below the highest set bit
    private static boolean holes(int bitmap) {
        return bitmap != 0 && Integer.bitCount(bitmap) < 32 - Integer.numberOfLeadingZeros(bitmap);
    }

    private static Object read(Class<?> type, Object target, String name) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
