package com.yuji.profile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuji.profile.entity.Profile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProfileMapper extends BaseMapper<Profile> {
}
