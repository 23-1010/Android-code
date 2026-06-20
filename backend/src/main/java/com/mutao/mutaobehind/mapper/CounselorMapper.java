package com.mutao.mutaobehind.mapper;

import com.mutao.mutaobehind.entity.Counselor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface CounselorMapper {

    // 获取所有在职咨询师（status=1）
    @Select("SELECT id, user_id as userId, name, title, avatar, " +
            "short_desc as shortDesc, full_desc as fullDesc, " +
            "specialties, phone, email, status, " +
            "create_time as createTime, update_time as updateTime " +
            "FROM counselors WHERE status = 1 ORDER BY id ASC")
    List<Counselor> getAllCounselors();

    // 通过 ID 获取单个咨询师
    @Select("SELECT id, user_id as userId, name, title, avatar, " +
            "short_desc as shortDesc, full_desc as fullDesc, " +
            "specialties, phone, email, status, " +
            "create_time as createTime, update_time as updateTime " +
            "FROM counselors WHERE id = #{id}")
    Counselor getCounselorById(@Param("id") Long id);

    // 通过 sys_user.id（= counselors.user_id）获取老师自己的咨询师资料
    @Select("SELECT id, user_id as userId, name, title, avatar, " +
            "short_desc as shortDesc, full_desc as fullDesc, " +
            "specialties, phone, email, status, " +
            "create_time as createTime, update_time as updateTime " +
            "FROM counselors WHERE user_id = #{userId}")
    Counselor getCounselorByUserId(@Param("userId") Long userId);

    // 更新咨询师个人资料
    @Update("UPDATE counselors SET " +
            "short_desc = #{shortDesc}, full_desc = #{fullDesc}, " +
            "specialties = #{specialties}, phone = #{phone}, email = #{email} " +
            "WHERE user_id = #{userId}")
    int updateProfileByUserId(Counselor counselor);

    // ── 绑定码 ──

    @Select("SELECT bind_code FROM counselors WHERE id = #{counselorId}")
    String getBindCodeByCounselorId(@Param("counselorId") Long counselorId);

    @Update("UPDATE counselors SET bind_code = #{code} WHERE id = #{counselorId}")
    int setBindCode(@Param("counselorId") Long counselorId, @Param("code") String code);

    @Select("SELECT id, name, title, avatar FROM counselors WHERE bind_code = #{code} LIMIT 1")
    Map<String, Object> getCounselorByBindCode(@Param("code") String code);
}
