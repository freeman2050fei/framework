local keyD2 = KEYS[1];
local keyD4 = KEYS[2];
local machineId = ARGV[1];
local currentTime = tonumber(ARGV[2]);


local function notNull(value)
    if (value == nil or value == false or value == "" or value == {} or value == table.empty) then
        return false
    else
        return true
    end
end;

redis.call('ZADD', keyD2, currentTime, machineId);

local d4Exists = redis.call('EXISTS', keyD4);
if d4Exists ~= 1 then
    redis.call('ZADD', keyD4, 0, machineId);
else
    local d4MachineExists = redis.call('ZRANK', keyD4, machineId);
    if notNull(d4MachineExists) then
    else
        redis.call('ZADD', keyD4, 0, machineId);
    end
end
return 'true'