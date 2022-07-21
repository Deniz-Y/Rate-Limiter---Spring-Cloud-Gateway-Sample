package com.valensas.ratelimiter.hazelcast;

import com.hazelcast.core.Offloadable;
import io.github.bucket4j.distributed.remote.Request;

public class HazelcastOffloadableEntryProcessor<K, T> extends HazelcastEntryProcessor<K, T> implements Offloadable {

    private static final long serialVersionUID = 1L;

    private final String executorName;

    public HazelcastOffloadableEntryProcessor(Request<T> request, String executorName) {
        super(request);
        this.executorName = executorName;
    }

    public HazelcastOffloadableEntryProcessor(byte[] requestBytes, String executorName) {
        super(requestBytes);
        this.executorName = executorName;
    }

    @Override
    public String getExecutorName() {
        return executorName;
    }

}