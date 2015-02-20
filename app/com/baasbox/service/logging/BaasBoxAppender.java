package com.baasbox.service.logging;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.AsyncAppenderBase;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import com.baasbox.service.events.EventsService;
import com.baasbox.service.events.EventsService.StatType;

public class BaasBoxAppender<E> extends  UnsynchronizedAppenderBase<E>{

	private final String PATTERN = "%date - %-5level - %logger: %t  %message %n%rootException";
	
	public static final String name = "BaasBoxAppender";
	public static final BaasBoxAppender<ILoggingEvent> appender = new BaasBoxAppender<ILoggingEvent>();
	
	private PatternLayout layout;
	protected BaasBoxAppender(){
		super();
		this.setName(name);
		this.layout=new PatternLayout();
		layout.setPattern(PATTERN);
		this.start();
	}

	@Override
	protected void append(E message) {
		String toSend=((ILoggingEvent)message).getFormattedMessage();//layout.doLayout((ILoggingEvent)message);
		
		System.out.println("****BAASBOX_APPENDER***: " + toSend);
		EventsService.publish(StatType.SYSTEM_LOGGER, toSend);
	}
	
}
