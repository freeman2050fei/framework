local keyD1 = KEYS[1];
local keyD2 = KEYS[2];
local keyD3 = KEYS[3];
local keyD4 = KEYS[4];
local machineId = ARGV[1];
local currentTime = tonumber(ARGV[2]);


local function notNull(value)
    if (value == nil or value == false or value == "" or value == {} or value == table.empty) then
        return false
    else
        return true
    end
end;


local mkey3 = string.format("%s%s%s", keyD3, '_', machineId);
local d3Exists = redis.call('EXISTS', mkey3);
if d3Exists == 1 then
    local qmRels = redis.call('SMEMBERS', mkey3)
    for i, queue in ipairs(qmRels) do
        redis.call('ZADD', keyD1, currentTime, queue);
    end
end

redis.call('ZADD', keyD2, currentTime, machineId);

local d4Exists = redis.call('EXISTS', keyD4);
if d4Exists ~= 1 then
    redis.call('ZADD', keyD4, 0, machineId);
else
   local d4MaExists = redis.call('ZRANK', keyD4, machineId);
    if notNull(d4MaExists) then
    else
        redis.call('ZADD', keyD4, 0, machineId);
    end
end
return 'success'