local keyMgr = KEYS[1];
local keyD1 = KEYS[2];
local keyD2 = KEYS[3];
local keyD3 = KEYS[4];
local keyD4 = KEYS[5];
local currentTime = tonumber(ARGV[1]);
local offLineMillisecond = tonumber(ARGV[2]);
local queueIdleMillisecond = tonumber(ARGV[3]);

local function allocationQueue(_keyD1, _keyD3, _keyD4, _queueName, _lastTime)
    local d4Exists = redis.call('EXISTS', _keyD4);
    if d4Exists == 1 then
        local d4MaQueNums = redis.call('ZRANGE', _keyD4, '0', '0', 'WITHSCORES')
        for i, d4Ma in ipairs(d4MaQueNums) do
            if i % 2 ~= 0 then
                local d4MaQueNum = tonumber(d4MaQueNums[i+1])
                local mkey3 = string.format("%s%s%s", _keyD3, '_', d4Ma);
                redis.call('SADD', mkey3, _queueName);
                redis.call('ZADD', _keyD4, (d4MaQueNum+1), d4Ma);
                redis.call('ZADD', _keyD1, _lastTime, _queueName);
            end
        end
    end
end

local function main()
    local d2Exists = redis.call('EXISTS', keyD2);
    if d2Exists == 1 then
        local d2Members = redis.call('ZRANGE', keyD2, '0', '-1', 'WITHSCORES')
        for i, member in ipairs(d2Members) do
            if i % 2 ~= 0 then
                local lastTime = d2Members[i+1]
                local subTime = currentTime-lastTime;
                if lastTime~=0 and subTime > offLineMillisecond then
                    local mkey3 = string.format("%s%s%s", keyD3, '_', member);
                    local d3Exists = redis.call('EXISTS', mkey3);
                    if d3Exists == 1 then
                        local qmRels = redis.call('SMEMBERS', mkey3)
                        for i, queue in ipairs(qmRels) do
                            redis.call('ZADD', keyD1, 0, queue);
                        end
                        redis.call('DEL', mkey3);
                    end
                    redis.call('ZREM', keyD4, member);
                    redis.call('ZREM', keyD2, member);
                end
            end
        end
    end

    local d1Exists = redis.call('EXISTS', keyD1);
    if d1Exists == 1 then
        local d1Queues = redis.call('ZRANGE', keyD1, '0', '-1', 'WITHSCORES')
        for i, queue in ipairs(d1Queues) do
            if i % 2 ~= 0 then
                local lastTime = d1Queues[i+1]
                local queueIdleTime = currentTime-lastTime;
                if queueIdleTime > queueIdleMillisecond then
                    allocationQueue(keyD1, keyD3, keyD4, queue, currentTime)
                end
            end
        end
    end

    local queueNum = redis.call('ZCARD', keyD1);
    local d4Exists = redis.call('EXISTS', keyD4);
    if d4Exists == 1 then
        local maNums = redis.call('ZCARD', keyD4);
        if maNums ~= 0 then
            local maQueNumMax = queueNum/maNums;
            local tmpNum = queueNum%maNums;
            if tmpNum ~= 0 then
                maQueNumMax = math.floor(queueNum/maNums)+1;
            end
            local d4MaQueNums = redis.call('ZREVRANGEBYSCORE', keyD4, queueNum, maQueNumMax, 'WITHSCORES')
            for i, d4Ma in ipairs(d4MaQueNums) do
                if i % 2 ~= 0 then
                    local d4MaQueNum = tonumber(d4MaQueNums[i+1])
                    local subQueNum = d4MaQueNum - maQueNumMax;
                    if subQueNum > 0 then
                        local mkey3 = string.format("%s%s%s", keyD3, '_', d4Ma);
                        local popQues = {}
                        local qmRels = redis.call('SMEMBERS', mkey3)
                        for i, queue in ipairs(qmRels) do
                            if i<=subQueNum then
                                popQues[i] = queue;
                                allocationQueue(keyD1, keyD3, keyD4, queue, currentTime)
                            end
                        end
                        for i, queue in ipairs(popQues) do
                            if i<=subQueNum then
                                redis.call('SREM', mkey3, queue);
                            end
                        end
                        redis.call('ZADD', keyD4, (d4MaQueNum-subQueNum), d4Ma);
                    end
                end
            end
        end
    end
    return 'true'
end

local initOk = redis.call('SETNX', keyMgr, '1');

if initOk ~= 1 then
    return 'empty run'
else
    redis.call('EXPIRE', keyMgr, 30);

    local result = main();
    redis.call('DEL', keyMgr);
    return result
end