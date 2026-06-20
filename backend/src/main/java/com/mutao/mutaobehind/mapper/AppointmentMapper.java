package com.mutao.mutaobehind.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface AppointmentMapper {

    // 1. 获取某月所有预约日期标记
    @Select("SELECT DISTINCT DATE_FORMAT(appointment_date, '%Y-%m-%d') as date " +
            "FROM appointments " +
            "WHERE counselor_id = #{teacherId} " +
            "  AND DATE_FORMAT(appointment_date, '%Y-%m') = #{month} " +
            "  AND status != 'cancelled' " +
            "ORDER BY date")
    List<String> getDatesByMonth(@Param("teacherId") Long teacherId, @Param("month") String month);

    // 2. 获取某天的预约详情列表（含学生信息，使用 users 表的真实 avatar）
    @Select("SELECT a.id, " +
            "u.nickname as studentName, " +
            "u.avatar, " +
            "a.appointment_time as appointmentTime, " +
            "a.status, " +
            "a.reason " +
            "FROM appointments a " +
            "LEFT JOIN users u ON a.student_id = u.id " +
            "WHERE a.counselor_id = #{teacherId} " +
            "  AND a.appointment_date = #{date} " +
            "ORDER BY a.appointment_time ASC")
    List<Map<String, Object>> getByDate(@Param("teacherId") Long teacherId, @Param("date") String date);

    // 3. 修改预约状态
    @Update("UPDATE appointments SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    // 4. 学生端创建预约
    @Insert("INSERT INTO appointments(student_id, counselor_id, appointment_date, appointment_time, status, reason) " +
            "VALUES(#{studentId}, #{teacherId}, #{appointmentDate}, #{timeSlot}, #{status}, #{remark})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Map<String, Object> appointment);

    // 5. 查询某日某时段是否已被预约
    @Select("SELECT COUNT(*) FROM appointments " +
            "WHERE counselor_id = #{teacherId} " +
            "  AND appointment_date = #{date} " +
            "  AND appointment_time = #{timeSlot} " +
            "  AND status IN ('pending', 'confirmed')")
    int countConflict(@Param("teacherId") Long teacherId,
                      @Param("date") String date,
                      @Param("timeSlot") String timeSlot);

    // 6. 统计某月每天的预约数量
    @Select("SELECT DATE_FORMAT(appointment_date, '%Y-%m-%d') as date, COUNT(*) as count " +
            "FROM appointments " +
            "WHERE counselor_id = #{teacherId} " +
            "  AND DATE_FORMAT(appointment_date, '%Y-%m') = #{month} " +
            "  AND status != 'cancelled' " +
            "GROUP BY appointment_date " +
            "ORDER BY appointment_date")
    List<Map<String, Object>> getCountByMonth(@Param("teacherId") Long teacherId, @Param("month") String month);

    // 7. 【Dashboard】本月预约总数
    @Select("SELECT COUNT(*) FROM appointments " +
            "WHERE counselor_id = #{teacherId} " +
            "  AND DATE_FORMAT(appointment_date, '%Y-%m') = DATE_FORMAT(NOW(), '%Y-%m')")
    Integer countMonthAppointments(@Param("teacherId") Long teacherId);

    // 8. 【Dashboard】待确认预约数
    @Select("SELECT COUNT(*) FROM appointments " +
            "WHERE counselor_id = #{teacherId} " +
            "  AND status = 'pending'")
    Integer countPendingAppointments(@Param("teacherId") Long teacherId);
}
