package com.mutao.mutaobehind.mapper;

import com.mutao.mutaobehind.entity.Assessment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AssessmentMapper {
    // 👇 获取列表时，查出 scale_type
    @Select("SELECT id, scale_type as scaleType, title, short_desc as shortDesc, description, questions_json as questionsJson, create_time as createTime FROM assessment")
    List<Assessment> getAllAssessments();

    // 👇 获取单个详情时，查出 scale_type
    @Select("SELECT id, scale_type as scaleType, title, short_desc as shortDesc, description, questions_json as questionsJson, create_time as createTime FROM assessment WHERE id = #{id}")
    Assessment getAssessmentById(Long id);
}