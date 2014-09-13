/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
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

package com.baasbox.db;

import play.Logger;

import com.baasbox.dao.RoleDao;
import com.baasbox.enumerations.DefaultRoles;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class Evolution_0_9_0 implements IEvolution {
	private String version="0.9.0";
	
	public Evolution_0_9_0() {}

	@Override
	public String getFinalVersion() {
		return version;
	}

	@Override
	public void evolve(ODatabaseRecordTx db) {
		Logger.info ("Applying evolutions to evolve to the " + version + " level");
		try{
			registeredRoleInheritsFromAnonymousRole(db);
			updateDefaultTimeFormat(db);
		}catch (Throwable e){
			Logger.error("Error applying evolution to " + version + " level!!" ,e);
			throw new RuntimeException(e);
		}
		Logger.info ("DB now is on " + version + " level");
	}
	
	//issue #195 Registered users should have access to anonymous resources
	private void registeredRoleInheritsFromAnonymousRole(ODatabaseRecordTx db) {
		Logger.info("...updating registered role");
		
		RoleDao.getRole(DefaultRoles.ADMIN.toString()).getDocument().field(RoleDao.FIELD_INHERITED, RoleDao.getRole("admin").getDocument().getRecord() ).save();
		RoleDao.getRole(DefaultRoles.ANONYMOUS_USER.toString()).getDocument().field(RoleDao.FIELD_INHERITED, RoleDao.getRole("writer").getDocument().getRecord() ).save();
		RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString()).getDocument().field(RoleDao.FIELD_INHERITED, RoleDao.getRole("anonymous").getDocument().getRecord() ).save();
		RoleDao.getRole(DefaultRoles.BACKOFFICE_USER.toString()).getDocument().field(RoleDao.FIELD_INHERITED, RoleDao.getRole("writer").getDocument().getRecord() ).save();
		
		
		RoleDao.getRole(DefaultRoles.BASE_READER.toString()).getDocument().field(RoleDao.FIELD_INHERITED, (ODocument) null ).save();
		RoleDao.getRole(DefaultRoles.BASE_WRITER.toString()).getDocument().field(RoleDao.FIELD_INHERITED, (ODocument) null ).save();
		RoleDao.getRole(DefaultRoles.BASE_ADMIN.toString()).getDocument().field(RoleDao.FIELD_INHERITED, (ODocument) null ).save();
		
		db.getMetadata().reload();
		Logger.info("...done");
	}
	
	private void updateDefaultTimeFormat(ODatabaseRecordTx db) {
			DbHelper.execMultiLineCommands(db,true,"alter database DATETIMEFORMAT yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	}
    
}