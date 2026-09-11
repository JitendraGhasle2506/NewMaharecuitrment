package com.maharecruitment.gov.in.web.security.headers;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.web.servlet.IServletWebExchange;

import jakarta.servlet.http.HttpServletRequest;

/** Adds a CSP nonce while Thymeleaf is processing server-owned script and style nodes. */
final class CspTemplateProcessor extends AbstractElementTagProcessor {

    private static final int PRECEDENCE = 2_000;

    CspTemplateProcessor(String dialectPrefix) {
        super(TemplateMode.HTML, dialectPrefix, null, false, null, false, PRECEDENCE);
    }

    @Override
    protected void doProcess(
            ITemplateContext context,
            IProcessableElementTag tag,
            IElementTagStructureHandler structureHandler) {
        HttpServletRequest request = currentRequest(context);
        if (request == null) {
            return;
        }

        String elementName = tag.getElementCompleteName();
        if ("script".equalsIgnoreCase(elementName) || "style".equalsIgnoreCase(elementName)) {
            structureHandler.setAttribute("nonce", SecurityHeaderPolicy.nonce(request));
        }
    }

    private HttpServletRequest currentRequest(ITemplateContext context) {
        if (!(context instanceof IWebContext webContext)
                || !(webContext.getExchange() instanceof IServletWebExchange servletExchange)
                || !(servletExchange.getNativeRequestObject() instanceof HttpServletRequest request)) {
            return null;
        }
        return request;
    }
}
