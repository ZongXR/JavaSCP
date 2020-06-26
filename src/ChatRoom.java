import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * <p>Title: ChatRoom</p>
 * <p>Description: 简易的网络聊天室，基于UDP传输</p>
 * <p>Copyright: Copyright (c) 2020</p>
 * <p>Company: www.tedu.cn</p>
 *
 * @author 宗祥瑞
 * @version 1.0
 * @date 2020/6/21
 */
public class ChatRoom {
    public static void main(String[] args) {
        Sender sender = new Sender(args[0], Integer.parseInt(args[1]), args[2]);
        Receiver receiver = new Receiver(Integer.parseInt(args[1]), args[2]);
        Thread t_sender = new Thread(sender);
        Thread t_receiver = new Thread(receiver);
        t_receiver.start();
        t_sender.start();
    }
}

/**
 * 发送端
 * */
class Sender implements Runnable {
    private String name;
    private InetSocketAddress address;

    /**
     * 将名字默认取为线程名字
     *
     * @param host 域名
     * @param port 端口号
     * */
    public Sender(String host, int port) {
        this.name = Thread.currentThread().getName();
        this.address = new InetSocketAddress(host, port);
    }

    /**
     * 指定名字
     *
     * @param host 域名
     * @param port 端口号
     * @param name 名字
     * */
    public Sender(String host, int port, String name){
        this(host, port);
        this.name = name;
    }

    /**
     * 获取名字
     *
     * @return 名字
     * */
    public String getName() {
        return this.name;
    }

    /**
     * 设置名字
     *
     * @param name 新的名字
     * */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 设置域名
     * @param host 新的域名
     * */
    public void setHost(String host) {
        this.address = new InetSocketAddress(host, this.address.getPort());
    }

    /**
     * 设置端口
     *
     * @param port 新的端口号
     * */
    public void setPort(int port){
        this.address = new InetSocketAddress(this.address.getHostName(), port);
    }

    /**
     * 设置传输地址
     *
     * @param host 新的域名
     * @param port 新的端口号
     * */
    public void setAddress(String host, int port){
        this.setHost(host);
        this.setPort(port);
    }

    /**
     * 发送消息
     *
     * @param msg 要发送的消息
     * */
    public void send(String msg){
        try {
            DatagramSocket socket = new DatagramSocket();
            DatagramPacket data = new DatagramPacket(
                    msg.getBytes(StandardCharsets.UTF_8),
                    msg.getBytes(StandardCharsets.UTF_8).length,
                    this.address
            );
            socket.send(data);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送线程要执行的任务
     * */
    @Override
    public void run() {
        while (true){
            Scanner s = new Scanner(System.in);
            String msg = s.nextLine();
            this.send(msg);
            System.out.printf("我说：%s%n", msg);
        }
    }
}

/**
 * 接收端
 * */
class Receiver implements Runnable {
    private String name;
    private int port;
    private String msg;
    private int initialCapacity;

    /**
     * 默认构造函数，使用线程名字
     *
     * @param port 端口号
     * */
    public Receiver(int port) {
        this.name = Thread.currentThread().getName();
        this.port = port;
        this.initialCapacity = 16384;
    }

    /**
     * 使用端口号和名字构造
     *
     * @param port 端口号
     * @param name 名字
     * */
    public Receiver(int port, String name) {
        this(port);
        this.name = name;
    }

    /**
     * 使用端口号和初始容量构造
     *
     * @param port 端口号
     * @param initialCapacity 初始容量
     * */
    public Receiver(int port, int initialCapacity){
        this(port);
        this.initialCapacity = initialCapacity;
    }

    /**
     * 使用端口号、初始容量、名字构造
     * @param port 端口号
     * @param initialCapacity 初始容量
     * @param name 名字
     * */
    public Receiver(int port, int initialCapacity, String name) {
        this(port, initialCapacity);
        this.name = name;
    }

    /**
     * 获取端口号
     * */
    public int getPort() {
        return this.port;
    }

    /**
     * 设置端口号
     *
     * @param port 新的端口号
     * */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * 获取消息
     *
     * @return 消息
     * */
    public String getMsg() {
        return msg;
    }

    /**
     * 设置消息
     *
     * @param msg 设置消息
     * */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * 接收消息
     *
     * @return 接收到的消息
     * */
    public String receive(){
        try {
            DatagramSocket socket = new DatagramSocket(this.port);
            DatagramPacket data = new DatagramPacket(new byte[this.initialCapacity], this.initialCapacity);
            socket.receive(data);
            socket.close();
            this.name = String.format("%s:%s", data.getAddress().getHostName(), data.getPort());
            return new String(data.getData(), 0, data.getLength(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void run() {
        while (true){
            String msg = this.receive();
            System.out.printf("%n%s说：%s%n", this.name, msg);
        }
    }
}


