import request from '/@/utils/request';

export const fetchSensorDevices = () => {
	return request({
		url: '/api/sensor/devices',
		method: 'get',
	});
};

export const fetchLatestSensorData = (deviceId?: string) => {
	return request({
		url: '/api/sensor/latest',
		method: 'get',
		params: deviceId ? { deviceId } : undefined,
	});
};

export const fetchSensorHistory = (params: {
	deviceId?: string;
	startTime?: string;
	endTime?: string;
}) => {
	return request({
		url: '/api/sensor/history',
		method: 'get',
		params,
	});
};

export const fetchSensorSummary = (deviceId?: string) => {
	return fetchLatestSensorData(deviceId).then((res: any) => {
		if (res?.code !== 0 && res?.code !== '0') return res;

		const latest = res?.data || {};
		const latestTimestamp = latest.timestamp ? new Date(latest.timestamp) : null;
		const isOnline =
			latestTimestamp instanceof Date &&
			!Number.isNaN(latestTimestamp.getTime()) &&
			Date.now() - latestTimestamp.getTime() <= 20 * 60 * 1000;

		return {
			...res,
			data: {
				deviceId: latest.deviceId || deviceId || '',
				deviceName: latest.deviceId || deviceId || '未命名设备',
				online: isOnline,
				signalStrength: isOnline ? 88 : 0,
				lastHeartbeat: latest.timestamp || null,
				batteryLevel: null,
				statusText: isOnline ? '在线' : '离线',
				extra: latest,
			},
		};
	});
};

export const fetchSensorTrend = (params: {
	deviceId?: string;
	range?: string;
}) => {
	const range = (params.range || '1h').toLowerCase();
	const rangeHours = range === '6h' ? 6 : range === '24h' || range === '1d' ? 24 : 1;
	const end = new Date();
	const start = new Date(end.getTime() - rangeHours * 60 * 60 * 1000);

	return fetchSensorHistory({
		deviceId: params.deviceId,
		startTime: formatDateTime(start),
		endTime: formatDateTime(end),
	}).then((res: any) => {
		if (res?.code !== 0 && res?.code !== '0') return res;
		const history = Array.isArray(res?.data) ? res.data : [];
		const points = [...history]
			.sort((a: any, b: any) => {
				const timeA = new Date(a?.timestamp || 0).getTime();
				const timeB = new Date(b?.timestamp || 0).getTime();
				return timeA - timeB;
			})
			.map((item: any) => ({
				timestamp: item.timestamp,
				airTemperature: item.airTemperature ?? item.temperature ?? null,
				airHumidity: item.airHumidity ?? item.humidity ?? null,
				soilHumidity: item.soilHumidity ?? item.soilMoisture ?? null,
				lightIntensity: item.lightIntensity ?? null,
				waterLevel: item.waterLevel ?? null,
			}));

		return {
			...res,
			data: points,
		};
	});
};

const formatDateTime = (date: Date) => {
	const y = date.getFullYear();
	const m = String(date.getMonth() + 1).padStart(2, '0');
	const d = String(date.getDate()).padStart(2, '0');
	const h = String(date.getHours()).padStart(2, '0');
	const mm = String(date.getMinutes()).padStart(2, '0');
	const s = String(date.getSeconds()).padStart(2, '0');
	return `${y}-${m}-${d} ${h}:${mm}:${s}`;
};
