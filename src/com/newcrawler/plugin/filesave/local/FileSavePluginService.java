package com.newcrawler.plugin.filesave.local;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.soso.plugin.bo.FileSavePluginBo;

public final class FileSavePluginService implements com.soso.plugin.FileSavePlugin{
	private static Log logger=LogFactory.getLog(FileSavePluginService.class);
	
	private static final String PROPERTIES_SAVE_DIR="file.save.dir";
	private static final String PROPERTIES_ABSOLUTE_PATH="file.new.absolute.path";
	private static final String PROPERTIES_SAVE_SUBDIR="file.save.sub.dir";
	private static final String PROPERTIES_LENGTH_MIN="file.length.min";
	private static final String PROPERTIES_LENGTH_MAX="file.length.max";
	
	private static final String USERAGENT="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.71 Safari/537.36";
	private static final String METHOD="GET";
	
	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException{
		final String cookie=null;
		String urlString="http://img13.360buyimg.com/n1/g13/M00/18/13/rBEhU1MRSQMIAAAAAACgViqmEYsAAJRVQOiom4AAKBu568.jpg";
		//urlString=URLEncoder.encode(urlString, "UTF-8");
		urlString=urlString.replaceAll(" ", "%20");
		System.out.println(urlString);
		
		FileSavePluginService fileSavePluginService=new FileSavePluginService();
		FileItem fileItem=fileSavePluginService.getFileItem(urlString);
		ResponseItem responseItem=fileSavePluginService.new ResponseItem();
		long time=System.currentTimeMillis();
		FileSavePluginService.readImage(responseItem, urlString, METHOD, cookie, USERAGENT, 1000, -1);
		
		System.out.println((System.currentTimeMillis()-time)+"ms" );
		System.out.println(responseItem.getRealUrl());
		System.out.println(responseItem.getFileByte());
	}
	public String execute(final FileSavePluginBo fileSavePluginBo) {
		final Map<String, String> properties=fileSavePluginBo.getProperties();
		final String fileUrl=fileSavePluginBo.getUrlString();
		final com.soso.plugin.FileSavePlugin.FileDownloadStatusService fileDownloadStatusService=fileSavePluginBo.getFileDownloadStatusService();
		
		String saveDir=".";
		String saveSubDir="";
		String absolutePath="";
		long minLength=-1;//不限制
		long maxLength=-1;//不限制
		
		if(properties!=null ){
			if(properties.containsKey(PROPERTIES_LENGTH_MIN) 
					&& !"".equals(properties.get(PROPERTIES_LENGTH_MIN))){
				minLength=Long.parseLong(properties.get(PROPERTIES_LENGTH_MIN));
			}
			if(properties.containsKey(PROPERTIES_LENGTH_MAX) 
					&& !"".equals(properties.get(PROPERTIES_LENGTH_MAX))){
				maxLength=Long.parseLong(properties.get(PROPERTIES_LENGTH_MAX));
			}
			
			if(properties.containsKey(PROPERTIES_SAVE_DIR) 
				&& !"".equals(properties.get(PROPERTIES_SAVE_DIR))){
				saveDir=properties.get(PROPERTIES_SAVE_DIR);
			}
			
			if(properties.containsKey(PROPERTIES_SAVE_SUBDIR) 
					&& !"".equals(properties.get(PROPERTIES_SAVE_SUBDIR))){
				saveSubDir=properties.get(PROPERTIES_SAVE_SUBDIR);
				saveSubDir=(saveSubDir==null?"":saveSubDir);
				if(!saveSubDir.endsWith("/")){
					saveSubDir+="/";
				}
			}
			
			if(properties.containsKey(PROPERTIES_ABSOLUTE_PATH) 
					&& !"".equals(properties.get(PROPERTIES_ABSOLUTE_PATH))){
				absolutePath=properties.get(PROPERTIES_ABSOLUTE_PATH);
				absolutePath=(absolutePath==null?"":absolutePath);
				if(!absolutePath.endsWith("/")){
					absolutePath+="/";
				}
				absolutePath+=saveSubDir;
			}
		}
		
		File fileDir=null;
		if(saveDir.startsWith(".")){
			String appPath=FileSavePluginService.class.getResource("/").getPath();
			try {
				appPath = URLDecoder.decode(appPath, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				logger.error(ExceptionUtils.getFullStackTrace(e));
			} 
			saveDir=saveDir.replaceFirst("^\\.[/\\\\]+", "");
			appPath=new File(appPath).getParentFile().getParent();
			fileDir=new File(appPath, saveDir);
		}else{
			fileDir=new File(saveDir);
		}
		if(saveSubDir!=null && !"".equals(saveSubDir)){
			fileDir=new File(fileDir, saveSubDir);
		}
		if(!fileDir.exists() || !fileDir.isDirectory()){
			fileDir.mkdirs();
		}
		String urlString=fileUrl.replaceAll(" ", "%20");
		FileItem fileItem=getFileItem(urlString);
		String targetFile=urlString;
		String storageLocation=null;
		try {
			File file=new File(fileDir, fileItem.getFileName());
			targetFile=absolutePath+fileItem.getFileName();
			storageLocation=file.getAbsolutePath();
			if(!file.exists()){
				long time=System.currentTimeMillis();
				
				final String cookie=null;
				
				ResponseItem responseItem=new ResponseItem();
				readImage(responseItem, urlString, METHOD, cookie, USERAGENT, minLength, maxLength);
				if(responseItem.getFileByte()!=null){
					FileOutputStream fileOutputStream=new FileOutputStream(file);
					fileOutputStream.write(responseItem.getFileByte());
					fileOutputStream.close();
				}
				logger.info("Download file: "+urlString +", Save to: "+targetFile+" , time:"+ (System.currentTimeMillis()-time)+"ms" );
				com.soso.plugin.FileSavePlugin.FileDownloadStatusBo fileDownloadStatusBo=new com.soso.plugin.FileSavePlugin.FileDownloadStatusBo();
				fileDownloadStatusBo.setE(null);
				fileDownloadStatusBo.setFailure(false);
				fileDownloadStatusBo.setMessage("time:"+ (System.currentTimeMillis()-time)+"ms");
				fileDownloadStatusBo.setPercentage(100);
				fileDownloadStatusBo.setRate(0);
				fileDownloadStatusBo.setStorageLocation(storageLocation);
				fileDownloadStatusBo.setTargetFile(targetFile);
				fileDownloadStatusBo.setFileLength(responseItem.getFileLength());
				fileDownloadStatusBo.setDownloadFileLength(responseItem.getDownloadFileLength());
				fileDownloadStatusService.save(fileDownloadStatusBo);
			}else{
				com.soso.plugin.FileSavePlugin.FileDownloadStatusBo fileDownloadStatusBo=new com.soso.plugin.FileSavePlugin.FileDownloadStatusBo();
				fileDownloadStatusBo.setFailure(false);
				fileDownloadStatusBo.setMessage("The file exists.");
				fileDownloadStatusBo.setPercentage(100);
				fileDownloadStatusBo.setRate(0);
				fileDownloadStatusBo.setStorageLocation(storageLocation);
				fileDownloadStatusBo.setTargetFile(targetFile);
				
				fileDownloadStatusService.save(fileDownloadStatusBo);
			}
		} catch (Exception e) {
			com.soso.plugin.FileSavePlugin.FileDownloadStatusBo fileDownloadStatusBo=new com.soso.plugin.FileSavePlugin.FileDownloadStatusBo();
			fileDownloadStatusBo.setE(e);
			fileDownloadStatusBo.setFailure(true);
			fileDownloadStatusBo.setMessage(null);
			fileDownloadStatusBo.setPercentage(0);
			fileDownloadStatusBo.setRate(0);
			fileDownloadStatusBo.setStorageLocation(storageLocation);
			fileDownloadStatusBo.setTargetFile(targetFile);
			
			fileDownloadStatusService.save(fileDownloadStatusBo);
		}
		return targetFile;
	}
	
	private FileItem getFileItem(String urlString){
		
		String fileName=null;
		String fileExt=null;
		try {
			fileName=getMD5Str(urlString);
		} catch (Exception e) {
			logger.error(urlString+"/r/n"+ExceptionUtils.getFullStackTrace(e));
		}
		
		String regex="^.*\\.(jpg|gif|png|jpeg).*$";
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(urlString);
		if(matcher.find()) {
			fileExt=matcher.group(1);
		}
		if(fileExt==null){
			fileExt="temp";
		}
		fileName=fileName+"."+fileExt;
		
		FileItem fileItem=new FileItem();
		fileItem.setFileName(fileName);
		fileItem.setFileExt(fileExt);
		return fileItem;
	}
	/**
	 * //使用全路径的MD5值作为文件名，以保障不会重复下载
	 * @param str
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	private String getMD5Str(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		MessageDigest messageDigest = null;
		messageDigest = MessageDigest.getInstance("MD5");
		messageDigest.reset();
		messageDigest.update(str.getBytes("UTF-8"));
		byte[] byteArray = messageDigest.digest();
		StringBuffer md5StrBuff = new StringBuffer();
		for (int i = 0; i < byteArray.length; i++) {
			if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)
				md5StrBuff.append("0").append(
						Integer.toHexString(0xFF & byteArray[i]));
			else
				md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));
		}
		return md5StrBuff.toString();
	}
	private static void readImage(ResponseItem responseItem, String urlString, String method, String cookie, String userAgent, long minLength, long maxLength) throws IOException{
		byte[] fileByte=null;
		String realUrl=urlString;
		HttpURLConnection urlConnection = null;
		try {
			URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.setRequestMethod(method);
			urlConnection.setDefaultUseCaches(false);
			urlConnection.setUseCaches(false);
			urlConnection.setConnectTimeout(30000);
			urlConnection.setReadTimeout(30000);
			if(cookie!=null && !"".equals(cookie)){
				urlConnection.setRequestProperty("Cookie", cookie);
			}
			if(userAgent!=null && !"".equals(userAgent)){
				urlConnection.setRequestProperty("User-Agent", userAgent);
			}
			urlConnection.getResponseCode();
			long fileLength=urlConnection.getContentLengthLong();
			long downloadFileLength=0;
			
			String contentType=urlConnection.getContentType();
			realUrl=urlConnection.getURL().toString();
			if(contentType!=null){
				InputStream in = urlConnection.getInputStream();
				fileByte = readByByte(in);
				in.close();
				downloadFileLength=fileByte.length;
			}
			if(fileLength!=-1 && fileLength!=downloadFileLength){
				throw new RuntimeException("文件下载完成，完整性效验失败，需要重新下载。");
			}
			if(minLength!=-1 && downloadFileLength<minLength){
				throw new RuntimeException("文件下载完成，文件小于最小长度时下载失败。");
			}
			if(maxLength!=-1 && downloadFileLength>maxLength){
				throw new RuntimeException("文件下载完成，文件大于最大长度时下载失败。");
			}
			responseItem.setFileByte(fileByte);
			responseItem.setRealUrl(realUrl);
			responseItem.setFileLength(fileLength);
			responseItem.setDownloadFileLength(downloadFileLength);
		} finally{
			if (urlConnection != null)
				urlConnection.disconnect();
		}
	}
	
	private static byte[] readByByte(InputStream inStream) throws IOException { 
        ByteArrayOutputStream outstream = new ByteArrayOutputStream(); 
        byte[] buffer = new byte[1024];
        int len = -1; 
        while ((len = inStream.read(buffer)) != -1) { 
            outstream.write(buffer, 0, len); 
        }
        outstream.close(); 
        return outstream.toByteArray();
    } 
	private class ResponseItem{
		private byte[] fileByte;
		private String realUrl;
		private long fileLength=-1;
		private long downloadFileLength=0;
		
		public byte[] getFileByte() {
			return fileByte;
		}
		public void setFileByte(byte[] fileByte) {
			this.fileByte = fileByte;
		}
		public String getRealUrl() {
			return realUrl;
		}
		public void setRealUrl(String realUrl) {
			this.realUrl = realUrl;
		}
		public long getFileLength() {
			return fileLength;
		}
		public void setFileLength(long fileLength) {
			this.fileLength = fileLength;
		}
		public long getDownloadFileLength() {
			return downloadFileLength;
		}
		public void setDownloadFileLength(long downloadFileLength) {
			this.downloadFileLength = downloadFileLength;
		}
	}
	class FileItem{
		private String fileName;
		private String fileExt;
		
		public String getFileName() {
			return fileName;
		}
		public void setFileName(String fileName) {
			this.fileName = fileName;
		}
		public String getFileExt() {
			return fileExt;
		}
		public void setFileExt(String fileExt) {
			this.fileExt = fileExt;
		}
	}
}
