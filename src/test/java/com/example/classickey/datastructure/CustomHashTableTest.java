package com.example.classickey.datastructure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomHashTableTest {

    @Test
    @DisplayName("UT-HT-01: 해시 테이블 기본 삽입 및 조회")
    void testBasicPutAndGet() {
        CustomHashTable<Character, Integer> table = new CustomHashTable<>();
        table.put('a', 0);

        Integer result = table.get('a');
        assertNotNull(result);
        assertEquals(0, result, "put('a', 0) 후 get('a')의 반환값은 0이어야 합니다.");
    }

    @Test
    @DisplayName("UT-HT-02: 해시 충돌 체이닝 검증")
    void testCollisionChaining() {
        // 작은 초기 용량을 지정하여 해시 충돌 강제 유발
        CustomHashTable<CollidingKey, String> table = new CustomHashTable<>(4, 1.0f);

        CollidingKey key1 = new CollidingKey("key1", 5);
        CollidingKey key2 = new CollidingKey("key2", 5); // key1과 동일한 hash code 반환

        table.put(key1, "Value1");
        table.put(key2, "Value2");

        // 동일 버킷에 연결 리스트 형태로 체이닝되었는지 확인
        assertEquals("Value1", table.get(key1));
        assertEquals("Value2", table.get(key2));
        assertTrue(table.containsKey(key1));
        assertTrue(table.containsKey(key2));
        assertEquals(2, table.size());
    }

    @Test
    @DisplayName("CustomHashTable 동적 리사이징 및 재해싱 검증")
    void testDynamicResizing() {
        CustomHashTable<Integer, String> table = new CustomHashTable<>(4, 0.75f);
        int initialCapacity = table.getCapacity();
        assertEquals(4, initialCapacity);

        // 3개 삽입 시 load factor 3/4 = 0.75 도달하여 2배 리사이징(용량 8)
        table.put(1, "one");
        table.put(2, "two");
        table.put(3, "three");

        assertTrue(table.getCapacity() > initialCapacity, "적재율 0.75 도달 시 버킷 용량이 2배로 확장되어야 합니다.");
        assertEquals("one", table.get(1));
        assertEquals("two", table.get(2));
        assertEquals("three", table.get(3));
    }

    /**
     * 충돌 테스트를 위해 의도적으로 지정된 해시 코드를 반환하는 헬퍼 클래스
     */
    static class CollidingKey {
        private final String name;
        private final int fixedHash;

        public CollidingKey(String name, int fixedHash) {
            this.name = name;
            this.fixedHash = fixedHash;
        }

        @Override
        public int hashCode() {
            return fixedHash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            CollidingKey that = (CollidingKey) obj;
            return name.equals(that.name);
        }
    }
}
