package com.paytech.demo.web;

import com.paytech.demo.paytech.PaytechService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
public class PaymentController {
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private static final String PAYMENT_TOKEN_KEY = "paymentToken";

    private final PaytechService paytechService;

    public PaymentController(PaytechService paytechService) {
        this.paytechService = paytechService;
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        String paymentToken = UUID.randomUUID().toString();
        session.setAttribute(PAYMENT_TOKEN_KEY, paymentToken);

        if (!model.containsAttribute("paymentRequest")) {
            PaymentRequestDto paymentRequest = new PaymentRequestDto(null, paymentToken);
            model.addAttribute("paymentRequest", paymentRequest);
        }
        return "index";
    }

    @GetMapping("/error")
    public String error(HttpSession session, Model model) {
        String rateLimitError = (String) session.getAttribute("rateLimitError");
        if (rateLimitError != null) {
            model.addAttribute("error", rateLimitError);
            Object retryAfter = session.getAttribute("retryAfter");
            if (retryAfter != null) {
                model.addAttribute("retryAfter", retryAfter);
            }
            session.removeAttribute("rateLimitError");
            session.removeAttribute("retryAfter");
        } else if (!model.containsAttribute("error")) {
            model.addAttribute("error", "An error occurred");
        }
        return "error";
    }

    @PostMapping("/pay")
    public String pay(
            @Valid @ModelAttribute("paymentRequest") PaymentRequestDto paymentRequest,
            BindingResult bindingResult,
            HttpSession session,
            Model model) {

        String sessionToken = (String) session.getAttribute(PAYMENT_TOKEN_KEY);
        String submittedToken = paymentRequest.paymentToken();

        if (sessionToken == null || !sessionToken.equals(submittedToken)) {
            log.warn("Attempt to resubmit form or invalid token");
            model.addAttribute("error", "Form has already been submitted. Please fill out the form again.");
            return "error";
        }

        if (bindingResult.hasErrors()) {
            log.warn("Validation errors: {}", bindingResult.getAllErrors());
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getDefaultMessage())
                    .orElse("Data validation error");
            model.addAttribute("error", errorMessage);
            return "error";
        }

        session.removeAttribute(PAYMENT_TOKEN_KEY);

        try {
            log.info("Creating deposit for amount: {}", paymentRequest.amount());
            String redirectUrl = paytechService.createDeposit(paymentRequest.amount());
            log.info("Deposit successfully created, redirectUrl: {}", redirectUrl);

            model.addAttribute("redirectUrl", redirectUrl);
            model.addAttribute("amount", paymentRequest.amount());
            return "success";
        } catch (Exception e) {
            log.error("Error creating deposit for amount {}: {}",
                    paymentRequest.amount(), e.getMessage(), e);
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }
}

