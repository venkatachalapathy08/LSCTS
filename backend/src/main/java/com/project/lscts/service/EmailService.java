
package com.project.lscts.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Existing resident account setup email
    public void sendPasswordSetupEmail(String toEmail, String setupLink) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Set up your LSTS account password");

        message.setText(
                "Hello,\n\n" +
                        "Your apartment administrator has created an LSTS account for you.\n\n" +
                        "Use the link below to set your password:\n" +
                        setupLink + "\n\n" +
                        "This link expires in 30 minutes and can be used only once.\n\n" +
                        "If you were not expecting this email, please contact your apartment administrator.\n\n" +
                        "Regards,\nLSTS Support"
        );

        mailSender.send(message);
    }

    // New forgot-password OTP email
    public void sendPasswordResetOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("LSTS Password Reset OTP");

        message.setText(
                "Hello,\n\n" +
                        "We received a request to reset your LSTS account password.\n\n" +
                        "Your OTP is: " + otp + "\n\n" +
                        "This OTP expires in 5 minutes and can be used only once.\n\n" +
                        "Do not share this OTP with anyone.\n\n" +
                        "If you did not request a password reset, please ignore this email.\n\n" +
                        "Regards,\nLSTS Support"
        );

        mailSender.send(message);
    }
}
