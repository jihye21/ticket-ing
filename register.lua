-- KEYS[1] = wait_queue_key (ZSET)           e.g. "ticket:wait:{eventId}"
-- KEYS[2] = seq_key (STRING)                e.g. "ticket:seq:{eventId}"
-- KEYS[3] = proceed_set_key (SET)            e.g. "ticket:proceed:{eventId}" (optional)
-- ARGY[1] = userId (string)

local member = ARGV[1]

if KEYS[3] ~= nill and KEYS[3] ~= '' then
    if redis.call('SISMEMBER', KEYS[3], member) == 1 then
        return 0
    end 
end

local r = redis.call('ZRANK', KEYS[1], member)
if r ~= false and r ~= nil then
    return (r + 1)
end 

local seq = redis.call('INCR', KEYS[2])
redis.call('ZADD', KEYS[1], seq, member)

local newRank = redis.call('ZRANK', KEYS[1], member)
if newRank ~= false and newRank ~= nil then
    return (newRank + 1)
end 

return -1