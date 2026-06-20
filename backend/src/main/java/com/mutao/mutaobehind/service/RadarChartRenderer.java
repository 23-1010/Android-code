package com.mutao.mutaobehind.service;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * SCL-90 九因子雷达图渲染器
 * 使用 Java 2D 绘制 600×600 的雷达图，再嵌入 PDF
 */
public class RadarChartRenderer {

    private static final int IMG_SIZE = 600;
    private static final int CENTER = IMG_SIZE / 2;    // 300
    private static final int RADIUS = 220;
    private static final int[] LEVELS = {1, 2, 3, 4};  // 4 层背景网格（对应 1-4 级）

    private Font labelFont;

    /**
     * @param fontPath 系统中文字体路径（用于图表中的因子标签）
     */
    public RadarChartRenderer(String fontPath) {
        // 加载 TrueType 字体供 Java 2D 使用
        Font f;
        try {
            f = Font.createFont(Font.TRUETYPE_FONT, new File(fontPath))
                    .deriveFont(Font.PLAIN, 13f);
        } catch (Exception e) {
            // 兜底：尝试系统默认中文字体名
            f = new Font("SimHei", Font.PLAIN, 13);
        }
        this.labelFont = f;
    }

    /**
     * 根据因子列表生成雷达图 BufferedImage
     * @param dimensions SCL90 9 个因子 [{name, total, avg, rating}, ...]
     */
    public BufferedImage render(List<Map<String, Object>> dimensions) {
        BufferedImage img = new BufferedImage(IMG_SIZE, IMG_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // 抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 白色背景
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, IMG_SIZE, IMG_SIZE);

        int numAxes = dimensions.size();
        if (numAxes == 0) {
            g.dispose();
            return img;
        }

        // 1. 绘制背景网格（4 层同心正多边形）
        for (int level : LEVELS) {
            int r = RADIUS * level / 4;
            drawRegularPolygon(g, CENTER, CENTER, r, numAxes, new Color(200, 200, 200), false);
        }

        // 2. 绘制轴线
        g.setColor(new Color(180, 180, 180));
        g.setStroke(new BasicStroke(0.8f));
        for (int i = 0; i < numAxes; i++) {
            Point2D p = getPoint(CENTER, CENTER, RADIUS, i, numAxes);
            g.draw(new Line2D.Double(CENTER, CENTER, p.getX(), p.getY()));
        }

        // 3. 绘制数据多边形（半透明蓝色填充）
        int[] dataXs = new int[numAxes];
        int[] dataYs = new int[numAxes];
        for (int i = 0; i < numAxes; i++) {
            double avg = getAvg(dimensions.get(i));
            int r = (int) (RADIUS * avg / 4.0);  // avg 范围 0~4+，映射到 0~RADIUS
            r = Math.max(15, Math.min(r, RADIUS)); // 限制范围
            Point2D p = getPoint(CENTER, CENTER, r, i, numAxes);
            dataXs[i] = (int) p.getX();
            dataYs[i] = (int) p.getY();
        }
        g.setColor(new Color(66, 133, 244, 100)); // 半透明蓝色填充
        g.fillPolygon(dataXs, dataYs, numAxes);
        g.setColor(new Color(66, 133, 244));
        g.setStroke(new BasicStroke(2f));
        g.drawPolygon(dataXs, dataYs, numAxes);

        // 4. 绘制数据点
        for (int i = 0; i < numAxes; i++) {
            g.fillOval(dataXs[i] - 3, dataYs[i] - 3, 6, 6);
        }

        // 5. 绘制因子标签
        g.setFont(labelFont);
        g.setColor(Color.BLACK);
        for (int i = 0; i < numAxes; i++) {
            String name = (String) dimensions.get(i).getOrDefault("name", "?");
            double avg = getAvg(dimensions.get(i));
            // 标签放在轴线末端外侧
            Point2D p = getPoint(CENTER, CENTER, RADIUS + 28, i, numAxes);
            // 根据位置调整文字锚点
            float x = (float) p.getX();
            float y = (float) p.getY();

            // 计算文字宽度用于居中
            FontMetrics fm = g.getFontMetrics();
            int textW = fm.stringWidth(name);
            x -= textW / 2f;
            y += fm.getAscent() / 3f; // 垂直微调

            g.drawString(name + "(" + String.format("%.1f", avg) + ")", x, y);
        }

        g.dispose();
        return img;
    }

    /** 获取某个维度的均分 */
    private double getAvg(Map<String, Object> dim) {
        Object avg = dim.get("avg");
        if (avg instanceof Number) return ((Number) avg).doubleValue();
        return 2.0;
    }

    /** 计算正多边形顶点坐标 */
    private Point2D getPoint(int cx, int cy, int r, int index, int total) {
        // 从顶部开始（-90°），顺时针排列
        double angle = -Math.PI / 2 + 2 * Math.PI * index / total;
        double x = cx + r * Math.cos(angle);
        double y = cy + r * Math.sin(angle);
        return new Point2D.Double(x, y);
    }

    /** 绘制正多边形 */
    private void drawRegularPolygon(Graphics2D g, int cx, int cy, int r, int sides, Color color, boolean fill) {
        g.setColor(color);
        int[] xs = new int[sides];
        int[] ys = new int[sides];
        for (int i = 0; i < sides; i++) {
            Point2D p = getPoint(cx, cy, r, i, sides);
            xs[i] = (int) p.getX();
            ys[i] = (int) p.getY();
        }
        if (fill) {
            g.fillPolygon(xs, ys, sides);
        } else {
            g.setStroke(new BasicStroke(0.6f));
            g.drawPolygon(xs, ys, sides);
        }
    }
}
