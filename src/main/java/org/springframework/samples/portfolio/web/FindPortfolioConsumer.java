package org.springframework.samples.portfolio.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.samples.portfolio.Portfolio;
import org.springframework.samples.portfolio.service.PortfolioService;
import org.springframework.stereotype.Component;
import reactor.event.Event;
import reactor.function.Consumer;

@Component
public class FindPortfolioConsumer implements Consumer<Event<String>> {
    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private PortfolioService portfolioService;

    @Override
    public void accept(Event<String> event) {
        String username = event.getData();
        Portfolio portfolio = portfolioService.findPortfolio(username);
        messagingTemplate.convertAndSendToUser(username, "/queue/positions", portfolio.getPositions());
    }
}
