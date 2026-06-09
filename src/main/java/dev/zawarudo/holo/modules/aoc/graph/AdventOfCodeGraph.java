package dev.zawarudo.holo.modules.aoc.graph;

import dev.zawarudo.holo.modules.aoc.data.AdventDay;
import dev.zawarudo.holo.modules.aoc.data.AdventOfCodeClient;
import dev.zawarudo.holo.utils.FontUtils;
import dev.zawarudo.holo.utils.exceptions.APIException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public abstract class AdventOfCodeGraph {

    protected static final int MAX_DAYS = 25;

    /**
     * Width of the whole image.
     */
    protected static final int IMAGE_WIDTH = 1500;
    /**
     * Height of the whole image.
     */
    protected static final int IMAGE_HEIGHT = 1200;
    /**
     * Distance from the left edge.
     */
    protected static final int OFFSET_X1 = 150;
    /**
     * Distance from the right edge.
     */
    protected static final int OFFSET_X2 = 50;
    /**
     * Distance from the top edge.
     */
    protected static final int OFFSET_Y1 = 200;
    /**
     * Distance from the bottom edge.
     */
    protected static final int OFFSET_Y2 = 150;

    protected static final float FONT_SIZE = 25f;

    protected int year;
    protected int leaderboardId;
    protected String sessionKey;

    protected Theme theme;

    /**
     * The number of participants in the leaderboard.
     */
    protected int participants;
    /**
     * The rounded up number of participants.
     */
    protected int maxCount;
    /**
     * The number of rows the grid should have.
     */
    protected int rows;

    protected AdventOfCodeGraph(int year, int leaderboardId, String sessionKey) {
        this.year = year;
        this.leaderboardId = leaderboardId;
        this.sessionKey = sessionKey;

        theme = GraphTheme.AOC.load();
    }

    /**
     * Initializes a graph object of the given graph type and properties.
     */
    public static AdventOfCodeGraph createGraph(ChartType type, int year, int leaderboardId, String sessionKey) {
        return switch (type) {
            case BAR_CHART -> new BarChart(year, leaderboardId, sessionKey);
            case AREA_CHART -> new AreaChart(year, leaderboardId, sessionKey);
            case STACKED_BAR_CHART -> new StackedBarChart(year, leaderboardId, sessionKey);
        };
    }

    public void setTheme(GraphTheme theme) {
        this.theme = theme.load();
    }

    public BufferedImage generateImage() throws APIException {
        BufferedImage result = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2d = result.createGraphics();

        drawBackground(g2d, result);

        List<AdventDay> days = AdventOfCodeClient.getAdventDays(year, leaderboardId, sessionKey);

        participants = getParticipantCount(days);
        maxCount = roundUp(participants);
        rows = calculateNumberRows();
        BufferedImage chart = generateChart(days);

        g2d.drawImage(chart, 0, 0, chart.getWidth(), chart.getHeight(), null);

        g2d.dispose();
        return result;
    }

    protected abstract BufferedImage generateChart(List<AdventDay> days);

    protected void drawBackground(Graphics2D g2d, BufferedImage image) {
        g2d.setPaint(theme.getBackgroundColor());
        g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
    }

    protected void drawTitle(Graphics2D g2d) {
        Font font = FontUtils.loadFontFromFile("ComicSansBold", FONT_SIZE * 2);
        g2d.setFont(font);
        g2d.setPaint(theme.getTextColor());

        String titleString = String.format("Advent of Code %d", year);
        FontMetrics metrics = g2d.getFontMetrics();

        int textWidth = metrics.stringWidth(titleString);
        int startX = (IMAGE_WIDTH - textWidth) / 2;

        int topArea = OFFSET_Y1 / 4 * 3;
        int startY = topArea / 2 - metrics.getHeight() / 2 + metrics.getAscent();

        g2d.drawString(titleString, startX, startY);
    }

    protected void drawGrid(Graphics2D g2d, int graphWidth, int graphHeight) {
        g2d.setPaint(theme.getGridColor());

        int x = OFFSET_X1;
        int y = OFFSET_Y1;

        int colWidth = graphWidth / MAX_DAYS;
        int rowHeight = graphHeight / rows;

        int counter = 0;
        while (counter <= rows) {
            int currentRowY = y + rowHeight * counter;
            g2d.drawLine(x, currentRowY, x + graphWidth, currentRowY);
            counter++;
        }

        counter = 0;
        while (counter <= MAX_DAYS) {
            int currentColX = x + colWidth * counter;
            g2d.drawLine(currentColX, y, currentColX, y + graphHeight);
            counter++;
        }
    }

    protected void drawLegends(Graphics2D g2d) {
        Font font = FontUtils.loadFontFromFile("ComicSansBold", FONT_SIZE);
        g2d.setFont(font);
        g2d.setPaint(theme.getTextColor());

        FontMetrics metrics = g2d.getFontMetrics(font);

        int legendY = OFFSET_Y1 - OFFSET_Y1 / 5 - metrics.getHeight() / 2 + metrics.getAscent();

        String leaderboardString = String.format("Leaderboard ID: %d", leaderboardId);
        String participantsString = String.format("Participants: %d", participants);

        g2d.drawString(leaderboardString, IMAGE_WIDTH / 3 - metrics.stringWidth(leaderboardString) / 2, legendY);
        g2d.drawString(participantsString, IMAGE_WIDTH / 3 * 2 - metrics.stringWidth(participantsString) / 2, legendY);
    }

    protected void drawAxisValues(Graphics2D g2d, int graphWidth, int graphHeight) {
        Font font = FontUtils.loadFontFromFile("ComicSansBold", FONT_SIZE);
        g2d.setFont(font);
        g2d.setPaint(theme.getTextColor());

        FontMetrics metrics = g2d.getFontMetrics(font);

        int colWidth = graphWidth / MAX_DAYS;
        int rowHeight = graphHeight / rows;

        int posX = OFFSET_X1;
        int daysY = IMAGE_HEIGHT - (OFFSET_Y2 + graphHeight % rows) / 3 * 2 - metrics.getHeight() / 2;

        // Draw day numbers
        int count = 1;
        for (int i = posX + colWidth / 2; i < posX + graphWidth; i += colWidth) {
            g2d.drawString(String.valueOf(count), i - metrics.stringWidth(String.valueOf(count)) / 2, daysY);
            count++;
        }

        posX = OFFSET_X1 / 4 * 3;
        int posY = OFFSET_Y1 + graphHeight;
        count = 0;

        // Draw numbers of people
        for (int i = 0; i <= rows; i++) {
            String text = String.valueOf(count);
            g2d.drawString(text, posX - metrics.stringWidth(text) / 2, posY - i * rowHeight - metrics.getHeight() / 2 + metrics.getAscent());
            count += maxCount > 100 ? 10 : 5;
        }
    }

    protected void drawAxisLabels(Graphics2D g2d, int graphWidth, int graphHeight) {
        Font font = FontUtils.loadFontFromFile("ComicSansBold", FONT_SIZE);
        g2d.setFont(font);
        g2d.setPaint(theme.getTextColor());

        FontMetrics metrics = g2d.getFontMetrics();

        String text = "Day";
        int startX = graphWidth / 2 + OFFSET_X1 - metrics.stringWidth(text) / 2;
        int startY = IMAGE_HEIGHT - OFFSET_Y2 / 2;

        g2d.drawString(text, startX, startY);

        text = "People";
        startX = OFFSET_X1 / 3 - metrics.getHeight() / 2 + metrics.getAscent();
        startY = graphHeight / 2 + OFFSET_Y1 + metrics.stringWidth(text) / 2;

        g2d.setFont(FontUtils.rotate(font, -90));
        g2d.drawString(text, startX, startY);

        g2d.setFont(font);
        drawLegendSquares(g2d, graphWidth, graphHeight);
    }

    protected void drawLegendSquares(Graphics2D g2d, int graphWidth, int graphHeight) {
        FontMetrics metrics = g2d.getFontMetrics();

        int lineHeight = IMAGE_HEIGHT - (OFFSET_Y2 + graphHeight % rows) / 4;
        int textHeight = lineHeight - metrics.getHeight() / 2 + metrics.getAscent();

        int squareSize = (int) FONT_SIZE;
        int squareHeight = lineHeight - squareSize / 2;

        int startX = graphWidth / 6 + OFFSET_X1;
        int separation = graphWidth / 3;

        String text = "Two Stars";
        int elementWidth = (2 * squareSize + metrics.stringWidth(text)); // Width of square + text

        int positionX = startX - elementWidth / 2; // Position of square + text

        g2d.setColor(theme.getTwoStarsColor());
        g2d.fillRect(positionX, squareHeight, squareSize, squareSize);
        g2d.drawString(text, positionX + 2 * squareSize, textHeight);

        text = "One Star";
        elementWidth = (2 * squareSize + metrics.stringWidth(text));

        positionX = startX + separation - elementWidth / 2;

        g2d.setColor(theme.getOneStarColor());
        g2d.fillRect(positionX, squareHeight, squareSize, squareSize);
        g2d.drawString(text, positionX + 2 * squareSize, textHeight);

        text = "No Star";
        elementWidth = (2 * squareSize + metrics.stringWidth(text));

        positionX = startX + 2 * separation - elementWidth / 2;

        g2d.setColor(theme.getNoStarsColor());
        g2d.fillRect(positionX, squareHeight, squareSize, squareSize);
        g2d.drawString(text, positionX + 2 * squareSize, textHeight);
    }

    private int getParticipantCount(List<AdventDay> days) {
        return days.stream()
            .mapToInt(day -> day.goldCount() + day.silverCount() + day.grayCount())
            .max()
            .orElse(0);
    }

    private int roundUp(int number) {
        return number > 100 ? (number + 9) / 10 * 10 : (number + 4) / 5 * 5;
    }

    private int calculateNumberRows() {
        return maxCount > 100 ? maxCount / 10 : maxCount / 5;
    }
}
