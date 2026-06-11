package com.acunese.ljzp.service;

import com.acunese.ljzp.dto.DeviceControlRequest;
import com.acunese.ljzp.dto.DeviceControlResponse;

public interface DeviceService {
    DeviceControlResponse executeControl(DeviceControlRequest request);
}

