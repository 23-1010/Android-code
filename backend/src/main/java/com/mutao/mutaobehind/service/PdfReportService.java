package com.mutao.mutaobehind.service;

import com.mutao.mutaobehind.dto.PrintReportRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * PDF 报告生成服务
 * 将测评数据 + 填写人信息渲染为 A4 PDF，支持 SCL90 和 SDS 两种量表
 */
@Service
public class PdfReportService {

    @Value("${pdf.font-path:C:/Windows/Fonts/simhei.ttf}")
    private String fontPath;

    // A4 纸
    private static final float PAGE_W = PDRectangle.A4.getWidth();   // 595
    private static final float PAGE_H = PDRectangle.A4.getHeight();  // 842
    private static final float MARGIN = 50;
    private static final float LEFT = MARGIN;
    private static final float RIGHT = PAGE_W - MARGIN;
    private static final float USABLE_W = RIGHT - LEFT;              // 495

    // 颜色
    private static final float[] COLOR_PRIMARY = {0.16f, 0.32f, 0.75f};  // 深蓝
    private static final float[] COLOR_GRAY = {0.4f, 0.4f, 0.4f};
    private static final float[] COLOR_LIGHT_GRAY = {0.7f, 0.7f, 0.7f};
    private static final float[] COLOR_BLACK = {0f, 0f, 0f};

    public byte[] generateReport(PrintReportRequest request) throws Exception {
        Map<String, Object> report = request.getReport();
        PrintReportRequest.Respondent respondent = request.getRespondent();
        String reportType = report != null ? (String) report.getOrDefault("reportType", "SCL90") : "SCL90";

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            // 加载中文字体
            PDType0Font font = loadFont(doc);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                float y = PAGE_H - MARGIN; // 从顶部开始

                // ── 1. 标题 ──
                String title;
                if ("SDS".equals(reportType)) {
                    title = "SDS 抑郁自评量表测评报告";
                } else {
                    title = "SCL-90 症状自评量表测评报告";
                }
                y = drawCenteredText(cs, font, 18, title, y);
                y -= 8;
                drawHLine(cs, y, COLOR_PRIMARY, 1.5f);
                y -= 28;

                // ── 2. 填写人信息（量表名称已在标题显示，此处不重复） ──
                y = drawSectionTitle(cs, font, "填写人信息", y);
                y -= 20;
                y = drawText(cs, font, 11, "昵　称：" + nvl(respondent.getNickname()), LEFT, y, COLOR_BLACK);
                y = drawText(cs, font, 11, "日　期：" + nvl(respondent.getTestDate()), LEFT + 240, y, COLOR_BLACK);
                y -= 28;

                // ── 3. 测评结果概述 ──
                y = drawSectionTitle(cs, font, "测评结果概述", y);
                y -= 20;

                if ("SDS".equals(reportType)) {
                    y = drawSDSOverview(cs, font, report, y);
                } else {
                    y = drawSCL90Overview(cs, font, report, y);
                }

                // ── 4. 详情（SCL90 因子表 + 雷达图 / SDS 诊断） ──
                if (!"SDS".equals(reportType)) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> dimensions = (List<Map<String, Object>>) report.get("dimensions");
                    if (dimensions != null && !dimensions.isEmpty()) {
                        y = drawSectionTitle(cs, font, "各因子得分详情", y);
                        y -= 18;
                        y = drawDimensionTable(cs, font, dimensions, y);
                        y -= 18;

                        // 雷达图
                        y = drawRadarChart(cs, doc, font, dimensions, y);
                    }
                } else {
                    y = drawSDSDiagnosis(cs, font, report, y);
                }

                // ── 5. 签名区（跟随内容流，确保与上方图表有间距） ──
                y = Math.min(y - 20, 120); // 取内容结束位置 -20 和 120 中较小的，保证不会冲出页面底部
                drawHLine(cs, y, COLOR_LIGHT_GRAY, 0.5f);
                y -= 22;
                drawText(cs, font, 11, "测评人签名：______________　　　日期：______________", LEFT, y, COLOR_GRAY);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    // ═══════════════════════════════════════════════
    //  SCL90 概述
    // ═══════════════════════════════════════════════
    private float drawSCL90Overview(PDPageContentStream cs, PDType0Font font,
                                     Map<String, Object> r, float y) throws IOException {
        Object totalScore = r.get("totalScore");
        Object overallAvg = r.get("overallAvg");
        Object positiveCount = r.get("positiveCount");
        Object isPositive = r.get("isPositive");
        Object overallRating = r.get("overallRating");

        String line1 = String.format("总　分：%s　分　　均　分：%s　　阳性项目数：%s",
                totalScore, overallAvg, positiveCount);
        String line2 = String.format("阳性判定：%s　　综合评级：%s", isPositive, overallRating);
        y = drawText(cs, font, 11, line1, LEFT, y, COLOR_BLACK);
        y -= 18;
        y = drawText(cs, font, 11, line2, LEFT, y, COLOR_BLACK);
        y -= 28;
        return y;
    }

