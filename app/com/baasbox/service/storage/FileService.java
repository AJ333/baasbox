package com.baasbox.service.storage;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.output.ByteArrayOutputStream;

import com.baasbox.configuration.ImagesConfiguration;
import com.baasbox.dao.FileDao;
import com.baasbox.dao.PermissionsHelper;
import com.baasbox.dao.RoleDao;
import com.baasbox.dao.exception.DocumentNotFoundException;
import com.baasbox.dao.exception.FileNotFoundException;
import com.baasbox.dao.exception.InvalidCollectionException;
import com.baasbox.dao.exception.InvalidModelException;
import com.baasbox.dao.exception.SqlInjectionException;
import com.baasbox.enumerations.Permissions;
import com.baasbox.exception.DocumentIsNotAFileException;
import com.baasbox.exception.DocumentIsNotAnImageException;
import com.baasbox.exception.InvalidSizePatternException;
import com.baasbox.exception.RoleNotFoundException;
import com.baasbox.exception.UserNotFoundException;
import com.baasbox.service.storage.StorageUtils.ImageDimensions;
import com.baasbox.service.storage.StorageUtils.WritebleImageFormat;
import com.baasbox.service.user.UserService;
import com.baasbox.util.QueryParams;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class FileService {
	public static final String DATA_FIELD_NAME="attachedData";
	public static final String BINARY_FIELD_NAME=FileDao.BINARY_FIELD_NAME;
	public final static String CONTENT_TYPE_FIELD_NAME=FileDao.CONTENT_TYPE_FIELD_NAME;
	public final static String CONTENT_LENGTH_FIELD_NAME=FileDao.CONTENT_LENGTH_FIELD_NAME;
	
		public static ODocument createFile(String fileName,String data,String contentType, byte[] content) throws Throwable{
			FileDao dao = FileDao.getInstance();
			ODocument doc=dao.create(fileName,contentType,content);
			if (data!=null && !data.trim().isEmpty()) {
				ODocument metaDoc=(new ODocument()).fromJSON("{ '"+DATA_FIELD_NAME+"' : " + data + "}");
				doc.merge(metaDoc, true, false);
			}
			dao.save(doc);
			return doc;
		}

		
		public static ODocument getById(String id) throws SqlInjectionException, InvalidModelException {
			FileDao dao = FileDao.getInstance();
			return dao.getById(id);
		}
		
		public static void deleteById(String id) throws Throwable, SqlInjectionException, FileNotFoundException{
			FileDao dao = FileDao.getInstance();
			ODocument file=getById(id);
			if (file==null) throw new FileNotFoundException();
			dao.delete(file.getIdentity());
		}


		public static List<ODocument> getFiles(QueryParams criteria) throws SqlInjectionException {
			FileDao dao = FileDao.getInstance();
			return dao.get(criteria); 
		}


		public static ODocument grantPermissionToRole(String id,Permissions permission, String rolename) 
				throws RoleNotFoundException, FileNotFoundException, SqlInjectionException, InvalidModelException {
			ORole role=RoleDao.getRole(rolename);
			if (role==null) throw new RoleNotFoundException(rolename);
			ODocument doc = getById(id);
			if (doc==null) throw new FileNotFoundException(id);
			return PermissionsHelper.grant(doc, permission, role);
		}


		public static ODocument revokePermissionToRole(String id,
				Permissions permission, String rolename) throws RoleNotFoundException, FileNotFoundException, SqlInjectionException, InvalidModelException  {
			ORole role=RoleDao.getRole(rolename);
			if (role==null) throw new RoleNotFoundException(rolename);
			ODocument doc = getById(id);
			if (doc==null) throw new FileNotFoundException(id);
			return PermissionsHelper.revoke(doc, permission, role);
		}	
		
		public static ODocument grantPermissionToUser(String id, Permissions permission, String username) throws UserNotFoundException, RoleNotFoundException, FileNotFoundException, SqlInjectionException, IllegalArgumentException, InvalidModelException  {
			OUser user=UserService.getOUserByUsername(username);
			if (user==null) throw new UserNotFoundException(username);
			ODocument doc = getById(id);
			if (doc==null) throw new FileNotFoundException(id);
			return PermissionsHelper.grant(doc, permission, user);
		}

		public static ODocument revokePermissionToUser(String id, Permissions permission, String username) throws UserNotFoundException, RoleNotFoundException, FileNotFoundException, SqlInjectionException, IllegalArgumentException, InvalidModelException {
			OUser user=UserService.getOUserByUsername(username);
			if (user==null) throw new UserNotFoundException(username);
			ODocument doc = getById(id);
			if (doc==null) throw new FileNotFoundException(id);
			return PermissionsHelper.revoke(doc, permission, user);
		}
		
		public static byte[] getResizedPicture(String id, String width, String height) throws InvalidSizePatternException, SqlInjectionException, InvalidModelException, DocumentIsNotAnImageException, DocumentIsNotAFileException, DocumentNotFoundException, IOException{
			String sizePattern = width + "-" + height;
			return getResizedPicture (id,sizePattern);
		}
		
		public static byte[] getResizedPicture(String id, int sizeId) throws InvalidSizePatternException, SqlInjectionException, InvalidModelException, DocumentIsNotAnImageException, DocumentIsNotAFileException, DocumentNotFoundException, IOException{
			String sizePattern = "";
			try{
				sizePattern=ImagesConfiguration.IMAGE_ALLOWED_AUTOMATIC_RESIZE_FORMATS.getValueAsString().split(" ")[sizeId];
			}catch (IndexOutOfBoundsException e){
				throw new InvalidSizePatternException("The specified id is out of range.");
			}
			return getResizedPicture (id,sizePattern);
		}
		
		private static byte[] getResizedPicture(String id, String sizePattern) throws InvalidSizePatternException, SqlInjectionException, InvalidModelException, DocumentIsNotAnImageException, DocumentIsNotAFileException, DocumentNotFoundException, IOException {
			ImageDimensions dimensions = StorageUtils.convertSizeToDimensions(sizePattern);
			return getResizedPicture (id,dimensions);
		}


		private static byte[] getResizedPicture(String id, ImageDimensions dimensions) throws SqlInjectionException, InvalidModelException, DocumentIsNotAnImageException, DocumentNotFoundException, DocumentIsNotAFileException, IOException {
			//get the file
			ODocument file = getById(id);
			if (file==null) throw new DocumentNotFoundException();
			//is the file an image?
			if (!StorageUtils.docIsAnImage(file)) throw new DocumentIsNotAnImageException("The file " + id + " is not an image");
			//are the dimensions allowed?
			//the check is delegated to the caller
			String sizePattern= dimensions.toString();
			try{
				FileDao dao=FileDao.getInstance();
				byte[] resizedImage = dao.getStoredResizedPicture( file,  sizePattern);
				if (resizedImage!=null) return resizedImage;
				
				ByteArrayOutputStream fileContent = StorageUtils.extractFileFromDoc(file);
				String contentType = getContentType(file);
				String ext = contentType.substring(contentType.indexOf("/")+1);
				WritebleImageFormat format;
				try{
					format = WritebleImageFormat.valueOf(ext);
				}catch (Exception e){
					format= WritebleImageFormat.png;
				}
				resizedImage=StorageUtils.resizeImage(fileContent.toByteArray(), format, dimensions);
				
				//save the resized image for future requests
				dao.storeResizedPicture(file, sizePattern, resizedImage);
				return resizedImage;
			}catch ( InvalidModelException e) {
				throw new RuntimeException("A very strange error occurred! ",e);
			}

		}

		public static String getContentType(ODocument file) {
			return (String) file.field(CONTENT_TYPE_FIELD_NAME);
		}
		
}
