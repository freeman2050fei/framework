local t = redis.call('time')
local timestamp = tonumber(t[1]) * 1000 + tonumber(t[2])/1000
return tostring(timestamp);