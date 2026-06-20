package com.mutao.mutaobehind.entity;

import java.util.Date;

/**
 * 预约实体 — 对应 appointments 表
 */
public class Appointment {
    private Long id;
    private Long studentId;
    private Long counselorId;      // 咨询师ID（对应 counselors.id）
    private String appointmentDate; // 格式: 2026-06-19
    private String appointmentTime; // 如: 09:00-10:00
    private String status;          // pending / confirmed / cancelled / completed
    private String reason;          // 申请原因
    private String studentNotes;
    private String teacherNotes;
    private Date createTime;
    private Date updateTime;

    // 关联字段（非数据库列，查询时填充）
    private String studentName;
    private String studentAvatar;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getCounselorId() { return counselorId; }
    public void setCounselorId(Long counselorId) { this.counselorId = counselorId; }

    public String getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(String appointmentDate) { this.appointmentDate = appointmentDate; }

    public String getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(String appointmentTime) { this.appointmentTime = appointmentTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStudentNotes() { return studentNotes; }
    public void setStudentNotes(String studentNotes) { this.studentNotes = studentNotes; }

    public String getTeacherNotes() { return teacherNotes; }
    public void setTeacherNotes(String teacherNotes) { this.teacherNotes = teacherNotes; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentAvatar() { return studentAvatar; }
    public void setStudentAvatar(String studentAvatar) { this.studentAvatar = studentAvatar; }
}
