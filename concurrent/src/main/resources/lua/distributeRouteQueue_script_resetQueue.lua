local keyD1 = KEYS[1];
local keyD2 = KEYS[2];
local keyD3 = KEYS[3];
local keyD4 = KEYS[4];
local prefix_queue = ARGV[1];
local newQueueNum = tonumber(ARGV[2]);

local d2Exists = redis.call('EXISTS', keyD2);
if d2Exists == 1 then
    local d2Members = redis.call('ZRANGE', keyD2, '0', '-1', 'WITHSCORES')
    for i, member in ipairs(d2Members) do
        if i % 2 ~= 0 then
            local mkey3 = string.format("%s%s%s", keyD3, '_', member);
            redis.call('DEL', mkey3);
            redis.call('ZADD', keyD4, 0, member);
        end
    end
end
local oldQueueNum = redis.call('ZCARD', keyD1);

if oldQueueNum >= newQueueNum then
    return 'false'
end

local max = newQueueNum;
if newQueueNum < oldQueueNum then
    max = oldQueueNum
end

for i = 1, max do
    if i > oldQueueNum then
        redis.call('ZADD', keyD1, 0, prefix_queue..i);
    end
    if i > newQueueNum then
        redis.call('ZREM', keyD1, prefix_queue..i);
    end
end

return 'true'