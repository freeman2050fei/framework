local keyInit = KEYS[1];
local keyD1 = KEYS[2];
local queueNum = tonumber(ARGV[1]);
local prefix_queue = ARGV[2];

local initOk = redis.call('SETNX', keyInit, '1');

if initOk == 1 then
    local d1Exists = redis.call('EXISTS', keyD1);
    if d1Exists ~= 1 then
        for i = 1, queueNum do
            redis.call('ZADD', keyD1, 0, prefix_queue..i);
        end
    end
    return 'true'
else
    return 'false'
end