package com.hospital.notification.service;

/**
 * Interface for the email sending service.
 *
 * Abstracts the email delivery mechanism so it can be easily mocked in tests
 * and swapped with alternative implementations (e.g., SendGrid, AWS SES).
 */
public interface EmailService {

    /**
     * Sends an email to the specified recipient.
     *
     * @param to      the recipient's email address
     * @param subject the email subject line
     * @param body    the email body content (plain text)
     * @return true if the email was sent successfully, false if sending failed or is disabled
     */
    boolean sendEmail(String to, String subject, String body);
}
