package com.example.agribackend.utils;

import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

public class CaptchaUtils {
    private static final int WIDTH = 80;   // 验证码宽度
    private static final int HEIGHT = 40;  // 验证码高度
    private static final int LENGTH = 4;   // 验证码长度
    private static final Random RANDOM = new Random();

    /**
     * 生成验证码图片并输出到响应，同时返回验证码文本
     */
    public static String createCaptchaImage(HttpServletResponse response) throws IOException {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 1. 绘制背景
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 2. 绘制干扰线
        g.setColor(Color.LIGHT_GRAY);
        for (int i = 0; i < 20; i++) {
            int x1 = RANDOM.nextInt(WIDTH);
            int y1 = RANDOM.nextInt(HEIGHT);
            int x2 = RANDOM.nextInt(12);
            int y2 = RANDOM.nextInt(12);
            g.drawLine(x1, y1, x1 + x2, y1 + y2);
        }

        // 3. 生成并绘制验证码文本
        g.setColor(new Color(RANDOM.nextInt(255), RANDOM.nextInt(255), RANDOM.nextInt(255)));
        g.setFont(new Font("Arial", Font.BOLD, 20));
        String captchaText = generateRandText(LENGTH);
        int x = 10;
        for (char c : captchaText.toCharArray()) {
            g.drawString(String.valueOf(c), x, 28 + RANDOM.nextInt(10));
            x += 20;
        }

        g.dispose();

        // 4. 输出图片到响应
        response.setContentType("image/png");
        ImageIO.write(image, "png", response.getOutputStream());
        response.getOutputStream().flush();

        return captchaText;
    }

    /**
     * 生成随机验证码文本
     */
    private static String generateRandText(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}