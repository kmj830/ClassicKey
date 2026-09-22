package com.example.classickey.datastructure;

import java.util.Objects;

/**
 * 직접 구현한 제네릭 해시 테이블 (CustomHashTable).
 * 버킷 배열과 분리 연결법(Separate Chaining)을 기반으로 구현되었으며,
 * 적재율(Load Factor) 0.75 도달 시 2배 크기로 동적 리사이징 및 재해싱을 수행합니다.
 *
 * @param <K> 키 타입
 * @param <V> 값 타입
 */
public class CustomHashTable<K, V> {

    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    public static class Node<K, V> {
        private final K key;
        private V value;
        private Node<K, V> next;

        public Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public Node<K, V> getNext() {
            return next;
        }
    }

    private Node<K, V>[] buckets;
    private int capacity;
    private int size;
    private final float loadFactor;

    @SuppressWarnings("unchecked")
    public CustomHashTable(int initialCapacity, float loadFactor) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("초기 용량은 1 이상이어야 합니다.");
        }
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("적재율 임계값은 0보다 커야 합니다.");
        }
        this.capacity = initialCapacity;
        this.loadFactor = loadFactor;
        this.size = 0;
        this.buckets = new Node[this.capacity];
    }

    public CustomHashTable(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public CustomHashTable() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * 버킷 인덱스를 계산합니다.
     */
    private int hashIndex(K key, int tableCapacity) {
        if (key == null) {
            return 0;
        }
        return (key.hashCode() & 0x7fffffff) % tableCapacity;
    }

    /**
     * 키-값 쌍을 해시 테이블에 삽입합니다.
     * 동일한 키가 존재하면 값을 갱신하고, 충돌 시 Separate Chaining 방식으로 노드를 연결합니다.
     */
    public void put(K key, V value) {
        int index = hashIndex(key, this.capacity);
        Node<K, V> current = buckets[index];

        while (current != null) {
            if (Objects.equals(current.key, key)) {
                current.value = value;
                return;
            }
            current = current.next;
        }

        // 충돌 시 분리 연결법(Separate Chaining)으로 버킷 헤드에 노드 추가
        buckets[index] = new Node<>(key, value, buckets[index]);
        size++;

        // 적재율 0.75 도달 시 2배 크기로 동적 리사이징 및 재해싱 수행
        if ((float) size / capacity >= loadFactor) {
            resize();
        }
    }

    /**
     * 키에 해당하는 값을 O(1)으로 탐색하여 반환합니다.
     * 키가 존재하지 않으면 null을 반환합니다.
     */
    public V get(K key) {
        int index = hashIndex(key, this.capacity);
        Node<K, V> current = buckets[index];

        while (current != null) {
            if (Objects.equals(current.key, key)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    /**
     * 해당 키의 존재 유무를 확인합니다.
     */
    public boolean containsKey(K key) {
        int index = hashIndex(key, this.capacity);
        Node<K, V> current = buckets[index];

        while (current != null) {
            if (Objects.equals(current.key, key)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /**
     * 해시 테이블 용량을 2배로 확장하고 모든 엔트리를 재해싱합니다.
     */
    @SuppressWarnings("unchecked")
    private void resize() {
        int newCapacity = this.capacity * 2;
        Node<K, V>[] newBuckets = new Node[newCapacity];

        for (int i = 0; i < this.capacity; i++) {
            Node<K, V> current = buckets[i];
            while (current != null) {
                Node<K, V> next = current.next;
                int newIndex = hashIndex(current.key, newCapacity);

                current.next = newBuckets[newIndex];
                newBuckets[newIndex] = current;

                current = next;
            }
        }

        this.capacity = newCapacity;
        this.buckets = newBuckets;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int getCapacity() {
        return capacity;
    }

    public Node<K, V> getBucketHead(int index) {
        if (index < 0 || index >= capacity) {
            throw new IndexOutOfBoundsException("유효하지 않은 버킷 인덱스입니다: " + index);
        }
        return buckets[index];
    }
}
