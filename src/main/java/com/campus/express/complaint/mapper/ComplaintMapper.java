package com.campus.express.complaint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.express.complaint.Complaint;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ComplaintMapper extends BaseMapper<Complaint> {
}
