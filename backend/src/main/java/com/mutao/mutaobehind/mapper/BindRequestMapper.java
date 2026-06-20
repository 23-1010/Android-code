package com.mutao.mutaobehind.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface BindRequestMapper {

    // 学生提交绑定申请
    @Insert("INSERT INTO bind_requests (student_id, counselor_id, status) " +
            "VALUES (#{studentId}, #{counselorId}, 'pending')")
    int insertBindRequest(@Param("studentId") Long studentId,
                          @Param("counselorId") Long counselorId);

    // 查学生是否已有待处理申请
    @Select("SELECT id, status FROM bind_requests " +
            "WHERE student_id = #{studentId} AND status = 'pending' LIMIT 1")
    Map<String, Object> getPendingByStudentId(@Param("studentId") Long studentId);

    // 查学生是否已有已通过的绑定
    @Select("SELECT br.id, br.counselor_id as counselorId, c.name, c.title, c.avatar " +
            "FROM bind_requests br " +
            "LEFT JOIN counselors c ON c.id = br.counselor_id " +
            "WHERE br.student_id = #{studentId} AND br.status = 'approved' LIMIT 1")
    Map<String, Object> getApprovedTeacher(@Param("studentId") Long studentId);

    // 老师查待处理申请列表（含学生信息）
    @Select("SELECT br.id, br.student_id as studentId, br.status, " +
            "DATE_FORMAT(br.create_time, '%Y-%m-%d %H:%i') as createTime, " +
            "u.nickname, u.real_name as realName, u.avatar " +
            "FROM bind_requests br " +
            "LEFT JOIN users u ON u.id = br.student_id " +
            "WHERE br.counselor_id = #{counselorId} AND br.status = 'pending' " +
            "ORDER BY br.create_time DESC")
    List<Map<String, Object>> getPendingByCounselorId(@Param("counselorId") Long counselorId);

    // 老师审批
    @Update("UPDATE bind_requests SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    // 审批通过后，获取该申请对应的 student_id 和 counselor_id
    @Select("SELECT student_id as studentId, counselor_id as counselorId " +
            "FROM bind_requests WHERE id = #{id}")
    Map<String, Object> getRequestById(@Param("id") Long id);
}
