package com.notify.backend.service.impl;

import com.notify.backend.service.DeduplicationService;
import io.lettuce.core.RedisAsyncCommandsImpl;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.BooleanOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeduplicationServiceImpl implements DeduplicationService {

    private final StringRedisTemplate redisTemplate;

    private enum CuckooCommand implements ProtocolKeyword {
        CF_ADDNX("CF.ADDNX"),
        CF_EXISTS("CF.EXISTS"),
        CF_DEL("CF.DEL");

        private final byte[] bytes;

        CuckooCommand(String name) {
            this.bytes = name.getBytes(StandardCharsets.US_ASCII);
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }
    }

    @Override
    public boolean addIfAbsent(Long campaignId, String externalUserId) {
        byte[] key  = filterKey(campaignId);
        byte[] item = externalUserId.getBytes(StandardCharsets.UTF_8);
        boolean isNew = dispatch(CuckooCommand.CF_ADDNX, key, item);
        log.debug("CF.ADDNX campaignId={} userId={} → {}", campaignId, externalUserId, isNew ? "NEW" : "DUPLICATE");
        return isNew;
    }

    @Override
    public boolean exists(Long campaignId, String externalUserId) {
        byte[] key  = filterKey(campaignId);
        byte[] item = externalUserId.getBytes(StandardCharsets.UTF_8);
        return dispatch(CuckooCommand.CF_EXISTS, key, item);
    }

    @Override
    public void delete(Long campaignId, String externalUserId) {
        byte[] key  = filterKey(campaignId);
        byte[] item = externalUserId.getBytes(StandardCharsets.UTF_8);
        dispatch(CuckooCommand.CF_DEL, key, item);
        log.debug("CF.DEL campaignId={} userId={}", campaignId, externalUserId);
    }

    @Override
    public boolean addCohortMemberIfAbsent(Long cohortId, String externalUserId) {
        byte[] key  = cohortFilterKey(cohortId);
        byte[] item = externalUserId.getBytes(StandardCharsets.UTF_8);
        boolean isNew = dispatch(CuckooCommand.CF_ADDNX, key, item);
        log.debug("CF.ADDNX cohortId={} userId={} → {}", cohortId, externalUserId, isNew ? "NEW" : "DUPLICATE");
        return isNew;
    }

    @Override
    public void removeCohortMember(Long cohortId, String externalUserId) {
        byte[] key  = cohortFilterKey(cohortId);
        byte[] item = externalUserId.getBytes(StandardCharsets.UTF_8);
        dispatch(CuckooCommand.CF_DEL, key, item);
        log.debug("CF.DEL cohortId={} userId={}", cohortId, externalUserId);
    }

    @SuppressWarnings("unchecked")
    private boolean dispatch(CuckooCommand command, byte[] key, byte[] item) {
        Boolean result = redisTemplate.execute((RedisCallback<Boolean>) conn -> {
            // conn may be a Spring transaction proxy — getNativeConnection() is defined on
            // RedisConnection itself so it safely delegates through the proxy without casting
            Object native_ = conn.getNativeConnection();

            CommandArgs<byte[], byte[]> args = new CommandArgs<>(ByteArrayCodec.INSTANCE)
                    .addKey(key)
                    .add(item);

            if (native_ instanceof StatefulRedisConnection<?, ?> stateful) {
                return ((StatefulRedisConnection<byte[], byte[]>) stateful).sync()
                        .dispatch(command, new BooleanOutput<>(ByteArrayCodec.INSTANCE), args);
            }
            if (native_ instanceof StatefulRedisClusterConnection<?, ?> cluster) {
                return ((StatefulRedisClusterConnection<byte[], byte[]>) cluster).sync()
                        .dispatch(command, new BooleanOutput<>(ByteArrayCodec.INSTANCE), args);
            }
            if (native_ instanceof RedisCommands<?, ?> sync) {
                return ((RedisCommands<byte[], byte[]>) sync)
                        .dispatch(command, new BooleanOutput<>(ByteArrayCodec.INSTANCE), args);
            }
            if (native_ instanceof RedisClusterCommands<?, ?> clusterSync) {
                return ((RedisClusterCommands<byte[], byte[]>) clusterSync)
                        .dispatch(command, new BooleanOutput<>(ByteArrayCodec.INSTANCE), args);
            }
            // Lettuce returns RedisAsyncCommandsImpl when shareNativeConnection=false;
            // getStatefulConnection() on RedisAsyncCommandsImpl returns StatefulRedisConnection directly
            if (native_ instanceof RedisAsyncCommandsImpl<?, ?> async) {
                return ((RedisAsyncCommandsImpl<byte[], byte[]>) async)
                        .getStatefulConnection().sync()
                        .dispatch(command, new BooleanOutput<>(ByteArrayCodec.INSTANCE), args);
            }
            throw new IllegalStateException(
                    "Unsupported native Redis connection type: " + native_.getClass().getName());
        });
        return Boolean.TRUE.equals(result);
    }

    private byte[] filterKey(Long campaignId) {
        return ("cuckoo:campaign:" + campaignId).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] cohortFilterKey(Long cohortId) {
        return ("cuckoo:cohort:" + cohortId).getBytes(StandardCharsets.UTF_8);
    }
}