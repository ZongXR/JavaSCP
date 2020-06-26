import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * <p>Title: FileTrans</p>
 * <p>Description: 简易的文件传输助手，基于TCP传输</p>
 * <p>Copyright: Copyright (c) 2020</p>
 * <p>Company: www.tedu.cn</p>
 *
 * @author 宗祥瑞
 * @version 1.0
 * @date 2020/6/21
 */
public class FileTrans implements Runnable {
    /**
     * 主函数
     *
     * @param args 存储有对端主机名、接收文本端口、接收文件端口
     */
    public static void main(String[] args) {
        List<String> myArgs = Arrays.asList(args);
        LinkedList<String> argList = new LinkedList<>(myArgs);
        String remoteHost = argList.remove(0);
        int receiveTextPort = Integer.parseInt(argList.remove(0));
        int receiveFilePort = Integer.parseInt(argList.remove(0));
        ReceiveFile fileReceive = new ReceiveFile(receiveFilePort);
        FileTrans textReceive = new FileTrans(receiveTextPort, receiveFilePort);
        textReceive.setRemoteHost(remoteHost);
        Thread fileReceiver = new Thread(fileReceive, "接收文件的线程");
        Thread textReceiver = new Thread(textReceive, "接收文字的线程");
        fileReceiver.start();
        textReceiver.start();
        while (true) {
            System.out.println("请输入请求：");
            Scanner s = new Scanner(System.in);
            String line = s.nextLine();
            fileReceive.setFilePath(new File(line.strip()).getName());
            textReceive.sendText(line, remoteHost);
        }
    }

    private int receiveTextPort;
    private int receiveFilePort;
    private int initialCapacity;
    private String remoteHost;

    /**
     * 指定接收文字端口号、接收文件端口号
     *
     * @param receiveTextPort 接收文字端口号
     * @param receiveFilePort 接收文件端口号
     * */
    public FileTrans(int receiveTextPort, int receiveFilePort) {
        this.receiveTextPort = receiveTextPort;
        this.receiveFilePort = receiveFilePort;
        this.initialCapacity = 16384;
    }

    /**
     * 指定接收文字端口号、接收文件端口号、缓存容量
     *
     * @param receiveTextPort 接收文字端口号
     * @param receiveFilePort 接收文件端口号
     * @param initialCapacity 缓存容量
     * */
    public FileTrans(int receiveTextPort, int receiveFilePort, int initialCapacity) {
        this(receiveTextPort, receiveFilePort);
        this.initialCapacity = initialCapacity;
    }

    /**
     * 设置接收到的请求ip
     *
     * @param remoteHost 假设192.168.1.100给我发送了请求，那么把remoteHost设置为192.168.1.100
     * */
    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    /**
     * 给某个地址发送指定内容
     *
     * @param content 要发送的内容
     * @param remoteAddress 发给谁
     * */
    public void sendText(String content, String remoteAddress) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(remoteAddress, this.receiveTextPort));
            OutputStream out = socket.getOutputStream();
            out.write(content.getBytes(StandardCharsets.UTF_8));
            socket.shutdownOutput();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 给this.remoteHost发送文件
     *
     * @param filePath 文件路径
     * */
    public void sendFile(String filePath) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(this.remoteHost, this.receiveFilePort));
            File file = new File(filePath);
            byte[] buffer = new byte[this.initialCapacity];
            InputStream in = new FileInputStream(file);
            OutputStream out = socket.getOutputStream();
            int length;
            while (true) {
                length = in.read(buffer);
                if (length == -1)
                    break;
                out.write(buffer, 0, length);
            }
            socket.shutdownOutput();
            socket.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 监听本地端口接收文字
     *
     * @return 接收到的文字
     * */
    public String receiveText() {
        try {
            ServerSocket listenSocket = new ServerSocket();
            listenSocket.bind(new InetSocketAddress(this.receiveTextPort));
            Socket socket = listenSocket.accept();
            this.remoteHost = socket.getInetAddress().getHostAddress();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[this.initialCapacity];
            int length;
            while (true) {
                length = in.read(buffer);
                if (length == -1)
                    break;
                out.write(buffer, 0, length);
            }
            socket.shutdownInput();
            listenSocket.close();
            out.close();
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 子线程要执行的任务：
     * 如果监听到了文字，并且文字是有效文件路径，则发送该文件
     * 如果监听到了文字，并且文字是有效本地目录，则发送子目录内容
     * 如果监听到了文字，并且非上述两种情况，则打印字符串，说明这是别人返回来的子目录内容
     * */
    @Override
    public void run() {
        String text;
        while (true) {
            text = this.receiveText();
            if (new File(text).isFile()) {
                // 如果是文件名，则发送文件
                this.sendFile(text);
            } else if (new File(text).isDirectory()) {
                // 如果是目录名，则发送子目录字符串
                File[] files = new File(text).listFiles();
                if (files == null) {
                    this.sendText("（空文件夹）\n", this.remoteHost);
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (File file : files) {
                        sb.append(file.getName());
                        sb.append("\n");
                    }
                    this.sendText(sb.toString(), this.remoteHost);
                }
            } else {
                // 如果都不是，说明接收到的是子目录字符串，打印出来
                System.out.println("\n" + text);
            }
        }
    }
}

/**
 * 用于接收文件的线程
 * */
class ReceiveFile implements Runnable {
    private int receiveFilePort;
    private int initialCapacity;
    private String filePath;

    /**
     * 指定接收文件的端口、缓存容量
     *
     * @param receiveFilePort 接收文件的端口
     * @param initialCapacity 缓存容量
     * */
    public ReceiveFile(int receiveFilePort, int initialCapacity) {
        this(receiveFilePort);
        this.initialCapacity = initialCapacity;
    }

    /**
     * 指定接收文件的端口
     *
     * @param receiveFilePort 接收文件的端口
     * */
    public ReceiveFile(int receiveFilePort) {
        this.receiveFilePort = receiveFilePort;
        this.initialCapacity = 16384;
    }

    /**
     * 设置接收文件在本地的存储路径
     *
     * @param filePath 文件路径
     * */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * 接收文件，向本地写文件就是了
     * */
    public void receiveFile() {
        try {
            ServerSocket listen = new ServerSocket();
            listen.bind(new InetSocketAddress(this.receiveFilePort));
            Socket socket = listen.accept();
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[this.initialCapacity];
            int length;
            FileOutputStream out = new FileOutputStream(this.filePath);
            while (true) {
                length = in.read(buffer);
                if (length == -1)
                    break;
                out.write(buffer, 0, length);
            }
            socket.shutdownInput();
            listen.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 线程要执行的任务
     * */
    @Override
    public void run() {
        while (true)
            this.receiveFile();
    }
}