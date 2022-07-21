package com.valensas.ratelimiter.hazelcast;

import com.hazelcast.map.IMap;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.AbstractLockBasedProxyManager;
import io.github.bucket4j.distributed.proxy.generic.pessimistic_locking.LockBasedTransaction;

import java.util.Objects;

public class HazelcastLockBasedProxyManager<K> extends AbstractLockBasedProxyManager<K> {

    private final IMap<K, byte[]> map;

    public HazelcastLockBasedProxyManager(IMap<K, byte[]> map) {
        this(map, ClientSideConfig.getDefault());
    }

    public HazelcastLockBasedProxyManager(IMap<K, byte[]> map, ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
        this.map = Objects.requireNonNull(map);
    }

    @Override
    public void removeProxy(K key) {
        map.remove(key);
    }

    @Override
    public boolean isAsyncModeSupported() {
        // Because Hazelcast IMap does not provide "lockAsync" API.
        return false;
    }

    @Override
    protected LockBasedTransaction allocateTransaction(K key) {
        return new LockBasedTransaction() {

            @Override
            public void begin() {
                // do nothing
            }

            @Override
            public void rollback() {
                // do nothing
            }

            @Override
            public void commit() {
                // do nothing
            }

            @Override
            public byte[] lockAndGet() {
                map.lock(key);
                return map.get(key);
            }

            @Override
            public void unlock() {
                map.unlock(key);
            }

            @Override
            public void create(byte[] data) {
                map.put(key, data);
            }

            @Override
            public void update(byte[] data) {
                map.put(key, data);
            }

            @Override
            public void release() {
                // do nothing
            }
        };
    }

}