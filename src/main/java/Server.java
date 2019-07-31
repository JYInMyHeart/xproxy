/**
 * @Author: xck
 * @File: Server
 * @Time: 11:41 2019/7/26
 */
public class Server {

  public static void main(String[] args) {
    Reactor reactor = new Reactor(9999);
    new Thread(reactor,"reactor").start();
  }
}
