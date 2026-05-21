package com.hospital.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Implementation of EmailService that uses Spring's JavaMailSender to send actual emails.
 *
 * Behavior:
 * - When notification.email.enabled=false (default), emails are NOT sent. Instead, the
 *   email content is logged at INFO level. This allows the service to run without SMTP config.
 * - When notification.email.enabled=true, emails are sent via the configured SMTP server
 *   (typically Gmail). Failures are caught and logged, returning false to indicate failure.
 *
 * The from address is taken from the spring.mail.username property.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    /** Controls whether emails are actually sent or just logged */
    @Value("${notification.email.enabled}")
    private boolean emailEnabled;

    /** The sender email address (same as SMTP username) */
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Sends an email to the specified recipient.
     *
     * If email sending is disabled via configuration, the email details are logged
     * but no actual email is dispatched. This is the default behavior so the application
     * can start without valid SMTP credentials.
     *
     * @param to      recipient email address
     * @param subject email subject line
     * @param body    email body (plain text)
     * @return true if email was sent successfully, false if disabled or if an error occurred
     */
    @Override
    public boolean sendEmail(String to, String subject, String body) {
        // Check if email sending is enabled via configuration
        if (!emailEnabled) {
            log.info("Email sending is disabled. Would have sent email to: {}, subject: {}", to, subject);
            log.debug("Email body that was not sent: {}", body);
            return false;
        }

        try {
            log.info("Sending email to: {}, subject: {}", to, subject);

            // Build the email message using Spring's SimpleMailMessage
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(body);

            // Send the email via JavaMailSender (delegates to SMTP server)
            mailSender.send(mailMessage);

            log.info("Email sent successfully to: {}", to);
            return true;

        } catch (MailException e) {
            // MailException covers SMTP authentication failures, connection timeouts,
            // invalid addresses, and other mail delivery issues
            log.error("Failed to send email to: {}. Error: {}", to, e.getMessage(), e);
            return false;
        }
    }
}
