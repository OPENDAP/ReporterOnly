package org.opendap.harvester.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
//import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
//import org.springframework.context.event.ContextRefreshedEvent;

@Component
public interface Registration extends ApplicationListener<ApplicationReadyEvent> {
	void init();
	void registerationCall();
}