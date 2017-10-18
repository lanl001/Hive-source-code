/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.ql.exec.vector.mapjoin.fast;

import java.io.IOException;
import java.util.Random;

import org.apache.hadoop.hive.ql.exec.JoinUtil;
import org.apache.hadoop.hive.ql.exec.vector.mapjoin.hashtable.VectorMapJoinHashMultiSetResult;
import org.apache.hadoop.hive.ql.exec.vector.mapjoin.fast.CheckFastHashTable.VerifyFastLongHashMultiSet;
import org.apache.hadoop.hive.ql.exec.vector.mapjoin.fast.VectorMapJoinFastLongHashMultiSet;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.plan.VectorMapJoinDesc.HashTableKeyType;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestVectorMapJoinFastLongHashMultiSet extends CommonFastHashTable {

  @Test
  public void testOneKey() throws Exception {
    random = new Random(458);

    VectorMapJoinFastLongHashMultiSet map =
        new VectorMapJoinFastLongHashMultiSet(
            false, false, HashTableKeyType.LONG, CAPACITY, LOAD_FACTOR, WB_SIZE);

    VerifyFastLongHashMultiSet verifyTable = new VerifyFastLongHashMultiSet();

    long key = random.nextLong();

    map.testPutRow(key);
    verifyTable.add(key);
    verifyTable.verify(map);

    // Second time.
    map.testPutRow(key);
    verifyTable.add(key);
    verifyTable.verify(map);

    // Third time.
     map.testPutRow(key);
    verifyTable.add(key);
    verifyTable.verify(map);
  }

  @Test
  public void testMultipleKeysSingleValue() throws Exception {
    random = new Random(8000);

    VectorMapJoinFastLongHashMultiSet map =
        new VectorMapJoinFastLongHashMultiSet(
            false, false, HashTableKeyType.LONG, CAPACITY, LOAD_FACTOR, WB_SIZE);

    VerifyFastLongHashMultiSet verifyTable = new VerifyFastLongHashMultiSet();

    int keyCount = 100 + random.nextInt(1000);
    for (int i = 0; i < keyCount; i++) {
      long key;
      while (true) {
        key = random.nextLong();
        if (!verifyTable.contains(key)) {
          // Unique keys for this test.
          break;
        }
      }

      map.testPutRow(key);
      verifyTable.add(key);
      // verifyTable.verify(map);
    }
    verifyTable.verify(map);
  }

  @Test
  public void testGetNonExistent() throws Exception {
    random = new Random(4000);

    VectorMapJoinFastLongHashMultiSet map =
        new VectorMapJoinFastLongHashMultiSet(
            false, false, HashTableKeyType.LONG, CAPACITY, LOAD_FACTOR, WB_SIZE);

    VerifyFastLongHashMultiSet verifyTable = new VerifyFastLongHashMultiSet();

    long key1 = random.nextLong();

    map.testPutRow(key1);
    verifyTable.add(key1);
    verifyTable.verify(map);

    long key2 = key1 += 1;
    VectorMapJoinHashMultiSetResult hashMultiSetResult = map.createHashMultiSetResult();
    JoinUtil.JoinResult joinResult = map.contains(key2, hashMultiSetResult);
    assertTrue(joinResult == JoinUtil.JoinResult.NOMATCH);
    assertEquals(hashMultiSetResult.count(), 0);

    map.testPutRow(key2);
    verifyTable.add(key2);
    verifyTable.verify(map);

    long key3 = key2 += 1;
    hashMultiSetResult = map.createHashMultiSetResult();
    joinResult = map.contains(key3, hashMultiSetResult);
    assertTrue(joinResult == JoinUtil.JoinResult.NOMATCH);
    assertEquals(hashMultiSetResult.count(), 0);
  }

  @Test
  public void testFullMap() throws Exception {
    random = new Random(25000);

    // Make sure the map does not expand; should be able to find space.
    VectorMapJoinFastLongHashMultiSet map =
        new VectorMapJoinFastLongHashMultiSet(
            false, false, HashTableKeyType.LONG, CAPACITY, 1f, WB_SIZE);

    VerifyFastLongHashMultiSet verifyTable = new VerifyFastLongHashMultiSet();

    for (int i = 0; i < CAPACITY; i++) {
      long key;
      while (true) {
        key = random.nextLong();
        if (!verifyTable.contains(key)) {
          // Unique keys for this test.
          break;
        }
      }

      map.testPutRow(key);
      verifyTable.add(key);
      // verifyTable.verify(map);
    }
    verifyTable.verify(map);

    long anotherKey;
    while (true) {
      anotherKey = random.nextLong();
      if (!verifyTable.contains(anotherKey)) {
        // Unique keys for this test.
        break;
      }
    }

    VectorMapJoinHashMultiSetResult hashMultiSetResult = map.createHashMultiSetResult();
    JoinUtil.JoinResult joinResult = map.contains(anotherKey, hashMultiSetResult);
    assertTrue(joinResult == JoinUtil.JoinResult.NOMATCH);
  }

  @Test
  public void testExpand() throws Exception {
    random = new Random(30000);

    // Start with capacity 1; make sure we expand on every put.
    VectorMapJoinFastLongHashMultiSet map =
        new VectorMapJoinFastLongHashMultiSet(
            false, false, HashTableKeyType.LONG, 1, 0.0000001f, WB_SIZE);

    VerifyFastLongHashMultiSet verifyTable = new VerifyFastLongHashMultiSet();

    for (int i = 0; i < 18; ++i) {
      long key;
      while (true) {
        key = random.nextLong();
        if (!verifyTable.contains(key)) {
          // Unique keys for this test.
          break;
        }
      }

      map.testPutRow(key);
      verifyTable.add(key);
      // verifyTable.verify(map);
    }
    verifyTable.verify(map);
    // assertEquals(1 << 18, map.getCapacity());
  }

  public void addAndVerifyMultipleKeyMultipleValue(int keyCount,
      VectorMapJoinFastLongHashMultiSet map, VerifyFastLongHashMultiSet verifyTable)
          throws HiveException, IOException {
    for (int i = 0; i < keyCount; i++) {
      byte[] value = new byte[generateLargeCount() - 1];
      random.nextBytes(value);

      // Add a new key or add a value to an existing key?
      if (random.nextBoolean() || verifyTable.getCount() == 0) {
        long key;
        while (true) {
          key = random.nextLong();
          if (!verifyTable.contains(key)) {
            // Unique keys for this test.
            break;
          }
        }

        map.testPutRow(key);
        verifyTable.add(key);
        verifyTable.verify(map);
      } else {
        long randomExistingKey = verifyTable.addRandomExisting(value, random);
        map.testPutRow(randomExistingKey);
        // verifyTable.verify(map);
      }
      verifyTable.verify(map);
    }
  }
  @Test
  public void testMultipleKeysMultipleValue() throws Exception {
    random = new Random(333);

    // Use a large capacity that doesn't require expansion, yet.
    VectorMapJoinFastLongHashMultiSet map =
        new VectorMapJoinFastLongHashMultiSet(
            false, false, HashTableKeyType.LONG, LARGE_CAPACITY, LOAD_FACTOR, LARGE_WB_SIZE);

    VerifyFastLongHashMultiSet verifyTable = new VerifyFastLongHashMultiSet();

    int keyCount = 1000;
    addAndVerifyMultipleKeyMultipleValue(keyCount, map, verifyTable);
  }

  @Test
  public void testLargeAndExpand() throws Exception {
    random = new Random(790);

    // Use a large capacity that doesn't require expansion, yet.
    VectorMapJoinFastLongHashMultiSet map =
        new VectorMapJoinFastLongHashMultiSet(
            false, false, HashTableKeyType.LONG, MODERATE_CAPACITY, LOAD_FACTOR, MODERATE_WB_SIZE);

    VerifyFastLongHashMultiSet verifyTable = new VerifyFastLongHashMultiSet();

    int keyCount = 1000;
    addAndVerifyMultipleKeyMultipleValue(keyCount, map, verifyTable);
  }
}
