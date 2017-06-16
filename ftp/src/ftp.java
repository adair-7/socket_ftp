import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.StringTokenizer;

import javax.swing.plaf.synth.SynthSpinnerUI;
import javax.tools.Tool;

import org.omg.CosNaming.IstringHelper;

public class ftp {
	
	Socket mFtpClient=null;
	BufferedReader mReader=null;
	BufferedWriter mWriter=null;

	
	//连接FTP
	public void connectFtp(String ip,int port){
		try{
			mFtpClient = new Socket(ip,port);
			mReader= new BufferedReader(new InputStreamReader(mFtpClient.getInputStream()));
			mWriter = new BufferedWriter(new OutputStreamWriter(mFtpClient.getOutputStream()));
			
			System.out.println(mReader.readLine()+"\n");
			
			sendCommand("USER "+"anonymous");
			System.out.println(mReader.readLine()+"\n");
			
			sendCommand("PASS "+"");
			System.out.println(mReader.readLine()+"\n");
			
			
		}catch(Exception e) {  
            e.printStackTrace();  
        }  
	}
	
	
	//发送命令
	private void sendCommand(String command){
		if(command.isEmpty()){
			return;
		}
		
		if(mFtpClient==null){
			return ;
		}
		try {

			mWriter.write(command +"\r\n");
			mWriter.flush();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}
	
	
	//断开FTP
	public void disconnectFtp(){
		if(mFtpClient==null) return;
		
		if(!mFtpClient.isConnected()) return ;
		
		try {
			mFtpClient.close();
			System.out.println("断开连接");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//读取服务器返回信息
	 private String readNewMessage() throws IOException {
	        String response = null;
	        while (true) {
	            response = mReader.readLine();
	            if (response == null || response.length() == 0) {
	                return null;
	            }
	            if (isLegalMessage(response)) {
	                break;
	            }
	        }
	        System.out.println(response+"\n");
	        return response;
	    }
	
	//判断信息合法
	 private boolean isLegalMessage(String msg) {
	        String rexp = "Entering Passive Mode";
	        if (msg.contains(rexp)) {
	            return true;
	        }
	        return false;
	    }
	
	//截取IP端口
	private String[] getIpPort(String response) throws IOException{
		String[] ipPort=new String[2];
		String ip=null;
		int port = -1;
        int opening = response.indexOf('(');
        int closing = response.indexOf(')', opening + 1);

        if (closing > 0) {
            String dataLink = response.substring(opening + 1, closing);
            StringTokenizer tokenizer = new StringTokenizer(dataLink, ",");
            try {
                ip = tokenizer.nextToken() + "." + tokenizer.nextToken() + "." + tokenizer.nextToken() + "." + tokenizer.nextToken();
                port = Integer.parseInt(tokenizer.nextToken()) * 256 + Integer.parseInt(tokenizer.nextToken());
            } catch (Exception e) {
                throw new IOException("链路信息错误: " + response);
            }
        }

        ipPort[0] = ip;
        ipPort[1] = String.valueOf(port);

        return ipPort;
	}
	
	
	
	//获取socket状态
	private boolean socketStatus(Socket socket){
		if(socket==null || !socket.isConnected()){
			return false;
		}
		return true;
	}
	
	
	//下载文件
	public void downloadFile(String localPath,String ftpPath)throws Exception{
		//进入被动模式
		sendCommand("PASV");
		
		//获取IP和端口
		String response=readNewMessage();
		String[] ipPort=getIpPort(response);
		String ip=ipPort[0];
		int port=Integer.parseInt(ipPort[1]);
		
		//建立数据端口连接
		Socket dataSocket= new Socket(ip, port);
		sendCommand("RETR "+ftpPath);
		
		//下载前准备
		File localFile=new File(localPath);
		InputStream inputStream=dataSocket.getInputStream();
		FileOutputStream fileoutputstream =new FileOutputStream(localFile);
		
		System.out.println(inputStream.available()+"\n");
		//下载文件
		int offset;
        byte[] bytes = new byte[1024];
//        fileoutputstream.write(bytes, 0, 41);
        while ( (offset = inputStream.read(bytes)) != -1) {
        	fileoutputstream.write(bytes, 0, offset);
        }
        System.out.println("下载成功");
		
		//下载完成 关闭资源
		
		fileoutputstream.close();
		inputStream.close();
		dataSocket.close();
	
	}
	
	
	//上传文件
	public void uploadFile(String localPath, String ftpPath) throws IOException {
        //进入被动模式
        sendCommand("PASV");

        //获取IP和端口
        String response = readNewMessage();
        String[] ipPort = getIpPort(response);
        String ip = ipPort[0];
        int port = Integer.parseInt(ipPort[1]);

        //建立端口数据连接
        Socket dataSocket = new Socket(ip, port);
        sendCommand("STOR " + ftpPath);

        // 上传文件前准备
        File localFile = new File(localPath);
        OutputStream outputStream = dataSocket.getOutputStream();
        FileInputStream fileInputStream = new FileInputStream(localFile);

        //上传文件
        int offset;
        byte[] bytes = new byte[1024];
        while ((offset = fileInputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, offset);
        }
        System.out.println("上传成功");

        //传输完成关闭资源
        outputStream.close();
        fileInputStream.close();
        dataSocket.close();
    }
	
	
	
	public static void main(String[] args) {
		Scanner scanf=new Scanner(System.in);
		String ip=scanf.next();
		int port=scanf.nextInt();
		
		//ftp连接
		ftp ftpClient=new ftp();
 		ftpClient.connectFtp(ip,port);
		
		System.out.println("ftp连接状态："+ftpClient.socketStatus(ftpClient.mFtpClient)+"\n");
		
		try {
			
			ftpClient.downloadFile("D:/ftp/receive.txt", "/downLoad.txt");
			
			ftpClient.uploadFile("D:/ftp/upLoad.txt", "/receive.txt");
			
			ftpClient.disconnectFtp();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
}

