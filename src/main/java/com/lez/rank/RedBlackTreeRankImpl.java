package com.lez.rank;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * 基于红黑树实现的高效排行榜
 *
 * @author tanyz
 * @date 2020-09-07 06:33:00
 */
public class RedBlackTreeRankImpl<K, V> {
    private final Comparator<V> comparator;
    private final Function<V, V> valueCopier;
    private final Function<V, K> keyExtractor;

    private final Map<K, Node<V>> keyMap = new HashMap<>();

    private transient Node<V> root;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();


    public RedBlackTreeRankImpl(Comparator<V> comparator, Function<V, V> valueCopier, Function<V, K> keyExtractor) {
        this.comparator = comparator;
        this.valueCopier = valueCopier;
        this.keyExtractor = keyExtractor;
    }

    /**
     * 获取key上的数据
     */
    public V get(K key) {
        readLock.lock();
        try {
            Node<V> vNode = keyMap.get(key);
            return vNode == null ? null : valueCopier.apply(vNode.value);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 获取key的排名
     *
     * @return null：key未在排行榜上 others：key的排名
     */
    public Integer getRank(K key) {
        readLock.lock();
        try {
            Node<V> y = keyMap.get(key);
            if (y == null) {
                return null;
            }

            int r = (y.left == null ? 0 : y.left.size) + 1;
            while (y != root) {
                if (y == rightOf(parentOf(y))) {
                    Node<V> vNode = leftOf(parentOf(y));
                    r += (vNode != null ? vNode.size : 0) + 1;
                }
                y = y.parent;
            }
            return r;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 获取排行榜当前长度
     */
    public int rankSize() {
        readLock.lock();
        try {
            return keyMap.size();
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 获取排于n的关键字
     *
     * @return null：不存在排于n的关键字  other：排于n关键字
     */
    public K rankIn(int n) {
        if (n < 1) {
            throw new RuntimeException("n 不能小于 1 n:" + n);
        }

        readLock.lock();
        try {
            if (n > keyMap.size()) {
                return null;
            }

            return keyExtractor.apply(rankIn(root, n).value);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 获取排名在 区间 fromInclusive 到 toExclusive 之间的value集合
     */
    public List<V> rankRange(int fromInclusive, int toExclusive) {
        if (fromInclusive < 0 || toExclusive < 0 || fromInclusive >= toExclusive) {
            throw new RuntimeException("错误参数 fromInclusive：" + fromInclusive + "  toExclusive:" + toExclusive);
        }
        readLock.lock();
        try {
            int size = keyMap.size();
            if (fromInclusive > size) {
                fromInclusive = size;
            }
            if (toExclusive > size + 1) {
                toExclusive = size + 1;
            }
            if (fromInclusive >= toExclusive) {
                return Collections.emptyList();
            }

            List<V> result = new ArrayList<>(toExclusive - fromInclusive);

            Node<V> node = rankIn(root, fromInclusive);
            final int lastNum = toExclusive - fromInclusive;
            for (int i = 0; i < lastNum; i++) {
                result.add(valueCopier.apply(node.value));
                node = successor(node);
            }

            return result;
        } finally {
            readLock.unlock();
        }
    }

    public void printAll() {
        print(root);
    }

    private void print(Node<V> node) {
        if (node != null) {
            print(node.left);
            System.out.println("key:" + keyExtractor.apply(node.value) + " value:" + node.value);
            print(node.right);
        }
    }

    private Node<V> rankIn(Node<V> x, int i) {
        Node<V> leftNode = leftOf(x);
        int r = (leftNode == null ? 0 : leftNode.size) + 1;
        if (i == r) {
            return x;
        } else if (i < r) {
            return rankIn(x.left, i);
        } else {
            return rankIn(x.right, i - r);
        }
    }

    /**
     * 添加，或者更新已经添加了的数据
     *
     * @param key   关键字
     * @param value 相关数据
     * @return 原来的数据
     */
    public V put(K key, V value) {
        if (!keyExtractor.apply(value).equals(key)) {
            throw new RuntimeException("value 中抽取的key：" + keyExtractor.apply(value) + " 于输入key：" + key + " 不一致");
        }

        value = valueCopier.apply(value);
        writeLock.lock();
        try {
            Node<V> t = root;
            if (t == null) {
                root = new Node<>(value, null);
                keyMap.put(key, root);
                return null;
            }

            //更新操作
            Node<V> oldNode = keyMap.get(key);
            if (oldNode != null) {
                final V oldValue = oldNode.value;

                int compare = comparator.compare(value, oldValue);
                if (compare == 0) {
                    oldNode.value = value;
                } else if (compare > 0) {
                    Node<V> pre = oldNode;
                    Node<V> current = successor(pre);
                    while (current != null && comparator.compare(value, current.value) > 0) {
                        K affectedKey = keyExtractor.apply(current.value);
                        keyMap.put(affectedKey, pre);

                        pre.value = current.value;
                        pre = current;
                        current = successor(pre);
                    }

                    pre.value = value;
                    keyMap.put(key, pre);
                } else {
                    Node<V> pre = oldNode;
                    Node<V> current = predecessor(pre);
                    while (current != null && comparator.compare(value, current.value) < 0) {
                        K affectedKey = keyExtractor.apply(current.value);
                        keyMap.put(affectedKey, pre);

                        pre.value = current.value;
                        pre = current;
                        current = predecessor(pre);
                    }

                    pre.value = value;
                    keyMap.put(key, pre);
                }
                return oldValue;
            }

            //插入操作
            int cmp;
            Node<V> parent;
            Comparator<V> cpr = comparator;
            do {
                parent = t;
                cmp = cpr.compare(value, t.value);
                if (cmp < 0)
                    t = t.left;
                else if (cmp > 0)
                    t = t.right;
                else
                    throw new RuntimeException("两个元素的排名不能相等");
            } while (t != null);

            Node<V> e = new Node<>(value, parent);
            if (cmp < 0) {
                parent.left = e;
            } else {
                parent.right = e;
            }

            addToAllParentNodes(e.parent, 1);

            fixAfterInsertion(e);
            keyMap.put(key, e);
            return null;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 将关键字从排行榜上删除
     *
     * @return 原来和关键字相关联的数据或者null
     */
    public V remove(K key) {
        writeLock.lock();
        try {
            Node<V> value = keyMap.get(key);
            if (value == null) {
                return null;
            }

            deleteNode(value);
            keyMap.remove(key);
            return value.value;
        } finally {
            writeLock.unlock();
        }
    }

    private void addToAllParentNodes(Node<V> parent, int num) {
        while (parent != null) {
            parent.size += num;
            parent = parent.parent;
        }
    }

    // Red-black mechanics
    private static final boolean RED = false;
    private static final boolean BLACK = true;

    final static class Node<V> {
        V value;

        Node<V> left, right, parent;
        int size = 1;
        boolean color = BLACK;

        public Node(V value, Node<V> parent) {
            this.value = value;
            this.parent = parent;
        }
    }

    final Node<V> getFirstNode() {
        Node<V> p = root;
        if (p != null) {
            while (p.left != null) {
                p = p.left;
            }
        }
        return p;
    }

    final Node<V> getLastNode() {
        Node<V> p = root;
        if (p != null) {
            while (p.right != null) {
                p = p.right;
            }
        }
        return p;
    }

    static <V> Node<V> successor(Node<V> t) {
        if (t == null) {
            return null;
        } else if (t.right != null) {
            Node<V> p = t.right;
            while (p.left != null) {
                p = p.left;
            }
            return p;
        } else {
            Node<V> p = t.parent;
            Node<V> ch = t;
            while (p != null && ch == p.right) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }

    static <V> Node<V> predecessor(Node<V> t) {
        if (t == null) {
            return null;
        } else if (t.left != null) {
            Node<V> p = t.left;
            while (p.right != null) {
                p = p.right;
            }
            return p;
        } else {
            Node<V> p = t.parent;
            Node<V> ch = t;
            while (p != null && ch == p.left) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }

    /**
     * Balancing operations.
     * <p>
     * Implementations of rebalancings during insertion and deletion are
     * slightly different than the CLR version.  Rather than using dummy
     * nilnodes, we use a set of accessors that deal properly with null.  They
     * are used to avoid messiness surrounding nullness checks in the main
     * algorithms.
     */
    private static <V> boolean colorOf(Node<V> p) {
        return (p == null ? BLACK : p.color);
    }

    private static <V> Node<V> parentOf(Node<V> p) {
        return (p == null ? null : p.parent);
    }

    private static <V> void setColor(Node<V> p, boolean c) {
        if (p != null) {
            p.color = c;
        }
    }

    private static <V> Node<V> leftOf(Node<V> p) {
        return (p == null) ? null : p.left;
    }

    private static <V> Node<V> rightOf(Node<V> p) {
        return (p == null) ? null : p.right;
    }

    /**
     * From CLR
     */
    private void rotateLeft(Node<V> p) {
        if (p != null) {
            Node<V> r = p.right;
            p.right = r.left;
            if (r.left != null)
                r.left.parent = p;
            r.parent = p.parent;
            if (p.parent == null)
                root = r;
            else if (p.parent.left == p)
                p.parent.left = r;
            else
                p.parent.right = r;
            r.left = p;
            p.parent = r;

            r.size = p.size;
            int leftSize = p.left != null ? p.left.size : 0;
            int rightSize = p.right != null ? p.right.size : 0;
            p.size = leftSize + rightSize + 1;
        }
    }

    /**
     * From CLR
     */
    private void rotateRight(Node<V> p) {
        if (p != null) {
            Node<V> l = p.left;
            p.left = l.right;
            if (l.right != null) l.right.parent = p;
            l.parent = p.parent;
            if (p.parent == null)
                root = l;
            else if (p.parent.right == p)
                p.parent.right = l;
            else p.parent.left = l;
            l.right = p;
            p.parent = l;

            l.size = p.size;
            int leftSize = p.left != null ? p.left.size : 0;
            int rightSize = p.right != null ? p.right.size : 0;
            p.size = leftSize + rightSize + 1;
        }
    }

    /**
     * From CLR 2256
     */
    private void fixAfterInsertion(Node<V> x) {
        x.color = RED;

        while (x != null && x != root && x.parent.color == RED) {
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                Node<V> y = rightOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == rightOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateLeft(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateRight(parentOf(parentOf(x)));
                }
            } else {
                Node<V> y = leftOf(parentOf(parentOf(x)));
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateRight(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateLeft(parentOf(parentOf(x)));
                }
            }
        }
        root.color = BLACK;
    }

    /**
     * Delete node p, and then rebalance the tree. 2299
     */
    private void deleteNode(Node<V> p) {
        // If strictly internal, copy successor's element to p and then make p
        // point to successor.
        if (p.left != null && p.right != null) {
            Node<V> s = successor(p);
            p.value = s.value;
            keyMap.put(keyExtractor.apply(p.value), p);
            p = s;
        } // p has 2 children

        // Start fixup at replacement node, if it exists.
        Node<V> replacement = (p.left != null ? p.left : p.right);

        if (replacement != null) {
            // Link replacement to parent
            replacement.parent = p.parent;
            if (p.parent == null)
                root = replacement;
            else if (p == p.parent.left)
                p.parent.left = replacement;
            else
                p.parent.right = replacement;

            addToAllParentNodes(p, -1);
            // Null out links so they are OK to use by fixAfterDeletion.
            p.left = p.right = p.parent = null;

            // Fix replacement
            if (p.color == BLACK)
                fixAfterDeletion(replacement);
        } else if (p.parent == null) { // return if we are the only node.
            root = null;
        } else { //  No children. Use self as phantom replacement and unlink.
            if (p.color == BLACK)
                fixAfterDeletion(p);

            if (p.parent != null) {
                addToAllParentNodes(p, -1);

                if (p == p.parent.left)
                    p.parent.left = null;
                else if (p == p.parent.right)
                    p.parent.right = null;
                p.parent = null;
            }
        }
    }

    /**
     * From CLR 2349
     */
    private void fixAfterDeletion(Node<V> x) {
        while (x != root && colorOf(x) == BLACK) {
            if (x == leftOf(parentOf(x))) {
                Node<V> sib = rightOf(parentOf(x));

                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateLeft(parentOf(x));
                    sib = rightOf(parentOf(x));
                }

                if (colorOf(leftOf(sib)) == BLACK &&
                        colorOf(rightOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(rightOf(sib)) == BLACK) {
                        setColor(leftOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateRight(sib);
                        sib = rightOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(rightOf(sib), BLACK);
                    rotateLeft(parentOf(x));
                    x = root;
                }
            } else { // symmetric
                Node<V> sib = leftOf(parentOf(x));

                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);
                    setColor(parentOf(x), RED);
                    rotateRight(parentOf(x));
                    sib = leftOf(parentOf(x));
                }

                if (colorOf(rightOf(sib)) == BLACK &&
                        colorOf(leftOf(sib)) == BLACK) {
                    setColor(sib, RED);
                    x = parentOf(x);
                } else {
                    if (colorOf(leftOf(sib)) == BLACK) {
                        setColor(rightOf(sib), BLACK);
                        setColor(sib, RED);
                        rotateLeft(sib);
                        sib = leftOf(parentOf(x));
                    }
                    setColor(sib, colorOf(parentOf(x)));
                    setColor(parentOf(x), BLACK);
                    setColor(leftOf(sib), BLACK);
                    rotateRight(parentOf(x));
                    x = root;
                }
            }
        }

        setColor(x, BLACK);
    }
}
