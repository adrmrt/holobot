package dev.zawarudo.holo.modules.aoc.graph;

import dev.zawarudo.holo.modules.aoc.data.AdventDay;
import dev.zawarudo.holo.utils.FontUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class BarChart extends AdventOfCodeGraph {

    public BarChart(int year, int leaderboardId, String sessionKey) {
        super(year, leaderboardId, sessionKey);
    }

    @Override
    protected BufferedImage generateChart(List<AdventDay> days) {
        BufferedImage chart = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2d = chart.createGraphics();

        FontUtils.enableHighQuality(g2d);

        int graphWidth = IMAGE_WIDTH - OFFSET_X1 - OFFSET_X2;
        int graphHeight = (IMAGE_HEIGHT - OFFSET_Y1 - OFFSET_Y2) / rows * rows;

        drawTitle(g2d);
        drawLegends(g2d);

        drawGrid(g2d, graphWidth, graphHeight);
        drawAxisValues(g2d, graphWidth, graphHeight);
        drawAxisLabels(g2d, graphWidth, graphHeight);

        renderGraphBars(g2d, days, graphWidth, graphHeight);

        g2d.dispose();
        return chart;
    }

    /**
     * Draws the bars for each AdventDay.
     */
    private void renderGraphBars(Graphics2D g2d, List<AdventDay> days, int graphWidth, int graphHeight) {
        int colWidth = graphWidth / 25;
        int thickness = colWidth / 5;

        int currentX = OFFSET_X1;

        for (AdventDay day : days) {
            renderStarCountBars(g2d, day, currentX, graphHeight, thickness);
            currentX += colWidth;
        }
    }

    /**
     * Draws bars for two stars, one star and no star of the given day.
     */
    protected void renderStarCountBars(Graphics2D g2d, AdventDay day, int startX, int graphHeight, int thickness) {
        int xPos = startX + thickness + thickness / 4;
        renderSingleBar(g2d, theme.getTwoStarsColor(), day.goldCount(), xPos, graphHeight, thickness);
        xPos += thickness;
        renderSingleBar(g2d, theme.getOneStarColor(), day.silverCount(), xPos, graphHeight, thickness);
        xPos += thickness;
        renderSingleBar(g2d, theme.getNoStarsColor(), day.grayCount(), xPos, graphHeight, thickness);
    }

    /**
     * Draws a single bar on the graph of the given color and given properties.
     */
    protected void renderSingleBar(Graphics2D g2d, Paint color, int count, int x, int graphHeight, int thickness) {
        g2d.setPaint(color);
        int heightBar = graphHeight * count / maxCount;
        g2d.fillRect(x, OFFSET_Y1 + (graphHeight - heightBar), thickness, heightBar);
    }
}
