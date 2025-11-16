local mutex = KEYS[1];
local contenderId = ARGV[1];
local transition = ARGV[2];

local mutexKey = 'arch:mutex:' .. mutex;

---定义获取当前持有者信息函数
local function getCurrentOwner(mKey)
	local result = {};
	local ownerId = redis.call('get', mKey);
	if ownerId then
		local ttl = redis.call('pttl', mKey);
		result['ownerId'] = ownerId;
		result['ttl'] = ttl;
		return cjson.encode(result);
	end ;
	result['ownerId'] = '';
	result['ttl'] = 0;
	return cjson.encode(result);
end;

---1.判断当前持有者是否为自己
---如果不是自己持有返回当前持有者信息
if redis.call('get', mutexKey) ~= contenderId then
	return getCurrentOwner(mutexKey);
end ;
---如果自己持有延长锁持有时间并返回持有者信息
if redis.call('set', mutexKey, contenderId, 'nx', 'px', transition) then
	local result = {};
	result['ownerId'] = contenderId;
	result['ttl'] = transition;
	return cjson.encode(result);
else
	return getCurrentOwner(mutexKey);
end ;

