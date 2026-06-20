-- ============================================================
-- 慕陶心理小程序 - 数据库初始化/升级脚本
-- 请在 mutao_base 库上执行
-- 兼容 MySQL 8.0（不使用 IF NOT EXISTS）
-- ============================================================

USE mutao_base;

-- 1. 补充 counselors 表的字段（使用存储过程模拟 IF NOT EXISTS）
DROP PROCEDURE IF EXISTS add_column_if_not_exists;
DELIMITER //
CREATE PROCEDURE add_column_if_not_exists(
    IN tbl VARCHAR(64), IN col VARCHAR(64), IN col_def VARCHAR(256))
BEGIN
    DECLARE cnt INT DEFAULT 0;
    SELECT COUNT(*) INTO cnt FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = tbl
      AND COLUMN_NAME = col;
    IF cnt = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', tbl, ' ADD COLUMN ', col, ' ', col_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_column_if_not_exists('counselors', 'full_desc',   'TEXT          COMMENT "详细介绍"');
CALL add_column_if_not_exists('counselors', 'specialties', 'VARCHAR(500)  DEFAULT "" COMMENT "擅长领域"');
CALL add_column_if_not_exists('counselors', 'phone',       'VARCHAR(20)   DEFAULT "" COMMENT "联系电话"');
CALL add_column_if_not_exists('counselors', 'email',       'VARCHAR(100)  DEFAULT "" COMMENT "邮箱"');
CALL add_column_if_not_exists('counselors', 'status',      'TINYINT       DEFAULT 1  COMMENT "状态: 1=在职, 0=离职"');

-- 2. 预约表
CREATE TABLE IF NOT EXISTS appointments (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    student_id       INT          NOT NULL COMMENT '学生用户ID',
    counselor_id     INT          NOT NULL COMMENT '咨询师ID',
    appointment_date DATE         NOT NULL COMMENT '预约日期',
    appointment_time VARCHAR(20)  NOT NULL COMMENT '时间段，如 09:00-10:00',
    status           ENUM('pending','confirmed','cancelled','completed') DEFAULT 'pending',
    reason           VARCHAR(500) COMMENT '申请原因',
    student_notes    TEXT         COMMENT '学生备注',
    teacher_notes    TEXT         COMMENT '老师备注',
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_counselor_date (counselor_id, appointment_date),
    INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约记录';

-- 3. 确认 counselors 表有基本数据（无则插入）
INSERT IGNORE INTO counselors (id, user_id, name, title, avatar, short_desc, specialties, status)
VALUES (1, 1, '张老师', '高级心理咨询师',
        'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200',
        '十年青少年心理辅导经验，擅长情绪管理与学业压力疏导',
        '情绪管理,学业压力,亲子关系,青春期心理', 1);

-- 3.1 给咨询师添加绑定码字段
CALL add_column_if_not_exists('counselors', 'bind_code', 'VARCHAR(4) DEFAULT NULL COMMENT "4位绑定码"');

-- 3.2 绑定申请表
CREATE TABLE IF NOT EXISTS bind_requests (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    student_id   INT NOT NULL COMMENT '学生用户ID',
    counselor_id INT NOT NULL COMMENT '咨询师ID',
    status       ENUM('pending','approved','rejected') DEFAULT 'pending',
    create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_counselor_status (counselor_id, status),
    INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绑定申请记录';

-- 4. 补充 users 表的学生个人资料字段
CALL add_column_if_not_exists('users', 'real_name',  'VARCHAR(50)  DEFAULT ""  COMMENT "真实姓名"');
CALL add_column_if_not_exists('users', 'gender',     'VARCHAR(10)  DEFAULT ""  COMMENT "性别"');
CALL add_column_if_not_exists('users', 'birth_date', 'DATE         DEFAULT NULL COMMENT "出生日期"');

-- 5. teacher_students 表（老师-学生关联 + 备注）
CREATE TABLE IF NOT EXISTS teacher_students (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    teacher_id  INT NOT NULL COMMENT '老师用户ID',
    student_id  INT NOT NULL COMMENT '学生用户ID',
    notes       TEXT COMMENT '老师对学生的备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_teacher_student (teacher_id, student_id),
    INDEX idx_teacher (teacher_id),
    INDEX idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老师-学生关联表';

CALL add_column_if_not_exists('teacher_students', 'notes', 'TEXT COMMENT "老师对学生的备注"');

-- 6. 确认 users 表存在且有关联
UPDATE users SET counselor_id = 1 WHERE id = 1 AND role = 'teacher';

DROP PROCEDURE IF EXISTS add_column_if_not_exists;
