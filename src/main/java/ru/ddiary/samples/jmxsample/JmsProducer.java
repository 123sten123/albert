package ru.ddiary.samples.jmxsample;

import java.util.Date;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.persistence.Id;

import com.sun.xml.internal.bind.v2.model.core.ID;
import com.test.dao.HibernateSessionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import com.test.dao.HibernateSessionFactory;


public class JmsProducer extends Thread implements AutoCloseable {
    private static String DEF_QUEUE = "test.in";

    private final ActiveMQConnectionFactory _connectionFactory;
    private Connection _connection = null;
    private Session _session = null;
    public Queue<String> _messagesQueue;
    private boolean _active = true;
    org.hibernate.Session session = HibernateSessionFactory.getSessionFactory().openSession();


    public JmsProducer(String url) {
        this(url, null, null);
    }


    public JmsProducer(String url, String user, String password) {
        if (user != null && !user.isEmpty() && password != null)
            _connectionFactory = new ActiveMQConnectionFactory(url, user, password);
        else
            _connectionFactory = new ActiveMQConnectionFactory(url);

        _messagesQueue = new PriorityBlockingQueue<String>();

    }


    private MessageProducer init() throws JMSException {
        _connection = _connectionFactory.createConnection();
        _connection.start();
        _session = _connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination dest = _session.createQueue(DEF_QUEUE);
        return _session.createProducer(dest);

    }


    public void send(String line) {
        _messagesQueue.add(line);
    }

    public synchronized void sendMsg(String line) {
        _messagesQueue.add(line);
        notifyAll();
    }


    @Override
    public synchronized void run() {
        try {
            System.out.println("Init producer...");
            MessageProducer producer = init();
            System.out.println("Producer successfully initialized");

            while (_active) {
                try {
                    String text = null;

                    while (_active && (text = _messagesQueue.poll()) != null) {

                        Message msg = _session.createTextMessage(text);
                        producer.send(msg);
                        System.out.println("Message " + msg.getJMSMessageID() + " was sent");
                        session.beginTransaction();
                        com.test.session.Message message = new com.test.session.Message();
                        message.setFirstname(text);
                        message.setId(msg.getJMSMessageID());
                        session.save(message);
                    }

                } catch (JMSException e) {
                    e.printStackTrace();
                    _session.close();
                    _connection.close();
                    producer = init(); // trying to reconnect
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    public void close() {
        _active = false;
        if (_connection != null) {
            try {
                _connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

}
