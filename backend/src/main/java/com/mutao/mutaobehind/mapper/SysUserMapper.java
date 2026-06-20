package com.mutao.mutaobehind.mapper;

import com.mutao.mutaobehind.entity.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysUserMapper {

    // ── 用户表查询（表名请确认是 users 还是 sys_user，此处用 users） ──

    @Select("SELECT id, openid, nickname, avatar, role, counselor_id as counselorId, " +
            "real_name as realName, gender, birth_date as birthDate, " +
            "create_time as createTime, update_time as updateTime " +
            "FROM users WHERE openid = #{openid}")
    SysUser getUserByOpenid(String openid);

    @Insert("INSERT INTO users(openid, nickname, role) VALUES(#{openid}, '微信用户', 'student')")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertUser(SysUser user);

    @Select("SELECT id, openid, nickname, avatar, role, counselor_id as counselorId, " +
            "real_name as realName, gender, birth_date as birthDate, " +
            "create_time as createTime, update_time as updateTime " +
            "FROM users WHERE id = #{id}")
    SysUser getUserById(@Param("id") Long id);

    // ── 学生个人资料更新 ──

    @Update("UPDATE users SET real_name = #{realName}, gender = #{gender}, " +
            "birth_date = #{birthDate} WHERE id = #{userId}")
    int updateUserProfile(@Param("userId") Long userId,
                          @Param("realName") String realName,
                          @Param("gender") String gender,
                          @Param("birthDate") String birthDate);

    // 获取学生完整资料（含年龄计算）
    @Select("SELECT id, nickname, avatar, real_name as realName, gender, " +
            "birth_date as birthDate, role, " +
            "TIMESTAMPDIFF(YEAR, birth_date, CURDATE()) as age " +
            "FROM users WHERE id = #{userId}")
    Map<String, Object> getUserProfile(@Param("userId") Long userId);

    // ── 老师-学生关联 ──

    @Select("SELECT DISTINCT u.id, u.nickname, u.avatar, u.real_name as realName, " +
            "u.gender, u.birth_date as birthDate, u.create_time as createTime, " +
            "TIMESTAMPDIFF(YEAR, u.birth_date, CURDATE()) as age, " +
            "(SELECT ar.result_rating FROM assessment_records ar WHERE ar.user_id = u.id ORDER BY ar.create_time DESC LIMIT 1) as lastRating, " +
            "(SELECT DATE_FORMAT(ar.create_time, '%Y-%m-%d') FROM assessment_records ar WHERE ar.user_id = u.id ORDER BY ar.create_time DESC LIMIT 1) as lastTestDate " +
            "FROM users u " +
            "LEFT JOIN teacher_students ts ON ts.student_id = u.id AND ts.teacher_id = #{counselorId} " +
            "LEFT JOIN appointments apt ON apt.student_id = u.id AND apt.counselor_id = #{counselorId} " +
            "LEFT JOIN assessment_records ar ON ar.user_id = u.id " +
            "WHERE u.role = 'student' AND ts.id IS NOT NULL " +
            "ORDER BY u.create_time DESC")
    List<Map<String, Object>> getStudentsByTeacherId(@Param("counselorId") Long counselorId, @Param("teacherId") Long teacherId);

    @Select("SELECT ar.id, ar.scale_name, ar.total_score, " +
            "ar.result_rating, ar.scale_type, " +
            "DATE_FORMAT(ar.create_time, '%Y-%m-%d %H:%i') as create_time " +
            "FROM assessment_records ar " +
            "WHERE ar.user_id = #{studentId} " +
            "ORDER BY ar.create_time DESC")
    List<Map<String, Object>> getStudentAssessmentRecords(@Param("studentId") Long studentId);

    // ── 老师对学生备注 ──

    @Select("SELECT notes FROM teacher_students WHERE teacher_id = #{teacherId} AND student_id = #{studentId}")
    String getTeacherNotes(@Param("teacherId") Long teacherId, @Param("studentId") Long studentId);

    @Insert("INSERT INTO teacher_students (teacher_id, student_id, notes) " +
            "VALUES (#{teacherId}, #{studentId}, #{notes}) " +
            "ON DUPLICATE KEY UPDATE notes = #{notes}")
    int upsertTeacherNotes(@Param("teacherId") Long teacherId,
                           @Param("studentId") Long studentId,
                           @Param("notes") String notes);

    // ── counselorId → userId 反查 ──

    /** 通过 counselors.id（咨询师ID）查找对应的用户ID（users.id） */
    @Select("SELECT u.id FROM users u WHERE u.counselor_id = #{counselorId} AND u.role = 'teacher' LIMIT 1")
    Long getTeacherUserIdByCounselorId(@Param("counselorId") Long counselorId);

    /** 通过 counselors.id 查找用户ID（旧名称，与上面等价） */
    @Select("SELECT id FROM users WHERE counselor_id = #{counselorId} AND role = 'teacher' LIMIT 1")
    Long getTeacherIdByCounselorId(@Param("counselorId") Long counselorId);
}
