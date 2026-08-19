package com.maharecruitment.gov.in.web.security.headers;

import java.util.Set;

import org.springframework.stereotype.Component;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

/**
 * Applies CSP trust only to elements produced from server-owned Thymeleaf
 * templates. Unescaped or client-provided HTML is never processed by this
 * dialect and therefore never receives a nonce or an allowed attribute hash.
 */
@Component
public final class CspTemplateDialect extends AbstractProcessorDialect {

    private static final String PREFIX = "csp";
    private static final int PRECEDENCE = 2_000;

    public CspTemplateDialect() {
        super("Content Security Policy", PREFIX, PRECEDENCE);
    }

    @Override
    public Set<IProcessor> getProcessors(String dialectPrefix) {
        return Set.of(new CspTemplateProcessor(dialectPrefix));
    }
}
