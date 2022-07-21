package com.valensas.ratelimiter.hazelcast;

import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.map.IMap;
import com.valensas.ratelimiter.hazelcast.serialization.HazelcastEntryProcessorSerializer;
import com.valensas.ratelimiter.hazelcast.serialization.HazelcastOffloadableEntryProcessorSerializer;
import io.github.bucket4j.distributed.proxy.AbstractProxyManager;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.remote.CommandResult;
import io.github.bucket4j.distributed.remote.Request;
import io.github.bucket4j.distributed.serialization.InternalSerializationHelper;
import io.github.bucket4j.distributed.versioning.Version;
import io.github.bucket4j.grid.hazelcast.HazelcastEntryProcessor;
import io.github.bucket4j.grid.hazelcast.HazelcastOffloadableEntryProcessor;
import io.github.bucket4j.grid.hazelcast.SimpleBackupProcessor;
import io.github.bucket4j.grid.hazelcast.serialization.SimpleBackupProcessorSerializer;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.github.bucket4j.distributed.serialization.InternalSerializationHelper.deserializeResult;

class HazelcastProxyManager<K> extends AbstractProxyManager<K> {


    private final IMap <K, byte[]> map;
    private final String offloadableExecutorName;

    public HazelcastProxyManager(IMap<K, byte[]> map) {
        this(map, ClientSideConfig.getDefault());
    }

    public HazelcastProxyManager(IMap<K, byte[]> map, ClientSideConfig clientSideConfig) {
        super(clientSideConfig);
        this.map = Objects.requireNonNull(map);
        this.offloadableExecutorName = null;
    }

    public HazelcastProxyManager(IMap<K, byte[]> map, ClientSideConfig clientSideConfig, String offlodableExecutorName) {
        super(clientSideConfig);
        this.map = Objects.requireNonNull(map);
        this.offloadableExecutorName = Objects.requireNonNull(offlodableExecutorName);
    }

    @Override
    public <T> CommandResult<T> execute(K key, Request<T> request) {
        HazelcastEntryProcessor<K, T> entryProcessor = offloadableExecutorName == null?
        new HazelcastEntryProcessor<>(request) :
        new HazelcastOffloadableEntryProcessor<>(request, offloadableExecutorName);
        byte[] response = map.executeOnKey(key, entryProcessor);
        Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
        return deserializeResult(response, backwardCompatibilityVersion);
    }

    @Override
    public boolean isAsyncModeSupported() {
        return true;
    }

    @Override
    public <T> CompletableFuture<CommandResult<T>> executeAsync(K key, Request<T> request) {
        HazelcastEntryProcessor<K, T> entryProcessor = offloadableExecutorName == null?
        new HazelcastEntryProcessor<>(request) :
        new HazelcastOffloadableEntryProcessor<>(request, offloadableExecutorName);
        CompletionStage<byte[]> future = map.submitToKey(key, entryProcessor);
        Version backwardCompatibilityVersion = request.getBackwardCompatibilityVersion();
        return (CompletableFuture) future.thenApply((byte[] bytes) -> deserializeResult(bytes, backwardCompatibilityVersion));
    }

    @Override
    public void removeProxy(K key) {
        map.remove(key);
    }

    @Override
    protected CompletableFuture<Void> removeAsync(K key) {
        CompletionStage<byte[]> hazelcastFuture = map.removeAsync(key);
        CompletableFuture<Void> resultFuture = new CompletableFuture<>();
        hazelcastFuture.whenComplete((oldState, error) -> {
        if (error == null) {
            resultFuture.complete(null);
        } else {
            resultFuture.completeExceptionally(error);
        }
    });
        return resultFuture;
    }

    /**
     * Registers custom Hazelcast serializers for all classes from Bucket4j library which can be transferred over network.
     * Each serializer will have different typeId, and this id will not be changed in the feature releases.
     *
     * <p>
     *     <strong>Note:</strong> it would be better to leave an empty space in the Ids in order to handle the extension of Bucket4j library when new classes can be added to library.
     *     For example if you called {@code getAllSerializers(10000)} then it would be reasonable to avoid registering your custom types in the interval 10000-10100.
     * </p>
     *
     * @param typeIdBase a starting number from for typeId sequence
     */
    public static void addCustomSerializers(SerializationConfig serializationConfig, final int typeIdBase) {
        serializationConfig.addSerializerConfig(
            new SerializerConfig()
                .setImplementation(new HazelcastEntryProcessorSerializer(typeIdBase))
                .setTypeClass(HazelcastEntryProcessor.class)
                );

        serializationConfig.addSerializerConfig(
            new SerializerConfig()
                .setImplementation(new SimpleBackupProcessorSerializer(typeIdBase + 1))
                .setTypeClass(SimpleBackupProcessor.class)
                );

        serializationConfig.addSerializerConfig(
            new SerializerConfig()
                .setImplementation(new HazelcastOffloadableEntryProcessorSerializer(typeIdBase + 2))
                .setTypeClass(HazelcastOffloadableEntryProcessor.class)
                );

    }
}