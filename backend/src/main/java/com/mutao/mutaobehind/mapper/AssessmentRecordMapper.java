package com.mutao.mutaobehind.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface AssessmentRecordMapper {
    // 1. 学生交卷入库
    @Insert("INSERT INTO assessment_records(user_id, scale_type, scale_name, total_score, result_rating, detail_json) " +
            "VALUES(#{userId}, #{scaleType}, #{scaleName}, #{totalScore}, #{resultRating}, #{reportJson})")
    void insertRecord(@Param("userId") Long userId,
                      @Param("scaleType") String scaleType,
                      @Param("scaleName") String scaleName,
                      @Param("totalScore") Double totalScore,
                      @Param("resultRating") String resultRating,
                      @Param("reportJson") String reportJson);

    // 2. 所有成绩报表（关联 users 表）
    @Select("SELECT a.id, a.scale_type, a.scale_name, a.total_score, a.result_rating, " +
            "DATE_FORMAT(a.create_time, '%Y-%m-%d %H:%i') as create_time, " +
            "u.nickname " +
            "FROM assessment_records a " +
            "LEFT JOIN users u ON a.user_id = u.id " +
            "ORDER BY a.create_time DESC")
    List<Map<String, Object>> getAllRecordsForTeacher();

    // 3. 单条答卷详情
    @Select("SELECT a.id, a.user_id, a.scale_name, a.total_score, a.result_rating, " +
            "a.detail_json as report_json, " +
            "DATE_FORMAT(a.create_time, '%Y-%m-%d %H:%i') as create_time, u.nickname " +
            "FROM assessment_records a " +
            "LEFT JOIN users u ON a.user_id = u.id " +
            "WHERE a.id = #{id}")
    Map<String, Object> getRecordById(@Param("id") Long id);

    // 4. 【Dashboard】学生总数
    @Select("SELECT COUNT(DISTINCT u.id) FROM users u " +
            "INNER JOIN teacher_students ts ON ts.student_id = u.id AND ts.teacher_id = #{teacherId} " +
            "WHERE u.role = 'student'")
    Integer countStudentsByTeacherId(@Param("teacherId") Long teacherId);

    // 5. 【Dashboard】本月测评数（仅已绑定学生）
    @Select("SELECT COUNT(*) FROM assessment_records ar " +
            "INNER JOIN users u ON ar.user_id = u.id " +
            "INNER JOIN teacher_students ts ON ts.student_id = u.id AND ts.teacher_id = #{teacherId} " +
            "WHERE u.role = 'student' " +
            "  AND DATE_FORMAT(ar.create_time, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m')")
    Integer countMonthAssessmentsByTeacherId(@Param("teacherId") Long teacherId);

    // 6. 【Dashboard】预警人数（仅已绑定学生）
    @Select("SELECT COUNT(DISTINCT ar.user_id) FROM assessment_records ar " +
            "INNER JOIN users u ON ar.user_id = u.id " +
            "INNER JOIN teacher_students ts ON ts.student_id = u.id AND ts.teacher_id = #{teacherId} " +
            "WHERE u.role = 'student' " +
            "  AND DATE_FORMAT(ar.create_time, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m') " +
            "  AND (ar.result_rating = '阳性' OR ar.result_rating IN ('轻度抑郁','中度抑郁','重度抑郁'))")
    Integer countWarningByTeacherId(@Param("teacherId") Long teacherId);

    // 7. 【Dashboard】累计测评总数（仅已绑定学生）
    @Select("SELECT COUNT(*) FROM assessment_records ar " +
            "INNER JOIN users u ON ar.user_id = u.id " +
            "INNER JOIN teacher_students ts ON ts.student_id = u.id AND ts.teacher_id = #{teacherId} " +
            "WHERE u.role = 'student'")
    Integer countTotalAssessmentsByTeacherId(@Param("teacherId") Long teacherId);
}
