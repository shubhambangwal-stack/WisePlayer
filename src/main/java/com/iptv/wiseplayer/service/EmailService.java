package com.iptv.wiseplayer.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // @Async
    public void sendAdminInvitation(String toEmail, String inviteLink) {
        try {
            log.info("==== EMAIL TRIGGER START ====");
            log.info("Sending admin invitation email to: {} from: {}", toEmail, fromEmail);
            log.info("Received invite link parameter: [{}]", inviteLink);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            String htmlContent = createInvitationHtml(inviteLink);
            log.info("Generated HTML Content containing the href. Checking value in HTML: {}",
                    htmlContent.contains("href=\"" + inviteLink + "\""));

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Invitation to Join WisePlayer Admin Panel");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Invitation email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("CRITICAL: Unexpected error while sending invitation email to {}. Error type: {}, Message: {}",
                    toEmail, e.getClass().getName(), e.getMessage(), e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            log.info("Sending password reset email to: {}", toEmail);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            String htmlContent = createResetPasswordHtml(resetLink);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Request - WisePlayer Admin");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Error sending password reset email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String createInvitationHtml(String inviteLink) {
        log.info("Passing invite link to HTML generator: [{}]", inviteLink);
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f0f0f; color: #ffffff; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background-color: #1a1a1a; padding: 40px; border-radius: 12px; border: 1px solid #333; }
                        .logo { color: #e50914; font-size: 28px; font-weight: bold; margin-bottom: 20px; text-align: center; }
                        .title { font-size: 24px; font-weight: 600; margin-bottom: 20px; text-align: center; color: #00d4ff; }
                        .content { line-height: 1.6; color: #cccccc; margin-bottom: 30px; }
                        .button-container { text-align: center; }
                        .button { background-color: #e50914; color: white; padding: 14px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block; transition: background-color 0.3s; }
                        .footer { margin-top: 40px; font-size: 12px; color: #666; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="logo">WisePlayer</div>
                        <div class="title">Join Our Admin Team</div>
                        <div class="content">
                            <p>Hello,</p>
                            <p>You have been invited to join the WisePlayer Administrative Panel. As an admin, you will have access to manage devices, subscriptions, and payments.</p>
                            <p>To get started and set up your account, please click the button below:</p>
                        </div>
                        <div class="button-container">
                            <a href="%s" class="button">Accept Invitation & Setup Account</a>
                        </div>
                        <div class="content">
                            <p>This link will expire in 7 days. If you did not expect this invitation, please ignore this email.</p>
                        </div>
                        <div class="footer">
                            &copy; 2024 WisePlayer. All rights reserved.
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(inviteLink);
    }

    private String createResetPasswordHtml(String resetLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f0f0f; color: #ffffff; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background-color: #1a1a1a; padding: 40px; border-radius: 12px; border: 1px solid #333; }
                        .logo { color: #e50914; font-size: 28px; font-weight: bold; margin-bottom: 20px; text-align: center; }
                        .title { font-size: 24px; font-weight: 600; margin-bottom: 20px; text-align: center; color: #00d4ff; }
                        .content { line-height: 1.6; color: #cccccc; margin-bottom: 30px; }
                        .button-container { text-align: center; }
                        .button { background-color: #00d4ff; color: #000000; padding: 14px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block; transition: background-color 0.3s; }
                        .footer { margin-top: 40px; font-size: 12px; color: #666; text-align: center; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="logo">WisePlayer</div>
                        <div class="title">Reset Your Password</div>
                        <div class="content">
                            <p>Hello,</p>
                            <p>We received a request to reset the password for your WisePlayer Admin account.</p>
                            <p>To proceed with the password reset, please click the button below:</p>
                        </div>
                        <div class="button-container">
                            <a href="%s" class="button">Reset Password</a>
                        </div>
                        <div class="content">
                            <p>This link will expire in 1 hour. If you did not request a password reset, please ignore this email.</p>
                        </div>
                        <div class="footer">
                            &copy; 2024 WisePlayer. All rights reserved.
                        </div>
                    </div>
                </body>
                </html>
                """
                .formatted(resetLink);
    }



//    Reseller/Subreseller Forgot Password
public void sendOtpEmail(String toEmail, String otp) {
    try {
        log.info("Sending OTP email to: {}", toEmail);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name());

        String htmlContent = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <style>
                            body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f0f0f; color: #ffffff; padding: 20px; }
                            .container { max-width: 600px; margin: 0 auto; background-color: #1a1a1a; padding: 40px; border-radius: 12px; border: 1px solid #333; }
                            .logo { color: #e50914; font-size: 28px; font-weight: bold; margin-bottom: 20px; text-align: center; }
                            .title { font-size: 24px; font-weight: 600; margin-bottom: 20px; text-align: center; color: #00d4ff; }
                            .content { line-height: 1.6; color: #cccccc; margin-bottom: 30px; }
                            .otp-box { background-color: #2a2a2a; border: 2px solid #00d4ff; border-radius: 8px; text-align: center; padding: 20px; font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #00d4ff; margin: 20px 0; }
                            .footer { margin-top: 40px; font-size: 12px; color: #666; text-align: center; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="logo">WisePlayer</div>
                            <div class="title">Verify Your Email</div>
                            <div class="content">
                                <p>Hello,</p>
                                <p>Use the OTP below to verify your WisePlayer Reseller account. It expires in 10 minutes.</p>
                            </div>
                            <div class="otp-box">%s</div>
                            <div class="content">
                                <p>If you did not register, please ignore this email.</p>
                            </div>
                            <div class="footer">&copy; 2024 WisePlayer. All rights reserved.</div>
                        </div>
                    </body>
                    </html>
                    """.formatted(otp);

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("Your WisePlayer Verification OTP");
        helper.setText(htmlContent, true);

        mailSender.send(message);
        log.info("OTP email sent successfully to: {}", toEmail);
    } catch (Exception e) {
        log.error("Error sending OTP email to {}: {}", toEmail, e.getMessage(), e);
    }
}

    public void sendResellerPasswordResetEmail(String toEmail, String resetLink) {
        try {
            log.info("Sending reseller password reset email to: {}", toEmail);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            String htmlContent = createResetPasswordHtml(resetLink);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Request - WisePlayer Reseller");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Reseller password reset email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Error sending reseller password reset email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendWelcomeEmail(String toEmail, String username, String fullName) {
        try {
            log.info("Sending welcome email to: {}", toEmail);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            String htmlContent = createWelcomeHtml(fullName, username, toEmail);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to WisePlayer - Account Verified Successfully");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Error sending welcome email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String createWelcomeHtml(String fullName, String username, String email) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #0f0f0f; color: #ffffff; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background-color: #1a1a1a; padding: 40px; border-radius: 12px; border: 1px solid #333; }
                    .logo { color: #e50914; font-size: 28px; font-weight: bold; margin-bottom: 20px; text-align: center; }
                    .title { font-size: 24px; font-weight: 600; margin-bottom: 20px; text-align: center; color: #00d4ff; }
                    .content { line-height: 1.6; color: #cccccc; margin-bottom: 30px; }
                    .info-box { background-color: #2a2a2a; border: 2px solid #00d4ff; border-radius: 8px; padding: 20px; margin: 20px 0; }
                    .info-label { color: #888; font-size: 12px; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 4px; }
                    .info-value { color: #ffffff; font-size: 15px; font-weight: bold; margin-bottom: 16px; }
                    .info-value:last-child { margin-bottom: 0; }
                    .status-badge { display: inline-block; background-color: #0d2e0d; border: 1px solid #00ff88; color: #00ff88; font-size: 12px; font-weight: bold; padding: 4px 14px; border-radius: 20px; margin-bottom: 16px; }
                    .divider { height: 1px; background-color: #333; margin: 24px 0; }
                    .security-box { background-color: #1e1e1e; border-left: 3px solid #e50914; padding: 16px 20px; border-radius: 4px; margin: 20px 0; }
                    .security-title { color: #e50914; font-size: 12px; font-weight: bold; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 8px; }
                    .security-text { color: #aaaaaa; font-size: 13px; line-height: 1.6; }
                    .button-container { text-align: center; margin-top: 30px; }
                    .button { background-color: #e50914; color: white; padding: 14px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block; }
                    .footer { margin-top: 40px; font-size: 12px; color: #666; text-align: center; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="logo">WisePlayer</div>
                    <div class="title">Account Verified Successfully</div>
                    <div class="content">
                        <p>Hello <strong style="color: #ffffff;">%s</strong>,</p>
                        <p style="margin-top: 12px;">Your WisePlayer Reseller account has been successfully verified and activated. You now have full access to the Reseller Dashboard.</p>
                    </div>
                    <div class="info-box">
                        <div class="info-label">Username</div>
                        <div class="info-value">%s</div>
                        <div class="info-label">Registered Email</div>
                        <div class="info-value">%s</div>
                        <div class="info-label">Account Status</div>
                        <span class="status-badge">&#10003; Active &amp; Verified</span>
                    </div>
                    <div class="divider"></div>
                    <div class="security-box">
                        <div class="security-title">Security Reminder</div>
                        <div class="security-text">
                            Never share your username or password with anyone.<br>
                            Contact support immediately if you notice any suspicious activity.
                        </div>
                    </div>
                    <div class="button-container">
                        <a href="https://wise-player.com/login" class="button">Go to Dashboard</a>
                    </div>
                    <div class="footer">
                        &copy; 2024 WisePlayer. All rights reserved.
                    </div>
                </div>
            </body>
            </html>
            """.formatted(fullName, username, email);
    }

}
