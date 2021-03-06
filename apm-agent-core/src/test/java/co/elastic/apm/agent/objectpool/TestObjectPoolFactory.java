/*-
 * #%L
 * Elastic APM Java agent
 * %%
 * Copyright (C) 2018 - 2020 Elastic and contributors
 * %%
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */
package co.elastic.apm.agent.objectpool;

import co.elastic.apm.agent.objectpool.impl.BookkeeperObjectPool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Extension of default pool factory that keeps track of all pools and thus allows to query their state while testing
 */
public class TestObjectPoolFactory extends ObjectPoolFactory {

    private final List<BookkeeperObjectPool<?>> createdPools = new ArrayList<BookkeeperObjectPool<?>>();

    @Override
    protected <T extends Recyclable> ObjectPool<T> createRecyclableObjectPool(int maxCapacity, Allocator<T> allocator) {
        ObjectPool<T> pool = super.createRecyclableObjectPool(maxCapacity, allocator);
        BookkeeperObjectPool<T> wrappedPool = new BookkeeperObjectPool<>(pool);
        createdPools.add(wrappedPool);
        return wrappedPool;
    }

    public void checkAllPooledObjectsHaveBeenRecycled() {
        assertThat(createdPools)
            .describedAs("at least one object pool should have been created, test object pool factory likely not used whereas it should")
            .isNotEmpty();
        for (BookkeeperObjectPool<?> pool : createdPools) {
            Collection<?> toReturn = pool.getRecyclablesToReturn();
            String pooledItemClass = toReturn.stream().findFirst().map(e -> e.getClass().getName()).orElse("?");
            assertThat(toReturn)
                .describedAs("pool should have all its items recycled : instance = %s, class = %s", toReturn, pooledItemClass)
                .isEmpty();
        }
    }

}
