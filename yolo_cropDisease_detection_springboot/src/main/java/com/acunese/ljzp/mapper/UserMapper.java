package com.acunese.ljzp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.acunese.ljzp.entity.User;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<User> {
    @Select("select * from tb_user where username=#{username}")
    User selectByName(String username);


    @Select("select * from tb_user where username = #{username}")
    User selectByUsername(String username);
}
