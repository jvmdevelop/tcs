package com.jvmd.transationapp.service.notification;

import com.jvmd.transationapp.config.NotificationProperties;
import com.jvmd.transationapp.model.NotificationConfig;
import com.jvmd.transationapp.model.Transactions;
import com.jvmd.transationapp.service.rules.RuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateService {

    private final TemplateEngine templateEngine;
    private final NotificationProperties properties;

    public String build(NotificationConfig config, Transactions tx, RuleEngine.RuleEvaluationResult result) {
        String template = config.getMessageTemplate();
        if (template == null || template.isBlank()) {
            template = getDefaultTemplate();
        }

        Context ctx = new Context();
        ctx.setVariable("tx", tx);
        ctx.setVariable("result", result);
        ctx.setVariable("detailsUrl", properties.getBaseUrl() + "/admin/transactions/" + tx.getId());
        ctx.setVariable("reasons", String.join(", ", result.getAlertReasons()));

        try {
            return templateEngine.process(template, ctx);
        } catch (Exception e) {
            log.error("Template render failed, falling back to default", e);
            return "ALERT: Transaction " + tx.getId() + " - fraud detected";
        }
    }

    private String getDefaultTemplate() {
        return """
                FRAUD ALERT
                Transaction: [[${tx.id}]]
                Amount: [[${tx.amount}]]
                From: [[${tx.from}]] → To: [[${tx.to}]]
                Severity: [[${result.maxSeverity}]]/5
                ML Score: [[${result.mlScore}]]
                Reasons: [[${reasons}]]
                Details: [[${detailsUrl}]]
                """;
    }
}