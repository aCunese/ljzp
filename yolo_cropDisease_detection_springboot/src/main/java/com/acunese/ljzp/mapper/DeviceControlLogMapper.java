package com.acunese.ljzp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.acunese.ljzp.entity.DeviceControlLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface DeviceControlLogMapper extends BaseMapper<DeviceControlLog> {

    @Update("UPDATE tb_device_control_log SET status=#{status}, response=#{response} WHERE id=#{id}")
    int updateStatusAndResponse(@Param("id") Long id,
                                @Param("status") String status,
                                @Param("response") String response);
}
