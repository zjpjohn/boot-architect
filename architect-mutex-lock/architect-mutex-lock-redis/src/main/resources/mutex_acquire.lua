redis.replicate_commands();

local mutex = KEYS[1];

local contenderId = ARGV[1];
local transition = ARGV[2];

local mutexKey = 'arch:mutex:' .. mutex;

local succeed = redis.call('set', mutexKey, contenderId, 'nx', 'px', transition);
local result = {};
if succeed then
	local message = {};
	message['event'] = 'acquired';
	message['ownerId'] = contenderId;
	--推送竞争获得锁事件
	redis.call('publish', mutexKey, cjson.encode(message));
	-- 返回锁持有者信息
	result['ownerId'] = contenderId;
	result['ttl'] = transition;
	return cjson.encode(result);
end ;

local contenderQueueKey = mutexKey .. ':contender';
local nowTime = redis.call('time')[1];
redis.call('zadd', contenderQueueKey, 'nx', nowTime, contenderId);

local ownerId = redis.call('get', mutexKey);
local ttl = redis.call('pttl', mutexKey);
--返回锁持有者信息
result['ownerId'] = ownerId;
result['ttl'] = ttl;
return cjson.encode(result);
