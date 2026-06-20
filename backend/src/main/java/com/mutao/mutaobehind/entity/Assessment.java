package com.mutao.mutaobehind.entity;

import java.util.Date;

public class Assessment {
    private String scaleType;
    private Long id;
    private String title;
    private String shortDesc;
    private String description;
    private String questionsJson; // 我们用 String 来直接接收数据库里的 JSON 文本
    private Date createTime;

    // 下面是 Getter 和 Setter 方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getQuestionsJson() { return questionsJson; }
    public void setQuestionsJson(String questionsJson) { this.questionsJson = questionsJson; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public String getShortDesc() { return shortDesc; }
    public void setShortDesc(String shortDesc) { this.shortDesc = shortDesc; }

    public String getScaleType() { return scaleType; }
    public void setScaleType(String scaleType) { this.scaleType = scaleType; }
}