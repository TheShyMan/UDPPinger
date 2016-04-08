/**
 * UDPClient.java -- Simple UDP client
 *
 */

import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Date;

public class UDPClient extends UDPPinger implements Runnable {
	/** Host to ping */
	String remoteHost;

	/** Port number of remote host */
	int remotePort;

	/** How many pings to send */
	static final int NUM_PINGS = 10;

	/** How many reply pings have we received */
	int numReplies = 0;

	/** Array for holding replies and RTTs */
	static boolean[] replies = new boolean[NUM_PINGS];

	static long[] rtt = new long[NUM_PINGS];

	/*
	 * Send our own pings at least once per second. If no replies received
	 * within 5 seconds, assume ping was lost.
	 */
	/** 1 second timeout for waiting replies */
	static final int TIMEOUT = 1000;

	/** 5 second timeout for collecting pings at the end */
	static final int REPLY_TIMEOUT = 5000;

	/** Create UDPClient object. */
	public UDPClient(String host, int port) {
		remoteHost = host;
		remotePort = port;
	}

	/**
	 * Main Function.
	 */
	public static void main(String args[]) {
		//TODO: 修改为期望Ping的机器正确的IP地址和端口,现在是本机设置.改端口时同时要改UDPServer的设置.
		String host = "59.66.133.223";
		int port = 9876;

		System.out.println("Contacting host " + host + " at port " + port);

		UDPClient client = new UDPClient(host, port);
		client.run();	//运行程序
	}
	
	/** Main code for pinger client thread. */
	public void run() {
		/* Create socket. We do not care which local port we use. */
		createSocket();
		try {
			socket.setSoTimeout(TIMEOUT);
		} catch (SocketException e) {
			System.out.println("Error setting timeout TIMEOUT: " + e);
		}

		for (int i = 0; i < NUM_PINGS; i++) {
			/*
			 * Message we want to send. Add space at the end for easy parsing of
			 * replies.
			 */
			Date now = new Date();
			String message = "PING " + i + " " + now.getTime() + " ";
			replies[i] = false;
			rtt[i] = 1000000;
			PingMessage ping = null;

			/* Send ping to recipient */
			try {
				//TODO: 1.PingMessage对象包含了什么信息?
				//这里pingMessage先通过InetAddress.getByName函数获取了本机的IP地址，然后包含了本地端口号
				//最后的message是要发送的ping命令
				ping = new PingMessage(InetAddress.getByName(remoteHost),
						remotePort, message);
			} catch (UnknownHostException e) {
				System.out.println("Cannot find host: " + e);
			}
			sendPing(ping);

			/* Read reply */
			try {
				//TODO: 2.这里取得的PingMessage reply对象与上面发送的ping对象内容是否一样?
				//这里取得的reply对象是服务器端收到ping命令后，得到的发送时间并且将这个时间原封不动返回
				PingMessage reply = receivePing();
				//TODO:	3.handleReply的作用?是以致它所改变的变量值是什么?
				//函数的作用是从返回的reply信息中获取时间，并计算rtt的值，并且加以计数
				handleReply(reply.getContents());	
			} catch (SocketTimeoutException e) {
				/*
				 * Reply did not arrive. Do nothing for now. Figure out lost
				 * pings later.
				 */
			}
		}
		/*
		 * We sent all our pings. Now check if there are still missing replies.
		 * Wait for a reply, if nothing comes, then assume it was lost. If a
		 * reply arrives, keep looking until nothing comes within a reasonable
		 * timeout.
		 */
		try {
			socket.setSoTimeout(REPLY_TIMEOUT);
		} catch (SocketException e) {
			System.out.println("Error setting timeout REPLY_TIMEOUT: " + e);
		}
		//TODO:	4.这个while循环的作用是什么,与第96行的receivePing调用有什么关系,既然上面都已调用了receivePing()
		//		为何这里要重新调用??
		//这个while循环的作用是在发送完全部的ping命令之后持续等待一段时间，如有返回的包则接受，而对于那些
		//未接到回应的ping判定为丢失
		while (numReplies < NUM_PINGS) {
			try {
				PingMessage reply = receivePing();
				handleReply(reply.getContents());
			} catch (SocketTimeoutException e) {
				/* Nothing coming our way apparently. Exit loop. */
				numReplies = NUM_PINGS;
			}
		}
		/* Print statistics */
		for (int i = 0; i < NUM_PINGS; i++) {
			System.out.println("PING " + i + ": " + replies[i] + " RTT: "
					+ ((rtt[i] > 0) ? Long.toString(rtt[i]):"< 1")+ " ms");
		}

	}

	/**
	 * Handle the incoming ping message. For now, just count it as having been
	 * correctly received.
	 */
	private void handleReply(String reply) {
		String[] tmp = reply.split(" ");
		int pingNumber = Integer.parseInt(tmp[1]);
		long then = Long.parseLong(tmp[2]);
		replies[pingNumber] = true;
		/* Calculate RTT and store it in the rtt-array. */
		Date now = new Date();
		//TODO:	5. 请简要说明这里的rtt的计算过程.
		//服务器端返回的reply信息中含两方面信息，首先是这是对应第几个ping的返回，其次是那个ping命令的发送时间
		//这里提取出reply当中的后半截信息，也就是原先ping命令的发送时间，然后通过gettime获取系统现在的时间，
		//和发送时间相减，作为一个往返所需要的时间
		rtt[pingNumber] = now.getTime() - then;	
		numReplies++;
	}
}
