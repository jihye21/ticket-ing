package com.example.ticket.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScript;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisException;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketQueueService {

    private final RedissonClient redissonClient;

    // Lua 스크립트와 SHA 값
    private String registerScript;
    @Getter
    private String registerScriptSha;

    private static final String WAIT_KEY_FMT = "ticket:wait:{%s}";
    private static final String SEQ_KEY_FMT = "ticket:seq:{%s}";
    private static final String PROCEED_KEY_FMT = "ticket:proceed:{%s}";

    @PostConstruct
    public void init() {
        registerScript =
            "local member = ARGV[1] " +
            "if KEYS[3] ~= nil and KEYS[3] ~= '' then " +
            "  if redis.call('SISMEMBER', KEYS[3], member) == 1 then return 0 end " +
            "end " +
            "local r = redis.call('ZRANK', KEYS[1], member) " +
            "if r ~= false and r ~= nil then return (r + 1) end " +
            "local seq = redis.call('INCR', KEYS[2]) " +
            "redis.call('ZADD', KEYS[1], seq, member) " +
            "local newRank = redis.call('ZRANK', KEYS[1], member) " +
            "if newRank ~= false and newRank ~= nil then return (newRank + 1) end " +
            "return -1";

        try {
            RScript rScript = redissonClient.getScript(StringCodec.INSTANCE);
            registerScriptSha = rScript.scriptLoad(registerScript);
            log.info("Lua register script loaded, sha={}", registerScriptSha);
        } catch (Exception e) {
            log.warn("Failed to load Lua script on startup: {}", e.getMessage());
            registerScriptSha = null;
        }
    }

    // Lua 호출
    public Long registerWithLua(String eventId, Long userId) {
        String waitKey = String.format(WAIT_KEY_FMT, eventId);
        String seqKey = String.format(SEQ_KEY_FMT, eventId);
        String proceedKey = String.format(PROCEED_KEY_FMT, eventId);

        RScript rScript = redissonClient.getScript(StringCodec.INSTANCE);
        List<Object> keys = List.of(waitKey, seqKey, proceedKey);
        Object[] args = new Object[]{ String.valueOf(userId) };

        try {
            if (registerScriptSha != null) {
                Object res = rScript.evalSha(RScript.Mode.READ_WRITE, registerScriptSha,
                        RScript.ReturnType.VALUE, keys, args);
                return parseLuaResult(res);
            }

            Object res = rScript.eval(RScript.Mode.READ_WRITE, registerScript,
                    RScript.ReturnType.VALUE, keys, args);
            try {
                registerScriptSha = rScript.scriptLoad(registerScript);
            } catch (Exception ignore) { }
            return parseLuaResult(res);

        } catch (RedisException rex) {
            if (rex.getMessage() != null && rex.getMessage().contains("NOSCRIPT")) {
                try {
                    Object res = rScript.eval(RScript.Mode.READ_WRITE, registerScript,
                            RScript.ReturnType.VALUE, keys, args);
                    try {
                        registerScriptSha = rScript.scriptLoad(registerScript);
                    } catch (Exception ignore) { }
                    return parseLuaResult(res);
                } catch (Exception ex) {
                    log.error("Lua eval fallback failed: {}", ex.getMessage(), ex);
                    return -1L;
                }
            }
            log.error("RedisException during Lua evalSha: {}", rex.getMessage(), rex);
            return -1L;
        } catch (Exception e) {
            log.error("Failed to execute Lua register script: {}", e.getMessage(), e);
            return -1L;
        }
    }

    // 읽기 전용 조회
    public Long getWaitRankReadOnly(String eventId, Long userId) {
        String waitKey = String.format(WAIT_KEY_FMT, eventId);
        RScoredSortedSet<String> waitQueue = redissonClient.getScoredSortedSet(waitKey);
        Integer rank = waitQueue.rank(String.valueOf(userId));
        if (rank == null) {
            return null;
        }
        return rank + 1L;
    }

    public boolean isAllowed(String eventId, Long userId) {
        String proceedKey = String.format(PROCEED_KEY_FMT, eventId);
        RSet<String> proceedSet = redissonClient.getSet(proceedKey);
        return proceedSet.contains(String.valueOf(userId));
    }

    private Long parseLuaResult(Object res) {
        if (res == null) return -1L;
        if (res instanceof Number) {
            return ((Number) res).longValue();
        }
        String s = String.valueOf(res);
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            log.warn("Unexpected Lua result type: {}", s);
            return -1L;
        }
    }

    // allowEntry
    public void allowEntry(String eventId, int userCount) {
        String waitKey = String.format(WAIT_KEY_FMT, eventId);
        String proceedKey = String.format(PROCEED_KEY_FMT, eventId);

        RScoredSortedSet<String> waitQueue = redissonClient.getScoredSortedSet(waitKey);
        RSet<String> proceedSet = redissonClient.getSet(proceedKey);

        Collection<String> popped = waitQueue.pollFirst(userCount);
        if (popped != null && !popped.isEmpty()) {
            proceedSet.addAll(popped);
            log.info("[allowEntry] event={}, allowed={}, proceedSize={}", eventId, popped.size(), proceedSet.size());
        }
    }
}