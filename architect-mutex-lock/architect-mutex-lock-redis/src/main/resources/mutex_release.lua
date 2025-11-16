local mutex = KEYS[1];
local contenderId = ARGV[1];

local mutexKey = 'arch:mutex:' .. mutex;
local contenderQueueKey = mutexKey .. ':contender';

---1.获取当前锁是否自己持有，如果没有持有直接退出，返回释放失败
if redis.call('get', mutexKey) ~= contenderId then
	redis.call('zrem', contenderQueueKey, contenderId);
	return 0;
end
---2.如果自己持有删除锁定资源
local succeed = redis.call('del', mutexKey);
if not succeed then
	return succeed;
end ;

---3.从等待队列中获取第一个contender发布锁释放通知
local contenderQueue = redis.call('zrevrange', contenderQueueKey, -1, -1);
if #contenderQueue == 0 then
	return succeed;
end ;

---4.向等待队列中第一个contender发布释放锁通知
local nextContender = contenderQueue[1];
redis.call('zrem', contenderQueueKey, nextContender);

local channel = mutexKey .. ':' .. nextContender;
local message = {};
message['event'] = 'released';
message['ownerId'] = contenderId;
redis.call('publish', channel, cjson.encode(message));

return succeed;