    // ═══════════════════════════════════════════════
    //  SDS 概述
    // ═══════════════════════════════════════════════
    private float drawSDSOverview(PDPageContentStream cs, PDType0Font font,
                                   Map<String, Object> r, float y) throws IOException {
        Object rawScore = r.get("rawScore");
        Object standardScore = r.get("standardScore");
        Object rating = r.get("rating");

        y = drawText(cs, font, 11, "原始分（粗分）：" + rawScore, LEFT, y, COLOR_BLACK);
        y -= 18;
        y = drawText(cs, font, 11, "标准分（原始分 × 1.25）：" + standardScore, LEFT, y, COLOR_BLACK);
        y -= 18;
        y = drawText(cs, font, 11, "抑郁程度判定：" + rating, LEFT, y, COLOR_BLACK);
        y -= 28;
        return y;
    }

    // ═══════════════════════════════════════════════
    //  SCL90 因子得分表（手动画线）
    // ═══════════════════════════════════════════════
    private float drawDimensionTable(PDPageContentStream cs, PDType0Font font,
                                      List<Map<String, Object>> dims, float y) throws IOException {
        float[] colX = {LEFT, LEFT + 110, LEFT + 185, LEFT + 260, LEFT + 340}; // 5 列起点
        float[] colW = {110, 75, 75, 80, 155};                                  // 5 列宽度
        String[] headers = {"因子名称", "总分", "均分", "评级", "参考范围"};
        float rowH = 20;
        float tableTop = y;
        int rows = dims.size();

        // 表头背景
        cs.setNonStrokingColor(0.95f, 0.95f, 0.95f);
        cs.addRect(LEFT, y - rowH, USABLE_W, rowH);
        cs.fill();

        // 表头文字
        y = drawTableRow(cs, font, 10, colX, headers, y, rowH, COLOR_PRIMARY, true);

        // 数据行
        int idx = 0;
        for (Map<String, Object> dim : dims) {
            String name = String.valueOf(dim.getOrDefault("name", ""));
            String total = String.valueOf(dim.getOrDefault("total", ""));
            String avg = String.valueOf(dim.getOrDefault("avg", ""));
            String rating = String.valueOf(dim.getOrDefault("rating", ""));
            String ref = getRefRange(idx);
            String[] cells = {name, total, avg, rating, ref};

            // 交替行背景
            if (idx % 2 == 1) {
                cs.setNonStrokingColor(0.97f, 0.97f, 0.97f);
                cs.addRect(LEFT, y - rowH, USABLE_W, rowH);
                cs.fill();
            }
            y = drawTableRow(cs, font, 10, colX, cells, y, rowH, COLOR_BLACK, false);
            idx++;
        }

        // 表格外框
        cs.setStrokingColor(0.7f, 0.7f, 0.7f);
        cs.setLineWidth(0.5f);
        cs.addRect(LEFT, y, USABLE_W, tableTop - y);
        cs.stroke();

        // 竖线
        for (int i = 1; i < colX.length; i++) {
            cs.moveTo(colX[i], tableTop);
            cs.lineTo(colX[i], y);
            cs.stroke();
        }

        return y - 6;
    }

    /** 绘制表格中的一行 */
    private float drawTableRow(PDPageContentStream cs, PDType0Font font, float fontSize,
                                float[] colX, String[] cells, float y, float rowH,
                                float[] color, boolean bold) throws IOException {
        float textY = y - rowH + 5;
        cs.setStrokingColor(color[0], color[1], color[2]);
        cs.setNonStrokingColor(color[0], color[1], color[2]);
        for (int i = 0; i < cells.length; i++) {
            float x = colX[i] + 4; // 左边距微调
            drawText(cs, font, fontSize, cells[i], x, textY, color);
        }
        // 行分隔线
        cs.setStrokingColor(0.85f, 0.85f, 0.85f);
        cs.setLineWidth(0.3f);
        cs.moveTo(LEFT, y - rowH);
        cs.lineTo(RIGHT, y - rowH);
        cs.stroke();
        return y - rowH;
    }

    /** 各因子的参考范围 */
    private String getRefRange(int idx) {
        String[] refs = {
            "≤1.7 正常", "≤1.7 正常", "≤1.7 正常", "≤1.8 正常",
            "≤1.6 正常", "≤1.6 正常", "≤1.4 正常", "≤1.6 正常", "≤1.5 正常"
        };
        return idx < refs.length ? refs[idx] : "≤2.0 正常";
    }

    // ═══════════════════════════════════════════════
    //  雷达图（SCL90）
    // ═══════════════════════════════════════════════
    private float drawRadarChart(PDPageContentStream cs, PDDocument doc, PDType0Font font,
                                  List<Map<String, Object>> dimensions, float y) throws Exception {
        float chartSize = 280;
        float chartX = (PAGE_W - chartSize) / 2; // 居中
        float chartY = y - chartSize;

        RadarChartRenderer renderer = new RadarChartRenderer(fontPath);
        BufferedImage img = renderer.render(dimensions);
        PDImageXObject pdImg = LosslessFactory.createFromImage(doc, img);
        cs.drawImage(pdImg, chartX, chartY, chartSize, chartSize);

        y = chartY - 16;
        drawCenteredText(cs, font, 9, "▲ SCL-90 各因子均分雷达图（1-4 级评分）", y);
        return y - 30; // 图表下方留足间距，避免与签名区重合
    }

