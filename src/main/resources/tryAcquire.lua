local key = KEYS[1]
local now = tonumber(ARGV[1])
local windowStart = tonumber(ARGV[2])
local ttl = tonumber(ARGV[3])
local maxCount = tonumber(ARGV[4])
local random =tonumber(ARGV[5])

redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)
local cnt = redis.call('ZCARD', key)

if cnt < maxCount then
    redis.call('ZADD', key, now, now .. ':' .. random)
    redis.call('EXPIRE', key, ttl + 10)
    return 1
end
return 0
