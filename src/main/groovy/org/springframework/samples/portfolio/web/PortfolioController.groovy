/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.portfolio.web

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.messaging.simp.annotation.SendToUser
import org.springframework.samples.portfolio.Portfolio
import org.springframework.samples.portfolio.service.PortfolioService
import org.springframework.samples.portfolio.service.Trade
import org.springframework.samples.portfolio.service.TradeService
import org.springframework.stereotype.Controller
import reactor.core.Environment
import reactor.core.Reactor
import reactor.core.spec.Reactors
import reactor.event.Event

import javax.annotation.PostConstruct
import java.security.Principal

import static reactor.event.selector.Selectors.$

@Controller
public class PortfolioController {

  private static final Log logger = LogFactory.getLog(PortfolioController.class);

  @Bean
  Environment env() {
    return new Environment();
  }

  @Bean
  Reactor createReactor(Environment env) {
    return Reactors.reactor().env(env).dispatcher(Environment.THREAD_POOL).get();
  }

  @Autowired
  private Reactor reactor;

  @Autowired
  private TradeService tradeService;

  @Autowired
  private SimpMessageSendingOperations messagingTemplate;

  @Autowired
  private PortfolioService portfolioService;

  @PostConstruct
  void registerReactor() {
    reactor.on($("findPortfolio")) { Event<String> event ->
      String username = event.getData();
      Portfolio portfolio = portfolioService.findPortfolio(username);
      messagingTemplate.convertAndSendToUser(username, "/queue/positions", portfolio.getPositions());
    }
  }

  @MessageMapping("/positions")
  public void getPositions(final Principal principal) throws Exception {
    reactor.notify("findPortfolio", Event.wrap(principal.getName()));
    logger.debug("Positions for " + principal.getName());
  }

  @MessageMapping("/trade")
  public void executeTrade(Trade trade, Principal principal) {
    trade.setUsername(principal.getName());
    logger.debug("Trade: " + trade);
    this.tradeService.executeTrade(trade);
  }

  @MessageExceptionHandler
  @SendToUser("/queue/errors")
  public String handleException(Throwable exception) {
    return exception.getMessage();
  }

}