    // ═══════════════════════════════════════════════
    //  SDS 诊断文字
    // ═══════════════════════════════════════════════
    private float drawSDSDiagnosis(PDPageContentStream cs, PDType0Font font,
                                    Map<String, Object> r, float y) throws IOException {
        y = drawSectionTitle(cs, font, "抑郁程度诊断", y);
        y -= 20;
        int ss = 0;
        Object ssObj = r.get("standardScore");
        if (ssObj instanceof Number) ss = ((Number) ssObj).intValue();

        String[] lines = getSDSInterpretation(ss);
        for (String line : lines) {
            y = drawText(cs, font, 11, line, LEFT, y, COLOR_BLACK);
            y -= 16;
        }
        y -= 10;
        return y;
    }

    private String[] getSDSInterpretation(int standardScore) {
        if (standardScore >= 70) {
            return new String[]{
                "标准分 ≥ 70 分，属于【重度抑郁】范围。",
                "建议：请尽快联系心理咨询师或精神科医生进行专业评估和干预。",
                "日常注意保证充足睡眠，避免独处，适当运动。"
            };
        } else if (standardScore >= 60) {
            return new String[]{
                "标准分 60-69 分，属于【中度抑郁】范围。",
                "建议：建议寻求心理咨询服务，进行系统的心理评估。",
                "尝试与信任的人倾诉，保持规律作息，每天进行适量运动。"
            };
        } else if (standardScore >= 50) {
            return new String[]{
                "标准分 50-59 分，属于【轻度抑郁】范围。",
                "建议：关注自身情绪变化，可尝试正念冥想等自我调节方法。",
                "若持续两周以上情绪低落，建议预约学校心理咨询中心。"
            };
        } else {
            return new String[]{
                "标准分 < 50 分，属于【正常】范围。",
                "目前未检测到明显抑郁倾向，请继续保持积极健康的生活方式。"
            };
        }
    }

    // ═══════════════════════════════════════════════
    //  绘图工具方法
    // ═══════════════════════════════════════════════

    /** 绘制居中文本，返回新的 y */
    private float drawCenteredText(PDPageContentStream cs, PDType0Font font,
                                    float fontSize, String text, float y) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float x = (PAGE_W - textWidth) / 2;
        return drawText(cs, font, fontSize, text, x, y, COLOR_PRIMARY);
    }

    /** 区段标题：背景色块 + padding-left，文字与色块垂直居中对齐 */
    private float drawSectionTitle(PDPageContentStream cs, PDType0Font font,
                                    String text, float y) throws IOException {
        // 文字 14pt 中文字形在基准线上方约 12pt，下方约 3pt
        // 色块覆盖文字区域并留少量上下 padding
        float barTop = y - 5;   // 基准线往上 5pt
        float barH = 18;        // 高度 18pt，覆盖 y-5 到 y+13，包住文字
        // 浅蓝背景条（全宽）
        cs.setNonStrokingColor(0.92f, 0.95f, 1.0f);
        cs.addRect(LEFT, barTop, USABLE_W, barH);
        cs.fill();
        // 左侧深蓝小色块作为视觉锚点
        cs.setNonStrokingColor(COLOR_PRIMARY[0], COLOR_PRIMARY[1], COLOR_PRIMARY[2]);
        cs.addRect(LEFT, barTop, 4, barH);
        cs.fill();
        // 文字基准线不变：y-2（与色块视觉中心对齐）
        return drawText(cs, font, 14, text, LEFT + 12, y - 2, COLOR_PRIMARY);
    }

    /** 绘制左对齐文本 */
    private float drawText(PDPageContentStream cs, PDType0Font font, float fontSize,
                            String text, float x, float y, float[] color) throws IOException {
        cs.beginText();
        cs.setNonStrokingColor(color[0], color[1], color[2]);
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
        return y;
    }

    /** 绘制水平线 */
    private void drawHLine(PDPageContentStream cs, float y, float[] color, float width) throws IOException {
        cs.setStrokingColor(color[0], color[1], color[2]);
        cs.setLineWidth(width);
        cs.moveTo(LEFT, y);
        cs.lineTo(RIGHT, y);
        cs.stroke();
    }

    /** 加载中文字体 */
    private PDType0Font loadFont(PDDocument doc) throws IOException {
        File fontFile = new File(fontPath);
        if (fontFile.exists()) {
            return PDType0Font.load(doc, fontFile);
        }
        // 兜底：尝试常见路径
        String[] fallbackPaths = {
            "C:/Windows/Fonts/simhei.ttf",
            "C:/Windows/Fonts/simsun.ttc",
            "C:/Windows/Fonts/msyh.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"
        };
        for (String path : fallbackPaths) {
            File f = new File(path);
            if (f.exists()) {
                return PDType0Font.load(doc, f);
            }
        }
        throw new IOException(
            "未找到中文字体！请在 application.yml 中配置 pdf.font-path。\n" +
            "已尝试路径：" + String.join(", ", fallbackPaths)
        );
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
