/*
 * Copyright (c) 2014.
 *
 * BaasBox - info@baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baasbox.controllers;

import static play.mvc.Controller.ctx;
import static play.mvc.Controller.response;
import static play.mvc.Results.forbidden;
import static play.mvc.Results.ok;

import java.util.Set;

import org.slf4j.LoggerFactory;

import play.Logger;
import play.mvc.Result;
import ch.qos.logback.classic.Level;

import com.baasbox.BBConfiguration;
import com.baasbox.controllers.actions.filters.SessionTokenAccess;
import com.baasbox.dao.RoleDao;
import com.baasbox.db.DbHelper;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.exception.InvalidAppCodeException;
import com.baasbox.service.events.EventSource;
import com.baasbox.service.events.EventsService;
import com.baasbox.service.events.EventsService.StatType;
import com.baasbox.service.logging.BaasBoxEvenSourceAppender;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;

/**
 * Created by eto on 13/10/14.
 */
public class EventsController {

	private static volatile Level initialLogInfo=null;
	
	private static Result doTask(StatType typeOfLog){
        response().setContentType("text/event-stream");
        return ok(EventSource.source((eventSource) -> {
        	
            eventSource.onDisconnected(() -> {
            		Logger.debug("Help! I'm loosing the connection....."+eventSource.id);;
            		boolean noMore=EventsService.removeListener(typeOfLog,eventSource);
            		if (typeOfLog==StatType.SYSTEM_LOGGER){
	            		if (noMore){
	                        ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger("application")) 
	                    	.setLevel(initialLogInfo);
	                        ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger("application")) 
	                        .detachAppender(BaasBoxEvenSourceAppender.name);
	            		}
            		}
            	});
            EventsService.addListener(typeOfLog,eventSource);
        }));
	}
	
    public static Result openLogger(){
    	if (!checkAuth()) return forbidden("Please check your credentials. Only administrators can access this resource");
        try {
            if (Logger.isTraceEnabled())Logger.trace("Method start");
            return doTask(StatType.SCRIPT);
        } finally {
            if (Logger.isTraceEnabled()) Logger.trace("Method end");
        }
    }
    
    public static Result openSystemLogger(){
    	if (!checkAuth()) return forbidden("Please check your credentials. Only administrators can access this resource");
        try {
            if (Logger.isTraceEnabled())Logger.trace("Method start");
            if (initialLogInfo==null) 
            	initialLogInfo= ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger("application"))
            				.getLevel();
            ch.qos.logback.classic.Logger logger = ((ch.qos.logback.classic.Logger)LoggerFactory.getLogger("application")) ;
            logger.setLevel(Level.DEBUG);
            if (logger.getAppender(BaasBoxEvenSourceAppender.name) == null) 
            	logger.addAppender(BaasBoxEvenSourceAppender.appender);
            return doTask(StatType.SYSTEM_LOGGER);
        } finally {
            if (Logger.isTraceEnabled()) Logger.trace("Method end");
        }
    }
    
    private static boolean checkAuth(){
        SessionTokenAccess sessionTokenAccess = new SessionTokenAccess();
        boolean okCredentials = sessionTokenAccess.setCredential(ctx());
        if (!okCredentials) {
            return false;
        } else {
            String username = (String) ctx().args.get("username");
            if (username.equalsIgnoreCase(BBConfiguration.getBaasBoxUsername()) ||
                    username.equalsIgnoreCase(BBConfiguration.getBaasBoxAdminUsername())) {
                return false;
            }
        }
        String appcode = (String) ctx().args.get("appcode");
        String username = (String) ctx().args.get("username");
        String password = (String) ctx().args.get("password");
        try {
            DbHelper.open(appcode, username, password);
            OUser user = DbHelper.getConnection().getUser();
            Set<ORole> roles = user.getRoles();
            if (!roles.contains(RoleDao.getRole(DefaultRoles.ADMIN.toString()))) {
                return false;
            }
            return true;
        } catch (InvalidAppCodeException e) {
            return false;
        } finally {
            DbHelper.close(DbHelper.getConnection());
        }
    }
}
